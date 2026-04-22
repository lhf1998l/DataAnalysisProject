package com.lottery.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 基础信息配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lotteryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Data API")
                        .description("数据报表与规则分析后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("Data Team"))
                        .license(new License().name("Internal Use")))
                .externalDocs(new ExternalDocumentation()
                        .description("Swagger UI")
                        .url("/swagger-ui/index.html"));
    }
}
