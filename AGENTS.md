# 项目概述

> 这是项目概述。

- 语言：Java
- 构建工具：Maven
- 框架：Spring Boot
- 测试框架：JUnit

# 架构描述

> 项目结构在此描述

## 配置文件地址

- `src/main/resources/config/*.yml` 核心依赖配置文件，优先级：1
- `src/main/resources/application.yml` 项目主配置文件，优先级：0
- `${user.home}/.pix_vision/application.yml` 用户自定义配置文件，优先级：2
- `src/main/resources/log4j.properties` log4j日志配置文件
- `src/main/java/top/playereg/pix_vision/config/*.java` 配置类

## 模板文件

`src/main/resources/template/` 模板目录，存放模板文件。
在项目初始化时，会使用 `CreateFile.java` 内的 `create()` 方法将模板初始化到用户文件夹

列出你的后端服务，例如数据库、消息队列、外部服务等。

- 主数据库：Mysql8
- 表命名：tb_*
- 缓存：redis8