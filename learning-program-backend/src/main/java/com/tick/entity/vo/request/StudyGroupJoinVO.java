package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 通过邀请码加入学习小组的请求。
 */
@Data
public class StudyGroupJoinVO {
    private String inviteCode;
}
