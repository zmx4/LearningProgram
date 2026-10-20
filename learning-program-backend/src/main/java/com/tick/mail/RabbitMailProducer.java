package com.tick.mail;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ 生产者：把待发邮件以 JSON 字符串投递到交换机。
 * <p>
 * 只在 {@code learning.mail.queue.enabled=true} 时被创建（见 {@link MailQueueConfiguration}），
 * 因此默认配置下不会去连 broker。
 */
public class RabbitMailProducer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMailProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final MailProperties properties;

    public RabbitMailProducer(RabbitTemplate rabbitTemplate, MailProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * 投递邮件消息。
     *
     * @throws RuntimeException broker 不可用等异常，由调用方决定是否回退
     */
    public void publish(EmailMessage message) {
        MailProperties.Queue queue = properties.getQueue();
        rabbitTemplate.convertAndSend(queue.getExchange(), queue.getRoutingKey(), JSON.toJSONString(message));
        log.debug("邮件已投递到 RabbitMQ：to={} exchange={} routingKey={}",
                message.getTo(), queue.getExchange(), queue.getRoutingKey());
    }
}
