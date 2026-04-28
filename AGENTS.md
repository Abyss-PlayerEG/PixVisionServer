# PixVisionServer - AI Agent 开发指南

## 核心原则

本文档提供项目关键信息，帮助 AI Agent 快速上手并避免常见错误。每一行都是经过验证的重要指引。

---

## 身份与认证

### 需要认证的接口

```java
// ❌ 错误：不需要 @PublicAccess（需要认证的接口）
@PostMapping("/change/nickname")
public ResponsePojo<Boolean> updateNickname(...) { }

// ✅ 正确：公开接口加 @PublicAccess
@PublicAccess("用户注册接口，无需认证")
@PostMapping("/register")
public ResponsePojo<User> register(...) { }
```

### 修改密码后必须调用

```java
// 修改密码后必须移除所有 Token，使所有设备下线
tokenWhitelistService.removeAllUserTokens(userId, username);
```

### 密码处理

```java
// ✅ 正确：SHA-256 加密
String hashedPassword = StrSwitchUtils.PasswdToHash256(password);

// ❌ 绝对禁止：记录或存储明文密码
log.info("密码：{}", password);
```

---

## 认证与权限

### 获取用户 ID

```java
// 方式1：从 request 属性（拦截器自动设置）
Integer userId = (Integer) request.getAttribute("userId");

// 方式2：手动解析
String token = JWTUtils.extractTokenWithLog(request, "接口名称");
Integer userId = JWTUtils.getUserIdFromToken(token);
```

### 角色代码

| 代码 | 角色 |
|------|------|
| 11 | 普通用户 |
| 22 | 创作者 |
| 55 | 审核员 |
| 66 | 工单管理员 |
| 77 | 系统管理员 |

### 用户状态

| 代码 | 状态 |
|------|------|
| 10 | 正常 |
| 20 | 冻结 |
| 30 | 封禁 |

**注意**：
- 冻结状态（20）：用户无法登录，但数据保留
- 封禁状态（30）：用户无法登录，严重违规使用
- 只有系统管理员（角色 77）可以修改用户状态

---

## Spring Boot 3 依赖注入

```java
// ✅ 推荐：构造器注入（配合 Lombok）
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

// ❌ 错误：Spring Boot 3 不支持 @Resource
@Resource
private UserService userService;
```

---

## 常见陷阱

### 1. 分页查询顺序（必须先创建 Page）

```java
Page<User> page = new Page<>(current, size);
IPage<User> result = userService.selectPageUserInfo(page, query);
```

### 2. 验证码格式

- 存储 Key：`verification:code:{email}`
- 有效期：5 分钟
- 一次性使用

### 3. 邮件接口参数

- `/send-register-code`：username 必填
- `/send-login-code`：usernameOrEmail 可为用户名或邮箱

### 4. Token 白名单机制

```java
// ✅ 检查 Token 是否在白名单
if (!tokenWhitelistService.isInWhitelist(token)) {
    return ResponsePojo.error(null, "Token 已失效");
}
```

---

## 常用工具类速查

```java
// JWT
String token = JWTUtils.createToken(userId, username);
Integer userId = JWTUtils.getUserIdFromToken(token);

// 密码哈希
String hash = StrSwitchUtils.PasswdToHash256(password);

// UUID 转换
byte[] bytes = StrSwitchUtils.uuid2Bytes(uuidString);
String uuid = StrSwitchUtils.bytes2Uuid(bytes);
```

---

## 配置

### 配置文件位置

```
~/.pix_vision/application.yml  ← 用户自定义配置（修改这里）
src/main/resources/yml-config/*.yml  ← 核心模板（不修改）
src/main/resources/application.yml  ← 基础配置（不修改）
```

### 日志位置

```
~/.pix_vision/log/pix_vision.log
```

---

## 项目结构

```
top.playereg.pix_vision
├── controller/      # REST API（C - Controller）
├── service/         # 业务逻辑（M - Model/Service）
│   └── Impl/
├── mapper/          # 数据访问层接口（M - Model/Mapper）
├── pojo/            # 实体类（M - Model/Entity）
├── handler/         # 拦截器
├── config/          # 配置类
└── util/           # 工具类
```

**资源文件**：
```
src/main/resources/mapper/*.xml  # MyBatis 自定义 SQL（V - View/XML）
```

---

## MVC 架构规范

### 严格遵循 MVC 分层

**Controller 层**：
- 仅负责接收请求、参数验证、调用 Service、返回响应
- 不包含业务逻辑
- 不直接操作数据库

**Service 层**：
- 实现核心业务逻辑
- 事务控制
- 调用 Mapper 进行数据操作
- 不直接编写 SQL

**Mapper 层**：
- 定义数据访问接口
- 所有 SQL 必须在 XML 文件中编写
- **禁止使用 MyBatis-Plus 的自动生成的 SQL 方法**

**XML 文件**：
- 所有 SQL 语句必须写在 `src/main/resources/mapper/*.xml` 中
- 使用自定义 SQL，不使用 MyBatis-Plus 的 Wrapper

### SQL 编写规范

```java
// ❌ 错误：使用 MyBatis-Plus 的 LambdaQueryWrapper
userMapper.selectCount(new LambdaQueryWrapper<User>()
    .eq(User::getUsername, username));

// ✅ 正确：使用自定义 XML SQL
userMapper.countByUsername(username);
```

```xml
<!-- UserMapper.xml -->
<select id="countByUsername" resultType="int">
    SELECT COUNT(1)
    FROM tb_user
    WHERE username = #{username}
      AND is_delete = 0
</select>
```

### Mapper 接口规范

```java
@Mapper
@Repository
public interface UserMapper extends BaseMapper<User> {
    // ✅ 推荐：自定义方法 + XML 实现
    int countByUsername(@Param("username") String username);
    
    // ❌ 避免：直接使用 BaseMapper 的方法（除非必要）
    // selectCount, selectList 等应通过自定义 XML 实现
}
```

---

## 运行命令

```bash
# 启动服务
mvn spring-boot:run

# 构建
mvn clean package -DskipTests

# 运行 JAR
java -jar target/PixVisionServer-0.0.1-SNAPSHOT.jar
```

---

## API 文档

访问地址：http://localhost:9090/doc.html（需 springdoc: enabled: on）

---

## Git 提交规范

```
<type>(<scope>): <subject>

feat(user): 添加用户注册功能
fix(auth): 修复 Token 验证问题
docs: 更新 README
refactor(service): 优化查询逻辑
```

---