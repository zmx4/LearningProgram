package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.Notification;
import com.tick.service.AccountService;
import com.tick.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
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
    private static final Set<String> SUPPORTED_TARGETS = Set.of("all", "role", "users");

    private final AccountService accountService;
    private final NotificationService notificationService;

    public AdminController(AccountService accountService, NotificationService notificationService) {
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @GetMapping("/users")
    public RestBean<List<Map<String, Object>>> users() {
        List<Map<String, Object>> users = accountService.list().stream()
                .map(this::userView)
                .toList();
        return RestBean.success(users);
    }

    @PutMapping("/users/{id}/role")
    public RestBean<Void> updateRole(@PathVariable Integer id,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        String role = body.get("role");
        if (!Set.of("user", "admin").contains(role)) {
            return RestBean.failure(400, "角色只能是 user 或 admin");
        }
        Integer currentId = (Integer) request.getAttribute("id");
        if (currentId != null && currentId.equals(id) && !"admin".equals(role)) {
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
    public RestBean<Void> deleteUser(@PathVariable Integer id, HttpServletRequest request) {
        Integer currentId = (Integer) request.getAttribute("id");
        if (currentId != null && currentId.equals(id)) {
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
    public RestBean<Map<String, Object>> createUsers(@RequestBody BatchCreateRequest request) {
        List<BatchAccountItem> items = request.accounts();
        if (items == null || items.isEmpty()) {
            return RestBean.failure(400, "请提供要创建的账号列表");
        }
        List<BatchCreateError> errors = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < items.size(); i++) {
            BatchAccountItem item = items.get(i);
            String username = item.username() == null ? "" : item.username().trim();
            String email = item.email() == null ? "" : item.email().trim();
            String password = item.password() == null ? "" : item.password().trim();
            String error;
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                error = "用户名、邮箱和密码不能为空";
            } else {
                error = accountService.createAccount(username, email, password);
            }
            if (error != null) {
                errors.add(new BatchCreateError(i, error));
            } else {
                created++;
            }
        }
        return RestBean.success(Map.of(
                "createdCount", created,
                "failedCount", errors.size(),
                "errors", errors
        ));
    }

    @PostMapping("/notifications")
    public RestBean<Map<String, Integer>> sendNotification(
            @RequestBody AdminNotificationRequest request) {
        if (request.title() == null || request.title().isBlank()
                || request.content() == null || request.content().isBlank()
                || !SUPPORTED_TARGETS.contains(request.targetType())) {
            return RestBean.failure(400, "请填写标题、内容和有效的发送范围");
        }
        List<Account> recipients = switch (request.targetType()) {
            case "all" -> accountService.list();
            case "role" -> accountService.query().eq("role", request.targetRole()).list();
            case "users" -> request.userIds() == null || request.userIds().isEmpty()
                    ? List.of()
                    : accountService.listByIds(request.userIds());
            default -> List.of();
        };
        if (recipients.isEmpty()) {
            return RestBean.failure(400, "发送范围内没有用户");
        }
        List<Notification> notifications = recipients.stream()
                .map(account -> new Notification(
                        null, account.getId(), request.title().trim(), request.content().trim(), null,
                        request.type() == null || request.type().isBlank() ? "system" : request.type().trim(),
                        false, new Date()))
                .toList();
        notificationService.saveBatch(notifications);
        return RestBean.success(Map.of("sentCount", notifications.size()));
    }

    private Map<String, Object> userView(Account account) {
        return Map.of(
                "id", account.getId(),
                "username", account.getUsername(),
                "email", account.getEmail() == null ? "" : account.getEmail(),
                "phone", account.getPhone() == null ? "" : account.getPhone(),
                "role", account.getRole(),
                "registerDate", account.getRegisterDate()
        );
    }

    public record AdminNotificationRequest(
            String title,
            String content,
            String type,
            String targetType,
            String targetRole,
            List<Integer> userIds
    ) {
    }

    public record BatchCreateRequest(List<BatchAccountItem> accounts) {
    }

    public record BatchAccountItem(String username, String email, String password) {
    }

    public record BatchCreateError(int index, String message) {
    }
}
