package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.config.FilePathConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
@RequestMapping("/aip/get-image")
@Tag(name = "图片访问接口", description = "提供头像、作品、Logo 等图片资源的访问接口")
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    // 允许访问的图片扩展名白名单
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"
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
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg 等
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非图片格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（png/jpg/jpeg/gif/webp/bmp/svg）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/avatar/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - 支持子目录结构，如：`default/11.png`、`custom/user_avatar.png`
                    - 仅允许访问图片格式文件，其他格式会被拒绝
                    - 文件路径相对于 `~/.pix_vision/data/avatar/` 目录

                    ## 使用示例：
                    ```
                    GET /image/avatar?filePath=default/1.png
                    GET /image/avatar?filePath=default/11.png
                    GET /image/avatar?filePath=custom/my_avatar.jpg
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
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg 等
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非图片格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（png/jpg/jpeg/gif/webp/bmp/svg）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/works/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - 支持子目录结构，建议按日期或用户分类，如：`2024/04/artwork.png`
                    - 仅允许访问图片格式文件，其他格式会被拒绝
                    - 文件路径相对于 `~/.pix_vision/data/works/` 目录
                    - 适用于展示用户上传的作品图片

                    ## 使用示例：
                    ```
                    GET /image/works?filePath=artwork_001.png
                    GET /image/works?filePath=2024/04/spring_art.jpg
                    GET /image/works?filePath=user123/gallery/photo.webp
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
                    - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg 等
                    - **文件不存在**：返回 **404 Not Found**
                    - **非法路径**：返回 **400 Bad Request**（包含 .. 或 / 开头）
                    - **不支持的格式**：返回 **400 Bad Request**（非图片格式）
                    - **路径越权**：返回 **403 Forbidden**（超出允许目录范围）
                    - **服务器错误**：返回 **500 Internal Server Error**

                    ## 业务逻辑：
                    1. 校验文件路径安全性（禁止 .. 和绝对路径）
                    2. 检查文件扩展名是否在白名单中（png/jpg/jpeg/gif/webp/bmp/svg）
                    3. 构建完整文件路径并验证文件存在性
                    4. 确保文件在允许的目录下（~/.pix_vision/data/logo-img/）
                    5. 设置响应头 Content-Type 和 Cache-Control
                    6. 返回图片二进制数据

                    ## 注意事项：
                    - 该接口**无需认证**，任何人都可以访问
                    - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
                    - Logo 图片通常不包含子目录，直接使用文件名即可
                    - 仅允许访问图片格式文件，其他格式会被拒绝
                    - 文件路径相对于 `~/.pix_vision/data/logo-img/` 目录
                    - 适用于网站 Logo、品牌标识等静态图片资源

                    ## 使用示例：
                    ```
                    GET /image/logo?filePath=dark.png
                    GET /image/logo?filePath=light.png
                    GET /image/logo?filePath=brand_logo.svg
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
