package com.rivalcode.authservice.api;

import com.rivalcode.authservice.exception.ErrorResponse;
import com.rivalcode.authservice.model.AvatarMetadata;
import com.rivalcode.contracts.users.model.UpdateProfileRequest;
import com.rivalcode.contracts.users.model.UserProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequestMapping("/api/users")
@Tag(name = "Профиль и аватары", description = "Получение/редактирование профиля, загрузка аватаров")
public interface UserApi {

    @GetMapping("/me")
    @Operation(
            summary = "Получить свой профиль",
            description = "Возвращает профиль текущего аутентифицированного пользователя, включая активный аватар."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль пользователя",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    UserProfileDto getMyProfile();

    @GetMapping("/{userId}/profile")
    @Operation(
            summary = "Получить публичный профиль пользователя",
            description = "Возвращает публичный профиль любого пользователя по его ID. Не требует аутентификации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Публичный профиль",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"User not found\"}")))
    })
    @SecurityRequirements({})
    UserProfileDto getUserProfile(
            @Parameter(description = "UUID пользователя", required = true, in = ParameterIn.PATH)
            @PathVariable("userId") UUID userId
    );

    @PatchMapping("/me")
    @Operation(
            summary = "Обновить свой профиль",
            description = "Позволяет изменить displayName и/или сменить активный аватар по его ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён",
                    content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Попытка установить чужой аватар",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Avatar does not belong to user\"}"))),
            @ApiResponse(responseCode = "404", description = "Аватар или пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Avatar not found\"}")))
    })
    UserProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request);

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузить новый аватар",
            description = "Загружает изображение в MinIO и сохраняет метаданные. Чтобы сделать аватар активным, используйте PATCH /api/users/me."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аватар загружен",
                    content = @Content(schema = @Schema(implementation = AvatarMetadata.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации файла"),
            @ApiResponse(responseCode = "502", description = "Ошибка доступа к MinIO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Failed to upload file to MinIO\"}")))
    })
    AvatarMetadata uploadAvatar(
            @Parameter(description = "Файл изображения (jpg, png и т.д.)", required = true)
            @RequestParam("file") MultipartFile file);

    @GetMapping("/avatars/{avatarId}")
    @Operation(summary = "Get avatar file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar image"),
            @ApiResponse(responseCode = "404", description = "Avatar not found")
    })
    @SecurityRequirements({})
    ResponseEntity<Resource> getAvatarFile(@PathVariable("avatarId") UUID avatarId);
}
