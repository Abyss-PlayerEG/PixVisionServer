package top.playereg.pix_vision.controller;

import cn.hutool.core.io.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import top.playereg.pix_vision.pojo.WorkUploadResult;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.service.WorkService;
import top.playereg.pix_vision.util.Annotation.PublicAccess;
import top.playereg.pix_vision.util.Annotation.RequireRole;
import top.playereg.pix_vision.util.ImageUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

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
 * <p>
 * - 文件类型白名单校验（仅允许图片格式）
 * <p>
 * - 路径安全检查（防止目录遍历攻击）
 * <p>
 * - 自动设置缓存头（1小时）
 * <p>
 * - 支持子目录结构
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.config.FilePathConfig
 */
@RestController
@RequestMapping("/api/image")
@Tag(name = "图像接口", description = "提供头像、作品、Logo 等图片资源的访问接口")
public class ImageController {
    private static final PixVisionLogger log = PixVisionLogger.create(ImageController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private WorkService workService;

    @Autowired
    private top.playereg.pix_vision.mapper.WorksMapper worksMapper;

    // 允许访问的图片扩展名白名单（仅支持 JPG、JPEG、PNG）
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");

    // 作品文件的状态后缀（.fail / .pend / .del），用于剥离后获取真实图片扩展名
    private static final List<String> WORK_FILE_SUFFIXES = Arrays.asList(".fail", ".pend", ".del");

    /**
     * 获取头像图片
     *
     * @param filePath 图像相对路径（支持子目录，如：default/11.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(summary = "获取头像图片", description = """
        # 获取用户头像图片（无需登录认证）

        ## 特性
        - 公开接口（无需认证）
        - 路径安全校验（防目录遍历攻击）
        - 文件类型白名单（JPG/JPEG/PNG）
        - HTTP 缓存支持（1小时）
        - 子目录结构支持

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
        """)
    @PublicAccess("获取头像图片，无需认证")
    @GetMapping("/avatar/get")
    public ResponseEntity<Resource> getAvatar(@Parameter(description = "图像相对路径，支持子目录，如：default/11.png", required = true, example = "default/1.png") @RequestParam String filePath) {
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
            # 获取作品图片（无需登录认证）

            ## 特性
            - 公开接口（无需认证）
            - 路径安全校验（防目录遍历攻击）
            - 文件类型白名单（JPG/JPEG/PNG）
            - HTTP 缓存支持（1小时）
            - 多层子目录支持
            - **根据审核状态动态查找文件**

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
            3. **从文件名提取作品 ID，查询审核状态**
            4. **根据审核状态动态拼接文件后缀**：
               - 待审核（20）：查找 `.pend` 后缀文件
               - 未过审（30）：查找 `.fail` 后缀文件
               - 正常（10）：查找正常格式文件
            5. 构建完整文件路径并验证文件存在性
            6. 确保文件在允许的目录下（~/.pix_vision/data/works/）
            7. 设置响应头 Content-Type 和 Cache-Control
            8. 返回图片二进制数据

            ## 注意事项：
            - 该接口**无需认证**，任何人都可以访问
            - 图片会自动缓存 **1 小时**（Cache-Control: max-age=3600）
            - 支持子目录结构，建议按日期或用户分类，如：`2024/04/artwork.png`
            - **仅支持 JPG、JPEG、PNG 格式**的图片
            - 文件路径相对于 `~/.pix_vision/data/works/` 目录
            - 适用于展示用户上传的作品图片
            - **数据库存储的文件名始终为正常格式**，实际文件根据审核状态有不同的后缀
            """)
    @PublicAccess("获取作品图片，无需认证")
    @GetMapping("/work/get")
    public ResponseEntity<Resource> getWorkImage(@Parameter(description = "图像相对路径，支持子目录，如：2024/04/artwork.png", required = true, example = "artwork_001.png") @RequestParam String filePath) {
        // 根据审核状态动态查找文件
        String actualFilePath = resolveWorkFilePath(filePath);
        return getImageResource(FilePathConfig.WorksPath, actualFilePath, "作品");
    }

    /**
     * 管理员查看非公开作品图片（.fail / .pend / .del）
     *
     * @param filePath 数据库存储的作品图片文件名（正常格式，如：uuid.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(summary = "管理员查看非公开作品图片", description = """
        # 管理员查看非公开作品图片（需要登录认证 + 角色权限[55, 77]）

        ## 特性
        - Token 认证（通过拦截器自动验证）
        - 角色权限控制（仅审核员和系统管理员）
        - 路径安全校验（防目录遍历攻击）
        - 文件类型白名单（JPG/JPEG/PNG）
        - **按优先级查找非公开文件后缀**：.fail → .pend → .del → 正常
        - 适用于审核员查看违规作品、待审核作品、已删除作品

        ## 权限要求
        - **角色代码 55**：审核员
        - **角色代码 77**：系统管理员
        - 其他角色访问将返回 **403 Forbidden**

        ## 参数说明：
        - filePath: **数据库存储的作品图片文件名**，字符串类型，必填，为正常格式（如 uuid.png），不含 .fail/.pend/.del 后缀

        ## 返回说明：
        - **获取成功**：直接返回图片二进制数据，Content-Type 为 image/png 或 image/jpeg
        - **未授权**：返回 401 状态码（Token 无效或不存在）
        - **权限不足**：返回 403 状态码（角色不符合要求）
        - **文件不存在**：返回 404 Not Found（所有后缀均未找到）
        - **非法路径**：返回 400 Bad Request（包含 .. 或 / 开头）
        - **不支持的格式**：返回 400 Bad Request（非 JPG/JPEG/PNG 格式）
        - **服务器错误**：返回 500 Internal Server Error

        ## 业务逻辑：
        1. 校验文件路径安全性（禁止 .. 和绝对路径）
        2. 检查文件扩展名是否在白名单中（jpg/jpeg/png）
        3. 按优先级查找实际文件：
           - `.fail` 后缀（违规未过审作品）
           - `.pend` 后缀（待审核作品）
           - `.del` 后缀（已删除作品）
           - 正常格式（审核通过作品）
        4. 构建完整文件路径并返回图片

        ## 注意事项：
        - 该接口**需要认证**，必须在请求头中携带有效的 Token
        - **仅角色代码 55（审核员）和 77（系统管理员）**可以访问
        - 传入的 filePath 为数据库存储的正常格式文件名（如 uuid.png），**不需要**手动添加 .fail/.pend/.del 后缀
        - 接口会自动按优先级查找实际存在的文件
        - 与公开接口 `/api/image/work/get` 的区别：公开接口优先查找正常文件，本接口优先查找非公开后缀文件
        - 用于审核员在管理后台查看待审核、违规或已删除的作品图片
        """)
    @RequireRole(value = {55, 77})
    @GetMapping("/work/admin-view")
    public ResponseEntity<Resource> adminViewWorkImage(@Parameter(description = "数据库存储的作品图片文件名（正常格式，如：uuid.png）", required = true, example = "a1b2c3d4.png") @RequestParam String filePath) {
        // 按管理员优先级查找文件（.fail → .pend → .del → 正常）
        String actualFilePath = resolveAdminWorkFilePath(filePath);
        return getImageResource(FilePathConfig.WorksPath, actualFilePath, "管理员查看作品");
    }

    /**
     * 获取Logo图片
     *
     * @param filePath 图像文件名（如：dark.png、light.png）
     * @return 图片资源（二进制数据）
     * @author PlayerEG
     */
    @Operation(summary = "获取Logo图片", description = """
        # 获取 Logo 图片（无需登录认证）

        ## 特性
        - 公开接口（无需认证）
        - 路径安全校验（防目录遍历攻击）
        - 文件类型白名单（JPG/JPEG/PNG）
        - HTTP 缓存支持（1小时）
        - 静态资源访问

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
        """)
    @PublicAccess("获取Logo图片，无需认证")
    @GetMapping("/logo/get")
    public ResponseEntity<Resource> getLogo(@Parameter(description = "图像文件名，如：dark.png", required = true, example = "dark.png") @RequestParam String filePath) {
        return getImageResource(FilePathConfig.LogoPath, filePath, "Logo");
    }

    /**
     * 上传用户头像
     *
     * @param file    头像文件（JPG/JPEG/PNG 格式，最大 5MB，必须是正方形）
     * @param request HTTP 请求对象（用于获取用户 ID）
     * @return 响应结果，包含上传后的头像路径
     * @author PlayerEG
     */
    @Operation(summary = "上传用户头像", description = """
        # 上传用户头像（需要登录认证）

        ## 特性
        - Token 认证（通过拦截器自动验证）
        - 文件格式校验（JPG/JPEG/PNG）
        - 文件大小限制（最大5MB）
        - 正方形图片校验
        - 自动缩放至600x600 PNG格式
        - UUID 唯一文件名生成

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
        """)
    @PostMapping("/avatar/upload")
    public ResponseEntity<ResponsePojo<String>> uploadAvatar(@Parameter(description = "头像文件", required = true) @RequestParam MultipartFile file, HttpServletRequest request) {
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
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS)));
            }

            // 4. 验证文件大小（最大 5MB）- 在读取文件内容之前验证
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                log.warn("文件大小超出限制: {} bytes ({} MB)", file.getSize(), file.getSize() / 1024.0 / 1024.0);
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "文件大小超出限制，最大允许 5MB"));
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
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "文件不是有效的图片格式，请上传 JPG/JPEG/PNG 格式的图片"));
            }

            // 6. 验证图像是否为正方形
            if (!ImageUtils.isSquareImage(imageBytes)) {
                log.warn("图像不是正方形");
                return ResponseEntity.badRequest().body(ResponsePojo.error(null, "头像必须是正方形图片，请上传宽高相等的图片"));
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

            FileUtil.writeBytes(resizedImage, saveFile);
            log.info("头像文件保存成功: {}", savePath);

            // 10. 更新数据库中的头像路径（只保存相对路径）
            String avatarUrl = fileName; // 直接保存文件名，访问时使用 /api/get-image/avatar?filePath=xxx.png
            Boolean updateResult = userService.updateUserAvatar(userId, avatarUrl, userId);

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

    /**
     * 上传作品图片
     *
     * @param file       作品图片文件（JPG/JPEG/PNG 格式，最大 32MB）
     * @param workTitle  作品标题（最多 16 个中文字符）
     * @param seriesId   系列 ID（可选，0 表示不属于任何系列）
     * @param isOriginal 是否原创（true-原创，false-转载）
     * @param outUrl     外部转载链接（转载时必填）
     * @param request    HTTP 请求对象（用于获取用户 ID）
     * @return 响应结果，包含作品 ID
     * @author PlayerEG
     */
    @Operation(summary = "上传作品图片", description = """
        # 上传作品图片（需要登录认证 + 角色权限[22,77]）

        ## 特性
        - Token 认证（通过拦截器自动验证）
        - 角色权限控制（仅创作者和系统管理员）
        - 文件格式校验（JPG/JPEG/PNG）
        - 文件大小限制（最大 32MB）
        - 图片真实性校验（魔数检查）
        - 系列归属验证（可选，不填则不属于任何系列）
        - AI 内容安全审核（自动检测标题和转载链接）
        - 封面缩略图自动生成（400px 短边，JPG 格式）
        - 文件状态后缀管理（.pend 待审核 / .fail 违规）
        - 事务一致性保证（数据库插入失败自动回滚文件）
        - 封面生成容错降级（失败不阻塞上传）

        ## 权限要求
        - **角色代码 22**：创作者
        - **角色代码 77**：系统管理员
        - 其他角色访问将返回 **403 Forbidden**

        ## 参数说明：
        - file: **作品图片文件**，MultipartFile 类型，必填
        - workTitle: **作品标题**，字符串类型，必填，最多 16 个中文字符（48 字节）
        - seriesId: **系列 ID**，整数类型，可选，**不填或为 0 则不属于任何系列**
        - isOriginal: **是否原创**，布尔类型，必填，true-原创，false-转载
        - outUrl: **外部转载链接**，字符串类型，转载时必填，原创时可选

        ## 返回说明：
        - **上传成功（待审核）**：返回 200 状态码和 **{"data": true}**，message 为"人工审查后将直接发布"
        - **AI 审核不通过（违规）**：返回 200 状态码和 **{"data": false}**，message 为审核原因
        - **未授权**：返回 401 状态码（Token 无效或不存在）
        - **权限不足**：返回 403 状态码（角色不符合要求）
        - **文件格式不支持**：返回 400 状态码
        - **文件大小超限**：返回 400 状态码（最大 32MB）
        - **标题过长**：返回 400 状态码（超过 16 个中文字符）
        - **系列不存在**：返回 400 状态码
        - **无权操作该系列**：返回 403 状态码
        - **转载缺少链接**：返回 400 状态码
        - **服务器错误**：返回 500 状态码

        ## 业务逻辑：
        1. 从 Token 中获取当前登录用户的 ID
        2. 验证文件格式（仅支持 jpg/jpeg/png）
        3. 验证文件大小（最大 32MB）
        4. 读取上传的图片数据并验证真实性（魔数检查）
        5. 验证作品标题长度（最多 16 个中文字符 = 48 字节）
        6. 验证系列 ID（如果 > 0，则检查是否存在且属于当前用户；否则设置为 NULL）
        7. 验证转载链接（isOriginal=false 时 outUrl 必填，且需符合 URL 格式）
        8. **调用 AI 审核服务对作品内容进行安全审核**：
           - 原创作品审核格式：`作品标题: {标题}`
           - 转载作品审核格式：`作品标题: {标题}\n转载地址: {链接}`
           - 审核通过（normal）/存疑（neutral）：审核状态设为待审核（20）
           - 审核不通过（violation）：审核状态设为未过审（30），文件保存为 .fail 后缀
           - AI 服务不可用时自动降级为待审核（20）
        9. 生成唯一文件名（UUID.原始扩展名），根据审核状态附加文件后缀：
           - 待审核（20）→ `.pend` 后缀
           - 违规（30）→ `.fail` 后缀
        10. 保存原图文件到 `~/.pix_vision/data/works/` 目录
        11. **生成并保存封面缩略图**：
            - 使用 `ImageUtils.generateThumbnail()` 智能缩略，短边缩放至 400px，保持原始比例
            - 输出固定为 JPG 格式
            - 命名规则：原图 `uuid.png` → 封面 `uuid_thumb.jpg`
            - 封面文件使用与原图相同的状态后缀（.pend / .fail）
            - **封面生成失败不阻塞上传**：自动将 thumb_url 设为 NULL，记录错误日志
        12. 构建 Works 对象并插入数据库：
            - `img_url` 存储正常格式文件名（如 uuid.png），不随审核状态变化
            - `thumb_url` 存储封面文件名（如 uuid_thumb.jpg），生成失败时为 NULL
        13. 数据库插入失败时，自动删除已保存的原图和封面文件（事务回滚）
        14. 返回作品 ID、审核状态和审核原因

        ## 注意事项：
        - 该接口**需要认证**，必须在请求头中携带有效的 Token
        - **仅角色代码 22（创作者）和 77（系统管理员）**可以访问
        - **仅支持 JPG、JPEG、PNG 格式**的图片上传
        - **不要求正方形图片**，保持原始比例和尺寸，图片不会被缩放或压缩
        - 文件名使用 UUID 生成，保留原始扩展名，避免冲突
        - 文件大小限制为 **32MB**（比头像的 5MB 宽松）
        - **seriesId 可选**，不填或为 0 则作品不属于任何系列（数据库存储为 NULL）；如填写具体系列 ID，必须是自己拥有的系列
        - 转载作品必须提供有效的外部链接，原创作品的 outUrl 可为空
        - 数据库字段默认值：like_count=0, star_count=0, view_count=0
        - **AI 文案审核**在文件保存前执行，违规作品以 .fail 后缀保存，不会公开展示
        - **封面缩略图**始终为 JPG 格式，与原图格式无关；生成失败不影响上传成功
        - 原图文件和封面文件共享相同的状态后缀，审核状态变更时同步重命名
        - 数据库存储的文件名始终为正常格式（如 uuid.png、uuid_thumb.jpg），不包含状态后缀
        """)
    @RequireRole(value = {22, 77})
    @PostMapping("/work/upload")
    public ResponseEntity<ResponsePojo<Boolean>> uploadWork(@Parameter(description = "作品图片文件", required = true) @RequestParam MultipartFile file, @Parameter(description = "作品标题，最多16个中文字符", required = true, example = "春日樱花") @RequestParam String workTitle, @Parameter(description = "系列 ID（0 表示不属于任何系列）", required = false, example = "1") @RequestParam(required = false, defaultValue = "0") Integer seriesId, @Schema(description = "是否原创", allowableValues = {"true", "false"}, example = "true") @RequestParam Boolean isOriginal, @Parameter(description = "外部转载链接（仅转载时必填）", required = false, example = "https://example.com/original") @RequestParam(required = false) String outUrl, HttpServletRequest request) {
        try {
            // 1. 从 request 中获取用户 ID（由 JWT 拦截器设置）
            Integer userId = (Integer) request.getAttribute("userId");
            if (userId == null) {
                log.warn("未获取到用户 ID，请先登录");
                return ResponseEntity.status(401).body(ResponsePojo.error(false, "未授权访问：请先登录"));
            }

            // 2. 验证文件是否为空
            if (file.isEmpty()) {
                log.warn("上传的文件为空");
                return ResponseEntity.badRequest().body(ResponsePojo.error(false, "上传的文件不能为空"));
            }

            // 3. 验证文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.warn("文件名为空");
                return ResponseEntity.badRequest().body(ResponsePojo.error(false, "文件名不能为空"));
            }

            // 4. 读取文件数据
            byte[] imageBytes = file.getBytes();

            // 5. 调用 Service 层处理业务逻辑
            Integer finalSeriesId = (seriesId != null && seriesId > 0) ? seriesId : null;
            WorkUploadResult uploadResult = workService.uploadWork(userId, imageBytes, originalFilename, file.getSize(), workTitle, finalSeriesId, isOriginal, outUrl);

            Integer workId = uploadResult.getWorkId();
            Integer approvalStatus = uploadResult.getApprovalStatus();
            String auditReason = uploadResult.getAuditReason();

            // 根据审核状态返回差异化响应
            if (approvalStatus != null && approvalStatus == 30) {
                // 违规内容
                String reason = auditReason != null ? auditReason : "未知原因";
                log.warn("作品审核不通过（违规），用户 ID: {}, 作品 ID: {}, 原因: {}", userId, workId, reason);
                return ResponseEntity.ok(ResponsePojo.error(false, reason));
            }

            // 待审核（20）
            log.info("作品上传成功，待人工审核，用户 ID: {}, 作品 ID: {}", userId, workId);
            return ResponseEntity.ok(ResponsePojo.success(true, "人工审查后将直接发布"));

        } catch (IllegalArgumentException e) {
            log.error("参数验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponsePojo.error(false, e.getMessage()));
        } catch (SecurityException e) {
            log.error("权限验证失败: {}", e.getMessage());
            return ResponseEntity.status(403).body(ResponsePojo.error(false, e.getMessage()));
        } catch (Exception e) {
            log.error("作品上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ResponsePojo.error(false, "作品上传失败: " + e.getMessage()));
        }
    }

    // ================================================================================

    /**
     * 通用图片资源获取方法
     *
     * @param basePath  基础路径
     * @param filePath  文件相对路径(可包含子目录)
     * @param imageType 图片类型(用于日志)
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

            // 2. 剥离作品文件状态后缀，获取真实图片扩展名并检查白名单
            String realExtension = getRealImageExtension(filePath);
            if (!ALLOWED_EXTENSIONS.contains(realExtension.toLowerCase())) {
                log.warn("不允许的文件类型: {}, 文件路径: {}", realExtension, filePath);
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
            String contentType = getContentType(realExtension);
            log.info("返回{}图片: {}, 文件路径: {}", imageType, filePath, fullPath);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).header(HttpHeaders.CACHE_CONTROL, "max-age=3600")  // 缓存1小时
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
        String requestUri = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping", 0).toString();

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
     * 获取文件真实图片扩展名，剥离作品状态后缀
     * <p>
     * 作品文件可能带有状态后缀（如 uuid.png.fail、uuid.png.pend、uuid.png.del），
     * 本方法先剥离这些后缀，再提取真实的图片扩展名（如 png、jpg）。
     * <p>
     * 对于不含状态后缀的普通文件，行为与 {@link #getFileExtension(String)} 一致。
     *
     * @param filename 文件名（可能含有 .fail/.pend/.del 后缀）
     * @return 真实图片扩展名（如 png、jpg、jpeg），不含点
     * @author PlayerEG
     * @see #getFileExtension(String)
     */
    private String getRealImageExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String suffix : WORK_FILE_SUFFIXES) {
            if (lowerFilename.endsWith(suffix)) {
                // 剥离状态后缀后提取图片扩展名
                String stripped = filename.substring(0, filename.length() - suffix.length());
                return getFileExtension(stripped);
            }
        }
        return getFileExtension(filename);
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

    /**
     * 根据作品文件名和审核状态动态解析实际文件路径
     * <p>
     * 数据库存储的文件名始终为正常格式（如 uuid.png），但实际文件根据审核状态有不同的后缀：
     * - 待审核（approval_status = 20）：文件名为 uuid.png.pend
     * - 未过审（approval_status = 30）：文件名为 uuid.png.fail
     * - 正常（approval_status = 10）：文件名为 uuid.png
     *
     * @param filePath 数据库存储的文件名（正常格式）
     * @return 实际文件路径（根据审核状态添加相应后缀）
     * @author PlayerEG
     */
    private String resolveWorkFilePath(String filePath) {
        try {
            // 从文件名提取作品 ID（假设文件名格式为 uuid.ext 或 uuid.ext.suffix）
            // 由于无法直接从文件名获取作品 ID，我们需要通过 img_url 反查作品
            // 这里采用简化方案：直接查询所有作品中 img_url 匹配的记录

            // 注意：filePath 可能包含子目录，如 "2024/04/artwork.png"
            // 我们需要提取纯文件名部分进行匹配
            String pureFileName = filePath;
            int lastSlashIndex = filePath.lastIndexOf("/");
            if (lastSlashIndex >= 0) {
                pureFileName = filePath.substring(lastSlashIndex + 1);
            }

            // 查询作品信息（需要自定义 SQL 来根据 img_url 查询）
            // 由于当前 Mapper 没有这个方法，我们采用另一种策略：
            // 直接尝试不同的文件后缀，按优先级查找

            // 优先级：正常文件 > .pend 文件 > .fail 文件
            String[] possiblePaths = {filePath,                    // 正常格式（审核通过）
                filePath + ".pend",         // 待审核
                filePath + ".fail"          // 未过审
            };

            for (String possiblePath : possiblePaths) {
                Path fullPath = Paths.get(FilePathConfig.WorksPath, possiblePath).normalize();
                File file = fullPath.toFile();
                if (file.exists() && file.isFile()) {
                    log.debug("找到作品文件: {}, 路径: {}", filePath, possiblePath);
                    return possiblePath;
                }
            }

            // 如果都没找到，返回原始路径（让后续逻辑处理 404）
            log.warn("未找到作品文件: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("解析作品文件路径异常: {}, 错误: {}", filePath, e.getMessage());
            return filePath; // 发生异常时返回原始路径
        }
    }

    /**
     * 管理员视角解析作品文件路径（优先查找非公开后缀文件）
     * <p>
     * 与公开接口 {@link #resolveWorkFilePath(String)} 不同，本方法优先查找非公开后缀的文件：
     * <ol>
     *   <li>.fail - 违规未过审作品（approval_status = 30）</li>
     *   <li>.pend - 待审核作品（approval_status = 20）</li>
     *   <li>.del - 已删除作品（is_delete = 1）</li>
     *   <li>正常格式 - 审核通过作品（approval_status = 10）</li>
     * </ol>
     * <p>
     * 用于管理员在后台审核流程中查看各种状态的作品图片
     *
     * @param filePath 数据库存储的文件名（正常格式）
     * @return 实际文件路径（根据文件系统中存在的文件添加相应后缀）
     * @author PlayerEG
     * @see #resolveWorkFilePath(String)
     */
    private String resolveAdminWorkFilePath(String filePath) {
        try {
            // 管理员优先级：.fail → .pend → .del → 正常格式
            String[] possiblePaths = {filePath + ".fail", filePath + ".pend", filePath + ".del", filePath};

            for (String possiblePath : possiblePaths) {
                Path fullPath = Paths.get(FilePathConfig.WorksPath, possiblePath).normalize();
                File file = fullPath.toFile();
                if (file.exists() && file.isFile()) {
                    log.debug("管理员查看作品文件: {}, 实际路径: {}", filePath, possiblePath);
                    return possiblePath;
                }
            }

            log.warn("管理员未找到作品文件: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("管理员解析作品文件路径异常: {}, 错误: {}", filePath, e.getMessage());
            return filePath;
        }
    }
}
