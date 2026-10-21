package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.AdminBatchCreateVO;
import com.tick.entity.vo.request.AdminNotificationVO;
import com.tick.entity.vo.response.AdminUserVO;
import com.tick.entity.vo.response.BatchCreateResultVO;
import com.tick.service.AccountService;
import com.tick.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理员平台接口：用户列表、角色调整、删除、批量创建、随机密码重置与定向通知发送。
 * 挂在 /api/admin 下，由 SecurityConfiguration 限定为 admin 角色。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AccountService accountService;
    private final AdminService adminService;

    public AdminController(AccountService accountService, AdminService adminService) {
        this.accountService = accountService;
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public RestBean<List<AdminUserVO>> users() {
        return RestBean.success(accountService.list().stream().map(AdminUserVO::from).toList());
    }

    @PutMapping("/users/{id}/role")
    public RestBean<Void> updateRole(@PathVariable Integer id,
                                     @RequestBody Map<String, String> body,
                                     @RequestAttribute("id") Integer currentId) {
        String role = body.get("role");
        if (!Set.of("user", "admin").contains(role)) {
            return RestBean.failure(400, "角色只能是 user 或 admin");
        }
        if (currentId.equals(id) && !"admin".equals(role)) {
            return RestBean.failure(400, "不能移除当前管理员的管理员权限");
        }
        Account account = accountService.getById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        account.setRole(role);
        accountService.updateById(account);
        return RestBean.success();
    }

    @DeleteMapping("/users/{id}")
    public RestBean<Void> deleteUser(@PathVariable Integer id,
                                     @RequestAttribute("id") Integer currentId) {
        if (currentId.equals(id)) {
            return RestBean.failure(400, "不能删除当前登录账号");
        }
        if (!accountService.removeById(id)) {
            return RestBean.failure(404, "用户不存在");
        }
        return RestBean.success();
    }

    @PutMapping("/users/{id}/password")
    public RestBean<Map<String, String>> resetPassword(@PathVariable Integer id) {
        Account account = accountService.getById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        String newPassword = accountService.resetPassword(id);
        if (newPassword == null) {
            return RestBean.failure(500, "内部错误,请联系管理员");
        }
        return RestBean.success(Map.of(
                "username", account.getUsername(),
                "password", newPassword
        ));
    }

    @PostMapping("/users/batch")
    public RestBean<BatchCreateResultVO> createUsers(@Valid @RequestBody AdminBatchCreateVO vo) {
        return RestBean.success(adminService.createAccounts(vo));
    }

    @PostMapping("/notifications")
    public RestBean<Map<String, Integer>> sendNotification(@Valid @RequestBody AdminNotificationVO vo) {
        return RestBean.success(Map.of("sentCount", adminService.sendNotification(vo)));
    }
}
