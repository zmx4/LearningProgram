package com.tick.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件相关配置，对应 application.yaml 中的 learning.mail。
 * <p>
 * 默认是「不依赖任何外部服务也能跑」的组合：{@code delivery=log} 只把验证码写进日志，
 * {@code queue.enabled=false} 走同步发送。装好 SMTP 与 RabbitMQ 后改配置即可切换，
 * 不需要改代码。
 */
@Data
@ConfigurationProperties(prefix = "learning.mail")
public class MailProperties {

    /** 投递方式：log（只写日志，开发用）/ smtp（真实发信） */
    private String delivery = "log";

    /** 发件人地址 */
    private String from = "no-reply@learning.local";

    private Queue queue = new Queue();

    private Code code = new Code();

    /** RabbitMQ 队列相关配置 */
    @Data
    public static class Queue {
        /** 是否通过 RabbitMQ 异步投递；broker 不可用时投递失败会自动回退为直接发送 */
        private boolean enabled = false;
        private String exchange = "learning.mail.exchange";
        private String queueName = "learning.mail.queue";
        private String routingKey = "learning.mail.send";
    }

    /** 验证码相关配置 */
    @Data
    public static class Code {
        /** 验证码位数 */
        private int length = 6;
        /** 有效期（秒） */
        private long ttlSeconds = 300;
        /** 同一邮箱两次请求的最小间隔（秒） */
        private long intervalSeconds = 60;
    }
}
