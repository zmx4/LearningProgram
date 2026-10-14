package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习小组成员列表项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberVO {
    private Integer accountId;
    private String username;
    private String role;
    private LocalDateTime joinedAt;
}
