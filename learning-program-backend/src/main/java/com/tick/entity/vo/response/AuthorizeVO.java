package com.tick.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 登录成功响应：token、过期时间、用户名与角色。
 */
@Data
public class AuthorizeVO  {
    String username;
    String role;
    String token;
    Date expireTime;
}
