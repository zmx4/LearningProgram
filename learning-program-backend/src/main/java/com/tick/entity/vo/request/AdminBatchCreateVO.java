package com.tick.entity.vo.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 管理员批量创建账号。允许部分成功，空字段按条返回失败原因，不做整体校验。
 */
public record AdminBatchCreateVO(
        @NotEmpty(message = "请提供要创建的账号列表") List<AccountItem> accounts
) {
    public record AccountItem(String username, String email, String password) {
    }
}
