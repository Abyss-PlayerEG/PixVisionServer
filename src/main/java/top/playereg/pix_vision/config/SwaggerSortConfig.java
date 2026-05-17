package top.playereg.pix_vision.config;

import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.playereg.pix_vision.util.PixVisionLogger;

import java.util.List;

/**
 * Swagger 接口文档全局排序配置
 * <p>
 * 通过 GlobalOpenApiCustomizer 对所有分组（包括默认分组和自定义分组）的 Tags 进行排序。
 * 排序规则：数字开头优先 > 英文/字母开头次之 > 中文/其他开头最后
 * </p>
 *
 * @author PlayerEG
 */
@Configuration
public class SwaggerSortConfig {
    private static final PixVisionLogger log = PixVisionLogger.create(SwaggerSortConfig.class);

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null && !openApi.getTags().isEmpty()) {
                List<Tag> tags = openApi.getTags();
                // 执行排序
                tags.sort(this::compareTags);
                log.debug("已对分组中的 {} 个标签完成排序", tags.size());
            }
        };
    }

    /**
     * 比较两个 Tag 的排序优先级
     *
     * @param t1 第一个 Tag
     * @param t2 第二个 Tag
     * @return 排序结果
     */
    private int compareTags(Tag t1, Tag t2) {
        String name1 = t1.getName();
        String name2 = t2.getName();

        int type1 = getCharType(name1);
        int type2 = getCharType(name2);

        // 1. 按类型排序：数字(0) > 英文(1) > 中文/其他(2)
        if (type1 != type2) {
            return Integer.compare(type1, type2);
        }

        // 2. 同类型内按自然顺序排序
        return name1.compareTo(name2);
    }

    /**
     * 获取字符串首字符的类型代码
     *
     * @param text 输入文本
     * @return 0-数字, 1-英文/字母, 2-中文/其他
     */
    private int getCharType(String text) {
        if (text == null || text.isEmpty()) return 2;
        char c = text.charAt(0);
        // 检查是否为 ASCII 数字
        if (c >= '0' && c <= '9') return 0;
        // 检查是否为 ASCII 字母或英文字符
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return 1;
        // 其他情况（包括中文）归为第三类
        return 2;
    }
}
