package com.tick.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    List<Notification> findByAccountId(Integer accountId);

    boolean markRead(Integer accountId, Integer notificationId);

    boolean markAllRead(Integer accountId);
}
