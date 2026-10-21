package com.tick.exception;

/**
 * 请求与当前数据状态冲突（如邮箱已被占用），由全局处理器转换为 409。
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
