package com.tick.entity.vo.response;

import java.time.LocalDate;

public record CheckInSummaryVO(
        LocalDate date,
        Integer points,
        Integer streak
) {
}
