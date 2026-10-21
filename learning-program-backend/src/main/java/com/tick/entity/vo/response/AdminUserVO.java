package com.tick.entity.vo.response;

import com.tick.entity.dto.Account;

import java.util.Date;

/**
 * 管理员用户列表项。
 */
public record AdminUserVO(Integer id, String username, String email, String phone, String role, Date registerDate) {
    public static AdminUserVO from(Account account) {
        return new AdminUserVO(
                account.getId(),
                account.getUsername(),
                account.getEmail() == null ? "" : account.getEmail(),
                account.getPhone() == null ? "" : account.getPhone(),
                account.getRole(),
                account.getRegisterDate()
        );
    }
}
