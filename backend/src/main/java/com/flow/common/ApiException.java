package com.flow.common;

/**
 * 화면에 그대로 보여줄 메시지를 담는 업무 예외.
 * GlobalExceptionHandler가 { code, message } 형식으로 응답한다.
 */
public class ApiException extends RuntimeException {

	private final ErrorCode code;

	/** args는 ErrorCode 메시지의 %s, %d 자리에 들어갈 값 */
	public ApiException(ErrorCode code, Object... args) {
		super(code.message(args));
		this.code = code;
	}

	public ErrorCode getCode() {
		return code;
	}
}
