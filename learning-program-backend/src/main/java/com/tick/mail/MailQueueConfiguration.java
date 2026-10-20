package com.tick.mail;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 相关装配，**只在 {@code learning.mail.queue.enabled=true} 时生效**。
 * <p>
 * 这样默认配置（未安装 broker）下不会创建监听容器，应用照常启动；
 * 把开关打开后才有交换机、队列、生产者与消费者。
 */
@Configuration
@ConditionalOnProperty(prefix = "learning.mail.queue", name = "enabled", havingValue = "true")
public class MailQueueConfiguration {

    @Bean
    public TopicExchange mailExchange(MailProperties properties) {
        return new TopicExchange(properties.getQueue().getExchange(), true, false);
    }

    @Bean
    public Queue mailQueue(MailProperties properties) {
        return new Queue(properties.getQueue().getQueueName(), true);
    }

    @Bean
    public Binding mailBinding(TopicExchange mailExchange, Queue mailQueue, MailProperties properties) {
        return BindingBuilder.bind(mailQueue).to(mailExchange).with(properties.getQueue().getRoutingKey());
    }

    @Bean
    public RabbitMailProducer rabbitMailProducer(RabbitTemplate rabbitTemplate, MailProperties properties) {
        return new RabbitMailProducer(rabbitTemplate, properties);
    }

    @Bean
    public RabbitMailConsumer rabbitMailConsumer(MailDelivery mailDelivery) {
        return new RabbitMailConsumer(mailDelivery);
    }
}
