package com.flow.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 확장자 정규화 규칙. db/schema.sql의 CK_FILE_EXTENSION_FORMAT과 같은 규칙이다.
 * 앞뒤 공백 제거 → 앞의 '.' 하나 제거 → 소문자 변환 → 영문 소문자/숫자 1~20자만 허용
 */
public final class ExtensionRule {

	public static final int MAX_LENGTH = 20;

	private static final Pattern FORMAT = Pattern.compile("^[a-z0-9]{1," + MAX_LENGTH + "}$");

	private ExtensionRule() {
	}

	/** 정규화한 값을 돌려준다. 규칙에 맞지 않으면 null. */
	public static String normalize(String input) {
		if (input == null) {
			return null;
		}
		String value = input.strip();
		if (value.startsWith(".")) {
			value = value.substring(1);
		}
		value = value.toLowerCase(Locale.ROOT);
		return FORMAT.matcher(value).matches() ? value : null;
	}
}
