package com.tick.entity.vo.response;

import java.time.LocalDate;

/**
 * 单日签到汇总：日期、获得积分与连签天数。
 */
public record CheckInSummaryVO(
        LocalDate date,
        Integer points,
        Integer streak
) {
}
