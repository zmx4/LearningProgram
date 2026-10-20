# 两步验证部署说明

数据库新安装可直接使用 `scripts/learning.sql` 中的 `db_account` 定义。已有数据库需要先执行：

```sql
ALTER TABLE db_account
  ADD COLUMN totp_secret varchar(64) DEFAULT NULL,
  ADD COLUMN totp_enabled tinyint(1) NOT NULL DEFAULT 0;
```

用户在「设置」中生成密钥后，将密钥添加到 Google Authenticator、Microsoft Authenticator
或其他兼容 TOTP 的验证器应用，并输入应用生成的 6 位验证码确认启用。

当 `spring.security.email.enable=true` 时，启用流程还必须先通过账号邮箱验证码；
验证码类型为 `2fa`，仍使用现有的 `/api/auth/ask-code` 接口。登录挑战和邮箱验证码均有有效期，
服务端不会返回或保存明文 TOTP 验证码。
