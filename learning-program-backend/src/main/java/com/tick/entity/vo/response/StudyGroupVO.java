package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习小组概要。role 为当前用户在组内的身份（owner / member）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupVO {
    private Integer id;
    private String name;
    private Integer ownerId;
    private String ownerName;
    private String inviteCode;
    private Integer memberCount;
    private String role;
    private LocalDateTime createdAt;
}
