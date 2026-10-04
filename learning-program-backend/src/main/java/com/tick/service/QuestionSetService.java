package com.tick.service;

import com.tick.entity.vo.request.QuestionSetSaveVO;
import com.tick.entity.vo.response.QuestionSetDetailVO;
import com.tick.entity.vo.response.QuestionSetVO;

import java.util.List;

/**
 * 题集服务：登录端读取与管理端 CRUD。
 */
public interface QuestionSetService {
    /**
     * 题集列表（含题目数）。
     */
    List<QuestionSetVO> listSets();

    /**
     * 题集详情（含题目列表），题集不存在返回 null。
     */
    QuestionSetDetailVO getSetDetail(Integer id);

    /**
     * 创建题集并关联题目；questionIds 为题集内题目顺序。
     */
    QuestionSetVO createSet(QuestionSetSaveVO vo);

    /**
     * 更新题集并全量覆盖题目关联。
     */
    QuestionSetVO updateSet(Integer id, QuestionSetSaveVO vo);

    /**
     * 删除题集，题目本身不受影响。
     */
    void deleteSet(Integer id);
}
