package top.playereg.pix_vision.service.Impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.pojo.userPojo.User;
import top.playereg.pix_vision.service.UserService;

@SpringBootTest
class UserServiceImplTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImplTest.class);
    @Autowired
    private UserService userService;

    @Test
    void registerUser() {
        User user = userService.registerUser("dev-test2", "password-test2", "老李", "email2@dev.com");
        log.info("用户信息：{}", user);
    }

    @Test
    void selectAllUserByUsername() {
        User user = userService.selectAllUserByUsername("dev_user_2");
        log.info("用户信息：{}", user);
    }

    @Test
    void selectAllUserByEmail() {
        User user = userService.selectAllUserByEmail("test3@example.com");
        log.info("用户信息：{}", user);
    }

    @Test
    void addUserData() {
        // 测试新增用户拓展数据
        Boolean result = userService.addUserData(1, "电话", "13800138000");
        log.info("添加结果：{}", result);

        Boolean result2 = userService.addUserData(1, "微信", "wx_test_user");
        log.info("添加结果2：{}", result2);
    }
}