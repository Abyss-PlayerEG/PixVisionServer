# PixVisionServer - AI Agent 开发指南

> **注意**：详细的开发规范（MVC 分层、代码风格、安全要求等）已迁移至 `.lingma/rules/DevRule.md`。本文档主要提供项目核心信息、工具类速查及环境配置指引。

---

## 1. 身份与认证

### 角色与状态
| 类型 | 代码 | 说明 |
|------|------|------|
| **角色** | 11, 22, 55, 66, 77 | 普通用户, 创作者, 审核员, 工单管理员, 系统管理员 |
| **状态** | 10, 20, 30 | 正常, 冻结 (保留数据), 封禁 (严重违规) |

### 关键操作
- **获取用户 ID**：优先从 `request.getAttribute("userId")` 获取，或手动解析 Token。
- **密码处理**：使用 `StrSwitchUtils.PasswdToHash256(password)`。**严禁记录明文密码**。
- **Token 下线**：修改密码后必须调用 `tokenWhitelistService.removeAllUserTokens(userId, username)`。

---

## 2. 常用工具类速查

```java
// JWT 工具
String token = JWTUtils.createToken(userId, username);
Integer userId = JWTUtils.getUserIdFromToken(token);

// UUID 转换
byte[] bytes = StrSwitchUtils.uuid2Bytes(uuidString);
String uuid = StrSwitchUtils.bytes2Uuid(bytes);

// 自定义日志（带颜色）
private static final PixVisionLogger log = PixVisionLogger.create(ClassName.class);
log.info("消息内容"); // 自动根据级别着色
```

---

## 3. 环境与配置

### 配置文件路径
- **用户自定义**：`~/.pix_vision/application.yml` （修改这里）
- **核心模板**：`src/main/resources/yml-config/*.yml` （不修改）

### 运行与构建
```bash
mvn spring-boot:run          # 启动服务
mvn clean package -DskipTests # 构建 JAR
java -jar target/PixVisionServer-0.0.1-SNAPSHOT.jar
```

### API 文档
访问地址：http://localhost:9090/doc.html

---

## 4. Git 提交规范
```
<type>(<scope>): <subject>
feat(user): 添加用户注册功能
fix(auth): 修复 Token 验证问题
refactor(service): 优化查询逻辑
```