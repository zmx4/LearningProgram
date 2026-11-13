package com.tick.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.plugin.oss.service.OssTemplate;
import com.tick.entity.dto.Account;
import com.tick.mapper.AccountMapper;
import com.tick.service.AvatarService;
import com.tick.storage.StoredFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 头像服务实现。与学习资源不同，头像不支持本地磁盘回退：
 * 只有对象存储启用时才允许上传，对象键统一落在 {@link #OBJECT_PREFIX} 前缀下，
 * 与资源文件（resources/）共用一个桶但互不干扰。
 */
@Slf4j
@Service
public class AvatarServiceImpl implements AvatarService {
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final String OBJECT_PREFIX = "avatars/";
    // 允许的图片类型白名单：托管文件名的扩展名由声明类型决定，不信任用户上传的文件名
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    private final ObjectProvider<OssTemplate> ossTemplateProvider;
    private final AccountMapper accountMapper;
    private final String bucketName;

    public AvatarServiceImpl(ObjectProvider<OssTemplate> ossTemplateProvider,
                             AccountMapper accountMapper,
                             @Value("${oss.bucket-name:}") String bucketName) {
        this.ossTemplateProvider = ossTemplateProvider;
        this.accountMapper = accountMapper;
        this.bucketName = bucketName;
    }

    @Override
    public boolean enabled() {
        return ossTemplateProvider.getIfAvailable() != null;
    }

    @Override
    @Transactional
    public Account upload(Integer accountId, MultipartFile file) {
        OssTemplate ossTemplate = ossTemplateProvider.getIfAvailable();
        if (ossTemplate == null) {
            throw new IllegalArgumentException("头像功能仅在启用对象存储时可用");
        }
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            return null;
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("头像图片不能超过 2MB");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("仅支持 JPG / PNG / GIF / WebP 图片");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("头像文件读取失败，请稍后再试");
        }
        if (!matchesImageMagic(bytes, contentType)) {
            throw new IllegalArgumentException("文件内容不是有效的图片，仅支持 JPG / PNG / GIF / WebP 图片");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        ossTemplate.putObject(bucketName, OBJECT_PREFIX + storedName,
                new ByteArrayInputStream(bytes), bytes.length, contentType);

        String previous = account.getAvatar();
        // 只更新 avatar 列，避免把加载后其他字段的旧值写回；置空必须走 set(null)，updateById 会忽略 null
        accountMapper.update(null, Wrappers.<Account>update()
                .set("avatar", storedName)
                .eq("id", accountId));
        account.setAvatar(storedName);
        deleteObject(ossTemplate, previous);
        return account;
    }

    @Override
    @Transactional
    public Account remove(Integer accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            return null;
        }
        String previous = account.getAvatar();
        if (previous == null || previous.isBlank()) {
            return account;
        }
        accountMapper.update(null, Wrappers.<Account>update()
                .set("avatar", null)
                .eq("id", accountId));
        account.setAvatar(null);
        OssTemplate ossTemplate = ossTemplateProvider.getIfAvailable();
        if (ossTemplate != null) {
            deleteObject(ossTemplate, previous);
        }
        return account;
    }

    @Override
    public StoredFile open(Account account) {
        OssTemplate ossTemplate = ossTemplateProvider.getIfAvailable();
        if (ossTemplate == null || account == null
                || account.getAvatar() == null || account.getAvatar().isBlank()) {
            return null;
        }
        try {
            InputStream stream = ossTemplate.getObject(bucketName, OBJECT_PREFIX + account.getAvatar());
            // 长度未知，交给容器按 chunked 输出
            return stream == null ? null : new StoredFile(stream, -1);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    /**
     * 清理旧头像。清理失败只记日志，不影响上传 / 移除本身。
     */
    private void deleteObject(OssTemplate ossTemplate, String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            ossTemplate.removeObject(bucketName, OBJECT_PREFIX + storedName);
        } catch (RuntimeException exception) {
            log.warn("旧头像清理失败: storedName={}", storedName, exception);
        }
    }

    /**
     * 校验文件头魔数与声明类型一致，防止把任意内容伪装成图片存进桶里。
     */
    private boolean matchesImageMagic(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                    && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
            case "image/gif" -> bytes.length >= 6
                    && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8'
                    && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}