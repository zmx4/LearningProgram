package com.tick.service.impl;

import com.tick.mapper.AccountPointsMapper;
import com.tick.service.AccountPointsService;
import org.springframework.stereotype.Service;

@Service
public class AccountPointsServiceImpl implements AccountPointsService {
    private final AccountPointsMapper accountPointsMapper;

    public AccountPointsServiceImpl(AccountPointsMapper accountPointsMapper) {
        this.accountPointsMapper = accountPointsMapper;
    }

    @Override
    public int getTotalPoints(Integer accountId) {
        Integer points = accountPointsMapper.findTotalPoints(accountId);
        return points == null ? 0 : points;
    }

    @Override
    public void addPoints(Integer accountId, int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("积分必须为正数");
        }
        accountPointsMapper.addPoints(accountId, points);
    }
}
