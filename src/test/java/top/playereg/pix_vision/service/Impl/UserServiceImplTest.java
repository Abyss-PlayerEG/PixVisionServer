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

    @Test
    void getUserDataList() {
        // 测试查询用户拓展数据（公开接口，直接传入 userId）
        java.util.List<top.playereg.pix_vision.pojo.userPojo.UserData> dataList = userService.getUserDataList(1);
        if (dataList != null) {
            log.info("查询到 {} 条拓展数据", dataList.size());
            for (top.playereg.pix_vision.pojo.userPojo.UserData data : dataList) {
                log.info("数据 ID: {}, 数据名称: {}, 数据内容: {}",
                    data.getData_id(), data.getUser_data_name(), data.getUser_data());
            }
        } else {
            log.warn("用户不存在或查询失败");
        }
    }

    @Test
    void deleteUserData() {
        // 测试删除用户拓展数据（需要验证权限）
        Boolean result = userService.deleteUserData(1, 1); // 删除数据 ID 为 1 的数据，用户 ID 为 1
        log.info("删除结果：{}", result);

        // 测试删除不属于自己的数据（应该失败）
        Boolean result2 = userService.deleteUserData(2, 999); // 尝试用用户 999 删除数据 2
        log.info("删除他人数据结果（应为 false）：{}", result2);
    }
}
