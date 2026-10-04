package com.tick.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ConfirmRestVO;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<Account> , UserDetailsService {
    public Account findAccountByUsernameOrEmail(String text);
    String registerEmailAccount(EmailRegisterVO vo);
    String createAccount(String username, String email, String password);
    String changePassword(Integer accountId, ChangePasswordVO vo);
    String resetConfirm(ConfirmRestVO vo);
    String restEmailAccountPassword(EmailRegisterVO vo);
    Account updateProfile(Integer accountId, ProfileUpdateVO vo);
}
