package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Notification;
import com.tick.mapper.NotificationMapper;
import com.tick.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 站内通知服务实现。
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Override
    public List<Notification> findByAccountId(Integer accountId) {
        return query()
                .eq("account_id", accountId)
                .orderByDesc("created_at")
                .list();
    }

    @Override
    public void create(Integer accountId, String title, String content, String link, String type) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        notification.setType(type);
        notification.setReadStatus(false);
        notification.setCreatedAt(new Date());
        save(notification);
    }

    @Override
    public long countUnread(Integer accountId) {
        Long count = query().eq("account_id", accountId)
                .eq("is_read", false)
                .count();
        return count == null ? 0 : count;
    }

    @Override
    public boolean markRead(Integer accountId, Integer notificationId) {
        return update()
                .eq("id", notificationId)
                .eq("account_id", accountId)
                .set("is_read", true)
                .update();
    }

    @Override
    public boolean markAllRead(Integer accountId) {
        return update()
                .eq("account_id", accountId)
                .eq("is_read", false)
                .set("is_read", true)
                .update();
    }
}
