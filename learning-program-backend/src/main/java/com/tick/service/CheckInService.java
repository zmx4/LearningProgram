package com.tick.service;

import com.tick.entity.dto.CheckInRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CheckInService {
    Map<String, Object> getStatus(Integer accountId, int year, int month);
    CheckInRecord checkIn(Integer accountId);
}
