package com.tick.utils;

/**
 * 全局常量：过滤器顺序与 Redis 键前缀等。
 */
public class Const {
    public static final String JWT_BLACK_LIST = "jwt::blacklist:";


    public static final int ORDER_LIMIT = -101;
    public static final int ORDER_CORS = -102;

    public static final String FLOW_LIMIT_COUNTER = "flow::counter:";
    public static final String FLOW_LIMIT_BLOCK = "flow::block:";

    /** 邮箱验证码：email::code:<type>:<email> */
    public static final String EMAIL_CODE = "email::code:";
    /** 同一邮箱的发送频控：email::code::interval:<type>:<email> */
    public static final String EMAIL_CODE_INTERVAL = "email::code::interval:";

}
