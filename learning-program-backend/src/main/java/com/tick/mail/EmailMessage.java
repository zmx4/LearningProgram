package com.tick.mail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一封待发送的邮件。
 * <p>
 * 通过 RabbitMQ 投递时会被序列化成 JSON 字符串（用项目已有的 fastjson2，
 * 不额外引入 AMQP 的 Jackson 转换器，避免依赖 Jackson 2 / 3 的差异）。
 * <p>
 * {@code code} 单独带一份，是为了在 log 模式下能把验证码醒目地打出来，
 * 明文验证码**不会**出现在任何 HTTP 响应里。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {
    private String to;
    private String subject;
    private String content;
    /** 验证码明文；仅普通邮件（如通知）时为 null */
    private String code;
}
