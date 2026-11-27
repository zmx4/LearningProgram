package com.tick.service;

import com.tick.entity.vo.response.GroupOverviewVO;

/**
 * 首页小组概览：一次返回小组卡片与当前用户待办。
 */
public interface GroupOverviewService {
    GroupOverviewVO getOverview(Integer accountId);
}
