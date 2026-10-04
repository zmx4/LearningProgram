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
 * 学习资源：管理员上传的资料文件，文件本体存放在配置的上传目录，
 * stored_name 为磁盘托管文件名，visible 决定是否对学员展示。
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
