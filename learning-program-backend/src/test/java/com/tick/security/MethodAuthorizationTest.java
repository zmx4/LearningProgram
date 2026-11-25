package com.tick.security;

import com.tick.controller.AdminController;
import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.AdminBatchCreateVO;
import com.tick.entity.vo.response.BatchCreateResultVO;
import com.tick.service.AccountService;
import com.tick.service.AdminService;
import com.tick.service.NotificationService;
import com.tick.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(MethodAuthorizationTest.Config.class)
class MethodAuthorizationTest {

    @Autowired
    private AdminController adminController;

    @Autowired
    private AdminService adminService;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void resetMocks() {
        reset(accountService);
    }

    @Test
    @WithMockUser(roles = "admin")
    void administratorCanInvokeProtectedControllerMethods() {
        Account account = mock(Account.class);
        when(account.getUsername()).thenReturn("target-user");
        when(accountService.getById(42)).thenReturn(account);
        when(accountService.resetPassword(42)).thenReturn("generated-password");

        RestBean<Map<String, String>> response = adminController.resetPassword(42, 7);

        assertEquals(200, response.code());
        verify(accountService).resetPassword(42);
    }

    @Test
    @WithMockUser(roles = "user")
    void ordinaryUserCannotInvokeProtectedControllerMethods() {
        assertThrows(AccessDeniedException.class, () -> adminController.resetPassword(42, 2));

        verifyNoInteractions(accountService);
    }

    @Test
    @WithAnonymousUser
    void anonymousUserCannotInvokeProtectedControllerMethods() {
        assertThrows(AccessDeniedException.class, () -> adminController.resetPassword(42, null));

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(roles = "admin")
    void administratorCanInvokeProtectedServiceMethods() {
        BatchCreateResultVO response = adminService.createAccounts(new AdminBatchCreateVO(List.of()));

        assertEquals(0, response.createdCount());
        assertTrue(AopUtils.isAopProxy(adminService));
    }

    @Test
    @WithMockUser(roles = "user")
    void ordinaryUserCannotInvokeProtectedServiceMethods() {
        assertThrows(AccessDeniedException.class,
                () -> adminService.createAccounts(new AdminBatchCreateVO(List.of())));

        verifyNoInteractions(accountService);
    }

    @Test
    void methodSecurityCreatesControllerAndServiceProxies() {
        assertTrue(AopUtils.isAopProxy(adminController));
        assertTrue(AopUtils.isAopProxy(adminService));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }

        @Bean
        AdminService adminService(AccountService accountService, NotificationService notificationService) {
            return new AdminServiceImpl(accountService, notificationService);
        }

        @Bean
        AdminController adminController(AccountService accountService, AdminService adminService) {
            return new AdminController(accountService, adminService);
        }
    }
}
