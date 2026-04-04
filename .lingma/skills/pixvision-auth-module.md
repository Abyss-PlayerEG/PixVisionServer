---
name: pixvision-auth-module
description: JWT 认证模块开发指南，涵盖 Token 生成、验证、拦截器和白名单机制
---

# JWT 认证模块开发技能

## 模块概述

JWT 认证模块提供基于 Token 的身份验证机制，采用 JWT + Redis 白名单双重验证策略。

## 核心文件

| 文件 | 路径 |
|------|------|
| 拦截器 | `handler/JwtAuthenticationInterceptor.java` |
| JWT 工具类 | `util/JWTUtils.java` |
| 白名单服务 | `service/TokenWhitelistService.java` → `service/Impl/TokenWhitelistServiceImpl.java` |
| Web 配置 | `config/WebConfig.java` |
| 安全配置 | `config/SecureConfig.java` |

## 认证流程

### Token 生成
```java
// 简化方式
String token = JWTUtils.createToken(userId, username);

// 自定义载荷方式
Map<String, Object> payload = new HashMap<>();
payload.put("userId", userId);
payload.put("username", username);
payload.put("customKey", "customValue");
String token = JWTUtils.createToken(payload);
```

### Token 验证流程
1. 从 Header 获取 `Authorization` 或 URL 参数 `token`
2. 去除 `Bearer ` 前缀（如存在）
3. 检查 Token 是否在 Redis 白名单中
4. 验证 JWT 签名
5. 检查 Token 是否过期
6. 从 Token 中提取用户信息，存入 request 属性

### 拦截器配置

**拦截路径**: `/api/**` 及特定测试路径

**排除路径**:
- `/api/user/register` - 注册
- `/api/user/login` - 登录
- `/api/mail/send-email-code` - 发送验证码
- `/api/mail/verify-email-code-test` - 验证码测试
- `/api/test/**` - 测试接口
- `/7e212056/no-auth` - 无认证测试

## JWT 配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 签名算法 | HS256 | HMAC-SHA256 |
| Token 有效期 | 7 天 | 604800000 毫秒 |
| 签发者 | PixVisionServer | - |
| 密钥来源 | `SecureConfig.getJwtSecret()` | 配置文件 |

## Token 白名单

### Redis Key 格式
```
token:whitelist:{token} -> {userId}:{username}
```

### 操作
```java
// 添加到白名单（登录时）
tokenWhitelistService.addToWhitelist(token, userId, username, expireTime);

// 检查是否在白名单
boolean exists = tokenWhitelistService.isInWhitelist(token);

// 从白名单移除（登出/修改密码时）
tokenWhitelistService.removeFromWhitelist(token);
```

## 获取当前用户

在 Controller 中通过 request 属性获取：
```java
Integer userId = (Integer) request.getAttribute("userId");
String username = (String) request.getAttribute("username");
```

或直接从 Token 解析：
```java
String token = request.getHeader("Authorization");
Integer userId = JWTUtils.getUserIdFromToken(token);
```

## 错误响应

| 场景 | HTTP 状态码 | 错误信息 |
|------|------------|----------|
| Token 不存在 | 401 | 未授权访问：Token 不存在 |
| Token 不在白名单 | 401 | 未授权访问：Token 未在白名单中，请重新登录 |
| Token 签名无效 | 401 | 未授权访问：Token 无效或已过期 |
| Token 已过期 | 401 | 未授权访问：Token 已过期 |

## 安全机制

- **双重验证**: JWT 签名验证 + Redis 白名单验证
- **主动失效**: 登出或修改密码时从白名单移除 Token
- **过期控制**: JWT 内置过期时间 + Redis 过期时间双重保障
- **密钥管理**: 密钥从配置文件读取，支持用户自定义配置
