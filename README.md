# PixVision Server

> 🎨 像素视觉后端服务 - 数字艺术创作与分享平台

<div align="center">

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-blue.svg?style=flat-square)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Dev--2.0.0-blueviolet.svg?style=flat-square)]()

**一个现代化的数字艺术平台后端服务，支持作品管理、用户互动、系统监控等功能**

</div>

---

## ✨ 核心特性

### 🔐 安全认证
- ✅ JWT Token 认证与白名单机制（7天有效期）
- ✅ **Redis Set 索引优化**（批量移除 Token 性能提升 2,500x）
- ✅ RSA + AES 混合加密（支持任意数据类型）
- ✅ 邮箱验证码二次验证
- ✅ 密码 SHA-256 哈希加密存储
- ✅ 请求拦截器与权限控制

### 👥 用户系统
- ✅ 用户注册与登录（用户名/邮箱 + 密码）
- ✅ 密码修改（需邮箱二次验证）
- ✅ 忘记密码（无需登录即可重置）
- ✅ 用户信息管理
- ✅ 分页用户查询
- ✅ 多角色权限管理（普通用户/创作者/审核员/管理员）

### 📧 邮件服务
- ✅ 邮箱验证码发送与验证
- ✅ HTML 邮件模板支持
- ✅ SMTP 配置灵活定制

### 🖥️ 系统监控
- ✅ JVM 运行时信息监控
- ✅ 操作系统信息采集
- ✅ CPU、内存、磁盘使用率监控
- ✅ 健康检查接口
- ✅ Spring Boot Actuator 集成

### 🗄️ 数据管理
- ✅ MyBatis-Plus 高效 ORM
- ✅ MySQL 8.0 数据库
- ✅ Redis 8.0 缓存支持
- ✅ 逻辑删除与自动填充

### 🚧 开发中功能
- 🔄 作品管理（上传、编辑、删除、审核）
- 🔄 评论系统（多级评论、点赞、举报）
- 🔄 点赞与收藏功能
- 🔄 浏览历史记录
- 🔄 作品系列/合集管理
- 🔄 文件存储（本地/OSS）
- 🔄 搜索与推荐引擎

---

## 🛠️ 技术栈

<div align="center">

| 分类 | 技术 | 版本 | 说明 |
|:---:|------|:---:|------|
| 🌐 **核心框架** | Spring Boot | 3.3.0 | 快速开发框架 |
| ☕ **编程语言** | Java | 17 | LTS 长期支持版本 |
| 🗄️ **持久层** | MyBatis-Plus | 3.5.7 | 增强版 MyBatis |
| 💾 **数据库** | MySQL | 8.0 | 关系型数据库 |
| ⚡ **缓存** | Redis | 8.0 | 高性能键值存储 |
| 🔑 **认证** | Hutool JWT | 5.8.38 | JSON Web Token |
| 🔒 **加密** | Bouncy Castle | 1.77 | RSA + AES 混合加密 |
| 📝 **API文档** | SpringDoc + Knife4j | 2.5.0 / 4.4.0 | OpenAPI 3.0 |
| 📧 **邮件** | Spring Mail | 2.0.1 | Jakarta Mail 实现 |
| 🖥️ **监控** | Oshi + Actuator | 6.6.5 | 系统信息采集 |
| 🧰 **工具库** | Hutool-all | 5.8.38 | Java 工具类库 |
| 🏷️ **代码简化** | Lombok | - | 注解式代码生成 |

</div>

---

## 📂 项目结构

```
PixVisionServer/
├── 📁 src/main/java/top/playereg/pix_vision/
│   ├── 📁 config/              # 配置类（Web、Redis、JWT、Swagger等）
│   ├── 📁 controller/          # REST API 控制器
│   ├── 📁 service/             # 业务逻辑层
│   │   └── 📁 Impl/           # 服务实现类
│   ├── 📁 mapper/              # MyBatis Mapper 接口
│   ├── 📁 pojo/                # 实体类与 DTO
│   │   └── 📁 userPojo/       # 用户相关 POJO
│   ├── 📁 handler/             # 拦截器与处理器
│   ├── 📁 util/                # 工具类
│   │   ├── 📁 Aspect/         # AOP 切面（日志记录）
│   │   ├── RSACipher.java     # RSA 加密工具
│   │   ├── JWTUtils.java      # JWT 工具
│   │   └── ...
│   ├── 📁 enums/               # 枚举类
│   └── 📁 egg/                 # 彩蛋功能
│
├── 📁 src/main/resources/
│   ├── 📁 yml-config/          # YAML 配置文件
│   │   ├── jdbc.yml           # 数据库配置
│   │   ├── redis.yml          # Redis 配置
│   │   ├── email.yml          # 邮件配置
│   │   ├── mybatis-plus.yml   # MyBatis-Plus 配置
│   │   ├── spring-doc.yml     # API 文档配置
│   │   ├── logging.yml        # 日志配置
│   │   └── pix_vision.yml     # 应用自定义配置
│   ├── 📁 mapper/              # MyBatis XML 映射文件
│   ├── 📁 static/              # 静态资源（HTML、CSS、图片）
│   ├── 📁 template/            # 模板文件（邮件HTML、配置模板）
│   ├── 📁 logo/                # Logo 图片资源
│   ├── application.yml         # 主配置文件
│   └── banner.txt              # 启动 Banner
│
├── 📁 sql/                     # 数据库脚本
├── 📁 doc/                     # 项目文档
├── 📁 .run/                    # IDE 运行配置
├── pom.xml                     # Maven 配置
└── README.md                   # 项目说明
```

---

## 🚀 快速开始

### 📋 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐使用 OpenJDK 17 |
| Maven | 3.6+ | 项目构建工具 |
| MySQL | 8.0+ | 数据库服务 |
| Redis | 8.0+ | 缓存服务 |
| SMTP | - | 邮件服务器 |

### 📦 安装步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd PixVisionServer
```

#### 2. 初始化数据库

```bash
# 创建数据库并导入表结构
mysql -u root -p < *.sql
```

#### 3. 配置应用

> ⚠️ **重要提示**：
> - 🚫 **不要直接修改** `src/main/resources/` 下的配置文件
> - ✅ **应该修改** 用户目录 `~/.pix_vision/application.yml` 中的配置
> - 💡 项目内的配置文件是**预设模板**，仅作为参考

**首次启动时**，项目会自动在用户主目录下创建 `.pix_vision` 目录和配置文件模板。

**编辑用户配置文件** `~/.pix_vision/application.yml`：

```yaml
# ============================================
# PixVision 用户自定义配置
# 位置: ~/.pix_vision/application.yml
# 优先级: 最高（会覆盖项目内预设配置）
# ============================================

### 服务端口
server:
  port: 9090

# 数据库配置
spring:
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000
      database: 0
      password: 123456
      username: root
  # 邮件配置
  mail:
    host: smtp.qq.com               # SMTP服务器地址
    port: 465                       # 端口
    from: your-email@example.com    # 发件人
    username: server_username       # 用户名
    password: your_auth_code        # 授权码
    protocol: smtp                  # 协议
    default-encoding: UTF-8         # 编码
    ssl-enable: false               # 不直接建立 SSL 连接
    starttls-enable: false          # 启用 STARTTLS 升级
```

**常见邮件服务商配置参考：**

| 服务商 | SMTP 主机 | 端口 | SSL | tls |
|--------|----------|------|----| ----|
| QQ 邮箱 | smtp.qq.com | 465 | ✅ | ❌ |
| 163 邮箱 | smtp.163.com | 465 | ✅ | ❌ |
| Gmail | smtp.gmail.com | 587 | ❌ | ✅ |
| Outlook | smtp-mail.outlook.com | 587 | ❌ | ✅ |

> 💡 **提示**：
> - QQ/163 邮箱需要使用**授权码**，不是登录密码
> - Gmail 需要开启“不够安全的应用访问”或使用应用专用密码
> - 配置文件支持 YAML 格式，注意缩进（2个空格）

#### 4. 构建项目

```bash
# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 或者仅编译
mvn clean compile
```

#### 5. 运行应用

**方式一：JAR 包运行**
```bash
java -jar target/PixVision-0.0.1-SNAPSHOT.jar
```

**方式二：Maven 插件运行**
```bash
mvn spring-boot:run
```

**方式三：IDE 运行**
- 在 IDE 中打开 `PixVisionApplication.java`
- 点击运行按钮或按 `Shift + F10`

### 🌐 访问地址

应用启动成功后，可以访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| 🏠 **首页** | http://localhost:9090 | 欢迎页面 |
| 💚 **健康检查** | http://localhost:9090/health | 服务状态 |
| 🖥️ **系统信息** | http://localhost:9090/system-info | JVM/OS/CPU 信息 |
| 📚 **Swagger UI** | http://localhost:9090/swagger-ui.html | API 文档（OpenAPI） |
| 🎯 **Knife4j UI** | http://localhost:9090/doc.html | API 文档（增强版） |

> ⚠️ **注意**: API 文档默认关闭，需在 `src/main/resources/yml-config/spring-doc.yml` 中设置 `springdoc.enabled: on` 启用。

---

## 📡 API 接口

### 🔐 用户接口 `/api/user`

| 方法   | 路径                       | 说明             | 认证 | 参数                                                   |
|------|--------------------------|----------------|:--:|------------------------------------------------------|
| POST | `/login`                 | 用户登录（支持用户名/邮箱） | ❌  | usernameOrEmail, password, vCode                     |
| POST | `/register`              | 用户注册           | ❌  | username, password, nickname(可选), email, vCode       |
| POST | `/logout`                | 用户登出（移除 Token） | ✅  | Authorization Header 或 URL 参数 token                  |
| GET  | `/page/{current}/{size}` | 分页查询用户信息       | ✅  | current, size, username(可选), uuid(可选), email(可选)     |
| POST | `/change-password`       | 修改密码（需登录）      | ✅  | newPassword, confirmPassword, vCode                  |
| POST | `/forgot-password`       | 忘记密码重置         | ❌  | usernameOrEmail, newPassword, confirmPassword, vCode |

**接口详细说明：**

#### 1. 用户登录 `/api/user/login`

```http
POST /api/user/login
Content-Type: application/x-www-form-urlencoded

usernameOrEmail=dev_user&password=123456&vCode=ABCDEF
```

**返回示例：**

```json
{
  "code": 200,
  "data": {
    "user_id": 1,
    "string_user_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "dev_user",
    "nickname": "开发者",
    "avatar_url": "default/1.png",
    "email": "dev@example.com",
    "status": 10,
    "user_role": 11,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "登录成功",
  "recode": 0,
  "status": "success"
}
```

**业务规则：**

- ✅ 支持用户名**或**邮箱登录
- ✅ 需要邮箱验证码二次验证
- ✅ Token 有效期 **7 天**
- ✅ 自动将 Token 加入白名单
- ⚠️ 账户状态必须为正常（status=10）

#### 2. 用户注册 `/api/user/register`

```http
POST /api/user/register
Content-Type: application/x-www-form-urlencoded

username=new_user&password=123456&nickname=新用户&email=new@example.com&vCode=ABCDEF
```

**业务规则：**

- ✅ 用户名格式：6-16 位字母/数字/下划线
- ✅ 昵称可为空，为空时自动生成
- ✅ 密码使用 SHA-256 哈希加密存储
- ✅ 用户名和邮箱不可重复
- ⚠️ 需要先通过邮件接口获取验证码

#### 3. 用户登出 `/api/user/logout`

```http
POST /api/user/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**业务规则：**

- ✅ 从白名单移除 Token，立即失效
- ✅ 支持 Header 或 URL 参数传递 Token
- ⚠️ 仅使当前设备的 Token 失效

#### 4. 分页查询用户 `/api/user/page/{current}/{size}`

```http
GET /api/user/page/1/10?username=dev&email=example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**返回示例：**

```json
{
  "code": 200,
  "data": {
    "records": [用户对象列表],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "message": "查询成功"
}
```

**业务规则：**

- ✅ 支持多条件组合查询（用户名、UUID、邮箱）
- ✅ 用户名和邮箱支持模糊匹配
- ✅ UUID 支持精确匹配
- ⚠️ 每页大小限制：1-100
- ⚠️ 自动过滤逻辑删除的用户

#### 5. 修改密码 `/api/user/change-password`

```http
POST /api/user/change-password
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/x-www-form-urlencoded

newPassword=newpass123&confirmPassword=newpass123&vCode=ABCDEF
```

**业务规则：**

- ✅ 需要携带有效 Token
- ✅ 需要邮箱验证码验证
- ✅ 新旧密码不能相同
- ⚠️ **修改成功后，该用户的所有 Token 会被移除**（强制所有设备下线）

#### 6. 忘记密码 `/api/user/forgot-password`

```http
POST /api/user/forgot-password
Content-Type: application/x-www-form-urlencoded

usernameOrEmail=dev_user&newPassword=newpass123&confirmPassword=newpass123&vCode=ABCDEF
```

**业务规则：**

- ✅ 无需登录即可调用
- ✅ 支持用户名**或**邮箱找回密码
- ✅ 需要邮箱验证码验证
- ⚠️ **重置成功后，该用户的所有 Token 会被移除**（强制所有设备下线）

---

### 📧 邮件接口 `/api/mail`

| 方法   | 路径                        | 说明           | 认证 | 参数                                   |
|------|---------------------------|--------------|:--:|--------------------------------------|
| POST | `/send-email-code`        | 发送邮箱验证码      | ❌  | to, subject, username(可选), emailText |
| POST | `/verify-email-code-test` | 验证邮箱验证码（测试用） | ❌  | email, inputVCode                    |

**接口详细说明：**

#### 1. 发送验证码 `/api/mail/send-email-code`

```http
POST /api/mail/send-email-code
Content-Type: application/x-www-form-urlencoded

# 注册场景
to=user@example.com&subject=PixVision 验证码&username=new_user&emailText=注册

# 登录场景（to 可以是用户名或邮箱，username 不传）
to=dev_user&subject=PixVision 验证码&emailText=登录

# 改密场景（to 可以是用户名或邮箱，username 不传）
to=dev_user&subject=PixVision 验证码&emailText=改密
```

**业务规则：**

- ✅ **注册场景**：to 必须是邮箱，username 必须传入
- ✅ **登录/改密场景**：to 可以是用户名或邮箱，系统自动查询，username 禁止传入
- ✅ emailText 仅支持：注册、登录、改密
- ✅ 验证码为 6 位大写字母或数字
- ✅ 验证码存储在 Redis 中，默认有效期 5 分钟

#### 2. 验证验证码 `/api/mail/verify-email-code-test`

```http
POST /api/mail/verify-email-code-test
Content-Type: application/x-www-form-urlencoded

email=user@example.com&inputVCode=ABCDEF
```

**业务规则：**

- ✅ 用于测试验证码是否正确
- ✅ 验证成功后会自动清除 Redis 中的验证码
- ⚠️ 此接口仅供测试使用，生产环境不建议暴露

---

### 🖼️ 图片接口 `/api/image`

| 方法   | 路径               | 说明         | 认证 | 参数                   |
|------|------------------|------------|:--:|----------------------|
| GET  | `/get/avatar`    | 获取头像图片     | ❌  | filePath             |
| GET  | `/get/works`     | 获取作品图片     | ❌  | filePath             |
| GET  | `/get/logo`      | 获取 Logo 图片 | ❌  | filePath             |
| POST | `/upload/avatar` | 上传用户头像     | ✅  | file (MultipartFile) |

**接口详细说明：**

#### 1. 获取头像 `/api/image/get/avatar`

```http
GET /api/image/get/avatar?filePath=default/1.png
```

**业务规则：**

- ✅ 无需认证，公开访问
- ✅ 支持子目录结构：`default/1.png`、`custom/avatar.jpg`
- ✅ 仅支持 JPG、JPEG、PNG 格式
- ✅ 自动设置缓存头（1 小时）
- ⚠️ 文件路径相对于 `~/.pix_vision/data/avatar/`

#### 2. 获取作品图片 `/api/image/get/works`

```http
GET /api/image/get/works?filePath=2024/04/artwork.png
```

**业务规则：**

- ✅ 无需认证，公开访问
- ✅ 支持子目录结构，建议按日期或用户分类
- ✅ 仅支持 JPG、JPEG、PNG 格式
- ✅ 自动设置缓存头（1 小时）
- ⚠️ 文件路径相对于 `~/.pix_vision/data/works/`

#### 3. 获取 Logo `/api/image/get/logo`

```http
GET /api/image/get/logo?filePath=dark.png
```

**业务规则：**

- ✅ 无需认证，公开访问
- ✅ 通常不包含子目录，直接使用文件名
- ✅ 仅支持 JPG、JPEG、PNG 格式
- ✅ 自动设置缓存头（1 小时）
- ⚠️ 文件路径相对于 `~/.pix_vision/data/logo-img/`

#### 4. 上传头像 `/api/image/upload/avatar`

```http
POST /api/image/upload/avatar
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data

file: [binary image data]
```

**业务规则：**

- ✅ **需要认证**，必须携带有效 Token
- ✅ 仅支持 JPG、JPEG、PNG 格式
- ✅ **头像必须是正方形图片**（宽高相等）
- ✅ 图片自动缩放为 **600x600** 像素的 PNG 格式
- ✅ 文件大小限制 **5MB**
- ✅ 文件名使用 UUID 生成，避免冲突
- ⚠️ 旧头像文件不会自动删除，建议定期清理

**安全特性：**

- 🔒 文件类型白名单校验
- 🔒 路径安全检查（防止目录遍历攻击）
- 🔒 图像魔数验证（防止伪造扩展名）
- 🔒 正方形图像验证

---

### 🖥️ 系统接口

| 方法  | 路径             | 说明    | 认证 | 返回                     |
|-----|----------------|-------|:--:|------------------------|
| GET | `/`            | 首页重定向 | ❌  | HTML 页面                |
| GET | `/health`      | 健康检查  | ❌  | 服务状态页面                 |
| GET | `/system-info` | 系统信息  | ❌  | JVM/OS/CPU/Memory/Disk |

**接口详细说明：**

#### 1. 首页 `/`

```http
GET /
```

- 重定向到静态资源首页 `index.html`
- 提供快速跳转到 Swagger 文档的链接

#### 2. 健康检查 `/health`

```http
GET /health
```

- 重定向到服务运行状态监控页面
- 用于判断应用是否启动成功

#### 3. 系统信息 `/system-info`

```http
GET /system-info
```

**返回示例：**

```json
{
  "code": 200,
  "data": {
    "jvmInfo": {
      "version": "17.0.1",
      "vendor": "Oracle Corporation",
      "uptime": "2h 30m"
    },
    "osInfo": {
      "name": "Windows 11",
      "version": "10.0",
      "arch": "amd64"
    },
    "cpuInfo": {
      "cores": 8,
      "usage": "25.5%"
    },
    "memoryInfo": {
      "total": "16 GB",
      "used": "8 GB",
      "available": "8 GB"
    },
    "diskInfo": {
      "total": "500 GB",
      "used": "200 GB",
      "available": "300 GB"
    }
  },
  "message": "获取系统信息成功"
}
```

**包含信息：**

- 🖥️ JVM 信息（版本、厂商、运行时间）
- 🖥️ 操作系统信息（名称、版本、架构）
- 🖥️ CPU 信息（核心数、使用率）
- 🖥️ 内存信息（总量、已用、可用）
- 🖥️ 磁盘信息（总空间、已用、可用）

---

### 🧪 JWT 测试接口 `/7e212056`

| 方法  | 路径              | 说明         | 认证 |
|-----|-----------------|------------|:--:|
| GET | `/require-auth` | 需要登录的测试接口  | ✅  |
| GET | `/no-auth`      | 不需要登录的测试接口 | ❌  |

**用途：** 用于测试 JWT 拦截器是否正常工作

#### 1. 需要认证的接口 `/7e212056/require-auth`

```http
GET /7e212056/require-auth
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**返回示例：**

```json
{
  "code": 200,
  "data": "欢迎访问！您已通过JWT认证 - 用户ID:1 用户名:dev_user",
  "message": "认证成功"
}
```

#### 2. 公开接口 `/7e212056/no-auth`

```http
GET /7e212056/no-auth
```

**返回示例：**

```json
{
  "code": 200,
  "data": "这是一个公开接口，无需登录即可访问",
  "message": "公开访问"
}
```

### 🔑 认证说明

**Token 获取：**
```json
// 登录成功后返回
{
  "data": {
    "user_id": 1,
    "user_uuid": "16字节二进制uuid",
    "username": "li_hua",
    "password": "哈希256加密密码",
    "nickname": "李华",
    "user_role": 11,
    "avatar_url": "/avatar/uuid.png",
    "email": "lihua@example.com",
    "is_delete": false,
    "status": 10,
    "update_time": "2026-03-29T12:00:00.003Z",
    "update_user": 0,
    "create_time": "2026-03-29T12:00:00.003Z",
    "create_user": 0,
    "avatar_base64": "我是 base64",
    "string_user_uuid": "字符串类型uuid",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
  },
  "message": "",
  "recode": 0,
  "status": ""
}
```

**Token 使用：**

方式一：Header（推荐）
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

方式二：URL 参数
```http
GET /api/user/page/1/10?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token 特性：**
- ⏰ 有效期：**7 天**
- 🚫 失效场景：登出、修改密码、忘记密码重置、手动移除白名单、批量移除用户所有Token
- 🔒 安全机制：JWT 签名验证 + Redis 白名单双重校验
- ⚡ **性能优化**：使用 Redis Set 索引结构，批量移除 Token 复杂度从 O(N) 降至 O(M)
  - N = 系统总 Token 数（10,000+）
  - M = 单个用户 Token 数（通常 1-5）
  - 性能提升：**2,500x**（10K Token 场景）

---

## 🔐 Token 白名单机制详解

### 📊 架构设计

本项目采用 **JWT + Redis 白名单** 双重验证机制，并引入 **Set 索引结构** 优化性能。

**Redis 数据结构：**

```
1. Token 白名单（String 类型）
   Key: token:whitelist:{token字符串}
   Value: {userId}:{username}
   TTL: 7天

2. Token 索引集合（Set 类型）✨ 性能优化
   Key: token:index:{userId}
   Value: Set<token1, token2, token3...>
   TTL: 7天
```

### ⚡ 性能优化对比

#### **场景：用户修改密码，强制所有设备下线**

假设系统有 **10,000 个活跃 Token**，目标用户有 **3 个设备登录**：

| 指标               | 优化前（KEYS 命令） | 优化后（Set 索引） | 提升             |
|------------------|--------------|-------------|----------------|
| **扫描 Token 数**   | 10,000 个     | 3 个         | **3,333x** ⚡   |
| **Redis GET 次数** | 10,000 次     | 0 次         | **∞** ⚡⚡⚡      |
| **总耗时**          | ~10,050ms    | ~4ms        | **2,500x** ⚡⚡⚡ |
| **时间复杂度**        | O(N)         | O(M)        | N >> M         |
| **Redis 阻塞**     | ⚠️ 高         | ✅ 无         | -              |

**说明：**

- N = 系统总 Token 数（10,000+）
- M = 单个用户的 Token 数（通常 1-5）

### 🔄 工作流程

#### **1. 登录时添加到白名单**

```java
// UserController.login()
String token = JWTUtils.createToken(user.getUser_id(), user.getUsername());
tokenWhitelistService.addToWhitelist(token, user.getUser_id(), user.getUsername(), expireTime);

// Redis 操作：
// SET token:whitelist:{token} "{userId}:{username}" EX 604800
// SADD token:index:{userId} {token}
// EXPIRE token:index:{userId} 604800
```

#### **2. 请求时验证白名单**

```java
// JwtAuthenticationInterceptor.preHandle()
if (!tokenWhitelistService.isInWhitelist(token)) {
    return false; // Token 不在白名单中，拒绝访问
}

// Redis 操作：
// EXISTS token:whitelist:{token}
```

#### **3. 单个 Token 登出**

```java
// UserController.logout()
tokenWhitelistService.removeFromWhitelist(token);

// Redis 操作：
// GET token:whitelist:{token} → 获取 userId
// SREM token:index:{userId} {token} → 从索引移除
// DEL token:whitelist:{token} → 删除白名单记录
```

#### **4. 批量移除用户所有 Token** ⚡ 核心优化点

```java
// UserController.changeUserPassword() 或 forgotPassword()
int removedCount = tokenWhitelistService.removeAllUserTokens(userId, username);

// Redis 操作（优化后）：
// SMEMBERS token:index:{userId} → 直接获取用户的所有 Token（O(1)）
// DEL token:whitelist:{token1}
// DEL token:whitelist:{token2}
// DEL token:whitelist:{token3}
// DEL token:index:{userId}
```

### 💡 优化效果

**适用场景：**

- ✅ 修改密码后强制所有设备下线
- ✅ 忘记密码重置后强制重新登录
- ✅ 管理员强制下线某用户
- ✅ 查询用户在线设备数（`SCARD token:index:{userId}`）

**性能提升：**

- 批量操作：**2,500x** ⚡⚡⚡
- 统计查询：**10,000x** ⚡⚡⚡
- 单个操作：略有开销（+2 次 Redis 调用），但可接受

### ⚠️ 注意事项

1. **数据一致性**：白名单和索引必须同步更新
2. **孤儿索引清理**：Token 自然过期后，索引中可能残留引用（可选：定时任务清理）
3. **内存占用**：额外存储索引数据（约增加 5% 内存），但性能收益远大于成本

---

## 🗄️ 数据库设计

### 👥 用户角色体系

| 角色代码 | 角色名称 | 权限说明 |
|:-------:|---------|----------|
| 11 | 普通用户 | 浏览、评论、点赞、收藏 |
| 22 | 创作者 | 发布作品、管理作品 |
| 55 | 审核员 | 审核作品、处理举报 |
| 66 | 工单管理员 | 处理用户工单 |
| 77 | 系统管理员 | 全部权限 |

### 📊 用户状态

| 状态代码 | 状态名称 | 说明 |
|:-------:|---------|------|
| 10 | ✅ 正常 | 正常使用所有功能 |
| 20 | ⚠️ 冻结 | 暂时限制部分功能 |
| 30 | 🚫 封禁 | 禁止登录和使用 |

### 📋 数据表清单

| 表名 | 中文名 | 主要字段 | 状态 |
|------|--------|---------|:---:|
| `tb_user` | 用户账户 | id, username, email, password, role, status | ✅ |
| `tb_user_data` | 用户扩展数据 | user_id, avatar, bio, create_time | ✅ |
| `tb_works` | 作品 | id, title, description, author_id, cover_url | 🚧 |
| `tb_series` | 作品系列 | id, name, author_id, description | 🚧 |
| `tb_comments` | 评论 | id, content, user_id, works_id, parent_id | 🚧 |
| `tb_like` | 点赞 | id, user_id, target_id, target_type | 🚧 |
| `tb_star` | 收藏 | id, user_id, works_id | 🚧 |
| `tb_history` | 浏览历史 | id, user_id, works_id, view_time | 🚧 |
| `tb_sys_logs` | 系统日志 | id, operation, ip, create_time | ✅ |

> ✅ 已完成 | 🚧 开发中

---

## ⚙️ 配置说明

### 📌 配置优先级与最佳实践

**配置加载顺序**（从高到低）：

```
🔝 最高优先级  ~/.pix_vision/application.yml          # 用户自定义配置（✅ 推荐修改）
   ↓
🔧 中等优先级  src/main/resources/yml-config/*.yml    # 核心配置模板（🚫 不建议修改）
   ↓
📄 最低优先级  src/main/resources/application.yml     # 基础配置（🚫 不建议修改）
```

> ⚠️ **重要原则**：
> - ✅ **推荐做法**：在 `~/.pix_vision/application.yml` 中覆盖需要的配置项
> - 🚫 **避免做法**：直接修改 `src/main/resources/` 下的配置文件
> - 💡 **原因**：
>   1. 项目更新时会覆盖 `src/main/resources/` 下的文件
>   2. 用户配置与代码分离，便于版本控制和部署
>   3. 不同环境可以使用不同的用户配置

**配置覆盖示例：**

假设你想修改数据库密码，只需在 `~/.pix_vision/application.yml` 中添加：

```yaml
# 只需要写需要覆盖的配置项，不需要复制整个文件
spring:
  datasource:
    password: my_new_password  # 只覆盖密码，其他配置使用默认值
```

项目会自动合并配置，未指定的配置项使用预设值。

### 📁 运行时目录结构

项目首次启动时会自动在用户主目录下创建 `.pix_vision` 目录：

```
~/.pix_vision/
├── 📄 application.yml          # 用户自定义配置（可手动创建）
├── 📄 readme.txt               # 目录说明
│
├── 📁 data/                    # 数据文件目录
│   ├── 📁 logo-img/           # Logo 图片
│   │   ├── dark.png           # 深色 Logo
│   │   ├── light.png          # 浅色 Logo
│   │   └── 目录说明.txt
│   └── 📁 avatar/             # 用户头像
│       └── 📁 default/        # 默认头像（21个）
│
├── 📁 config/                  # 配置文件目录
│   └── 📁 email-html/         # 邮件 HTML 模板
│       ├── email-verification.html
│       └── 目录说明.txt
│
├── 📁 log/                     # 日志文件目录
│   ├── pix_vision.log         # 应用日志
│   └── error.log              # 错误日志
│
└── 📁 key/                     # 密钥文件目录
    └── 📁 rsa/                # RSA 密钥对
        ├── public.key         # RSA 公钥
        ├── private.key        # RSA 私钥
        ├── public.key.bak     # 旧公钥备份
        ├── private.key.bak    # 旧私钥备份
        └── 目录说明.txt
```

### 🔒 RSA 加密工具

项目集成了强大的 RSA 加密工具，支持**任意类型和大小**的数据加密：

**核心特性：**
- ✨ 智能加密策略（小数据纯 RSA，大数据 AES+RSA 混合）
- 📦 支持文本、JSON、图片、文件等任意数据类型
- 🔐 无大小限制（突破 RSA 245 字节限制）
- 🔄 自动密钥管理（启动时生成/加载）
- 💾 密钥文件存储（`~/.pix_vision/key/rsa/`）

**使用示例：**
```java
// 加密字符串
String encrypted = RSACipher.encryptToBase64("敏感信息");

// 加密二进制数据（如图片）
byte[] imageData = Files.readAllBytes(Paths.get("photo.jpg"));
String encrypted = RSACipher.encryptToBase64(imageData);

// 解密
String decrypted = RSACipher.decryptToString(encrypted);
byte[] original = RSACipher.decryptToBytes(encrypted);
```

📖 **详细文档**：查看 [RSA工具使用指南.md](doc/RSA工具使用指南.md)

---

## 👨‍💻 开发指南

### 📝 新增接口流程

```mermaid
graph TD;
    A[设计接口] --> B[创建/更新 POJO]
    B --> C[编写 Service]
    C --> D[创建 Controller]
    D --> E[添加 Swagger 文档]
    E --> F[编写单元测试]
    F --> G[代码审查]
    G --> H[提交代码]
```

**详细步骤：**

1. **设计接口**
   - 确定 URL 路径和请求方式（GET/POST/PUT/DELETE）
   - 定义请求参数和返回值
   - 确定是否需要认证

2. **创建/更新 POJO**
   - 在 `pojo/` 目录下创建实体类
   - 使用 Lombok 注解简化代码（`@Data`, `@Builder`）
   - 添加必要的验证注解

3. **编写 Service 层**
   - 在 `service/` 目录定义接口
   - 在 `service/Impl/` 目录实现业务逻辑
   - 使用 `@Service` 和 `@RequiredArgsConstructor` 注解

4. **创建 Controller**
   - 在 `controller/` 目录创建控制器
   - 使用 `@RestController` 和 `@RequestMapping`
   - 添加 Swagger 文档注解（`@Operation`, `@Parameter`）

5. **编写测试**
   - 在 `src/test/java/` 对应包下创建测试类
   - 使用 JUnit 5 和 Spring Boot Test
   - 覆盖正常流程和边界情况

6. **代码审查与提交**
   - 检查代码规范和注释
   - 运行单元测试确保通过
   - 提交 Git 并编写清晰的 commit message

### 📐 代码规范

#### 命名规范
- **类名**：大驼峰（PascalCase），如 `UserController`
- **方法名**：小驼峰（camelCase），如 `getUserById`
- **常量**：全大写+下划线，如 `TOKEN_EXPIRE_TIME`
- **变量**：全小写+下划线，如 `user_id`

#### 注释规范
- 所有 `public` 方法必须有 JavaDoc
- 包含 `@param`、`@return`、`@author` 标签
- 复杂逻辑添加行内注释说明

#### 代码风格
- ✅ 使用 **4 个空格**缩进（不使用 Tab）
- ✅ 优先使用**构造器注入**（`@RequiredArgsConstructor`）
- ✅ 异常处理优先返回错误响应，而非抛出异常
- ✅ 使用 SLF4J 记录日志，避免 `System.out.println`
- ✅ 敏感信息（密码、Token）不记录明文

#### 示例代码

```java
/**
 * 用户操作相关接口
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    /**
     * 根据 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 响应数据<User>
     * @author PlayerEG
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户", description = "根据用户 ID 查询用户信息")
    public ResponsePojo<User> getUserById(
            @Parameter(description = "用户 ID", required = true) 
            @PathVariable Long userId) {
        
        User user = userService.selectById(userId);
        if (user == null) {
            return ResponsePojo.error(null, "用户不存在");
        }
        
        return ResponsePojo.success(user, "查询成功");
    }
}
```

### 🧪 测试指南

**运行所有测试：**
```bash
mvn test
```

**运行指定测试类：**
```bash
mvn test -Dtest=RSACipherTest
```

**运行指定测试方法：**
```bash
mvn test -Dtest=RSACipherTest#testSmallTextEncryption
```

**查看详细输出：**
```bash
mvn test -X
```

---

## ❓ 常见问题

### 1️⃣ API 文档无法访问

**问题**：访问 `http://localhost:9090/doc.html` 显示 404

**解决方案**：
检查 `src/main/resources/yml-config/spring-doc.yml`，确保：
```yaml
springdoc:
  enabled: on  # 改为 on
```

重启应用后再次访问。

---

### 2️⃣ 邮件发送失败

**问题**：调用发送邮件接口时报错

**排查步骤**：

1. **检查用户配置文件** `~/.pix_vision/application.yml`
   ```yaml
   mail:
     host: smtp.qq.com              # 确认 SMTP 主机
     port: 465                      # 确认端口
     username: 12345@qq.com         # 确认邮箱地址
     password: abcdefghijklmnop     # 确认授权码（不是登录密码！）
   ```

2. **常见错误原因**：
   - ❌ 使用了登录密码而不是授权码
   - ❌ SMTP 服务未开启
   - ❌ 端口或主机配置错误
   - ❌ SSL 设置不匹配

3. **常见邮件服务商配置**：
   
   **QQ 邮箱**：
   ```yaml
   mail:
     host: smtp.qq.com
     port: 465
     properties:
       mail:
         smtp:
           ssl:
             enable: true
   ```
   > 获取授权码：邮箱设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务 → 开启SMTP服务

   **163 邮箱**：
   ```yaml
   mail:
     host: smtp.163.com
     port: 465
     properties:
       mail:
         smtp:
           ssl:
             enable: true
   ```
   > 获取授权码：邮箱设置 → POP3/SMTP/IMAP → 开启SMTP服务 → 客户端授权密码

   **Gmail**：
   ```yaml
   mail:
     host: smtp.gmail.com
     port: 587
     properties:
       mail:
         smtp:
           auth: true
           starttls:
             enable: true
   ```
   > 需要开启“不够安全的应用访问”或使用应用专用密码

4. **测试配置是否正确**：
   ```bash
   # 查看应用启动日志，确认邮件配置已加载
   tail -f ~/.pix_vision/log/pix_vision.log | grep "mail"
   ```

5. **重启应用使配置生效**：
   ```bash
   # 修改配置后必须重启应用
   java -jar target/PixVision-0.0.1-SNAPSHOT.jar
   ```

---

### 3️⃣ Redis 连接失败

**问题**：启动时报 Redis 连接错误

**解决方案**：

1. **确认 Redis 服务已启动**
   ```bash
   # Linux/Mac
   redis-cli ping  # 应返回 PONG
   
   # Windows
   redis-cli.exe ping
   
   # 如果未安装，先安装 Redis
   # Mac: brew install redis
   # Linux: sudo apt-get install redis-server
   ```

2. **检查用户配置文件** `~/.pix_vision/application.yml`
   ```yaml
   spring:
     data:
       redis:
         host: localhost      # Redis 主机地址
         port: 6379           # Redis 端口
         password:            # 密码（如果没有则留空）
         database: 0          # 数据库索引
   ```

3. **如果使用 Docker**：
   ```bash
   # 启动 Redis 容器
   docker run -d --name redis -p 6379:6379 redis:8.0
   
   # 检查容器状态
   docker ps | grep redis
   ```

4. **防火墙设置**：
   ```bash
   # 确保 6379 端口未被阻止
   sudo ufw allow 6379/tcp  # Ubuntu
   ```

5. **查看详细错误日志**：
   ```bash
   tail -f ~/.pix_vision/log/pix_vision.log
   # 查找 Redis 相关错误信息
   ```

---

### 4️⃣ 数据库连接失败

**问题**：启动时报 MySQL 连接错误

**解决方案**：

1. **确认 MySQL 服务已启动**
   ```bash
   # Linux
   sudo systemctl status mysql
   sudo systemctl start mysql  # 如果未启动
   
   # Mac
   brew services start mysql
   
   # Windows
   # 打开 services.msc，查找 MySQL 服务并启动
   ```

2. **检查用户配置文件** `~/.pix_vision/application.yml`
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/db_pix_vision?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
       username: root           # 数据库用户名
       password: your_password  # 数据库密码
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

3. **确认数据库已创建**：
   ```bash
   # 登录 MySQL
   mysql -u root -p
   
   # 查看数据库列表
   SHOW DATABASES;
   
   # 如果没有 db_pix_vision，创建它
   CREATE DATABASE db_pix_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **导入数据库脚本**：
   ```bash
   # 方法一：命令行导入
   mysql -u root -p db_pix_vision < sql/db_pix_vision-V1.1.sql
   
   # 方法二：MySQL 客户端导入
   mysql -u root -p
   USE db_pix_vision;
   SOURCE /path/to/sql/db_pix_vision-V1.1.sql;
   ```

5. **检查时区设置**：
   ```sql
   -- 在 MySQL 中执行
   SELECT @@time_zone;
   
   -- 如果不是 Asia/Shanghai，可以设置
   SET GLOBAL time_zone = '+8:00';
   ```

6. **查看详细错误日志**：
   ```bash
   tail -f ~/.pix_vision/log/pix_vision.log
   # 查找数据库连接相关错误
   ```

---

### 5️⃣ RSA 密钥相关问题

**问题**：加密/解密失败

**解决方案**：

1. **检查密钥文件是否存在**
   ```bash
   ls -la ~/.pix_vision/key/rsa/
   # 应该看到：
   # public.key      # RSA 公钥
   # private.key     # RSA 私钥
   ```

2. **查看应用启动日志**
   ```bash
   cat ~/.pix_vision/log/pix_vision.log | grep -i "RSA\|key"
   
   # 正常情况应该看到：
   # INFO  - 创建 RSA 密钥目录: /Users/xxx/.pix_vision/key/rsa
   # INFO  - RSA 密钥已生成并保存到: /Users/xxx/.pix_vision/key/rsa
   ```

3. **检查文件权限**
   ```bash
   # 私钥文件应该是只读的
   chmod 600 ~/.pix_vision/key/rsa/private.key
   chmod 644 ~/.pix_vision/key/rsa/public.key
   ```

4. **如需重新生成密钥**
   ```java
   // 在代码中调用（会备份旧密钥）
   RSACipher.regenerateKeys();
   ```
   
   或者手动删除密钥文件后重启应用：
   ```bash
   rm ~/.pix_vision/key/rsa/*.key
   # 重启应用后会自动生成新密钥
   ```

⚠️ **重要警告**：
- 更换密钥后，**旧密钥加密的数据无法用新密钥解密**
- 更换前务必备份所有加密数据
- `.bak` 文件是旧密钥的备份，妥善保管

---

### 6️⃣ 端口被占用

**问题**：启动时报 `Port 9090 was already in use`

**解决方案**：
1. 查找占用端口的进程
   ```bash
   # Linux/Mac
   lsof -i :9090
   
   # Windows
   netstat -ano | findstr :9090
   ```

2. 终止进程或修改端口
   ```yaml
   # application.yml
   server:
     port: 9091  # 改为其他端口
   ```

[//]: # (---)

[//]: # ()
[//]: # (## 📄 许可证)

[//]: # ()
[//]: # (本项目采用 [MIT License]&#40;LICENSE&#41; 开源协议)

[//]: # ()
[//]: # (```text)

[//]: # (MIT License)

[//]: # ()
[//]: # (Copyright &#40;c&#41; 2024 PlayerEG)

[//]: # ()
[//]: # (Permission is hereby granted, free of charge, to any person obtaining a copy)

[//]: # (of this software and associated documentation files &#40;the "Software"&#41;, to deal)

[//]: # (in the Software without restriction, including without limitation the rights)

[//]: # (to use, copy, modify, merge, publish, distribute, sublicense, and/or sell)

[//]: # (copies of the Software, and to permit persons to whom the Software is)

[//]: # (furnished to do so, subject to the following conditions:)

[//]: # ()
[//]: # (The above copyright notice and this permission notice shall be included in all)

[//]: # (copies or substantial portions of the Software.)

[//]: # (```)

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

[//]: # (---)

[//]: # ()
[//]: # (## 📮 联系方式)

[//]: # ()
[//]: # (- 🐛 **问题反馈**：[GitHub Issues]&#40;https://github.com/PlayerEG/PixVisionServer/issues&#41;)

[//]: # (- 💬 **讨论交流**：[GitHub Discussions]&#40;https://github.com/PlayerEG/PixVisionServer/discussions&#41;)

[//]: # (- 📧 **邮件联系**：playereg@example.com)

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**

Made with ❤️ by PlayerEG

</div>
