# 主界面未登录(游客)模式

## 目标
未登录访问 `/index` 不再跳登录页:显示 hero + 统计(公开数据,0 值)+ **课程列表** + **登录引导卡**;顶栏显示「登录」按钮替代铃铛/头像/退出。其余页面仍跳登录。已登录用户行为完全不变。

## 后端(2 文件,已核实服务层天然支持游客)

1. **`SecurityConfiguration.java`**(permitAll 区,约 L58-63 间加两条,精确到 GET 避免放开写接口):
   - `.requestMatchers(HttpMethod.GET, "/api/courses/progress").permitAll()`
   - `.requestMatchers(HttpMethod.GET, "/api/dictionary/**").permitAll()`(侧栏每日一词卡对游客可见)

2. **`CourseProgressController.java:31`** `overview` 的 `@RequestAttribute("id")` → `@RequestAttribute(value = "id", required = false)`。
   - 已核实 `CourseProgressServiceImpl.getOverview` 对 null 全程安全:游客返回**全部课程**(status=not_started)+ summary(0,0,0);已登录用户携带 token 时行为不变。`POST /{courseId}/progress` 等仍要求登录。

## 前端(4 文件)

3. **`router/index.ts:211`**:守卫白名单移除 `/index` 前缀(其余保持跳 welcome-login)。

4. **`MainView.vue`**:
   - import(L22)加 `publicGet, unauthorized`;新增 `const isLoggedIn = !unauthorized()`;
   - 顶栏 topbar-actions(L267-285):铃铛/头像/退出三个按钮包 `v-if="isLoggedIn"`,`v-else` 渲染「登录」按钮 → `router.push({name:'welcome-login'})`;
   - `loadDailyWord`(L130)的 `get` → `publicGet`(登录用户经 axios 请求拦截器仍自动带 token;且 sessionExpiryOn401=false,避免游客 401 触发全局跳登录)。

5. **`HomePage.vue`**:
   - import 加 `publicGet, unauthorized`;`const isLoggedIn = !unauthorized()`;
   - 新增 `courses` ref 存 overview 响应里的 `courses` 数组;`loadProgressSummary` 按登录态选 `get`/`publicGet`;`onMounted` 游客时**不调** `loadCheckInStatus`;
   - 模板:`check-in-card` 加 `v-if="isLoggedIn"`;`v-else` 渲染登录引导卡(标题「登录后开启完整体验」+ 描述签到/积分/进度 + 「立即登录」按钮);stats-grid 之后新增游客课程列表区 `v-if="!isLoggedIn"`(heading + 静态卡片:标题/描述/章节数/「未开始」tag,复用 `courses.notStartedTag`,不可点击避免 confusing 跳转)。

6. **`i18n/locales/zh-CN.ts`**:`common` 加 `login: '登录'`;`home` 加游客卡与课程区标题/描述等 4-5 个 key。

## 验证
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn compile`(本机 java-25 目录缺 javac,必须显式指 21);前端 `npm run type-check` + `npm run build-only`。
- 起后端(1232 端口)+ vite dev,浏览器无痕窗口验证:游客 /index 正常渲染、无 401 弹窗、课程列表有数据;登录后签到/统计/顶栏一切如旧;游客访问 /courses 仍跳登录。

## 说明
- 改动只在 web 前端(learning-program-frontend)+ 后端;tauri 版不同步(如需要另说)。
- token 过期的用户访问 /index 会被 `takeAccessToken()` 清掉过期态、按游客展示,合理。
