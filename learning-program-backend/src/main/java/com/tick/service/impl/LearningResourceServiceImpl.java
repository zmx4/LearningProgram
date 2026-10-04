package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.mapper.AccountMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.service.LearningResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LearningResourceServiceImpl extends ServiceImpl<LearningResourceMapper, LearningResource>
        implements LearningResourceService {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    // 托管文件名的扩展名只保留字母数字，防止把路径分隔符等写进磁盘文件名
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

    private final AccountMapper accountMapper;
    private final Path storageDir;

    public LearningResourceServiceImpl(AccountMapper accountMapper,
                                       @Value("${learning.resources.upload-dir:./uploads/resources}") String uploadDir) {
        this.accountMapper = accountMapper;
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public List<ResourceVO> listVisible() {
        return lambdaQuery().eq(LearningResource::getVisible, true)
                .orderByDesc(LearningResource::getCreatedAt).orderByDesc(LearningResource::getId)
                .list().stream().map(this::toVO).toList();
    }

    @Override
    public List<ResourceVO> listAll() {
        return query().orderByDesc("created_at").orderByDesc("id").list().stream()
                .map(this::toVO).toList();
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
        try {
            Files.createDirectories(storageDir);
            file.transferTo(storageDir.resolve(storedName).toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败，请稍后再试");
        }

        LearningResource resource = new LearningResource(null, normalizedTitle, normalizedDescription,
                originalName, storedName, file.getContentType(), file.getSize(), visible, accountId,
                LocalDateTime.now());
        save(resource);
        return toVO(resource);
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
        deleteStoredFile(resource.getStoredName());
    }

    @Override
    public LearningResource getResource(Integer id) {
        return id == null ? null : getById(id);
    }

    /**
     * 存储目录下的托管文件路径。stored_name 由服务端生成（UUID + 校验过的扩展名），不含路径成分。
     */
    @Override
    public Path resolveStoredPath(String storedName) {
        return storageDir.resolve(storedName).normalize();
    }

    private void deleteStoredFile(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredPath(storedName));
        } catch (IOException exception) {
            // 磁盘清理失败不阻塞删除，遗留文件不影响业务
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

    private ResourceVO toVO(LearningResource resource) {
        String uploaderName = null;
        if (resource.getAccountId() != null) {
            Account account = accountMapper.selectById(resource.getAccountId());
            uploaderName = account == null ? null : account.getUsername();
        }
        return new ResourceVO(resource.getId(), resource.getTitle(), resource.getDescription(),
                resource.getFileName(), resource.getFileSize(), resource.getContentType(),
                resource.getVisible(), uploaderName, resource.getCreatedAt());
    }
}
