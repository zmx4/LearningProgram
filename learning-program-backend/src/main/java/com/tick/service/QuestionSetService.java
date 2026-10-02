package com.tick.service;

import com.tick.entity.vo.request.QuestionSetSaveVO;
import com.tick.entity.vo.response.QuestionSetDetailVO;
import com.tick.entity.vo.response.QuestionSetVO;

import java.util.List;

public interface QuestionSetService {
    List<QuestionSetVO> listSets();

    QuestionSetDetailVO getSetDetail(Integer id);

    QuestionSetVO createSet(QuestionSetSaveVO vo);

    QuestionSetVO updateSet(Integer id, QuestionSetSaveVO vo);

    void deleteSet(Integer id);
}
