package com.flow.common;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.springframework.http.HttpStatus;

/**
 * 응답 { code, message }의 목록. code는 enum 이름, message는 화면에 그대로 보여줄 문장이다.
 * 메시지의 %s, %d 자리는 ApiException을 만들 때 넘기는 값으로 채운다.
 */
public enum ErrorCode {

	INVALID_REQUEST(BAD_REQUEST, "요청을 처리할 수 없습니다."),
	INTERNAL_ERROR(INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

	// 확장자 정책
	INVALID_EXTENSION(BAD_REQUEST, "확장자는 영문과 숫자로 1~%d자까지 입력할 수 있습니다."),
	FIXED_EXTENSION_NOT_FOUND(NOT_FOUND, "'%s'는 고정 확장자가 아닙니다."),
	FIXED_EXTENSION_CONFLICT(CONFLICT, "'%s'는 고정 확장자입니다. 고정 확장자 영역에서 체크해 주세요."),
	DUPLICATE_EXTENSION(CONFLICT, "'%s'는 이미 추가된 확장자입니다."),
	CUSTOM_LIMIT_EXCEEDED(CONFLICT, "커스텀 확장자는 최대 %d개까지 추가할 수 있습니다."),
	CUSTOM_EXTENSION_NOT_FOUND(NOT_FOUND, "삭제할 커스텀 확장자가 없습니다. 이미 삭제되었을 수 있습니다."),

	// 파일 업로드
	INVALID_FILE_NAME(BAD_REQUEST, "파일명이 없거나 사용할 수 없는 문자가 포함되어 있습니다."),
	FILE_NAME_TOO_LONG(BAD_REQUEST, "파일명은 최대 %d자까지 가능합니다."),
	EMPTY_FILE(BAD_REQUEST, "빈 파일은 업로드할 수 없습니다."),
	UNREADABLE_FILE(BAD_REQUEST, "파일을 읽을 수 없습니다. 다시 시도해 주세요."),
	BLOCKED_EXTENSION(BAD_REQUEST, "'%s' 확장자는 업로드가 차단되어 있습니다."),
	DISGUISED_EXECUTABLE(BAD_REQUEST, "파일 내용은 %s인데 %s 업로드할 수 없습니다."),
	FILE_TOO_LARGE(PAYLOAD_TOO_LARGE, "파일은 최대 %dMB까지 업로드할 수 있습니다."),
	STORAGE_UNAVAILABLE(SERVICE_UNAVAILABLE, "파일 저장소에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String message(Object... args) {
		return message.formatted(args);
	}
}
