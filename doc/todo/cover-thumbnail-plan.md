# 作品封面图（缩略图）预生成方案（V2）

## 概述

上传作品时同步生成 400px 长边的 **WebP 封面缩略图**，数据库新增 `thumb_url` 字段存储封面文件名。
前端无需推算，直接从 API 响应中拿到封面 URL。封面与原图共享相同的状态后缀生命周期。

### 命名规则

```
原图:     uuid.png   →  封面:  uuid_thumb.webp
原图:     uuid.jpg   →  封面:  uuid_thumb.webp
原图:     uuid.jpeg  →  封面:  uuid_thumb.webp

带状态后缀（封面与原图保持相同后缀）:
原图:     uuid.png.pend  →  封面:  uuid_thumb.webp.pend
原图:     uuid.png.fail  →  封面:  uuid_thumb.webp.fail
原图:     uuid.png.del   →  封面:  uuid_thumb.webp.del
```

**推算公式**：`img_url.replaceFirst("\.(png|jpg|jpeg)$", "_thumb.webp")`

### 封面参数

| 参数 | 值 |
|------|-----|
| 长边 | 400px |
| 格式 | WebP（需添加 `webp-imageio` 依赖） |
| 质量 | 80 |
| 预估大小 | 12-25 KB |

---

## 涉及文件清单

### 全部修改文件（11 个）

| # | 文件 | 层级 | 修改量 |
|---|------|------|--------|
| 1 | `sql/db_pix_vision-V2.2.sql` | 数据库 | 新增 DDL |
| 2 | `pom.xml` | 依赖 | 新增 `webp-imageio` |
| 3 | `Works.java` | 实体 | 新增 `thumb_url` 字段 |
| 4 | `WorksMapper.java` | Mapper 接口 | `updateWorkInfo` 加参数 |
| 5 | `WorksMapper.xml` | Mapper XML | 6 处加 `thumb_url` |
| 6 | `HistoryMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 7 | `LikesMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 8 | `StarsMapper.xml` | Mapper XML | 1 处加 `thumb_url` |
| 9 | `ImageUtils.java` | 工具类 | 新增 `resizeToWebp()` |
| 10 | `WorkServiceImpl.java` | Service | 新增 1 方法 + 修改 6 方法 |
| 11 | `ImageController.java` | Controller | 白名单加 `"webp"` |

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
    COMMENT '封面缩略图文件名，从 img_url 派生，格式为 uuid_thumb.webp';
```

- 默认值 `NULL`（兼容存量数据，旧作品无封面）
- 与 `img_url` 相同长度（`VARCHAR(100)`）

---

## 二、依赖层

### 文件 2：`pom.xml`

新增 WebP 编码依赖，使 `ImageIO.write()` 支持 WebP 格式输出：

```xml
<!-- WebP 图像编码支持 -->
<dependency>
    <groupId>org.sejda.imageio</groupId>
    <artifactId>webp-imageio</artifactId>
    <version>0.1.6</version>
</dependency>
```

---

## 三、实体层

### 文件 3：`Works.java`

**行号**：第 16 行 `img_url` 字段下方

**新增字段**：
```java
@Schema(description = "封面缩略图文件名")
String thumb_url;
```

> `History extends Works`，自动继承 `thumb_url`，无需修改 History.java

---

## 四、Mapper 接口层

### 文件 4：`WorksMapper.java`

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

## 五、Mapper XML 层（4 个文件）

### 文件 5：`WorksMapper.xml`

共 6 处修改：

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 10 行 | `resultMap` | 新增 `<result column="thumb_url" property="thumb_url"/>` |
| B | 第 28 行 | `Base_Column_List` | 在 `img_url` 后加 `, thumb_url` |
| C | 第 36 行 | `selectHomepageWorks` SELECT | 加 `w.thumb_url,` |
| D | 第 128 行 | `updateWorkInfo` 动态 SET | 新增：`<if test="thumbUrl != null">thumb_url = #{thumbUrl},</if>` |
| E | 第 188 行 | `selectMyWorks` SELECT | 加 `w.thumb_url,` |
| F | — | `adminSelectWorks` | 使用 `Base_Column_List`，自动覆盖 |

### 文件 6：`HistoryMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 16 行 | `selectUserHistory` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

### 文件 7：`LikesMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 42 行 | `selectUserLikedWorks` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

### 文件 8：`StarsMapper.xml`

| # | 行号 | 位置 | 修改 |
|---|------|------|------|
| A | 第 42 行 | `selectUserStarredWorks` SELECT | 在 `w.img_url,` 后加 `w.thumb_url,` |

---

## 六、工具类层

### 文件 9：`ImageUtils.java`

**新增方法**：`resizeToWebp(byte[] imageBytes, int targetLongEdge, float quality)`

```java
/**
 * 图像缩放并输出为 WebP 格式
 * <p>
 * 按长边等比缩放，输出 WebP 格式，适用于封面缩略图生成。
 *
 * @param imageBytes      原始图像字节数组
 * @param targetLongEdge  目标长边像素（宽或高中较大者）
 * @param quality         输出质量（0.0 ~ 1.0），推荐 0.8
 * @return WebP 格式的图像字节数组
 */
public static byte[] resizeToWebp(byte[] imageBytes, int targetLongEdge, float quality) {
    // 1. 读取原图 → BufferedImage
    // 2. 计算等比缩放尺寸（按长边）
    // 3. Graphics2D 双三次插值缩放
    // 4. ImageIO.write(bufferedImage, "webp", outputStream) — 需要 webp-imageio 依赖
    // 5. 返回字节数组
}
```

> 依赖 `webp-imageio` 的 SPI 机制，`ImageIO.getWriterFormatNames()` 自动包含 `"webp"`。

---

## 七、Service 层

### 文件 10：`WorkServiceImpl.java`

#### 10.1 新增辅助方法

**方法名**：`thumbFileName(String imgUrl)`

```
输入 → 输出:
"a1b2c3d4.png"   → "a1b2c3d4_thumb.webp"
"a1b2c3d4.jpg"   → "a1b2c3d4_thumb.webp"
"a1b2c3d4.jpeg"  → "a1b2c3d4_thumb.webp"
null              → null
```

```java
private String thumbFileName(String imgUrl) {
    if (imgUrl == null) return null;
    return imgUrl.replaceFirst("\\.(png|jpg|jpeg)$", "_thumb.webp");
}
```

---

#### 10.2 `uploadWork()` — 上传时同步生成封面

- **行号**：约第 301-345 行
- **修改位置**：步骤 10 保存原图后，步骤 11 构建 Works 对象

**修改内容**：
1. 原图保存后，调用 `ImageUtils.resizeToWebp(fileBytes, 400, 0.8f)` 生成封面
2. 保存封面文件（相同状态后缀）
3. `works.setThumb_url(thumbFileName(uniqueFileName))` 写入数据库

---

#### 10.3 `updateWork()` — 编辑作品更换图片

- **行号**：约第 560-773 行
- **修改位置**：步骤 6 保存新文件后 + 步骤 7 删除旧文件时 + 步骤 763 `updateWorkInfo` 调用

**修改内容**：
1. 新原图保存后 → 生成新封面 → 保存为 `.pend`
2. 删除旧原图时 → 同步删除旧封面（调用 `deleteOldImageFile`）
3. `worksMapper.updateWorkInfo()` 传入 `newThumbUrl` 参数

---

#### 10.4 `batchDeleteWorks()` — 用户删除

- **行号**：约第 131-158 行
- **修改**：重命名原图时，同步重命名封面为 `.del`

---

#### 10.5 `adminBatchDeleteWorks()` — 管理员删除

- **行号**：约第 994-1021 行
- **修改**：与 10.4 相同

---

#### 10.6 `batchUpdateApprovalStatus()` — 审核状态变更

- **行号**：约第 860-898 行
- **修改**：重命名原图时，同步重命名封面

---

## 八、Controller 层

### 文件 11：`ImageController.java`

**行号**：第 65-67 行 `ALLOWED_EXTENSIONS`

**修改**：`"jpg", "jpeg", "png"` → `"jpg", "jpeg", "png", "webp"`

> `getContentType()` 已有 `webp → image/webp` 映射（第 772-773 行），`getRealImageExtension()` 自动剥离状态后缀后得到 `webp`，无需额外修改。

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
// 后端返回: work.thumb_url = "a1b2c3d4_thumb.webp"
// 直接使用，无需推算:
const coverUrl = `/api/image/work/get?filePath=${work.thumb_url}`;
```

### 封面图片访问

```
公开: GET /api/image/work/get?filePath=a1b2c3d4_thumb.webp
管理: GET /api/image/work/admin-view?filePath=a1b2c3d4_thumb.webp
```

`resolveWorkFilePath()` / `resolveAdminWorkFilePath()` 自动查找 `.pend` / `.fail` / `.del` 后缀的封面文件。

---

## 文件生命周期总览

```
上传:
  uuid.png.pend          ← 原图（待审核）
  uuid_thumb.webp.pend   ← 封面（待审核）
  DB: img_url=uuid.png, thumb_url=uuid_thumb.webp

审核通过 (approval_status: 20→10):
  uuid.png               ← 原图（正常）
  uuid_thumb.webp        ← 封面（正常）

审核不通过 (approval_status: 20→30):
  uuid.png.fail          ← 原图（未过审）
  uuid_thumb.webp.fail   ← 封面（未过审）

用户/管理员删除:
  uuid.png.del           ← 原图（已删除）
  uuid_thumb.webp.del    ← 封面（已删除）

编辑作品更换图片:
  旧原图/旧封面 → 重命名为 .del
  新原图/新封面 → 保存为 .pend
  DB: img_url + thumb_url 同步更新
```

