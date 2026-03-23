package com.ddingjoo.urlshortener.controller;

import com.ddingjoo.urlshortener.controller.docs.health.HealthApiDocs;
import com.ddingjoo.urlshortener.dto.health.HealthResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Tag(name = "Health")
@RestController
@RequestMapping("/api")
public class HealthController implements HealthApiDocs {
	
	@Override
	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("UP", OffsetDateTime.now());
	}
}
