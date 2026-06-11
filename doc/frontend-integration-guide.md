# 消息系统前端联调文档

## 一、基础信息

| 项目 | 说明 |
|------|------|
| **Base URL** | `/api/message` |
| **认证方式** | Header `Authorization: Bearer <token>` 或 URL 参数 `?token=xxx` |
| **响应格式** | JSON，统一使用 `ResponsePojo` 包装 |
| **WebSocket 地址** | `ws://localhost:9090/api/ws/notification?token=<token>` |

---

## 二、统一响应结构

```typescript
interface ResponsePojo<T> {
  code: number;      // 200 成功，其他失败
  message: string;   // 提示信息
  data: T;           // 业务数据
}
```

---

## 三、接口列表

### 3.1 获取未读消息数量

**请求**
```
GET /api/message/unread-count
```

**响应 `data` 结构**
```typescript
{
  total: number;      // 总未读数
  private: number;    // 未读私信数
  system: number;     // 未读系统通知数
}
```

**使用场景**
- 导航栏消息图标红点/数字显示
- 登录后初始化消息计数

---

### 3.2 查询会话列表

**请求**
```
GET /api/message/conversations/{current}/{size}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| current | Long | 页码，从 1 开始 |
| size | Long | 每页大小，1-500 |

**响应 `data` 结构（分页）**
```typescript
interface IPage<T> {
  records: T[];       // 数据列表
  total: number;      // 总记录数
  size: number;       // 每页大小
  current: number;    // 当前页码
  pages: number;      // 总页数
}

interface ConversationVO {
  other_user_id: number;        // 对方用户ID
  other_username: string;       // 对方用户名
  other_nickname: string;       // 对方昵称
  other_avatar_url: string;     // 对方头像
  last_message: string;         // 最后一条消息内容
  last_message_time: string;    // 最后消息时间（ISO 8601）
  unread_count: number;         // 未读消息数量
}
```

**使用场景**
- 私信页面的会话列表
- 按最后消息时间倒序排列

---

### 3.3 查询聊天记录

**请求**
```
GET /api/message/chat/{otherUserId}/{current}/{size}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| otherUserId | Integer | 对方用户ID |
| current | Long | 页码，从 1 开始 |
| size | Long | 每页大小，1-500 |

**响应 `data` 结构**
```typescript
interface MessageVO {
  message_id: number;           // 消息ID
  message: string;              // 消息内容
  project: string;              // 消息主题
  from_user_id: number;         // 发送者ID
  message_type: string;         // 消息类型：system/private
  ref_id: number | null;        // 关联实体ID
  to: number;                   // 接收者ID
  is_read: boolean;             // 是否已读
  is_delete: boolean;           // 是否删除
  create_time: string;          // 创建时间（ISO 8601）
  from_username: string;        // 发送者用户名
  from_nickname: string;        // 发送者昵称
  from_avatar_url: string;      // 发送者头像
}
```

**使用场景**
- 私信聊天页面
- 按时间倒序排列（最新的在前面）

---

### 3.4 查询系统通知

**请求**
```
GET /api/message/system/{current}/{size}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | Long | 是 | 页码，从 1 开始 |
| size | Long | 是 | 每页大小，1-500 |
| project | String | 否 | 消息主题筛选 |
| isRead | Boolean | 否 | 已读状态筛选 |

**可选的 `project` 值**
| 值 | 说明 |
|------|------|
| work_audit | 作品审核通知 |
| series_audit | 合集审核通知 |
| comment_audit | 评论审核通知 |
| system_notice | 系统公告 |
| account_notice | 账号通知 |
| like | 点赞通知 |
| star | 收藏通知 |
| comment | 评论通知 |

**响应 `data` 结构**
同 `MessageVO`（见 3.3）

**使用场景**
- 系统通知列表页面
- 支持按类型和已读状态筛选

---

### 3.5 发送私信

**请求**
```
POST /api/message/send
Content-Type: application/x-www-form-urlencoded
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| toUserId | Integer | 是 | 接收者用户ID |
| content | String | 是 | 消息内容 |

**响应 `data`**
```typescript
true  // 发送成功
```

**业务规则**
- 不能给自己发送私信
- 消息内容不能为空
- 消息会通过 WebSocket 实时推送给在线用户

---

### 3.6 标记会话已读

**请求**
```
POST /api/message/read/conversation/{otherUserId}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| otherUserId | Integer | 对方用户ID |

**响应 `data`**
```typescript
true  // 标记成功
```

**使用场景**
- 打开会话时自动调用
- 标记与该用户的所有未读私信为已读

---

### 3.7 全部标记已读

**请求**
```
POST /api/message/read-all
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messageType | String | 否 | 消息类型：system/private，不传则标记所有 |

**响应 `data`**
```typescript
true  // 标记成功
```

**使用场景**
- "全部已读"按钮

---

### 3.8 删除消息

**请求**
```
POST /api/message/delete/{messageId}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| messageId | Integer | 消息ID |

**响应 `data`**
```typescript
true  // 删除成功
```

**业务规则**
- 软删除，仅对自己可见
- 只能删除自己收到的消息

---

### 3.9 批量删除消息

**请求**
```
POST /api/message/batch-delete
Content-Type: application/json
```

**请求体**
```json
[1, 2, 3]  // 消息ID数组
```

**响应 `data`**
```typescript
true  // 删除成功
```

---

## 四、WebSocket 实时推送

### 4.1 连接地址

```
ws://localhost:9090/api/ws/notification?token={JWT_TOKEN}
```

### 4.2 连接流程

```javascript
// 1. 建立连接
const ws = new WebSocket(`ws://localhost:9090/api/ws/notification?token=${token}`);

// 2. 连接成功
ws.onopen = () => {
  console.log('WebSocket 连接成功');
  // 启动心跳
  startHeartbeat();
};

// 3. 接收消息
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type === 'notification') {
    // 处理新消息通知
    handleNewMessage(data.data);
  }
};

// 4. 连接关闭
ws.onclose = () => {
  console.log('WebSocket 连接关闭');
  // 重连逻辑
  setTimeout(() => reconnect(), 3000);
};

// 5. 错误处理
ws.onerror = (error) => {
  console.error('WebSocket 错误:', error);
};
```

### 4.3 心跳机制

```javascript
// 每 30 秒发送一次心跳
function startHeartbeat() {
  setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send('PING');
    }
  }, 30000);
}

// 收到 PONG 响应
ws.onmessage = (event) => {
  if (event.data === 'PONG') {
    // 心跳响应，连接正常
    return;
  }
  // 处理其他消息...
};
```

### 4.4 推送消息格式

```typescript
interface WebSocketNotification {
  type: 'notification';
  data: MessageVO;  // 同接口返回的 MessageVO 结构
}
```

**示例**
```json
{
  "type": "notification",
  "data": {
    "message_id": 123,
    "message": "赞了你的作品《风景画》",
    "project": "like",
    "from_user_id": 1002,
    "message_type": "system",
    "ref_id": 456,
    "to": 1001,
    "is_read": false,
    "is_delete": false,
    "create_time": "2026-06-11T10:30:00",
    "from_username": "zhang_san",
    "from_nickname": "张三",
    "from_avatar_url": "/avatar/xxx.png"
  }
}
```

---

## 五、联调流程建议

### 5.1 开发顺序

1. **获取未读消息数量** → 导航栏消息图标
2. **WebSocket 连接** → 实时通知基础
3. **查询系统通知** → 通知列表页面
4. **查询会话列表** → 私信列表页面
5. **查询聊天记录** → 聊天详情页面
6. **发送私信** → 发送消息功能
7. **标记已读/删除** → 消息操作功能

### 5.2 状态管理建议

```typescript
// 消息状态
interface MessageState {
  unreadCount: {
    total: number;
    private: number;
    system: number;
  };
  conversations: ConversationVO[];
  systemMessages: MessageVO[];
  wsConnected: boolean;
}
```

### 5.3 错误处理

| HTTP 状态码 | 说明 | 处理建议 |
|------------|------|----------|
| 200 | 成功 | 正常处理 |
| 401 | 未认证 | 跳转登录页 |
| 403 | 无权限 | 提示无权限 |
| 500 | 服务器错误 | 提示稍后重试 |

### 5.4 注意事项

1. **分页参数**：current 从 1 开始，不是 0
2. **时间格式**：接口返回 ISO 8601 格式，前端需要自行格式化显示
3. **头像路径**：返回的是相对路径，需要拼接基础 URL
4. **WebSocket 重连**：断开后需要实现自动重连机制
5. **未读计数同步**：收到 WebSocket 推送时，需要更新未读计数

---

## 六、测试数据

### 测试用户

| 用户ID | 用户名 | 说明 |
|--------|--------|------|
| 1 | admin | 管理员 |
| 2 | test_user | 测试用户 |

### 测试流程

1. 使用两个浏览器分别登录不同用户
2. 用户 A 向用户 B 发送私信
3. 用户 B 应实时收到 WebSocket 推送
4. 验证未读计数是否正确更新

---

## 七、Swagger 文档

在线 API 文档地址：`http://localhost:9090/doc.html`

可在此页面直接测试接口。
