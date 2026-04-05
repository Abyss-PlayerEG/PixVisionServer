# PixVisionServer - AI Agent 开发规范

## 项目概述

- **项目名称**：PixVisionServer
- **语言**：Java 17
- **构建工具**：Maven 3.x
- **框架**：Spring Boot 3.3.0
- **测试框架**：JUnit + Spring Boot Test
- **作者**：PlayerEG
- **包名**：`top.playereg.pix_vision`

---

# 技术栈

## 核心依赖

- **Web 框架**：Spring Boot Web (Jakarta EE)
- **ORM 框架**：MyBatis-Plus 3.5.7
- **数据库**：MySQL 8.0（表命名规范：`tb_*`）
- **缓存**：Redis 8.0（使用 Lettuce 客户端）
- **JWT 认证**：Hutool JWT
- **文档工具**：SpringDoc OpenAPI 2.5.0 + Knife4j 4.4.0
- **工具库**：Hutool 5.8.38、Lombok、Oshi 6.6.5
- **邮件服务**：Spring Boot Mail + Jakarta Mail
- **模板引擎**：Thymeleaf
- **日志框架**：SLF4J + Log4j 1.2.17

---

# 项目结构

```
src/main/java/top/playereg/pix_vision/
├── config/              # 配置类（@Configuration）
├── controller/          # 控制器层（@RestController）
├── service/             # 服务接口
│   └── Impl/           # 服务实现类（@Service）
├── mapper/              # MyBatis Mapper 接口
├── pojo/                # 实体类和数据传输对象
│   └── userPojo/       # 用户相关 POJO
├── handler/             # 拦截器和处理器
├── util/                # 工具类
│   └── Aspect/         # AOP 切面
├── enums/               # 枚举类
└── egg/                 # 彩蛋功能

src/main/resources/
├── yml-config/          # YAML 配置文件（优先级 1）
├── application.yml      # 主配置文件（优先级 0）
├── mapper/              # MyBatis XML 映射文件
├── template/            # 模板文件（邮件、配置模板等）
├── static/              # 静态资源
└── logo/                # Logo 图片
```

## 配置文件说明

- **优先级 0**：`src/main/resources/application.yml`（项目主配置）
- **优先级 1**：`src/main/resources/yml-config/*.yml`（核心依赖配置）
- **优先级 2**：`${user.home}/.pix_vision/application.yml`（用户自定义配置，最高优先级）
- **日志配置**：`src/main/resources/log4j.properties`
- **配置类**：`src/main/java/top/playereg/pix_vision/config/*.java`

## 模板文件初始化

`src/main/resources/template/` 目录存放模板文件。项目初始化时，使用 `CreateFile.java` 的 `create()` 方法将模板复制到用户文件夹。

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
if (user == null) {
    return ResponsePojo.error(null, "用户不存在");
}

// ❌ 避免：直接抛出异常（除非必要）
throw new RuntimeException("用户不存在");
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
log.info("用户登录成功：{}", username);
log.error("无法从 Token 中获取用户 ID");

// ❌ 避免：字符串拼接
log.info("用户登录成功：" + username);
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
- **布尔值场景**：使用 `Boolean` 类型（`true`/`false`）
  ```java
  public ResponsePojo<Boolean> logout(HttpServletRequest request) {
      return ResponsePojo.success(true, "登出成功");
  }
  ```
- **对象场景**：返回具体对象或 `null`
  ```java
  public ResponsePojo<User> registerUser(...) {
      return ResponsePojo.success(user, "注册成功");
  }
  ```
- **列表/分页**：返回 `IPage<T>` 或 `List<T>`

### 状态码规范
- **200**：成功
- **500**：失败

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
- 使用 **SHA-256** 哈希加密存储
- 不在日志中记录明文密码
- 密码比对前先加密再比较

### Token 管理
- 使用 JWT + 白名单机制
- Token 有效期：**7 天**
- 登出或修改密码时，将 Token 从白名单移除
- 支持两种传递方式：
  - Header：`Authorization: Bearer <token>`
  - URL 参数：`?token=<token>`

### 输入验证
- 所有外部输入必须验证
- 使用正则表达式校验格式
- 防止 SQL 注入、XSS 等攻击

### 敏感操作二次验证
- 修改密码、删除账户等操作需要邮箱验证码
- 验证码有效期由 Redis 配置决定（默认 5 分钟）

## 4. 数据库规范

### 表命名
- 统一前缀：`tb_`
- 小写字母+下划线：`tb_user`、`tb_user_info`

### 字段命名
- 小写字母+下划线：`user_id`、`create_time`
- 主键：`id` 或 `{table}_id`
- 外键：`{ref_table}_id`

### 逻辑删除
- 使用 `is_delete` 字段标记删除状态
- `false`（0）：未删除
- `true`（1）：已删除
- MyBatis-Plus 自动过滤已删除记录

### UUID 处理
- 数据库存储：16 字节二进制（`BINARY(16)`）
- 接口传输：标准 UUID 字符串（36 字符）
- 转换工具：`StrSwitchUtils.uuid2Bytes()` 和 `StrSwitchUtils.bytes2Uuid()`

## 5. Redis 使用规范

### 键命名
- 格式：`{业务}:{标识}:{具体key}`
- 示例：`verification:code:user@example.com`

### 过期时间
- 验证码：5 分钟（可配置）
- Token 白名单：与 JWT 有效期一致（7 天）

### 序列化
- 使用 String 序列化器
- 复杂对象使用 JSON 序列化

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
if (user.getUsername().equals("admin")) { ... }

// ✅ 正确
if (user != null && "admin".equals(user.getUsername())) { ... }
```

### ❌ 避免魔法数字
```java
// ❌ 错误
if (user.getStatus() != 10) { ... }

// ✅ 正确
private static final int USER_STATUS_NORMAL = 10;
if (user.getStatus() != USER_STATUS_NORMAL) { ... }
```

## 2. 最佳实践

### ✅ 使用 Optional 处理可能为空的结果
```java
Optional<User> user = Optional.ofNullable(userService.selectById(id));
user.ifPresent(u -> log.info("找到用户：{}", u.getUsername()));
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
log.debug("调试信息：参数值={}", param);
log.info("业务流程：用户登录成功");
log.warn("警告信息：Token 即将过期");
log.error("错误信息：数据库连接失败", exception);
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

# 环境配置

## 开发环境要求

- **JDK**：17+
- **Maven**：3.6+
- **MySQL**：8.0+
- **Redis**：8.0+
- **IDE**：IntelliJ IDEA（推荐）

## 启动步骤

1. **配置数据库**：修改 `application.yml` 中的数据库连接信息
2. **配置 Redis**：修改 Redis 连接信息
3. **配置邮件**：配置 SMTP 服务器信息
4. **初始化数据库**：执行 `sql/db_pix_vision-V1.1.sql`
5. **启动应用**：运行 `PixVisionApplication.main()`

## 访问地址

- **应用首页**：http://localhost:8080
- **Swagger 文档**：http://localhost:8080/swagger-ui.html
- **Knife4j 文档**：http://localhost:8080/doc.html
- **健康检查**：http://localhost:8080/actuator/health

---

# 附录

## 常用工具类

- **JWTUtils**：JWT Token 生成和验证
- **RegexUtils**：正则表达式验证（用户名、邮箱、验证码等）
- **StrSwitchUtils**：字符串转换工具（UUID、密码哈希等）
- **ImageUtils**：图片处理工具
- **IpUtil**：IP 地址获取工具
- **ConversionUtils**：数据类型转换工具

## 常用注解

- `@RestController`：RESTful 控制器
- `@RequestMapping`：请求映射
- `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`：HTTP 方法映射
- `@RequestParam`：请求参数
- `@PathVariable`：路径变量
- `@RequestBody`：请求体
- `@Operation`：Swagger 接口描述
- `@Parameter`：Swagger 参数描述
- `@Tag`：Swagger 标签分组
- `@Service`：服务层组件
- `@Autowired`：自动注入
- `@RequiredArgsConstructor`：Lombok 构造器注入
- `@Data`：Lombok 自动生成 getter/setter
- `@Transactional`：事务管理

## 参考资源

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Hutool 工具库文档](https://hutool.cn/)
- [SpringDoc OpenAPI 文档](https://springdoc.org/)
- [JWT 官方网站](https://jwt.io/)
