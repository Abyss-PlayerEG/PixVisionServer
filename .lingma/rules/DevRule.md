---
trigger: model_decision
description: 代码功能实现，代码规范化调整
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

## 6. AI 助手常见误区（重要！）
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