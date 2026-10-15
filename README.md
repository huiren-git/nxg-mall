# NXG Mall（电商平台 + 实时运营/风控分析）

NXG Mall 是一个包含「业务后端 + 数据分析后端 + 前端」的电商平台示例项目，覆盖商品/订单/优惠券/购物车等核心业务能力，并通过 Kafka + Flink 构建实时埋点与审计分析链路，为运营看板与安全风控提供数据支撑。

## 功能概览

- 账号体系：注册/登录、JWT 无状态鉴权、角色权限控制（Customer / Sales / Admin）
- 安全能力：验证码/防刷、登录错误熔断锁定、重置密码 Token（过期 + 用后即焚防回放）
- 业务能力：商品/类目/购物车/订单/优惠券等（按模块拆分实现）
- 审计与埋点：AOP 采集操作日志异步投递 Kafka（审计 Topic）
- 实时分析：Flink 消费 Kafka 进行 PV/UV、热词、用户偏好、异常高频操作等实时计算并落库，驱动分析看板
- 静态资源：支持商品图片等上传资源通过 `/api/uploads/**` 映射访问

## 技术栈（后端重点）

- 语言与框架：Java 17、Spring Boot 3.3（多模块）、Spring Security、JWT
- 数据访问：MyBatis-Plus、MySQL
- 缓存与消息：Redis、Kafka
- 流式计算：Flink（支持 Checkpoint）
- 其他：Thymeleaf 邮件模板、ip2region、Fastjson2、Hutool
- 前端：React（CRA）、Ant Design、Axios、ECharts

## 项目结构

```
nxg/
├─ nxg-back/                 # 后端（Maven 多模块）
│  ├─ nxg-common/            # 通用模块：VO/Entity/工具类/拦截器等
│  ├─ nxg-api/               # 业务接口服务（电商主业务）
│  └─ nxg-analytics/         # 分析服务（实时分析 API + 内置 Flink 作业启动）
└─ nxg-front/                # 前端（用户端/销售端/管理端）
```

## 角色功能演示视频

以下视频位于项目根目录的 `video/` 文件夹，可用于快速了解三类角色的主要操作流程。

| 角色 | 演示内容 | 视频 |
| --- | --- | --- |
| Customer（顾客） | 登录、浏览商品、购物车、下单与支付等顾客端流程 | [观看顾客端演示](video/compressed/nxg-customer.mp4) |
| Sales（销售） | 商品维护、订单处理与发货等销售端流程 | [观看销售端演示](video/compressed/nxg-sales.mp4) |
| Admin（管理员） | 用户/订单管理、运营看板与数据分析等后台流程 | [观看管理员端演示](video/compressed/nxg-admin.mp4) |

## 运行前准备

- JDK 17、Maven 3.9+（或使用项目自带 Maven Wrapper）
- Node.js 18+、npm
- MySQL 8+（至少两个库：`nxg`、`nxg_analytics`）
- Redis、Kafka

说明：
- 本仓库默认配置偏向容器/内网域名（如 `nxg-mysql` / `nxg-redis` / `nxg-kafka`）。本地启动时请将配置改为你的实际地址（如 `localhost`）。
- 请勿将邮箱口令、JWT 密钥等敏感配置直接提交到公共仓库；建议使用环境变量或私有配置覆盖。

## 快速开始（本地开发）

### 1) 启动后端：nxg-api（业务服务）

默认端口与上下文：
- `http://localhost:8080/api`

运行（Windows）：

```bash
cd nxg-back
.\mvnw.cmd -pl nxg-api -am spring-boot:run
```

运行（macOS/Linux）：

```bash
cd nxg-back
./mvnw -pl nxg-api -am spring-boot:run
```

关键配置位置：
- `nxg-back/nxg-api/src/main/resources/application.yml`

### 2) 启动后端：nxg-analytics（分析服务）

默认端口与上下文：
- `http://localhost:8082/api/analytics`

运行（Windows）：

```bash
cd nxg-back
.\mvnw.cmd -pl nxg-analytics -am spring-boot:run
```

说明：
- 分析服务内包含作业启动器，会在 Spring Boot 启动后异步拉起多个 Flink Job（用于消费 Kafka 并将指标落库）。
- 关键配置位置：`nxg-back/nxg-analytics/src/main/resources/application.yml`

### 3) 启动前端：nxg-front

```bash
cd nxg-front
npm ci
npm start
```

前端默认开发端口：
- `http://localhost:3000`

关于接口代理：
- 前端请求使用相对路径 `/api` 与 `/api/analytics`。
- 推荐通过反向代理（如 Nginx）或本地代理将：
  - `/api` → `http://localhost:8080/api`
  - `/api/analytics` → `http://localhost:8082/api/analytics`

## 端口与路由速查

- nxg-api：`8080`，context-path：`/api`
  - 静态资源：`/api/uploads/**`
- nxg-analytics：`8082`，context-path：`/api/analytics`
- nxg-front：`3000`

## 关键实现点（便于阅读代码）

- JWT 鉴权与 RBAC：`nxg-back/nxg-common/.../JwtAuthenticationFilter.java`、`nxg-back/nxg-api/.../SecurityConfig.java`
- 审计日志 AOP → Kafka：`nxg-back/nxg-api/.../aspect/LogAspect.java`
- 防爬/限流拦截器：`nxg-back/nxg-common/.../AntiCrawlerInterceptor.java`、`ProductRateLimitInterceptor.java`
- Flink 实时分析 Job：`nxg-back/nxg-analytics/.../job/*Job.java`
