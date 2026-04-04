---
name: pixvision-user-module
description: 用户管理模块开发指南，涵盖注册、登录、密码修改、分页查询等功能
---

# 用户管理模块开发技能

## 模块概述

用户管理模块提供完整的用户生命周期管理，包括注册、登录、登出、密码修改和分页查询。

## 核心文件

| 文件 | 路径 |
|------|------|
| Controller | `controller/UserController.java` |
| Service 接口 | `service/UserService.java` |
| Service 实现 | `service/Impl/UserServiceImpl.java` |
| Mapper | `mapper/UserMapper.java` |
| Mapper XML | `resources/mapper/UserMapper.xml` |
| 用户实体 | `pojo/userPojo/User.java` |
| 登录实体 | `pojo/userPojo/UserLogin.java` |

## 接口清单

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 注册 | POST | `/api/user/register` | 用户注册 |
| 登录 | POST | `/api/user/login` | 用户登录 |
| 登出 | POST | `/api/user/logout` | 用户登出 |
| 修改密码 | POST | `/api/user/changepassword` | 修改密码 |
| 分页查询 | GET | `/api/user/page/{current}/{size}` | 分页查询用户 |

## 数据库表

**表名**: `tb_user`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键 |
| user_uuid | BINARY(16) | 用户 UUID |
| username | VARCHAR | 用户名 |
| password | VARCHAR | 密码 (SHA-256) |
| nickname | VARCHAR | 昵称 |
| email | VARCHAR | 邮箱 |
| avatar | VARCHAR | 头像 URL |
| role | INT | 角色 (11=普通用户) |
| status | INT | 状态 (10=正常) |
| is_delete | BOOLEAN | 逻辑删除标记 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 核心业务逻辑

### 注册流程
1. 校验用户名格式 (6-16 位字母/数字/下划线)
2. 校验邮箱格式（如提供）
3. 校验验证码（如提供）
4. 检查用户名是否已存在
5. 生成 UUID 并转换为 BINARY(16)
6. 密码使用 SHA-256 加密
7. 设置默认角色 (11) 和状态 (10)
8. 分配随机头像
9. 插入数据库

### 登录流程
1. 校验用户名/邮箱格式
2. 验证邮箱验证码
3. 查询用户（支持用户名或邮箱）
4. 验证密码
5. 检查用户状态
6. 生成 JWT Token
7. 将 Token 加入白名单
8. 返回用户信息和 Token

### 修改密码流程
1. 从 Token 获取当前用户
2. 验证邮箱验证码
3. 验证旧密码
4. 加密新密码
5. 更新密码
6. 使当前 Token 失效

## 工具类使用

| 工具类 | 用途 |
|--------|------|
| `RegexUtils` | 用户名、邮箱格式校验 |
| `StrSwitchUtils` | UUID 转换、密码哈希 |
| `JWTUtils` | Token 生成和解析 |
