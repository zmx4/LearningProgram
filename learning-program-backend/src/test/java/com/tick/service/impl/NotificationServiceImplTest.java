package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tick.entity.dto.Notification;
import com.tick.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private final NotificationMapper mapper = mock(NotificationMapper.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void createsUnreadNotificationWithGroupMetadata() {
        service.create(7, "小组成员完成了学习任务", "四级冲刺：本周任务，获得 2 积分",
                "/groups/10", "group_task_finished");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        Notification notification = captor.getValue();
        assertEquals(7, notification.getAccountId());
        assertEquals("group_task_finished", notification.getType());
        assertEquals("/groups/10", notification.getLink());
        assertFalse(notification.getReadStatus());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void countsOnlyUnreadNotificationsForCurrentAccount() {
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        assertEquals(3, service.countUnread(7));
        verify(mapper).selectCount(any(Wrapper.class));
    }
}
