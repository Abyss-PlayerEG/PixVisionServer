---
name: pixvision-database
description: 数据库操作指南，涵盖 MyBatis-Plus 配置、实体映射、分页查询和 SQL 编写规范
---

# 数据库操作技能

## 模块概述

数据库层使用 MyBatis-Plus 3.5.7 作为 ORM 框架，MySQL 8.0 作为数据库，HikariCP 作为连接池。

## 核心文件

| 文件 | 路径 |
|------|------|
| MyBatis-Plus 配置 | `config/MyBatisPlusConfig.java` |
| 数据库配置 | `yml-config/jdbc.yml` |
| MyBatis 配置 | `yml-config/mybatis-plus.yml` |
| Mapper 接口 | `mapper/*.java` |
| Mapper XML | `resources/mapper/*.xml` |
| 实体类 | `pojo/*.java` |

## 数据库配置

### 连接配置 (`jdbc.yml`)
- 主机: `localhost:3306`
- 数据库: `db_pix_vision`
- 连接池: HikariCP
- 最大连接数: 10
- 最小空闲: 5

### MyBatis-Plus 配置 (`mybatis-plus.yml`)
- Mapper XML 位置: `classpath:mapper/*.xml`
- 表前缀: `tb_`
- 逻辑删除字段: `is_delete`
- 逻辑删除值: `1` (已删除) / `0` (未删除)

## 实体类规范

### 注解使用
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @TableId("user_id")          // 指定主键字段
    Integer user_id;
    
    @TableLogic                   // 逻辑删除标记
    Boolean is_delete;
    
    // 其他字段...
}
```

### 字段命名
- 数据库: 下划线命名 (`user_uuid`, `avatar_url`)
- Java: 下划线命名（与数据库一致）
- 非数据库字段: 不添加任何注解即可

### UUID 处理
- 数据库存储: `BINARY(16)` → Java `byte[]`
- 转换工具: `StrSwitchUtils.uuid2Bytes()` / `StrSwitchUtils.bytes2Uuid()`
- 实体类中同时保留 `user_uuid` (byte[]) 和 `string_user_uuid` (String)

## Mapper 编写

### 接口定义
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 获得基础 CRUD
    // 自定义方法写在此处
}
```

### XML 映射
```xml
<mapper namespace="top.playereg.pix_vision.mapper.UserMapper">
    <!-- 查询 -->
    <select id="方法名" resultType="返回类型">
        SELECT ... FROM tb_xxx WHERE ...
    </select>
    
    <!-- 插入 -->
    <insert id="方法名" parameterType="参数类型" useGeneratedKeys="true" keyProperty="主键字段">
        INSERT INTO tb_xxx (...) VALUES (...)
    </insert>
    
    <!-- 更新 -->
    <update id="方法名">
        UPDATE tb_xxx SET ... WHERE ...
    </update>
    
    <!-- 删除 -->
    <delete id="方法名">
        DELETE FROM tb_xxx WHERE ...
    </delete>
</mapper>
```

### 动态 SQL
```xml
<select id="selectPageUserInfo" resultType="User">
    SELECT * FROM tb_user WHERE is_delete = 0
    <if test="user.username != null and user.username != ''">
        AND username LIKE CONCAT('%', #{user.username}, '%')
    </if>
    <if test="user.email != null and user.email != ''">
        AND email LIKE CONCAT('%', #{user.email}, '%')
    </if>
</select>
```

## 分页查询

### 配置
已配置 `PaginationInnerInterceptor`，支持 MySQL 分页。

### 使用方式
```java
// Service 层
IPage<User> page = new Page<>(current, size);
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.like(StringUtils.isNotBlank(username), "username", username);
return userMapper.selectPage(page, wrapper);
```

## 常用 CRUD 操作

### BaseMapper 提供的方法
```java
// 插入
userMapper.insert(user);

// 根据 ID 删除
userMapper.deleteById(id);

// 根据条件删除
userMapper.delete(wrapper);

// 更新
userMapper.updateById(user);
userMapper.update(user, wrapper);

// 查询
userMapper.selectById(id);
userMapper.selectList(wrapper);
userMapper.selectOne(wrapper);
userMapper.selectCount(wrapper);
userMapper.selectPage(page, wrapper);
```

## 逻辑删除

- 使用 `@TableLogic` 注解标记 `is_delete` 字段
- 查询时自动添加 `AND is_delete = 0` 条件
- 删除时自动转换为 `UPDATE tb_xxx SET is_delete = 1 WHERE ...`

## SQL 编写规范

- 表名使用 `tb_` 前缀
- 字段名使用下划线命名
- 查询时明确指定字段，避免使用 `SELECT *`
- 使用 `#{}` 防止 SQL 注入
- 复杂查询使用 XML 映射，简单查询可使用注解或 BaseMapper 方法
