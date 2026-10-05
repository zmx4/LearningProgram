# 接口文档

## 基础信息

- 默认服务地址：`http://localhost:1231`
- 所有接口使用 `/api` 前缀
- 除特别说明外，请求和响应均使用 JSON
- 登录接口使用 `application/x-www-form-urlencoded`

## 统一响应格式

成功响应：

```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

失败响应：

```json
{
  "code": 400,
  "data": null,
  "message": "错误信息"
}
```

常见状态码：

| code | 含义 |
| --- | --- |
| `200` | 请求成功 |
| `400` | 参数错误或业务校验失败 |
| `401` | 未登录或登录状态已失效 |
| `403` | 没有访问权限 |
| `404` | 资源不存在 |
| `409` | 数据冲突 |

## 认证

登录成功后，服务端返回 JWT。访问需要登录的接口时，请在请求头携带：

```http
Authorization: Bearer YOUR_JWT_VALUE
```

前端会将登录信息保存到浏览器的 `authorize` 存储项中，并自动为请求添加认证头。

## 认证接口

### 登录

```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名或邮箱 |
| `password` | string | 是 | 密码 |

成功响应中的 `data` 包含 `token`、`expireTime`、`username` 和 `role`。

### 注册

```http
POST /api/auth/register
Content-Type: application/json
```

请求体包含用户名、密码、重复密码、邮箱和验证码等注册字段，具体校验以 `EmailRegisterVO` 为准。
密码强度要求见下方「密码强度策略」，不满足时返回 `400`，`message` 里会列出所有不满足的项。

### 查询密码强度策略

```http
GET /api/auth/password-policy
```

匿名可访问。返回当前生效的密码要求，供前端展示规则并做即时校验；
规则由后端的 `PasswordPolicy` 责任链决定，改配置后本接口即随之变化，前端无需改动。

```json
{
  "code": 200,
  "data": {
    "minLength": 8,
    "maxLength": 32,
    "requirements": [
      { "code": "length", "message": "长度需在 8 到 32 个字符之间", "min": 8, "max": 32 },
      { "code": "require-uppercase", "message": "至少包含一个大写字母", "characterClass": "uppercase" },
      { "code": "require-lowercase", "message": "至少包含一个小写字母", "characterClass": "lowercase" },
      { "code": "require-digit", "message": "至少包含一个数字", "characterClass": "digit" },
      { "code": "require-special", "message": "至少包含一个特殊字符", "characterClass": "special" },
      { "code": "no-whitespace", "message": "不能包含空格等空白字符" },
      { "code": "no-account-info", "message": "不能包含用户名或邮箱" },
      { "code": "not-common", "message": "不能是常见弱密码" }
    ]
  },
  "message": "success"
}
```

`requirements[].code` 为规则标识，前端按它决定用哪段逻辑做即时校验；未知规则码应视为通过、留给服务端判定
（例如 `not-common` 就用的是服务端词表）。

### 密码强度策略

所有「设置密码」的入口都走同一条 `PasswordPolicy` 责任链，包括注册、忘记密码重置、
修改密码与管理员的批量建号；管理员重置密码时由 `PasswordGenerator` 按同一策略生成随机密码。
默认要求（可在 `application.yaml` 的 `learning.password.policy` 下调整）：

| 规则 | 默认 |
| --- | --- |
| 长度 | 8 到 32 个字符 |
| 大写字母 | 至少 1 个 |
| 小写字母 | 至少 1 个 |
| 数字 | 至少 1 个 |
| 特殊字符 | 至少 1 个（非字母、非数字、非空白，按 Unicode 判断） |
| 空白字符 | 不允许 |
| 账号信息 | 密码中不能出现用户名或邮箱（含 `@` 前的部分，少于 3 个字符时不参与判断） |
| 弱密码词表 | 精确匹配（忽略大小写）即拒绝 |

校验不通过时返回 `400`，`message` 会把所有不满足的项用 `；` 连成一句，便于一次改完。

### 确认重置密码

```http
POST /api/auth/reset-confirm
Content-Type: application/json
```

请求体：

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

### 重置密码

```http
POST /api/auth/reset-password
Content-Type: application/json
```

请求体包含邮箱、验证码和新密码。

### 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

服务端会使当前 JWT 失效。

## 个人资料接口

### 查询当前用户资料

```http
GET /api/profile
Authorization: Bearer <token>
```

响应数据：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "phone": "",
  "bio": "",
  "role": "user"
}
```

### 更新当前用户资料

```http
PUT /api/profile
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "phone": "13800000000",
  "bio": "学习者"
}
```

角色不接受前端修改，由管理员接口管理。

### 修改密码

```http
PUT /api/profile/password
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "oldPassword": "OldPass123!",
  "newPassword": "NewPass123!"
}
```

新密码需满足「密码强度策略」，校验不通过返回 `400`；旧密码不正确或新密码与当前密码相同时同样返回 `400`。

## 通知接口

### 查询当前用户通知

```http
GET /api/notifications
Authorization: Bearer <token>
```

响应数据包含 `items` 和 `unreadCount`。每条通知的结构：

```json
{
  "id": 12,
  "title": "你的文章有新评论",
  "content": "tick 评论了你的文章《如何学习 Java》：建议先看官方教程",
  "link": "/discussions/3",
  "type": "discussion",
  "read": false,
  "createdAt": "2026-10-01T17:20:00"
}
```

`link` 为点击通知后前端跳转的路由，管理员群发通知时为 `null`。

### 标记单条通知已读

```http
POST /api/notifications/{id}/read
Authorization: Bearer <token>
```

### 标记全部通知已读

```http
POST /api/notifications/read-all
Authorization: Bearer <token>
```

## 用户接口

### 查询用户公开信息

```http
GET /api/user/{id}
Authorization: Bearer <token>
```

返回用户名、邮箱、个人简介和临时头像信息。

## 单词接口

### 查询单词

```http
GET /api/dictionary?word=hello
Authorization: ******
```

也支持使用路径参数：`GET /api/dictionary/{word}`。

成功响应中的 `data` 包含单词、释义和记录 ID：

```json
{
  "id": 1,
  "word": "hello",
  "translation": "你好"
}
```

单词不存在时返回 `404`，单词为空或超过 255 个字符时返回 `400`。

## 签到接口

### 查询签到日历

```http
GET /api/check-in?year=2026&month=9
Authorization: ******
```

返回指定月份的签到日期、今日签到状态、连续签到天数、累计积分，以及该月的签到次数、积分和每日积分明细。

### 签到

```http
POST /api/check-in
Authorization: ******
```

每天只能签到一次。首次签到获得 1 分，连续签到时获得的积分等于当前连续签到天数；中断后重新从 1 分开始。

### 查询总积分

```http
GET /api/points
Authorization: ******
```

响应：

```json
{
  "totalPoints": 12
}
```

### 查询 CET4/CET6 单词

```http
GET /api/dictionary/cet4?count=10
Authorization: ******
```

## 测试接口

### 保存四六级单词测试结果

```http
POST /api/tests/words/results
Authorization: ******
Content-Type: application/json
```

请求体包含 `source`、`totalCount`、`correctCount` 和 `durationSeconds`，服务端计算错误数和得分。

### 查询测试历史和汇总

```http
GET /api/tests/words/history
Authorization: ******
```

```http
GET /api/dictionary/cet6?count=10
Authorization: ******
```

`count` 可选，默认值为 `1`，取值范围为 `1` 至 `100`。接口每次随机返回指定数量的单词，单个请求和批量请求均返回数组：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "word": "ability",
      "translation": "能力"
    }
  ],
  "message": "success"
}
```

## 测试题库接口

题库由「测试类型」和「题目」两级组成，题目内容以 JSON 存放在 `db_test_question.content` 列中。
题型用 `kind` 表示，取值为 `single`（单选）、`multiple`（多选）、`blank`（填空）。
以下读取接口登录后即可访问。

### 查询全部测试类型

```http
GET /api/test-types
Authorization: ******
```

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "code": "knowledge-basic",
      "name": "计算机基础",
      "description": "计算机基础知识测试，包含单选、多选与填空三种题型。",
      "createdAt": "2026-10-01T16:30:00"
    }
  ],
  "message": "success"
}
```

### 查询单个测试类型

```http
GET /api/test-types/{id}
Authorization: ******
```

类型不存在时 `code` 为 `404`。

### 查询某个类型的题目

```http
GET /api/test-questions?typeId=1&count=5&kind=single
Authorization: ******
```

参数说明：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `typeId` | 是 | 测试类型 id |
| `kind` | 否 | 题型，取值为 `single` / `multiple` / `blank`；不传表示不限题型 |
| `count` | 否 | 题目数量，取值范围 `1` 至 `100`；**不传返回该类型全部题目**（按 id 升序），传了则随机抽取 |

响应中的 `content` 就是数据库中存放的那段 JSON，按题型含义如下：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "typeId": 1,
      "kind": "single",
      "content": {
        "stem": "HTTP 协议默认使用的端口号是？",
        "options": [
          { "key": "A", "text": "21" },
          { "key": "B", "text": "80" }
        ],
        "answer": ["B"],
        "analysis": "HTTP 默认端口为 80，HTTPS 默认端口为 443。"
      },
      "createdAt": "2026-10-01T16:30:00"
    }
  ],
  "message": "success"
}
```

`content` 字段约定：

- `stem`：题干，必填
- `options`：选项数组，元素为 `{ "key": "A", "text": "..." }`；**填空题恒为空数组**
- `answer`：答案数组。单选恰好 1 项且为选项 `key`；多选至少 2 项且均为选项 `key`；填空题按空格顺序每空一项
- `analysis`：解析，可选，没有时为 `null`；内容为 Markdown 文本，学员端渲染时内嵌 HTML 会被转义

`typeId` 不存在返回 `404`，其他参数非法返回 `400`。

## 题集接口

题集（`db_question_set`）把若干题目（引用 `db_test_question`）组织为一个集合，包含标题、描述与题目列表，
题目通过关联表 `db_question_set_item` 引用，题目在题集中的顺序即加入顺序。以下读取接口登录后即可访问。

### 查询全部题集

```http
GET /api/question-sets
Authorization: ******
```

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "第一章练习",
      "description": "HTTP 与数据库基础题。",
      "questionCount": 3,
      "createdAt": "2026-10-02T10:00:00"
    }
  ],
  "message": "success"
}
```

### 查询题集详情

```http
GET /api/question-sets/{id}
Authorization: ******
```

`questions` 按题目加入题集的顺序排列，元素结构与「查询某个类型的题目」响应中的题目一致。
题集不存在时 `code` 为 `404`。

## 知识测试接口

知识测试从「测试题库接口」中读取测试类型和题目，交卷后成绩保存在 `db_knowledge_test_record` 表中。
判分由前端完成（填空题按忽略大小写、去首尾空格比较），服务端负责校验并计算错误数与得分。

### 保存知识测试结果

```http
POST /api/tests/knowledge/results
Authorization: ******
Content-Type: application/json
```

请求体：

```json
{
  "typeId": 1,
  "setId": null,
  "totalCount": 10,
  "correctCount": 8,
  "durationSeconds": 125,
  "detail": [
    { "questionId": 1, "kind": "single", "userAnswer": ["B"], "correct": true },
    { "questionId": 2, "kind": "multiple", "userAnswer": ["A", "C"], "correct": false },
    { "questionId": 3, "kind": "blank", "userAnswer": ["final", "StringBuilder"], "correct": true }
  ]
}
```

- `typeId` 与 `setId` 二选一：普通模式传 `typeId`（测试类型不存在返回 `400`）；课程题集模式传 `setId`（题集不存在返回 `400`），两者都缺省或同时传入返回 `400`
- `totalCount` 取值范围 1 至 100；`correctCount` 介于 0 与 `totalCount` 之间，否则返回 `400`
- `detail` 为答题明细，可缺省；服务端会清理非法项（缺少题目 id、题型非法）并截断过长答案
- 服务端计算 `wrongCount` 和 `score`（百分制），并记录 `typeName` 快照；题集模式下 `typeName` 为 `题集：{title}`

成功响应：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "typeId": 1,
    "typeName": "计算机基础",
    "totalCount": 10,
    "correctCount": 8,
    "wrongCount": 2,
    "score": 80,
    "durationSeconds": 125,
    "detail": [
      { "questionId": 1, "kind": "single", "userAnswer": ["B"], "correct": true }
    ],
    "createdAt": "2026-10-01T17:20:00"
  },
  "message": "success"
}
```

### 查询知识测试历史和汇总

```http
GET /api/tests/knowledge/history
Authorization: ******
```

```json
{
  "code": 200,
  "data": {
    "summary": {
      "testCount": 3,
      "averageScore": 76.7,
      "bestScore": 90,
      "totalQuestions": 30,
      "totalCorrect": 23
    },
    "records": [
      {
        "id": 3,
        "typeId": 1,
        "typeName": "计算机基础",
        "totalCount": 10,
        "correctCount": 9,
        "wrongCount": 1,
        "score": 90,
        "durationSeconds": 98,
        "detail": [],
        "createdAt": "2026-10-01T17:20:00"
      }
    ],
    "message": "success"
  }
}
```

## 课程接口

课程由「课程 → 章节 / 题集关联」三级组成，题集复用「题集接口」中的集合。以下读取接口登录后即可访问。

### 查询全部课程

```http
GET /api/courses
Authorization: ******
```

响应按 `sortOrder` 升序，每项含 `chapterCount` 与 `questionSetCount` 两个统计字段。

### 查询课程详情

```http
GET /api/courses/{id}
Authorization: ******
```

课程不存在返回 `404`。响应含全部章节（按 `sortOrder` 升序）与挂载的题集 `questionSets`
（按课程内顺序，每项含 `id`、`title`、`description` 与 `questionCount`），学习页据此渲染练习入口，
点击后以 `/tests/knowledge?set={id}` 进入题集模式的知识测试。

### 课程管理（管理员）

```http
GET    /api/admin/courses                      课程列表（含 chapterCount 与 questionSetIds，不含章节正文）
GET    /api/admin/courses/{id}                 课程详情（含完整章节列表）
POST   /api/admin/courses                      新建课程
PUT    /api/admin/courses/{id}                 修改课程（全量覆盖章节与题集关联）
DELETE /api/admin/courses/{id}                 删除课程（级联删除章节、题集关联与学员进度）
Authorization: Bearer <admin-token>
```

请求体为 `title`（必填，≤100 字符）、`description`（≤500）、`icon`（emoji，≤16）、`sortOrder`（新建缺省排在末尾，
修改缺省保持原值）、`chapters`（章节列表，按数组顺序排序：`title` 必填 ≤150 字符、`content` 为
Markdown 文本（前端渲染，内嵌 HTML 会被转义）、`id` 为已有章节 id 时原位更新以保留学员进度，未带 id 的新增，缺失的旧章节删除）、
`questionSetIds`（课程挂载的题集，去重且必须已存在，按数组顺序展示）。
参数非法返回 `400`；修改/删除不存在的课程返回 `400`，查询不存在返回 `404`。

## 学习资源接口

管理员在管理端上传资料文件（单文件上限 50MB），文件本体保存在后端 `learning.resources.upload-dir`
配置的目录（默认 `./uploads/resources`），元数据存于 `db_learning_resource`。资源带 `visible` 标记：
**只有 `visible` 为 `true` 的资源出现在学员端列表**；隐藏资源对非管理员一律按不存在处理（列表不返回、下载 `404`）。

### 查询可见资源

```http
GET /api/resources
Authorization: ******
```

按创建时间倒序返回当前登录用户可见（`visible=true`）的资源列表。

### 下载资源

```http
GET /api/resources/{id}/download
Authorization: ******
```

以 `attachment` 方式返回文件本体，文件名取自上传时的原始文件名。资源不存在、不可见或磁盘文件丢失返回 `404`。

### 资源管理（管理员）

```http
GET    /api/admin/resources                    全部资源（含隐藏与上传者）
POST   /api/admin/resources                    上传资源（multipart/form-data）
PUT    /api/admin/resources/{id}               修改是否在页面上显示
DELETE /api/admin/resources/{id}               删除资源（文件一并删除）
Authorization: Bearer <admin-token>
```

上传请求体为 `multipart/form-data`：`file`（必填，≤50MB）、`title`（必填，≤100 字符）、
`description`（≤255，可缺省）、`visible`（可缺省，默认 `true`）。服务端以 UUID 重命名落盘，
保留原始文件名用于展示与下载。参数非法返回 `400`。

修改显示状态请求体为 `{ "visible": true }`。删除时数据库记录与磁盘文件一并移除，不可恢复。

## 讨论区接口

任何登录用户都可以发表文章和评论。文章的评论数由服务端维护；**有人评论你的文章时会收到一条站内通知**
（自己评论自己的文章不通知），通知带 `link`，点击可直接跳到该文章。

### 分页查询文章

```http
GET /api/discussions?page=1&size=20
Authorization: Bearer <token>
```

`page` 从 `1` 开始，缺省 `1`；`size` 缺省 `20`，上限 `50`。按发表时间倒序，返回摘要而非全文：

```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 3,
        "title": "如何学习 Java",
        "summary": "先把语法过一遍，然后动手写小项目……",
        "authorId": 1,
        "authorName": "tick",
        "commentCount": 2,
        "createdAt": "2026-10-01T17:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  },
  "message": "success"
}
```

### 查询文章详情

```http
GET /api/discussions/{id}
Authorization: Bearer <token>
```

返回同上结构但含 `content` 全文（不含 `summary`）。文章不存在返回 `404`。

### 发表文章

```http
POST /api/discussions
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "title": "如何学习 Java",
  "content": "先把语法过一遍，然后动手写小项目。"
}
```

`title` 必填且不超过 150 字符，`content` 必填且不超过 5000 字符，不满足返回 `400`。

### 删除文章

```http
DELETE /api/discussions/{id}
Authorization: Bearer <token>
```

仅作者本人或管理员可以删除，否则返回 `403`；文章不存在返回 `404`。
文章下的评论由外键级联删除。

### 查询文章的评论

```http
GET /api/discussions/{id}/comments
Authorization: Bearer <token>
```

按发表时间正序返回全部评论（不分页）；文章不存在返回 `404`。

```json
{
  "code": 200,
  "data": [
    {
      "id": 7,
      "postId": 3,
      "authorId": 2,
      "authorName": "someone",
      "content": "建议先看官方教程。",
      "createdAt": "2026-10-01T17:20:00"
    }
  ],
  "message": "success"
}
```

### 发表评论

```http
POST /api/discussions/{id}/comments
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "content": "建议先看官方教程。"
}
```

`content` 必填且不超过 1000 字符。评论成功后给文章作者写入一条站内通知（`type` 为 `discussion`，
`link` 为 `/discussions/{id}`）；评论者就是作者本人时不发通知。文章不存在返回 `404`，内容非法返回 `400`。

### 删除评论

```http
DELETE /api/discussions/{postId}/comments/{commentId}
Authorization: Bearer <token>
```

仅评论作者本人或管理员可以删除，否则返回 `403`。

## 管理员接口

以下接口要求当前用户具有 `admin` 角色，否则返回 `403`。

### 查询所有用户

```http
GET /api/admin/users
Authorization: Bearer <admin-token>
```

### 修改用户角色

```http
PUT /api/admin/users/{id}/role
Authorization: Bearer <admin-token>
Content-Type: application/json
```

请求体：

```json
{
  "role": "user"
}
```

角色支持 `user` 和 `admin`。管理员不能移除自己的管理员权限。

### 删除用户

```http
DELETE /api/admin/users/{id}
Authorization: Bearer <admin-token>
```

管理员不能删除当前登录账号。

### 重置用户密码

```http
PUT /api/admin/users/{id}/password
Authorization: Bearer <admin-token>
```

服务端按当前密码强度策略生成随机密码并加密保存，明文只在本次响应中返回一次：

```json
{
  "code": 200,
  "data": {
    "username": "alice",
    "password": "oRewHa2YvHg7K"
  },
  "message": "success"
}
```

生成字符集剔除了 `I/O/l/0/1` 等易混淆字符；长度由 `learning.password.policy.generated-length` 决定（默认 12）。
用户不存在返回 `404`。

### 批量创建账号

```http
POST /api/admin/users/batch
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "accounts": [
    { "username": "alice", "email": "alice@example.com", "password": "Str0ng!Pass1" },
    { "username": "bob", "email": "bob@example.com", "password": "123456" }
  ]
}
```

逐条创建，单条失败不影响其他条目。密码同样受「密码强度策略」约束，不合规的条目会出现在 `errors` 中：

```json
{
  "code": 200,
  "data": {
    "createdCount": 1,
    "failedCount": 1,
    "errors": [
      { "index": 1, "message": "长度需在 8 到 32 个字符之间；至少包含一个大写字母；…；不能是常见弱密码" }
    ]
  },
  "message": "success"
}
```

### 发送通知

```http
POST /api/admin/notifications
Authorization: Bearer <admin-token>
Content-Type: application/json
```

发送给全体用户：

```json
{
  "title": "系统通知",
  "content": "平台将于今晚进行维护。",
  "type": "system",
  "targetType": "all"
}
```

按角色发送：

```json
{
  "title": "管理员通知",
  "content": "请查看最新管理安排。",
  "type": "system",
  "targetType": "role",
  "targetRole": "admin"
}
```

发送给指定用户：

```json
{
  "title": "学习提醒",
  "content": "请完成本周学习任务。",
  "type": "system",
  "targetType": "users",
  "userIds": [2, 5, 8]
}
```

`targetType` 支持：

- `all`：全体用户
- `role`：指定角色用户
- `users`：指定用户 ID

成功响应：

```json
{
  "code": 200,
  "data": {
    "sentCount": 3
  },
  "message": "success"
}
```

### 新增测试类型

```http
POST /api/admin/test-types
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "code": "knowledge-basic",
  "name": "计算机基础",
  "description": "计算机基础知识测试，包含单选、多选与填空三种题型。"
}
```

`code` 只能包含字母、数字、下划线和短横线，长度 1 到 50，且全局唯一；`name` 必填且不超过 100 个字符；
`description` 可选，不超过 255 个字符。编码重复或参数非法时返回 `400`。

### 修改测试类型

```http
PUT /api/admin/test-types/{id}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

请求体与新增一致，校验规则相同（`code` 唯一性检查会排除自身）。类型不存在返回 `400`。

### 删除测试类型

```http
DELETE /api/admin/test-types/{id}
Authorization: Bearer <admin-token>
```

类型下仍有题目，或已有知识测试记录引用该类型时返回 `400` 并给出提示，否则删除成功。

### 新增题目

```http
POST /api/admin/test-questions
Authorization: Bearer <admin-token>
Content-Type: application/json
```

单选题：

```json
{
  "typeId": 1,
  "kind": "single",
  "content": {
    "stem": "HTTP 协议默认使用的端口号是？",
    "options": [
      { "key": "A", "text": "21" },
      { "key": "B", "text": "80" },
      { "key": "C", "text": "443" }
    ],
    "answer": ["B"],
    "analysis": "HTTP 默认端口为 80，HTTPS 默认端口为 443。"
  }
}
```

多选题的 `answer` 至少 2 项：

```json
{
  "typeId": 1,
  "kind": "multiple",
  "content": {
    "stem": "下列哪些属于关系型数据库？",
    "options": [
      { "key": "A", "text": "MySQL" },
      { "key": "B", "text": "Redis" },
      { "key": "C", "text": "MariaDB" }
    ],
    "answer": ["A", "C"]
  }
}
```

填空题不传 `options`，`answer` 按空格顺序每空一项：

```json
{
  "typeId": 1,
  "kind": "blank",
  "content": {
    "stem": "Java 中用于声明常量的关键字是 ____，可变字符串类是 ____。",
    "answer": ["final", "StringBuilder"]
  }
}
```

服务端校验规则：题干不能为空；选择题至少 2 个选项、选项标识不能重复、答案必须是已有选项标识；
单选恰好 1 个答案，多选至少 2 个答案；填空题不能带选项且答案不能为空。任一不满足返回 `400`，
`typeId` 不存在同样返回 `400`。创建成功后返回该题目的完整数据（结构与查询接口一致）。

### 修改题目

```http
PUT /api/admin/test-questions/{id}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

请求体与新增题目一致（`typeId` 必填，可借此转移题目所属类型），校验规则相同。
题目不存在返回 `400`，成功后返回更新后的题目数据。

### 删除题目

```http
DELETE /api/admin/test-questions/{id}
Authorization: Bearer <admin-token>
```

题目被题集引用时返回 `400` 并提示先在题集中移除，否则删除成功。

### 新增题集

```http
POST /api/admin/question-sets
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "title": "第一章练习",
  "description": "HTTP 与数据库基础题。",
  "questionIds": [1, 2, 3]
}
```

`title` 必填且不超过 100 个字符；`description` 可选，不超过 255 个字符；`questionIds` 为题目 id 列表，
可空（先建空题集再补题），重复 id 会自动去重，包含不存在的题目返回 `400`。
创建成功后返回题集数据（结构同「查询全部题集」）。

### 修改题集

```http
PUT /api/admin/question-sets/{id}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

请求体与新增题集一致；`questionIds` 为该题集完整的题目列表，**全量覆盖**现有题目及顺序。
校验规则同新增。题集不存在返回 `400`。

### 删除题集

```http
DELETE /api/admin/question-sets/{id}
Authorization: Bearer <admin-token>
```

删除题集及其题目关联（不影响题目本身）。题集不存在返回 `400`。

## 测试接口

```http
GET /api/test/hello
Authorization: Bearer <token>
```

用于验证登录态和后端鉴权是否正常。
