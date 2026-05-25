package top.playereg.pix_vision.service;

/**
 * 验证码邮件发送结果封装类
 *
 * @author PlayerEG
 */
public interface EmailService {

    /**
     * 发送 HTML 邮件
     *
     * @param to         收件人邮箱
     * @param subject    邮件主题
     * @param htmlContent HTML 内容
     * @return 发送结果，成功返回 "SUCCESS"
     */
    String sendEMail(String to, String subject, String htmlContent);

    /**
     * 批量发送邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param tos     收件人列表
     * @return 发送结果
     * @deprecated 系统设计暂不支持群发邮件
     */
    @Deprecated
    String sendMailToMany(String subject, String content, String... tos);

    /**
     * 生成验证码并发送验证邮件（一站式方法）
     * <p>
     * 封装了验证码发送的完整流程：检查已存在验证码 → 生成验证码 → 渲染邮件模板 → 发送邮件 → 存入 Redis。
     * 替代 Controller 中分散的多个 Service 调用，减少重复代码。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>注册/登录/改密等所有需要发送邮箱验证码的场景</li>
     *   <li>Controller 层只需调用此方法即可完成验证码邮件的完整发送流程</li>
     * </ol>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>如果邮箱已有未过期的验证码，返回 {@link VerificationEmailResult#existingCode}</li>
     *   <li>如果邮件发送失败，返回 {@link VerificationEmailResult#failure}</li>
     *   <li>发送成功时，验证码已自动存入 Redis（5 分钟有效期）</li>
     * </ul>
     *
     * @param email        收件人邮箱
     * @param username     用户名（用于邮件模板中的称呼）
     * @param action       操作类型（如"注册验证"、"登录验证"、"修改密码"等）
     * @param emailSubject 邮件主题（如"PixVision 注册验证码"）
     * @return 发送结果，通过 {@link VerificationEmailResult#isSuccess()} 判断成功与否
     * @author PlayerEG
     */
    VerificationEmailResult sendVerificationEmail(String email, String username, String action, String emailSubject);

    /**
     * 验证码邮件发送结果
     * <p>
     * 封装验证码邮件发送的完整结果，包括成功状态、已存在验证码的剩余秒数和错误信息。
     * </p>
     *
     * <h3>使用场景</h3>
     * <ol>
     *   <li>验证码已存在时，通过 {@link #getExistingCodeRemainingSeconds()} 获取剩余秒数</li>
     *   <li>邮件发送失败时，通过 {@link #getErrorMessage()} 获取失败原因</li>
     *   <li>发送成功时，通过 {@link #isSuccess()} 判断</li>
     * </ol>
     *
     * @author PlayerEG
     */
    class VerificationEmailResult {
        private final boolean success;
        private final Long existingCodeRemainingSeconds;
        private final String errorMessage;

        private VerificationEmailResult(boolean success, Long existingCodeRemainingSeconds, String errorMessage) {
            this.success = success;
            this.existingCodeRemainingSeconds = existingCodeRemainingSeconds;
            this.errorMessage = errorMessage;
        }

        /**
         * 创建发送成功结果
         */
        public static VerificationEmailResult success() {
            return new VerificationEmailResult(true, null, null);
        }

        /**
         * 创建验证码已存在的结果
         *
         * @param remainingSeconds 剩余秒数
         */
        public static VerificationEmailResult existingCode(Long remainingSeconds) {
            return new VerificationEmailResult(false, remainingSeconds, null);
        }

        /**
         * 创建发送失败的结果
         *
         * @param errorMessage 错误信息
         */
        public static VerificationEmailResult failure(String errorMessage) {
            return new VerificationEmailResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Long getExistingCodeRemainingSeconds() {
            return existingCodeRemainingSeconds;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
