package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.TotpLoginVO;
import com.tick.config.SecurityConfiguration;
import com.tick.service.TwoFactorService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorLoginController {
    private final TwoFactorService twoFactorService;
    private final SecurityConfiguration securityConfiguration;

    public TwoFactorLoginController(TwoFactorService twoFactorService, SecurityConfiguration securityConfiguration) {
        this.twoFactorService = twoFactorService;
        this.securityConfiguration = securityConfiguration;
    }

    @PostMapping("/login")
    public void login(@Valid @RequestBody TotpLoginVO vo, HttpServletResponse response) throws IOException {
        try {
            Account account = twoFactorService.verifyLoginChallenge(vo.getChallenge(), Integer.parseInt(vo.getCode()));
            if (account == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write(RestBean.unauthorized("两步验证码错误或登录挑战已过期").asJsonString());
                return;
            }
            response.setContentType("application/json;charset=utf-8");
            securityConfiguration.writeAuthorization(response, account);
        } catch (NumberFormatException exception) {
            response.setStatus(400);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(RestBean.failure(400, "验证码格式不正确").asJsonString());
        }
    }
}
