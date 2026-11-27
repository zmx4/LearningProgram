package com.tick.event;

/**
 * 学习活动事件，在写事务提交后触发小组任务结算。
 */
public record GroupLearningActivityEvent(Integer accountId) {
}
