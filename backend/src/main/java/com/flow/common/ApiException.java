package com.flow.common;

import org.springframework.http.HttpStatus;

/**
 * 화면에 그대로 보여줄 메시지를 담는 업무 예외.
 * GlobalExceptionHandler가 { code, message } 형식으로 응답한다.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
