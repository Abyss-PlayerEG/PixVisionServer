package top.playereg.pix_vision.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.playereg.pix_vision.pojo.ResponsePojo;
import top.playereg.pix_vision.pojo.User;
import top.playereg.pix_vision.service.UserService;

@RestController
@SuppressWarnings("all")
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户操作相关接口")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponsePojo<User> registerUser(
            @Parameter(description = "用户名", required = true, example = "dev-username") @RequestParam String username,
            @Parameter(description = "密码", required = true, example = "dev-password") @RequestParam String password,
            @Parameter(description = "昵称", required = true, example = "dev-nickname") @RequestParam String nickname,
            @Parameter(description = "邮箱", required = true, example = "dev-email") @RequestParam String email
    ){
        return null;
    }
}
