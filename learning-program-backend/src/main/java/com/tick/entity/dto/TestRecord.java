package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一的测试成绩记录，对应 db_test_record，category 区分测试大类（见 {@link TestCategory}）。
 * <ul>
 *     <li>word：仅使用 source（cet4 / cet6）</li>
 *     <li>knowledge：使用 typeId/setId 与 typeName 快照，答题明细 JSON 存放在 detail 列，
 *     结构对应 {@link KnowledgeTestAnswer}</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("db_test_record")
public class TestRecord extends BaseTestRecord {
    private String category;
    @TableField("source")
    private String source;
    @TableField("type_id")
    private Integer typeId;
    @TableField("set_id")
    private Integer setId;
    @TableField("type_name")
    private String typeName;
    private String detail;
}
