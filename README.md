# PixVision Server

**像素视觉后端服务** - 数字艺术创作与分享平台

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/Version-DEV--2.0.0-purple)](https://github.com/Ender-g/PixVisionServer)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 项目简介

PixVision 是一个现代化的数字艺术平台后端服务，提供用户管理、作品管理、互动评论等功能。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.3.0 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| Redis | 8.0 |

## 核心功能

- **用户系统**：注册、登录、资料管理、角色权限
- **安全认证**：JWT Token 白名单、RSA+AES 混合加密、邮箱验证码
- **作品管理**：上传、编辑、删除、审核
- **互动功能**：评论、点赞、收藏
- **系统监控**：JVM、CPU、内存信息

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 8.0+

### 配置

编辑 `~/.pix_vision/application.yml`：

```yaml
server:
  port: 9090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_pix_vision
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your_auth_code
```

### 运行

```bash
mvn spring-boot:run
```

### 访问

| 服务 | 地址 |
|------|------|
| 首页 | http://localhost:9090 |
| API 文档 | http://localhost:9090/doc.html |
| 健康检查 | http://localhost:9090/health |
| 系统信息 | http://localhost:9090/system-info |

## 项目结构

```
PixVisionServer/
├── src/main/java/top/playereg/pix_vision/
│   ├── config/          # 配置类
│   ├── controller/       # REST API
│   ├── service/         # 业务逻辑
│   ├── mapper/          # 数据访问
│   ├── pojo/            # 实体类
│   ├── handler/         # 拦截器
│   └── util/            # 工具类
├── src/main/resources/   # 配置资源
└── sql/                 # 数据库脚本
```

## 数据库

初始化脚本：`sql/db_pix_vision-V1.1.sql`

主要表：
- `tb_user` - 用户账户
- `tb_user_data` - 用户拓展数据
- `tb_works` - 作品
- `tb_comments` - 评论

## 作者

- PlayerEG - gaster@vip.playereg.top
- 贡献者：blue_sky_ks

---

<div align="center">

**如果对你有帮助，请给个 Star 支持一下！**

</div>