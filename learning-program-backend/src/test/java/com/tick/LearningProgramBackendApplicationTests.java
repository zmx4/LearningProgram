package com.tick;

import com.tick.controller.AdminController;
import com.tick.controller.StudyGroupController;
import com.tick.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LearningProgramBackendApplicationTests {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    AccountService accountService;

    @Test
    void contextLoads() {
//        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }

    @Test
    void auditedControllersAreProxied() {
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(AdminController.class)));
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(StudyGroupController.class)));
    }

    @Test
    @WithMockUser(roles = "user")
    void accountAdministrationMethodsRejectOrdinaryUsers() {
        assertThrows(AccessDeniedException.class,
                () -> accountService.createAccount("test", "test@example.com", "password"));
        assertThrows(AccessDeniedException.class, () -> accountService.resetPassword(999));
    }

}
