import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import top.playereg.pix_vision.PixVisionApplication;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = PixVisionApplication.class)
public class RedisTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testString() {
        // String存储
        String key = "test:String"; // 用户ID
        redisTemplate.opsForValue().set(
                key, // key
                UUID.randomUUID().toString(), // value
                1, // 过期时间
                TimeUnit.MINUTES // 时间单位
        );
        System.out.println(redisTemplate.opsForValue().get(key));
    }

    @Test
    public void testString2() {
        // 计数器
        String key = "test:String2";
        redisTemplate.opsForValue().increment(key);
        System.out.println(redisTemplate.opsForValue().get(key));
    }

    @Test
    public void testString3() {
        // 存储对象
        Map<String, Object> user = new HashMap<>();
        user.put("name", "playereg");
        user.put("age", 18);
        user.put("sex", "男");
        redisTemplate.opsForValue().set(
                "test:String3",
                user,
                5,
                TimeUnit.MINUTES
        );
        System.out.println(redisTemplate.opsForValue().get("test:String3")); // 从Redis中获取对象
    }

    @Test
    public void testHash() {
        String key = "test:Hash";
        Map<String, Object> shoppingCart = new HashMap<>();
        shoppingCart.put("cartId", "123456");
        shoppingCart.put("userId", "playereg");
        // 修复：使用Arrays.asList替代List.of以兼容较低Java版本，并添加使用items的代码
        List<Map<String, Object>> items = Arrays.asList(
                new HashMap<String, Object>() {{
                    put("itemId", "1");
                    put("itemName", "商品1");
                }},
                new HashMap<String, Object>() {{
                    put("itemId", "2");
                    put("itemName", "商品2");
                }},
                new HashMap<String, Object>() {{
                    put("itemId", "3");
                    put("itemName", "商品3");
                }}
        );
        shoppingCart.put("items", items); // 添加items字段
        redisTemplate.opsForHash().putAll(key, shoppingCart); // 存储购物车信息
        System.out.println(redisTemplate.opsForHash().get(key, "items")); // 获取购物车信息
    }

    @Test
    public void testSet() {
        // Set存储无序、不重复的元素
        String key = "test:Set";
        redisTemplate.opsForSet().add(key, "小明", "小王", "小李", "小张", "张三", "李四", "王五");
        System.out.println(redisTemplate.opsForSet().members(key)); // 获取所有元素
        System.out.println(redisTemplate.opsForSet().size(key)); // 获取元素个数
    }

    @Test
    public void testZset() {
        // ZSet存储有序、不重复的元素
        String key = "test:Zset";
        redisTemplate.opsForZSet().add(key, "小明", System.currentTimeMillis()); // 键、值、优先级
        redisTemplate.opsForZSet().add(key, "小王", System.currentTimeMillis());
        redisTemplate.opsForZSet().add(key, "小李", System.currentTimeMillis());
        redisTemplate.opsForZSet().add(key, "小张", System.currentTimeMillis());
        System.out.println(redisTemplate.opsForZSet().reverseRange(key, 0, -1)); // 获取所有元素
    }

    @Test
    public void testList() {
        // List存储有序、可重复的元素
        String key = "test:List";
        redisTemplate.opsForList().rightPush(key, "小明");
        redisTemplate.opsForList().rightPush(key, "小王");
        redisTemplate.opsForList().rightPush(key, "小李");
        redisTemplate.opsForList().rightPush(key, "小张");
        redisTemplate.opsForList().rightPush(key, "小刘");
    }
    @Test
    public void testList2() {
        String key = "test:List";
        int size = Math.toIntExact(redisTemplate.opsForList().size(key));
        for (int i = 0 ; i < size; i++) {
            System.out.println(redisTemplate.opsForList().leftPop(key));
        }
    }
}