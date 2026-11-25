package com.tick;

import com.tick.controller.AdminController;
import com.tick.controller.StudyGroupController;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LearningProgramBackendApplicationTests {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void contextLoads() {
//        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }

    @Test
    void auditedControllersAreProxied() {
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(AdminController.class)));
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(StudyGroupController.class)));
    }

}
