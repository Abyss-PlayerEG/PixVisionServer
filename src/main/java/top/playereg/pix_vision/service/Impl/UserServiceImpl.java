package top.playereg.pix_vision.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.playereg.pix_vision.mapper.UserMapper;
import top.playereg.pix_vision.pojo.User;
import top.playereg.pix_vision.service.UserService;
import top.playereg.pix_vision.util.StrSwitchUtils;

import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     */
//    @Override
//    public int registerUser(
//            String username,
//            String password,
//            String nickname,
//            String email
//    ) {
//        User user = new User();
//
//        user.setUsername(username);
//        user.setPassword(password);
//        user.setNickname(nickname);
//        user.setEmail(email);
//
//        user.setUser_uuid(StrSwitchUtils.generateUUID().getBytes(StandardCharsets.UTF_8));
//    }

}
