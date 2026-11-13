package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.entity.vo.response.StorageStatusVO;
import com.tick.mapper.AccountMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.ResourceAccessLogMapper;
import com.tick.service.LearningResourceService;
import com.tick.storage.ResourceStorage;
import com.tick.storage.ResourceStorageManager;
import com.tick.storage.StoredFile;
import com.tick.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 学习资源服务实现。
 */
@Slf4j
@Service
public class LearningResourceServiceImpl extends ServiceImpl<LearningResourceMapper, LearningResource>
        implements LearningResourceService {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    // 托管文件名的扩展名只保留字母数字，防止把路径分隔符等写进磁盘文件名 / 对象键
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

    private final AccountMapper accountMapper;
    private final ResourceAccessLogMapper accessLogMapper;
    private final ResourceStorageManager storageManager;

    public LearningResourceServiceImpl(AccountMapper accountMapper,
                                       ResourceAccessLogMapper accessLogMapper,
                                       ResourceStorageManager storageManager) {
        this.accountMapper = accountMapper;
        this.accessLogMapper = accessLogMapper;
        this.storageManager = storageManager;
    }

    @Override
    public List<ResourceVO> listVisible() {
        return toVOList(lambdaQuery().eq(LearningResource::getVisible, true)
                .orderByDesc(LearningResource::getCreatedAt).orderByDesc(LearningResource::getId)
                .list());
    }

    @Override
    public List<ResourceVO> listAll() {
        return toVOList(query().orderByDesc("created_at").orderByDesc("id").list());
    }

    @Override
    @Transactional
    public ResourceVO upload(MultipartFile file, String title, String description, boolean visible,
                             Integer accountId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 50MB");
        }
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("资源标题不能为空");
        }
        if (normalizedTitle.length() > 100) {
            throw new IllegalArgumentException("资源标题不能超过 100 个字符");
        }
        String normalizedDescription = description == null ? null : description.trim();
        if (normalizedDescription != null && normalizedDescription.isEmpty()) {
            normalizedDescription = null;
        }
        if (normalizedDescription != null && normalizedDescription.length() > 255) {
            throw new IllegalArgumentException("资源描述不能超过 255 个字符");
        }
        String originalName = cleanFileName(file.getOriginalFilename());

        String storedName = UUID.randomUUID().toString().replace("-", "")
                + extensionOf(originalName);
        StorageType storageType = defaultStorageType();
        ResourceStorage storage = storageManager.forType(storageType);
        try (InputStream stream = file.getInputStream()) {
            storage.store(storedName, stream, file.getSize(), file.getContentType());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败，请稍后再试");
        }

        LearningResource resource = new LearningResource(null, normalizedTitle, normalizedDescription,
                originalName, storedName, storageType.code(), file.getContentType(), file.getSize(),
                visible, accountId, LocalDateTime.now());
        save(resource);
        return toVO(resource, uploaderNames(List.of(resource)).get(accountId));
    }

    @Override
    @Transactional
    public void setVisible(Integer id, boolean visible) {
        LearningResource resource = getResource(id);
        if (resource == null) {
            throw new IllegalArgumentException("资源不存在");
        }
        resource.setVisible(visible);
        updateById(resource);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        LearningResource resource = getResource(id);
        if (resource == null) {
            throw new IllegalArgumentException("资源不存在");
        }
        removeById(id);
        deleteStoredFile(resource);
    }

    @Override
    public LearningResource getResource(Integer id) {
        return id == null ? null : getById(id);
    }

    @Override
    public StoredFile openFile(LearningResource resource) {
        if (resource == null || resource.getStoredName() == null || resource.getStoredName().isBlank()) {
            return null;
        }
        ResourceStorage storage = storageManager.forType(StorageType.from(resource.getStorage()));
        try {
            InputStream stream = storage.open(resource.getStoredName());
            if (stream == null) {
                return null;
            }
            long size = resource.getFileSize() == null ? -1 : resource.getFileSize();
            return new StoredFile(stream, size);
        } catch (IllegalArgumentException exception) {
            // stored_name 被改成带路径的值：按文件不存在处理，别把异常抛成 400 打断下载
            log.warn("资源文件名非法，按文件丢失处理: id={}, storedName={}",
                    resource.getId(), resource.getStoredName());
            return null;
        } catch (IOException exception) {
            throw new IllegalStateException("资源文件读取失败，请稍后再试");
        }
    }

    @Override
    public StorageStatusVO storageStatus() {
        boolean enabled = storageManager.objectStorageEnabled();
        return new StorageStatusVO(storageManager.defaultType().code(), enabled,
                enabled ? storageManager.ossBucket() : null);
    }

    @Override
    public StorageType defaultStorageType() {
        return storageManager.defaultType();
    }

    @Override
    public boolean objectStorageEnabled() {
        return storageManager.objectStorageEnabled();
    }

    @Override
    public void recordAccess(Integer accountId, Integer resourceId) {
        if (accountId == null || resourceId == null) {
            return;
        }
        accessLogMapper.upsertAccess(accountId, resourceId, LocalDateTime.now());
    }

    /**
     * 清理文件。删库成功而清理失败时不回滚业务，只记日志：遗留对象不影响功能，
     * 也避免对象存储抖动把删除接口拖成失败。
     */
    private void deleteStoredFile(LearningResource resource) {
        if (resource.getStoredName() == null || resource.getStoredName().isBlank()) {
            return;
        }
        ResourceStorage storage = storageManager.forType(StorageType.from(resource.getStorage()));
        try {
            storage.delete(resource.getStoredName());
        } catch (IOException | RuntimeException exception) {
            log.warn("资源文件清理失败: id={}, storage={}, storedName={}",
                    resource.getId(), resource.getStorage(), resource.getStoredName(), exception);
        }
    }

    private String cleanFileName(String original) {
        String name = original == null ? "" : original.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\x00-\\x1f]", "").trim();
        if (name.isEmpty()) {
            name = "file";
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("文件名过长");
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot + 1);
        Matcher matcher = SAFE_EXTENSION.matcher(extension);
        return matcher.matches() ? "." + extension.toLowerCase() : "";
    }

    private List<ResourceVO> toVOList(List<LearningResource> resources) {
        Map<Integer, String> uploaderNames = uploaderNames(resources);
        return resources.stream()
                .map(resource -> toVO(resource, uploaderNames.get(resource.getAccountId())))
                .toList();
    }

    /**
     * 一次查出整批上传人，避免按条回查账号。只取 id/username，不带出密码等字段。
     */
    private Map<Integer, String> uploaderNames(List<LearningResource> resources) {
        Set<Integer> accountIds = resources.stream().map(LearningResource::getAccountId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .select(Account::getId, Account::getUsername)
                        .in(Account::getId, accountIds))
                .stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));
    }

    private ResourceVO toVO(LearningResource resource, String uploaderName) {
        return new ResourceVO(resource.getId(), resource.getTitle(), resource.getDescription(),
                resource.getFileName(), resource.getFileSize(), resource.getContentType(),
                resource.getVisible(), uploaderName, resource.getCreatedAt(),
                StorageType.from(resource.getStorage()).code());
    }
}
