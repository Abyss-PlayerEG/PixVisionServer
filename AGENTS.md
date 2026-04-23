# PixVisionServer - AI Agent 开发指南

> **核心原则**：本文档专为 AI Agent 设计，提供快速检索的关键信息和决策依据

---

## 🎯 快速决策树

### 新增接口时

```
需要认证？
├─ 是 → 不加 @PublicAccess
│        ├─ 需要特定角色？ → 加 @RequireRole(value = {角色代码})
│        └─ 普通用户？ → 只需 Token 验证（拦截器自动处理）
└─ 否 → 加 @PublicAccess("功能描述，无需认证")

接口复杂度？
├─ 简单 → 使用简化版 Swagger 模板
└─ 复杂 → 使用完整版 Swagger 模板（5个章节）
```

### 修改密码/重置密码时

```
必须执行：
1. SHA-256 加密密码
2. 更新数据库
3. 调用 tokenWhitelistService.removeAllUserTokens(userId, username)
```

### 删除数据时

```
单条 or 批量？
├─ 统一使用 List<Integer> 参数
├─ 单条传 [1]
└─ 批量传 [1,2,3]
```

---

## 📋 项目核心信息

### 技术栈（AI 需知）

| 类别 | 技术 | 关键版本 | AI 注意事项 |
|------|------|---------|------------|
| 语言 | Java | 17 | 使用 record、switch 表达式等新特性 |
| 框架 | Spring Boot | 3.3.0 | Jakarta EE，非 javax |
| ORM | MyBatis-Plus | 3.5.7 | 继承 BaseMapper<T>，自动 CRUD |
| 缓存 | Redis | 8.0 | Lettuce 客户端 |
| 认证 | JWT + Redis | - | 双重验证，有效期 7 天 |
| 文档 | SpringDoc + Knife4j | 2.5.0 + 4.4.0 | 地址：http://localhost:9090/doc.html |
| 加密 | Bouncy Castle | 1.77 | RSA + AES 混合加密 |

### 包结构约定

```
top.playereg.pix_vision
├── controller/      # REST API，必须加 @Tag 和 @Operation
├── service/         # 业务逻辑接口
│   └── Impl/       # 实现类，加 @Service
├── mapper/          # 数据访问，继承 BaseMapper<T>
├── pojo/            # 实体类，加 @Data
├── handler/         # 拦截器
├── config/          # 配置类，加 @Configuration
└── util/            # 工具类，静态方法
```

---

## 🔑 认证与权限（高频使用）

### Token 验证流程

```java
// Controller 中获取用户 ID（两种方式）

// 方式1：从 request 属性（拦截器已设置）
Integer userId = (Integer) request.getAttribute("userId");

// 方式2：手动解析 Token
String token = JWTUtils.extractTokenWithLog(request, "接口名称");
Integer userId = JWTUtils.getUserIdFromToken(token);
```

### 公开接口标记

```java
// ✅ 正确：公开接口
@PublicAccess("用户注册接口，无需认证")
@PostMapping("/register")
public ResponsePojo<User> register(...) { }

// ❌ 错误：需要认证的接口不要加 @PublicAccess
@PostMapping("/change/nickname")
public ResponsePojo<Boolean> updateNickname(...) { }
```

### 角色权限控制

```java
// 角色代码对照表
// 11=普通用户, 22=创作者, 55=审核员, 66=工单管理员, 77=系统管理员

// 整个 Controller 需要权限
@RequireRole(value = {77})  // 仅系统管理员
@RestController
public class AdminController { }

// 单个方法需要权限
@RequireRole(value = {66, 77}, allowHigher = true)  // 工单管理员及以上
@GetMapping("/ticket-management")
public ResponsePojo<String> ticketManagement() { }
```

---

## 📝 Swagger 文档规范（强制执行）

### 必填注解清单

每个 Controller 方法**必须**包含：

1. `@Operation(summary = "...", description = """...""")`
2. `@Parameter(description = "...", required = true/false, example = "...")` （每个参数）
3. `@Tag(name = "...")` （Controller 类上）
4. `@PublicAccess("...")` （仅公开接口）

### Description 模板（复制即用）

#### 完整版（复杂接口）

```markdown
# 接口标题（无需登录认证/需要登录认证）

## 特性
- 特性1
- 特性2
- 特性3

## 参数说明：
- 参数1: 描述，类型，是否必填，约束条件
- 参数2: 描述，类型，是否必填，示例值

## 返回说明：
- **成功场景**：返回 **{"data": {数据类型}}** 和提示信息
- **失败场景1**：返回 **{"data": null/false}** 和错误提示
- **失败场景2**：返回 **{"data": null/false}** 和错误提示

## 业务逻辑：
1. 步骤1
2. 步骤2
3. 步骤3

## 注意事项：
- 注意项1
- 注意项2
```

#### 简化版（简单接口）

```markdown
# 接口标题（认证方式）

## 特性
- 特性1
- 特性2

## 参数说明：
- 参数1: 描述，类型，必填性

## 返回说明：
- **成功**：返回数据和提示
- **失败**：返回错误信息

## 业务逻辑：
1. 步骤1
2. 步骤2

## 注意事项：
- 注意点
```

### Parameter 注解模板

```java
// 必填参数
@Parameter(description = "用户名，6-16 位字母/数字/下划线", required = true, example = "dev_user")

// 可选参数
@Parameter(description = "昵称（可选，为空时自动生成）", required = false, example = "测试用户")

// 带范围约束
@Parameter(description = "每页大小，范围 1-100", required = true, example = "10")
```

### 完整示例（直接参考）

```java
@PostMapping("/login")
@PublicAccess("用户登录接口，无需认证")
@Operation(
    summary = "用户登录接口",
    description = """
        # 用户登录（无需登录认证）

        ## 特性
        - 支持用户名或邮箱登录
        - JWT Token 生成（有效期 7 天）
        - Token 白名单管理
        - SHA-256 密码验证

        ## 参数说明：
        - usernameOrEmail: 用户名或邮箱，字符串类型，必填
        - password: 登录密码，字符串类型，必填
        - vCode: 邮箱验证码，6 位大写字母或数字，必填

        ## 返回说明：
        - **登录成功**：返回 **{"data": UserLogin 对象}** 和 Token
        - **验证码错误**：返回 **{"data": null}** 和"验证码错误"提示
        - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示

        ## 业务逻辑：
        1. 校验用户名/邮箱格式、验证码格式
        2. 验证邮箱验证码
        3. 查询用户并验证密码（SHA-256）
        4. 检查用户状态（status=10 正常）
        5. 生成 JWT Token（7 天有效期）
        6. 将 Token 加入白名单

        ## 注意事项：
        - Token 有效期 **7 天**
        - 支持用户名**或**邮箱登录
        - Header: `Authorization: Bearer <token>` 或 URL: `?token=<token>`
        """
)
public ResponsePojo<UserLogin> login(
    @Parameter(description = "用户名或邮箱", required = true, example = "dev_user") 
    @RequestParam String usernameOrEmail,
    @Parameter(description = "登录密码", required = true, example = "123456") 
    @RequestParam String password,
    @Parameter(description = "邮箱验证码，6 位", required = true, example = "ABCDEF") 
    @RequestParam String vCode
) {
    // 实现代码...
}
```

---

## 🗄️ 数据库规范

### 表命名规则

- **前缀**：`tb_`（如 `tb_user`、`tb_works`）
- **逻辑删除字段**：`is_delete`（0=未删除，1=已删除）

### 用户角色代码（重要）

| 代码 | 角色 | 权限说明 |
|------|------|---------|
| 11 | 普通用户 | 基础功能 |
| 22 | 创作者 | 发布作品 |
| 55 | 审核员 | 内容审核 |
| 66 | 工单管理员 | 工单处理 |
| 77 | 系统管理员 | 最高权限 |

### 用户状态代码

| 代码 | 状态 | 说明 |
|------|------|------|
| 10 | 正常 | 可正常使用 |
| 20 | 冻结 | 临时冻结 |
| 30 | 封禁 | 永久封禁 |

### UUID 处理

```java
// 数据库存储：16 字节二进制
// 接口传输：36 字符标准 UUID 字符串

// 转换工具
byte[] uuidBytes = StrSwitchUtils.uuid2Bytes(uuidString);  // 字符串 → 二进制
String uuidString = StrSwitchUtils.bytes2Uuid(uuidBytes);  // 二进制 → 字符串
```

---

## 🔐 安全规范（必须遵守）

### 密码处理

```java
// ✅ 正确：SHA-256 加密
String hashedPassword = StrSwitchUtils.PasswdToHash256(password);

// ❌ 错误：明文存储或记录日志
log.info("密码：{}", password);  // 绝对禁止！
```

### Token 失效场景（必须移除所有 Token）

```java
// 以下操作后必须调用：
tokenWhitelistService.removeAllUserTokens(userId, username);

触发场景：
1. 用户登出
2. 修改密码
3. 忘记密码重置
4. 注销账户
```

### 验证码机制

```java
// 生成验证码
String code = verificationCodeServices.verificationCode();

// 存储到 Redis（key: verification:code:{email}）
verificationCodeServices.setRedisVCode(email, code);

// 验证（一次性使用，验证后自动删除）
boolean isValid = verificationCodeServices.verificationCodeVerify(email, code);
```

**关键点**：
- 有效期：5 分钟
- 一次性使用
- Key 格式：`verification:code:{email}`

---

## 🛠️ 常用工具类速查

### JWTUtils

```java
// 创建 Token
String token = JWTUtils.createToken(userId, username);

// 从 Token 获取用户信息
Integer userId = JWTUtils.getUserIdFromToken(token);
String username = JWTUtils.getUsernameFromToken(token);

// 从 request 提取 Token（带日志）
String token = JWTUtils.extractTokenWithLog(request, "接口名称");
```

### RegexUtils

```java
// 验证用户名（6-16位字母/数字/下划线）
boolean valid = RegexUtils.isUsername("dev_user");

// 验证邮箱
boolean valid = RegexUtils.isEmail("test@example.com");

// 验证验证码（6位大写字母或数字）
boolean valid = RegexUtils.isVCode("ABC123", 6);

// 验证 UUID
boolean valid = RegexUtils.isUUID("550e8400-e29b-41d4-a716-446655440000");
```

### StrSwitchUtils

```java
// 密码哈希
String hash = StrSwitchUtils.PasswdToHash256(password);

// UUID 转换
byte[] bytes = StrSwitchUtils.uuid2Bytes(uuidString);
String uuid = StrSwitchUtils.bytes2Uuid(bytes);

// 生成随机昵称
String nickname = StrSwitchUtils.generateRandomUserDefaultNickName("user");
```

### ImageUtils

```java
// 验证图片格式（魔数检查）
boolean valid = ImageUtils.isValidImage(imageBytes);

// 验证正方形
boolean square = ImageUtils.isSquareImage(imageBytes);

// 缩放图片（600x600 PNG）
byte[] resized = ImageUtils.resizeImage(imageBytes, 600, 600, true);
```

### RSACipher

```java
// 加密（自动选择 RSA 或 AES+RSA）
String encrypted = RSACipher.encryptToBase64(data);
byte[] encrypted = RSACipher.encryptToBytes(data);

// 解密
String decrypted = RSACipher.decryptToString(encrypted);
byte[] decrypted = RSACipher.decryptToBytes(encrypted);
```

---

## 📦 响应格式规范

### 成功响应

```java
// 返回数据
return ResponsePojo.success(data, "成功消息");

// 示例
return ResponsePojo.success(user, "注册成功");
return ResponsePojo.success(true, "修改成功");
return ResponsePojo.success(userDataList, "查询成功");
```

### 失败响应

```java
// 返回错误
return ResponsePojo.error(null, "错误消息");
return ResponsePojo.error(false, "错误消息");

// 示例
return ResponsePojo.error(null, "用户名格式错误");
return ResponsePojo.error(false, "验证码错误");
```

---

## 🧪 测试规范

### 测试类命名

```
被测试类：UserServiceImpl
测试类名：UserServiceImplTest
位置：src/test/java/top/playereg/pix_vision/service/Impl/
```

### 测试方法命名

```java
// 格式：test{方法名}{场景}
@Test
void testLoginWithValidCredentials() { }

@Test
void testRegisterWithDuplicateUsername() { }

@Test
void testUpdateNicknameWithInvalidToken() { }
```

---

## 🚀 开发工作流

### 新增接口步骤

```
1. 设计接口
   ├─ URL 路径：/api/{模块}/{功能}
   ├─ 请求方式：GET/POST/PUT/DELETE
   ├─ 是否需要认证？
   └─ 参数和返回值定义

2. 创建/更新 POJO
   └─ src/main/java/top/playereg/pix_vision/pojo/

3. Service 层
   ├─ 定义接口方法
   └─ 实现业务逻辑

4. Controller 层
   ├─ 添加 @Tag、@Operation、@Parameter
   ├─ 添加 @PublicAccess（如需要）
   ├─ 参数校验
   └─ 调用 Service

5. 测试
   ├─ 单元测试
   └─ 手动测试（Swagger UI）

6. 提交代码
   └─ git commit -m "feat(module): 功能描述"
```

### 修改现有功能

```
1. 影响分析
   ├─ 哪些接口受影响？
   ├─ 数据库是否需要变更？
   └─ 是否需要迁移数据？

2. 修改代码
   ├─ 保持向后兼容
   └─ 更新相关测试

3. 同步文档
   ├─ 更新 Swagger 文档
   ├─ 更新 JavaDoc
   └─ 更新 AGENTS.md（如需要）
```

---

## ⚠️ 常见陷阱（必读）

### 1. 分页查询顺序

```java
// ✅ 正确顺序
Page<User> page = new Page<>(current, size);           // 1. 创建 Page 对象
IPage<User> result = userService.selectPageUserInfo(page, ...);  // 2. 执行查询
for (User user : result.getRecords()) {                // 3. 处理结果
    // 处理逻辑
}

// ❌ 错误：先查询再创建 Page
```

### 2. 缓存 Key 一致性

```java
// 设置缓存
String key = "verification:code:" + email;
redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

// 删除缓存（必须使用相同的 key 生成逻辑）
String key = "verification:code:" + email;  // ✅ 相同逻辑
redisTemplate.delete(key);
```

### 3. 邮件接口 username 参数

```java
// 注册接口：username 必填
@PostMapping("/send-register-code")
public ResponsePojo<Boolean> sendRegisterCode(
    @RequestParam String email,
    @RequestParam String username  // ✅ 必填
) { }

// 登录/改密接口：usernameOrEmail 可为用户名或邮箱
@PostMapping("/send-login-code")
public ResponsePojo<Boolean> sendLoginCode(
    @RequestParam String usernameOrEmail  // ✅ 用户名或邮箱
) { }
```

### 4. Token 黑名单 vs 白名单

```java
// 本项目使用白名单机制（Redis 存储有效 Token）
// ✅ 正确：检查 Token 是否在白名单中
if (!tokenWhitelistService.isInWhitelist(token)) {
    return ResponsePojo.error(null, "Token 已失效");
}

// ❌ 错误：不要使用黑名单机制
```

### 5. Spring Boot 3 依赖注入

```java
// ✅ 推荐：构造器注入（配合 Lombok）
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

// ✅ 备选：@Autowired（测试类可用）
@Autowired
private UserService userService;

// ❌ 错误：@Resource（Spring Boot 3 不支持）
@Resource
private UserService userService;
```

---

## 📊 API 接口速查表

### 用户认证 `/api/user/auth`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| POST | `/register` | ❌ | username, password, email, vCode |
| POST | `/login` | ❌ | usernameOrEmail, password, vCode |
| POST | `/logout` | ✅ | token |
| POST | `/delete-account` | ✅ | vCode, token |

### 用户资料 `/api/user/profile`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| GET | `/page/{current}/{size}` | ❌ | current, size, username(可选), email(可选) |
| POST | `/change/nickname` | ✅ | nickname, token |

### 用户拓展数据 `/api/user/data`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| POST | `/add` | ✅ | dataName, dataContent, token |
| GET | `/list` | ❌ | userId |
| POST | `/delete` | ✅ | dataIds (List), token |

### 密码管理 `/api/user/password`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| POST | `/change` | ✅ | newPassword, confirmPassword, vCode |
| POST | `/forgot` | ❌ | usernameOrEmail, newPassword, confirmPassword, vCode |

### 邮件服务 `/api/mail`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| POST | `/send-register-code` | ❌ | email, username |
| POST | `/send-login-code` | ❌ | usernameOrEmail |
| POST | `/send-forget-password-code` | ❌ | usernameOrEmail |

### 图片资源 `/api/image`

| 方法 | 路径 | 认证 | 关键参数 |
|------|------|:----:|---------|
| GET | `/get/avatar` | ❌ | filePath |
| GET | `/get/works` | ❌ | filePath |
| GET | `/get/logo` | ❌ | filePath |
| POST | `/upload/avatar` | ✅ | file (MultipartFile) |

---

## 🔧 配置管理

### 配置文件优先级

```
最高：~/.pix_vision/application.yml        ← 用户自定义（✅ 修改这里）
中等：src/main/resources/yml-config/*.yml  ← 核心模板（🚫 不要改）
最低：src/main/resources/application.yml   ← 基础配置（🚫 不要改）
```

### 关键配置项

```yaml
# 数据库
spring.datasource.url: jdbc:mysql://localhost:3306/db_pix_vision

# Redis
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# 邮件
spring.mail.host: smtp.qq.com
spring.mail.port: 465
spring.mail.password: your_auth_code  # 授权码，非密码

# API 文档
springdoc.enabled: on  # 开启 Swagger
```

---

## 📖 Git 提交规范

### Commit 格式

```
<type>(<scope>): <subject>

<body>
```

### Type 类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(user): 添加邮箱修改接口` |
| `fix` | Bug 修复 | `fix(auth): 修复 Token 验证漏洞` |
| `docs` | 文档更新 | `docs: 更新 API 文档` |
| `refactor` | 重构 | `refactor(service): 优化用户查询逻辑` |
| `test` | 测试 | `test: 添加用户注册测试用例` |
| `chore` | 构建/工具 | `chore: 更新依赖版本` |

### 完整示例

```bash
git commit -m "feat(password): 添加密码修改接口

- 实现登录后修改密码功能
- 添加邮箱验证码验证
- 修改成功后移除所有 Token

Closes #123"
```

---

## 🎓 AI 使用建议

### 代码生成时

1. **优先参考现有代码**：查看相似功能的实现
2. **遵循命名规范**：使用项目约定的命名方式
3. **添加完整文档**：Swagger + JavaDoc 缺一不可
4. **考虑边界情况**：空值、异常、权限验证

### 代码审查时

1. **检查注解完整性**：@Operation、@Parameter、@PublicAccess
2. **验证安全性**：密码加密、Token 验证、输入校验
3. **确认事务处理**：批量操作是否加 @Transactional
4. **日志合理性**：敏感信息脱敏、日志级别正确

### 问题排查时

1. **查看日志**：`~/.pix_vision/log/pix_vision.log`
2. **检查 Redis**：Token 白名单、验证码
3. **验证数据库**：数据是否正确、逻辑删除状态
4. **测试接口**：使用 Swagger UI 手动测试

---

<div align="center">

**💡 提示**：遇到不确定的情况，优先参考项目中已有的相似实现

**📝 更新**：本文档随项目迭代持续更新

</div>
