package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.Notification;
import com.tick.service.AccountService;
import com.tick.service.NotificationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Set<String> SUPPORTED_TARGETS = Set.of("all", "role", "users");

    @Resource
    private AccountService accountService;

    @Resource
    private NotificationService notificationService;

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
                        null, account.getId(), request.title().trim(), request.content().trim(),
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
}
