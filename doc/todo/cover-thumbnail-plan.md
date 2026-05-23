# 作品封面图（缩略图）预生成方案（V2）

## 概述

上传作品时同步生成 400px 短边的 **JPG 封面缩略图**，数据库新增 `thumb_url` 字段存储封面文件名。
前端无需推算，直接从 API 响应中拿到封面 URL。封面与原图共享相同的状态后缀生命周期。

### 命名规则

```
原图:     uuid.png   →  封面:  uuid_thumb.jpg
原图:     uuid.jpg   →  封面:  uuid_thumb.jpg
原图:     uuid.jpeg  →  封面:  uuid_thumb.jpg

带状态后缀（封面与原图保持相同后缀）:
原图:     uuid.png.pend  →  封面:  uuid_thumb.jpg.pend
原图:     uuid.png.fail  →  封面:  uuid_thumb.jpg.fail
原图:     uuid.png.del   →  封面:  uuid_thumb.jpg.del
```

**推算公式**：`img_url.replaceFirst("\.(png|jpg|jpeg)$", "_thumb.jpg")`

### 封面参数

| 参数 | 值 |
|------|-----|
| 短边 | 400px |
| 格式 | JPG（Java 内置支持，无需额外依赖） |
| 质量 | Java 默认 JPG 编码质量 |
| 预估大小 | 12-25 KB |

### 核心设计原则

- **封面是辅助优化功能**，不应影响作品上传/编辑/删除等核心流程
- 封面生成失败时降级：`thumb_url` 设为 `NULL`，前端使用原图
- 封面文件操作失败静默跳过（存量作品无封面文件）

---

## 涉及文件清单

### 全部修改文件（10 个）

| # | 文件 | 层级 | 修改量 |
|---|------|------|--------|
| 1 | `sql/db_pix_vision-V2.2.sql` | 数据库 | 新增 DDL |
| 2 | `Works.java` | 实体 | 新增 `thumb_url` 字段 |
| 3 | `WorksMapper.java` | Mapper 接口 | `updateWorkInfo` 加参数 |
| 4 | `WorksMapper.xml` | Mapper XML | 6 处加 `thumb_url` |
| 5 | `HistoryMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 6 | `LikesMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 7 | `StarsMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 8 | `ImageUtils.java` | 工具类 | 已有 `generateThumbnail()` |
| 9 | `WorkServiceImpl.java` | Service | 新增 1 方法 + 修改 6 方法 |
| 10 | `ImageController.java` | Controller | 已支持 JPG 访问，无需修改 |

### 无需修改

| 文件 | 原因 |
|------|------|
| `AdminWorksController.java` | 返回 Works 对象，继承新增字段 |
| `WorkController.java` | 同上 |
| `HistoryController.java` | 返回 History（继承 Works），XML 已改 |
| `LikeController.java` | 返回 Works 对象，XML 已改 |
| `StarController.java` | 返回 Works 对象，XML 已改 |
| `CommentResponseVo.java` | 评论不涉及作品图片字段 |

---

## 一、数据库层

### 文件 1：`sql/db_pix_vision-V2.2.sql`（新建）

```sql
ALTER TABLE tb_works
    ADD COLUMN thumb_url VARCHAR(100) DEFAULT NULL
    COMMENT '封面缩略图文件名，从 img_url 派生，格式为 uuid_thumb.jpg';
```

- 默认值 `NULL`（兼容存量数据，旧作品无封面）
- 与 `img_url` 相同长度（`VARCHAR(100)`）

---

## 二、实体层

### 文件 2：`Works.java`

**行号**：第 16 行 `img_url` 字段下方

**新增字段**：
```java
@Schema(description = "封面缩略图文件名")
String thumb_url;
```

> `History extends Works`，自动继承 `thumb_url`，无需修改 History.java

---

## 三、Mapper 接口层

### 文件 3：`WorksMapper.java`

**行号**：第 91-100 行 `updateWorkInfo()` 方法

**修改**：新增 `@Param("thumbUrl") String thumbUrl` 参数（在 `imgUrl` 之后）

```java
int updateWorkInfo(
    @Param("workId") Integer workId,
    @Param("userId") Integer userId,
    @Param("workTitle") String workTitle,
    @Param("imgUrl") String imgUrl,
    @Param("thumbUrl") String thumbUrl,       // [新增]
    @Param("seriesId") Integer seriesId,
    @Param("shouldUpdateSeries") Boolean shouldUpdateSeries,
    @Param("isOriginal") Boolean isOriginal,
    @Param("outUrl") String outUrl
);
```

---

## 四、Mapper XML 层（4 个文件）

### 文件 4：`WorksMapper.xml`

共 6 处修改：

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 10 行 | `resultMap` | 新增 `<result column="thumb_url" property="thumb_url"/>` |
| B | 第 28 行 | `Base_Column_List` | 在 `img_url` 后加 `, thumb_url` |
| C | 第 36 行 | `selectHomepageWorks` SELECT | 加 `w.thumb_url,` |
| D | 第 128 行 | `updateWorkInfo` 动态 SET | 新增：`<if test="thumbUrl != null">thumb_url = #{thumbUrl},</if>` |
| E | 第 188 行 | `selectMyWorks` SELECT | 加 `w.thumb_url,` |
| F | — | `adminSelectWorks` | 使用 `Base_Column_List`，自动覆盖 |

### 文件 5：`HistoryMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 16 行 | `selectUserHistory` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

### 文件 6：`LikesMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 42 行 | `selectUserLikedWorks` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

### 文件 7：`StarsMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 42 行 | `selectUserStarredWorks` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

---

## 五、工具类层

### 文件 8：`ImageUtils.java`

**已有方法**：`generateThumbnail(byte[] imageBytes, int targetSize)`

```java
/**
 * 智能生成封面缩略图
 * <p>
 * 自动判断图像宽高方向，以较短边为基准缩放至目标尺寸，保持宽高比。
 * 横图约束高度，竖图约束宽度，输出为 JPG 格式。
 * 若原图尺寸已小于等于目标尺寸，则直接返回 JPG 编码。
 *
 * @param imageBytes 原始图像字节数组
 * @param targetSize 目标尺寸（较短边的像素值），如 400 表示短边不超过 400px
 * @return JPG 格式的缩略图字节数组
 */
public static byte[] generateThumbnail(byte[] imageBytes, int targetSize) {
    // 1. getImageWidth/getImageHeight 获取原始尺寸
    // 2. 判断横图/竖图，以较短边为基准调用 resizeImage 缩放
    // 3. encodeToFormat 转为 JPG（含透明→白色填充处理）
    // 4. 返回 JPG 字节数组
}
```

> 内部复用 `getImageWidth`、`getImageHeight`、`resizeImage`、`encodeToFormat`、`readImage` 等已封装方法。纯内存操作，不产生临时文件。

---

## 六、Service 层

### 文件 9：`WorkServiceImpl.java`

#### 6.1 新增辅助方法

**方法名**：`thumbFileName(String imgUrl)`

```
输入 → 输出:
"a1b2c3d4.png"   → "a1b2c3d4_thumb.jpg"
"a1b2c3d4.jpg"   → "a1b2c3d4_thumb.jpg"
"a1b2c3d4.jpeg"  → "a1b2c3d4_thumb.jpg"
null              → null
```

```java
private String thumbFileName(String imgUrl) {
    if (imgUrl == null) return null;
    return imgUrl.replaceFirst("\\.(png|jpg|jpeg)$", "_thumb.jpg");
}
```

---

#### 6.2 `uploadWork()` — 上传时同步生成封面

- **行号**：约第 301-345 行
- **修改位置**：步骤 10 保存原图后，步骤 11 构建 Works 对象

**修改内容**：
1. 原图保存后，调用 `ImageUtils.generateThumbnail(fileBytes, 400)` 生成 JPG 封面
2. 封面文件名 = `thumbFileName(uniqueFileName)`（如 `uuid_thumb.jpg`）
3. 封面文件与原图保存到同一目录，保持相同的状态后缀（如 `.pend`）
4. `works.setThumb_url(thumbFileName(uniqueFileName))` 写入数据库

**伪代码**：
```java
// 保存原图（现有逻辑）
String uniqueFileName = UUID + ".png";
String savePath = workDirPath + uniqueFileName;
ImageUtils.saveImageToFile(fileBytes, savePath);

// [新增] 生成并保存封面
try {
    byte[] thumbBytes = ImageUtils.generateThumbnail(fileBytes, 400);
    String thumbName = thumbFileName(uniqueFileName); // uuid_thumb.jpg
    // 封面与原图同一目录，状态后缀保持一致
    FileUtil.writeBytes(thumbBytes, workDirPath + thumbName + ".pend");
    works.setThumb_url(thumbName);
} catch (Exception e) {
    log.error("封面生成失败，thumb_url 设为 NULL", e);
    works.setThumb_url(null); // 降级：不阻塞上传
}
```

---

#### 6.3 `updateWork()` — 编辑作品更换图片

- **行号**：约第 560-773 行
- **修改位置**：步骤 6 保存新文件后 + 步骤 7 删除旧文件时 + 步骤 763 `updateWorkInfo` 调用

**修改内容**：
1. 新原图保存后 → 生成新封面 → 封面文件保存为 `.pend`（逻辑与 uploadWork 相同）
2. 删除旧原图时 → 同步处理旧封面：
   - 根据旧 `img_url` 用 `thumbFileName()` 推导旧封面文件名
   - 旧原图重命名为 `.del` → 旧封面同步重命名为 `.del`
   - 旧封面文件不存在时静默跳过（存量作品无封面）
3. `worksMapper.updateWorkInfo()` 额外传入 `newThumbUrl` 参数

**文件清理伪代码**：
```java
// 删除/重命名旧文件（需扩展为同时处理封面）
String oldImgUrl = existingWork.getImg_url();
String oldThumbUrl = thumbFileName(oldImgUrl); // 推导旧封面文件名

// 重命名旧原图为 .del（现有逻辑）
renameToDel(workDirPath + oldImgUrl);

// [新增] 重命名旧封面为 .del
if (oldThumbUrl != null) {
    try {
        renameToDel(workDirPath + oldThumbUrl);
    } catch (Exception e) {
        log.warn("旧封面文件清理失败，跳过", e);
    }
}
```

---

#### 6.4 `batchDeleteWorks()` — 用户删除

- **行号**：约第 131-158 行
- **修改**：重命名原图片时，同步重命名封面为 `.del`
  - 遍历 `workIds` 查询对应 Works 记录
  - 从 `img_url` 用 `thumbFileName()` 推导 `thumb_url`
  - 原图重命名为 `.del` → 封面文件同步重命名
  - 封面文件不存在时静默跳过（存量作品无封面）

---

#### 6.5 `adminBatchDeleteWorks()` — 管理员删除

- **行号**：约第 994-1021 行
- **修改**：与 6.4 相同，重命名原图时同步重命名封面为 `.del`

---

#### 6.6 `batchUpdateApprovalStatus()` — 审核状态变更

- **行号**：约第 860-898 行
- **修改**：重命名原图时，同步重命名封面
  - 审核通过（30→10 或 20→10）：去掉 `.fail` / `.pend` 后缀，封面同步去后缀
  - 审核不通过（20→30）：`.pend` → `.fail`，封面同步改后缀
  - 从 `img_url` 用 `thumbFileName()` 推导封面文件名
  - 封面文件不存在时静默跳过

---

#### 6.7 错误处理与降级策略

封面生成失败时**不应阻塞核心业务流程**，采用优雅降级：

| 场景 | 处理方式 |
|------|----------|
| `generateThumbnail()` 抛出异常 | 捕获异常，`log.error` 记录，`thumb_url` 设为 `NULL`，继续后续流程 |
| 封面文件写入失败 | 同上，`thumb_url` 设为 `NULL` |
| 旧封面文件清理失败（删除/重命名） | 仅 `log.warn`，不阻塞新文件保存 |
| 审核状态变更时封面文件操作失败 | 仅 `log.warn`，继续主流程 |

---

## 七、Controller 层

### 文件 10：`ImageController.java`

**已有支持**：`ImageController` 中 `ALLOWED_EXTENSIONS` 已包含 `"jpg", "jpeg", "png"`，JPG 封面直接可用，无需新增白名单。

> `getContentType()` 已有 `jpg/jpeg → image/jpeg` 映射，`getRealImageExtension()` 自动剥离状态后缀后得到 `jpg`，无需额外修改。

---

## 涉及 Controller 接口（无需修改接口签名）

以下接口返回的 Works 对象自动包含 `thumb_url` 字段，**前端直接读取，无需推算**：

| 接口 | 返回对象 |
|------|----------|
| `GET /api/work/page/{current}/{size}` | `IPage<Works>` |
| `GET /api/work/detail/{workId}` | `Works` |
| `GET /api/admin/works/page/{current}/{size}` | `IPage<Works>` |
| `GET /api/work/my-works/{current}/{size}` | `IPage<Works>` |
| `GET /api/user/history/{current}/{size}` | `IPage<History>`（继承 Works） |
| `GET /api/like/list/{current}/{size}` | `IPage<Works>` |
| `GET /api/star/list/{current}/{size}` | `IPage<Works>` |

### 前端使用方式

```javascript
// 后端返回: work.thumb_url = "a1b2c3d4_thumb.jpg"
// 直接使用，无需推算:
const coverUrl = `/api/image/work/get?filePath=${work.thumb_url}`;
```

### 存量数据兼容

旧作品（V2.2 之前上传）`thumb_url` 字段为 `NULL`，前端需做降级判断：

```javascript
// 封面降级策略：有 thumb_url 用封面，否则用原图
const displayUrl = work.thumb_url
    ? `/api/image/work/get?filePath=${work.thumb_url}`
    : `/api/image/work/get?filePath=${work.img_url}`;
```

| 场景 | thumb_url | 显示行为 |
|------|-----------|----------|
| 新上传作品 | `uuid_thumb.jpg` | 400px JPG 封面（12-25KB） |
| 旧作品（存量） | `NULL` | 降级使用原图 |
| 封面生成失败 | `NULL` | 降级使用原图 |

> 后续版本可考虑后台任务批量生成存量封面，本版本不做强制要求。

### 封面图片访问

```
公开: GET /api/image/work/get?filePath=a1b2c3d4_thumb.jpg
管理: GET /api/image/work/admin-view?filePath=a1b2c3d4_thumb.jpg
```

`resolveWorkFilePath()` / `resolveAdminWorkFilePath()` 自动查找 `.pend` / `.fail` / `.del` 后缀的封面文件。
- 封面文件的路径查找逻辑与原图完全一致，共享同一套状态后缀匹配机制
- 封面文件不存在时返回 404，不会影响原图访问

---

## 文件生命周期总览

```
上传:
  uuid.png.pend          ← 原图（待审核）
  uuid_thumb.jpg.pend   ← 封面（待审核）
  DB: img_url=uuid.png, thumb_url=uuid_thumb.jpg

审核通过 (approval_status: 20→10):
  uuid.png               ← 原图（正常）
  uuid_thumb.jpg        ← 封面（正常）

审核不通过 (approval_status: 20→30):
  uuid.png.fail          ← 原图（未过审）
  uuid_thumb.jpg.fail   ← 封面（未过审）

用户/管理员删除:
  uuid.png.del           ← 原图（已删除）
  uuid_thumb.jpg.del    ← 封面（已删除）

编辑作品更换图片:
  旧原图/旧封面 → 重命名为 .del
  新原图/新封面 → 保存为 .pend
  DB: img_url + thumb_url 同步更新
```

> 关键约束：任何状态变更操作（审核通过/不通过、删除、编辑换图），必须同时处理原图和封面两个文件。封面文件操作失败不应阻塞主流程，仅记录 warn 日志。

