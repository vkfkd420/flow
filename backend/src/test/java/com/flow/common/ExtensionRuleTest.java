package com.flow.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ExtensionRuleTest {

	@ParameterizedTest
	@CsvSource({
		"sh, sh",
		"SH, sh",
		"'  sh  ', sh",
		".sh, sh",
		"' .Sh ', sh",
		"mp4, mp4",
		"7z, 7z",
		"abcdefghijklmnopqrst, abcdefghijklmnopqrst", // 20자
		".abcdefghijklmnopqrst, abcdefghijklmnopqrst" // 점 제거 후 20자
	})
	void 규칙에_맞으면_정규화한_값을_돌려준다(String input, String expected) {
		assertThat(ExtensionRule.normalize(input)).isEqualTo(expected);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" ", ".", "..sh", "tar.gz", "s h", "sh!", "sh/", "../sh", "한글", "ｅｘｅ",
		"abcdefghijklmnopqrstu" // 21자
	})
	void 규칙에_맞지_않으면_null(String input) {
		assertThat(ExtensionRule.normalize(input)).isNull();
	}
}
