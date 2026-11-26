package com.tick.service.impl.group;

import java.time.LocalDateTime;

/**
 * 单个任务项对某个成员的完成状态。
 */
public record GroupTaskItemState(boolean completed, LocalDateTime completedAt) {
}