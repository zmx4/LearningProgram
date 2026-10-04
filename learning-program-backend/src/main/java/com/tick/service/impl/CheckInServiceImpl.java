package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.CheckInRecord;
import com.tick.mapper.CheckInRecordMapper;
import com.tick.service.AccountPointsService;
import com.tick.service.CheckInService;
import com.tick.entity.vo.response.CheckInSummaryVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日签到服务实现。
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInRecordMapper, CheckInRecord>
        implements CheckInService {
    private final AccountPointsService accountPointsService;

    public CheckInServiceImpl(AccountPointsService accountPointsService) {
        this.accountPointsService = accountPointsService;
    }

    @Override
    public Map<String, Object> getStatus(Integer accountId, int year, int month) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = query()
                .select("checkin_date")
                .eq("account_id", accountId)
                .ge("checkin_date", LocalDate.of(year, month, 1))
                .lt("checkin_date", LocalDate.of(year, month, 1).plusMonths(1))
                .orderByAsc("checkin_date")
                .list()
                .stream().map(CheckInRecord::getCheckinDate).toList();
        List<CheckInSummaryVO> records = query()
                .eq("account_id", accountId)
                .ge("checkin_date", LocalDate.of(year, month, 1))
                .lt("checkin_date", LocalDate.of(year, month, 1).plusMonths(1))
                .orderByDesc("checkin_date")
                .list()
                .stream()
                .map(record -> new CheckInSummaryVO(
                        record.getCheckinDate(), record.getPoints(), record.getStreak()))
                .toList();
        CheckInRecord todayRecord = query().eq("account_id", accountId)
                .eq("checkin_date", today).last("LIMIT 1").one();
        Map<String, Object> status = new HashMap<>();
        status.put("dates", dates);
        status.put("todayChecked", todayRecord != null);
        status.put("streak", todayRecord == null ? currentStreak(accountId) : todayRecord.getStreak());
        status.put("points", accountPointsService.getTotalPoints(accountId));
        status.put("checkInCount", records.size());
        status.put("monthPoints", records.stream().mapToInt(CheckInSummaryVO::points).sum());
        status.put("records", records);
        return status;
    }

    @Override
    @Transactional
    public CheckInRecord checkIn(Integer accountId) {
        LocalDate today = LocalDate.now();
        CheckInRecord existing = query().eq("account_id", accountId)
                .eq("checkin_date", today).last("LIMIT 1").one();
        if (existing != null) return existing;

        CheckInRecord record = new CheckInRecord();
        record.setAccountId(accountId);
        record.setCheckinDate(today);
        int streak = currentStreak(accountId) + 1;
        record.setStreak(streak);
        record.setPoints(streak);
        try {
            save(record);
        } catch (DataIntegrityViolationException exception) {
            return query().eq("account_id", accountId)
                    .eq("checkin_date", today).last("LIMIT 1").one();
        }

        accountPointsService.addPoints(accountId, record.getPoints());
        return record;
    }

    private int currentStreak(Integer accountId) {
        CheckInRecord latest = query().eq("account_id", accountId)
                .orderByDesc("checkin_date").last("LIMIT 1").one();
        if (latest == null) return 0;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return latest.getCheckinDate().equals(LocalDate.now())
                || latest.getCheckinDate().equals(yesterday) ? latest.getStreak() : 0;
    }
}
