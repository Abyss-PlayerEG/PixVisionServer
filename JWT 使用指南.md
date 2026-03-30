# PixVisionServer JWT 鉴权使用指南

## 📋 目录

- [概述](#概述)
- [已实现的功能](#已实现的功能)
- [快速开始](#快速开始)
- [API 接口说明](#api-接口说明)
- [代码示例](#代码示例)
- [最佳实践](#最佳实践)

---

## 🎯 概述

本项目已集成 **Hutool JWT** 模块，实现了完整的后端鉴权系统。JWT（JSON Web Token）是一种开放标准（RFC 7519），用于在各方之间安全地传输信息作为 JSON 对象。

### 技术栈
- **框架**: Spring Boot 3.3.0
- **JWT 库**: Hutool 5.8.38
- **签名算法**: HS256
- **Token 有效期**: 7 天

---

## ✨ 已实现的功能

### 1. JWT 工具类 (`JWTUtils.java`)
提供完整的 JWT 操作功能：
- ✅ 生成 Token
- ✅ 验证 Token
- ✅ 解析 Token Payload
- ✅ 获取用户信息
- ✅ 检查 Token 过期

### 2. JWT 拦截器 (`JwtAuthenticationInterceptor.java`)
自动拦截需要认证的请求：
- ✅ 从 Header 或参数中提取 Token
- ✅ 验证 Token 有效性
- ✅ 检查 Token 是否过期
- ✅ 将用户信息注入到 Request 中

### 3. Web 配置 (`WebConfig.java`)
配置拦截规则：
- ✅ 自动拦截所有请求
- ✅ 排除公开接口（注册、登录等）
- ✅ 排除静态资源和文档路径

### 4. 登录接口 (`UserController.java`)
用户登录并获取 Token：
- ✅ 支持用户名或邮箱登录
- ✅ 密码验证
- ✅ 生成并返回 JWT Token

### 5. 测试接口 (`TestAuthController.java`)
演示 JWT 鉴权的使用：
- ✅ 需要认证的接口
- ✅ 公开接口

---

## 🚀 快速开始

### 步骤 1: 用户注册
```bash
POST http://localhost:8080/api/user/register
Content-Type: application/x-www-form-urlencoded

username=dev_user&password=your_password&nickname=测试用户&email=test@example.com&vCode=123456
```

### 步骤 2: 用户登录获取 Token
```bash
POST http://localhost:8080/api/user/login
Content-Type: application/x-www-form-urlencoded

username=dev_user&password=your_password
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user_id": 1,
    "username": "dev_user",
    "nickname": "测试用户",
    "email": "test@example.com",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 步骤 3: 使用 Token 访问受保护的接口
```bash
GET http://localhost:8080/api/test/require-auth
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📖 API 接口说明

### 公开接口（无需认证）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 用户注册 |
| `/api/user/login` | POST | 用户登录 |
| `/api/test/no-auth` | GET | 公开测试接口 |

### 受保护接口（需要 Token）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/test/require-auth` | GET | 需要认证的测试接口 |

---

## 💻 代码示例

### 1. 在其他 Controller 中使用 Token

```java
@RestController
@RequestMapping("/api/works")
@Tag(name = "作品管理")
public class WorksController {
    
    @GetMapping("/my-works")
    @Operation(summary = "获取我的作品", description = "需要登录")
    public ResponsePojo<List<Works>> getMyWorks(HttpServletRequest request) {
        // 从 request 中获取当前用户 ID
        Integer userId = (Integer) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        
        // 根据 userId 查询用户作品
        List<Works> works = worksService.findByUserId(userId);
        
        return ResponsePojo.success(works, "获取成功");
    }
}
```

### 2. 在 Service 中获取当前用户

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private HttpServletRequest request;
    
    public User getCurrentUser() {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("用户未登录");
        }
        return userMapper.selectById(userId);
    }
}
```

### 3. 自定义 Token 有效期

修改 `JWTUtils.java` 中的常量：
```java
// Token 有效期（毫秒）- 可自定义
private static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7 天
// 或
private static final long TOKEN_EXPIRE_TIME = 30 * 60 * 1000L; // 30 分钟
```

### 4. 修改 JWT 密钥

为了安全起见，建议在生产环境中修改默认密钥：
```java
// 建议从配置文件读取
@Value("${jwt.secret-key}")
private String secretKey;

private static final String SECRET_KEY = "your-super-secret-key";
```

---

## 🔧 配置说明

### 排除特定路径的认证

如果需要添加更多不需要认证的路径，在 `WebConfig.java` 中配置：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtAuthenticationInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                    "/api/user/register",      // 用户注册
                    "/api/user/login",         // 用户登录
                    "/api/public/**",          // 添加新的公开路径
                    "/swagger-ui/**",          // Swagger UI
                    "/v3/api-docs/**",         // OpenAPI 文档
                    "/static/**",              // 静态资源
                    "/favicon.ico"             // 网站图标
            );
}
```

---

## 🔐 安全建议

### 1. 生产环境配置
- ✅ 使用 HTTPS 传输
- ✅ JWT 密钥存储在配置文件或环境变量中
- ✅ 不要将密钥提交到版本控制
- ✅ 定期更换密钥

### 2. Token 安全
- ✅ 设置合理的有效期
- ✅ 客户端安全存储 Token（建议使用 HttpOnly Cookie）
- ✅ 登出时将 Token 加入黑名单（可选）
- ✅ 敏感操作要求重新验证

### 3. 密码安全
- ✅ 密码加密存储（已实现 SHA-256）
- ✅ 使用强密码策略
- ✅ 限制登录失败次数

---

## 🧪 测试

运行单元测试：
```bash
mvn test -Dtest=JWTUtilsTest
```

测试用例包括：
- ✅ 生成 Token
- ✅ 验证 Token
- ✅ 解析 Payload
- ✅ 获取用户信息
- ✅ 过期检测
- ✅ 无效 Token 处理

---

## 📝 常见问题

### Q1: Token 如何存储？
**A:** 客户端可以选择以下方式存储：
- LocalStorage（简单但需注意 XSS）
- SessionStorage（标签页关闭后清除）
- HttpOnly Cookie（推荐，防止 XSS）

### Q2: Token 过期了怎么办？
**A:** 
1. 前端捕获 401 响应
2. 跳转到登录页或刷新 Token
3. 或者实现双 Token 机制（Access + Refresh）

### Q3: 如何实现登出？
**A:** 
- 方案 1：客户端删除 Token
- 方案 2：服务端实现 Token 黑名单（使用 Redis）
- 方案 3：使用短期 Token + Refresh Token

### Q4: 如何在微服务中使用？
**A:** 
1. 统一 JWT 密钥
2. 网关层验证 Token
3. 微服务间传递用户上下文
4. 可使用 JWK 实现密钥轮换

---

## 🛠️ 扩展功能建议

### 1. 实现 Refresh Token 机制
```java
// 生成 Access Token（短期，如 30 分钟）
String accessToken = JWTUtils.createToken(userId, username);

// 生成 Refresh Token（长期，如 30 天）
String refreshToken = JWTUtils.createTokenWithCustomExpiry(userId, username, 30L);
```

### 2. 实现 Token 黑名单
使用 Redis 存储已登出的 Token：
```java
@Autowired
private RedisTemplate<String, String> redisTemplate;

// 登出时将 Token 加入黑名单
redisTemplate.opsForValue().set(
    "token:blacklist:" + token, 
    "logout", 
    expireTime, 
    TimeUnit.MILLISECONDS
);
```

### 3. 添加角色权限控制
```java
// 在 Token 中添加角色信息
Map<String, Object> payload = new HashMap<>();
payload.put("userId", userId);
payload.put("username", username);
payload.put("role", "admin");

// 创建注解 @RequireRole("admin")
// 在拦截器中检查角色权限
```

---

## 📞 技术支持

如有问题，请查看：
- Hutool JWT 官方文档：https://hutool.cn/docs/#/jwt/README
- Spring Boot 官方文档：https://spring.io/projects/spring-boot
- JWT RFC 7519：https://tools.ietf.org/html/rfc7519

---

**最后更新**: 2026-03-30  
**作者**: PlayerEG & AI Assistant
