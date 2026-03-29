package top.playereg.pix_vision.service;

import top.playereg.pix_vision.pojo.User;

public interface UserService {
    User registerUser(
            String username,
            String password,
            String nickname,
            String email
    );
    User selectUserByUsername(String username);
    User selectUserByEmail(String email);
}
