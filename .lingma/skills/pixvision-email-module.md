---
name: pixvision-email-module
description: 邮件验证码模块开发指南，涵盖验证码生成、存储、验证和邮件模板渲染
---

# 邮件验证码模块开发技能

## 模块概述

邮件验证码模块提供验证码生成、Redis 存储、邮件发送和模板渲染功能，用于用户注册、登录、密码修改等场景的二次验证。

## 核心文件

| 文件 | 路径 |
|------|------|
| Controller | `controller/MailController.java` |
| 邮件服务 | `service/EmailService.java` → `service/Impl/EmailServiceImpl.java` |
| 模板服务 | `service/EmailTemplateService.java` → `service/Impl/EmailTemplateServiceImpl.java` |
| 验证码服务 | `service/VerificationCodeServices.java` → `service/Impl/VerificationCodeServicesImpl.java` |
| 邮件配置 | `config/EmailConfig.java` |
| 邮件模板 | `resources/template/email-html/email-verification.html` |

## 接口清单

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 发送验证码 | POST | `/api/mail/send-email-code` | 发送邮箱验证码 |
| 验证码测试 | POST | `/api/mail/verify-email-code-test` | 测试验证码（无需登录） |

## 验证码机制

### 生成规则
- 长度：6 位
- 字符集：数字 (1-9) + 大写字母 (A-Z，排除 O)
- 示例：`A3F7K9`

### 存储机制
- **存储位置**: Redis
- **Key 格式**: `userEmailCode:{SHA256(email)}`
- **Value**: `SHA256(验证码)`（哈希存储）
- **过期时间**: 5 分钟

### 验证流程
1. 对用户输入的验证码进行 SHA256 哈希
2. 对邮箱进行 SHA256 哈希，构建 Key
3. 从 Redis 获取存储的哈希值
4. 比对两个哈希值
5. 验证成功后删除缓存（一次性使用）

## 邮件模板系统

### 模板占位符

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `{{username}}` | 用户名 | `dev_user` |
| `{{email_text}}` | 邮件用途说明 | `注册验证` |
| `{{code}}` | 验证码 | `A3F7K9` |
| `{{expireTime}}` | 过期时间（分钟） | `5` |
| `{{year}}` | 当前年份 | `2026` |
| `{{systemName}}` | 系统名称 | `Pixie Vision` |
| `{{logoUriLight}}` | 浅色 Logo (Base64) | `data:image/png;base64,...` |
| `{{logoUriDark}}` | 深色 Logo (Base64) | `data:image/png;base64,...` |

### Logo 缓存
- 使用 `ConcurrentHashMap` 缓存 Logo 的 Base64 编码
- 避免重复读取和转换图片
- 可通过 `clearLogoCache()` 清除缓存

## 邮件发送

### 配置项 (`EmailConfig`)
- `from`: 发件人邮箱
- SMTP 主机、端口、SSL 配置

### 发送方式
```java
// 发送 HTML 邮件
emailService.sendEMail(to, subject, htmlContent);
```

## 使用场景

| 场景 | 说明 |
|------|------|
| 用户注册 | 验证邮箱归属 |
| 用户登录 | 二次身份验证 |
| 修改密码 | 敏感操作确认 |

## 注意事项

- 验证码为一次性使用，验证后即失效
- 邮箱和验证码均以 SHA256 哈希存储，保护隐私
- 模板中的 Logo 使用 Base64 内嵌，避免外部依赖
