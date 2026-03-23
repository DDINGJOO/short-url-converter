package com.ddingjoo.urlshortener.controller.docs.health;

import com.ddingjoo.urlshortener.dto.health.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public interface HealthApiDocs {
	
	@Operation(summary = "헬스체크", description = "애플리케이션 상태를 확인합니다.")
	@ApiResponse(responseCode = "200", description = "서비스 정상")
	HealthResponse health();
}
