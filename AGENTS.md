# 项目概述

> 这是项目概述。

- 语言：Java
- 构建工具：Maven
- 框架：Spring Boot
- 测试框架：JUnit

---

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

---

# Swagger 文档要求

1. 使用注解：@Operation
2. 补全参数：summary、description
3. description 格式要求，重点部分需要使用斜体、粗体、删除线等
    ```markdown
    # 接口描述
    
    ## 参数说明：
    - 参数1: 描述
    - 参数2: 描述
    
    ## 返回说明：
    - XX成功：返回XXXX
    - XX失败：返回XXXX
    
    ## 业务逻辑：
    1. 步骤1
    2. 步骤2
    3. 步骤3
    4. 步骤4
    
    ## 注意事项：
    - 注意事项1
    - 注意事项2
    ```
4. description 示例
   ```markdown
   # 发送一封 HTML 格式的验证码邮件
   
   ## 参数说明：
   - to: 收件人邮箱地址，格式为标准邮箱格式
   - subject: 邮件主题，字符串类型
   - username: 用户昵称，用于邮件模板中个性化显示
   - emailText: 邮件内容类型，可选值：注册、登录、修改密码
   
   ## 返回说明：
   - 发送成功：返回 **"data": true** 和"邮件发送成功"提示
   - 发送失败：返回 **"data": false** 和"邮件发送失败"提示
   - 格式错误：返回 **"data": false** 和相应的"邮箱或内容类型错误"提示
   
   ## 业务逻辑：
   1. 校验邮箱格式是否合法
   2. 根据 emailText 类型生成对应的邮件内容（注册验证/登录验证/密码修改）
   3. 生成 6 位随机验证码并存入 Redis
   4. 使用 HTML 邮件模板渲染邮件内容
   5. 发送邮件并将验证码与邮箱绑定存储
   
   ## 注意事项：
   - 验证码默认有效期由 Redis 配置决定
   - emailText 仅支持：**注册**、**登录**、**改密** 三种类型
   ```