package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.userPojo.UserData;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.TokenUtils;

import java.util.List;

/**
 * 用户拓展数据管理相关接口（增删查）
 *
 * @author PlayerEG
 * @see top.playereg.pix_vision.service.Impl.UserServiceImpl
 */
@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user/data")
@RequiredArgsConstructor
@Tag(name = "用户拓展数据管理相关接口")
public class UserDataController {
    private static final Logger log = LoggerFactory.getLogger(UserDataController.class);

    private final UserService userService;
    private final TokenWhitelistService tokenWhitelistService;

    /**
     * 新增用户拓展数据
     *
     * @param request     HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param dataName    数据名称（电话、邮箱、网站、微信等）
     * @param dataContent 数据内容（具体的电话号码、邮箱地址、网站 url 等）
     * @return 添加结果
     * @author PlayerEG
     */
    @PostMapping("/add")
    @Operation(
        summary = "新增用户拓展数据接口",
        description = """
            # 新增用户拓展数据（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 数据长度限制校验（名称≤26字符，内容≤96字符）
            - 1对n关系（同一用户可添加多条拓展数据）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - dataName: 数据名称，字符串类型，必填，长度不超过 26 个字符（如：电话、邮箱、网站、微信等）
            - dataContent: 数据内容，字符串类型，必填，长度不超过 96 个字符（如：具体的电话号码、邮箱地址、网站 url 等）

            ## 返回说明：
            - **添加成功**：返回 **{"data": true}** 和"用户拓展数据添加成功"提示
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **数据名称为空**：返回 **{"data": null}** 和"数据名称不能为空"提示
            - **数据名称过长**：返回 **{"data": null}** 和"数据名称长度不能超过 26 个字符"提示
            - **数据内容为空**：返回 **{"data": null}** 和"数据内容不能为空"提示
            - **数据内容过长**：返回 **{"data": null}** 和"数据内容长度不能超过 96 个字符"提示
            - **添加失败**：返回 **{"data": false}** 和"用户拓展数据添加失败"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验数据名称和数据内容参数（非空、长度限制）
            5. 检查用户是否存在
            6. 调用服务层新增用户拓展数据
            7. 返回添加结果

            ## 注意事项：
            - 需要携带有效的 Token 才能添加拓展数据
            - Token 必须在白名单中（未过期、未登出）
            - 数据名称长度限制：**不超过 26 个字符**
            - 数据内容长度限制：**不超过 96 个字符**
            - 同一用户可以添加多条拓展数据（1 对 n 关系）
            - 常见的数据名称示例：电话、邮箱、网站、微信、QQ 等
            """
    )
    public ResponsePojo<Boolean> addUserData(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "数据名称，长度不超过 26 个字符", required = true, example = "电话") @RequestParam String dataName,
        @Parameter(description = "数据内容，长度不超过 96 个字符", required = true, example = "13800138000") @RequestParam String dataContent
    ) {
        // 提取 Token
        String token = TokenUtils.extractTokenWithLog(request, "新增用户拓展数据接口");

        if (token == null || token.isEmpty()) {
            log.error("新增用户拓展数据失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        log.info("开始新增用户拓展数据，用户 ID: {}, 用户名: {}, 数据名称: {}, 数据内容: {}", userId, username, dataName, dataContent);

        // 校验数据名称参数
        if (dataName == null || dataName.isEmpty()) {
            log.warn("数据名称为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "数据名称不能为空");
        }

        if (dataName.length() > 26) {
            log.warn("数据名称长度不符合要求，用户 ID: {}, 数据名称长度: {}", userId, dataName.length());
            return ResponsePojo.error(null, "数据名称长度不能超过 26 个字符");
        }

        // 校验数据内容参数
        if (dataContent == null || dataContent.isEmpty()) {
            log.warn("数据内容为空，用户 ID: {}", userId);
            return ResponsePojo.error(null, "数据内容不能为空");
        }

        if (dataContent.length() > 96) {
            log.warn("数据内容长度不符合要求，用户 ID: {}, 数据内容长度: {}", userId, dataContent.length());
            return ResponsePojo.error(null, "数据内容长度不能超过 96 个字符");
        }

        // 调用服务层新增用户拓展数据
        Boolean result = userService.addUserData(userId, dataName, dataContent);

        if (result) {
            log.info("用户拓展数据添加成功，用户 ID: {}, 用户名: {}, 数据名称: {}", userId, username, dataName);
            return ResponsePojo.success(true, "用户拓展数据添加成功");
        } else {
            log.error("用户拓展数据添加失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "用户拓展数据添加失败");
        }
    }

    /**
     * 查询用户所有拓展数据（公开接口）
     *
     * @param userId 用户 ID
     * @return 响应数据<List < UserData>>，包含用户的所有拓展数据列表
     * @author PlayerEG
     */
    @GetMapping("/list")
    @Operation(
        summary = "查询用户所有拓展数据接口",
        description = """
            # 查询用户所有拓展数据（无需登录验证）

            ## 特性
            - 公开接口（无需 Token 认证）
            - 自动过滤逻辑删除数据
            - 按创建时间倒序排列

            ## 参数说明：
            - userId: 用户 ID，Integer 类型，必填

            ## 返回说明：
            - **查询成功**：返回 **{"data": [UserData 列表]}** ，包含用户的所有拓展数据
            - **用户 ID 无效**：返回 **{"data": null}** 和"用户 ID 无效"提示
            - **用户不存在**：返回 **{"data": null}** 和"用户不存在"提示
            - **查询失败**：返回 **{"data": null}** 和"查询失败"提示

            ## 返回数据结构：
            ```json
            {
              "code": 200,
              "data": [
                {
                  "data_id": 2,
                  "user_id": 1,
                  "user_data_name": "微信",
                  "user_data": "wx_test_user"
                },
                {
                  "data_id": 1,
                  "user_id": 1,
                  "user_data_name": "电话",
                  "user_data": "13800138000"
                }
              ],
              "message": "查询成功"
            }
            ```

            ## 业务逻辑：
            1. 校验用户 ID 参数有效性
            2. 检查用户是否存在
            3. 查询用户的所有拓展数据（自动排除逻辑删除的数据）
            4. 按创建时间倒序返回结果
            5. 返回拓展数据列表

            ## 注意事项：
            - **此接口为公开接口，无需登录即可访问**
            - 自动过滤已逻辑删除的数据（is_delete=0）
            - 返回结果按创建时间倒序排列（最新的在前）
            - 如果用户没有拓展数据，返回空列表 []
            - 常见的数据名称示例：电话、邮箱、网站、微信、QQ 等
            """
    )
    public ResponsePojo<List<UserData>> getUserDataList(
        @Parameter(description = "用户 ID", required = true, example = "1") @RequestParam Integer userId
    ) {
        // 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户 ID 无效: {}", userId);
            return ResponsePojo.error(null, "用户 ID 无效");
        }

        log.info("开始查询用户拓展数据，用户 ID: {}", userId);

        // 调用服务层查询用户拓展数据
        List<UserData> userDataList = userService.getUserDataList(userId);

        // 用户不存在
        if (userDataList == null) {
            log.warn("用户不存在，用户 ID: {}", userId);
            return ResponsePojo.error(null, "用户不存在");
        }

        log.info("查询用户拓展数据成功，用户 ID: {}, 数据条数: {}", userId, userDataList.size());
        return ResponsePojo.success(userDataList, "查询成功");
    }

    /**
     * 删除用户拓展数据（支持单条和批量删除，需要登录）
     *
     * @param request HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token
     * @param dataIds 要删除的数据 ID 列表（支持单个或多个）
     * @return 删除结果
     * @author PlayerEG
     */
    @PostMapping("/delete")
    @Operation(
        summary = "删除用户拓展数据接口",
        description = """
            # 删除用户拓展数据（需要登录认证）

            ## 特性
            - Token 认证（支持 Header 和 URL 参数两种方式）
            - 支持单条/批量删除
            - SQL 层面权限验证（只能删除自己的数据）
            - 逻辑删除（数据不真正从数据库移除）

            ## 参数说明：
            - Authorization: Header 中的 Token，格式为 `Bearer <token>`，或通过 URL 参数 `?token=<token>` 传递
            - dataIds: 要删除的数据 ID 列表，整形数组类型，必填
              * 单条删除：传入 [1]
              * 批量删除：传入 [1, 2, 3]

            ## 返回说明：
            - **删除成功**：返回 **{"data": true}** 和"用户拓展数据删除成功"提示
            - **Token 不存在**：返回 **{"data": null}** 和"Token 不存在"提示
            - **Token 已失效**：返回 **{"data": null}** 和"Token 已失效"提示
            - **数据 ID 列表为空**：返回 **{"data": false}** 和"数据 ID 列表不能为空"提示
            - **无权删除**：返回 **{"data": false}** 和"部分或全部数据无权删除"提示（数据不属于当前用户）
            - **删除失败**：返回 **{"data": false}** 和"用户拓展数据删除失败"提示

            ## 业务逻辑：
            1. 从请求头或 URL 参数中提取 Token（支持 Bearer 前缀）
            2. 验证 Token 是否在白名单中
            3. 从 Token 中解析用户 ID
            4. 校验数据 ID 列表参数有效性
            5. 检查用户是否存在
            6. 执行批量逻辑删除（SQL 层面验证 user_id，确保只能删除自己的数据）
            7. 返回删除结果

            ## 注意事项：
            - **需要携带有效的 Token 才能删除拓展数据**
            - Token 必须在白名单中（未过期、未登出）
            - **用户只能删除自己的拓展数据**，无法删除他人的数据
            - 采用逻辑删除方式，数据不会真正从数据库中移除
            - 如果部分数据不属于当前用户，只会删除属于当前用户的数据
            - 删除后，这些数据在查询接口中将不再显示
            - 建议单次批量删除不超过 100 条数据
            - 单条删除时传入单个元素的数组，如 [1]
            """
    )
    public ResponsePojo<Boolean> deleteUserData(
        @Parameter(description = "HTTP 请求对象，用于从 Header 或 URL 参数中获取 Token", required = true) HttpServletRequest request,
        @Parameter(description = "要删除的数据 ID 列表（支持单条或多条）", required = true, example = "[1, 2, 3]") @RequestParam List<Integer> dataIds
    ) {
        // 提取 Token
        String token = TokenUtils.extractTokenWithLog(request, "删除用户拓展数据接口");

        if (token == null || token.isEmpty()) {
            log.error("删除用户拓展数据失败 - Token 不存在");
            return ResponsePojo.error(null, "Token 不存在，请在 Header 中添加 Authorization: Bearer <token> 或在 URL 参数中添加 ?token=<token>");
        }

        // 检查 Token 是否在白名单中
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("Token 不在白名单中，可能已过期或被移除");
            return ResponsePojo.error(null, "Token 已失效");
        }

        // 从 Token 中获取用户 ID
        Integer userId = JWTUtils.getUserIdFromToken(token);
        if (userId == null) {
            log.error("从 Token 中解析用户 ID 失败");
            return ResponsePojo.error(null, "Token 无效");
        }

        String username = JWTUtils.getUsernameFromToken(token);
        int dataCount = dataIds != null ? dataIds.size() : 0;
        log.info("开始删除用户拓展数据，用户 ID: {}, 用户名: {}, 数据 ID 数量: {}", userId, username, dataCount);

        // 校验数据 ID 列表参数
        if (dataIds == null || dataIds.isEmpty()) {
            log.warn("数据 ID 列表为空，用户 ID: {}", userId);
            return ResponsePojo.error(false, "数据 ID 列表不能为空");
        }

        // 调用服务层批量删除用户拓展数据（统一使用批量方法）
        Boolean result = userService.batchDeleteUserData(dataIds, userId);

        if (result) {
            String successMsg = dataCount == 1 ? "用户拓展数据删除成功" : "批量删除用户拓展数据成功";
            log.info("{}，用户 ID: {}, 用户名: {}, 删除数量: {}", successMsg, userId, username, dataCount);
            return ResponsePojo.success(true, successMsg);
        } else {
            log.warn("用户拓展数据删除失败，用户 ID: {}, 用户名: {}", userId, username);
            return ResponsePojo.error(false, "用户拓展数据删除失败");
        }
    }
}
