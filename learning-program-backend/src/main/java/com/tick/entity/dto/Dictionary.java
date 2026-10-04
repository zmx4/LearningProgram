package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 词典词条，对应 db_dictionary。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_dictionary")
public class Dictionary {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String word;
    private String translation;
}
