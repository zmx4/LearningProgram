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

}
