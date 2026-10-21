package com.tick.entity.vo.response;

import com.tick.entity.dto.Account;

/**
 * 当前登录用户的个人资料。
 */
public record ProfileVO(String username, String email, String phone, String bio, String role) {
    public static ProfileVO from(Account account) {
        return new ProfileVO(
                account.getUsername(),
                account.getEmail() == null ? "" : account.getEmail(),
                account.getPhone() == null ? "" : account.getPhone(),
                account.getBio() == null ? "" : account.getBio(),
                account.getRole() == null ? "user" : account.getRole()
        );
    }
}
