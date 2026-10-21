package com.tick.exception;

/**
 * 请求的实体不存在或对当前用户不可见，由全局处理器转换为 404。
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
