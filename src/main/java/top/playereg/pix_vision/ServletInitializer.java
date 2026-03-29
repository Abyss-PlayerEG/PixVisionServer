/*
 *      ____________________________
 *     !\_________________________/!\
 *     !!                         !! \
 *     !!                         !!  \
 *     !!                         !!  !
 *     !!                         !!  !
 *     !!         No Bugs         !!  !
 *     !!                         !!  !
 *     !!                         !!  !
 *     !!                         !!  /
 *     !!_________________________!! /
 *     !/_________________________\!/
 *        __\_________________/__/!_
 *       !_______________________!/ )
 *     ________________________    (__
 *    /oooo  oooo  oooo  oooo /!   _  )_
 *   /ooooooooooooooooooooooo/ /  (_)_(_)
 *  /ooooooooooooooooooooooo/ /    (o o)
 * /C=_____________________/_/    ==\o/==
 *
 */

package top.playereg.pix_vision;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet初始化类
 *
 * @author PlayerEG
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(PixVisionApplication.class);
    }

}
