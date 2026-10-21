package com.tick.entity.vo.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 管理员定向通知：targetType 取 all / role / users。
 */
public record AdminNotificationVO(
        @NotBlank(message = "请填写通知标题") String title,
        @NotBlank(message = "请填写通知内容") String content,
        String type,
        @NotBlank(message = "请选择发送范围") String targetType,
        String targetRole,
        List<Integer> userIds
) {
}
