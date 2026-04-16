package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.playereg.pix_vision.config.FilePathConfig;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.ImageUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 图片访问控制器 - 提供细粒度的图片访问控制
 * <p>
 * 支持头像、作品、Logo 等图片资源的访问，具备以下特性：
 * - 文件类型白名单校验（仅允许图片格式）
 * - 路径安全检查（防止目录遍历攻击）
 * - 自动设置缓存头（1小时）
 * - 支持子目录结构
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.config.FilePathConfig
 */
@RestController
@RequestMapping("/api/get-image")
@Tag(name = "图片访问接口", description = "提供头像、作品、Logo 等图片资源的访问接口")
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private UserService userService;

    // 允许访问的图片扩展名白名单（仅支持 JPG、JPEG、PNG）
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png"
    );

    /**
     * 获取头像图片
     *
     * @param filePath 图像相对路径（支持子目录，如：default/11.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(
            summary = "获取头像图片",
            description = """
                    # 获取用户头像图片

                    ## 参数说明：
                    - filePath: **图像相对路径**，字符串类型，必填，支持子目录结构

                    ## 返回说明：
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非 JPG/JPEG/PNG 格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（jpg/jpeg/png）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/avatar/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - 支持子目录结构，如：`default/11.png`、`custom/user_avatar.jpg`
                    - **仅支持 JPG、JPEG、PNG 格式**的图片
                    - 文件路径相对于 `~/.pix_vision/data/avatar/` 目录

                    ## 使用示例：
                    ```
                    GET /image/avatar?filePath=default/1.png
                    GET /image/avatar?filePath=default/11.jpg
                    GET /image/avatar?filePath=custom/my_avatar.jpeg
                    ```
                    """
    )
    @GetMapping("/avatar")
    public ResponseEntity<Resource> getAvatar(
        @Parameter(description = "图像相对路径，支持子目录，如：default/11.png", required = true, example = "default/1.png") @RequestParam String filePath
    ) {
        return getImageResource(FilePathConfig.AvatarPath, filePath, "头像");
    }

    /**
     * 获取作品图片
     *
     * @param filePath 图像相对路径（支持子目录，如：2024/04/artwork.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(
            summary = "获取作品图片",
            description = """
                    # 获取作品图片

                    ## 参数说明：
                    - filePath: **图像相对路径**，字符串类型，必填，支持子目录结构

                    ## 返回说明：
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非 JPG/JPEG/PNG 格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（jpg/jpeg/png）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/works/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - 支持子目录结构，建议按日期或用户分类，如：`2024/04/artwork.png`
                    - **仅支持 JPG、JPEG、PNG 格式**的图片
                    - 文件路径相对于 `~/.pix_vision/data/works/` 目录
                    - 适用于展示用户上传的作品图片

                    ## 使用示例：
                    ```
                    GET /image/works?filePath=artwork_001.png
                    GET /image/works?filePath=2024/04/spring_art.jpg
                    GET /image/works?filePath=user123/gallery/photo.jpeg
                    ```
                    """
    )
    @GetMapping("/works")
    public ResponseEntity<Resource> getWorkImage(
        @Parameter(description = "图像相对路径，支持子目录，如：2024/04/artwork.png", required = true, example = "artwork_001.png") @RequestParam String filePath
    ) {
        return getImageResource(FilePathConfig.WorksPath, filePath, "作品");
    }

    /**
     * 获取Logo图片
     *
     * @param filePath 图像相对路径（如：dark.png、light.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(
            summary = "获取Logo图片",
            description = """
                    # 获取 Logo 图片

                    ## 参数说明：
                    - filePath: **图像文件名**，字符串类型，必填

                    ## 返回说明：
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非 JPG/JPEG/PNG 格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（jpg/jpeg/png）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/logo-img/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - Logo 图片通常不包含子目录，直接使用文件名即可
                    - **仅支持 JPG、JPEG、PNG 格式**的图片
                    - 文件路径相对于 `~/.pix_vision/data/logo-img/` 目录
                    - 适用于网站 Logo、品牌标识等静态图片资源

                    ## 使用示例：
                    ```
                    GET /image/logo?filePath=dark.png
                    GET /image/logo?filePath=light.jpg
                    GET /image/logo?filePath=brand_logo.jpeg
                    ```
                    """
    )
    @GetMapping("/logo")
    public ResponseEntity<Resource> getLogo(
        @Parameter(description = "图像文件名，如：dark.png", required = true, example = "dark.png") @RequestParam String filePath
    ) {
        return getImageResource(FilePathConfig.LogoPath, filePath, "Logo");
    }

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @param request HTTP 请求对象（用于获取用户 ID）
     * @return 响应结果
     * @author PlayerEG
     */
    @Operation(
            summary = "上传用户头像",
            description = """
                    # 上传用户头像

                    ## 参数说明：
                    - file: **头像文件**，MultipartFile 类型，必填

                    ## 返回说明：
                    - **上传成功**：返回 200 状态码和成功消息
                    - **未授权**：返回 401 状态码（Token 无效或不存在）
                    - **文件格式不支持**：返回 400 状态码
                    - **文件大小超限**：返回 400 状态码（最大 5MB）
                    - **图像不是正方形**：返回 400 状态码
                    - **服务器错误**：返回 500 状态码

                    ## 业务逻辑：
                    1. 从 Token 中获取当前登录用户的 ID
                    2. 验证文件格式（仅支持 jpg/jpeg/png）
                    3. 验证文件大小（最大 5MB）
                    4. 读取上传的图片数据
                    5. 验证图像是否为正方形（宽高必须相等）
                    6. 使用 ImageUtils.resizeImage() 将图片缩放为 600x600 的 PNG 格式
                    7. 生成唯一的文件名（UUID.png）
                    8. 保存文件到 ~/.pix_vision/data/avatar/ 目录
                    9. 更新数据库中的用户头像路径

                    ## 注意事项：
                    - 该接口**需要认证**，必须在请求头中携带有效的 Token
                    - **仅支持 JPG、JPEG、PNG 格式**的图片上传
                    - **头像必须是正方形图片**（宽高相等），如：800x800、1200x1200
                    - 图片会被自动缩放为 **600x600** 像素的 PNG 格式
                    - 文件名使用 UUID 生成，避免冲突
                    - 旧头像文件不会被自动删除，建议定期清理
                    - 文件大小限制为 **5MB**

                    ## 使用示例：
                    ```
                    POST /api/get-image/upload-avatar
                    Content-Type: multipart/form-data
                    Authorization: Bearer <your-token>

                    file: [binary image data]
                    ```
                    """
    )
    @PostMapping("/upload-avatar")
    public ResponseEntity<ResponsePojo<String>> uploadAvatar(
        @Parameter(description = "头像文件", required = true) @RequestParam MultipartFile file,
        HttpServletRequest request
    ) {
        try {
            // 1. 从 request 中获取用户 ID（由 JWT 拦截器设置）
            Integer userId = (Integer) request.getAttribute("userId");
            if (userId == null) {
                log.warn("未获取到用户 ID，请先登录");
                return ResponseEntity.status(401).body(ResponsePojo.error(null, "未授权访问：请先登录"));
            }

            // 2. 验证文件是否为空
            if (file.isEmpty()) {
                log.warn("上传的文件为空");
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "上传的文件不能为空"));
            }

            // 3. 验证文件格式
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.warn("文件名为空");
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "文件名不能为空"));
            }

            String extension = getFileExtension(originalFilename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("不支持的文件格式: {}", extension);
                return ResponseEntity.badRequest().body(ResponsePojo.error(null,
                    "不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS)));
            }

            // 4. 验证文件大小（最大 5MB）- 在读取文件内容之前验证
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                log.warn("文件大小超出限制: {} bytes ({} MB)", file.getSize(), file.getSize() / 1024.0 / 1024.0);
                return ResponseEntity.badRequest().body(ResponsePojo.error(null,
                    "文件大小超出限制，最大允许 5MB"));
            }

            // 5. 读取文件数据并验证
            byte[] imageBytes = file.getBytes();

            // 验证文件是否为空或太小
            if (imageBytes.length < 4) {
                log.warn("文件太小，可能不是有效图片，大小: {} bytes", imageBytes.length);
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "文件太小，不是有效的图片"));
            }

            // 验证是否为真实的图片格式（通过魔数检查）
            if (!ImageUtils.isValidImage(imageBytes)) {
                log.warn("文件不是有效的图片格式，文件大小: {} bytes", imageBytes.length);
                return ResponseEntity.badRequest().body(ResponsePojo.error(null,
                    "文件不是有效的图片格式，请上传 JPG/JPEG/PNG 格式的图片"));
            }
            
            // 6. 验证图像是否为正方形
            if (!ImageUtils.isSquareImage(imageBytes)) {
                log.warn("图像不是正方形");
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, 
                    "头像必须是正方形图片，请上传宽高相等的图片"));
            }
                            
            // 7. 缩放图片为 600x600 的 PNG 格式
            log.info("开始处理头像图片，原始大小: {} bytes", imageBytes.length);
            byte[] resizedImage = ImageUtils.resizeImage(imageBytes, 600, 600, true);
            log.info("头像图片处理完成，处理后大小: {} bytes", resizedImage.length);
                            
            // 8. 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".png";
            String savePath = Paths.get(FilePathConfig.AvatarPath, fileName).toString();
                
            // 9. 保存文件
            File saveFile = new File(savePath);
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
                
            cn.hutool.core.io.FileUtil.writeBytes(resizedImage, saveFile);
            log.info("头像文件保存成功: {}", savePath);
                
            // 10. 更新数据库中的头像路径（只保存相对路径）
            String avatarUrl = fileName; // 直接保存文件名，访问时使用 /api/get-image/avatar?filePath=xxx.png
            Boolean updateResult = userService.updateUserAvatar(userId, avatarUrl);

            if (!updateResult) {
                log.error("更新用户头像路径失败，用户 ID: {}", userId);
                // 如果数据库更新失败，删除已上传的文件
                if (saveFile.exists()) {
                    saveFile.delete();
                }
                return ResponseEntity.status(500).body(ResponsePojo.error(null, "更新用户头像失败"));
            }

            log.info("用户头像上传成功，用户 ID: {}, 头像路径: {}", userId, avatarUrl);
            return ResponseEntity.ok(ResponsePojo.success(avatarUrl, "头像上传成功"));

        } catch (IllegalArgumentException e) {
            log.error("参数验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponsePojo.error(null, e.getMessage()));
        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponsePojo.error(null, "头像上传失败: " + e.getMessage()));
        }
    }

    // ================================================================================

    /**
     * 通用图片资源获取方法
     *
     * @param basePath   基础路径
     * @param filePath   文件相对路径(可包含子目录)
     * @param imageType  图片类型(用于日志)
     * @return 图片资源响应
     * @author PlayerEG
     */
    private ResponseEntity<Resource> getImageResource(String basePath, String filePath, String imageType) {
        try {
            // 1. 安全检查:防止路径遍历攻击
            if (filePath.contains("..") || filePath.startsWith("/") || filePath.startsWith("\\")) {
                log.warn("非法的文件路径: {}", filePath);
                return ResponseEntity.badRequest().build();
            }

            // 2. 检查文件扩展名是否在白名单中
            String extension = getFileExtension(filePath);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                log.warn("不允许的文件类型: {}, 文件路径: {}", extension, filePath);
                return ResponseEntity.badRequest().build();
            }

            // 3. 构建完整文件路径
            Path fullPath = Paths.get(basePath, filePath).normalize();
            File file = fullPath.toFile();

            // 4. 检查文件是否存在
            if (!file.exists() || !file.isFile()) {
                log.warn("{}文件不存在: {}", imageType, filePath);
                return ResponseEntity.notFound().build();
            }

            // 5. 额外安全检查:确保文件在允许的目录下
            Path allowedPath = Paths.get(basePath).normalize();
            if (!fullPath.startsWith(allowedPath)) {
                log.warn("文件路径超出允许范围: {}", fullPath);
                return ResponseEntity.status(403).build();
            }

            // 6. 返回图片资源
            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(extension);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")  // 缓存1小时
                    .body(resource);

        } catch (Exception e) {
            log.error("获取{}图片失败: {}, 错误: {}", imageType, filePath, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 从请求路径中提取文件相对路径
     *
     * @param prefix 前缀路径(如: /image/avatar/)
     * @return 文件相对路径
     * @author PlayerEG
     */
    private String extractPath(String prefix) {
        // 获取当前请求的完整路径
        String requestUri = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping", 0)
                .toString();

        // 移除前缀,得到相对路径
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        return requestUri;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名(不含点)
     * @author PlayerEG
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 根据扩展名获取 Content-Type
     *
     * @param extension 文件扩展名
     * @return MIME 类型
     * @author PlayerEG
     */
    private String getContentType(String extension) {
        switch (extension.toLowerCase()) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }
}
