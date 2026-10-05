package com.tick.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * 登录成功响应：账号 id、token、过期时间、用户名与角色。
 * 前端用 id 判断「这条内容是不是我发的」。
 */
@Data
public class AuthorizeVO  {
    Integer id;
    String username;
    String role;
    String token;
    Date expireTime;
}
