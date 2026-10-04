package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源视图。管理端返回全部（含 visible 与上传者），学员端仅返回 visible 的资源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceVO {
    private Integer id;
    private String title;
    private String description;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Boolean visible;
    private String uploaderName;
    private LocalDateTime createdAt;
}
