package com.flow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ExecutableSignatureTest {

	/** MZ 헤더 + 0x3C의 오프셋이 가리키는 곳에 "PE\0\0"이 있는 최소 PE */
	public static byte[] pe() {
		byte[] b = new byte[0x80];
		b[0] = 'M';
		b[1] = 'Z';
		b[0x3C] = 0x40;
		b[0x40] = 'P';
		b[0x41] = 'E';
		return b;
	}

	public static byte[] elf() {
		return new byte[] { 0x7F, 'E', 'L', 'F', 2, 1, 1, 0 };
	}

	@Test
	void 실행_파일_형식을_판별한다() {
		assertThat(ExecutableSignature.detect(pe())).isEqualTo(ExecutableSignature.WINDOWS_PE);
		assertThat(ExecutableSignature.detect(elf())).isEqualTo(ExecutableSignature.ELF);
		assertThat(ExecutableSignature.detect(new byte[] { (byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE }))
			.isEqualTo(ExecutableSignature.MACH_O);
		assertThat(ExecutableSignature.detect(bytes("#!/bin/sh\necho hi"))).isEqualTo(ExecutableSignature.SCRIPT);
		assertThat(ExecutableSignature.detect(bytes("#! /usr/bin/env python"))).isEqualTo(ExecutableSignature.SCRIPT);
	}

	@Test
	void 실행_파일이_아니면_null() {
		// MZ로 시작하는 텍스트는 PE 헤더가 없으므로 실행 파일이 아니다
		assertThat(ExecutableSignature.detect(bytes("MZ 분기 실적 보고서 ".repeat(10)))).isNull();
		assertThat(ExecutableSignature.detect(bytes("#!important note"))).isNull();
		assertThat(ExecutableSignature.detect(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A })).isNull();
		assertThat(ExecutableSignature.detect(bytes("%PDF-1.7"))).isNull();
		assertThat(ExecutableSignature.detect(new byte[0])).isNull();
		assertThat(ExecutableSignature.detect(new byte[] { 'M' })).isNull();
	}

	@Test
	void PE_오프셋이_범위를_벗어나도_예외없이_null() {
		byte[] b = pe();
		b[0x3F] = (byte) 0x80; // 음수 오프셋
		assertThat(ExecutableSignature.detect(b)).isNull();
		b = pe();
		b[0x3C] = 0x7E; // 헤더 크기를 넘는 위치
		assertThat(ExecutableSignature.detect(b)).isNull();
	}

	@Test
	void 형식에_맞는_확장자인지_확인한다() {
		assertThat(ExecutableSignature.WINDOWS_PE.matchesExtension("exe")).isTrue();
		assertThat(ExecutableSignature.WINDOWS_PE.matchesExtension("jpg")).isFalse();
		assertThat(ExecutableSignature.WINDOWS_PE.matchesExtension(null)).isFalse();
		assertThat(ExecutableSignature.SCRIPT.matchesExtension("sh")).isTrue();
	}

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}
}
