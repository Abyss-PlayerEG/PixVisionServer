package top.playereg.pix_vision.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("all")
public class SwaggerConfig {

    @Value("${spring.application.version}")
    private String AppVersion;

    // 创建 API
    @Bean
    public OpenAPI createRestApi() {
        return new OpenAPI().info(
                new Info()
                        .title("像素视觉")
                        .description("像素视觉后端服务器")
                        .version(AppVersion)
        );
    }
    //
    @Bean
    public GroupedOpenApi rootApi() {
        return GroupedOpenApi.builder()
                .group("1_Root")
                .pathsToMatch("/**")
                .packagesToScan("top.playereg.pix_vision.controller")
                .build();
    }
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("2_PixelVisionApi")
                .pathsToMatch("/api/**")
                .packagesToScan("top.playereg.pix_vision.controller")
                .build();
    }
}
