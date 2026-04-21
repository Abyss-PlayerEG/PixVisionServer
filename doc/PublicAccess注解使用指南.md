# @PublicAccess 注解使用指南

## 📋 概述

`@PublicAccess` 注解用于标记**无需 JWT 认证**即可访问的公开接口，替代了之前在 `WebConfig.java` 中硬编码白名单的方式。

---

## 🎯 核心优势

### 之前的方式（❌ 不推荐）

```java
// WebConfig.java
registry.addInterceptor(jwtAuthenticationInterceptor)
    .addPathPatterns("/api/**")
    .excludePathPatterns(
        "/api/user/auth/login",      // 需要手动添加
        "/api/user/auth/register",   // 需要手动添加
        "/api/mail/send-*-code"      // 需要手动添加
        // 每新增一个公开接口都要来这里修改...
    );
```

**缺点：**
- ❌ 每次新增公开接口都要修改配置文件
- ❌ 白名单分散在配置文件中，不易维护
- ❌ 无法从代码层面直观看出哪些接口是公开的

### 现在的方式（✅ 推荐）

```java
@PostMapping("/login")
@PublicAccess("用户登录接口，无需认证")
public ResponsePojo<UserLogin> login(...) {
    // ...
}
```

**优点：**
- ✅ 直接在接口上标注，一目了然
- ✅ 新增公开接口无需修改配置文件
- ✅ 代码即文档，易于维护
- ✅ 支持方法级和类级别标注

---

## 💡 使用方式

### 方式一：方法级别（推荐）

为单个接口添加 `@PublicAccess` 注解：

```java
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {
    
    @PostMapping("/login")
    @PublicAccess("用户登录接口，无需认证")
    public ResponsePojo<UserLogin> login(...) {
        // 无需 Token 即可访问
    }
    
    @PostMapping("/register")
    @PublicAccess("用户注册接口，无需认证")
    public ResponsePojo<User> register(...) {
        // 无需 Token 即可访问
    }
}
```

### 方式二：类级别

为整个控制器添加 `@PublicAccess` 注解（所有接口都公开）：

```java
@RestController
@RequestMapping("/api/public")
@PublicAccess("公共服务接口，全部公开")
public class PublicController {
    
    @GetMapping("/info")
    public ResponsePojo<String> getInfo() {
        // 无需 Token 即可访问
    }
    
    @GetMapping("/status")
    public ResponsePojo<String> getStatus() {
        // 无需 Token 即可访问
    }
}
```

### 方式三：混合使用

类级别标注大部分接口公开，个别接口需要认证：

```java
@RestController
@RequestMapping("/api/docs")
@PublicAccess("文档接口，默认公开")
public class DocsController {
    
    @GetMapping("/public-docs")
    public ResponsePojo<List<Doc>> getPublicDocs() {
        // 继承类注解：公开访问
    }
    
    @GetMapping("/admin-docs")
    @RequireRole(value = {77})  // 覆盖为：仅管理员可访问
    public ResponsePojo<List<Doc>> getAdminDocs() {
        // 需要管理员权限
    }
}
```

---

## 📝 已应用的公开接口

以下接口已添加 `@PublicAccess` 注解：

### 用户认证模块

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/user/auth/register` | POST | 用户注册 |
| `/api/user/auth/login` | POST | 用户登录 |

### 密码管理模块

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/user/password/forgot` | POST | 忘记密码重置 |

### 邮件服务模块

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/mail/send-register-code` | POST | 发送注册验证码 |
| `/api/mail/send-login-code` | POST | 发送登录验证码 |
| `/api/mail/send-forget-password-code` | POST | 发送重置密码验证码 |

---

## 🔧 如何添加新的公开接口

### 步骤 1：导入注解

```java
import top.playereg.pix_vision.util.Annotation.PublicAccess;
```

### 步骤 2：添加注解

```java
@PostMapping("/your-public-endpoint")
@PublicAccess("接口描述，说明为什么需要公开")
public ResponsePojo<?> yourMethod(...) {
    // 业务逻辑
}
```

### 步骤 3：完成！

无需修改任何其他配置文件，接口立即可公开访问。

---

## ⚠️ 注意事项

### 1. 注解优先级

**方法级注解 > 类级注解**

```java
@RestController
@PublicAccess  // 类级别：默认公开
public class MyController {
    
    @GetMapping("/public")
    public ResponsePojo<?> publicMethod() {
        // 公开访问（继承类注解）
    }
    
    @GetMapping("/private")
    @RequireRole(value = {77})  // 方法级别：覆盖为需要权限
    public ResponsePojo<?> privateMethod() {
        // 需要管理员权限（方法注解优先）
    }
}
```

### 2. 与 @RequireRole 的关系

- `@PublicAccess`：跳过 JWT 认证，任何人都可以访问
- `@RequireRole`：需要 JWT 认证 + 角色权限验证
- **两者不能同时使用**（逻辑冲突）

```java
// ❌ 错误：逻辑冲突
@PublicAccess
@RequireRole(value = {77})
public ResponsePojo<?> method() { ... }

// ✅ 正确：只使用其中一个
@PublicAccess
public ResponsePojo<?> publicMethod() { ... }

@RequireRole(value = {77})
public ResponsePojo<?> adminMethod() { ... }
```

### 3. 安全性考虑

使用 `@PublicAccess` 时要谨慎，确保：
- ✅ 接口不包含敏感操作
- ✅ 有其他安全机制（如验证码、限流等）
- ✅ 不会造成安全风险（如暴力破解、数据泄露）

```java
// ✅ 安全：有验证码保护
@PostMapping("/send-code")
@PublicAccess
public ResponsePojo<Boolean> sendCode(@RequestParam String email) {
    // 有验证码频率限制
}

// ❌ 危险：无保护的敏感操作
@PostMapping("/delete-all-users")
@PublicAccess  // 绝对不要这样做！
public ResponsePojo<Boolean> deleteAllUsers() {
    // 这是严重的安全漏洞！
}
```

### 4. 日志记录

JWT 拦截器会记录公开接口的访问日志：

```
DEBUG: 公开接口，跳过认证 - URI: /api/user/auth/login
```

便于审计和排查问题。

---

## 🆚 对比总结

| 特性 | WebConfig 白名单 | @PublicAccess 注解 |
|------|-----------------|-------------------|
| **易用性** | ❌ 需修改配置文件 | ✅ 直接标注在方法上 |
| **可维护性** | ❌ 分散在配置中 | ✅ 代码即文档 |
| **灵活性** | ❌ 只能路径匹配 | ✅ 支持方法和类级别 |
| **可读性** | ❌ 不够直观 | ✅ 一目了然 |
| **扩展性** | ❌ 每次都要改配置 | ✅ 无需额外配置 |

---

## 📚 相关文件

| 文件 | 说明 |
|------|------|
| `util/Annotation/PublicAccess.java` | 注解定义 |
| `handler/JwtAuthenticationInterceptor.java` | JWT 认证拦截器（支持注解检测） |
| `config/WebConfig.java` | 拦截器配置（已简化） |

---

<div align="center">

**📝 文档版本**: v1.0  
**👤 作者**: PlayerEG  
**📅 创建日期**: 2026-04-21

</div>
