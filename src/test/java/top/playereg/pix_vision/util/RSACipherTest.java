package top.playereg.pix_vision.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class RSACipherTest {

    @Test
    public void encrypt() {
        String encryptText = RSACipher.encryptToBase64( "我的名字是吉良吉影，33岁。住在杜王町东北部的别墅一带，未婚。我在龟友百货店上班，每天最晚8点下班回家。我不抽烟，酒仅浅尝辄止。晚上11点睡觉，保证睡足8小时。睡前喝一杯温牛奶，然后做20分钟的舒缓运动暖身再睡觉，基本能熟睡到天亮。像婴儿一样，绝不把疲劳和压力留到第二天。连医生都说我很正常。\n" +
                "\n" +
                "我只是想说，我这个人别无奢求，只希望能够心情平静的活下去。胜负，输赢是我最不喜欢计较的。因为，那只会为自己弄来麻烦和敌人。我就是这样知足的人，这也是我的人生观。\n" +
                "\n" +
                "但是，若一定要动手的话，我是不会输给任何人的。" );

        System.out.print( "加密结果：" );
        System.out.println(encryptText);
        System.out.println(" ");

        String decryptText = RSACipher.decryptToString( encryptText );

        System.out.print( "解密结果：" );
        System.out.println(decryptText);
    }
}