package top.playereg.pix_vision.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
class EmailServiceImplTest {

    @Resource
    EmailServiceImpl EmailServiceImpl;

    @Test
    public void verificationCode() {
        System.out.println(EmailServiceImpl.verificationCode());
    }
}