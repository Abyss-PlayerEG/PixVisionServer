# PixVision Server

**像素视觉后端服务** — 数字艺术创作与分享平台

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/Version-V4.1.1-purple)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 项目简介

PixVision Server 是像素视觉数字艺术平台的 Java 后端服务，提供用户管理、作品发布、社区互动、AI 内容审核、实时消息推送等功能。项目采用前后端分离架构，配合 Vue 3 前端（PixVisionPage）和 Python 辅助服务（PixVisionPyServer）协同运行。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.0 | 主框架 |
| WebSocket | 6.x | 实时通信 |
| MyBatis-Plus | 3.5.7 | ORM |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 8.0 | 缓存 / Token 白名单 / 会话管理 |
| Hutool | 5.8.38 | JWT / 工具集 |
| SpringDoc + Knife4j | 2.5.0 / 4.4.0 | API 文档 |
| Fastjson2 | 2.0.53 | JSON 序列化 |
| Jakarta Mail | 2.0.1 | 邮件发送 |
| Bouncy Castle | 1.77 | RSA 加密库 |
| CommonMark | 0.21.0 | Markdown 解析 |
| Spring AOP | 6.x | 面向切面编程（日志记录） |
| Spring Actuator | 3.3.0 | 应用监控与健康检查 |

## 核心功能

- **用户系统**：邮箱验证码注册、密码登录、个人资料管理、角色权限体系（11/22/55/66/77）
- **安全认证**：JWT + Redis Token 白名单、SHA-256 多轮加密、双层拦截器链、注解驱动权限控制
- **作品管理**：图片上传、文案编辑、审核状态流转、系列合集管理
- **社区互动**：一级/二级评论、点赞、收藏、浏览历史
- **内容审核**：AI 大模型审核 + 本地敏感词黑名单前置过滤（2400+ 词），三级降级策略
- **消息通知**：系统消息推送、私信、WebSocket 实时通知、消息撤销（2 分钟内）、RSA-2048 加密存储
- **日志系统**：操作日志记录（@LogRecord 注解驱动）、日志持久化到数据库

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 8.0+
- Python 3.14+（辅助服务 PixVisionPyServer）

### 配置

1. 初始化数据库：执行 `sql/db_pix_vision-V4.1.1.sql`
2. 系统首次运行时会自动在 `~/.pix_vision/application.yml` 生成默认配置文件，根据需要修改：

```yaml
server:
  port: 9090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456

  data:
    redis:
      host: localhost
      port: 6379
      password: 123456

  mail:
    host: smtp.qq.com
    port: 465
    from: your-email@example.com
    username: server_username
    password: your_auth_code
```

### 运行

```bash
# 启动后端
mvn spring-boot:run

# 构建可执行 JAR
mvn clean package -DskipTests
java -jar target/PixVisionServer-V4.1.1.jar
```

### 访问

| 服务 | 地址 |
|------|------|
| API 文档 | http://localhost:9090/doc.html |
| 健康检查 | http://localhost:9090/actuator/health |
| WebSocket | ws://localhost:9090/api/ws/notification?token={JWT_TOKEN} |

## API 概览

### 用户与认证

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 用户注册 |
| `/api/user/login` | POST | 用户登录 |
| `/api/user/info` | GET | 获取当前用户信息 |
| `/api/user/profile/{id}` | GET | 获取用户主页 |

### 作品管理

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/works/publish` | POST | 发布作品 |
| `/api/works/detail/{id}` | GET | 获取作品详情 |
| `/api/works/list/{current}/{size}` | GET | 分页获取作品列表 |
| `/api/works/series/{id}` | GET | 获取系列作品 |

### 社区互动

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/comments/add` | POST | 发表评论 |
| `/api/comments/list/{workId}/{current}/{size}` | GET | 获取评论列表 |
| `/api/like/toggle/{workId}` | POST | 点赞/取消点赞 |
| `/api/star/toggle/{workId}` | POST | 收藏/取消收藏 |

### 消息系统

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/message/unread-count` | GET | 获取未读消息数量 |
| `/api/message/conversations/{current}/{size}` | GET | 分页查询会话列表 |
| `/api/message/chat/{otherUserId}/{current}/{size}` | GET | 分页查询聊天记录 |
| `/api/message/send` | POST | 发送私信 |
| `/api/message/recall/{messageId}` | POST | 撤销消息（2 分钟内） |

### 管理员接口

| 模块 | 基础路径 | 主要功能 | 权限 |
|------|----------|----------|------|
| 用户管理 | `/api/admin/user` | 批量更新、删除、重置密码、分页查询 | [55, 77] |
| 作品管理 | `/api/admin/works` | 批量审核、删除、更新、分页查询 | [55, 77] |
| 评论管理 | `/api/admin/comments` | 批量删除、审核状态、分页查询 | [55, 77] |
| 系列管理 | `/api/admin/series` | 批量审核、删除、更新、分页查询 | [55, 77] |
| 消息管理 | `/api/admin/message` | RSA 密钥轮换、管理员查询聊天记录 | [77] |
| 日志管理 | `/api/admin/logs` | 分页查询操作日志 | [77] |

## 项目结构

```
PixVisionServer/
├── src/main/java/top/playereg/pix_vision/
│   ├── config/              # 配置类
│   ├── controller/          # REST API 控制器
│   │   └── admin/           # 管理员接口
│   ├── service/             # 业务逻辑层
│   │   └── Impl/            # 业务实现
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── pojo/                # 实体类 / DTO / VO
│   ├── handler/             # 拦截器 + WebSocket 处理器
│   ├── manager/             # WebSocket 会话管理器
│   ├── enums/               # 枚举类
│   ├── scheduler/           # 定时任务
│   └── util/                # 工具类
│       └── Annotation/      # 自定义注解
├── src/main/resources/
│   ├── yml-config/          # 模块化配置
│   └── mapper/              # MyBatis XML 映射
├── doc/                     # 项目文档
├── scripts/                 # 实用脚本
├── sql/                     # 数据库脚本
└── pom.xml
```

## 数据库

初始化脚本：`sql/db_pix_vision-V4.1.1.sql`（13 张表）

| 表 | 说明 |
|----|------|
| tb_user | 用户账户 |
| tb_user_data | 用户扩展数据 |
| tb_user_data_change_lock | 用户信息变更锁 |
| tb_works | 作品 |
| tb_series | 作品系列合集 |
| tb_comments | 评论 |
| tb_like | 点赞关联表 |
| tb_star | 收藏关联表 |
| tb_history | 浏览记录 |
| tb_guest_history | 游客访问日志 |
| tb_content_audit_record | AI 内容审核记录 |
| tb_messages | 系统消息与私信 |
| tb_sys_logs | 系统操作日志 |

## 安全设计

- **双层拦截器**：JwtAuthenticationInterceptor → PermissionInterceptor
- **密码加密**：SHA-1 预处理 → 5 轮键盘域单表代换与数值映射混淆 → SHA-256 密文入库
- **Token 管理**：Redis 白名单 + JWT exp 自然过期
- **RSA 加密**：私信内容使用 RSA-2048 加密存储，支持密钥轮换

## 相关项目

| 项目 | 说明 | 仓库 |
|------|------|------|
| **PixVisionPage** | Vue 3 前端应用 | [Gitee](https://gitee.com/endergaster_geek/PixVisionPage) |
| **PixVisionPyServer** | Python 辅助服务（FastAPI） | [Gitee](https://gitee.com/endergaster_geek/PixVisionPyServer) |

## 作者

- PlayerEG — gaster@vip.playereg.top
- 贡献者：blue_sky_ks

## 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。
