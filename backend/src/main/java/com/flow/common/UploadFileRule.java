package com.flow.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 업로드 파일명 규칙.
 * 파일명의 첫 구간(이름)을 뺀 모든 점 구간을 확장자 후보로 보고 전부 정책 검사에 쓴다.
 * 예) "a.exe.txt" → [exe, txt], "archive.tar.gz" → [tar, gz], ".env" → [env], "noext" → []
 */
public final class UploadFileRule {

	public static final int MAX_FILE_NAME_LENGTH = 255;

	/** 제어 문자와 Windows에서 파일명으로 쓸 수 없는 문자 (경로 구분자는 basename에서 먼저 제거) */
	private static final String FORBIDDEN_CHARS = ":*?\"<>|";

	private UploadFileRule() {
	}

	/** 경로를 떼어낸 파일명. 브라우저가 아닌 클라이언트는 경로를 붙여 보낼 수 있다. */
	public static String baseName(String fileName) {
		int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		return fileName.substring(slash + 1);
	}

	public static boolean hasForbiddenChar(String name) {
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isISOControl(c) || FORBIDDEN_CHARS.indexOf(c) >= 0) {
				return true;
			}
		}
		return false;
	}

	/** 소문자로 바꾼 확장자 후보 목록 (앞에서부터 순서대로) */
	public static List<String> extensions(String name) {
		// Windows는 끝의 점과 공백을 무시한다: "a.exe." / "a.exe " → a.exe
		String trimmed = name.replaceAll("[.\\s]+$", "");
		String[] segments = trimmed.split("\\.", -1);
		List<String> result = new ArrayList<>();
		for (int i = 1; i < segments.length; i++) {
			String segment = segments[i].strip().toLowerCase(Locale.ROOT);
			if (!segment.isEmpty()) {
				result.add(segment);
			}
		}
		return result;
	}
}
