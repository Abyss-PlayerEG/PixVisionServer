---
name: mcp-usage
description: Sequential-Thinking MCP 工具使用指南
---

# MCP 工具使用技能

本技能提供 **Sequential-Thinking MCP 服务器**的使用指南，用于结构化思考和问题分析。

## 1. Sequential-Thinking MCP 服务器

### 1.1 基本信息
- **工具名称**: `mcp_sequential-thinking_sequentialthinking`
- **类型**: 结构化思考和问题分析工具
- **用途**: 复杂问题分析、方案设计、决策制定、问题诊断

### 1.2 可用工具
- `mcp_sequential-thinking_sequentialthinking`: 执行结构化思考过程

### 1.3 核心使用规则

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

### 1.4 实用使用示例

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

#### 问题分析思考示例
```javascript
// 步骤 1：定义问题
{
  thought: "如何优化系统性能？",
  nextThoughtNeeded: true,
  thoughtNumber: 1,
  totalThoughts: 5
}

// 步骤 2：识别关键方面
{
  thought: "影响性能的关键因素包括：数据库查询、缓存策略、代码逻辑、资源配置等",
  nextThoughtNeeded: true,
  thoughtNumber: 2,
  totalThoughts: 5
}

// 步骤 3：深入分析
{
  thought: "数据库查询优化是最直接有效的方式，应首先检查慢查询日志和执行计划",
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

### 1.5 最佳实践
- 🎯 **明确目标**：第一步清晰定义要解决的问题
- 📈 **逻辑递进**：后续步骤应建立在前序思考基础上
- 🔢 **适度细化**：根据问题复杂度合理设置思考步骤数量
- ✅ **及时总结**：最后一步应给出明确的结论或解决方案
- 🎯 **保持聚焦**：每步思考应围绕核心问题，避免偏离主题

---

## 2. 常见问题解决

### Q1: 何时使用 Sequential-Thinking 工具？
**解决方案**: 
- 需要分析复杂问题时
- 制定技术方案时
- 进行问题诊断时
- 需要结构化思考的场景

### Q2: 如何确定思考步骤数量？
**解决方案**: 
- 简单问题：3-4 步
- 中等复杂度：5-7 步
- 复杂问题：可以动态调整，不必一开始就确定

### Q3: 思考过程中发现需要更多步骤怎么办？
**解决方案**: 
- 可以随时调整 `totalThoughts` 参数
- 保持 `nextThoughtNeeded: true` 直到得出完整结论

---

## 3. 安全与性能提醒

### 🔒 安全要求
- **保持专注**：每步思考应围绕核心问题
- **避免泄露**：不要在思考内容中包含敏感信息

### ⚡ 性能优化
- **适度细化**：不要过度拆分思考步骤
- **及时总结**：避免无意义的循环思考

### 🐛 错误处理
- **逻辑不清**：重新审视问题定义
- **偏离主题**：回到核心问题重新分析

---

## 4. 快速参考卡片

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

## 5. 相关资源
- Sequential-Thinking 规则: `.lingma/rules/SequentialThinking.md`
- Sequential-Thinking 技能: `.lingma/skills/sequential-thinking/SKILL.md`
