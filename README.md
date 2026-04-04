# PixVision Server

> 像素视觉后端服务 - 数字艺术/图片平台后端（类似 Pixiv 风格）

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**当前状态**: 🚧 开发中 (DEV-2.0.0)

---

## 功能特性

- ✅ 用户注册与登录（用户名/邮箱 + 密码）
- ✅ JWT Token 认证与白名单机制
- ✅ 邮箱验证码发送与验证
- ✅ 密码修改（需邮箱二次验证）
- ✅ 分页用户查询
- ✅ 系统信息监控（JVM、OS、CPU、内存、磁盘）
- ✅ 健康检查接口
- 🚧 作品管理（TODO）
- 🚧 评论系统（TODO）
- 🚧 点赞与收藏（TODO）
- 🚧 浏览历史记录（TODO）
- 🚧 作品系列/合集（TODO）

---

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 17 |
| **框架** | Spring Boot | 3.3.0 |
| **ORM** | MyBatis-Plus | 3.5.7 |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis | 8.0 (Lettuce) |
| **JWT** | Hutool JWT | 5.8.38 |
| **API 文档** | SpringDoc OpenAPI + Knife4j | 2.5.0 / 4.4.0 |
| **邮件** | Spring Boot Mail + Jakarta Mail | 2.0.1 |
| **加密** | Bouncy Castle | 1.77 |
| **系统监控** | Oshi + Spring Boot Actuator | 6.6.5 |
| **工具库** | Hutool-all、Lombok | 5.8.38 |

---

## 项目结构

```
src/main/java/top/playereg/pix_vision/
├── config/              # 配置类
├── controller/          # REST 控制器
├── service/             # 服务接口
│   └── Impl/           # 服务实现
├── mapper/              # MyBatis Mapper
├── pojo/                # 实体类与 DTO
├── handler/             # 拦截器
├── util/                # 工具类
├── enums/               # 枚举
└── egg/                 # 彩蛋

src/main/resources/
├── yml-config/          # YAML 配置（数据库、Redis、邮件等）
├── mapper/              # MyBatis XML 映射
├── static/              # 静态资源
└── template/            # 模板文件（邮件模板等）
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 8.0+
- SMTP 邮件服务器

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd PixVisionServer
   ```

2. **初始化数据库**
   ```bash
   mysql -u root -p < sql/db_pix_vision-V1.1.sql.sql
   ```

3. **修改配置**

   编辑以下配置文件：

   - `src/main/resources/yml-config/jdbc.yml` - MySQL 连接信息
   - `src/main/resources/yml-config/redis.yml` - Redis 连接信息
   - `src/main/resources/yml-config/email.yml` - SMTP 邮件配置

4. **构建项目**
   ```bash
   mvn clean package -DskipTests
   ```

5. **运行**
   ```bash
   java -jar target/PixVision-0.0.1-SNAPSHOT.jar
   ```

   或在 IDE 中直接运行 `PixVisionApplication.main()`

### 访问地址

| 服务 | 地址 |
|------|------|
| 首页 | http://localhost:9090 |
| 健康检查 | http://localhost:9090/health |
| 系统信息 | http://localhost:9090/system-info |
| Swagger UI | http://localhost:9090/swagger-ui.html |
| Knife4j UI | http://localhost:9090/doc.html |

> **注意**: API 文档默认关闭，需在 `spring-doc.yml` 中设置 `springdoc.enabled: on` 启用。

---

## API 接口

### 用户接口 `/api/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 否 |
| POST | `/api/user/login` | 用户登录 | 否 |
| POST | `/api/user/logout` | 用户登出 | 是 |
| POST | `/api/user/changepassword` | 修改密码 | 是 |
| GET | `/api/user/page/{current}/{size}` | 分页查询用户 | 是 |

### 邮件接口 `/api/mail`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/mail/send-email-code` | 发送邮箱验证码 | 否 |
| POST | `/api/mail/verify-email-code-test` | 验证邮箱验证码（测试） | 否 |

### 系统接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/` | 首页重定向 | 否 |
| GET | `/health` | 健康检查 | 否 |
| GET | `/system-info` | 系统信息 | 否 |

### 认证说明

- Token 有效期：**7 天**
- 传递方式：
  - Header: `Authorization: Bearer <token>`
  - URL 参数: `?token=<token>`
- 登出或修改密码后 Token 自动失效

---

## 数据库设计

### 用户角色

| 角色代码 | 说明 |
|---------|------|
| 11 | 普通用户 |
| 22 | 创作者 |
| 55 | 审核员 |
| 66 | 工单管理员 |
| 77 | 系统管理员 |

### 用户状态

| 状态代码 | 说明 |
|---------|------|
| 10 | 正常 |
| 20 | 冻结 |
| 30 | 封禁 |

### 数据表

| 表名 | 说明 |
|------|------|
| `tb_user` | 用户账户 |
| `tb_user_data` | 用户扩展数据 |
| `tb_works` | 作品 |
| `tb_series` | 作品系列 |
| `tb_comments` | 评论 |
| `tb_like` | 点赞 |
| `tb_star` | 收藏 |
| `tb_history` | 浏览历史 |
| `tb_sys_logs` | 系统日志 |

---

## 配置说明

### 配置优先级

1. `src/main/resources/application.yml`（基础配置）
2. `src/main/resources/yml-config/*.yml`（核心配置）
3. `~/.pix_vision/application.yml`（用户自定义，最高优先级）

### 运行时目录

项目启动时会在 `~/.pix_vision/` 下创建以下目录：

```
~/.pix_vision/
├── data/          # 数据文件
├── config/        # 配置文件
├── log/           # 日志文件
├── key/           # 密钥文件
├── data/logo-img/ # Logo 图片
├── data/avatar/   # 头像
└── config/email-html/ # 邮件 HTML 模板
```

---

## 开发指南

### 新增接口流程

1. 设计接口（URL、请求方式、参数、返回值）
2. 创建/更新 POJO
3. 编写 Service 接口与实现
4. 创建 Controller，添加 Swagger 文档
5. 编写测试
6. 代码审查与提交

### 代码规范

- 使用 4 个空格缩进
- 所有 public 方法必须有 JavaDoc
- 使用 Lombok 简化代码
- 优先使用构造器注入（`@RequiredArgsConstructor`）
- 异常处理优先返回错误响应，而非抛出异常

---

## 常见问题

### API 文档无法访问

检查 `src/main/resources/yml-config/spring-doc.yml`，确保：
```yaml
springdoc:
  enabled: on
```

### 邮件发送失败

检查 `email.yml` 配置：
- SMTP 主机和端口是否正确
- 用户名和授权码是否配置
- SSL 设置是否匹配邮件服务器要求

### Redis 连接失败

确保 Redis 服务已启动，并检查 `redis.yml` 中的连接配置。

---

## 许可证

MIT License

---

## 作者

**PlayerEG**

---

> 💡 像素视觉开发中。。。。。。
