package com.flow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
		return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "요청 형식이 올바르지 않습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		// 없는 경로(404), 허용하지 않는 메서드(405) 등 Spring MVC가 상태 코드를 정해 둔 예외
		if (e instanceof org.springframework.web.ErrorResponse mvcError) {
			return ResponseEntity.status(mvcError.getStatusCode())
				.body(new ErrorResponse("INVALID_REQUEST", "요청을 처리할 수 없습니다."));
		}
		log.error("처리되지 않은 예외", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
	}
}
