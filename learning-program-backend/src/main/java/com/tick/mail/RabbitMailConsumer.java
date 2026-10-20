package com.tick.mail;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * RabbitMQ 消费者：从队列里取出邮件并真正发送。
 * <p>
 * 只在 {@code learning.mail.queue.enabled=true} 时注册（{@link MailQueueConfiguration}），
 * 否则不会有监听容器，也就不会在没装 broker 时不断重连刷日志。
 */
public class RabbitMailConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMailConsumer.class);

    private final MailDelivery mailDelivery;

    public RabbitMailConsumer(MailDelivery mailDelivery) {
        this.mailDelivery = mailDelivery;
    }

    @RabbitListener(queues = "${learning.mail.queue.queue-name:learning.mail.queue}")
    public void onMessage(String payload) {
        EmailMessage message = JSON.parseObject(payload, EmailMessage.class);
        if (message == null || message.getTo() == null) {
            log.warn("收到无法解析的邮件消息，已丢弃：{}", payload);
            return;
        }
        mailDelivery.send(message);
    }
}
