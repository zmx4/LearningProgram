package com.tick.entity.vo.response;

import java.util.List;

/**
 * 批量建号结果：逐条记录失败下标与原因，前端据此展示部分成功情况。
 */
public record BatchCreateResultVO(int createdCount, int failedCount, List<BatchCreateError> errors) {
    public record BatchCreateError(int index, String message) {
    }
}
