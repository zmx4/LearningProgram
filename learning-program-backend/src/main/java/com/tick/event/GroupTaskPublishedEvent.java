package com.tick.event;

/**
 * 小组任务发布事件，在写事务提交后触发现有成员结算。
 */
public record GroupTaskPublishedEvent(Integer groupId, Integer taskId) {
}
