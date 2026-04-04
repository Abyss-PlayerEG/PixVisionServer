---
name: pixvision-backend
description: PixVisionServer 后端开发指南，涵盖项目结构、开发规范、常用工具和调试方法
---

# PixVisionServer 后端开发技能

## 项目概述

- **框架**: Spring Boot 3.3.0 + Java 17
- **端口**: 9090
- **包名**: `top.playereg.pix_vision`
- **构建**: Maven

## 快速启动

```bash
# 启动依赖服务
brew services start mysql
brew services start redis

# 运行项目
mvn spring-boot:run
# 或在 IDE 中运行 PixVisionApplication.main()
```

## 访问地址

| 服务 | URL |
|------|-----|
| 应用首页 | http://localhost:9090 |
| Swagger 文档 | http://localhost:9090/swagger-ui.html |
| Knife4j 文档 | http://localhost:9090/doc.html |
| 健康检查 | http://localhost:9090/health |

## 项目结构

```
src/main/java/top/playereg/pix_vision/
├── config/              # 配置类
├── controller/          # 控制器层
├── service/             # 服务接口
│   └── Impl/           # 服务实现
├── mapper/              # MyBatis Mapper
├── pojo/                # 实体和 DTO
├── handler/             # 拦截器
├── util/                # 工具类
└── enums/               # 枚举
```

## 开发规范

### 命名规范
- Controller: `XxxController`
- Service: `XxxService` / `XxxServiceImpl`
- Mapper: `XxxMapper`
- 工具类: `XxxUtils`
- 配置类: `XxxConfig`

### 返回值规范
```java
// 成功
ResponsePojo.success(data, "成功信息")
// 失败
ResponsePojo.error(data, "错误信息")
```

### 依赖注入
优先使用构造器注入（`@RequiredArgsConstructor`）

### 日志规范
```java
private static final Logger log = LoggerFactory.getLogger(当前类.class);
log.info("操作成功：{}", 参数);
```

## 配置说明

配置文件优先级：
1. `application.yml` (优先级 0)
2. `yml-config/*.yml` (优先级 1)
3. `~/.pix_vision/application.yml` (优先级 2，用户自定义)

## 测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=UserServiceImplTest
```
