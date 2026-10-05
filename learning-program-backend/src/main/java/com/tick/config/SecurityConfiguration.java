package com.tick.config;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.response.AuthorizeVO;
import com.tick.filter.JwtAuthorizeFilter;
import com.tick.service.AccountService;
import com.tick.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Spring Security 配置：/api/auth/** 放行匿名，/api/admin/** 限定 admin 角色，其余请求需认证；
 * 无状态会话，登录 / 登出使用自定义成功与失败处理器。
 */
@Configuration
public class SecurityConfiguration {

    @Resource
    JwtUtils jwtUtils;

    @Resource
    JwtAuthorizeFilter jwtAuthorizeFilter;

    @Resource
    AccountService accountService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(conf ->
                        conf
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/api/auth/**").permitAll()
                                // 未处理的错误会被转发到 /error。这里若要求认证，任何错误最终都会
                                // 变成 401「Full authentication is required to access this resource」，
                                // 前端会误判成登录过期并清除登录态，真正的错误信息也丢失了。
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("admin")
                                .anyRequest().authenticated())
                .formLogin(conf ->
                        conf.loginProcessingUrl("/api/auth/login")
                                .successHandler(this::onAuthenticationSuccess)
                                .failureHandler(this::onAuthenticationFailure)
                )
                .logout(conf ->
                        conf.logoutUrl("/api/auth/logout")
                                .logoutSuccessHandler(this::onLogoutSuccess))
                .exceptionHandling(conf -> conf
                        .authenticationEntryPoint(this::onUnauthorized)
                        .accessDeniedHandler(this::onAccessDeny))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(conf ->
                        conf.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    public void onAccessDeny(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.forbidden(accessDeniedException.getMessage()).asJsonString());
    }

    public void onUnauthorized(HttpServletRequest request,
                               @NonNull HttpServletResponse response,
                               @NonNull AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }

    public void onAuthenticationSuccess(HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json");
        User user = (User)authentication.getPrincipal();
        assert user != null;
        Account account = accountService.findAccountByUsernameOrEmail(user.getUsername());
        String token = jwtUtils.createJwt(user,account.getId(),account.getUsername());
        AuthorizeVO authorizeVO = new AuthorizeVO();
//        authorizeVO.setRole(account.getRole());
//        authorizeVO.setUsername(account.getUsername());
        BeanUtils.copyProperties(account,authorizeVO);
        authorizeVO.setExpireTime(jwtUtils.expireTime());
        authorizeVO.setToken(token);
        response.getWriter().write(RestBean.success(authorizeVO).asJsonString());
    }
    public void onAuthenticationFailure(HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }
    public void onLogoutSuccess(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @Nullable Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if(jwtUtils.invalidateToken(authorization)){
            writer.write(RestBean.success(new AuthorizeVO()).asJsonString());
        }else{
            writer.write(RestBean.failure(400,"退出登录失败").asJsonString());
        }

    }
}
