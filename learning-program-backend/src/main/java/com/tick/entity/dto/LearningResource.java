package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源：管理员上传的资料文件，文件本体存放在本地上传目录或对象存储，
 * stored_name 为托管文件名（对象存储时是对象键名），storage 记录它落在哪一侧，
 * visible 决定是否对学员展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_learning_resource")
public class LearningResource {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    @TableField("file_name")
    private String fileName;
    @TableField("stored_name")
    private String storedName;
    /**
     * 存放位置：local=本地磁盘，oss=对象存储。空值按 local 处理（兼容历史数据）。
     */
    private String storage;
    @TableField("content_type")
    private String contentType;
    @TableField("file_size")
    private Long fileSize;
    private Boolean visible;
    @TableField("account_id")
    private Integer accountId;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
