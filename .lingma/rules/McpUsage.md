---
trigger: always_on
---

# MCP 工具使用强制规范

## ⚠️ 重要：首次使用 MCP 工具前必须阅读

**在第一次调用任何 MCP 工具（MySQL、Redis 或 Sequential-Thinking）之前，必须先执行以下操作：**

```
Skill: mcp-usage
```

这是**强制要求**，违反此规则会导致查询错误、安全风险或性能问题。

---

## MySQL MCP 工具核心规则（必须遵守）

### ✅ 必须做到
1. **显式指定数据库名**：所有表名必须使用 `db_pix_vision.table_name` 格式
2. **单条语句执行**：每次只能执行一条 SQL，不支持分号分隔的多语句
3. **排除敏感字段**：查询时不得包含 `password`、`user_uuid`、`token` 等敏感字段
4. **添加 LIMIT**：所有查询必须包含 `LIMIT` 子句限制结果集大小
5. **使用 information_schema**：查看表结构时使用标准 SQL，不使用 `DESCRIBE` 等非标准命令

### ❌ 严格禁止
1. **写操作**：禁止执行 INSERT、UPDATE、DELETE 等修改数据的操作
2. **USE 命令**：不支持 `USE database` 切换数据库上下文
3. **保留字别名**：不使用 MySQL 保留字作为列别名（如 `current_user`、`current_time`）
4. **rowid 伪列**：MySQL 不支持 `rowid`，这是 PostgreSQL 特有的
5. **明文密码查询**：绝对禁止查询或返回密码字段

### 📋 标准查询模板

#### 查看表列表
```sql
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'db_pix_vision'
```

#### 查看表结构
```sql
SELECT column_name, data_type, is_nullable, column_key 
FROM information_schema.columns 
WHERE table_schema = 'db_pix_vision' AND table_name = '{table_name}'
ORDER BY ordinal_position
```

#### 查询数据（安全版）
```sql
SELECT {非敏感字段} FROM db_pix_vision.{table_name} 
WHERE {condition} 
LIMIT {n}
```

---

## Redis MCP 工具核心规则

### ✅ 必须做到
1. **命名空间规范**：使用冒号分隔，如 `token:userId:username`
2. **设置过期时间**：临时数据必须设置 `expireSeconds`
3. **先查后删**：删除前先用 `list` 确认键是否存在

### ❌ 严格禁止
1. **不使用占位符**：必须使用真实值，不能用 `${REDIS_URL}` 等
2. **不存储敏感明文**：密码等敏感信息不应存入 Redis

### 📋 常用操作
```javascript
// Token 管理（7天过期）
mcp_redis_set({ key: "token:123:user", value: "...", expireSeconds: 604800 })
mcp_redis_get({ key: "token:123:user" })
mcp_redis_delete({ key: "token:123:user" })

// 验证码管理（5分钟过期）
mcp_redis_set({ key: "verify:email:user@example.com", value: "123456", expireSeconds: 300 })

// 键管理
mcp_redis_list({ pattern: "token:*" })
```

---

## Sequential-Thinking MCP 工具核心规则

### ✅ 必须做到
1. **明确思考目标**：第一步清晰定义要解决的问题
2. **逻辑递进**：后续步骤应建立在前序思考基础上
3. **适度细化**：根据问题复杂度合理设置思考步骤数量（通常 3-7 步）
4. **及时总结**：最后一步应给出明确的结论或解决方案
5. **保持聚焦**：每步思考应围绕核心问题，避免偏离主题

### ❌ 严格禁止
1. **空洞内容**：每个 thought 必须是有意义的思考步骤
2. **跳过步骤**：确保思考过程连贯，不要跳跃
3. **过早结束**：未得出结论前不要设置 `nextThoughtNeeded: false`
4. **偏离主题**：思考内容应与核心问题相关

### 📋 标准使用模板

```javascript
// 步骤 1：明确问题
mcp_sequential-thinking_sequentialthinking({
  thought: "我需要解决的问题是什么？",
  nextThoughtNeeded: true,
  thoughtNumber: 1,
  totalThoughts: 5
})

// 中间步骤：分析问题
mcp_sequential-thinking_sequentialthinking({
  thought: "问题的关键要素和可能方案",
  nextThoughtNeeded: true,
  thoughtNumber: 2,
  totalThoughts: 5
})

// 最后一步：得出结论
mcp_sequential-thinking_sequentialthinking({
  thought: "最终结论和解决方案",
  nextThoughtNeeded: false,
  thoughtNumber: 5,
  totalThoughts: 5
})
```

---

## 🚨 常见错误示例

### MySQL 错误
```sql
-- ❌ 错误：未指定数据库
SELECT * FROM tb_user

-- ❌ 错误：包含敏感字段
SELECT user_id, password FROM db_pix_vision.tb_user

-- ❌ 错误：使用保留字别名
SELECT VERSION() as current_user

-- ❌ 错误：使用 rowid
SELECT rowid FROM db_pix_vision.tb_user

-- ✅ 正确
SELECT user_id, username, nickname, email, status 
FROM db_pix_vision.tb_user 
LIMIT 5
```

---

## 📖 完整文档位置

详细的 MCP 使用指南请查看：
- Skill: `.lingma/skills/mcp-usage/SKILL.md`
- 规则文档: `.lingma/rules/McpUsage.md`
- Sequential-Thinking 规则: `.lingma/rules/SequentialThinking.md`
- Sequential-Thinking 技能: `.lingma/skills/sequential-thinking/SKILL.md`

**记住：首次使用前必须调用 `Skill: mcp-usage`！**
