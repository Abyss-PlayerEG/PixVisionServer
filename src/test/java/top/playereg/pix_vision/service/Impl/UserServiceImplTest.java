package top.playereg.pix_vision.service.Impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.playereg.pix_vision.pojo.User;
import top.playereg.pix_vision.service.UserService;

import static org.junit.jupiter.api.Assertions.*;

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
}