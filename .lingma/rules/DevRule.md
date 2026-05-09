---
trigger: always_on
---

# PixVisionServer 开发规范 (DevRule)

## 1. 核心架构与分层
- **MVC 严格分层**：
  - **Controller**: 仅负责参数校验、调用 Service、返回 `ResponsePojo`。禁止包含业务逻辑或直接操作数据库。
  - **Service**: 实现核心业务逻辑、事务控制。禁止直接编写 SQL。
  - **Mapper**: 定义数据访问接口。**所有 SQL 必须在 XML 中编写**，禁止使用 MyBatis-Plus 的 `LambdaQueryWrapper` 等自动生成 SQL 的方法。
- **依赖注入**：Spring Boot 3 环境下，统一使用构造器注入（配合 Lombok `@RequiredArgsConstructor`），禁止使用 `@Resource`。

## 2. 代码书写规范
- **注解简短化**：必须通过 `import` 引入后使用简短形式（如 `@Param`），**严禁**在代码中使用全限定名（如 `@org.apache.ibatis.annotations.Param`）。
- **实体类继承**：优先利用继承关系复用字段（如 `History extends Works`），避免在子类中重复定义父类已有的属性。
- **分页查询顺序**：必须先创建 `Page` 对象，再作为第一个参数传入 Service/Mapper 方法。

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
