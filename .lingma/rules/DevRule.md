---
trigger: always_on
---

# PixVisionServer 开发规范 (DevRule)

## 1. 核心架构与分层
- **MVC 严格分层**：
  - **Controller**: 仅负责参数校验、调用 Service、返回 `ResponsePojo`。禁止包含业务逻辑或直接操作数据库。
  - **Service**: 实现核心业务逻辑、事务控制。禁止直接编写 SQL。
  - **Mapper**: 定义数据访问接口。**所有 SQL 必须在 XML 中编写**，禁止使用 MyBatis-Plus 的 `LambdaQueryWrapper` 等自动生成 SQL 的方法。
- **依赖注入规范**：
  - ✅ **允许使用**：`@Autowired`（Spring 标准注解，字段注入或构造器注入均可）
  - ❌ **禁止使用**：`@Resource`（JSR-250 注解，不符合 Spring Boot 3 最佳实践）
  - 💡 **推荐做法**：优先使用构造器注入配合 Lombok `@RequiredArgsConstructor`，但字段注入也是允许的
  - ⚠️ **重要提醒**：不要将"推荐"误解为"强制"，`@Autowired` 字段注入完全符合规范

## 2. 代码书写规范
- **注解简短化**：必须通过 `import` 引入后使用简短形式（如 `@Param`），**严禁**在代码中使用全限定名（如 `@org.apache.ibatis.annotations.Param`）。
- **实体类继承**：优先利用继承关系复用字段（如 `History extends Works`），避免在子类中重复定义父类已有的属性。
- **分页查询顺序**：必须先创建 `Page` 对象，再作为第一个参数传入 Service/Mapper 方法。
- **注释规范**：
  - **禁止在代码注释、文档字符串中使用 emoji 表情符号**（如 ✅ ❌ 💡 ⚠️ 等），只允许在规范文档中使用 emoji 进行视觉区分。
  - **Javadoc 标准格式**：公共类、接口、方法必须使用标准 Javadoc 注释，包含以下要素：
    - 简要描述（第一行）
    - 详细说明（使用 `<p>`、`<h3>`、`<ol>`、`<ul>` 等 HTML 标签格式化）
    - 使用场景（`<h3>使用场景</h3>` + 有序列表）
    - 使用示例（`<h3>使用示例</h3>` + `<pre>{@code ... }</pre>` 代码块）
    - 注意事项（`<h3>注意事项</h3>` + 无序列表）
    - 最佳实践（`<h3>最佳实践</h3>` + 无序列表，可选）
    - 作者信息（`@author`）
    - 相关类引用（`@see`）
    - 版本信息（`@since`，可选）
  - **Controller 层特殊规范**：
    - Controller 已有 Swagger 文档（`@Operation`），Javadoc 应保持**简洁**
    - 只需包含：简要描述、参数说明（`@param`）、返回值说明（`@return`）、作者信息（`@author`）
    - **不需要**：使用场景、使用示例、注意事项、最佳实践等详细内容（这些在 Swagger 中已有）
    - 详细文档由 `@Operation` 注解提供，避免重复
  - **参考示例**：
    - 注解注释示例：`.lingma/rules/example/JavadocCommentExample.java`
    - Service 层注释示例：`.lingma/rules/example/ServiceCommentExample.java`
    - Controller 层注释示例：`.lingma/rules/example/ControllerCommentExample.java`

## 3. 认证与安全
- **密码安全**：密码必须使用 `StrSwitchUtils.PasswdToHash256` 进行 SHA-256 加密。**绝对禁止**在日志或数据库中存储/记录明文密码。
- **Token 管理**：修改密码后必须调用 `tokenWhitelistService.removeAllUserTokens` 使旧 Token 失效。
- **权限控制**：需要认证的接口不加 `@PublicAccess`；公开接口必须显式标注 `@PublicAccess`。

## 4. API 文档 (Swagger)
- **描述规范**：`@Operation` 必须包含特性、参数说明、返回说明、业务逻辑和注意事项。
- **禁止项**：禁止在文档中包含 JSON 示例代码块或 curl 请求示例。
- **参考示例**：详细的标准写法请查看 `.lingma/rules/example/SwaggerDocExample.java`。

## 5. 常见避坑指南
- **别名冲突**：MyBatis-Plus 会自动扫描 POJO，确保不同包下不存在同名类，否则会导致启动报错 `alias is already mapped`。
- **空结果处理**：分页查询结果为空时，应返回包含空数组的 `IPage` 对象及成功状态，而非返回错误响应。
- **日志颜色**：非 Spring Bean 类（如工具类）使用 `PixVisionLogger.create(ClassName.class)` 获取带颜色的日志实例。

## 6. 使用 MCP 工具查询技术栈信息

在开发过程中，经常需要查询数据库结构、Redis 缓存状态等技术栈相关信息。MCP 工具提供了安全便捷的查询方式。

### 6.1 查询数据库表结构

#### 查看所有表
```sql
SELECT table_name, table_comment 
FROM information_schema.tables 
WHERE table_schema = 'db_pix_vision'
ORDER BY table_name
```

#### 查看特定表结构
```sql
SELECT column_name, data_type, is_nullable, column_key, column_comment
FROM information_schema.columns 
WHERE table_schema = 'db_pix_vision' AND table_name = 'tb_user'
ORDER BY ordinal_position
```

#### 查看外键关系
```sql
SELECT 
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'db_pix_vision'
LIMIT 10
```

### 6.2 查询数据示例（安全规范）

#### 查询用户信息（排除敏感字段）
```sql
SELECT user_id, username, nickname, email, role, status, create_time
FROM db_pix_vision.tb_user
WHERE status = 10
LIMIT 10
```

#### 查询作品统计
```sql
SELECT 
    u.username,
    COUNT(w.work_id) as work_count,
    SUM(w.view_count) as total_views
FROM db_pix_vision.tb_user u
LEFT JOIN db_pix_vision.tb_works w ON u.user_id = w.user_id
WHERE u.status = 10
GROUP BY u.user_id, u.username
ORDER BY total_views DESC
LIMIT 10
```

#### 查询操作日志
```sql
SELECT 
    log_id,
    user_id,
    operation_type,
    module_name,
    operate_time,
    ip_address
FROM db_pix_vision.tb_operate_log
ORDER BY operate_time DESC
LIMIT 20
```

### 6.3 Redis 缓存查询

#### 查看 Token 缓存
```javascript
// 列出所有 Token 键
mcp_redis_list({ pattern: "token:*" })

// 获取特定用户的 Token
mcp_redis_get({ key: "token:123:username" })
```

#### 查看验证码缓存
```javascript
// 列出所有验证码键
mcp_redis_list({ pattern: "verify:*" })

// 获取特定邮箱的验证码
mcp_redis_get({ key: "verify:email:user@example.com" })
```

#### 清理过期缓存
```javascript
// 先列出要删除的键
mcp_redis_list({ pattern: "temp:*" })

// 确认后批量删除
mcp_redis_delete({ key: ["temp:key1", "temp:key2"] })
```

### 6.4 查询注意事项

#### MySQL 查询规范
1. **必须指定数据库名**：所有表名使用 `db_pix_vision.table_name` 格式
2. **必须添加 LIMIT**：限制返回结果数量，避免性能问题
3. **禁止查询敏感字段**：不得包含 password、user_uuid、token 等字段
4. **单条语句执行**：每次只能执行一条 SQL，不支持多语句
5. **避免保留字别名**：不使用 current_user、current_time 等 MySQL 保留字

#### Redis 操作规范
1. **命名空间规范**：使用冒号分隔，如 `token:userId:username`
2. **设置过期时间**：临时数据必须设置 expireSeconds
3. **先查后删**：删除前先用 list 确认键是否存在
4. **不存储敏感明文**：密码等敏感信息不应存入 Redis

### 6.5 常用查询场景

#### 调试用户相关问题
```sql
-- 查找特定用户
SELECT user_id, username, nickname, email, role, status
FROM db_pix_vision.tb_user
WHERE username = 'test_user'
LIMIT 1

-- 查看用户的作品数量
SELECT COUNT(*) as work_count
FROM db_pix_vision.tb_works
WHERE user_id = 123 AND is_delete = 0
```

#### 调试作品相关问题
```sql
-- 查看作品详情
SELECT work_id, work_name, user_id, view_count, like_count, create_time
FROM db_pix_vision.tb_works
WHERE work_id = 456
LIMIT 1

-- 查看作品的系列信息
SELECT s.series_id, s.series_name, s.description
FROM db_pix_vision.tb_series s
INNER JOIN db_pix_vision.tb_works w ON s.series_id = w.series_id
WHERE w.work_id = 456
LIMIT 1
```

#### 调试权限相关问题
```sql
-- 查看用户角色
SELECT user_id, username, role
FROM db_pix_vision.tb_user
WHERE user_id = 123
LIMIT 1

-- 查看角色的权限配置（如果有权限表）
SELECT * FROM db_pix_vision.tb_role_permission
WHERE role_id = 11
LIMIT 10
```

### 6.6 重要提醒

⚠️ **首次使用 MCP 工具前必须阅读完整指南**：
- 执行 `Skill: mcp-usage` 查看详细的使用规范
- 参考 `.lingma/rules/McpUsage.md` 了解核心规则
- 参考 `.lingma/skills/mcp-usage/SKILL.md` 了解最佳实践

❌ **严格禁止的操作**：
- 执行 INSERT、UPDATE、DELETE 等写操作
- 查询或记录明文密码
- 使用 USE 命令切换数据库
- 在生产环境随意执行复杂查询

✅ **推荐的最佳实践**：
- 先用 LIMIT 小数据集测试查询
- 复杂查询先在 MySQL 客户端验证
- 定期清理过期的 Redis 缓存
- 使用 information_schema 了解数据库结构

## 7. AI 助手常见误区（重要！）
### ❌ 误区 1：将"推荐"误解为"强制"
- **错误理解**：看到"推荐使用构造器注入"就认为必须修改所有 `@Autowired` 字段注入
- **正确理解**：
  - ✅ `@Autowired` 字段注入：**完全允许**，符合规范
  - ❌ `@Resource`：**明确禁止**，需要修改
  - 💡 构造器注入：**推荐但不强制**，可作为长期优化方向
- **判断原则**：只有明确标注"禁止"、"严禁"、"❌"的才必须修改

### ❌ 误区 2：过度优化现有代码
- **错误做法**：主动修改已经符合规范的代码，追求"更优"的实现方式
- **正确做法**：
  - 只修改**明确违反规范**的代码
  - 对于"推荐但不强制"的内容，除非用户明确要求，否则不要主动修改
  - 保持代码稳定性优先于理论上的最佳实践

### ❌ 误区 3：忽视规范的明确表述
- **错误理解**：根据业界通用标准自行推断规范要求
- **正确做法**：
  - 严格遵循项目规范文档的字面意思
  - 规范说"禁止 `@Resource`" ≠ "禁止所有字段注入"
  - 规范说"推荐构造器注入" ≠ "必须使用构造器注入"

### ✅ 正确的工作流程
1. **仔细阅读规范**：区分"禁止"、"推荐"、"可选"的不同级别
2. **精准识别问题**：只标记明确违反规范的地方
3. **谨慎执行修改**：不确定时先询问用户，不要自作主张
4. **尊重现有代码**：符合规范的代码即使不是最优也不要随意改动