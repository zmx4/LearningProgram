package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户账号，对应 db_account。role 取值 user / admin；
 * points 为早期遗留字段，有效积分以 {@link AccountPoints} 为准。
 */
@Data
@TableName("db_account")
public class Account {
    @TableId(type = IdType.AUTO)
    Integer id;
    String username;
    String password;
    String email;
    String phone;
    String bio;
    String role;
    Integer points;
    @TableField("register_time")
    Date registerDate;

    public Account(Integer id, String username, String password, String email,
                   String phone, String bio, String role, Date registerDate) {
        this(id, username, password, email, phone, bio, role, 0, registerDate);
    }

    public Account(Integer id, String username, String password, String email,
                   String phone, String bio, String role, Integer points, Date registerDate) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.role = role;
        this.points = points;
        this.registerDate = registerDate;
    }

    public Account(Integer id, String username, String password, String email, String role, Date registerDate) {
        this(id, username, password, email, null, null, role, registerDate);
    }
}
