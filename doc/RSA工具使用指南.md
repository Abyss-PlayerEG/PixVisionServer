# RSA 加密工具使用指南

## 📋 目录

- [概述](#概述)
- [核心特性](#核心特性)
- [快速开始](#快速开始)
- [API 参考](#api-参考)
- [使用示例](#使用示例)
- [加密策略](#加密策略)
- [密钥管理](#密钥管理)
- [注意事项](#注意事项)
- [常见问题](#常见问题)

---

## 概述

`RSACipher` 是一个功能强大的 RSA 加密工具类，支持**任意类型和大小**的数据加密。它采用智能的混合加密方案（RSA + AES），既能保证安全性，又能处理大数据和二进制文件。

**文件位置：** `src/main/java/top/playereg/pix_vision/util/RSACipher.java`

**密钥存储：** `${user.home}/.pix_vision/key/rsa/`

---

## 核心特性

✅ **智能加密策略**：自动根据数据大小选择最优加密方式  
✅ **支持任意数据类型**：文本、JSON、图片、文件等  
✅ **无大小限制**：通过 AES + RSA 混合加密突破 RSA 长度限制  
✅ **自动密钥管理**：启动时自动生成或加载密钥  
✅ **向后兼容**：支持解密旧版本的纯 RSA 加密数据  
✅ **密钥备份机制**：更换密钥时自动备份旧密钥  

---

## 快速开始

### 1. 基本加密解密

```java
import top.playereg.pix_vision.util.RSACipher;

// 加密字符串
String original = "Hello, PixVision!";
String encrypted = RSACipher.encryptToBase64(original);
System.out.println("密文: " + encrypted);

// 解密字符串
String decrypted = RSACipher.decryptToString(encrypted);
System.out.println("原文: " + decrypted);
```

### 2. 加密二进制数据

```java
import java.nio.file.Files;
import java.nio.file.Paths;

// 读取图片文件
byte[] imageData = Files.readAllBytes(Paths.get("photo.jpg"));

// 加密
String encrypted = RSACipher.encryptToBase64(imageData);

// 解密
byte[] decrypted = RSACipher.decryptToBytes(encrypted);

// 保存解密后的图片
Files.write(Paths.get("photo_decrypted.jpg"), decrypted);
```

---

## API 参考

### 加密方法

#### `encryptToBase64(String plainText)`

加密字符串，返回 Base64 编码的密文。

**参数：**
- `plainText` - 明文字符串

**返回：**
- Base64 编码的密文字符串

**示例：**
```java
String encrypted = RSACipher.encryptToBase64("敏感信息");
```

---

#### `encryptToBase64(byte[] data)`

加密字节数组，支持任意二进制数据。

**参数：**
- `data` - 明文数据的字节数组

**返回：**
- Base64 编码的密文字符串

**示例：**
```java
byte[] fileData = Files.readAllBytes(Paths.get("document.pdf"));
String encrypted = RSACipher.encryptToBase64(fileData);
```

---

### 解密方法

#### `decryptToString(String encryptedBase64)`

解密密文为字符串。

**参数：**
- `encryptedBase64` - Base64 编码的密文

**返回：**
- 解密后的明文字符串，如果输入为空则返回 `null`

**示例：**
```java
String decrypted = RSACipher.decryptToString(encrypted);
```

---

#### `decryptToBytes(String encryptedBase64)`

解密密文为字节数组，适用于二进制数据。

**参数：**
- `encryptedBase64` - Base64 编码的密文

**返回：**
- 解密后的字节数组，如果输入为空则返回 `null`

**示例：**
```java
byte[] decrypted = RSACipher.decryptToBytes(encrypted);
```

---

### 密钥管理方法

#### `generateRSABase64()`

生成新的 RSA 密钥对。

**返回：**
- 字符串数组，`[0]` 为公钥，`[1]` 为私钥（均为 Base64 编码）

**示例：**
```java
String[] keys = RSACipher.generateRSABase64();
String publicKey = keys[0];
String privateKey = keys[1];
```

---

#### `regenerateKeys()`

重新生成密钥对并保存到文件，同时备份旧密钥。

**返回：**
- 新的密钥数组，`[0]` 为公钥，`[1]` 为私钥

**注意：**
- ⚠️ 更换密钥后，**使用旧密钥加密的数据将无法解密**
- 旧密钥会自动备份为 `.bak` 文件

**示例：**
```java
String[] newKeys = RSACipher.regenerateKeys();
```

---

#### `getPublicKey()`

获取当前公钥（Base64 编码）。

**返回：**
- 公钥字符串

**示例：**
```java
String publicKey = RSACipher.getPublicKey();
```

---

#### `getPrivateKey()`

获取当前私钥（Base64 编码）。

**返回：**
- 私钥字符串

**警告：**
- 🔒 私钥包含敏感信息，请妥善保管，不要泄露

**示例：**
```java
String privateKey = RSACipher.getPrivateKey();
```

---

## 使用示例

### 示例 1：用户密码加密

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @PostMapping("/register")
    public ResponsePojo<Boolean> register(@RequestParam String password) {
        // 加密密码后存储
        String encryptedPassword = RSACipher.encryptToBase64(password);
        
        // 保存到数据库
        user.setPassword(encryptedPassword);
        userService.save(user);
        
        return ResponsePojo.success(true, "注册成功");
    }
    
    @PostMapping("/login")
    public ResponsePojo<UserLogin> login(@RequestParam String password) {
        // 从数据库获取加密的密码
        User user = userService.getByUsername(username);
        
        // 解密密码进行比对（实际项目中建议使用哈希而非加密）
        String decryptedPassword = RSACipher.decryptToString(user.getPassword());
        
        if (password.equals(decryptedPassword)) {
            return ResponsePojo.success(userLogin, "登录成功");
        }
        
        return ResponsePojo.error(null, "密码错误");
    }
}
```

---

### 示例 2：敏感配置文件加密

```java
@Service
public class ConfigService {
    
    /**
     * 加密配置文件内容
     */
    public void encryptConfigFile(String configPath) throws IOException {
        // 读取配置文件
        String configContent = Files.readString(Paths.get(configPath));
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(configContent);
        
        // 保存加密后的内容
        Files.writeString(Paths.get(configPath + ".enc"), encrypted);
        
        // 删除原始文件
        Files.delete(Paths.get(configPath));
    }
    
    /**
     * 解密配置文件内容
     */
    public String decryptConfigFile(String encryptedConfigPath) throws IOException {
        // 读取加密内容
        String encrypted = Files.readString(Paths.get(encryptedConfigPath));
        
        // 解密
        return RSACipher.decryptToString(encrypted);
    }
}
```

---

### 示例 3：图片文件加密存储

```java
@Service
public class ImageService {
    
    @Autowired
    private ImageMapper imageMapper;
    
    /**
     * 上传并加密图片
     */
    public String uploadEncryptedImage(MultipartFile file) throws IOException {
        // 读取图片数据
        byte[] imageData = file.getBytes();
        
        // 加密
        String encrypted = RSACipher.encryptToBase64(imageData);
        
        // 保存到数据库
        Image image = new Image();
        image.setFileName(file.getOriginalFilename());
        image.setEncryptedData(encrypted);
        image.setUploadTime(LocalDateTime.now());
        imageMapper.insert(image);
        
        return image.getId().toString();
    }
    
    /**
     * 获取并解密图片
     */
    public byte[] getDecryptedImage(Long imageId) {
        // 从数据库获取加密数据
        Image image = imageMapper.selectById(imageId);
        
        // 解密
        return RSACipher.decryptToBytes(image.getEncryptedData());
    }
}
```

---

### 示例 4：JSON 对象加密传输

```java
@Service
public class DataService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 加密对象为 JSON 字符串
     */
    public String encryptObject(Object obj) throws JsonProcessingException {
        // 对象转 JSON
        String json = objectMapper.writeValueAsString(obj);
        
        // 加密 JSON
        return RSACipher.encryptToBase64(json);
    }
    
    /**
     * 解密 JSON 字符串为对象
     */
    public <T> T decryptObject(String encrypted, Class<T> clazz) 
            throws JsonProcessingException {
        // 解密
        String json = RSACipher.decryptToString(encrypted);
        
        // JSON 转对象
        return objectMapper.readValue(json, clazz);
    }
}

// 使用示例
User user = new User("张三", "zhangsan@example.com");
String encrypted = dataService.encryptObject(user);

User restored = dataService.decryptObject(encrypted, User.class);
```

---

### 示例 5：邮件内容加密

```java
@Service
public class EmailService {
    
    /**
     * 发送加密邮件
     */
    public void sendEncryptedEmail(String to, String subject, String content) {
        // 加密邮件内容
        String encryptedContent = RSACipher.encryptToBase64(content);
        
        // 构建邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText("这是一封加密邮件，请使用密钥解密查看内容。\n\n" + 
                       "加密内容:\n" + encryptedContent);
        
        // 发送邮件
        mailSender.send(message);
    }
    
    /**
     * 接收方解密邮件
     */
    public String decryptEmailContent(String encryptedContent) {
        return RSACipher.decryptToString(encryptedContent);
    }
}
```

---

## 加密策略

### 自动选择机制

工具会根据数据大小自动选择最优的加密策略：

| 数据大小 | 加密方式 | 前缀标识 | 说明 |
|---------|---------|---------|------|
| < 200 字节 | 纯 RSA | `RSA:` | 速度快，适合短文本 |
| ≥ 200 字节 | AES + RSA | `HYBRID:` | 无大小限制，适合大数据 |

### 混合加密流程

```
加密过程：
┌─────────────┐
│  原始数据    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ 数据 >= 200字节？│
└────┬────────┬───┘
     │ Yes    │ No
     ▼        ▼
┌────────┐ ┌──────────┐
│AES加密  │ │RSA直接加密│
│数据     │ │          │
└────┬───┘ └────┬─────┘
     │           │
     ▼           │
┌────────┐      │
│RSA加密  │      │
│AES密钥  │      │
└────┬───┘      │
     │           │
     └─────┬─────┘
           ▼
    ┌─────────────┐
    │  组合结果    │
    │ HYBRID:     │
    │ xxx::yyy    │
    └─────────────┘

解密过程：
┌─────────────┐
│  密文       │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ 识别前缀         │
│ RSA: / HYBRID:  │
└────┬────────┬───┘
     │        │
     ▼        ▼
┌────────┐ ┌──────────┐
│RSA解密  │ │RSA解密    │
│        │ │AES密钥    │
└────────┘ └────┬─────┘
                │
                ▼
         ┌────────┐
         │AES解密  │
         │数据     │
         └────┬───┘
              │
              ▼
       ┌─────────────┐
       │  原始数据    │
       └─────────────┘
```

### 性能对比

| 数据大小 | 纯 RSA | AES + RSA | 推荐方案 |
|---------|--------|-----------|---------|
| 10 字节 | ~5ms | ~8ms | 纯 RSA ✅ |
| 100 字节 | ~5ms | ~8ms | 纯 RSA ✅ |
| 1 KB | ❌ 不支持 | ~10ms | 混合加密 ✅ |
| 1 MB | ❌ 不支持 | ~50ms | 混合加密 ✅ |
| 10 MB | ❌ 不支持 | ~300ms | 混合加密 ✅ |

---

## 密钥管理

### 密钥存储位置

```
${user.home}/.pix_vision/
└── key/
    └── rsa/
        ├── public.key          # RSA 公钥（Base64）
        ├── private.key         # RSA 私钥（Base64）
        ├── public.key.bak      # 旧公钥备份
        ├── private.key.bak     # 旧私钥备份
        └── 目录说明.txt        # 使用说明
```

### 密钥初始化

应用启动时会自动执行以下操作：

1. 检查密钥文件是否存在
2. 如果存在，从文件加载密钥
3. 如果不存在，生成新密钥对并保存到文件

**日志输出：**
```
INFO  - 创建 RSA 密钥目录: /Users/xxx/.pix_vision/key/rsa
INFO  - 未找到 RSA 密钥文件，正在生成新的密钥对...
INFO  - RSA 密钥已生成并保存到: /Users/xxx/.pix_vision/key/rsa
```

### 密钥更换

当需要更换密钥时（如安全审计、定期轮换）：

```java
// 更换密钥（会自动备份旧密钥）
String[] newKeys = RSACipher.regenerateKeys();

log.info("新公钥: {}", newKeys[0]);
log.warn("旧密钥已备份到 .bak 文件");
log.warn("注意：使用旧密钥加密的数据无法用新密钥解密！");
```

**备份文件：**
- `public.key.bak` - 旧公钥
- `private.key.bak` - 旧私钥

### 密钥备份建议

1. **定期备份**：每周或每月备份一次密钥文件
2. **异地存储**：将备份存储在安全的异地位置
3. **访问控制**：限制密钥文件的访问权限
4. **版本管理**：保留多个历史版本的密钥备份

```bash
# 手动备份示例
cp ~/.pix_vision/key/rsa/private.key ~/backup/rsa_private_$(date +%Y%m%d).key
chmod 600 ~/backup/rsa_private_*.key
```

---

## 注意事项

### ⚠️ 重要提醒

1. **私钥安全**
   - 🔒 私钥文件包含敏感信息，严禁泄露
   - 设置文件权限：`chmod 600 private.key`
   - 不要在代码中硬编码私钥
   - 不要将私钥提交到版本控制系统

2. **密钥更换影响**
   - ⚠️ 更换密钥后，**旧密钥加密的数据无法用新密钥解密**
   - 更换前务必备份所有加密数据或重新加密
   - 建议在低峰期进行密钥更换操作

3. **性能考虑**
   - 小数据（< 200 字节）使用纯 RSA，速度更快
   - 大数据自动切换到混合加密，避免 RSA 长度限制
   - 频繁加密大文件时，考虑缓存 AES 密钥（需评估安全风险）

4. **数据完整性**
   - RSA/AES 不提供数据完整性校验
   - 如需防篡改，建议结合 HMAC 或数字签名
   - 可以在加密前计算数据的哈希值并一起存储

5. **异常处理**
   - 加密/解密失败会抛出 `RuntimeException`
   - 建议在调用处进行 try-catch 处理
   - 记录详细的错误日志便于排查问题

### 💡 最佳实践

#### 1. 密码存储

```java
// ❌ 不推荐：使用可逆加密存储密码
String encryptedPassword = RSACipher.encryptToBase64(password);

// ✅ 推荐：使用不可逆哈希（如 BCrypt）
String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
```

**原因：** 密码应该使用单向哈希算法，而不是可逆加密。

#### 2. 敏感数据传输

```java
// ✅ 推荐：加密敏感字段
public class SensitiveData {
    private String username;
    private String encryptedIdCard;  // 身份证号加密存储
    private String encryptedPhone;   // 手机号加密存储
    
    public void setIdCard(String idCard) {
        this.encryptedIdCard = RSACipher.encryptToBase64(idCard);
    }
    
    public String getIdCard() {
        return RSACipher.decryptToString(encryptedIdCard);
    }
}
```

#### 3. 批量数据处理

```java
// ✅ 推荐：分批处理大量数据
public void encryptLargeDataset(List<String> dataList) {
    int batchSize = 100;
    for (int i = 0; i < dataList.size(); i += batchSize) {
        int end = Math.min(i + batchSize, dataList.size());
        List<String> batch = dataList.subList(i, end);
        
        // 处理批次
        batch.forEach(data -> {
            String encrypted = RSACipher.encryptToBase64(data);
            // 保存加密数据
        });
        
        log.info("已处理 {}/{}", end, dataList.size());
    }
}
```

#### 4. 错误处理

```java
// ✅ 推荐：完善的异常处理
try {
    String encrypted = RSACipher.encryptToBase64(sensitiveData);
    saveToDatabase(encrypted);
} catch (RuntimeException e) {
    log.error("数据加密失败", e);
    throw new BusinessException("数据加密失败，请稍后重试");
}

try {
    String decrypted = RSACipher.decryptToString(encryptedData);
    return decrypted;
} catch (RuntimeException e) {
    log.error("数据解密失败，可能密钥不匹配或数据损坏", e);
    throw new BusinessException("数据解密失败");
}
```

---

## 常见问题

### Q1: 为什么我的数据解密失败？

**可能原因：**
1. 密钥不匹配（使用了不同的密钥对）
2. 密文在传输过程中被修改
3. 密文格式不正确（缺少前缀或分隔符）

**解决方案：**
```java
// 检查密钥是否正确
log.info("当前公钥: {}", RSACipher.getPublicKey().substring(0, 20));

// 检查密文格式
if (!encrypted.startsWith("RSA:") && !encrypted.startsWith("HYBRID:")) {
    log.warn("密文格式可能不正确");
}

// 查看详细错误日志
try {
    RSACipher.decryptToString(encrypted);
} catch (Exception e) {
    log.error("解密失败详情", e);
}
```

---

### Q2: 如何迁移旧的加密数据到新密钥？

**步骤：**

1. 备份旧密钥和新密钥
2. 使用旧密钥解密所有数据
3. 使用新密钥重新加密
4. 验证解密正确性
5. 删除旧密钥备份（确认无误后）

```java
public void migrateEncryptedData(String oldPrivateKey, String newPublicKey) {
    // 1. 获取所有加密数据
    List<EncryptedRecord> records = mapper.selectAll();
    
    // 2. 逐个迁移
    for (EncryptedRecord record : records) {
        try {
            // 使用旧密钥解密
            String original = decryptWithOldKey(record.getEncryptedData(), oldPrivateKey);
            
            // 使用新密钥加密
            String reEncrypted = RSACipher.encryptToBase64(original);
            
            // 更新数据库
            record.setEncryptedData(reEncrypted);
            mapper.updateById(record);
            
            log.info("迁移成功: ID={}", record.getId());
        } catch (Exception e) {
            log.error("迁移失败: ID={}", record.getId(), e);
        }
    }
}
```

---

### Q3: 加密后的数据太大怎么办？

**原因：** Base64 编码会使数据增大约 33%

**解决方案：**
1. 数据库字段使用 `TEXT` 或 `LONGTEXT` 类型
2. 对于超大文件，考虑分块加密
3. 使用压缩后再加密

```java
// 压缩后加密示例
import java.util.zip.GZIPOutputStream;

public String compressAndEncrypt(String data) throws IOException {
    // 压缩
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
        gzos.write(data.getBytes(StandardCharsets.UTF_8));
    }
    
    // 加密
    return RSACipher.encryptToBase64(baos.toByteArray());
}
```

---

### Q4: 如何在分布式环境中共享密钥？

**方案 1：集中式密钥管理**
```java
// 从密钥管理服务获取密钥
@Component
public class KeyManagementService {
    
    @Value("${key.management.url}")
    private String keyManagementUrl;
    
    public String getPublicKey() {
        return restTemplate.getForObject(keyManagementUrl + "/public-key", String.class);
    }
}
```

**方案 2：配置文件分发**
```yaml
# application.yml
rsa:
  public-key: ${RSA_PUBLIC_KEY}
  private-key-path: /secure/path/to/private.key
```

**方案 3：环境变量**
```bash
export RSA_PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."
export RSA_PRIVATE_KEY_PATH="/secure/private.key"
```

---

### Q5: 如何测试加密解密功能？

运行单元测试：

```bash
mvn test -Dtest=RSACipherTest
```

**测试覆盖：**
- ✅ 小数据加密（纯 RSA）
- ✅ 大数据加密（混合加密）
- ✅ 二进制数据加密
- ✅ 自动策略选择
- ✅ 中文和特殊字符
- ✅ 空值和 null 处理
- ✅ 密钥生成和更换

---

## 技术支持

如有问题或建议，请联系：

- **项目仓库**：[PixVisionServer](https://github.com/your-repo/PixVisionServer)
- **Issue 提交**：GitHub Issues
- **作者**：PlayerEG, blue_sky_ks

---

## 更新日志

### v2.0 (2026-04-05)
- ✨ 新增 AES + RSA 混合加密支持
- ✨ 支持任意类型和大小的数据加密
- ✨ 自动选择最优加密策略
- ✨ 新增 `decryptToBytes()` 方法
- 🔄 优化密钥管理流程
- 📝 完善文档和示例

### v1.0 (2026-04-01)
- 🎉 初始版本发布
- ✅ 基础 RSA 加密解密
- ✅ 密钥文件存储
- ✅ 密钥自动生成

---

**最后更新：** 2026-04-05  
**文档版本：** v2.0
