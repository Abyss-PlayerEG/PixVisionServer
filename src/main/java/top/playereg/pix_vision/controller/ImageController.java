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
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.config.FilePathConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 图片访问控制器 - 提供细粒度的图片访问控制
 *
 * @author PlayerEG
 */
@RestController
@RequestMapping("/image")
@Tag(name = "ImageController", description = "图片访问接口")
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    // 允许访问的图片扩展名白名单
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"
    );

    /**
     * 获取头像图片
     *
     * @return 图片资源
     * @author PlayerEG
     */
    @Operation(
            summary = "获取头像图片",
            description = "根据文件路径获取用户头像图片,支持子目录"
    )
    @GetMapping("/avatar")
    public ResponseEntity<Resource> getAvatar(
        @Parameter(description = "图像路径") @RequestParam String filePath
    ) {
        return getImageResource(FilePathConfig.AvatarPath, filePath, "头像");
    }

    /**
     * 获取作品图片
     *
     * @return 图片资源
     * @author PlayerEG
     */
    @Operation(
            summary = "获取作品图片",
            description = "根据文件路径获取作品图片,支持子目录"
    )
    @GetMapping("/works")
    public ResponseEntity<Resource> getWorkImage(
        @Parameter(description = "图像路径") @RequestParam String filePath
    ) {
        return getImageResource(FilePathConfig.WorksPath, filePath, "作品");
    }

    /**
     * 获取Logo图片
     *
     * @return 图片资源
     * @author PlayerEG
     */
    @Operation(
            summary = "获取Logo图片",
            description = "根据文件路径获取Logo图片,支持子目录"
    )
    @GetMapping("/logo")
    public ResponseEntity<Resource> getLogo(
        @Parameter(description = "图像路径") @RequestParam String filePath
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
