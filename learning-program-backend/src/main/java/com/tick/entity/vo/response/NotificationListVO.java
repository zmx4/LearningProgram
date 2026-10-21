package com.tick.entity.vo.response;

import com.tick.entity.dto.Notification;

import java.util.List;

/**
 * 当前用户的通知列表与未读数。
 */
public record NotificationListVO(List<Notification> items, long unreadCount) {
}
