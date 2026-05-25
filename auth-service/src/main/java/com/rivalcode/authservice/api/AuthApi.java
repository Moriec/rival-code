package com.rivalcode.authservice.api;

import com.rivalcode.authservice.exception.ErrorResponse;
import com.rivalcode.contracts.users.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "Регистрация, вход и обновление токенов")
public interface AuthApi {

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт учётную запись и возвращает пару JWT токенов."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная регистрация",
                    content = @Content(schema = @Schema(implementation = AuthTokensResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiIs...",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
                                      "tokenType": "Bearer",
                                      "expiresInSeconds": 900,
                                      "user": {
                                        "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                        "email": "user@example.com",
                                        "username": "testuser",
                                        "displayName": "Test User",
                                        "roles": ["USER"],
                                        "enabled": true,
                                        "createdAt": "2026-05-25T12:00:00Z",
                                        "updatedAt": "2026-05-25T12:00:00Z"
                                      }
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Дубликат email или username",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Email already exists\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации")
    })
    AuthTokensResponse register(@Valid @RequestBody RegisterRequest request);

    @PostMapping("/login")
    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя по email/username и паролю, возвращает токены."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = AuthTokensResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid credentials\"}"))),
            @ApiResponse(responseCode = "401", description = "Учётная запись отключена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Account disabled\"}")))
    })
    AuthTokensResponse login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление пары токенов",
            description = "Принимает refresh-токен, возвращает новую пару access+refresh. Старый refresh-токен отзывается."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены",
                    content = @Content(schema = @Schema(implementation = AuthTokensResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh-токен недействителен, отозван или просрочен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Refresh token revoked\"}")))
    })
    AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request);
}