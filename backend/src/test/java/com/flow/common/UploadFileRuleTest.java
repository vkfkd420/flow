package com.flow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UploadFileRuleTest {

	@Test
	void 첫_구간을_뺀_모든_점_구간이_확장자_후보다() {
		assertThat(UploadFileRule.extensions("a.exe")).containsExactly("exe");
		assertThat(UploadFileRule.extensions("a.exe.txt")).containsExactly("exe", "txt");
		assertThat(UploadFileRule.extensions("archive.tar.gz")).containsExactly("tar", "gz");
		assertThat(UploadFileRule.extensions("report.PDF")).containsExactly("pdf");
		assertThat(UploadFileRule.extensions(".env")).containsExactly("env");
		assertThat(UploadFileRule.extensions("a..exe")).containsExactly("exe");
		assertThat(UploadFileRule.extensions("noext")).isEmpty();
	}

	@Test
	void 끝의_점과_공백은_무시한다() {
		// Windows는 "a.exe." / "a.exe " 를 a.exe로 저장한다
		assertThat(UploadFileRule.extensions("a.exe.")).containsExactly("exe");
		assertThat(UploadFileRule.extensions("a.exe  ")).containsExactly("exe");
		assertThat(UploadFileRule.extensions("a.exe . .")).containsExactly("exe");
	}

	@Test
	void 경로는_떼어낸다() {
		assertThat(UploadFileRule.baseName("../../etc/x.sh")).isEqualTo("x.sh");
		assertThat(UploadFileRule.baseName("C:\\Users\\me\\y.bat")).isEqualTo("y.bat");
		assertThat(UploadFileRule.baseName("plain.txt")).isEqualTo("plain.txt");
	}

	@ParameterizedTest
	@ValueSource(strings = { "a\u0000.exe.jpg", "a\n.txt", "a.txt::$DATA", "a?.txt", "a|b.txt", "a<b>.txt" })
	void 제어_문자와_파일명에_쓸_수_없는_문자를_찾는다(String name) {
		assertThat(UploadFileRule.hasForbiddenChar(name)).isTrue();
	}

	@Test
	void 일반_파일명은_허용한다() {
		for (String name : List.of("보고서 최종(1).pdf", "photo 2026-10-01.jpg", "a_b-c.tar.gz", ".env")) {
			assertThat(UploadFileRule.hasForbiddenChar(name)).as(name).isFalse();
		}
	}
}
