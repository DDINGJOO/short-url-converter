package com.ddingjoo.urlshortener.controller;

import com.ddingjoo.urlshortener.controller.docs.redirect.RedirectApiDocs;
import com.ddingjoo.urlshortener.service.url.UrlService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Redirect")
@RestController
public class RedirectController implements RedirectApiDocs {
	
	private final UrlService urlService;
	
	public RedirectController(UrlService urlService) {
		this.urlService = urlService;
	}
	
	@Override
	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		String originalUrl = urlService.resolveOriginalUrl(shortCode);
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, URI.create(originalUrl).toString())
				.build();
	}
}
