---
trigger: model_decision
description: 快速了解.lingma/rules/example目录
---

### 3. ControllerCommentExample.java
**Controller 层注释示例**

展示了如何为 Controller 类和接口方法编写完整的 Javadoc 注释，包括：
- Controller 类的功能说明
- RESTful 接口的详细文档
- Swagger @Operation 注解的标准写法
- 请求参数和响应格式说明
- 业务逻辑和注意事项

**适用场景**：Controller 类、REST API 接口

---

### 4. SwaggerDocExample.java
**Swagger 文档示例**

展示了如何在 Controller 中编写规范的 Swagger API 文档，包括：
- @Tag 注解的使用
- @Operation 注解的完整结构
- @Parameter 注解的参数说明
- 文档描述的 Markdown 格式

**适用场景**：所有需要生成 API 文档的 Controller 接口

---

## 注释规范要点

### 必须包含的要素

1. **简要描述**：第一行简短说明功能
2. **详细说明**：使用 `<p>` 标签分段说明
3. **使用场景**：`<h3>使用场景</h3>` + 有序列表
4. **使用示例**：`<h3>使用示例</h3>` + `<pre>{@code}</pre>` 代码块
5. **注意事项**：`<h3>注意事项</h3>` + 无序列表
6. **最佳实践**：`<h3>最佳实践</h3>` + 无序列表（可选）
7. **作者信息**：`@author`
8. **相关引用**：`@see`
9. **版本信息**：`@since`（可选）

### 禁止事项

- ❌ **禁止在注释中使用 emoji 表情符号**（如 ✅ ❌ 💡 ⚠️ 等）
- ❌ 禁止使用非标准 HTML 标签
- ❌ 禁止省略关键参数的说明
- ❌ 禁止缺少使用示例

### 推荐做法

- ✅ 使用构造器注入依赖时，在注释中说明
- ✅ 提供完整的 cURL 或代码调用示例
- ✅ 明确标注必填/可选参数
- ✅ 说明返回值的可能情况（成功/失败/null）
- ✅ 记录重要的业务规则和约束条件

---

## 使用方法

1. 编写新代码时，参考对应的示例文件
2. 确保注释包含所有必需的要素
3. 使用 IDE 的 Javadoc 生成功能辅助编写
4. 通过 `mvn javadoc:javadoc` 生成文档检查格式
5. 在 Swagger UI 中预览 API 文档效果

---

## 相关文档

- 开发规范：`../DevRule.md`
- Swagger 文档规范：`../DevRule.md` 第 4 章
- 项目 AGENTS 指南：`../../AGENTS.md`

---

**最后更新**：2026-05-10  
**维护者**：PlayerEG