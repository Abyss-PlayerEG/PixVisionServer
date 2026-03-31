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
    void selectUserByUsername() {
        User user = userService.selectUserByUsername("dev_user_2");
        log.info("用户信息：{}", user);
    }

    @Test
    void selectUserByEmail() {
        User user = userService.selectUserByEmail("test3@example.com");
        log.info("用户信息：{}", user);
    }
}