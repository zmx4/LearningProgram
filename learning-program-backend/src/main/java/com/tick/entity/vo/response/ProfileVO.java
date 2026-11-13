package com.tick.entity.vo.response;

import com.tick.entity.dto.Account;

/**
 * 当前登录用户的个人资料。avatar 为头像托管文件名（未设置时为 null），
 * avatarEnabled 表示头像功能是否可用（仅启用对象存储时为 true）。
 */
public record ProfileVO(String username, String email, String phone, String bio, String role,
                        String avatar, boolean avatarEnabled) {
    public static ProfileVO from(Account account, boolean avatarEnabled) {
        return new ProfileVO(
                account.getUsername(),
                account.getEmail() == null ? "" : account.getEmail(),
                account.getPhone() == null ? "" : account.getPhone(),
                account.getBio() == null ? "" : account.getBio(),
                account.getRole() == null ? "user" : account.getRole(),
                avatarEnabled ? account.getAvatar() : null,
                avatarEnabled
        );
    }
}