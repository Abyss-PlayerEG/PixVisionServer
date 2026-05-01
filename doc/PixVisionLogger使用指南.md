# PixVisionLogger 使用指南

## 📖 概述

`PixVisionLogger` 是一个基于 SLF4J 的自定义日志封装接口，提供了更简洁、统一的日志记录方式。通过默认方法实现，避免了在每个类中重复编写日志初始化代码。

---

## 🚀 快速开始

### 传统方式（旧）

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    public void doSomething() {
        log.info("执行操作");
        log.debug("调试信息: {}", data);
        log.error("发生错误", exception);
    }
}
```

### 新方式（推荐）

```java
import top.playereg.pix_vision.util.PixVisionLogger;

public class UserService {
    private static final PixVisionLogger log = PixVisionLogger.create(UserService.class);
    
    public void doSomething() {
        log.info("执行操作");
        log.debug("调试信息: {}", data);
        log.error("发生错误", exception);
    }
}
```

---

## 💡 核心特性

### 1. 简洁的 API

所有日志级别都提供了三种重载方法：

```java
// 简单消息
log.info("用户登录成功");

// 带参数的消息（支持 SLF4J 占位符）
log.info("用户 {} 登录成功，IP: {}", username, ipAddress);

// 带异常的消息
log.error("处理请求失败", exception);
```

### 2. 支持的日志级别

| 级别 | 方法 | 用途 |
|------|------|------|
| TRACE | `trace()` | 最详细的跟踪信息 |
| DEBUG | `debug()` | 调试信息 |
| INFO | `info()` | 一般信息 |
| WARN | `warn()` | 警告信息 |
| ERROR | `error()` | 错误信息 |

### 3. 自动 Logger 管理

无需手动创建 `LoggerFactory.getLogger()`，通过工厂方法自动管理：

```java
private static final PixVisionLogger log = PixVisionLogger.create(YourClass.class);
```

---

## 📝 使用示例

### 示例 1：工具类中使用

```java
package top.playereg.pix_vision.util;

import top.playereg.pix_vision.util.PixVisionLogger;

public class RegexUtils {
    private static final PixVisionLogger log = PixVisionLogger.create(RegexUtils.class);

    public static boolean isEmail(String email) {
        if (email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            log.debug("邮箱格式匹配成功: {}", email);
            return true;
        } else {
            log.debug("邮箱格式匹配失败: {}", email);
            return false;
        }
    }
}
```

### 示例 2：Service 层中使用

```java
package top.playereg.pix_vision.service.Impl;

import org.springframework.stereotype.Service;
import top.playereg.pix_vision.util.PixVisionLogger;

@Service
public class UserServiceImpl implements UserService {
    private static final PixVisionLogger log = PixVisionLogger.create(UserServiceImpl.class);

    @Override
    public User registerUser(String username, String password, String nickname, String email) {
        log.info("开始注册用户: {}", username);
        
        try {
            // 业务逻辑...
            log.info("用户注册成功: {}", username);
            return user;
        } catch (Exception e) {
            log.error("用户注册失败: {}", username, e);
            throw new RuntimeException("注册失败", e);
        }
    }
}
```

### 示例 3：Controller 层中使用

```java
package top.playereg.pix_vision.controller;

import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.util.PixVisionLogger;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final PixVisionLogger log = PixVisionLogger.create(UserController.class);

    @PostMapping("/login")
    public ResponsePojo<Boolean> login(@RequestParam String username, @RequestParam String password) {
        log.info("收到登录请求: {}", username);
        
        // 业务逻辑...
        
        log.info("用户 {} 登录成功", username);
        return ResponsePojo.success(true, "登录成功");
    }
}
```

### 示例 4：配置类中使用

```java
package top.playereg.pix_vision.config;

import org.springframework.context.annotation.Configuration;
import top.playereg.pix_vision.util.PixVisionLogger;

@Configuration
public class WebConfig {
    private static final PixVisionLogger log = PixVisionLogger.create(WebConfig.class);

    @PostConstruct
    public void init() {
        log.info("Web 配置初始化完成");
        log.debug("CORS 配置: {}", corsConfig);
    }
}
```

---

## 🔧 高级用法

### 1. 条件日志

```java
if (log.isDebugEnabled()) {
    log.debug("复杂计算结果: {}", expensiveCalculation());
}
```

### 2. 异常链记录

```java
try {
    // 业务逻辑
} catch (SpecificException e) {
    log.warn("捕获到特定异常: {}", e.getMessage());
    log.debug("完整堆栈", e);
} catch (Exception e) {
    log.error("未预期的错误", e);
}
```

### 3. 性能敏感日志

```java
long startTime = System.currentTimeMillis();
// 执行操作
long endTime = System.currentTimeMillis();
log.debug("操作耗时: {}ms", endTime - startTime);
```

---

## ⚠️ 注意事项

### 1. 不要记录敏感信息

```java
// ❌ 错误：记录明文密码
log.info("用户密码: {}", password);

// ✅ 正确：只记录脱敏信息
log.info("用户 {} 修改密码", username);
```

### 2. 合理使用日志级别

- **DEBUG**: 开发调试用，生产环境通常关闭
- **INFO**: 关键业务流程节点
- **WARN**: 潜在问题，但不影响正常运行
- **ERROR**: 需要立即关注的错误

### 3. 避免在循环中频繁记录 DEBUG 日志

```java
// ❌ 不推荐
for (User user : users) {
    log.debug("处理用户: {}", user.getId());
}

// ✅ 推荐
log.debug("开始处理 {} 个用户", users.size());
for (User user : users) {
    // 处理逻辑
}
log.debug("用户处理完成");
```

### 4. 异常日志要包含堆栈信息

```java
// ❌ 错误：丢失堆栈信息
log.error("错误: {}", e.getMessage());

// ✅ 正确：保留完整堆栈
log.error("处理失败", e);
```

---

## 🔄 迁移指南

### 从传统 Logger 迁移到 PixVisionLogger

**步骤 1**: 替换导入语句

```java
// 删除
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 添加
import top.playereg.pix_vision.util.PixVisionLogger;
```

**步骤 2**: 替换 Logger 声明

```java
// 删除
private static final Logger log = LoggerFactory.getLogger(YourClass.class);

// 添加
private static final PixVisionLogger log = PixVisionLogger.create(YourClass.class);
```

**步骤 3**: 日志调用保持不变

```java
// 无需修改，API 完全兼容
log.info("消息");
log.debug("格式: {}", param);
log.error("错误", exception);
```

---

## 📊 性能对比

`PixVisionLogger` 与传统 SLF4J Logger 性能完全一致，因为：

1. 底层仍使用 SLF4J + Logback/Log4j2
2. 默认方法在编译时内联，无额外开销
3. Logger 实例复用机制相同

---

## 🧪 测试示例

```java
@SpringBootTest
class UserServiceTest {
    private static final PixVisionLogger log = PixVisionLogger.create(UserServiceTest.class);
    
    @Autowired
    private UserService userService;

    @Test
    void testRegister() {
        log.info("开始测试用户注册");
        
        User user = userService.registerUser("test_user", "password", "Test", "test@example.com");
        
        log.info("注册用户 ID: {}", user.getUserId());
        assertNotNull(user);
    }
}
```

---

## 📚 相关文档

- [SLF4J 官方文档](https://www.slf4j.org/)
- [Logback 配置指南](https://logback.qos.ch/manual/configuration.html)
- [项目日志配置](../src/main/resources/yml-config/logging.yml)

---

## 👥 维护者

- **作者**: PlayerEG
- **维护**: PixVisionServer 开发团队

---

## 📅 更新日志

### v1.0.0 (2026-05-01)
- ✅ 初始版本发布
- ✅ 支持所有 SLF4J 日志级别
- ✅ 提供工厂方法创建 Logger
- ✅ 完全兼容现有 SLF4J API
