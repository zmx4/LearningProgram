package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

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
    @TableField("register_time")
    Date registerDate;

    public Account(Integer id, String username, String password, String email,
                   String phone, String bio, String role, Date registerDate) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.role = role;
        this.registerDate = registerDate;
    }

    public Account(Integer id, String username, String password, String email, String role, Date registerDate) {
        this(id, username, password, email, null, null, role, registerDate);
    }
}
