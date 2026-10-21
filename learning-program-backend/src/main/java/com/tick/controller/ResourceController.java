package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.vo.response.ResourceVO;
import com.tick.service.LearningResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 学习资源的读取与下载接口，登录用户可访问。仅展示 visible 的资源；
 * 隐藏资源对非管理员一律按不存在处理。
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

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
        Path path = resourceService.resolveStoredPath(resource.getStoredName());
        if (!Files.exists(path)) {
            return notFound("资源文件已丢失，请联系管理员");
        }
        // 访问流水只用于小组任务判定，写失败不应阻断下载
        try {
            resourceService.recordAccess(accountId, id);
        } catch (RuntimeException exception) {
            log.warn("记录资源访问流水失败: resourceId={}, accountId={}", id, accountId, exception);
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(resource.getFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(safeMediaType(resource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(path));
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
