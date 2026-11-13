package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.service.LearningResourceService;
import com.tick.storage.StoredFile;
import com.tick.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 学习资源的读取与下载接口，登录用户可访问。仅展示 visible 的资源；
 * 隐藏资源对非管理员一律按不存在处理。
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final LearningResourceService resourceService;

    public ResourceController(LearningResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public RestBean<List<ResourceVO>> listVisible() {
        return RestBean.success(resourceService.listVisible());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Integer id,
                                             @RequestAttribute("id") Integer accountId) {
        LearningResource resource = resourceService.getResource(id);
        if (resource == null || (!Boolean.TRUE.equals(resource.getVisible()) && !isAdmin())) {
            return notFound("资源不存在");
        }
        StoredFile file = resourceService.openFile(resource);
        if (file == null) {
            // 资源在 OSS 而对象存储被关掉时，说清原因，别让管理员以为文件真丢了
            boolean storedInOss = StorageType.from(resource.getStorage()) == StorageType.OSS;
            return notFound(storedInOss && !resourceService.objectStorageEnabled()
                    ? "该资源存放在对象存储，当前未启用对象存储，请联系管理员"
                    : "资源文件已丢失，请联系管理员");
        }
        // 访问流水只用于小组任务判定，写失败不应阻断下载
        try {
            resourceService.recordAccess(accountId, id);
        } catch (RuntimeException exception) {
            log.warn("记录资源访问流水失败: resourceId={}, accountId={}", id, accountId, exception);
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(resource.getFileName(), StandardCharsets.UTF_8).build();
        // 文件可能来自本地磁盘或对象存储，统一以流的形式回写，
        // 显式带上长度让浏览器能显示下载进度
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(safeMediaType(resource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (file.size() > 0) {
            builder.contentLength(file.size());
        }
        return builder.body(new InputStreamResource(file.stream()));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
    }

    private MediaType safeMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ResponseEntity<Resource> notFound(String message) {
        byte[] body = RestBean.failure(404, message).asJsonString()
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ByteArrayResource(body));
    }
}
