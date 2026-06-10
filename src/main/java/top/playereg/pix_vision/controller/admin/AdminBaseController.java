package top.playereg.pix_vision.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import top.playereg.pix_vision.service.TokenWhitelistService;
import top.playereg.pix_vision.util.JWTUtils;
import top.playereg.pix_vision.util.PixVisionLogger;

/**
 * 管理端控制器基类
 * <p>
 * 抽取管理端控制器的公共方法，减少代码重复
 * </p>
 *
 * @author PlayerEG
 * @since DevBeta-3.4
 */
public abstract class AdminBaseController {

    protected final PixVisionLogger log = PixVisionLogger.create(getClass());

    /**
     * 统一 Token 验证流程
     * <p>
     * 从请求中提取 Token，验证白名单，提取管理员 ID
     * </p>
     *
     * @param request        HTTP 请求对象
     * @param eventDesc      操作描述（用于日志）
     * @param tokenWhitelistService Token 白名单服务
     * @return 管理员 ID，验证失败返回 null
     */
    protected Integer validateToken(HttpServletRequest request, String eventDesc,
                                    TokenWhitelistService tokenWhitelistService) {
        String token = JWTUtils.extractTokenWithLog(request, eventDesc);
        if (token == null || token.isEmpty()) {
            log.error("{}失败 - Token不存在", eventDesc);
            return null;
        }
        if (!tokenWhitelistService.isInWhitelist(token)) {
            log.warn("{}失败 - Token不在白名单中，可能已过期或被移除", eventDesc);
            return null;
        }
        Integer adminId = JWTUtils.getUserIdFromToken(token);
        if (adminId == null) {
            log.error("{}失败 - 无法从Token中获取管理员ID", eventDesc);
            return null;
        }
        return adminId;
    }

    /**
     * 获取审核状态名称
     *
     * @param approvalStatus 审核状态代码
     * @return 状态名称
     */
    protected String getStatusName(Integer approvalStatus) {
        return switch (approvalStatus) {
            case 10 -> "正常";
            case 20 -> "待审核";
            case 30 -> "未过审";
            default -> "未知";
        };
    }
}
