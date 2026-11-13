package com.tick.entity.vo.response;

import com.tick.entity.dto.Account;

/**
 * 用户主页公开信息，不含邮箱等隐私字段。avatar 为头像托管文件名，
 * 未设置或未启用对象存储时为 null。
 */
public record UserProfileVO(String username, String bio, String avatar) {
    public static UserProfileVO from(Account account, boolean avatarEnabled) {
        return new UserProfileVO(
                account.getUsername(),
                account.getBio() == null ? "" : account.getBio(),
                avatarEnabled ? account.getAvatar() : null
        );
    }
}