# AI 审核记录入库升级清单

> 目标：将 AI 审核返回的 `reason` 和 `insult_words` 持久化到 `tb_content_audit_record` 表
> 范围：作品、评论、系列、昵称 4 种内容类型

---

## 数据库变更

- [x] 创建 `tb_content_audit_record` 表（已完成）

---

## 一、新增 Entity + Mapper 层（3 个文件）

### 1.1 ContentAuditRecord 实体类
- **路径**: `pojo/entity/ContentAuditRecord.java`
- **字段**: `record_id`, `content_type`, `content_id`, `approval_status`, `audit_reason`, `insult_words`, `create_time`
- **注解**: `@Data`, `@TableName("tb_content_audit_record")`, `@TableId`

### 1.2 ContentAuditRecordMapper 接口
- **路径**: `mapper/ContentAuditRecordMapper.java`
- `extends BaseMapper<ContentAuditRecord>`
- 方法: `int insertRecord(ContentAuditRecord record);`

### 1.3 ContentAuditRecordMapper.xml
- **路径**: `resources/mapper/ContentAuditRecordMapper.xml`
- `<insert>` 标签，`useGeneratedKeys="true" keyProperty="record_id"`

### 1.4 验证
- [ ] 编译通过
- [ ] 确认 MyBatis 能正确绑定

---

## 二、各 Service 接入审核记录（4 个 Service）

> 方案：各 Service 直接注入 `ContentAuditRecordMapper`，在 AI 审核调用后插入记录。
> 不改动 `ContentAuditService`（保持其纯粹性，只负责调用 AI API 返回结果）。

### 2.1 作品上传 — WorkServiceImpl.uploadWork()
- **content_type** = `100`
- **content_id** = 新创建的 `workId`
- **改动点**: `auditContent()` 调用后，INSERT 审核记录
- **审核文本**: 作品标题

### 2.2 评论发布 — CommentServiceImpl.addComment()
- **content_type** = `200`
- **content_id** = 新创建的 `commentId`
- **改动点**: `auditContent()` 调用后，INSERT 审核记录
- **审核文本**: 评论内容

### 2.3 系列新增/更新 — SeriesServiceImpl.addSeries() / updateSeriesInfo()
- **content_type** = `300`
- **content_id** = `seriesId`（新增时 useGeneratedKeys 回填）
- **改动点**: 两处 `auditContent()` 调用后，各 INSERT 审核记录
- **审核文本**: "标题：xxx\n描述：xxx"

### 2.4 昵称变更 — UserServiceImpl.updateNicknameWithAudit()
- **content_type** = `400`
- **content_id** = `lock.getLock_id()`（useGeneratedKeys 回填后的 lockId）
- **改动点**: `auditContent()` 调用后，INSERT 审核记录
- **审核文本**: 昵称字符串

### 2.5 验证
- [ ] 编译通过
- [ ] 各场景审核记录正确写入数据库

---

## 三、管理员查询升级 — 新建 Admin VO（3 个文件）

> 方案：创建 Admin VO 类继承实体，添加 `audit_reason` 和 `insult_words` 字段。
> Service 层查询实体后，批量查审核记录并填充到 VO 中返回。
> **不动 Mapper XML**，在 Service 层做数据组装。

### 3.1 AdminWorkVO
- **路径**: `pojo/VO/admin/AdminWorkVO.java`
- `extends Works`，新增 `@TableField(exist = false)` 字段：
  - `String audit_reason`
  - `String insult_words`

### 3.2 AdminCommentVO
- **路径**: `pojo/VO/admin/AdminCommentVO.java`
- `extends BaseComment`，新增同上两个字段

### 3.3 AdminSeriesVO
- **路径**: `pojo/VO/admin/AdminSeriesVO.java`
- `extends Series`，新增同上两个字段

### 3.4 UserDataChangeLockVO 扩展
- **路径**: `pojo/VO/UserDataChangeLockVO.java`（已存在）
- 新增字段：`String audit_reason`、`String insult_words`

---

## 四、管理端 Service 查询升级（4 个 Service）

> 每个管理查询方法：查实体 → 转 VO → 批量查审核记录 → 填充 → 返回

### 4.1 作品管理 — WorkServiceImpl.getAdminWorksPage()
- 返回类型：`IPage<Works>` → `IPage<AdminWorkVO>`
- content_type = `100`
- 批量查询 `ContentAuditRecordMapper.selectLatestByContentIds(100, workIds)`
- 转换并填充 AdminWorkVO

### 4.2 评论管理 — CommentServiceImpl.getCommentsPage()
- 返回类型：`IPage<Comments>` → `IPage<AdminCommentVO>`
- content_type = `200`

### 4.3 系列管理 — SeriesServiceImpl.getAdminSeriesPage()
- 返回类型：`IPage<Series>` → `IPage<AdminSeriesVO>`
- content_type = `300`

### 4.4 用户信息变更 — UserDataChangeLock 查询
- UserDataChangeLockVO 加字段后，查询时填充审核记录
- content_type = `400`

---

## 五、Controller 层适配（4 个 Controller）

- [ ] AdminWorksController → 返回 `ResponsePojo<IPage<AdminWorkVO>>`
- [ ] AdminCommentsController → 返回 `ResponsePojo<IPage<AdminCommentVO>>`
- [ ] AdminSeriesController → 返回 `ResponsePojo<IPage<AdminSeriesVO>>`
- [ ] AdminUserDataChangeController → UserDataChangeLockVO 已有新字段

---

## 六、ContentAuditRecordMapper 新增查询方法

```java
/**
 * 批量查询指定内容的最新审核记录
 * @param contentType 内容类型（100/200/300/400）
 * @param contentIds  内容 ID 列表
 * @return 每个 contentId 取最新一条（create_time DESC）
 */
List<ContentAuditRecord> selectLatestByContentIds(
    @Param("contentType") Integer contentType,
    @Param("contentIds") List<Integer> contentIds
);
```

对应 XML：子查询取每个 content_id 的最大 create_time，再 JOIN 回主表取完整记录。

---

## 七、可选扩展（不在本次范围内）

- [ ] 管理员查看完整审核历史（非仅最新一条）
- [ ] 违规统计仪表盘

---

## 改动文件总览

### 新建文件

| 文件 | 说明 |
|------|------|
| `pojo/entity/ContentAuditRecord.java` | 审核记录实体 |
| `mapper/ContentAuditRecordMapper.java` | Mapper 接口 + `selectLatestByContentIds` |
| `resources/mapper/ContentAuditRecordMapper.xml` | INSERT + 批量查询 SQL |
| `pojo/VO/admin/AdminWorkVO.java` | 作品管理 VO |
| `pojo/VO/admin/AdminCommentVO.java` | 评论管理 VO |
| `pojo/VO/admin/AdminSeriesVO.java` | 系列管理 VO |

### 修改文件

| 文件 | 改动内容 |
|------|----------|
| `service/Impl/WorkServiceImpl.java` | INSERT 审核记录 + 管理查询返回 AdminWorkVO |
| `service/Impl/CommentServiceImpl.java` | INSERT 审核记录 + 管理查询返回 AdminCommentVO |
| `service/Impl/SeriesServiceImpl.java` | INSERT 审核记录 + 管理查询返回 AdminSeriesVO |
| `service/Impl/UserServiceImpl.java` | INSERT 审核记录 + 管理查询填充 UserDataChangeLockVO |
| `controller/admin/AdminWorksController.java` | 返回类型改为 AdminWorkVO |
| `controller/admin/AdminCommentsController.java` | 返回类型改为 AdminCommentVO |
| `controller/admin/AdminSeriesController.java` | 返回类型改为 AdminSeriesVO |
| `controller/admin/AdminUserDataChangeController.java` | VO 已有新字段 |
| `pojo/VO/UserDataChangeLockVO.java` | + audit_reason, insult_words |
| `service/WorkService.java` | 接口返回类型同步更新 |
| `service/CommentService.java` | 接口返回类型同步更新 |
| `service/SeriesService.java` | 接口返回类型同步更新 |

---

**总计：6 新建 + 12 修改 = 18 个文件**

---

## 设计决策记录

1. **独立表而非各实体加字段** — 统一管理、审计历史可追溯、扩展零成本
2. **各 Service 直接注入 Mapper** — 不修改 ContentAuditService，单一职责
3. **approval_status 冗余存储** — 方便统计筛选，避免 JOIN 实体表
4. **insult_words 存 JSON 字符串** — 与项目 varchar 风格一致
5. **不设外键** — 多态关联（content_type + content_id）无法指向单一表
