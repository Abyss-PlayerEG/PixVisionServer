---
name: mcp-usage
description: MySQL、Redis 和 Sequential-Thinking MCP 工具使用指南，包含查询示例、注意事项和最佳实践
---

# MCP 工具使用技能

本技能提供 MySQL、Redis 和 Sequential-Thinking MCP 服务器的完整使用指南，帮助开发者安全高效地进行数据库操作和问题分析。

## 1. MySQL MCP 服务器

### 1.1 基本信息
- **包名**: `@benborla29/mcp-server-mysql`
- **类型**: 只读查询工具
- **数据库**: `db_pix_vision`
- **连接配置**: 
  - Host: `localhost`
  - Port: `3306`
  - Username: `root`
  - Password: `123456`

### 1.2 可用工具
- `mcp_mysql_mysql_query`: 执行 SQL 查询（仅支持 SELECT 等只读操作）

### 1.3 核心使用规则

#### ✅ 必须遵守的规则
1. **显式指定数据库名**：所有表名必须使用 `db_pix_vision.table_name` 格式
2. **单条语句执行**：每次只能执行一条 SQL，不支持分号分隔的多语句
3. **避免保留字别名**：不使用 MySQL 保留字作为列别名

#### ❌ 禁止的操作
1. **写操作**：不能执行 INSERT、UPDATE、DELETE 等修改数据的操作
2. **USE 命令**：不支持 `USE database` 切换数据库上下文
3. **敏感数据查询**：不要查询或返回密码、Token 等敏感字段

### 1.4 实用查询示例

#### 探索数据库结构
```sql
-- 查看所有数据库
SHOW DATABASES

-- 查看指定数据库中的表
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'db_pix_vision'

-- 查看表结构（列名、类型、是否可空、主键信息）
SELECT column_name, data_type, is_nullable, column_key 
FROM information_schema.columns 
WHERE table_schema = 'db_pix_vision' AND table_name = 'tb_user'
```

#### 数据查询示例
```sql
-- 基础查询（带限制）
SELECT user_id, username, nickname, email, status 
FROM db_pix_vision.tb_user 
LIMIT 5

-- 条件查询
SELECT user_id, username, nickname 
FROM db_pix_vision.tb_user 
WHERE status = 10 
LIMIT 10

-- 聚合统计
SELECT status, COUNT(*) as user_count 
FROM db_pix_vision.tb_user 
GROUP BY status

-- 多表关联查询
SELECT u.username, w.work_name, w.create_time
FROM db_pix_vision.tb_user u
INNER JOIN db_pix_vision.tb_works w ON u.user_id = w.user_id
LIMIT 10

-- 排序查询
SELECT user_id, username, create_time 
FROM db_pix_vision.tb_user 
ORDER BY create_time DESC 
LIMIT 10
```

#### 常见错误示例
```sql
-- ❌ 错误：使用保留字作为别名
SELECT VERSION() as current_user    -- current_user 是保留字
SELECT NOW() as current_time        -- current_time 是保留字

-- ❌ 错误：多条语句一起执行
USE db_pix_vision; SHOW TABLES      -- 不支持多语句

-- ❌ 错误：未指定数据库
SELECT * FROM tb_user               -- 必须使用 db_pix_vision.tb_user

-- ❌ 错误：使用 rowid（MySQL 不支持）
SELECT rowid FROM tb_user           -- PostgreSQL 特有，MySQL 不支持
```

### 1.5 最佳实践
- 📊 **优先使用 information_schema**：查询元数据了解数据库结构
- 🔍 **先用 LIMIT 测试**：复杂查询先用小数据集验证
- ⚡ **注意查询性能**：大表查询务必添加 LIMIT 和合适的 WHERE 条件
- 🔒 **保护敏感数据**：查询用户数据时排除 password、user_uuid 等字段
- 📝 **本地测试先行**：复杂 SQL 先在 MySQL 客户端测试再用于 MCP

---

## 2. Redis MCP 服务器

### 2.1 基本信息
- **连接地址**: `redis://localhost:6379`
- **用途**: Token 白名单管理、验证码缓存、会话管理

### 2.2 可用工具
- `mcp_redis_set`: 设置键值对（可选过期时间）
- `mcp_redis_get`: 根据键获取值
- `mcp_redis_delete`: 删除一个或多个键
- `mcp_redis_list`: 列出匹配模式的键

### 2.3 核心使用规则

#### ✅ 推荐做法
1. **命名空间规范**：使用冒号分隔，如 `token:userId:username`
2. **设置过期时间**：临时数据必须设置 expireSeconds
3. **先查后删**：删除前先用 list 确认键是否存在

#### ❌ 禁止操作
1. **不使用占位符**：必须使用真实值，不能用 `${REDIS_URL}` 等
2. **不存储敏感明文**：密码等敏感信息不应存入 Redis

### 2.4 实用操作示例

#### Token 管理
```javascript
// 设置 Token（7天过期）
mcp_redis_set({ 
  key: "token:123:dev_user", 
  value: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", 
  expireSeconds: 604800 
})

// 获取 Token
mcp_redis_get({ key: "token:123:dev_user" })

// 删除单个 Token
mcp_redis_delete({ key: "token:123:dev_user" })

// 批量删除用户的所有 Token
mcp_redis_delete({ 
  key: ["token:123:dev_user", "token:123:old_token"] 
})
```

#### 验证码管理
```javascript
// 设置验证码（5分钟过期）
mcp_redis_set({ 
  key: "verify:email:user@example.com", 
  value: "123456", 
  expireSeconds: 300 
})

// 获取验证码
mcp_redis_get({ key: "verify:email:user@example.com" })

// 验证后删除
mcp_redis_delete({ key: "verify:email:user@example.com" })
```

#### 键管理
```javascript
// 列出所有 Token 相关的键
mcp_redis_list({ pattern: "token:*" })

// 列出特定用户的键
mcp_redis_list({ pattern: "token:123:*" })

// 列出所有验证码键
mcp_redis_list({ pattern: "verify:*" })

// 列出所有键（谨慎使用）
mcp_redis_list({ pattern: "*" })
```

### 2.5 最佳实践
- 🏷️ **统一命名规范**：`{type}:{identifier}:{sub_identifier}`
- ⏱️ **合理设置过期时间**：
  - Token: 7天 (604800秒)
  - 验证码: 5分钟 (300秒)
  - 临时缓存: 根据业务需求设定
- 🧹 **定期清理**：使用 list + delete 清理过期或无用键
- 🔍 **模式匹配技巧**：
  - `*` 匹配任意字符
  - `?` 匹配单个字符
  - `[abc]` 匹配指定字符集

---

## 3. Sequential-Thinking MCP 服务器

### 3.1 基本信息
- **工具名称**: `mcp_sequential-thinking_sequentialthinking`
- **类型**: 结构化思考和问题分析工具
- **用途**: 复杂问题分析、方案设计、决策制定、问题诊断

### 3.2 可用工具
- `mcp_sequential-thinking_sequentialthinking`: 执行结构化思考过程

### 3.3 核心使用规则

#### ✅ 必须遵守的规则
1. **明确思考目标**：第一步清晰定义要解决的问题
2. **逻辑递进**：后续步骤应建立在前序思考基础上
3. **适度细化**：根据问题复杂度合理设置思考步骤数量（通常 3-7 步）
4. **及时总结**：最后一步应给出明确的结论或解决方案
5. **保持聚焦**：每步思考应围绕核心问题，避免偏离主题

#### ❌ 禁止的操作
1. **空洞内容**：每个 thought 必须是有意义的思考步骤
2. **跳过步骤**：确保思考过程连贯，不要跳跃
3. **过早结束**：未得出结论前不要设置 `nextThoughtNeeded: false`
4. **偏离主题**：思考内容应与核心问题相关

### 3.4 实用使用示例

#### 基本使用流程
```javascript
// 步骤 1：明确问题
mcp_sequential-thinking_sequentialthinking({
  thought: "我需要解决的问题是什么？",
  nextThoughtNeeded: true,
  thoughtNumber: 1,
  totalThoughts: 5
})

// 步骤 2：分析要素
mcp_sequential-thinking_sequentialthinking({
  thought: "问题的关键要素有哪些？",
  nextThoughtNeeded: true,
  thoughtNumber: 2,
  totalThoughts: 5
})

// 步骤 3：深入分析
mcp_sequential-thinking_sequentialthinking({
  thought: "针对每个要素进行深入分析",
  nextThoughtNeeded: true,
  thoughtNumber: 3,
  totalThoughts: 5
})

// 步骤 4：制定方案
mcp_sequential-thinking_sequentialthinking({
  thought: "基于分析结果制定解决方案",
  nextThoughtNeeded: true,
  thoughtNumber: 4,
  totalThoughts: 5
})

// 步骤 5：总结结论
mcp_sequential-thinking_sequentialthinking({
  thought: "最终结论和最佳实践建议",
  nextThoughtNeeded: false,
  thoughtNumber: 5,
  totalThoughts: 5
})
```

#### 数据库查询优化思考示例
```javascript
// 步骤 1：定义问题
{
  thought: "如何优化数据库查询性能？",
  nextThoughtNeeded: true,
  thoughtNumber: 1,
  totalThoughts: 5
}

// 步骤 2：识别关键方面
{
  thought: "影响数据库性能的关键因素包括：索引、SQL语句、连接池、缓存策略等",
  nextThoughtNeeded: true,
  thoughtNumber: 2,
  totalThoughts: 5
}

// 步骤 3：深入分析
{
  thought: "索引优化是最直接有效的方式，应首先检查慢查询日志和执行计划",
  nextThoughtNeeded: true,
  thoughtNumber: 3,
  totalThoughts: 5
}

// 步骤 4：制定方案
{
  thought: "具体优化措施：1)添加合适索引 2)优化SQL结构 3)配置连接池 4)实施缓存",
  nextThoughtNeeded: true,
  thoughtNumber: 4,
  totalThoughts: 5
}

// 步骤 5：总结结论
{
  thought: "最佳实践：定期监控慢查询、合理使用索引、优化SQL、使用缓存、调整连接池参数",
  nextThoughtNeeded: false,
  thoughtNumber: 5,
  totalThoughts: 5
}
```

### 3.5 最佳实践
- 🎯 **明确目标**：第一步清晰定义要解决的问题
- 📈 **逻辑递进**：后续步骤应建立在前序思考基础上
- 🔢 **适度细化**：根据问题复杂度合理设置思考步骤数量
- ✅ **及时总结**：最后一步应给出明确的结论或解决方案
- 🎯 **保持聚焦**：每步思考应围绕核心问题，避免偏离主题

---

## 4. 常见问题解决

### Q1: MySQL 查询报错 "reserved word" 怎么办？
**解决方案**: 
- 检查别名是否使用了 MySQL 保留字
- 参考 [MySQL 保留字列表](https://dev.mysql.com/doc/refman/8.0/en/reserved-words.html)
- 使用反引号包裹或更换别名

```sql
-- ❌ 错误
SELECT COUNT(*) as user FROM tb_user

-- ✅ 正确
SELECT COUNT(*) as user_count FROM db_pix_vision.tb_user
```

### Q2: 如何快速了解数据库结构？
**解决方案**: 分步查询

```sql
-- 第1步：查看所有表
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'db_pix_vision'

-- 第2步：查看特定表结构
SELECT column_name, data_type, is_nullable, column_key, column_comment
FROM information_schema.columns 
WHERE table_schema = 'db_pix_vision' AND table_name = 'tb_user'
ORDER BY ordinal_position
```

### Q3: Redis 连接失败怎么办？
**解决方案**: 
1. 检查 Redis 服务是否启动：`redis-cli ping`（应返回 PONG）
2. 检查端口是否正确：默认 6379
3. 检查防火墙设置

### Q4: 如何安全地清理大量 Redis 键？
**解决方案**: 分批操作

```javascript
// 第1步：先列出要删除的键
mcp_redis_list({ pattern: "temp:*" })

// 第2步：确认无误后批量删除（建议每次不超过100个）
mcp_redis_delete({ 
  key: ["temp:key1", "temp:key2", "temp:key3", ...] 
})
```

### Q5: 能否执行复杂的 JOIN 查询？
**解决方案**: 可以，但要注意性能

```sql
-- ✅ 推荐的复杂查询（带 LIMIT）
SELECT u.username, u.nickname, w.work_name, w.view_count
FROM db_pix_vision.tb_user u
INNER JOIN db_pix_vision.tb_works w ON u.user_id = w.user_id
WHERE u.status = 10 AND w.is_delete = 0
ORDER BY w.view_count DESC
LIMIT 20
```

---

## 5. 安全与性能提醒

### 🔒 安全要求
- **禁止暴露敏感信息**：不在查询结果中包含 password、secret_key 等
- **生产环境谨慎使用**：MCP 工具主要用于开发调试
- **权限最小化**：MySQL MCP 为只读，但仍需注意数据隐私

### ⚡ 性能优化
- **限制结果集大小**：始终使用 LIMIT 子句
- **避免 N+1 查询**：尽量使用 JOIN 而非多次单独查询
- **监控执行时间**：工具会返回查询耗时，超过 100ms 需优化
- **合理使用索引**：WHERE 和 ORDER BY 字段应有索引

### 🐛 错误处理
- **SQL 语法错误**：仔细阅读错误信息，检查语法和表名
- **连接失败**：确认 MySQL/Redis 服务已启动
- **空结果处理**：查询无结果时返回空数组，非错误状态

---

## 6. 快速参考卡片

### MySQL 快速查询模板
```sql
-- 查表列表
SELECT table_name FROM information_schema.tables WHERE table_schema = 'db_pix_vision'

-- 查表结构
SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'db_pix_vision' AND table_name = '{table_name}'

-- 查数据（安全版）
SELECT {columns} FROM db_pix_vision.{table_name} WHERE {condition} LIMIT {n}

-- 统计数据
SELECT COUNT(*) as total FROM db_pix_vision.{table_name} WHERE {condition}
```

### Redis 快速操作模板
```javascript
// 设置键（带过期）
mcp_redis_set({ key: "{prefix}:{id}", value: "{value}", expireSeconds: {seconds} })

// 获取键
mcp_redis_get({ key: "{prefix}:{id}" })

// 删除键
mcp_redis_delete({ key: "{prefix}:{id}" })

// 查找键
mcp_redis_list({ pattern: "{prefix}:*" })
```

### Sequential-Thinking 快速使用模板
```javascript
// 基本模板
mcp_sequential-thinking_sequentialthinking({
  thought: "{当前思考内容}",
  nextThoughtNeeded: {true/false},  // 最后一步为 false
  thoughtNumber: {当前步骤编号},     // 从 1 开始递增
  totalThoughts: {预估总步骤数}      // 可根据需要调整
})
```

---

## 7. 相关资源
- MySQL 官方文档: https://dev.mysql.com/doc/
- Redis 命令参考: https://redis.io/commands/
- 项目数据库配置: `src/main/resources/yml-config/jdbc.yml`
- 项目 Redis 配置: `src/main/resources/yml-config/redis.yml`
- 详细规范文档: `.lingma/rules/McpUsage.md`
- Sequential-Thinking 规则: `.lingma/rules/SequentialThinking.md`
- Sequential-Thinking 技能: `.lingma/skills/sequential-thinking/SKILL.md`
