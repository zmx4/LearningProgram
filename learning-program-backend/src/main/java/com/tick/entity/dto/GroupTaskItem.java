package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 小组学习任务的绑定项，对应 db_group_task_item。
 * 任务本体是 db_study_group_message 中 type = task 的消息，本表通过 message_id 挂接。
 * item_type：course 完成课程（自动判定）、resource 访问资源（自动判定）、
 * member_visit 组员访问小组（自动判定）、text 纯文本（组员手动标记）。
 */
@Data
@TableName("db_group_task_item")
public class GroupTaskItem {
    public static final String TYPE_COURSE = "course";
    public static final String TYPE_RESOURCE = "resource";
    public static final String TYPE_MEMBER_VISIT = "member_visit";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_WORD_TEST = "word_test";
    public static final String TYPE_QUESTION_SET = "question_set";

    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("group_id")
    private Integer groupId;
    @TableField("message_id")
    private Integer messageId;
    @TableField("item_type")
    private String itemType;
    /** item_type 为 course / resource / question_set 时的关联对象 id，其余类型为 null */
    @TableField("ref_id")
    private Integer refId;
    /** 单词测试词表来源：cet4 / cet6 */
    @TableField("source")
    private String source;
    /** 测试任务项及格分（0-100） */
    @TableField("pass_score")
    private Integer passScore;
    private String title;
    @TableField("sort_order")
    private Integer sortOrder;
}
