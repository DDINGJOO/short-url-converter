package com.ddingjoo.urlshortener.service.click;

public record ClickBufferState(
		long pendingClicks,
		long processingClicks,
		String processingToken
) {
}
