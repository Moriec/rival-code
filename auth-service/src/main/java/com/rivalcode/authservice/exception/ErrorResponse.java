package com.rivalcode.authservice.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Стандартный ответ с ошибкой")
public record ErrorResponse(@Schema(example = "Email already exists") String error) {}