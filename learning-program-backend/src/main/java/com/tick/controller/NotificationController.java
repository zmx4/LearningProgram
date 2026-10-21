package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Notification;
import com.tick.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 站内通知接口：当前用户的通知列表、单条已读与全部已读。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public RestBean<Map<String, Object>> list(@RequestAttribute("id") Integer accountId) {
        List<Notification> notifications = notificationService.findByAccountId(accountId);
        long unreadCount = notifications.stream()
                .filter(notification -> !Boolean.TRUE.equals(notification.getReadStatus()))
                .count();
        return RestBean.success(Map.of("items", notifications, "unreadCount", unreadCount));
    }

    @PostMapping("/{id}/read")
    public RestBean<Void> markRead(@PathVariable Integer id,
                                   @RequestAttribute("id") Integer accountId) {
        if (!notificationService.markRead(accountId, id)) {
            return RestBean.failure(404, "通知不存在");
        }
        return RestBean.success();
    }

    @PostMapping("/read-all")
    public RestBean<Void> markAllRead(@RequestAttribute("id") Integer accountId) {
        notificationService.markAllRead(accountId);
        return RestBean.success();
    }
}
