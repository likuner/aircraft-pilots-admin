package com.uav.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("无人机驾驶员管理后台 API")
                .description("无人机驾驶员考试管理、合格证颁发、训练机构资质认证")
                .version("1.0.0"));
    }
}
