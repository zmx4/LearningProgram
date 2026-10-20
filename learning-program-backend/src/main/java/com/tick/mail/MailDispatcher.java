package com.tick.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 邮件投递入口：优先走 RabbitMQ 异步发送，失败或不启用队列时直接发送。
 * <p>
 * 回退是刻意设计的：broker 没装、连不上、或生产环境临时挂了，
 * 用户依然应该能收到验证码，而不是让「发送验证码」这个操作整体失败。
 */
@Service
public class MailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);

    private final MailDelivery mailDelivery;
    private final MailProperties properties;
    private final ObjectProvider<RabbitMailProducer> producerProvider;

    public MailDispatcher(MailDelivery mailDelivery,
                          MailProperties properties,
                          ObjectProvider<RabbitMailProducer> producerProvider) {
        this.mailDelivery = mailDelivery;
        this.properties = properties;
        this.producerProvider = producerProvider;
    }

    /** 是否配置为走队列（用于日志与自检提示）。 */
    public boolean queueEnabled() {
        return properties.getQueue().isEnabled() && producerProvider.getIfAvailable() != null;
    }

    public void dispatch(EmailMessage message) {
        if (queueEnabled()) {
            try {
                producerProvider.getObject().publish(message);
                return;
            } catch (Exception exception) {
                // 连不上 broker 是常见情况（本地没装），降级为直接发送
                log.warn("投递到 RabbitMQ 失败，改为直接发送：to={} 原因={}",
                        message.getTo(), exception.getMessage());
            }
        }
        mailDelivery.send(message);
    }
}
