# 流程图证据说明

本文件记录 [README.md](./README.md) 中 10 个视图（00 图例 + 01–09 流程）的证据来源、
置信度、配置前提与已知限制。所有结论对应仓库当前代码状态，整理日期 2026-10-26。

## 1. 方法与置信度

| 等级 | 含义 | 在本套图中的使用 |
| --- | --- | --- |
| 高 | 控制器 / 服务实现 / 过滤器 / 前端页面可直接读出，无需推断 | 所有节点与连线 |
| 中 | 由配置默认值或多个文件组合得出，代码路径存在但默认不一定走到 | 配置开关类 Note、降级分支 |
| 低 | 仅有间接线索或运行时假设 | 未使用 |

生成脚本对每个页面强制要求 `evidence` 列表，节点 ID 唯一、边端点存在、节点包围盒不重叠，
否则生成失败。Draw.io 与 Mermaid 中的图形只表达流程；置信度只在本文件与页脚 Note 中维护。

## 2. 全局前提

### 2.1 认证与游客边界

- 除以下路径外，所有接口都要求认证（`SecurityConfiguration.java:53-69`）：
  - `/api/auth/**`、`/api/2fa/**`、`/error` 匿名放行；
  - `GET /api/courses/progress` 与 `GET /api/dictionary/**` 匿名只读；
  - `/api/admin/**` 需要 `admin` 角色；
  - 其余 `anyRequest().authenticated()`。
- 会话为无状态 JWT。`JwtAuthorizeFilter` 解析 Bearer Token 后写入 `SecurityContext`
  与 `request.id`，控制器用 `@RequestAttribute("id")` 取当前用户
  （`JwtAuthorizeFilter.java:32-49`）。
- 前端路由守卫在后端规则之上再做页面级拦截；`/admin` 为 `adminOnly`
  （`router/index.ts:153-188, 218-245`）。

### 2.2 影响流程走向的默认配置

| 配置 | 默认值 | 流程影响 | 证据 |
| --- | --- | --- | --- |
| `spring.security.email.enable` | `false` | 关闭时注册与找回密码**跳过**验证码校验；开启时才 `verifyAndConsume` / `verify + clear` | `application.yaml:6-7`；`AccountServiceImpl.java:38-39, 115-144, 205-236` |
| `learning.mail.delivery` | `log` | 验证码只写服务端日志，不连 SMTP | `application.yaml:51-55` |
| `learning.mail.queue.enabled` | `false` | 不注册 RabbitMQ 生产者/消费者，直接走 `MailDelivery` | `application.yaml:56-61`；`MailQueueConfiguration` |
| `learning.mail.code.*` | 6 位 / 300s / 60s | 验证码长度、有效期与频控窗口 | `application.yaml:62-67`；`EmailCodeService.java:48-103` |

因此 01、09 中的“验证码校验”“RabbitMQ 投递”都是**代码可达分支**，默认开发配置下会走
跳过校验与日志投递；图中保留完整分支用于说明开启配置后的行为。

## 3. 分页证据

### 01 注册与找回密码

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 获取验证码前按用途校验邮箱：注册要求未注册，找回要求已注册 | `AuthorizeController.java:42-90`；`AccountServiceImpl.java:60-88` | 高 |
| 同一邮箱 60 秒内只能发一次，验证码 6 位、Redis TTL 300 秒 | `EmailCodeService.java:48-103`；`application.yaml:62-67` | 高 |
| 注册流程：邮箱已存在检查 → （开关开启时）校验并消费验证码 → 密码策略 → BCrypt → 写 `account` → 写欢迎通知 | `AccountServiceImpl.java:115-144` | 高 |
| 邮箱验证开关关闭时，`verifyAndConsume` 整段被跳过 | `AccountServiceImpl.java:38-39, 119-125`；`application.yaml:6-7` | 高 |
| 找回密码两步：`reset-confirm` 只校验不消费；`reset-password` 再校验、按邮箱改密、成功后清除验证码 | `AccountServiceImpl.java:205-236` | 高 |
| 前端注册与找回均为“先取码、再提交”的两步表单 | `RegisterPage.vue:160-201`；`ForgetPage.vue:156-196` | 高 |
| 认证接口匿名放行 | `SecurityConfiguration.java:58` | 高 |

### 02 登录、两步验证与会话

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 表单登录成功后签发 JWT，失败返回统一 401 | `SecurityConfiguration.java:104-155` | 高 |
| 已启用 TOTP 的账号返回 428 + challenge，Redis 保存 5 分钟 | `SecurityConfiguration.java:104-155`；`TwoFactorServiceImpl.java:78-96` | 高 |
| `/api/2fa/login` 校验 challenge 与动态码，通过后签发 JWT | `TwoFactorLoginController.java:25-42` | 高 |
| JWT 含 id / username / authorities / exp；登出把 `jti` 写入 Redis 黑名单直至过期 | `JwtUtils.java:43-105` | 高 |
| 前端按“记住我”写入 `localStorage` 或 `sessionStorage`，Axios 统一注入 Bearer Token | `net/index.ts:249-315` | 高 |
| 受保护接口 401 时清除登录态并回登录页；公开接口的 401 不触发该逻辑 | `net/index.ts:142-157` | 高 |
| 游客可访问 `/index`、课程总览与每日一词；前端隐藏签到、用户区等需登录内容 | `router/index.ts:218-245`；`MainView.vue:45`；`SecurityConfiguration.java:60-63` | 高 |
| 登录页收到 428 后弹出动态码输入框 | `LoginPage.vue:94-128` | 高 |

### 03 课程学习与积分奖励

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 学习总览允许匿名：`id` 缺失时 `accountId=null`，返回全部课程与零值统计 | `CourseProgressController.java:36-40`；`CourseProgressServiceImpl.java:86-117` | 高 |
| 课程详情、单课进度、进度上报都要求登录（无匿名放行规则） | `SecurityConfiguration.java:53-69`；`CourseProgressController.java:42-64` | 高 |
| 上报前校验课程存在、章节非空且属于该课程；时长截断到 `0..3600` 秒 | `CourseProgressServiceImpl.java:170-209` | 高 |
| 章节进度 upsert（累计时长与学习次数），同时插入 `course_study_log` | `CourseProgressServiceImpl.java:185-197` | 高 |
| 已学章节数 / 总章节数决定 `not_started / in_progress / completed` | `CourseProgressServiceImpl.java:200-209, 258+` | 高 |
| 完成课程后按 `(account_id, course_id)` 唯一发放奖励，并发由唯一键兜底，只发一次 | `CourseProgressServiceImpl.java:214-233` | 高 |
| 打开章节即开始前端计时，离开或阶段结束时上报时长 | `CourseStudyPage.vue:78-130` | 高 |

### 04 单词测试与知识测试

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 单词测试取 CET4 / CET6 词表，前端过滤无释义单词并生成 4 选 1 选项 | `WordTestPage.vue:90-122`；`DictionaryController` | 高 |
| 单词成绩提交 `source/total/correct/duration`，后端校验 `source` 与计数范围 | `WordTestController.java:19-37`；`WordTestServiceImpl.java:22-33` | 高 |
| 成绩得分公式 `correct * 100 / total`，错误数由总数减正确数得出 | `BaseTestService.java:28-55` | 高 |
| 知识测试支持普通模式（`typeId`）与课程题集模式（`setId`），两者必须二选一 | `KnowledgeTestServiceImpl.java:44-78` | 高 |
| 题目总数上限 100，`correctCount` 必须在 `0..total` 内 | `KnowledgeTestServiceImpl.java:26, 68`；`BaseTestService.java:28-55` | 高 |
| `detail` 与正确数由前端判分后提交，后端只做结构清洗与范围校验 | `KnowledgeTestPage.vue:144-288`；`KnowledgeTestServiceImpl.java:44-78` | 高 |
| 历史与汇总按类别读取 `test_record`，计算平均分 / 最高分 / 正确率 | `BaseTestService.java:60-77`；`WordTestServiceImpl` / `KnowledgeTestServiceImpl` | 高 |

### 05 每日签到与积分

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 状态接口返回本月日期、今日是否签到、连续天数、总积分、月积分与流水 | `CheckInServiceImpl.java:31-62` | 高 |
| 已签到直接返回已有记录，重复请求幂等 | `CheckInServiceImpl.java:66-73` | 高 |
| 连续天数：最近记录是今天或昨天则延续，否则归零；签到后 `streak + 1` | `CheckInServiceImpl.java:66-96` | 高 |
| 积分等于连续天数（`points = streak`），写入签到记录后累加到账户积分 | `CheckInServiceImpl.java:74-92` | 高 |
| 一天一条由数据库唯一键保证；并发插入冲突时回查当日记录返回 | `CheckInServiceImpl.java:77-83` | 高 |
| 前端首页加载状态、切换月份、提交签到并刷新 | `HomePage.vue:97-127` | 高 |
| 游客不显示签到卡片 | `HomePage.vue`；`MainView.vue:45` | 高 |

### 06 学习小组

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 创建小组生成 8 位邀请码，同时写入 owner 成员关系 | `StudyGroupServiceImpl.java:144-195` | 高 |
| 邀请码加入校验邀请码有效且未加入，成员角色为 `member` | `StudyGroupServiceImpl.java:144-195` | 高 |
| 小组详情、消息仅成员可见；访问详情会记录访问流水供任务判定 | `StudyGroupServiceImpl.java:197-248` | 高 |
| 组长发布任务时写入 `type=task` 消息与任务项 | `StudyGroupServiceImpl.java:250-331` | 高 |
| 任务项四类：`text` 手动标记、`course` 章节全部学完、`resource` 发布后资源访问、`member_visit` 发布后小组访问 | `StudyGroupServiceImpl.java:542-569` | 高 |
| 任务进度按任务项实时计算，不单独存布尔完成态 | `StudyGroupServiceImpl.java:333-440, 492-569` | 高 |
| 组长退出即解散；组员退出即移除；组长可移除组员 | `StudyGroupServiceImpl.java:333-440`；`StudyGroupController.java:31-153` | 高 |

### 07 讨论区与通知

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 列表分页并生成 ≤120 字摘要，批量换取作者用户名 | `DiscussionServiceImpl.java:95-132` | 高 |
| 发帖标题 ≤150、正文 ≤5000；评论 ≤1000 | `DiscussionServiceImpl.java:95-132, 156-198` | 高 |
| 评论后 `comment_count + 1`；删除评论时计数 -1 | `DiscussionServiceImpl.java:156-198` | 高 |
| 评论者不是作者时写 `notification(type=discussion)`，链到帖子；自评不发通知 | `DiscussionServiceImpl.java:200-225` | 高 |
| 文章 / 评论仅作者或管理员可删；文章删除级联评论 | `DiscussionServiceImpl.java:95-132, 156-198` | 高 |
| 通知列表返回未读数，支持单条已读与全部已读 | `NotificationController.java:28-50` | 高 |
| 管理员定向通知复用同一张通知表 | `AdminController.java`；`AdminServiceImpl.java:34-80` | 高 |

### 08 管理后台

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 前端 `/admin` 路由 `adminOnly`，后端 `/api/admin/**` 要求 `hasRole("admin")` | `router/index.ts:153-188`；`SecurityConfiguration.java:68` | 高 |
| 用户管理：调整角色、删除用户、重置密码、批量创建 | `AdminController.java:41-102` | 高 |
| 不能降级当前管理员、不能删除当前账号；重置密码生成随机密码并发站内通知 | `AdminController.java:41-102` | 高 |
| 批量创建逐条返回失败原因，允许部分成功 | `AdminServiceImpl.java:34-80` | 高 |
| 课程管理维护章节与题集关联 | `AdminCourseController.java:22-58` | 高 |
| 题库管理维护测试类型与题目 | `AdminTestBankController.java:22-76` | 高 |
| 题集管理维护题目顺序 | `AdminQuestionSetController.java:19-40` | 高 |
| 资源管理支持 multipart 上传、编辑、删除，`visible` 控制可见性 | `AdminResourceController.java:26-60` | 高 |
| 通知目标 `all / role / users`，批量写 `notification` | `AdminController.java`；`AdminServiceImpl.java:34-80` | 高 |

### 09 邮件验证码异步投递

| 关键事实 | sourceRefs | 置信度 |
| --- | --- | --- |
| 发送验证码先做 Redis 频控，再生成 6 位码存 300 秒，然后交给 `MailDispatcher` | `EmailCodeService.java:48-103` | 高 |
| 队列开启且生产者可用时，消息经 `RabbitMailProducer` 投递到 exchange / routingKey | `MailDispatcher.java:35-47`；`RabbitMailProducer.java:30-35` | 高 |
| 消费者反序列化 `EmailMessage`，交给 `MailDelivery.send` | `RabbitMailConsumer.java:23-31` | 高 |
| 队列关闭、投递失败或 broker 不可用时回退为直接发送 | `MailDispatcher.java:35-47` | 高 |
| `delivery=smtp` 才真实发信；否则或 SMTP 失败时写 `[mail:log]` 日志，方法不抛异常 | `MailDelivery.java:32-66`；`application.yaml:51-55` | 高 |
| 默认 `queue.enabled=false`、`delivery=log`，开发环境验证码在服务端日志查看 | `application.yaml:51-61` | 高 |
| 注册 / 找回密码通过 `GET /api/auth/ask-code` 触发该链路 | `AuthorizeController.java:58-67`；`RegisterPage.vue`；`ForgetPage.vue` | 高 |

## 4. 跨流程结论

1. **身份传递统一**：前端只负责带 Bearer Token；后端过滤器统一解析并注入 `request.id`，
   业务控制器不自行解析令牌。
2. **匿名只读边界明确**：只有课程总览与词典接口匿名；课程详情、进度上报、签到、测试、
   小组、讨论、通知、管理接口都要求登录。
3. **幂等靠数据库约束**：签到一天一条、课程奖励一人一课一次、邮箱验证码一次性消费；
   并发冲突均由唯一键或回查兜底。
4. **通知是站内统一模型**：注册欢迎、评论提醒、管理员重置密码、管理员定向通知都写入
   `notification` 表，由 07 的列表 / 已读接口消费。
5. **异步链路可降级**：邮件投递从 RabbitMQ 到直接发送再到日志，任何一层失败都不会阻断
   注册 / 找回主流程。
6. **判分在前端**：单词与知识测试的正确数、明细由前端计算后提交，后端只校验范围与结构，
   这是当前产品行为，不是图中遗漏。

## 5. 生成与校验记录

最近一次生成（2026-10-26）：

```text
00-legend:               12 nodes,  0 edges, 1200x863
01-register-reset:       29 nodes, 28 edges, 1405x1731
02-login-session:        25 nodes, 24 edges, 1200x1659
03-course-learning:      19 nodes, 16 edges, 1200x1631
04-tests:                21 nodes, 21 edges, 1435x1431
05-checkin-points:        16 nodes, 13 edges, 1200x1417
06-study-group:          23 nodes, 26 edges, 1200x1933
07-discussion-notification: 19 nodes, 19 edges, 1200x1217
08-admin:                26 nodes, 25 edges, 1945x1231
09-mail-dispatch:        16 nodes, 17 edges, 1435x1431
```

已执行的校验：

- `node scripts/generate-flowcharts.mjs`：节点 / 边引用、ID 唯一、包围盒重叠全部通过；
- `xmllint --noout docs/architecture/flows/learning-program-flows.drawio`：XML 良构通过；
- 页面尺寸由节点包围盒自动推导（`right + 40`、`bottom + 证据区`），不存在节点越界；
- `flows.manifest.json` 记录页面规模，可用于再生前后比对。

未执行：在 diagrams.net 中做像素级渲染验收（本环境没有 Draw.io MCP/CLI）。

## 6. 格式降级与维护方式

| 方面 | 处理 |
| --- | --- |
| 事实源 | `scripts/generate-flowcharts.mjs` 中的 `PAGES` 模型；`.mmd` 与 `.drawio` 都由它生成 |
| Mermaid | 每个流程一份，页面末尾以 `%%` 注释附证据；不支持置信度字段 |
| Draw.io | 多页 `mxfile`，可编辑交付件；连线采用正交路由锚点，个别绕行可在编辑器中手动拖动 |
| 置信度 | 不编码进图形，只在本文件与每页底部 Note 中维护 |
| 手工修改 | 改流程必须同时改生成脚本并重新生成，否则 `.mmd`、`.drawio`、`manifest` 会漂移 |

## 7. 已知限制与未覆盖范围

- 流程是静态代码推导，未接入运行时日志、链路追踪或数据库实际数据，不代表特定环境的时序。
- 未单独成图的次要操作：个人资料修改、2FA 开启 / 关闭、排行榜、资源下载、每日一词、
  管理员对章节 / 题目顺序的细粒度编辑。其中 2FA 登录校验已包含在 02，资源访问判定
  已作为 06 的任务项分支出现。
- 知识测试 `detail` 与正确数的前端信任问题已在 04 与第 4 节标注。
- 邮件默认日志模式意味着“用户收到邮件”在默认环境不成立，09 页面用配置说明表达这一点。
- Draw.io 页面尚未做像素级视觉验收；如打开后发现连线交叉，不影响代码事实，可局部拖动调整。
