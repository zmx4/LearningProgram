package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Notification;
import com.tick.mapper.NotificationMapper;
import com.tick.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

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
