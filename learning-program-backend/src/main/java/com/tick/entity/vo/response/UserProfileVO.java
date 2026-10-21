package com.tick.entity.vo.response;

import com.tick.entity.dto.Account;

/**
 * 用户主页公开信息，不含邮箱等隐私字段。
 */
public record UserProfileVO(String username, String bio) {
    public static UserProfileVO from(Account account) {
        return new UserProfileVO(
                account.getUsername(),
                account.getBio() == null ? "" : account.getBio()
        );
    }
}
