package com.ddingjoo.urlshortener.controller.docs.url;

import com.ddingjoo.urlshortener.dto.url.ShortenRequest;
import com.ddingjoo.urlshortener.dto.url.ShortenResponse;
import com.ddingjoo.urlshortener.dto.url.UrlAnalyticsResponse;
import com.ddingjoo.urlshortener.dto.url.UrlStatsResponse;
import com.ddingjoo.urlshortener.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.OffsetDateTime;

public interface UrlApiDocs {

    @Operation(summary = "URL 단축", description = "원본 URL을 단축 URL로 생성합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "커스텀 코드 중복", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "IP rate limit 초과", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ShortenResponse shorten(ShortenRequest request);

    @Operation(summary = "단축 URL 통계 조회", description = "총 클릭 수와 활성 상태를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "404", description = "단축 코드 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    UrlStatsResponse stats(String shortCode);

    @Operation(summary = "클릭 분석 조회", description = "시간별 또는 일별 클릭 분석 버킷을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 granularity 또는 기간", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "단축 코드 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    UrlAnalyticsResponse analytics(
            String shortCode,
            @Parameter(description = "hour 또는 day", example = "hour")
            String granularity,
            @Parameter(description = "조회 시작 시각", example = "2026-03-23T00:00:00Z")
            OffsetDateTime from,
            @Parameter(description = "조회 종료 시각", example = "2026-03-23T23:59:59Z")
            OffsetDateTime to
    );

    @Operation(summary = "단축 URL 삭제", description = "관리자 API 키로 단축 URL을 소프트 딜리트합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "401", description = "관리자 API 키 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "단축 코드 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    void delete(String shortCode, String apiKey);
}
