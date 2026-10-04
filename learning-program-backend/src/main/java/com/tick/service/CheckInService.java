package com.tick.service;

import com.tick.entity.dto.CheckInRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日签到服务：签到日历查询与签到发放积分。
 */
public interface CheckInService {
    /**
     * 查询指定月份的签到状态：签到日期、连签天数、当月积分与签到次数等。
     */
    Map<String, Object> getStatus(Integer accountId, int year, int month);

    /**
     * 当日签到：幂等（重复签到返回已有记录）；按连签天数发放积分（连签第 N 天得 N 分）。
     */
    CheckInRecord checkIn(Integer accountId);
}
