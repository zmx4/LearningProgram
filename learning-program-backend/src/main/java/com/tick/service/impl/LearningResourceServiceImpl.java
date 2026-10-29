package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.mapper.AccountMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.ResourceAccessLogMapper;
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
@Service
public class LearningResourceServiceImpl extends ServiceImpl<LearningResourceMapper, LearningResource>
        implements LearningResourceService {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    // 托管文件名的扩展名只保留字母数字，防止把路径分隔符等写进磁盘文件名
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

    private final AccountMapper accountMapper;
    private final ResourceAccessLogMapper accessLogMapper;
    private final Path storageDir;

    public LearningResourceServiceImpl(AccountMapper accountMapper,
                                       ResourceAccessLogMapper accessLogMapper,
                                       @Value("${learning.resources.upload-dir:./uploads/resources}") String uploadDir) {
        this.accountMapper = accountMapper;
        this.accessLogMapper = accessLogMapper;
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
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
        deleteStoredFile(resource.getStoredName());
    }

    @Override
    public LearningResource getResource(Integer id) {
        return id == null ? null : getById(id);
    }

    @Override
    public void recordAccess(Integer accountId, Integer resourceId) {
        if (accountId == null || resourceId == null) {
            return;
        }
        accessLogMapper.upsertAccess(accountId, resourceId, LocalDateTime.now());
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
                resource.getVisible(), uploaderName, resource.getCreatedAt());
    }
}
