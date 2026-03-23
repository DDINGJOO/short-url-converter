package com.ddingjoo.urlshortener.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "표준 에러 응답")
public record ErrorResponse(
        @Schema(example = "404")
        int status,

        @Schema(example = "URL_NOT_FOUND")
        String code,

        @Schema(example = "Not Found")
        String error,

        @Schema(example = "Short URL not found")
        String message,

        @Schema(example = "2026-03-23T10:00:00Z")
        OffsetDateTime timestamp
) {
}
