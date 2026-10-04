package com.tick.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Notification;

import java.util.List;

/**
 * 站内通知服务：按账号查询与已读状态维护。
 */
public interface NotificationService extends IService<Notification> {
    /**
     * 某账号的通知列表，按时间倒序。
     */
    List<Notification> findByAccountId(Integer accountId);

    /**
     * 标记单条通知已读；通知不属于该账号时返回 false。
     */
    boolean markRead(Integer accountId, Integer notificationId);

    /**
     * 标记该账号全部通知已读。
     */
    boolean markAllRead(Integer accountId);
}
