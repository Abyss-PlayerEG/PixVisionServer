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

    /**
     * 优先排列的标签权重数组
     * 索引越小，位置越靠前。
     */
    private static final String[] PRIORITY_FIRST = {
        "认证接口",
        "图像接口",
        "邮件服务接口"
    };

    /**
     * 最后排列的标签权重数组
     * 索引越小，在尾部区域越靠前。
     */
    private static final String[] PRIORITY_LAST = {
        "用户接口",
        "系统管理员接口",
        "JWT 鉴权测试接口",
        "ServerRoot"
    };

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null && !openApi.getTags().isEmpty()) {
                List<Tag> tags = openApi.getTags();
                tags.sort(this::compareTags);
                log.debug("已对分组中的 {} 个标签完成排序", tags.size());
            }
        };
    }

    /**
     * 比较两个 Tag 的排序优先级
     */
    private int compareTags(Tag t1, Tag t2) {
        String name1 = t1.getName();
        String name2 = t2.getName();

        // 1. 获取详细权重信息
        WeightInfo w1 = getWeightInfo(name1);
        WeightInfo w2 = getWeightInfo(name2);

        // 2. 比较区域优先级 (FIRST=0, MIDDLE=1, LAST=2)
        if (w1.area != w2.area) {
            return Integer.compare(w1.area, w2.area);
        }

        // 3. 比较数组索引
        if (w1.index != w2.index) {
            // 如果其中一个没匹配到(index=-1)，则按字符类型排；否则按索引排
            if (w1.index == -1 || w2.index == -1) {
                // 保持原有的中间区域排序逻辑
                int typeCompare = Integer.compare(getCharType(name1), getCharType(name2));
                if (typeCompare != 0) return typeCompare;
                return name1.compareTo(name2);
            }
            return Integer.compare(w1.index, w2.index);
        }

        // 4. 如果索引相同（说明匹配了同一个通配符模式），比较通配符部分的内容
        if (w1.wildcardContent != null && w2.wildcardContent != null) {
            int wcCompare = w1.wildcardContent.compareTo(w2.wildcardContent);
            if (wcCompare != 0) return wcCompare;
        }

        // 5. 最后按全名自然排序
        return name1.compareTo(name2);
    }

    /**
     * 获取标签的详细权重信息
     */
    private WeightInfo getWeightInfo(String tagName) {
        if (tagName == null) return new WeightInfo(1, -1, null);

        // 检查优先数组
        WeightInfo first = findInArray(tagName, PRIORITY_FIRST, 0);
        if (first != null) return first;

        // 检查最后数组
        WeightInfo last = findInArray(tagName, PRIORITY_LAST, 2);
        if (last != null) return last;

        // 中间区域
        return new WeightInfo(1, -1, null);
    }

    /**
     * 在指定数组中查找匹配项（前缀匹配）
     */
    private WeightInfo findInArray(String tagName, String[] array, int area) {
        for (int i = 0; i < array.length; i++) {
            String prefix = array[i];
            // 只要标签名以配置的前缀开头，就认为匹配成功
            if (tagName.startsWith(prefix)) {
                return new WeightInfo(area, i, null);
            }
        }
        return null;
    }

    /**
     * 权重信息内部类
     */
    private static class WeightInfo {
        int area; // 0: FIRST, 1: MIDDLE, 2: LAST
        int index; // 数组中的索引
        String wildcardContent; // 通配符匹配到的具体内容

        WeightInfo(int area, int index, String wildcardContent) {
            this.area = area;
            this.index = index;
            this.wildcardContent = wildcardContent;
        }
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
