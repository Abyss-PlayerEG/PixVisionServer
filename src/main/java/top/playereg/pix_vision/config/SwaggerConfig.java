package top.playereg.pix_vision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@EnableOpenApi
@SuppressWarnings("all")
public class SwaggerConfig {
    @Value("${spring.application.version}")
    private String AppVersion;
    // 创建API
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("top.playereg.pix_vision.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    // 创建该API的基本信息，如标题、描述、版本
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("像素视觉") //  可以用来自定义API的主标题
                .description("像素视觉后端服务器") // 可以用来描述整体的API
                .termsOfServiceUrl("") // 用于定义服务的域名
                .version(AppVersion) // 可以用来定义版本。
                .build();
    }
}
