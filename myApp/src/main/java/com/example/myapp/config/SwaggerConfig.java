package com.example.myapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;  // Правильный импорт
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()  // Теперь используется правильный класс
                        .title("Release Manager API")
                        .version("1.0")
                        .description("API для управления релизами"));
    }
}