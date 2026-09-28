package com.tick.config;

import com.alibaba.fastjson2.JSONObject;
import com.tick.entity.dto.Account;
import com.tick.service.AccountService;
import com.tick.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigurationTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final AccountService accountService = mock(AccountService.class);
    private final SecurityConfiguration configuration = createConfiguration();

    @Test
    void authenticationSuccessReturnsAccountAndToken() throws Exception {
        UserDetails user = User.withUsername("tick").password("{noop}123456").roles("user").build();
        Authentication authentication = mock(Authentication.class);
        Account account = new Account(1, "tick", "encoded", "tick@example.com", "user", new Date());
        Date expireTime = new Date(1_800_000_000_000L);
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authentication.getPrincipal()).thenReturn(user);
        when(accountService.findAccountByUsernameOrEmail("tick")).thenReturn(account);
        when(jwtUtils.createJwt(user, 1, "tick")).thenReturn("jwt-token");
        when(jwtUtils.expireTime()).thenReturn(expireTime);

        configuration.onAuthenticationSuccess(request, response, authentication);

        assertEquals("application/json", response.getContentType());
        JSONObject result = JSONObject.parseObject(response.getContentAsString());
        assertEquals(200, result.getIntValue("code"));
        assertEquals("success", result.getString("message"));
        JSONObject data = result.getJSONObject("data");
        assertEquals("tick", data.getString("username"));
        assertEquals("user", data.getString("role"));
        assertEquals("jwt-token", data.getString("token"));
        assertEquals(expireTime.getTime(), data.getDate("expireTime").getTime());
    }

    @Test
    void authenticationFailureReturnsUnauthorizedResponse() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        configuration.onAuthenticationFailure(request, response, new BadCredentialsException("用户名或密码错误"));

        assertEquals("application/json", response.getContentType());
        assertEquals("{\"code\":401,\"data\":null,\"message\":\"用户名或密码错误\"}", response.getContentAsString());
    }

    private SecurityConfiguration createConfiguration() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        configuration.jwtUtils = jwtUtils;
        configuration.accountService = accountService;
        return configuration;
    }
}