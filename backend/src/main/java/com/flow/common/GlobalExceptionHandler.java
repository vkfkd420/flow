package com.flow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.util.unit.DataSize;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final DataSize maxFileSize;

	public GlobalExceptionHandler(@Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
		return respond(e.getCode().getStatus(), e.getCode(), e.getMessage());
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
		return respond(ErrorCode.INVALID_REQUEST);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
		return respond(ErrorCode.FILE_TOO_LARGE, maxFileSize.toMegabytes());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		// 없는 경로(404), 허용하지 않는 메서드(405) 등 Spring MVC가 상태 코드를 정해 둔 예외
		if (e instanceof org.springframework.web.ErrorResponse mvcError) {
			return respond(mvcError.getStatusCode(), ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.message());
		}
		log.error("처리되지 않은 예외", e);
		return respond(ErrorCode.INTERNAL_ERROR);
	}

	private static ResponseEntity<ErrorResponse> respond(ErrorCode code, Object... args) {
		return respond(code.getStatus(), code, code.message(args));
	}

	private static ResponseEntity<ErrorResponse> respond(HttpStatusCode status, ErrorCode code, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(code.name(), message));
	}
}
