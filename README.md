# PixVision Server

**像素视觉后端服务** — 数字艺术创作与分享平台

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/Version-DevBeta--3.4-purple)](https://github.com/Ender-g/PixVisionServer)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 项目简介

PixVision Server 是像素视觉数字艺术平台的 Java 后端服务，提供用户管理、作品发布、社区互动、AI 内容审核等功能。项目采用前后端分离架构，配合 Vue 3 前端（PixVisionPage）和 Python 辅助服务（PixVisionPyServer）协同运行。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.0 | 主框架 |
| MyBatis-Plus | 3.5.7 | ORM |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 8.0 | 缓存 / Token 白名单 |
| Hutool | 5.8.38 | JWT / 工具集 |
| SpringDoc + Knife4j | 2.5.0 / 4.4.0 | API 文档 |
| Fastjson2 | 2.0.53 | JSON 序列化 |
| Jakarta Mail | 2.0.1 | 邮件发送 |
| OSHI | 6.6.5 | 系统监控 |

## 核心功能

- **用户系统**：邮箱验证码注册、密码登录、个人资料管理、角色权限体系（11/22/55/66/77）
- **安全认证**：JWT HS256 + Redis Token 白名单、SHA-256 多轮加密（键盘域单表代换 + 数值映射混淆）、双层拦截器链（JwtAuthenticationInterceptor + PermissionInterceptor）、@PublicAccess / @RequireRole 注解驱动权限控制
- **作品管理**：图片上传、文案编辑、审核状态流转、系列合集管理
- **社区互动**：一级/二级评论、点赞、收藏、浏览历史
- **内容审核**：AI 大模型审核（Python 辅助服务调用 DeepSeek-Chat） + 本地敏感词黑名单前置过滤（2400+ 词），三级降级策略
- **账号检测**：B 站账号真实性核验
- **消息通知**：系统消息推送与已读管理

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 8.0+
- Python 3.14+（辅助服务 PixVisionPyServer）

### 配置

1. 初始化数据库：执行 `sql/db_pix_vision-V3.5.sql`
2. 系统首次运行时会自动在 `~/.pix_vision/application.yml` 生成默认配置文件，根据需要修改其中的数据库连接、邮箱、密钥等参数：

```yaml
server:
  port: 9090

app:
  test-controller:
    enabled: false

cors:
  allowed-origin: "
    http://localhost:3000,
    http://localhost:5173,
    http://127.0.0.1:3000,
    http://127.0.0.1:5173
  "

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000
      database: 0
      password: 123456
      username: root

  mail:
    dev-mode: false
    host: smtp.qq.com
    port: 465
    from: your-email@example.com
    username: server_username
    password: your_auth_code
    protocol: smtp
    default-encoding: UTF-8
    ssl-devMode: false
    starttls-devMode: false

springdoc:
  enabled: off

logging:
  level:
    root: info
    top.playereg.pix_vision: info
  file:
    name: ${user.home}/${workspace-name}/log/pix_vision.log
    max-size: 10MB
    max-history: 30

console-output:
  enabled: false

mu-ying-secure:
  jwt-secret: 1c8b7d76b91a4f7ade75943be4c6b36a8be73191d3fef702f2838323c4928d28
  salt: www.playereg.top
  num2str: ["alice","bob","charlie","david","eve","frank","grace","henry","ivy","jack"]
```

### 运行

```bash
# 启动后端
mvn spring-boot:run

# 构建可执行 JAR
mvn clean package -DskipTests
java -jar target/PixVisionServer-DevBeta-3.4.jar
```

### 访问

| 服务 | 地址 |
|------|------|
| API 文档 | http://localhost:9090/doc.html |
| 健康检查 | http://localhost:9090/actuator/health |

### 辅助服务

Python 辅助服务 PixVisionPyServer 需单独启动（默认 8000 端口），负责 AI 内容审核和 B 站账号检测任务。详见其项目仓库。

## 项目结构

```
PixVisionServer/
├── src/main/java/top/playereg/pix_vision/
│   ├── config/              # 配置类（SecureConfig, WebConfig 等）
│   ├── controller/          # REST API 控制器
│   ├── service/             # 业务逻辑层
│   │   └── Impl/            # 业务实现（TokenWhitelistService, VerificationCodeServices 等）
│   ├── mapper/              # MyBatis-Plus Mapper 接口
│   ├── pojo/                # 实体类 / DTO / VO
│   │   └── entity/          # 数据库实体
│   ├── handler/             # 拦截器（JwtAuthenticationInterceptor, PermissionInterceptor）
│   └── util/                # 工具类（JWTUtils, StrSwitchUtils, RegexUtils 等）
│       └── Annotation/      # 自定义注解（@PublicAccess, @RequireRole）
├── src/main/resources/
│   ├── yml-config/          # 模块化配置（10 个 YML 文件按功能拆分）
│   ├── template/            # 用户配置模板
│   └── mapper/              # MyBatis XML 映射
└── sql/                     # 数据库脚本（V3.5）
```

## 数据库

初始化脚本：`sql/db_pix_vision-V3.5.sql`（13 张表）

| 表 | 说明 |
|----|------|
| tb_user | 用户账户（角色/状态/密码哈希） |
| tb_user_data | 用户扩展数据（电话/网站/微信等） |
| tb_user_data_change_lock | 用户信息变更锁（审核流程） |
| tb_works | 作品（图片/标题/审核状态） |
| tb_series | 作品系列合集 |
| tb_comments | 评论（一级/二级嵌套） |
| tb_like | 点赞关联表（用户↔作品 M:N） |
| tb_star | 收藏关联表（用户↔作品 M:N） |
| tb_history | 浏览记录关联表（用户↔作品 M:N） |
| tb_guest_history | 游客访问日志 |
| tb_content_audit_record | AI 内容审核记录（多态关联） |
| tb_messages | 系统消息（**预留，未投入使用**） |
| tb_sys_logs | 系统操作日志 |

## 配置说明

配置采用分层覆盖机制：

1. `classpath:yml-config/*.yml` — 项目内置默认值（10 个模块文件）
2. `~/.pix_vision/application.yml` — 用户自定义覆盖（优先级最高）

关键配置模块：jdbc / redis / email / cors / mybatis-plus / spring-doc / logging / spring-core / pix_vision / log-output

## 安全设计

- **双层拦截器**：JwtAuthenticationInterceptor（JWT 白名单校验）→ PermissionInterceptor（@RequireRole 角色控制）
- **密码加密**：SHA-1 预处理 → 5 轮键盘域单表代换与数值映射混淆 → SHA-256 密文入库
- **Token 管理**：Redis 白名单 + JWT exp 自然过期，改密/注销时按 userId 索引集批量清除
- **验证码防滥用**：SHA-256 哈希存储，5 分钟过期，重复发送检查，一次性消费
- **输入校验**：RegexUtils 工具类全覆盖（邮箱/用户名/密码/验证码/各种联系方式）

## 作者

- PlayerEG — gaster@vip.playereg.top
- 贡献者：blue_sky_ks

---

<div align="center">

**如果对你有帮助，请给个 Star 支持一下！**

</div>
