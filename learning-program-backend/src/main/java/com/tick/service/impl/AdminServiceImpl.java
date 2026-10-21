package com.tick.service.impl;

import com.tick.entity.dto.Account;
import com.tick.entity.dto.Notification;
import com.tick.entity.vo.request.AdminBatchCreateVO;
import com.tick.entity.vo.request.AdminNotificationVO;
import com.tick.entity.vo.response.BatchCreateResultVO;
import com.tick.entity.vo.response.BatchCreateResultVO.BatchCreateError;
import com.tick.service.AccountService;
import com.tick.service.AdminService;
import com.tick.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 管理员批量操作实现。
 */
@Service
public class AdminServiceImpl implements AdminService {
    private static final Set<String> SUPPORTED_TARGETS = Set.of("all", "role", "users");

    private final AccountService accountService;
    private final NotificationService notificationService;

    public AdminServiceImpl(AccountService accountService, NotificationService notificationService) {
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @Override
    public BatchCreateResultVO createAccounts(AdminBatchCreateVO vo) {
        List<BatchCreateError> errors = new ArrayList<>();
        int created = 0;
        List<AdminBatchCreateVO.AccountItem> items = vo.accounts();
        for (int i = 0; i < items.size(); i++) {
            AdminBatchCreateVO.AccountItem item = items.get(i);
            String username = trim(item.username());
            String email = trim(item.email());
            String password = trim(item.password());
            String error = username.isEmpty() || email.isEmpty() || password.isEmpty()
                    ? "用户名、邮箱和密码不能为空"
                    : accountService.createAccount(username, email, password);
            if (error != null) {
                errors.add(new BatchCreateError(i, error));
            } else {
                created++;
            }
        }
        return new BatchCreateResultVO(created, errors.size(), errors);
    }

    @Override
    public int sendNotification(AdminNotificationVO vo) {
        if (!SUPPORTED_TARGETS.contains(vo.targetType())) {
            throw new IllegalArgumentException("不支持的发送范围");
        }
        List<Account> recipients = switch (vo.targetType()) {
            case "all" -> accountService.list();
            case "role" -> accountService.query().eq("role", vo.targetRole()).list();
            case "users" -> vo.userIds() == null || vo.userIds().isEmpty()
                    ? List.of()
                    : accountService.listByIds(vo.userIds());
            default -> List.of();
        };
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("发送范围内没有用户");
        }
        String type = vo.type() == null || vo.type().isBlank() ? "system" : vo.type().trim();
        List<Notification> notifications = recipients.stream()
                .map(account -> new Notification(
                        null, account.getId(), vo.title().trim(), vo.content().trim(), null,
                        type, false, new Date()))
                .toList();
        notificationService.saveBatch(notifications);
        return notifications.size();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
