# PixVisionServer - AI Agent 开发规范

## 项目概述

- **项目名称**：PixVisionServer（像素视觉后端服务）
- **语言**：Java 17
- **构建工具**：Maven 3.x
- **框架**：Spring Boot 3.3.0
- **测试框架**：JUnit + Spring Boot Test
- **作者**：PlayerEG
- **贡献者**：blue_sky_ks
- **包名**：`top.playereg.pix_vision`
- **版本**：DEV-2.0.0
- **描述**：数字艺术创作与分享平台后端服务，支持作品管理、用户互动、系统监控等功能

---

# 技术栈

## 核心依赖

### Web 框架

- **Spring Boot Web** (Jakarta EE)：RESTful API 开发
- **Spring Boot Actuator**：应用监控与健康检查

### 持久层

- **MyBatis-Plus 3.5.7**：增强版 MyBatis ORM 框架
- **MySQL Connector/J**：MySQL 8.0 数据库驱动
- **数据库表命名**：统一前缀 `tb_`（如 `tb_user`、`tb_works`）

### 缓存

- **Redis 8.0**：高性能键值存储
- **Lettuce**：Redis 客户端（明确指定版本避免冲突）
- **Commons Pool2**：连接池支持

### 认证与安全

- **Hutool JWT**：JSON Web Token 生成与验证
- **Bouncy Castle 1.77**：RSA + AES 混合加密（支持任意数据类型）
- **Spring Security**：基础安全配置

### API 文档

- **SpringDoc OpenAPI 2.5.0**：OpenAPI 3.0 规范实现
- **Knife4j 4.4.0**：增强的 Swagger UI
- **访问地址**：http://localhost:9090/doc.html

### 邮件服务

- **Spring Boot Mail**：邮件发送服务
- **Jakarta Mail 2.0.1**：邮件协议实现
- **Commonmark 0.21.0**：Markdown 解析支持

### 工具库

- **Hutool 5.8.38**：Java 工具类库集合
- **Lombok**：代码简化工具（@Data、@RequiredArgsConstructor 等）
- **Oshi 6.6.5**：系统信息采集（CPU、内存、磁盘等）
- **JetBrains Annotations**：代码注解支持

### 其他

- **Thymeleaf**：模板引擎（已注释，当前未使用）
- **Log4j 1.2.17**：日志框架
- **SLF4J**：日志门面接口
- **Spring AOP**：面向切面编程（日志记录）

---

# 项目结构

```
PixVisionServer/
├── src/main/java/top/playereg/pix_vision/
│   ├── config/              # 配置类（@Configuration）
│   │   ├── EmailConfig.java         # 邮件配置与模板渲染
│   │   ├── FilePathConfig.java      # 文件路径配置
│   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus 配置
│   │   ├── PVSLogConfig.java        # 日志配置
│   │   ├── RedisConfig.java         # Redis 配置
│   │   ├── SecureConfig.java        # 安全配置（CORS、拦截器）
│   │   ├── SwaggerConfig.java       # API 文档配置
│   │   └── WebConfig.java           # Web 配置
│   │
│   ├── controller/          # 控制器层（@RestController）
│   │   ├── MailController.java      # 邮件服务接口
│   │   ├── RootController.java      # 根路由（首页、健康检查、系统信息）
│   │   ├── TestAuthController.java  # JWT 鉴权测试接口
│   │   └── UserController.java      # 用户操作接口
│   │
│   ├── service/             # 服务接口
│   │   ├── Impl/           # 服务实现类（@Service）
│   │   │   ├── EmailServiceImpl.java
│   │   │   ├── EmailTemplateServiceImpl.java
│   │   │   ├── SystemInfoServiceImpl.java
│   │   │   ├── TokenWhitelistServiceImpl.java
│   │   │   ├── UserServiceImpl.java
│   │   │   └── VerificationCodeServicesImpl.java
│   │   ├── EmailService.java
│   │   ├── EmailTemplateService.java
│   │   ├── SystemInfoService.java
│   │   ├── TokenWhitelistService.java
│   │   ├── UserService.java
│   │   └── VerificationCodeServices.java
│   │
│   ├── mapper/              # MyBatis Mapper 接口
│   │   └── UserMapper.java          # 用户数据访问接口
│   │
│   ├── pojo/                # 实体类和数据传输对象
│   │   ├── userPojo/       # 用户相关 POJO
│   │   │   ├── User.java            # 用户实体
│   │   │   ├── UserData.java        # 用户扩展数据
│   │   │   └── UserLogin.java       # 登录响应对象
│   │   ├── Comments.java            # 评论实体
│   │   ├── History.java             # 浏览历史实体
│   │   ├── Like.java                # 点赞实体
│   │   ├── OperateLog.java          # 操作日志实体
│   │   ├── ResponsePojo.java        # 统一响应对象
│   │   ├── Series.java              # 作品系列实体
│   │   ├── Star.java                # 收藏实体
│   │   ├── SystemInfo.java          # 系统信息对象
│   │   └── Works.java               # 作品实体
│   │
│   ├── handler/             # 拦截器和处理器
│   │   └── JwtAuthenticationInterceptor.java  # JWT 认证拦截器
│   │
│   ├── util/                # 工具类
│   │   ├── Aspect/         # AOP 切面
│   │   │   ├── LogAspect.java       # 日志记录切面
│   │   │   └── LogRecord.java       # 日志记录注解
│   │   ├── ConversionUtils.java     # 数据类型转换工具
│   │   ├── CreateFile.java          # 文件创建工具（初始化模板）
│   │   ├── ImageUtils.java          # 图片处理工具
│   │   ├── IpUtil.java              # IP 地址获取工具
│   │   ├── JWTUtils.java            # JWT Token 工具
│   │   ├── RSACipher.java           # RSA + AES 混合加密工具
│   │   ├── RegexUtils.java          # 正则表达式验证工具
│   │   └── StrSwitchUtils.java      # 字符串转换工具
│   │
│   ├── enums/               # 枚举类
│   │   └── LogColor.java            # 日志颜色枚举
│   │
│   ├── egg/                 # 彩蛋功能
│   │   └── EggNoBug.java            # 启动彩蛋
│   │
│   ├── PixVisionApplication.java    # Spring Boot 启动类
│   └── ServletInitializer.java      # WAR 包部署支持
│
├── src/main/resources/
│   ├── yml-config/          # YAML 配置文件（优先级 1）
│   │   ├── email.yml                # 邮件配置
│   │   ├── jdbc.yml                 # 数据库配置
│   │   ├── logging.yml              # 日志配置
│   │   ├── mybatis-plus.yml         # MyBatis-Plus 配置
│   │   ├── pix_vision.yml           # 应用自定义配置
│   │   ├── redis.yml                # Redis 配置
│   │   └── spring-doc.yml           # API 文档配置
│   │
│   ├── application.yml      # 主配置文件（优先级 0）
│   ├── log4j.properties     # Log4j 配置文件
│   ├── banner.txt           # 启动 Banner
│   │
│   ├── mapper/              # MyBatis XML 映射文件
│   │   └── UserMapper.xml           # 用户 SQL 映射
│   │
│   ├── template/            # 模板文件
│   │   ├── email-html/     # 邮件 HTML 模板
│   │   │   ├── email-verification.html  # 验证码邮件模板
│   │   │   └── ...                  # 其他邮件模板
│   │   └── application-template.yml # 用户配置模板
│   │
│   ├── static/              # 静态资源
│   │   ├── index.html               # 首页
│   │   ├── style.css                # 样式文件
│   │   ├── logo/                    # Logo 图片
│   │   ├── default-avatar/          # 默认头像
│   │   ├── font/                    # 字体文件
│   │   └── server-status/           # 服务器状态页面
│   │
│   └── logo/                # Logo 图片资源
│       ├── dark.png                 # 深色 Logo
│       ├── light.png                # 浅色 Logo
│       └── about-path.txt           # 路径说明
│
├── sql/                     # 数据库脚本
│   └── db_pix_vision-V1.1.sql       # 数据库表结构
│
├── doc/                     # 项目文档
│   └── RSA工具使用指南.md            # RSA 加密工具文档
│
├── .run/                    # IDE 运行配置
│   ├── PixVisionApplication.run.xml
│   ├── Mac启动服务依赖.run.xml
│   ├── Win启动依赖服务.run.xml
│   └── ...
│
├── .lingma/commands/        # Lingma AI 命令
│   └── dev-command.md               # 开发常用命令
│
├── pom.xml                  # Maven 配置文件
├── README.md                # 项目说明文档
├── AGENTS.md                # AI Agent 开发规范（本文件）
├── .gitignore               # Git 忽略配置
├── .editorconfig            # 编辑器配置
└── icon.png                 # 项目图标
```

## 配置文件说明

### 配置加载优先级

**重要原则**：

- ✅ **推荐做法**：在 `~/.pix_vision/application.yml` 中覆盖需要的配置项
- 🚫 **避免做法**：直接修改 `src/main/resources/` 下的配置文件
- 💡 **原因**：
  1. 项目更新时会覆盖 `src/main/resources/` 下的文件
  2. 用户配置与代码分离，便于版本控制和部署
  3. 不同环境可以使用不同的用户配置

**配置加载顺序**（从高到低）：

```
🔝 最高优先级  ~/.pix_vision/application.yml          # 用户自定义配置（✅ 推荐修改）
   ↓
🔧 中等优先级  src/main/resources/yml-config/*.yml    # 核心配置（🚫 不建议修改）
   ↓
📄 最低优先级  src/main/resources/application.yml     # 基础配置（🚫 不建议修改）
```

### 配置文件详解

#### 1. application.yml（主配置）

- **位置**：`src/main/resources/application.yml`
- **作用**：项目主配置，定义服务端口、配置文件导入等
- **关键配置**：
  ```yaml
  workspace-name: .pix_vision  # 工作空间名称
  server:
      port: 9090               # 服务端口
  spring:
      config:
          import:              # 引入的配置文件
              - classpath:yml-config/email.yml
              - classpath:yml-config/jdbc.yml
              - classpath:yml-config/redis.yml
              - classpath:yml-config/mybatis-plus.yml
              - classpath:yml-config/pix_vision.yml
              - classpath:yml-config/spring-doc.yml
              - classpath:yml-config/logging.yml
              - optional:file:${user.home}/${workspace-name}/application.yml
  ```

#### 2. yml-config/ 目录（核心配置模板）

- **jdbc.yml**：数据库连接配置
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
  ```

- **redis.yml**：Redis 缓存配置
  ```yaml
  spring:
    data:
      redis:
        host: localhost
        port: 6379
        timeout: 5000
        database: 0
        jedis:
          pool:
            max-active: 8
            max-idle: 8
            min-idle: 0
  ```

- **email.yml**：邮件服务配置
  ```yaml
  spring:
    mail:
      host: smtp.qq.com
      port: 465
      from: your-email@example.com
      username: server_username
      password: your_auth_code  # 授权码，非登录密码
      protocol: smtp
      default-encoding: UTF-8
  ```

- **mybatis-plus.yml**：MyBatis-Plus 配置
  ```yaml
  mybatis-plus:
    mapper-locations: classpath:mapper/*.xml
    type-aliases-package: top.playereg.pix_vision.pojo
    configuration:
      map-underscore-to-camel-case: true
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    global-config:
      db-config:
        logic-delete-field: is_delete
        logic-delete-value: 1
        logic-not-delete-value: 0
  ```

- **spring-doc.yml**：API 文档配置
  ```yaml
  springdoc:
    enabled: on  # on/off 控制文档开关
    api-docs:
      path: /v3/api-docs
    swagger-ui:
      path: /swagger-ui.html
  knife4j:
    enable: true
    setting:
      language: zh_cn
  ```

- **logging.yml**：日志配置
- **pix_vision.yml**：应用自定义配置

#### 3. 用户自定义配置

- **位置**：`${user.home}/.pix_vision/application.yml`
- **作用**：覆盖预设配置，适配不同环境
- **首次启动**：项目自动创建该目录和配置模板

### 模板文件初始化

`src/main/resources/template/`
目录存放模板文件。项目初始化时，使用 `src/main/java/top/playereg/pix_vision/util/CreateFile.java` 的 `create()`
方法将模板复制到用户文件夹 `~/.pix_vision/`。

**运行时目录结构**：

```
~/.pix_vision/
├── application.yml          # 用户自定义配置
├── readme.txt               # 目录说明
├── data/                    # 数据文件目录
│   ├── logo-img/           # Logo 图片
│   └── avatar/             # 用户头像
├── config/                  # 配置文件目录
│   └── email-html/         # 邮件 HTML 模板
├── log/                     # 日志文件目录
│   ├── pix_vision.log      # 应用日志
│   └── error.log           # 错误日志
└── key/                     # 密钥文件目录
    └── rsa/                # RSA 密钥对
        ├── public.key      # RSA 公钥
        ├── private.key     # RSA 私钥
        └── *.bak           # 旧密钥备份
```

---

# 代码风格规范

## 1. 命名规范

### 类命名

- **Controller**：以 `Controller` 结尾，如 `UserController`
- **Service 接口**：以 `Service` 结尾，如 `UserService`
- **Service 实现**：以 `ServiceImpl` 结尾，如 `UserServiceImpl`
- **Mapper**：以 `Mapper` 结尾，如 `UserMapper`
- **POJO/Entity**：使用业务名称，如 `User`、`ResponsePojo`
- **工具类**：以 `Utils` 或 `Util` 结尾，如 `JWTUtils`、`RegexUtils`
- **配置类**：以 `Config` 结尾，如 `WebConfig`、`RedisConfig`
- **枚举类**：使用名词，如 `LogColor`

### 方法命名

- **查询单个**：`select*By*`，如 `selectAllUserByUsername`
- **查询列表**：`select*List` 或 `selectPage*`
- **新增**：`insert*` 或 `register*`、`create*`
- **修改**：`update*` 或 `change*`
- **删除**：`delete*` 或 `remove*`
- **验证**：`verify*` 或 `is*Exists`、`is*Valid`
- **生成**：`generate*` 或 `create*`

### 变量命名

- **局部变量**：驼峰命名，如 `userId`、`userName`
- **常量**：全大写+下划线，如 `TOKEN_EXPIRE_TIME`、`SECRET_KEY`
- **Logger**：统一命名为 `log`
  ```java
  private static final Logger log = LoggerFactory.getLogger(ClassName.class);
  ```

### 参数命名

- **布尔类型**：使用 `is*`、`has*`、`can*` 前缀
- **集合类型**：使用复数形式，如 `users`、`ids`
- **可选参数**：在注解中标注 `required = false`

## 2. 注释规范

### 类注释

```java
/**
 * 用户操作相关接口
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
  // ...
}
```

### 方法注释（必须包含）

```java
/**
 * 用户登录
 *
 * @param usernameOrEmail 用户名或邮箱
 * @param password         密码
 * @param vCode            验证码
 * @return 响应数据<UserLogin>，包含用户信息和 Token
 * @author PlayerEG
 */
```

### 行内注释

- 使用 `//` 进行单行注释
- 重要逻辑块前添加注释说明
- 调试日志前添加注释

### JavaDoc 要求

- 所有 public 方法必须有 JavaDoc
- 包含 `@param`、`@return`、`@author` 标签
- 复杂逻辑需要在 JavaDoc 中说明业务规则

## 3. 代码格式

### 缩进与空格

- 使用 **4 个空格** 缩进（不使用 Tab）
- 运算符两侧加空格：`a + b`、`x == y`
- 逗号后加空格：`method(a, b, c)`
- 控制语句关键字后加空格：`if (condition)`、`for (int i = 0; ...)`

### 大括号规范

- 左大括号不换行，右大括号独占一行
- 即使只有一行代码，也必须使用大括号
  ```java
  if (condition) {
      doSomething();
  }
  ```

### 空行规范

- 方法之间空一行
- 逻辑块之间空一行
- 字段声明与方法之间空一行

### 导入顺序

```java
// 1. JDK 标准库

import java.util.*;

// 2. 第三方库
import lombok.Data;
import org.springframework.web.bind.annotation.*;

// 3. 项目内部包
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.UserService;
```

## 4. 异常处理

### 原则

- 优先返回错误码和消息，而不是抛出异常
- 仅在严重错误时抛出异常（如数据不一致）
- 使用 `ResponsePojo.error()` 返回错误信息

### 示例

```java
// ✅ 推荐：返回错误响应
if(user ==null){
  return ResponsePojo.

error(null,"用户不存在");
}

// ❌ 避免：直接抛出异常（除非必要）
  throw new

RuntimeException("用户不存在");
```

## 5. 日志规范

### 日志级别

- **DEBUG**：调试信息，详细的数据流
- **INFO**：关键业务流程（登录成功、注册成功等）
- **WARN**：警告信息（Token 失效、参数异常等）
- **ERROR**：错误信息（数据库失败、系统异常等）

### 日志格式

```java
// ✅ 推荐：使用占位符
log.info("用户登录成功：{}",username);
log.

error("无法从 Token 中获取用户 ID");

// ❌ 避免：字符串拼接
log.

info("用户登录成功："+username);
```

### 敏感信息脱敏

- 密码、Token 等敏感信息不应完整记录
- 可以记录哈希值或部分字符
  ```java
  log.debug("Token: {}", token != null ? token.substring(0, 10) + "..." : "null");
  ```

## 6. 依赖注入

### 推荐方式

- 使用 **构造器注入**（配合 Lombok `@RequiredArgsConstructor`）
  ```java
  @RestController
  @RequiredArgsConstructor
  public class UserController {
      private final UserService userService;
      private final VerificationCodeServices verificationCodeServices;
  }
  ```

### 备选方式

- 对于测试类或特殊情况，可使用 `@Autowired` 字段注入

## 7. 返回值规范

### ResponsePojo 使用

- **成功响应**：`ResponsePojo.success(data, message)`
- **失败响应**：`ResponsePojo.error(data, message)`

### 数据类型选择

#### 1. 布尔值场景

使用 `Boolean` 类型（`true`/`false`）

```java
public ResponsePojo<Boolean> logout(HttpServletRequest request) {
  return ResponsePojo.success(true, "登出成功");
}
```

#### 2. 对象场景

返回具体对象或 `null`

```java
public ResponsePojo<User> registerUser(...) {
  return ResponsePojo.success(user, "注册成功");
}
```

#### 3. 列表/分页场景

返回 `IPage<T>` 或 `List<T>`

```java
public ResponsePojo<IPage<User>> getPageUserInfo(...) {
  IPage<User> page = userService.selectPageUserInfo(...);
  return ResponsePojo.success(page, "查询成功");
}
```

### 状态码规范

- **200**：成功（`ResponsePojo.success()`）
- **500**：失败（`ResponsePojo.error()`）
- **401**：未授权（拦截器自动返回）

### ResponsePojo 结构

```java
{
  "data":{...},      // 响应数据（泛型 T）
  "message":"提示信息", // 响应说明信息
  "recode":200,        // 响应状态码
  "status":null        // 保留字段
  }
```

---

# Swagger 文档规范

## 1. 基本要求

- 所有 Controller 方法必须添加 `@Operation` 注解
- 补全 `summary` 和 `description` 参数
- 使用 `@Parameter` 注解描述请求参数
- 使用 `@Tag` 注解对接口分组

## 2. Description 格式规范

### 标准模板

```markdown
# 接口标题

## 参数说明：

- 参数1: 描述，类型，是否必填
- 参数2: 描述，类型，是否必填

## 返回说明：

- **XX成功**：返回 **{"data": true}** 和提示信息
- **XX失败**：返回 **{"data": false}** 和提示信息

## 业务逻辑：

1. 步骤1
2. 步骤2
3. 步骤3

## 注意事项：

- 注意事项1
- 注意事项2
```

### 格式化要求

- **重点内容**：使用 `**粗体**`
- *次要强调*：使用 `*斜体*`
- ~~废弃内容~~：使用 `~~删除线~~`
- 代码块：使用反引号 `` `code` ``
- JSON 示例：使用三个反引号包裹

## 3. 完整示例

```java

@PostMapping("/login")
@Operation(
  summary = "用户登录接口",
  description = """
    # 用户登录
            
    ## 参数说明：
    - usernameOrEmail: **用户名**或**邮箱地址**，字符串类型，必填
    - password: 登录密码，字符串类型，必填
    - vCode: 邮箱验证码（6 位大写字母或数字），字符串类型，必填
            
    ## 返回说明：
    - **登录成功**：返回 **{"data": {UserLogin 对象}}**，包含用户信息和 JWT Token
    - **用户名或邮箱格式错误**：返回 **{"data": null}** 和"用户名或邮箱格式错误"提示
    - **验证码错误**：返回 **{"data": null}** 和"验证码错误"提示
    - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示
    - **用户名或密码错误**：返回 **{"data": null}** 和"用户名或密码错误"提示
            
    ## 业务逻辑：
    1. 校验用户名格式、邮箱格式、验证码格式
    2. 验证邮箱验证码是否正确（如提供用户名则先查询邮箱）
    3. 根据用户名或邮箱查询用户信息
    4. 验证密码是否正确（比对 SHA-256 哈希值）
    5. 检查用户状态是否正常（status=10 表示正常）
    6. 生成 JWT Token（有效期 7 天）
    7. 将 Token 加入白名单
    8. 返回用户信息和 Token
            
    ## 注意事项：
    - Token 有效期为 **7 天**
    - Token 需要保存在客户端，后续请求需在 Header 中携带
    - 建议使用 **HTTPS** 传输以保障安全
    - 密码会自动进行 SHA-256 哈希加密处理后再比对
    - 支持使用用户名**或**邮箱登录
            
    ## Token 使用方式：
    - Header 中添加：`Authorization: Bearer <token>`
    - 或者 URL 参数：`?token=<token>`
    """
)
public ResponsePojo<UserLogin> login(
  @Parameter(description = "用户名或邮箱，6-16 位字母/数字/下划线或标准邮箱格式", required = true, example = "dev_user")
  @RequestParam String usernameOrEmail,
  @Parameter(description = "登录密码，会使用 SHA-256 加密后比对", required = true, example = "123456")
  @RequestParam String password,
  @Parameter(description = "邮箱验证码，6 位大写字母或数字", required = true, example = "ABCDEF")
  @RequestParam String vCode
) {
  // 实现代码...
}
```

## 4. Parameter 注解规范

```java
@Parameter(
  description = "参数描述，包含格式要求和约束条件",
  required = true,  // 或 false
  example = "示例值"
)
```

### 描述要点

- 说明参数类型（字符串、整数等）
- 说明格式要求（长度、正则等）
- 说明是否必填
- 提供典型示例

---

# 架构设计规范

## 1. 分层架构

### Controller 层

- 负责接收请求和返回响应
- 参数校验和基础验证
- 调用 Service 层处理业务逻辑
- **禁止**：直接操作数据库或包含复杂业务逻辑

### Service 层

- **接口**：定义业务方法签名
- **实现类**：实现具体业务逻辑
- 事务管理（如需）
- 调用 Mapper 层进行数据操作
- **禁止**：直接处理 HTTP 请求或响应

### Mapper 层

- MyBatis-Plus Mapper 接口
- 继承 `BaseMapper<T>` 获得基础 CRUD
- 自定义 SQL 写在 XML 文件中
- **禁止**：包含业务逻辑

### POJO 层

- **Entity**：数据库实体类，对应表结构
- **DTO**：数据传输对象，用于接口传输
- **VO**：视图对象，用于前端展示
- 使用 Lombok 简化代码（`@Data`、`@Builder` 等）

## 2. 通用逻辑封装

### Service 层复用原则

- 将通用的 Token 验证、用户查询逻辑封装到 Service 层
- 避免在多个 Controller 中重复相同逻辑
- 示例：
  ```java
  // ✅ 推荐：封装通用方法
  public User getCurrentUserFromToken(String token) {
      Integer userId = JWTUtils.getUserIdFromToken(token);
      return selectAllUserById(userId);
  }
  
  // ❌ 避免：在每个 Controller 中重复
  Integer userId = JWTUtils.getUserIdFromToken(token);
  User user = userMapper.selectById(userId);
  ```

## 3. 安全性规范

### 密码处理

- **加密算法**：使用 **SHA-256** 哈希加密存储
- **工具类**：`StrSwitchUtils.PasswdToHash256(password)`
- **安全要求**：
  - 不在日志中记录明文密码
  - 密码比对前先加密再比较
  - 修改密码、忘记密码重置时强制所有 Token 失效

### Token 管理

#### JWT Token 特性

- **有效期**：**7 天**（604800000 毫秒）
- **生成工具**：`JWTUtils.createToken(userId, username)`
- **验证机制**：JWT 签名验证 + Redis 白名单双重校验
- **传递方式**：
  - Header：`Authorization: Bearer <token>`（推荐）
  - URL 参数：`?token=<token>`

#### Token 白名单机制

- **服务类**：`TokenWhitelistService`
- **存储位置**：Redis
- **过期时间**：与 JWT 有效期一致（7 天）
- **自动续期**：每次请求自动刷新过期时间

#### Token 失效场景

以下操作会将 Token 从白名单移除：

1. **用户登出**：`/api/user/logout`
2. **修改密码**：`/api/user/change-password`（移除该用户所有 Token）
3. **忘记密码重置**：`/api/user/forgot-password`（移除该用户所有 Token）
4. **手动移除**：`tokenWhitelistService.removeFromWhitelist(token)`
5. **批量移除**：`tokenWhitelistService.removeAllUserTokens(userId, username)`

#### 拦截器验证流程

[JwtAuthenticationInterceptor](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/handler/JwtAuthenticationInterceptor.java)
执行顺序：

1. 从 Header 或 URL 参数提取 Token
2. 检查 Token 是否存在
3. 去除 "Bearer " 前缀（如果有）
4. 检查 Token 是否在白名单中
5. 验证 JWT 签名是否有效
6. 检查 Token 是否过期
7. 从 Token 中提取用户信息并存入 `request.setAttribute()`
8. 验证通过，继续执行请求

### 输入验证

#### 正则表达式验证

使用 [RegexUtils](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/RegexUtils.java)
工具类：

```java
// 用户名验证：6-16 位字母/数字/下划线
RegexUtils.isUsername(username)

// 邮箱验证：标准邮箱格式
RegexUtils.

isEmail(email)

// 验证码验证：指定位数的大写字母或数字
RegexUtils.

isVCode(vCode, 6)

// UUID 验证：标准 UUID 格式
RegexUtils.

isUUID(uuid)
```

#### 防止常见攻击

- **SQL 注入**：使用 MyBatis-Plus 参数化查询
- **XSS 攻击**：对用户输入进行转义处理
- **CSRF 攻击**：使用 Token 认证机制
- **暴力破解**：验证码限制、登录失败次数限制（待实现）

### 敏感操作二次验证

#### 需要邮箱验证码的操作

- **用户注册**：验证邮箱所有权
- **用户登录**：防止暴力破解
- **修改密码**：确认用户身份
- **忘记密码重置**：验证邮箱所有权

#### 验证码机制

- **服务类**：`VerificationCodeServices`
- **存储位置**：Redis
- **键名格式**：`verification:code:{email}`
- **有效期**：5 分钟（可配置）
- **格式**：6 位大写字母或数字
- **一次性使用**：验证成功后立即删除

### RSA 加密工具

项目集成了强大的 RSA + AES
混合加密工具 [RSACipher](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/RSACipher.java)：

#### 核心特性

- ✨ **智能加密策略**：
  - 小数据（≤ 245 字节）：纯 RSA 加密
  - 大数据（> 245 字节）：AES + RSA 混合加密
- 📦 **支持任意数据类型**：文本、JSON、图片、文件等
- 🔐 **无大小限制**：突破 RSA 245 字节限制
- 🔄 **自动密钥管理**：启动时生成/加载密钥对
- 💾 **密钥文件存储**：`~/.pix_vision/key/rsa/`

#### 使用示例

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

#### 密钥管理

- **公钥文件**：`~/.pix_vision/key/rsa/public.key`
- **私钥文件**：`~/.pix_vision/key/rsa/private.key`
- **备份文件**：`.bak` 后缀（更换密钥时自动备份）
- **重新生成**：`RSACipher.regenerateKeys()`

⚠️ **重要警告**：

- 更换密钥后，**旧密钥加密的数据无法用新密钥解密**
- 更换前务必备份所有加密数据
- 私钥文件权限应设置为只读（chmod 600）

## 4. 数据库规范

### 表命名

- **统一前缀**：`tb_`
- **命名规则**：小写字母+下划线
- **示例**：`tb_user`、`tb_user_data`、`tb_works`、`tb_comments`

### 字段命名

- **规则**：小写字母+下划线
- **主键**：`{table}_id`（如 `user_id`、`work_id`）或 `id`
- **外键**：`{ref_table}_id`（如 `user_id` 引用 `tb_user`）
- **通用字段**：
  - `is_delete`：逻辑删除标记（0=未删除，1=已删除）
  - `create_time`：创建时间戳
  - `create_user`：创建者 ID
  - `update_time`：更新时间戳
  - `update_user`：修改者 ID

### 逻辑删除

- **字段**：`is_delete`（tinyint(1)）
- **值含义**：
  - `false`（0）：未删除
  - `true`（1）：已删除
- **MyBatis-Plus 配置**：自动过滤已删除记录
  ```yaml
  mybatis-plus:
    global-config:
      db-config:
        logic-delete-field: is_delete
        logic-delete-value: 1
        logic-not-delete-value: 0
  ```

### UUID 处理

#### 存储格式

- **数据库**：16 字节二进制（`BINARY(16)`）
- **接口传输**：标准 UUID 字符串（36 字符，如 `550e8400-e29b-41d4-a716-446655440000`）

#### 转换工具

使用 [StrSwitchUtils](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/StrSwitchUtils.java)：

```java
// 字符串 UUID → 16 字节二进制
byte[] uuidBytes = StrSwitchUtils.uuid2Bytes("550e8400-e29b-41d4-a716-446655440000");

// 16 字节二进制 → 字符串 UUID
String uuidString = StrSwitchUtils.bytes2Uuid(uuidBytes);
```

#### 实体类处理

在 User 实体中同时提供两种格式：

```java

@Data
public class User {
  private byte[] user_uuid;          // 数据库存储格式
  private String string_user_uuid;   // 接口传输格式（非数据库字段）
}
```

### 用户角色与状态

#### 用户角色（user_role）

| 代码 | 角色名称  | 权限说明        |
|:--:|-------|-------------|
| 11 | 普通用户  | 浏览、评论、点赞、收藏 |
| 22 | 创作者   | 发布作品、管理作品   |
| 55 | 审核员   | 审核作品、处理举报   |
| 66 | 工单管理员 | 处理用户工单      |
| 77 | 系统管理员 | 全部权限        |

#### 用户状态（status）

| 代码 | 状态名称  | 说明       |
|:--:|-------|----------|
| 10 | ✅ 正常  | 正常使用所有功能 |
| 20 | ⚠️ 冻结 | 暂时限制部分功能 |
| 30 | 🚫 封禁 | 禁止登录和使用  |

### 数据表清单

| 表名             | 中文名    | 主要功能           |   状态   |
|----------------|--------|----------------|:------:|
| `tb_user`      | 用户账户   | 用户基本信息、认证信息    | ✅ 已完成  |
| `tb_user_data` | 用户扩展数据 | 用户额外信息（电话、网站等） | ✅ 已完成  |
| `tb_works`     | 作品     | 作品信息、图片、统计     | 🚧 开发中 |
| `tb_series`    | 作品系列   | 作品合集、系列管理      | 🚧 开发中 |
| `tb_comments`  | 评论     | 多级评论、回复        | 🚧 开发中 |
| `tb_like`      | 点赞     | 作品点赞记录         | 🚧 开发中 |
| `tb_star`      | 收藏     | 作品收藏记录         | 🚧 开发中 |
| `tb_history`   | 浏览历史   | 用户浏览记录         | 🚧 开发中 |
| `tb_sys_logs`  | 系统日志   | 操作日志记录         | ✅ 已完成  |

> ✅ 已完成 | 🚧 开发中

## 5. Redis 使用规范

### 键命名规范

- **格式**：`{业务}:{标识}:{具体key}`
- **示例**：
  - 验证码：`verification:code:user@example.com`
  - Token 白名单：`token:whitelist:{token}`
  - 用户缓存：`user:info:{userId}`

### 过期时间

- **验证码**：5 分钟（300 秒，可配置）
- **Token 白名单**：与 JWT 有效期一致（7 天）
- **普通缓存**：根据业务需求设置（默认 30 分钟）

### 序列化

- **Key 序列化**：`StringRedisSerializer`（字符串）
- **Value 序列化**：`GenericJackson2JsonRedisSerializer`（JSON）
- **Hash Key/Value**：同上

### 连接池配置

```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          enabled: true
          max-active: 8      # 最大活跃连接数
          max-idle: 8        # 最大空闲连接数
          min-idle: 0        # 最小空闲连接数
          max-wait: -1       # 最大等待时间（-1=无限制）
          time-between-eviction-runs: 1000  # 连接空闲检测间隔
```

### 常用操作

```java

@Autowired
private StringRedisTemplate redisTemplate;

// 设置字符串值（带过期时间）
redisTemplate.

opsForValue().

set("key","value",5,TimeUnit.MINUTES);

// 获取字符串值
String value = redisTemplate.opsForValue().get("key");

// 删除键
redisTemplate.

delete("key");

// 检查键是否存在
Boolean exists = redisTemplate.hasKey("key");
```

---

# 测试规范

## 1. 单元测试

### 测试类命名

- 格式：`{被测试类名}Test`
- 位置：`src/test/java/` 对应包路径

### 测试方法命名

- 格式：`test{方法名}{场景}`
- 示例：`testLoginWithValidCredentials`、`testRegisterWithDuplicateUsername`

### 测试覆盖

- 每个 Service 方法至少一个测试用例
- 覆盖正常流程和异常流程
- 边界值测试

## 2. 集成测试

- 使用 `@SpringBootTest` 启动完整上下文
- 测试数据库操作使用 H2 内存数据库或测试数据库
- 测试完成后清理测试数据

---

# Git 提交规范

## 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具变动

### 示例

```bash
git commit -m "feat(user): 添加用户密码修改接口

- 实现登录后修改密码功能
- 添加邮箱验证码验证
- 修改成功后使当前 Token 失效

Closes #123"
```

---

# 常见陷阱与最佳实践

## 1. 常见陷阱

### ❌ 避免硬编码

```java
// ❌ 错误
long expireTime = 604800000L;

// ✅ 正确
private static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
```

### ❌ 避免空指针

```java
// ❌ 错误
if(user.getUsername().

equals("admin")){...}

// ✅ 正确
  if(user !=null&&"admin".

equals(user.getUsername())){...}
```

### ❌ 避免魔法数字

```java
// ❌ 错误
if(user.getStatus() !=10){...}

// ✅ 正确
private static final int USER_STATUS_NORMAL = 10;
if(user.

getStatus() !=USER_STATUS_NORMAL){...}
```

## 2. 最佳实践

### ✅ 使用 Optional 处理可能为空的结果

```java
Optional<User> user = Optional.ofNullable(userService.selectById(id));
user.

ifPresent(u ->log.

info("找到用户：{}",u.getUsername()));
```

### ✅ 批量操作使用事务

```java

@Transactional
public void batchUpdateUsers(List<User> users) {
  users.forEach(this::updateUser);
}
```

### ✅ 合理使用缓存

- 频繁读取且变化少的数据使用 Redis 缓存
- 设置合理的过期时间
- 注意缓存一致性

### ✅ 日志分级记录

```java
log.debug("调试信息：参数值={}",param);
log.

info("业务流程：用户登录成功");
log.

warn("警告信息：Token 即将过期");
log.

error("错误信息：数据库连接失败",exception);
```

---

# 开发工作流程

## 1. 新增接口流程

1. **设计接口**：确定 URL、请求方式、参数、返回值
2. **创建/更新 POJO**：定义请求和响应对象
3. **编写 Service 接口**：定义业务方法
4. **实现 Service**：编写业务逻辑
5. **创建 Controller**：实现接口，添加 Swagger 文档
6. **编写测试**：单元测试和集成测试
7. **代码审查**：检查代码规范和文档完整性
8. **提交代码**：按照 Git 规范提交

## 2. 修改现有功能流程

1. **理解需求**：明确修改范围和目标
2. **影响分析**：评估对相关模块的影响
3. **编写测试**：先写测试用例（TDD）
4. **实施修改**：修改代码
5. **运行测试**：确保所有测试通过
6. **更新文档**：同步更新 Swagger 文档和注释
7. **代码审查**：同行评审
8. **提交代码**

## 3. Bug 修复流程

1. **复现问题**：确认 Bug 存在
2. **定位原因**：通过日志和调试找到根本原因
3. **编写测试**：添加能复现 Bug 的测试用例
4. **修复问题**：修改代码
5. **验证修复**：运行测试，确认问题解决
6. **回归测试**：确保没有引入新问题
7. **提交代码**：在提交信息中引用 Issue 编号

---

# API 接口清单

## 🔐 用户接口 `/api/user`

| 方法   | 路径                       | 说明        | 认证 | 主要参数                                                 |
|------|--------------------------|-----------|:--:|------------------------------------------------------|
| POST | `/register`              | 用户注册      | ❌  | username, password, nickname(可选), email, vCode       |
| POST | `/login`                 | 用户登录      | ❌  | usernameOrEmail, password, vCode                     |
| POST | `/logout`                | 用户登出      | ✅  | token (Header/URL)                                   |
| POST | `/change-password`       | 修改密码（需登录） | ✅  | newPassword, confirmPassword, vCode                  |
| POST | `/forgot-password`       | 忘记密码重置    | ❌  | usernameOrEmail, newPassword, confirmPassword, vCode |
| GET  | `/page/{current}/{size}` | 分页查询用户    | ✅  | current, size, username(可选), uuid(可选), email(可选)     |

## 📧 邮件接口 `/api/mail`

| 方法   | 路径                        | 说明          | 认证 | 主要参数                                   |
|------|---------------------------|-------------|:--:|----------------------------------------|
| POST | `/send-email-code`        | 发送邮箱验证码     | ❌  | to, subject, username(条件必填), emailText |
| POST | `/verify-email-code-test` | 验证邮箱验证码（测试） | ❌  | email, code                            |

## 🖥️ 系统接口

| 方法  | 路径             | 说明    | 认证 | 返回                   |
|-----|----------------|-------|:--:|----------------------|
| GET | `/`            | 首页重定向 | ❌  | HTML 页面              |
| GET | `/health`      | 健康检查  | ❌  | {"status": "UP"}     |
| GET | `/system-info` | 系统信息  | ❌  | JVM/OS/CPU/Memory 信息 |

## 🧪 测试接口 `/7e212056`

| 方法  | 路径              | 说明       | 认证 | 返回     |
|-----|-----------------|----------|:--:|--------|
| GET | `/require-auth` | JWT 鉴权测试 | ✅  | 当前用户信息 |

---

# 环境配置

## 开发环境要求

| 依赖        | 版本            | 说明                   |
|-----------|---------------|----------------------|
| **JDK**   | 17+           | 推荐使用 OpenJDK 17      |
| **Maven** | 3.6+          | 项目构建工具               |
| **MySQL** | 8.0+          | 关系型数据库               |
| **Redis** | 8.0+          | 缓存服务                 |
| **SMTP**  | -             | 邮件服务器（QQ/163/Gmail等） |
| **IDE**   | IntelliJ IDEA | 推荐（已配置 .run 文件）      |

## 启动步骤

### 1. 克隆项目

```bash
git clone <repository-url>
cd PixVisionServer
```

### 2. 初始化数据库

```bash
# 创建数据库并导入表结构
mysql -u root -p < sql/db_pix_vision-V1.1.sql
```

### 3. 配置应用

⚠️ **重要提示**：

- 🚫 **不要直接修改** `src/main/resources/` 下的配置文件
- ✅ **应该修改** 用户目录 `~/.pix_vision/application.yml` 中的配置
- 💡 项目内的配置文件是**预设模板**，仅作为参考

**首次启动时**，项目会自动在用户主目录下创建 `.pix_vision` 目录和配置文件模板。

**编辑用户配置文件** `~/.pix_vision/application.yml`：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision?allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_db_password

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果有密码则填写

  # 邮件配置
  mail:
    host: smtp.qq.com
    port: 465
    from: your-email@qq.com
    username: your-email@qq.com
    password: your_auth_code  # 授权码，非登录密码！
```

### 4. 启动应用

**方式一：IDE 运行**

- 在 IDE
  中打开 [PixVisionApplication.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/PixVisionApplication.java)
- 点击运行按钮或按 `Shift + F10`

**方式二：Maven 插件运行**

```bash
mvn spring-boot:run
```

**方式三：JAR 包运行**

```bash
# 编译打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/PixVision-0.0.1-SNAPSHOT.jar
```

## 访问地址

应用启动成功后，可以访问以下地址：

| 服务                | 地址                                    | 说明                   |
|-------------------|---------------------------------------|----------------------|
| 🏠 **首页**         | http://localhost:9090                 | 欢迎页面                 |
| 💚 **健康检查**       | http://localhost:9090/health          | 服务状态                 |
| 🖥️ **系统信息**      | http://localhost:9090/system-info     | JVM/OS/CPU/Memory 信息 |
| 📚 **Swagger UI** | http://localhost:9090/swagger-ui.html | API 文档（OpenAPI）      |
| 🎯 **Knife4j UI** | http://localhost:9090/doc.html        | API 文档（增强版，推荐）       |

⚠️ **注意**: API 文档默认关闭，需在 `src/main/resources/yml-config/spring-doc.yml` 中设置 `springdoc.enabled: on` 启用。

---

# 附录

## 常用工具类

### 核心工具类

| 工具类                 | 位置                                                                                                                                  | 主要功能                     |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| **JWTUtils**        | [util/JWTUtils.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/JWTUtils.java)               | JWT Token 生成、验证、解析       |
| **RSACipher**       | [util/RSACipher.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/RSACipher.java)             | RSA + AES 混合加密/解密        |
| **RegexUtils**      | [util/RegexUtils.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/RegexUtils.java)           | 正则表达式验证（用户名、邮箱、验证码、UUID） |
| **StrSwitchUtils**  | [util/StrSwitchUtils.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/StrSwitchUtils.java)   | 字符串转换（UUID、密码哈希、随机昵称生成）  |
| **ImageUtils**      | [util/ImageUtils.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/ImageUtils.java)           | 图片处理（缩放、裁剪、格式转换）         |
| **IpUtil**          | [util/IpUtil.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/IpUtil.java)                   | IP 地址获取工具                |
| **ConversionUtils** | [util/ConversionUtils.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/ConversionUtils.java) | 数据类型转换工具                 |
| **CreateFile**      | [util/CreateFile.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/CreateFile.java)           | 文件创建工具（初始化模板文件）          |

### AOP 切面

| 切面类           | 位置                                                                                                                                    | 主要功能   |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------|--------|
| **LogAspect** | [util/Aspect/LogAspect.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/Aspect/LogAspect.java) | 日志记录切面 |
| **LogRecord** | [util/Aspect/LogRecord.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/Aspect/LogRecord.java) | 日志记录注解 |

### 使用示例

#### JWT Token 操作

```java
// 生成 Token
String token = JWTUtils.createToken(userId, username);

// 验证 Token
boolean isValid = JWTUtils.verifyToken(token);

// 从 Token 中提取用户 ID
Integer userId = JWTUtils.getUserIdFromToken(token);

// 从 Token 中提取用户名
String username = JWTUtils.getUsernameFromToken(token);

// 检查 Token 是否过期
boolean isExpired = JWTUtils.isTokenExpired(token);

// 获取 Token 剩余时间（毫秒）
long remainingTime = JWTUtils.getTokenRemainingTime(token);
```

#### 正则表达式验证

```java
// 用户名验证：6-16 位字母/数字/下划线
boolean isValid = RegexUtils.isUsername("dev_user");

// 邮箱验证
boolean isValid = RegexUtils.isEmail("test@example.com");

// 验证码验证：6 位大写字母或数字
boolean isValid = RegexUtils.isVCode("ABCDEF", 6);

// UUID 验证
boolean isValid = RegexUtils.isUUID("550e8400-e29b-41d4-a716-446655440000");
```

#### 字符串转换

```java
// 密码 SHA-256 哈希
String hashed = StrSwitchUtils.PasswdToHash256("password123");

// UUID 字符串 → 16 字节二进制
byte[] uuidBytes = StrSwitchUtils.uuid2Bytes("550e8400-e29b-41d4-a716-446655440000");

// 16 字节二进制 → UUID 字符串
String uuidString = StrSwitchUtils.bytes2Uuid(uuidBytes);

// 生成随机默认昵称
String nickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");
```

#### RSA 加密/解密

```java
// 加密字符串
String encrypted = RSACipher.encryptToBase64("敏感信息");

// 解密字符串
String decrypted = RSACipher.decryptToString(encrypted);

// 加密二进制数据
byte[] data = Files.readAllBytes(Paths.get("photo.jpg"));
String encrypted = RSACipher.encryptToBase64(data);

// 解密二进制数据
byte[] original = RSACipher.decryptToBytes(encrypted);
```

## 常用注解

### Spring Boot 注解

| 注解                | 作用          | 示例                                                 |
|-------------------|-------------|----------------------------------------------------|
| `@RestController` | RESTful 控制器 | `@RestController`                                  |
| `@RequestMapping` | 请求映射        | `@RequestMapping("/api/user")`                     |
| `@GetMapping`     | GET 请求映射    | `@GetMapping("/{id}")`                             |
| `@PostMapping`    | POST 请求映射   | `@PostMapping("/login")`                           |
| `@PutMapping`     | PUT 请求映射    | `@PutMapping("/{id}")`                             |
| `@DeleteMapping`  | DELETE 请求映射 | `@DeleteMapping("/{id}")`                          |
| `@RequestParam`   | 请求参数        | `@RequestParam String name`                        |
| `@PathVariable`   | 路径变量        | `@PathVariable Long id`                            |
| `@RequestBody`    | 请求体         | `@RequestBody User user`                           |
| `@Service`        | 服务层组件       | `@Service`                                         |
| `@Autowired`      | 自动注入        | `@Autowired private UserService userService;`      |
| `@Transactional`  | 事务管理        | `@Transactional`                                   |
| `@Configuration`  | 配置类         | `@Configuration`                                   |
| `@Bean`           | Bean 定义     | `@Bean public RedisTemplate redisTemplate() {...}` |

### Lombok 注解

| 注解                         | 作用                          | 示例                                  |
|----------------------------|-----------------------------|-------------------------------------|
| `@Data`                    | 自动生成 getter/setter/toString | `@Data public class User {...}`     |
| `@RequiredArgsConstructor` | 生成 final 字段构造器              | `@RequiredArgsConstructor`          |
| `@Builder`                 | 构建者模式                       | `@Builder public class User {...}`  |
| `@Slf4j`                   | 自动生成 log 对象                 | `@Slf4j public class Service {...}` |
| `@NoArgsConstructor`       | 无参构造器                       | `@NoArgsConstructor`                |
| `@AllArgsConstructor`      | 全参构造器                       | `@AllArgsConstructor`               |

### Swagger/OpenAPI 注解

| 注解           | 作用       | 示例                                                  |
|--------------|----------|-----------------------------------------------------|
| `@Tag`       | API 标签分组 | `@Tag(name = "用户操作相关接口")`                           |
| `@Operation` | 接口描述     | `@Operation(summary = "用户登录", description = "...")` |
| `@Parameter` | 参数描述     | `@Parameter(description = "用户名", required = true)`  |
| `@Schema`    | 模型描述     | `@Schema(description = "响应数据类型")`                   |

### MyBatis-Plus 注解

| 注解            | 作用        | 示例                                          |
|---------------|-----------|---------------------------------------------|
| `@Mapper`     | Mapper 接口 | `@Mapper public interface UserMapper {...}` |
| `@TableName`  | 表名映射      | `@TableName("tb_user")`                     |
| `@TableId`    | 主键映射      | `@TableId(type = IdType.AUTO)`              |
| `@TableField` | 字段映射      | `@TableField("user_name")`                  |
| `@TableLogic` | 逻辑删除      | `@TableLogic private Integer isDelete;`     |

### 自定义注解

| 注解           | 位置                                                                                                                                    | 作用     |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------|--------|
| `@LogRecord` | [util/Aspect/LogRecord.java](file:///D:/CodeProject/PixVisionServer/src/main/java/top/playereg/pix_vision/util/Aspect/LogRecord.java) | 日志记录注解 |

**使用示例**：

```java

@PostMapping("/send-email-code")
@LogRecord(module = "\"验证码\"模块", event = "发送\"验证码\"")
public ResponsePojo<Boolean> sendEmailCode(...) {
  // 方法执行时会自动记录日志
}
```

## 常见问题与最佳实践

### ❌ 避免硬编码

```java
// ❌ 错误
long expireTime = 604800000L;

// ✅ 正确
private static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
```

### ❌ 避免空指针

```java
// ❌ 错误
if(user.getUsername().

equals("admin")){...}

// ✅ 正确
  if(user !=null&&"admin".

equals(user.getUsername())){...}
```

### ❌ 避免魔法数字

```java
// ❌ 错误
if(user.getStatus() !=10){...}

// ✅ 正确
private static final int USER_STATUS_NORMAL = 10;
if(user.

getStatus() !=USER_STATUS_NORMAL){...}
```

### ✅ 使用 Optional 处理可能为空的结果

```java
Optional<User> user = Optional.ofNullable(userService.selectById(id));
user.

ifPresent(u ->log.

info("找到用户：{}",u.getUsername()));
```

### ✅ 批量操作使用事务

```java

@Transactional
public void batchUpdateUsers(List<User> users) {
  users.forEach(this::updateUser);
}
```

### ✅ 合理使用缓存

- 频繁读取且变化少的数据使用 Redis 缓存
- 设置合理的过期时间
- 注意缓存一致性

### ✅ 日志分级记录

```java
log.debug("调试信息：参数值={}",param);
log.

info("业务流程：用户登录成功");
log.

warn("警告信息：Token 即将过期");
log.

error("错误信息：数据库连接失败",exception);
```

## 参考资源

### 官方文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Hutool 工具库文档](https://hutool.cn/)
- [SpringDoc OpenAPI 文档](https://springdoc.org/)
- [Knife4j 文档](https://doc.xiaominfo.com/)
- [JWT 官方网站](https://jwt.io/)
- [Bouncy Castle 文档](https://www.bouncycastle.org/)
- [Redis 官方文档](https://redis.io/documentation)
- [MySQL 官方文档](https://dev.mysql.com/doc/)

### 项目相关文档

- [README.md](file:///D:/CodeProject/PixVisionServer/README.md) - 项目说明文档
- [RSA工具使用指南.md](file:///D:/CodeProject/PixVisionServer/doc/RSA工具使用指南.md) - RSA 加密工具详细文档
- [db_pix_vision-V1.1.sql](file:///D:/CodeProject/PixVisionServer/sql/db_pix_vision-V1.1.sql) - 数据库表结构

---

<div align="center">

**📝 本文档会根据项目迭代持续更新**

Made with ❤️ by PlayerEG & Contributors

</div>
