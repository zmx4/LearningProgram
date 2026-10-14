package com.tick.entity.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单词测试成绩记录，仅作为 /api/tests/words 接口的出入参使用。
 * 持久化统一走 db_test_record（category = word），见 {@link TestRecord}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WordTestRecord extends BaseTestRecord {
    /**
     * 词表来源：cet4 / cet6。
     */
    private String source;
}
