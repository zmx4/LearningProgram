package com.tick.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ConfirmRestVO;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 账号服务：注册、资料与密码管理，同时作为 Spring Security 的 UserDetailsService 加载登录用户。
 * 除特殊说明外，返回 String 的方法约定为「成功返回 null，失败返回用户可读的错误信息」。
 */
public interface AccountService extends IService<Account> , UserDetailsService {
    /**
     * 按用户名或邮箱查找账号（登录加载用户详情时使用），不存在返回 null。
     */
    public Account findAccountByUsernameOrEmail(String text);

    /**
     * 注册账号：校验邮箱唯一后加密落库并发送欢迎通知。
     * 注意：spring.security.email 开启时本方法不落库、直接返回 null，实际注册由验证码流程完成。
     */
    String registerEmailAccount(EmailRegisterVO vo);

    /**
     * 直接创建账号（批量添加）：校验用户名与邮箱唯一，密码加密落库并发送欢迎通知。
     */
    String createAccount(String username, String email, String password);

    /**
     * 已登录用户修改密码：校验旧密码正确、新密码不得与当前相同，且需满足 {@link com.tick.security.password.PasswordPolicy}。
     */
    String changePassword(Integer accountId, ChangePasswordVO vo);

    /**
     * 管理员重置密码：按 {@link com.tick.security.password.PasswordPolicy} 生成随机密码并加密落库，
     * 给用户发送重置通知，返回新密码明文（仅经重置接口返回给管理员展示，只出现这一次）；
     * 账号不存在或更新失败返回 null。
     */
    String resetPassword(Integer accountId);

    /**
     * 重置密码前校验邮箱验证码；spring.security.email 关闭时视为直接通过。
     */
    String resetConfirm(ConfirmRestVO vo);

    /**
     * 验证码通过后按邮箱重置密码：新密码需满足 {@link com.tick.security.password.PasswordPolicy}，
     * 更新成功后清除验证码缓存。验证码有误、密码不合规或邮箱未注册时返回对应提示，成功返回 null。
     */
    String restEmailAccountPassword(EmailRegisterVO vo);

    /**
     * 更新个人资料（昵称、邮箱、手机号、简介），邮箱被其他用户占用时抛 {@link IllegalArgumentException}；
     * 账号不存在返回 null。
     */
    Account updateProfile(Integer accountId, ProfileUpdateVO vo);

}
