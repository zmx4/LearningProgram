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
 * 完成课程的积分发放记录，(account_id, course_id) 唯一，保证每门课程的奖励只发放一次。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course_reward")
public class CourseReward {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer accountId;
    private Integer courseId;
    private Integer points;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
