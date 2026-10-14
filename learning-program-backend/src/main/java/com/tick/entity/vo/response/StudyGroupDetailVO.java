package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习小组详情：小组概要与成员列表，供小组界面展示右侧组员栏。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyGroupDetailVO {
    private StudyGroupVO group;
    private List<GroupMemberVO> members;
}
