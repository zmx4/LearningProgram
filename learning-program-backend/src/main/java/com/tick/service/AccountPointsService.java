package com.tick.service;

public interface AccountPointsService {
    int getTotalPoints(Integer accountId);

    void addPoints(Integer accountId, int points);
}
