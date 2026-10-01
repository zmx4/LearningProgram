package com.tick.service;

import com.tick.entity.vo.request.KnowledgeTestResultVO;
import com.tick.entity.vo.response.KnowledgeTestRecordVO;

import java.util.List;
import java.util.Map;

public interface KnowledgeTestService {
    KnowledgeTestRecordVO saveResult(Integer accountId, KnowledgeTestResultVO vo);

    Map<String, Object> getSummary(Integer accountId);

    List<KnowledgeTestRecordVO> getHistory(Integer accountId);
}
