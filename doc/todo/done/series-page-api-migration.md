# 系列分页查询接口迁移指南

## 一、变更概述

系列分页查询接口已更新，`userId` 从必填的路径参数改为可选的查询参数，支持查询所有用户的系列。

---

## 二、接口对比

| 项目 | 旧接口 | 新接口 |
|------|--------|--------|
| **路径** | `GET /api/work/series/page/{userId}/{current}/{size}` | `GET /api/work/series/page/{current}/{size}` |
| **认证** | 无需认证 | 无需认证 |
| **userId** | 路径参数，必填 | 查询参数，可选 |

---

## 三、参数变更

### 旧接口参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| userId | 路径 | Integer | 是 | 用户 ID |
| current | 路径 | Integer | 是 | 页码，从 1 开始 |
| size | 路径 | Integer | 是 | 每页数量，1-500 |
| keyword | 查询 | String | 否 | 搜索关键词 |

### 新接口参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| current | 路径 | Integer | 是 | 页码，从 1 开始 |
| size | 路径 | Integer | 是 | 每页数量，1-500 |
| userId | 查询 | Integer | 否 | 用户 ID，不传则查询所有用户 |
| keyword | 查询 | String | 否 | 搜索关键词 |

---

## 四、调用示例对比

### 旧接口调用

```javascript
// 查询指定用户的系列
const userId = 123;
const response = await fetch(`/api/work/series/page/${userId}/1/10`);

// 带关键词搜索
const response = await fetch(`/api/work/series/page/${userId}/1/10?keyword=风景`);
```

### 新接口调用

```javascript
// 查询指定用户的系列（通过查询参数）
const response = await fetch('/api/work/series/page/1/10?userId=123');

// 查询所有用户的系列（不传 userId）
const response = await fetch('/api/work/series/page/1/10');

// 带关键词搜索
const response = await fetch('/api/work/series/page/1/10?keyword=风景');

// 组合查询：指定用户 + 关键词
const response = await fetch('/api/work/series/page/1/10?userId=123&keyword=风景');
```

---

## 五、前端代码迁移

### 5.1 API 请求函数修改

**旧代码**
```javascript
async function getSeriesList(userId, current, size, keyword) {
  let url = `/api/work/series/page/${userId}/${current}/${size}`;
  if (keyword) {
    url += `?keyword=${encodeURIComponent(keyword)}`;
  }
  const response = await fetch(url);
  return response.json();
}
```

**新代码**
```javascript
async function getSeriesList(current, size, userId = null, keyword = null) {
  const params = new URLSearchParams();
  if (userId) {
    params.append('userId', userId);
  }
  if (keyword) {
    params.append('keyword', keyword);
  }
  
  const queryString = params.toString();
  let url = `/api/work/series/page/${current}/${size}`;
  if (queryString) {
    url += `?${queryString}`;
  }
  
  const response = await fetch(url);
  return response.json();
}
```

### 5.2 调用方式修改

**旧代码**
```javascript
// 查询指定用户的系列
getSeriesList(userId, 1, 10);

// 带搜索
getSeriesList(userId, 1, 10, '风景');
```

**新代码**
```javascript
// 查询指定用户的系列
getSeriesList(1, 10, userId);

// 查询所有用户的系列
getSeriesList(1, 10);

// 带搜索
getSeriesList(1, 10, null, '风景');

// 指定用户 + 搜索
getSeriesList(1, 10, userId, '风景');
```

### 5.3 TypeScript 类型定义

```typescript
interface SeriesListParams {
  current: number;      // 页码，从 1 开始
  size: number;         // 每页数量，1-500
  userId?: number;      // 用户 ID（可选）
  keyword?: string;     // 搜索关键词（可选）
}

interface Series {
  series_id: number;
  user_id: number;
  series_title: string;
  about_text: string;
  thumb_url: string | null;  // 封面缩略图，无作品时为 null
  create_time: string;       // ISO 8601 格式
}

interface IPage<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// API 函数
async function getSeriesList(params: SeriesListParams): Promise<IPage<Series>> {
  const { current, size, userId, keyword } = params;
  
  const searchParams = new URLSearchParams();
  if (userId) searchParams.append('userId', String(userId));
  if (keyword) searchParams.append('keyword', keyword);
  
  const queryString = searchParams.toString();
  const url = `/api/work/series/page/${current}/${size}${queryString ? '?' + queryString : ''}`;
  
  const response = await fetch(url);
  return response.json();
}
```

---

## 六、响应结构（未变更）

响应结构保持不变，返回 `ResponsePojo<IPage<Series>>`：

```typescript
interface ResponsePojo<T> {
  code: number;      // 200 成功
  message: string;   // 提示信息
  data: T;           // 业务数据
}
```

### Series 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| series_id | Integer | 系列 ID |
| user_id | Integer | 用户 ID |
| series_title | String | 系列标题（最多 16 个中文字符） |
| about_text | String | 系列描述（最多 24 个中文字符） |
| thumb_url | String / null | 封面缩略图文件名，无作品时为 null |
| create_time | String | 创建时间（ISO 8601 格式） |

### 封面图片访问

```
/api/image/works?filePath={thumb_url}
```

---

## 七、迁移检查清单

- [ ] 修改 API 请求路径，移除路径中的 `{userId}`
- [ ] 将 `userId` 改为查询参数
- [ ] 处理 `userId` 为可选的情况（查询所有用户）
- [ ] 更新相关 TypeScript 类型定义
- [ ] 测试查询指定用户的系列
- [ ] 测试查询所有用户的系列
- [ ] 测试带关键词搜索的场景

---

## 八、注意事项

1. **分页参数**：`current` 从 1 开始，不是 0
2. **userId 可选**：不传时查询所有审核通过的系列
3. **排序规则**：有关键词时标题匹配优先，否则按创建时间倒序
4. **数据过滤**：只返回审核通过（approval_status=10）且未删除的系列
5. **封面取值**：取自系列内最新发布且审核通过的作品

---

## 九、Swagger 文档

在线 API 文档地址：`http://localhost:9090/doc.html`

可在页面中搜索 "分页查询作品系列" 查看详细接口说明并进行测试。
