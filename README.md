# PixVision Server

<div align="center">

🎨 **像素视觉后端服务** - 数字艺术创作与分享平台

[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-blue?style=for-the-badge)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

**一个现代化的数字艺术平台后端服务，支持作品管理、用户互动、系统监控等功能**

</div>

---

## 📑 目录

- [✨ 核心特性](#-核心特性)
- [🛠️ 技术栈](#️-技术栈)
- [🚀 快速开始](#-快速开始)
- [📡 API 接口](#-api-接口)
- [🔐 Token 白名单机制](#-token-白名单机制)
- [🗄️ 数据库设计](#️-数据库设计)
- [⚙️ 配置说明](#️-配置说明)
- [👨‍💻 开发指南](#-开发指南)
- [❓ 常见问题](#-常见问题)
- [👤 作者](#-作者)

---

## ✨ 核心特性

### 🔐 安全认证

- JWT Token 认证与白名单机制（7天有效期）
- **Redis Set 索引优化**（批量移除 Token 性能提升 2,500x）
- RSA + AES 混合加密（支持任意数据类型）
- 邮箱验证码二次验证
- 密码 SHA-256 哈希加密存储

### 👥 用户系统

- 用户注册与登录（用户名/邮箱 + 密码）
- **模块化控制器设计**（认证、注册、密码管理、资料管理、拓展数据）
- 密码修改与重置（需邮箱二次验证）
- 用户信息管理与分页查询
- **用户拓展数据管理**（增删查批量操作）
- 多角色权限管理（普通用户/创作者/审核员/管理员）

### 📧 邮件服务

- 邮箱验证码发送与验证
- HTML 邮件模板支持
- SMTP 配置灵活定制

### 🖥️ 系统监控

- JVM 运行时信息监控
- CPU、内存、磁盘使用率监控
- Spring Boot Actuator 集成

### 🗄️ 数据管理

- MyBatis-Plus 高效 ORM
- MySQL 8.0 + Redis 8.0
- 逻辑删除与自动填充

### 🚧 开发中功能

- 作品管理（上传、编辑、删除、审核）
- 评论系统（多级评论、点赞、举报）
- 点赞与收藏功能
- 浏览历史记录
- 搜索与推荐引擎

---

## 🛠️ 技术栈

| 分类       | 技术                  | 版本            | 说明              |
|----------|---------------------|---------------|-----------------|
| 🌐 核心框架  | Spring Boot         | 3.3.0         | 快速开发框架          |
| ☕ 编程语言   | Java                | 17            | LTS 长期支持版本      |
| 🗄️ 持久层  | MyBatis-Plus        | 3.5.7         | 增强版 MyBatis     |
| 💾 数据库   | MySQL               | 8.0           | 关系型数据库          |
| ⚡ 缓存     | Redis               | 8.0           | 高性能键值存储         |
| 🔑 认证    | Hutool JWT          | 5.8.38        | JSON Web Token  |
| 🔒 加密    | Bouncy Castle       | 1.77          | RSA + AES 混合加密  |
| 📝 API文档 | SpringDoc + Knife4j | 2.5.0 / 4.4.0 | OpenAPI 3.0     |
| 📧 邮件    | Spring Mail         | 2.0.1         | Jakarta Mail 实现 |
| 🖥️ 监控   | Oshi + Actuator     | 6.6.5         | 系统信息采集          |
| 🧰 工具库   | Hutool-all          | 5.8.38        | Java 工具类库       |
| 🏷️ 代码简化 | Lombok              | -             | 注解式代码生成         |

---

## 🚀 快速开始

### 📋 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 8.0+
- SMTP 邮件服务器

### 📦 安装步骤

#### 1. 克隆项目
```bash
git clone <repository-url>
cd PixVisionServer
```

#### 2. 初始化数据库
```bash
mysql -u root -p < sql/db_pix_vision-V1.1.sql
```

#### 3. 配置应用

> ⚠️ **重要提示**：
> - 🚫 **不要直接修改** `src/main/resources/` 下的配置文件
> - ✅ **应该修改** `~/.pix_vision/application.yml` 中的配置

首次启动时，项目会自动创建 `~/.pix_vision` 目录和配置模板。

**编辑用户配置文件** `~/.pix_vision/application.yml`：

```yaml
server:
  port: 9090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your_auth_code  # 授权码，非登录密码
```

#### 4. 构建并运行

```bash
# 方式一：Maven 插件运行
mvn spring-boot:run

# 方式二：JAR 包运行
mvn clean package -DskipTests
java -jar target/PixVisionServer-0.0.1-SNAPSHOT.jar
```

### 🌐 访问地址

| 服务        | 地址                                | 说明            |
|-----------|-----------------------------------|---------------|
| 🏠 首页     | http://localhost:9090             | 欢迎页面          |
| 💚 健康检查   | http://localhost:9090/health      | 服务状态          |
| 🖥️ 系统信息  | http://localhost:9090/system-info | JVM/OS/CPU 信息 |
| 📚 API 文档 | http://localhost:9090/doc.html    | Knife4j 增强版   |

---

## 📡 API 接口

### 🔐 用户认证 `/api/user/auth`

| 方法   | 路径        | 说明             | 认证 |
|------|-----------|----------------|:--:|
| POST | `/login`  | 用户登录（支持用户名/邮箱） | ❌  |
| POST | `/logout` | 用户登出（移除 Token） | ✅  |

### 📝 用户注册 `/api/user/register`

| 方法   | 路径  | 说明   | 认证 |
|------|-----|------|:--:|
| POST | `/` | 用户注册 | ❌  |

### 🔑 密码管理 `/api/user/password`

| 方法   | 路径        | 说明        | 认证 |
|------|-----------|-----------|:--:|
| POST | `/change` | 修改密码（需登录） | ✅  |
| POST | `/forgot` | 忘记密码重置    | ❌  |

### 👤 用户资料 `/api/user/profile`

| 方法   | 路径                       | 说明       | 认证 |
|------|--------------------------|----------|:--:|
| GET  | `/page/{current}/{size}` | 分页查询用户信息 | ✅  |
| POST | `/nickname`              | 修改用户昵称   | ✅  |

### 📊 用户拓展数据 `/api/user/data`

| 方法   | 路径              | 说明       | 认证 |
|------|-----------------|----------|:--:|
| POST | `/add`          | 新增拓展数据   | ✅  |
| GET  | `/list`         | 查询拓展数据列表 | ❌  |
| POST | `/delete`       | 删除单条拓展数据 | ✅  |
| POST | `/batch-delete` | 批量删除拓展数据 | ✅  |

**拓展数据特性：**
- ✅ 支持多种数据类型（电话、邮箱、网站、微信、QQ 等）
- ✅ 数据名称长度限制：**不超过 26 个字符**
- ✅ 数据内容长度限制：**不超过 96 个字符**
- ✅ 同一用户可添加多条拓展数据（1 对 n 关系）
- ✅ **权限控制**：只能删除自己的数据
- ✅ **批量操作**：支持一次性删除多条数据

### 📧 邮件服务 `/api/mail`

| 方法   | 路径                 | 说明      | 认证 |
|------|--------------------|---------|:--:|
| POST | `/send-email-code` | 发送邮箱验证码 | ❌  |

### 🖼️ 图片服务 `/api/image`

| 方法   | 路径               | 说明         | 认证 |
|------|------------------|------------|:--:|
| GET  | `/get/avatar`    | 获取头像图片     | ❌  |
| GET  | `/get/works`     | 获取作品图片     | ❌  |
| GET  | `/get/logo`      | 获取 Logo 图片 | ❌  |
| POST | `/upload/avatar` | 上传用户头像     | ✅  |

---

## 🔐 Token 白名单机制

### 📊 架构设计

采用 **JWT + Redis 白名单** 双重验证机制，引入 **Set 索引结构** 优化性能。

**Redis 数据结构：**
```
1. Token 白名单（String）
   Key: token:whitelist:{token}
   Value: {userId}:{username}
   TTL: 7天

2. Token 索引集合（Set）✨ 性能优化
   Key: token:index:{userId}
   Value: Set<token1, token2, ...>
   TTL: 7天
```

### ⚡ 性能优化对比

**场景：用户修改密码，强制所有设备下线**（10,000 个活跃 Token，目标用户 3 个设备）

| 指标           | 优化前（KEYS） | 优化后（Set 索引） | 提升             |
|--------------|-----------|-------------|----------------|
| 扫描 Token 数   | 10,000 个  | 3 个         | **3,333x** ⚡   |
| Redis GET 次数 | 10,000 次  | 0 次         | **∞** ⚡⚡⚡      |
| 总耗时          | ~10,050ms | ~4ms        | **2,500x** ⚡⚡⚡ |
| 时间复杂度        | O(N)      | O(M)        | N >> M         |

### 🔄 工作流程

#### 1. 登录时添加到白名单
```java
String token = JWTUtils.createToken(userId, username);
tokenWhitelistService.

addToWhitelist(token, userId, username, expireTime);

// Redis:
// SET token:whitelist:{token} "{userId}:{username}" EX 604800
// SADD token:index:{userId} {token}
```

#### 2. 请求时验证白名单
```java
if(!tokenWhitelistService.isInWhitelist(token)){
  return false; // Token 不在白名单中，拒绝访问
  }

// Redis: EXISTS token:whitelist:{token}
```

#### 3. 批量移除用户所有 Token ⚡ 核心优化点
```java
int removedCount = tokenWhitelistService.removeAllUserTokens(userId, username);

// Redis:
// SMEMBERS token:index:{userId} → 直接获取用户的所有 Token（O(1)）
// DEL token:whitelist:{token1}, token:whitelist:{token2}, ...
// DEL token:index:{userId}
```

---

## 🗄️ 数据库设计

### 👥 用户角色体系

| 角色代码 | 角色名称  | 权限说明        |
|:----:|-------|-------------|
|  11  | 普通用户  | 浏览、评论、点赞、收藏 |
|  22  | 创作者   | 发布作品、管理作品   |
|  55  | 审核员   | 审核作品、处理举报   |
|  66  | 工单管理员 | 处理用户工单      |
|  77  | 系统管理员 | 全部权限        |

### 📊 用户状态

| 状态代码 | 状态名称  | 说明       |
|:----:|-------|----------|
|  10  | ✅ 正常  | 正常使用所有功能 |
|  20  | ⚠️ 冻结 | 暂时限制部分功能 |
|  30  | 🚫 封禁 | 禁止登录和使用  |

### 📋 数据表清单

| 表名             | 中文名    | 主要字段                                        | 状态 |
|----------------|--------|---------------------------------------------|:--:|
| `tb_user`      | 用户账户   | id, username, email, password, role, status | ✅  |
| `tb_user_data` | 用户扩展数据 | user_id, data_name, data_content            | ✅  |
| `tb_works`     | 作品     | id, title, description, author_id           | 🚧 |
| `tb_comments`  | 评论     | id, content, user_id, works_id              | 🚧 |
| `tb_like`      | 点赞     | id, user_id, target_id                      | 🚧 |
| `tb_star`      | 收藏     | id, user_id, works_id                       | 🚧 |

> ✅ 已完成 | 🚧 开发中

---

## ⚙️ 配置说明

### 📌 配置优先级

```
🔝 最高优先级  ~/.pix_vision/application.yml          # 用户自定义配置（✅ 推荐）
   ↓
🔧 中等优先级  src/main/resources/yml-config/*.yml    # 核心配置模板（🚫 不建议修改）
   ↓
📄 最低优先级  src/main/resources/application.yml     # 基础配置（🚫 不建议修改）
```

### 📁 运行时目录结构

```
~/.pix_vision/
├── application.yml          # 用户自定义配置
├── data/                    # 数据文件
│   ├── logo-img/           # Logo 图片
│   └── avatar/             # 用户头像
├── config/                  # 配置文件
│   └── email-html/         # 邮件 HTML 模板
├── log/                     # 日志文件
│   ├── pix_vision.log      # 应用日志
│   └── error.log           # 错误日志
└── key/                     # 密钥文件
    └── rsa/                # RSA 密钥对
        ├── public.key      # RSA 公钥
        └── private.key     # RSA 私钥
```

### 🔒 RSA 加密工具

项目集成了强大的 RSA 加密工具，支持**任意类型和大小**的数据加密。

**使用示例：**
```java
// 加密字符串
String encrypted = RSACipher.encryptToBase64("敏感信息");

// 解密
String decrypted = RSACipher.decryptToString(encrypted);
```

📖 **详细文档**：查看 [RSA工具使用指南.md](doc/RSA工具使用指南.md)

---

## 👨‍💻 开发指南

### 📝 新增接口流程

1. **设计接口** - 确定 URL 路径、请求方式、参数
2. **创建 POJO** - 在 `pojo/` 目录创建实体类
3. **编写 Service** - 定义接口并实现业务逻辑
4. **创建 Controller** - 添加 REST API 端点和 Swagger 文档
5. **编写测试** - 覆盖正常流程和边界情况
6. **代码审查** - 检查规范和注释

### 📐 代码规范

- **命名**：类名大驼峰，方法名小驼峰，常量全大写
- **注释**：所有 public 方法必须有 JavaDoc
- **缩进**：使用 4 个空格（不使用 Tab）
- **注入**：优先使用构造器注入（`@RequiredArgsConstructor`）
- **日志**：使用 SLF4J，敏感信息不记录明文

### 🧪 测试指南

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=RSACipherTest

# 运行指定测试方法
mvn test -Dtest=RSACipherTest#testSmallTextEncryption
```

---

## ❓ 常见问题

### 1️⃣ API 文档无法访问

**问题**：访问 `http://localhost:9090/doc.html` 显示 404

**解决**：检查 `src/main/resources/yml-config/spring-doc.yml`，确保：
```yaml
springdoc:
  enabled: on
```

### 2️⃣ 邮件发送失败

**排查步骤**：

1. 确认使用**授权码**而非登录密码
2. 检查 SMTP 服务是否开启
3. 验证端口和主机配置是否正确

**常见配置**：

```yaml
# QQ 邮箱
mail:
  host: smtp.qq.com
  port: 465
  ssl-enable: true

# Gmail
mail:
  host: smtp.gmail.com
  port: 587
  starttls-enable: true
```

### 3️⃣ Redis 连接失败

**解决**：

```bash
# 确认 Redis 服务已启动
redis-cli ping  # 应返回 PONG

# 检查配置文件
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
```

### 4️⃣ 数据库连接失败

**解决**：

```bash
# 确认 MySQL 服务已启动
sudo systemctl status mysql

# 创建数据库
CREATE DATABASE db_pix_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入脚本
mysql -u root -p db_pix_vision < sql/db_pix_vision-V1.1.sql
```

---

## 👤 作者

**PlayerEG**
- 📧 Email: gaster@vip.playereg.top
- 🌐 GitHub: [@PlayerEG](https://github.com/Ender-g)

**贡献者：**
- blue_sky_ks

---

## 🤝 参与贡献

我们欢迎任何形式的贡献！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**

Made with ❤️ by PlayerEG

</div>
