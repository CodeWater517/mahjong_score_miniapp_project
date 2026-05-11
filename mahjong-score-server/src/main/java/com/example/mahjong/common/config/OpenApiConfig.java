package com.example.mahjong.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// OpenAPI/Swagger 文档配置，启动后可通过 swagger-ui 或 knife4j 查看接口文档。
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // 设置接口文档页面显示的标题、版本和描述。
        return new OpenAPI().info(new Info()
            .title("麻将计分助手 API")
            .version("1.0.0")
            .description("面向线下麻将局的手动计分微信小程序接口"));
    }
}
