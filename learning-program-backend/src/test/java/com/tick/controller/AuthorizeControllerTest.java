package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.service.AccountService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizeControllerTest {

    private final AccountService accountService = mock(AccountService.class);
    private final AuthorizeController controller = createController();

    @Test
    void registerReturnsSuccessWhenServiceAcceptsAccount() {
        EmailRegisterVO request = validRequest();
        when(accountService.registerEmailAccount(request)).thenReturn(null);

        RestBean<Void> response = controller.register(request);

        assertEquals(200, response.code());
        assertNull(response.data());
        assertEquals("success", response.message());
    }

    @Test
    void registerReturnsBadRequestWhenServiceRejectsAccount() {
        EmailRegisterVO request = validRequest();
        when(accountService.registerEmailAccount(request)).thenReturn("此email已被其他用户注册.");

        RestBean<Void> response = controller.register(request);

        assertEquals(400, response.code());
        assertNull(response.data());
        assertEquals("此email已被其他用户注册.", response.message());
    }

    private AuthorizeController createController() {
        AuthorizeController controller = new AuthorizeController();
        controller.accountService = accountService;
        return controller;
    }

    private EmailRegisterVO validRequest() {
        EmailRegisterVO request = new EmailRegisterVO();
        request.setEmail("tick@example.com");
        request.setUsername("tick");
        request.setPassword("123456");
        request.setCode("123456");
        return request;
    }
}