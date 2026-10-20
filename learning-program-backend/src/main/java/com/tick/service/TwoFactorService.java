package com.tick.service;

import com.tick.entity.dto.Account;
import com.tick.entity.vo.response.TotpSetupVO;

public interface TwoFactorService {
    TotpSetupVO setup(Integer accountId, String emailCode);
    String enable(Integer accountId, int code);
    String disable(Integer accountId);
    boolean isEnabled(Account account);
    String createLoginChallenge(Integer accountId);
    Account verifyLoginChallenge(String challenge, int code);
}
