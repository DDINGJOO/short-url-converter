package com.ddingjoo.urlshortener.controller.docs.redirect;

import com.ddingjoo.urlshortener.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface RedirectApiDocs {

    @Operation(summary = "단축 URL 리다이렉트", description = "단축 URL을 원본 URL로 302 리다이렉트합니다.")
    @ApiResponse(responseCode = "302", description = "리다이렉트 성공")
    @ApiResponse(responseCode = "404", description = "단축 코드 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "410", description = "만료 또는 삭제된 URL", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "IP rate limit 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ResponseEntity<Void> redirect(String shortCode);
}
