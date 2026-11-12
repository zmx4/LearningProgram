# 第三方组件许可声明（Third-Party Notices）

> 最后核对：2026-11-12 · 对应 Spring Boot 4.1.1 / `pnpm-lock.yaml` 当前锁定版本
> 本项目自身代码采用 [MIT License](./LICENSE)。本文件只说明第三方依赖的许可归属，不改变本项目的许可条款，也不构成法律意见。

## 1. 范围与数据来源

| 范围 | 组件数 | 数据来源 |
| --- | --- | --- |
| 后端 Java 依赖 | 187 个 Maven artifact（含 `test` scope） | `mvn dependency:tree` + 本地 `~/.m2/repository` 中各 POM 的 `<licenses>` 节点（未声明时向上查父 POM） |
| 前端 npm 依赖 | 333 个包（含 `devDependencies` 与传递依赖） | `node_modules/.pnpm/*/node_modules/*/package.json` 的 `license` 字段，版本以 `pnpm-lock.yaml` 为准 |

几点说明：

- **并非所有依赖都会进入发布产物**。Maven `test` scope（JUnit、Mockito 等）和前端 `devDependencies`（Vite、TypeScript、vue-tsc 等）仅用于构建与测试；前端 `dependencies` 会被 Vite 打包进 `dist`，后端运行时依赖会随 Spring Boot 可执行 JAR 分发。
- 许可证名称在各 POM / `package.json` 中写法不统一（例如 `Apache License, Version 2.0` 与 `Apache-2.0`）。第 4 节做了归一化统计，第 5 节保留**原始声明**便于逐项核对。
- 完整许可证全文、版权声明、`NOTICE` 文件仍以各依赖**自带分发文件**为准；部分包在 `node_modules` 中附带 `LICENSE` 文件，JAR 内也可能含 `META-INF/LICENSE` / `META-INF/NOTICE`。

## 2. 后端直接依赖（`learning-program-backend/pom.xml`）

| 组件 | 版本 | 许可证 | 用途 |
| --- | --- | --- | --- |
| `org.springframework.boot`：`spring-boot-starter-security`、`spring-boot-starter-webmvc`、`spring-boot-starter-validation`、`spring-boot-starter-security-test`、`spring-boot-starter-webmvc-test`、`spring-boot-starter-data-redis`、`spring-boot-starter-mail`、`spring-boot-starter-amqp` | 4.1.1 | Apache-2.0 | Web、安全、校验、测试、Redis、邮件、RabbitMQ |
| `com.mysql:mysql-connector-j` | 9.7.0 | **GPL-2.0 + Universal FOSS Exception v1.0** | MySQL JDBC 驱动（见第 6 节） |
| `com.baomidou:mybatis-plus-spring-boot4-starter` | 3.5.17 | Apache-2.0 | ORM 与分页 |
| `com.alibaba.fastjson2:fastjson2` | 2.0.64 | Apache-2.0 | JSON 处理 |
| `com.auth0:java-jwt` | 4.6.2 | MIT | JWT 生成与校验 |
| `org.projectlombok:lombok` | 1.18.46 | MIT | 编译期代码生成 |
| `com.warrenstrange:googleauth` | 1.5.0 | BSD-3-Clause | TOTP 双因素认证 |
| `com.pig4cloud.plugin:oss-spring-boot-starter` | 3.1.2 | Apache-2.0 | 对象存储接入 |

由 Spring Boot BOM 统一管理版本的 Spring Framework、Tomcat、Jackson、SLF4J/Logback、Lettuce、Micrometer、Jakarta 等传递依赖，见第 5 节完整清单。

## 3. 前端直接依赖（`learning-program-frontend/package.json`）

运行时依赖（随构建产物分发）：

| 组件 | 版本 | 许可证 |
| --- | --- | --- |
| `vue` | 3.5.43 | MIT |
| `vue-router` | 5.3.1 | MIT |
| `vue-i18n` | 11.4.12 | MIT |
| `@vueuse/core` | 15.0.0 | MIT |
| `element-plus` | 2.14.6 | MIT |
| `axios` | 1.20.0 | MIT |
| `express` | 5.3.0 | MIT（SSR 服务） |
| `markdown-it` | 15.0.2 | MIT |
| `qrcode` | 1.5.4 | MIT |

构建 / 开发依赖（不随产物分发）：

| 组件 | 版本 | 许可证 |
| --- | --- | --- |
| `typescript` | 6.0.3 | **Apache-2.0** |
| `vite` | 8.3.0 | MIT |
| `@vitejs/plugin-vue` | 6.0.9 | MIT |
| `vue-tsc` | 3.3.11 | MIT |
| `vite-plugin-vue-devtools` | 8.2.1 | MIT |
| `unplugin-auto-import` | 21.1.0 | MIT |
| `unplugin-vue-components` | 32.1.0 | MIT |
| `npm-run-all2` | 9.0.3 | MIT |
| `@element-plus/icons-vue` | 2.3.2 | MIT |
| `@tsconfig/node24` | 24.0.5 | MIT |
| `@vue/tsconfig` | 0.9.1 | MIT |
| `@types/node` | 24.13.6 | MIT |
| `@types/markdown-it` | 14.2.0 | MIT |
| `@types/qrcode` | 1.5.6 | MIT |

## 4. 全量许可证概览（归一化）

后端 187 个 artifact：绝大多数为 **Apache-2.0**（Spring Boot / Spring Framework / Tomcat / Jackson / fastjson2 / MyBatis-Plus 等）；其余为 MIT（SLF4J、Lettuce、Mockito、Lombok、java-jwt 等）、EPL-2.0（JUnit、Jakarta 组件，部分与 LGPL-2.1 / EDL-1.0 / GPL2 w/ CPE 构成多许可）、BSD-3-Clause（ASM、Hamcrest、googleauth）、MIT-0（reactive-streams），以及 1 个 GPL-2.0 + FOSS Exception（MySQL 驱动）和 1 个 `AL 2.0 / GPL v2 / MPL 2.0` 三许可（RabbitMQ `amqp-client`，本项目按其 Apache-2.0 选项使用）。

前端 333 个包：

| 许可证 | 包数 | 备注 |
| --- | --- | --- |
| MIT | 297 | 绝大多数运行时依赖 |
| ISC | 22 | 宽松许可，需保留版权声明 |
| BSD-3-Clause | 4 | `source-map-js`、`qs`、`normalize-wheel-es` |
| Apache-2.0 | 3 | `typescript`、`baseline-browser-mapping`、`detect-libc` |
| BSD-2-Clause | 2 | `entities` |
| MPL-2.0 | 2 | `lightningcss` 及其 Windows 原生二进制（见第 6 节） |
| PSF-2.0 | 1 | `argparse` |
| CC-BY-4.0 | 1 | `caniuse-lite` 数据集（见第 6 节） |
| BlueOak-1.0.0 | 1 | `minimatch` 系列 |

## 5. 完整组件清单

以下按**依赖元数据中声明的原始许可证名称**分组，可折叠查看。格式为 `组件名@版本`。

### 5.1 前端 npm 包

<details>
<summary>展开前端依赖清单（共 333 项 / 9 类声明）</summary>

**MIT · 297 项**

`@babel/code-frame@7.29.7` · `@babel/compat-data@7.29.7` · `@babel/core@7.29.7` · `@babel/generator@7.29.8` · `@babel/helper-annotate-as-pure@7.29.7`  
`@babel/helper-compilation-targets@7.29.7` · `@babel/helper-create-class-features-plugin@7.29.7` · `@babel/helper-globals@7.29.7` · `@babel/helper-member-expression-to-functions@7.29.7` · `@babel/helper-module-imports@7.29.7`  
`@babel/helper-module-transforms@7.29.7` · `@babel/helper-optimise-call-expression@7.29.7` · `@babel/helper-plugin-utils@7.29.7` · `@babel/helper-replace-supers@7.29.7` · `@babel/helper-skip-transparent-expression-wrappers@7.29.7`  
`@babel/helper-string-parser@7.29.7` · `@babel/helper-validator-identifier@7.29.7` · `@babel/helper-validator-option@7.29.7` · `@babel/helpers@7.29.7` · `@babel/parser@7.29.9`  
`@babel/plugin-proposal-decorators@7.29.7` · `@babel/plugin-syntax-decorators@7.29.7` · `@babel/plugin-syntax-import-attributes@7.29.7` · `@babel/plugin-syntax-import-meta@7.10.4` · `@babel/plugin-syntax-jsx@7.29.7`  
`@babel/plugin-syntax-typescript@7.29.7` · `@babel/plugin-transform-typescript@7.29.9` · `@babel/template@7.29.7` · `@babel/traverse@7.29.8` · `@babel/types@7.29.8`  
`@ctrl/tinycolor@4.2.1` · `@element-plus/icons-vue@2.3.2` · `@floating-ui/core@1.8.0` · `@floating-ui/dom@1.8.0` · `@floating-ui/utils@0.2.12`  
`@intlify/core-base@11.4.12` · `@intlify/devtools-types@11.4.12` · `@intlify/message-compiler@11.4.12` · `@intlify/shared@11.4.12` · `@jridgewell/gen-mapping@0.3.12`  
`@jridgewell/gen-mapping@0.3.5` · `@jridgewell/remapping@2.3.5` · `@jridgewell/resolve-uri@3.1.2` · `@jridgewell/set-array@1.2.1` · `@jridgewell/sourcemap-codec@1.6.0`  
`@jridgewell/trace-mapping@0.3.31` · `@oxc-project/types@0.150.0` · `@polka/url@1.0.0-next.29` · `@rolldown/binding-win32-x64-msvc@1.2.9` · `@rolldown/pluginutils@1.0.1`  
`@sxzz/popperjs-es@2.11.8` · `@tsconfig/node24@24.0.5` · `@types/estree@1.0.9` · `@types/linkify-it@5.0.0` · `@types/lodash-es@4.17.12`  
`@types/lodash@4.17.25` · `@types/markdown-it@14.2.0` · `@types/mdurl@2.0.0` · `@types/node@24.13.6` · `@types/qrcode@1.5.6`  
`@types/web-bluetooth@0.0.21` · `@vitejs/plugin-vue@6.0.9` · `@volar/language-core@2.4.28` · `@volar/source-map@2.4.28` · `@volar/typescript@2.4.28`  
`@vue-macros/common@3.1.4` · `@vue/babel-helper-vue-transform-on@1.1.5` · `@vue/babel-plugin-jsx@1.1.5` · `@vue/compiler-core@3.5.43` · `@vue/compiler-dom@3.5.43`  
`@vue/compiler-sfc@3.5.43` · `@vue/compiler-ssr@3.5.43` · `@vue/devtools-api@6.5.0` · `@vue/devtools-api@8.2.1` · `@vue/devtools-core@8.2.1`  
`@vue/devtools-kit@8.2.1` · `@vue/devtools-shared@8.2.1` · `@vue/language-core@3.3.11` · `@vue/reactivity@3.5.43` · `@vue/runtime-core@3.5.43`  
`@vue/runtime-dom@3.5.43` · `@vue/server-renderer@3.5.43` · `@vue/shared@3.5.43` · `@vue/tsconfig@0.9.1` · `@vueuse/core@15.0.0`  
`@vueuse/metadata@15.0.0` · `@vueuse/shared@15.0.0` · `accepts@2.0.0` · `acorn@8.18.0` · `agent-base@6.0.0`  
`alien-signals@3.2.1` · `ansi-regex@5.0.1` · `ansi-styles@4.3.0` · `ansi-styles@7.0.0` · `ast-kit@2.1.2`  
`ast-kit@2.2.0` · `ast-walker-scope@0.9.0` · `async-validator@4.2.5` · `asynckit@0.4.0` · `axios@1.20.0`  
`birpc@2.6.1` · `birpc@4.2.0` · `body-parser@2.4.0` · `browserslist@4.29.0` · `bundle-name@4.1.0`  
`bytes@3.1.2` · `call-bind-apply-helpers@1.0.2` · `call-bound@1.0.4` · `camelcase@5.3.1` · `camelcase@6.3.0`  
`chokidar@5.0.0` · `color-convert@2.0.1` · `color-name@1.1.4` · `combined-stream@1.0.8` · `confbox@0.1.8`  
`confbox@0.2.4` · `confbox@0.3.1` · `content-disposition@2.0.1` · `content-type@2.1.0` · `convert-source-map@2.0.0`  
`cookie-signature@1.2.2` · `cookie@0.7.2` · `cross-spawn@7.0.6` · `csstype@3.2.3` · `dayjs@1.11.23`  
`debug@4.4.3` · `decamelize@1.2.0` · `default-browser-id@5.0.1` · `default-browser@5.5.1` · `define-lazy-prop@3.0.0`  
`delayed-stream@1.0.0` · `depd@2.0.0` · `dijkstrajs@1.0.3` · `dunder-proto@1.0.1` · `ee-first@1.1.1`  
`element-plus@2.14.6` · `emoji-regex@8.0.0` · `encodeurl@2.0.0` · `error-stack-parser-es@1.0.5` · `es-define-property@1.0.1`  
`es-errors@1.3.0` · `es-object-atoms@1.1.2` · `es-set-tostringtag@2.1.0` · `escalade@3.2.0` · `escape-html@1.0.3`  
`escape-string-regexp@5.0.0` · `estree-walker@2.0.2` · `estree-walker@3.0.3` · `etag@1.8.1` · `express@5.3.0`  
`exsolve@1.1.1` · `fdir@6.5.0` · `finalhandler@2.1.1` · `find-up@4.1.0` · `follow-redirects@1.16.0`  
`form-data@4.0.6` · `forwarded@0.2.0` · `fresh@2.0.0` · `function-bind@1.1.2` · `gensync@1.0.0-beta.2`  
`get-intrinsic@1.3.0` · `get-proto@1.0.1` · `gopd@1.2.0` · `has-symbols@1.1.0` · `has-tostringtag@1.0.2`  
`hasown@2.0.4` · `hookable@5.5.3` · `html-tags@3.3.1` · `http-errors@2.0.1` · `https-proxy-agent@5.0.1`  
`iconv-lite@0.7.3` · `ipaddr.js@1.9.1` · `is-docker@3.0.0` · `is-fullwidth-code-point@3.0.0` · `is-in-ssh@1.0.0`  
`is-inside-container@1.0.0` · `is-promise@4.0.0` · `is-wsl@3.1.1` · `js-tokens@10.0.0` · `js-tokens@4.0.0`  
`jsesc@3.1.0` · `json-parse-even-better-errors@6.0.0` · `json5@2.2.3` · `kolorist@1.8.0` · `linkify-it@6.1.0`  
`local-pkg@1.2.1` · `locate-path@5.0.0` · `lodash-es@4.18.1` · `lodash-unified@1.0.3` · `lodash@4.18.1`  
`magic-string-ast@1.0.2` · `magic-string@0.30.21` · `magic-string@0.30.4` · `magic-string@1.4.1` · `markdown-it@15.0.2`  
`math-intrinsics@1.1.0` · `mdurl@2.1.0` · `media-typer@1.1.1` · `memoize-one@6.0.0` · `memorystream@0.3.1`  
`merge-descriptors@2.0.0` · `mime-db@1.52.0` · `mime-db@1.54.0` · `mime-types@2.1.35` · `mime-types@3.0.2`  
`mlly@1.8.2` · `mrmime@2.0.1` · `ms@2.1.3` · `muggle-string@0.4.1` · `nanoid@3.3.19`  
`negotiator@1.1.0` · `node-releases@2.0.56` · `nostics@1.3.0` · `npm-run-all2@9.0.3` · `object-inspect@1.13.4`  
`obug@2.2.1` · `ohash@2.0.12` · `on-finished@2.4.2` · `open@11.0.4` · `p-limit@2.3.0`  
`p-locate@4.1.0` · `p-try@2.2.0` · `parseurl@1.3.3` · `path-browserify@1.0.1` · `path-exists@4.0.0`  
`path-key@3.1.1` · `path-to-regexp@8.4.2` · `pathe@2.0.3` · `perfect-debounce@2.1.0` · `picomatch@4.0.7`  
`pidtree@1.0.0` · `pkg-types@1.3.1` · `pkg-types@2.3.3` · `pngjs@5.0.0` · `postcss@8.5.28`  
`powershell-utils@0.1.0` · `powershell-utils@0.2.1` · `proxy-addr@2.0.8` · `proxy-from-env@2.1.0` · `punycode.js@2.3.1`  
`qrcode@1.5.4` · `quansync@0.2.11` · `range-parser@1.3.0` · `raw-body@3.0.2` · `readdirp@5.1.1`  
`require-directory@2.1.1` · `rolldown@1.2.9` · `router@2.2.0` · `run-applescript@7.1.0` · `safer-buffer@2.1.2`  
`scule@1.3.0` · `send@1.2.1` · `serve-static@2.2.1` · `shebang-command@2.0.0` · `shebang-regex@3.0.0`  
`shell-quote@1.10.0` · `side-channel-list@1.0.1` · `side-channel-map@1.0.1` · `side-channel-weakmap@1.0.2` · `side-channel@1.1.1`  
`sirv@3.0.2` · `statuses@2.0.2` · `string-width@4.2.3` · `strip-ansi@6.0.1` · `strip-literal@4.0.0`  
`svg-tags@1.0.0` · `tinyglobby@0.2.17` · `toidentifier@1.0.1` · `totalist@3.0.1` · `type-is@2.1.0`  
`uc.micro@3.0.0` · `ufo@1.6.4` · `undici-types@7.18.2` · `unimport@6.5.0` · `unpipe@1.0.0`  
`unplugin-auto-import@21.1.0` · `unplugin-utils@0.3.2` · `unplugin-vue-components@32.1.0` · `unplugin@3.4.0` · `update-browserslist-db@1.3.3`  
`vary@1.1.2` · `vite-dev-rpc@2.0.0` · `vite-hot-client@2.2.0` · `vite-plugin-inspect@11.4.1` · `vite-plugin-vue-devtools@8.2.1`  
`vite-plugin-vue-inspector@6.0.0` · `vite@8.3.0` · `vscode-uri@3.2.0` · `vue-component-type-helpers@3.3.11` · `vue-i18n@11.4.12`  
`vue-router@5.3.1` · `vue-tsc@3.3.11` · `vue@3.5.43` · `webpack-virtual-modules@0.6.2` · `wrap-ansi@6.2.0`  
`wsl-utils@1.0.0` · `yargs@15.4.1`

**ISC · 22 项**

`ansis@4.4.0` · `cliui@6.0.0` · `electron-to-chromium@1.5.433` · `get-caller-file@2.0.5` · `inherits@2.0.4`  
`isexe@2.0.0` · `lru-cache@5.1.1` · `npm-normalize-package-bin@6.0.0` · `once@1.4.0` · `picocolors@1.1.1`  
`read-package-json-fast@6.0.0` · `require-main-filename@2.0.0` · `semver@6.3.1` · `set-blocking@2.0.0` · `setprototypeof@1.2.0`  
`which-module@2.0.1` · `which@2.0.1` · `which@7.0.0` · `wrappy@1.0.2` · `y18n@4.0.3`  
`yallist@3.1.1` · `yargs-parser@18.1.3`

**BSD-3-Clause · 4 项**

`normalize-wheel-es@1.2.0` · `qs@6.16.1` · `source-map-js@1.2.1` · `source-map-js@1.2.2`

**Apache-2.0 · 3 项**

`baseline-browser-mapping@2.11.25` · `detect-libc@2.1.2` · `typescript@6.0.3`

**BSD-2-Clause · 2 项**

`entities@7.0.1` · `entities@8.1.0`

**MPL-2.0 · 2 项**

`lightningcss-win32-x64-msvc@1.33.0` · `lightningcss@1.33.0`

**BlueOak-1.0.0 · 1 项**

`isexe@4.0.0`

**CC-BY-4.0 · 1 项**

`caniuse-lite@1.0.30001810`

**PSF-2.0 · 1 项**

`argparse@3.0.2`

</details>

### 5.2 后端 Maven artifact

<details>
<summary>展开后端依赖清单（共 187 项 / 21 类声明）</summary>

**Apache License, Version 2.0 · 130 项**

`com.baomidou:mybatis-plus-annotation@3.5.17` · `com.baomidou:mybatis-plus-core@3.5.17` · `com.baomidou:mybatis-plus-extension@3.5.17` · `com.baomidou:mybatis-plus-spring-boot-autoconfigure@3.5.17` · `com.baomidou:mybatis-plus-spring-boot-native-image@3.5.17`  
`com.baomidou:mybatis-plus-spring-boot4-starter@3.5.17` · `com.baomidou:mybatis-plus-spring@3.5.17` · `com.baomidou:mybatis-plus@3.5.17` · `com.fasterxml:classmate@1.7.3` · `com.pig4cloud.plugin:oss-spring-boot-starter@3.1.2`  
`io.netty:netty-buffer@4.2.17.Final` · `io.netty:netty-codec-base@4.2.17.Final` · `io.netty:netty-codec-compression@4.2.17.Final` · `io.netty:netty-codec-dns@4.2.17.Final` · `io.netty:netty-codec-http2@4.2.17.Final`  
`io.netty:netty-codec-http@4.2.17.Final` · `io.netty:netty-codec-marshalling@4.2.17.Final` · `io.netty:netty-codec-protobuf@4.2.17.Final` · `io.netty:netty-codec@4.2.17.Final` · `io.netty:netty-common@4.2.17.Final`  
`io.netty:netty-handler@4.2.17.Final` · `io.netty:netty-resolver-dns@4.2.17.Final` · `io.netty:netty-resolver@4.2.17.Final` · `io.netty:netty-transport-classes-epoll@4.2.17.Final` · `io.netty:netty-transport-native-unix-common@4.2.17.Final`  
`io.netty:netty-transport@4.2.17.Final` · `io.projectreactor:reactor-core@3.8.7` · `net.bytebuddy:byte-buddy-agent@1.18.11` · `net.bytebuddy:byte-buddy@1.18.11` · `org.apache.httpcomponents:httpclient@4.5.12`  
`org.apache.httpcomponents:httpcore@4.4.16` · `org.apache.tomcat.embed:tomcat-embed-core@11.0.24` · `org.apache.tomcat.embed:tomcat-embed-el@11.0.24` · `org.apache.tomcat.embed:tomcat-embed-websocket@11.0.24` · `org.objenesis:objenesis@3.3`  
`org.springframework.amqp:spring-amqp@4.1.1` · `org.springframework.amqp:spring-rabbit@4.1.1` · `org.springframework.boot:spring-boot-amqp@4.1.1` · `org.springframework.boot:spring-boot-autoconfigure@4.1.1` · `org.springframework.boot:spring-boot-data-commons@4.1.1`  
`org.springframework.boot:spring-boot-data-redis@4.1.1` · `org.springframework.boot:spring-boot-http-converter@4.1.1` · `org.springframework.boot:spring-boot-jackson@4.1.1` · `org.springframework.boot:spring-boot-jdbc@4.1.1` · `org.springframework.boot:spring-boot-mail@4.1.1`  
`org.springframework.boot:spring-boot-netty@4.1.1` · `org.springframework.boot:spring-boot-persistence@4.1.1` · `org.springframework.boot:spring-boot-resttestclient@4.1.1` · `org.springframework.boot:spring-boot-security-test@4.1.1` · `org.springframework.boot:spring-boot-security@4.1.1`  
`org.springframework.boot:spring-boot-servlet@4.1.1` · `org.springframework.boot:spring-boot-sql@4.1.1` · `org.springframework.boot:spring-boot-starter-amqp@4.1.1` · `org.springframework.boot:spring-boot-starter-data-redis@4.1.1` · `org.springframework.boot:spring-boot-starter-jackson-test@4.1.1`  
`org.springframework.boot:spring-boot-starter-jackson@4.1.1` · `org.springframework.boot:spring-boot-starter-jdbc@4.1.1` · `org.springframework.boot:spring-boot-starter-logging@4.1.1` · `org.springframework.boot:spring-boot-starter-mail@4.1.1` · `org.springframework.boot:spring-boot-starter-security-test@4.1.1`  
`org.springframework.boot:spring-boot-starter-security@4.1.1` · `org.springframework.boot:spring-boot-starter-test@4.1.1` · `org.springframework.boot:spring-boot-starter-tomcat-runtime@4.1.1` · `org.springframework.boot:spring-boot-starter-tomcat@4.1.1` · `org.springframework.boot:spring-boot-starter-validation@4.1.1`  
`org.springframework.boot:spring-boot-starter-webmvc-test@4.1.1` · `org.springframework.boot:spring-boot-starter-webmvc@4.1.1` · `org.springframework.boot:spring-boot-starter@4.1.1` · `org.springframework.boot:spring-boot-test-autoconfigure@4.1.1` · `org.springframework.boot:spring-boot-test@4.1.1`  
`org.springframework.boot:spring-boot-tomcat@4.1.1` · `org.springframework.boot:spring-boot-transaction@4.1.1` · `org.springframework.boot:spring-boot-validation@4.1.1` · `org.springframework.boot:spring-boot-web-server@4.1.1` · `org.springframework.boot:spring-boot-webmvc-test@4.1.1`  
`org.springframework.boot:spring-boot-webmvc@4.1.1` · `org.springframework.boot:spring-boot@4.1.1` · `org.springframework.data:spring-data-commons@4.1.1` · `org.springframework.data:spring-data-keyvalue@4.1.1` · `org.springframework.data:spring-data-redis@4.1.1`  
`org.springframework.security:spring-security-config@7.1.1` · `org.springframework.security:spring-security-core@7.1.1` · `org.springframework.security:spring-security-crypto@7.1.1` · `org.springframework.security:spring-security-test@7.1.1` · `org.springframework.security:spring-security-web@7.1.1`  
`org.springframework:spring-aop@7.0.9` · `org.springframework:spring-beans@7.0.9` · `org.springframework:spring-context-support@7.0.9` · `org.springframework:spring-context@7.0.9` · `org.springframework:spring-core@7.0.9`  
`org.springframework:spring-expression@7.0.9` · `org.springframework:spring-jdbc@7.0.9` · `org.springframework:spring-messaging@7.0.9` · `org.springframework:spring-oxm@7.0.9` · `org.springframework:spring-test@7.0.9`  
`org.springframework:spring-tx@7.0.9` · `org.springframework:spring-web@7.0.9` · `org.springframework:spring-webmvc@7.0.9` · `org.yaml:snakeyaml@2.6` · `software.amazon.awssdk:annotations@2.39.6`  
`software.amazon.awssdk:apache-client@2.39.6` · `software.amazon.awssdk:arns@2.39.6` · `software.amazon.awssdk:auth@2.39.6` · `software.amazon.awssdk:aws-core@2.39.6` · `software.amazon.awssdk:aws-query-protocol@2.39.6`  
`software.amazon.awssdk:aws-xml-protocol@2.39.6` · `software.amazon.awssdk:checksums-spi@2.39.6` · `software.amazon.awssdk:checksums@2.39.6` · `software.amazon.awssdk:crt-core@2.39.6` · `software.amazon.awssdk:endpoints-spi@2.39.6`  
`software.amazon.awssdk:http-auth-aws-eventstream@2.39.6` · `software.amazon.awssdk:http-auth-aws@2.39.6` · `software.amazon.awssdk:http-auth-spi@2.39.6` · `software.amazon.awssdk:http-auth@2.39.6` · `software.amazon.awssdk:http-client-spi@2.39.6`  
`software.amazon.awssdk:identity-spi@2.39.6` · `software.amazon.awssdk:json-utils@2.39.6` · `software.amazon.awssdk:metrics-spi@2.39.6` · `software.amazon.awssdk:netty-nio-client@2.39.6` · `software.amazon.awssdk:profiles@2.39.6`  
`software.amazon.awssdk:protocol-core@2.39.6` · `software.amazon.awssdk:regions@2.39.6` · `software.amazon.awssdk:retries-spi@2.39.6` · `software.amazon.awssdk:retries@2.39.6` · `software.amazon.awssdk:s3@2.39.6`  
`software.amazon.awssdk:sdk-core@2.39.6` · `software.amazon.awssdk:third-party-jackson-core@2.39.6` · `software.amazon.awssdk:utils-lite@2.39.6` · `software.amazon.awssdk:utils@2.39.6` · `software.amazon.eventstream:eventstream@1.0.1`

**The Apache Software License, Version 2.0 · 15 项**

`com.fasterxml.jackson.core:jackson-annotations@2.21` · `com.fasterxml.jackson.core:jackson-core@2.21.5` · `com.fasterxml.jackson.core:jackson-databind@2.21.5` · `com.jayway.jsonpath:json-path@2.10.0` · `com.zaxxer:HikariCP@7.0.2`  
`io.micrometer:micrometer-commons@1.17.1` · `io.micrometer:micrometer-observation@1.17.1` · `net.minidev:accessors-smart@2.6.0` · `net.minidev:json-smart@2.6.0` · `org.mybatis:mybatis-spring@4.0.0`  
`org.mybatis:mybatis@3.5.19` · `org.skyscreamer:jsonassert@1.5.3` · `org.xmlunit:xmlunit-core@2.11.0` · `tools.jackson.core:jackson-core@3.1.5` · `tools.jackson.core:jackson-databind@3.1.5`

**Eclipse Public License v2.0 · 6 项**

`org.junit.jupiter:junit-jupiter-api@6.0.3` · `org.junit.jupiter:junit-jupiter-engine@6.0.3` · `org.junit.jupiter:junit-jupiter-params@6.0.3` · `org.junit.jupiter:junit-jupiter@6.0.3` · `org.junit.platform:junit-platform-commons@6.0.3`  
`org.junit.platform:junit-platform-engine@6.0.3`

**MIT · 6 项**

`io.lettuce:lettuce-core@7.5.2.RELEASE` · `org.mockito:mockito-core@5.23.0` · `org.mockito:mockito-junit-jupiter@5.23.0` · `org.slf4j:jul-to-slf4j@2.0.18` · `org.slf4j:slf4j-api@2.0.18`  
`redis.clients.authentication:redis-authx-core@0.1.1-beta2`

**Apache-2.0 · 5 项**

`commons-codec:commons-codec@1.21.0` · `commons-logging:commons-logging@1.3.6` · `org.apache.logging.log4j:log4j-api@2.25.5` · `org.apache.logging.log4j:log4j-to-slf4j@2.25.5` · `org.assertj:assertj-core@3.27.7`

**Apache License 2.0 · 4 项**

`com.vaadin.external.google:android-json@0.0.20131108.vaadin1` · `jakarta.validation:jakarta.validation-api@3.1.1` · `org.hibernate.validator:hibernate-validator@9.1.3.Final` · `org.jboss.logging:jboss-logging@3.6.3.Final`

**The Apache License, Version 2.0 · 3 项**

`org.apiguardian:apiguardian-api@1.1.2` · `org.jspecify:jspecify@1.0.1` · `org.opentest4j:opentest4j@1.3.0`

**BSD-3-Clause · 2 项**

`org.hamcrest:hamcrest@3.0` · `org.ow2.asm:asm@9.7.1`

**EDL 1.0 · 2 项**

`jakarta.activation:jakarta.activation-api@2.1.4` · `org.eclipse.angus:angus-activation@2.0.3`

**EPL 2.0 / GPL2 w/ CPE / EDL 1.0 · 2 项**

`jakarta.mail:jakarta.mail-api@2.1.5` · `org.eclipse.angus:angus-mail@2.0.5`

**EPL-2.0 / LGPL-2.1-only · 2 项**

`ch.qos.logback:logback-classic@1.5.38` · `ch.qos.logback:logback-core@1.5.38`

**AL 2.0 / GPL v2 / MPL 2.0 · 1 项**

`com.rabbitmq:amqp-client@5.30.0`

**Apache 2 · 1 项**

`com.alibaba.fastjson2:fastjson2@2.0.64`

**Apache 2.0 · 1 项**

`org.awaitility:awaitility@4.3.0`

**Eclipse Distribution License - v 1.0 · 1 项**

`jakarta.xml.bind:jakarta.xml.bind-api@4.0.5`

**EPL 2.0 / GPL2 w/ CPE · 1 项**

`jakarta.annotation:jakarta.annotation-api@3.0.0`

**MIT-0 · 1 项**

`org.reactivestreams:reactive-streams@1.0.4`

**The BSD 3-Clause License · 1 项**

`com.warrenstrange:googleauth@1.5.0`

**The GNU General Public License, v2 with Universal FOSS Exception, v1.0 · 1 项**

`com.mysql:mysql-connector-j@9.7.0`

**The MIT License · 1 项**

`org.projectlombok:lombok@1.18.46`

**The MIT License (MIT) · 1 项**

`com.auth0:java-jwt@4.6.2`

</details>


## 6. 需要特别注意的条款

### 6.1 MySQL Connector/J：GPL-2.0 + Universal FOSS Exception v1.0

`mysql-connector-j` 是清单中唯一带 GPL 字样的依赖。Oracle 的 [FOSS License Exception](https://www.mysql.com/about/legal/licensing/foss-exception/) 明确把 **MIT License 列入 FOSS License List**，因此本项目以 MIT 开源、通过标准 JDBC 接口调用该驱动时，不会因此被要求把整个项目改为 GPL；但例外本身有条件，分发时仍应：

- 对驱动本身遵守 GPL-2.0（保留驱动随附的 LICENSE / `FOSS Notice`，不修改驱动源码后闭源分发）；
- 保证项目自身部分作为独立作品以 MIT 分发，并同时提供对应源码（本仓库即满足）；
- 不把其他 GPL 许可的作品与驱动一起并入同一衍生作品。

若未来转向闭源商业分发、或不再提供源码，需重新评估：可改用商业许可的 MySQL 驱动、替换为 LGPL 许可的 MariaDB Connector/J，或向 Oracle 取得商业授权。

### 6.2 Apache-2.0：保留 NOTICE 与变更说明

Spring Boot / Spring / Tomcat / Jackson / fastjson2 / MyBatis-Plus / TypeScript 等均为 Apache-2.0。分发其二进制或源码时，需要保留原始版权、许可与 `NOTICE` 声明；如果**修改过**这些组件的文件，需在修改文件中添加显著说明。Apache-2.0 还含明确的专利授权与终止条款，修改或再分发时请保留原文。

### 6.3 MPL-2.0：`lightningcss`（文件级 copyleft）

`lightningcss` 由 Vite 构建链引入，属于**构建期**依赖，通常不进入运行时产物。MPL-2.0 是文件级 copyleft：若修改了 MPL 覆盖的源文件并对外分发，这些文件必须以 MPL-2.0 继续提供源码；项目其余代码不受影响。正常作为工具使用、不做修改时无需额外动作。

### 6.4 CC-BY-4.0：`caniuse-lite` 数据署名

`caniuse-lite` 中的浏览器兼容性数据来自 Can I Use，采用 CC BY 4.0。该包由 browserslist 构建工具链引入，不进入前端运行时产物；若构建产物或文档中实际包含这些数据，需按要求署名。署名文案：

> This product includes browser compatibility data from Can I Use (https://caniuse.com), licensed under CC BY 4.0.

### 6.5 EPL / LGPL / EDL 及多许可组件

- `logback-classic` / `logback-core`：`EPL-2.0 / LGPL-2.1-only` 双许可，可择一遵守；未修改组件源码时按常规保留声明即可。
- `jakarta.mail-api`、`angus-mail`：`EPL-2.0 / GPL-2.0 with Classpath Exception / EDL-1.0`；`jakarta.annotation-api`：`EPL-2.0 / GPL2 w/ CPE`。Classpath Exception 使其可与本项目代码链接而不触发 GPL。
- `amqp-client`：`AL 2.0 / GPL v2 / MPL 2.0` 三许可，本项目按 Apache-2.0 选项使用。
- `junit-jupiter*`、`junit-platform-*`：EPL-2.0，仅 `test` scope，不进入发布产物。

### 6.6 不在本文件范围内的内容

本文件只覆盖包管理器解析到的代码依赖。项目内的**题库与单词数据（CET-4 / CET-6）、课程与学习资源、字体、图标、图片、截图、文档素材**，以及部署时使用的 JDK、Node.js、MySQL Server、Redis、Nginx、MinIO 等服务，均有各自独立的许可或授权要求，使用前需单独确认。README「参考资料」中引用的模板项目 `SpringBoot-Vue-Template-Jwt` 也应单独核对其许可。

## 7. 分发前的核对清单

- [ ] 随发布物同时保留根目录 `LICENSE` 与本文 `THIRD_PARTY_NOTICES.md`。
- [ ] 后端可执行 JAR / 前端 `dist` 中保留依赖自带的 `META-INF/LICENSE`、`META-INF/NOTICE` 与包内 `LICENSE` 文件。
- [ ] 若修改了 Apache-2.0 / MPL-2.0 覆盖的组件文件，按对应条款补充变更说明与源码提供方式。
- [ ] 若分发物中包含 caniuse / Can I Use 数据，保留 6.4 的署名文案。
- [ ] 若项目改为闭源分发，重新评估 MySQL Connector/J 的 GPL + FOSS Exception 适用性。
- [ ] 升级依赖后重新生成并核对本文件（见下）。

## 8. 重新生成 / 复核命令

```bash
# 前端：列出所有已安装包的许可证（含传递依赖）
cd learning-program-frontend
pnpm licenses list            # 或 npx license-checker --summary

# 后端：导出完整依赖树
cd ../learning-program-backend
mvn dependency:tree -DoutputFile=target/dependency-tree.txt

# 用 Maven 插件生成带许可证的报告（需联网下载插件）
mvn license:aggregate-add-third-party
```

常用许可证全文：MIT（https://opensource.org/license/mit）、Apache-2.0（https://www.apache.org/licenses/LICENSE-2.0）、BSD-3-Clause（https://opensource.org/license/bsd-3-clause）、ISC（https://opensource.org/license/isc）、MPL-2.0（https://www.mozilla.org/en-US/MPL/2.0/）、EPL-2.0（https://www.eclipse.org/legal/epl-2.0/）、CC BY 4.0（https://creativecommons.org/licenses/by/4.0/）。
