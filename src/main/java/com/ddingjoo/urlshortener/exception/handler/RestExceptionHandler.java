package com.ddingjoo.urlshortener.exception.handler;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.ErrorResponse;
import com.ddingjoo.urlshortener.exception.core.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class RestExceptionHandler {
	
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleDomainException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.httpStatus())
				.body(errorBody(errorCode, exception.getMessage()));
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest()
				.body(errorBody(ErrorCode.INVALID_URL, "Request validation failed"));
	}
	
	private ErrorResponse errorBody(ErrorCode errorCode, String message) {
		return new ErrorResponse(
				errorCode.httpStatus().value(),
				errorCode.code(),
				errorCode.httpStatus().getReasonPhrase(),
				message,
				OffsetDateTime.now()
		);
	}
}
