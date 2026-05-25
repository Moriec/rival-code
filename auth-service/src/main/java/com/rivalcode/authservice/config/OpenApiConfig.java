package com.rivalcode.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("RivalCode Auth Service")
                        .version("1.0")
                        .description("""
                                Сервис аутентификации и управления профилем.
                                
                                **Доступные операции:**
                                - Регистрация, вход, обновление токенов
                                - Получение и редактирование профиля
                                - Загрузка и смена аватара через MinIO
                                
                                **Коды ошибок:**
                                - `400` — ошибка валидации, дубликат
                                - `401` — неверные учётные данные, просроченный токен
                                - `403` — доступ запрещён (чужой аватар)
                                - `404` — сущность не найдена
                                - `409` — конфликт данных
                                - `502` — сбой внешнего сервиса (MinIO)
                                """))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Вставьте сюда access token")));
    }
}