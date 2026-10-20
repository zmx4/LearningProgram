package com.tick.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投递策略：优先队列、失败回退。这是「没装 broker 也能用」的关键。
 */
class MailDispatcherTest {

    private final MailDelivery delivery = mock(MailDelivery.class);
    private final RabbitMailProducer producer = mock(RabbitMailProducer.class);
    private final ObjectProvider<RabbitMailProducer> producerProvider = mock(ObjectProvider.class);
    private final MailProperties properties = new MailProperties();

    private MailDispatcher dispatcher() {
        return new MailDispatcher(delivery, properties, producerProvider);
    }

    private EmailMessage message() {
        return new EmailMessage("user@example.test", "主题", "正文", "123456");
    }

    @Test
    void sendsDirectlyWhenTheQueueIsDisabled() {
        properties.getQueue().setEnabled(false);
        when(producerProvider.getIfAvailable()).thenReturn(producer);

        MailDispatcher dispatcher = dispatcher();
        assertFalse(dispatcher.queueEnabled());

        dispatcher.dispatch(message());

        verify(delivery).send(any());
        verify(producer, never()).publish(any());
    }

    @Test
    void publishesToRabbitWhenTheQueueIsEnabled() {
        properties.getQueue().setEnabled(true);
        when(producerProvider.getIfAvailable()).thenReturn(producer);
        when(producerProvider.getObject()).thenReturn(producer);

        MailDispatcher dispatcher = dispatcher();
        assertTrue(dispatcher.queueEnabled());

        dispatcher.dispatch(message());

        verify(producer).publish(any());
        verify(delivery, never()).send(any());
    }

    @Test
    void fallsBackToDirectSendingWhenTheBrokerIsUnreachable() {
        properties.getQueue().setEnabled(true);
        when(producerProvider.getIfAvailable()).thenReturn(producer);
        when(producerProvider.getObject()).thenReturn(producer);
        // 模拟 broker 连不上
        doThrow(new RuntimeException("Connection refused")).when(producer).publish(any());

        dispatcher().dispatch(message());

        // 异常被吞掉，邮件仍然要发出去，不能因为 broker 挂了就丢验证码
        verify(delivery).send(any());
    }

    @Test
    void enabledQueueWithoutProducerBeanAlsoFallsBack() {
        properties.getQueue().setEnabled(true);
        when(producerProvider.getIfAvailable()).thenReturn(null);

        MailDispatcher dispatcher = dispatcher();
        assertFalse(dispatcher.queueEnabled());

        dispatcher.dispatch(message());

        verify(delivery).send(any());
    }
}
