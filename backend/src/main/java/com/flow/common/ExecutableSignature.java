package com.flow.common;

import java.util.Set;

/**
 * 파일 앞부분(매직 넘버)으로 실행 파일을 판별한다.
 * 확장자가 해당 실행 파일 형식의 확장자가 아니면(jpg, 확장자 없음 등) "위장"으로 본다.
 */
public enum ExecutableSignature {

	WINDOWS_PE("Windows 실행 파일", Set.of("exe", "dll", "scr", "cpl", "com", "sys", "ocx", "drv", "efi")),
	ELF("Linux 실행 파일", Set.of("so", "o", "ko", "elf", "bin", "out")),
	MACH_O("macOS 실행 파일", Set.of("dylib", "bundle", "o")),
	SCRIPT("스크립트 파일", Set.of("sh", "bash", "zsh", "ksh", "csh", "fish", "py", "pl", "rb", "php", "js", "mjs",
		"cgi", "command", "tcl", "lua", "awk"));

	/** 판별에 필요한 최대 바이트 수 (PE 헤더 위치까지 포함) */
	public static final int HEAD_SIZE = 4096;

	private final String label;
	private final Set<String> extensions;

	ExecutableSignature(String label, Set<String> extensions) {
		this.label = label;
		this.extensions = extensions;
	}

	public String label() {
		return label;
	}

	/** 이 형식의 파일에 붙는 것이 자연스러운 확장자인지 */
	public boolean matchesExtension(String extension) {
		return extension != null && extensions.contains(extension);
	}

	/** 실행 파일이 아니면 null */
	public static ExecutableSignature detect(byte[] head) {
		if (isPe(head)) {
			return WINDOWS_PE;
		}
		if (startsWith(head, 0x7F, 'E', 'L', 'F')) {
			return ELF;
		}
		if (startsWith(head, 0xFE, 0xED, 0xFA, 0xCE) || startsWith(head, 0xFE, 0xED, 0xFA, 0xCF)
			|| startsWith(head, 0xCE, 0xFA, 0xED, 0xFE) || startsWith(head, 0xCF, 0xFA, 0xED, 0xFE)) {
			return MACH_O;
		}
		if (startsWith(head, '#', '!', '/') || startsWith(head, '#', '!', ' ', '/')) {
			return SCRIPT;
		}
		return null;
	}

	/**
	 * "MZ"만 보면 MZ로 시작하는 텍스트도 걸리므로,
	 * 0x3C 위치의 오프셋(e_lfanew)이 가리키는 곳에 "PE\0\0"이 있는지까지 확인한다.
	 */
	private static boolean isPe(byte[] head) {
		if (!startsWith(head, 'M', 'Z') || head.length < 0x40) {
			return false;
		}
		int offset = (head[0x3C] & 0xFF) | (head[0x3D] & 0xFF) << 8 | (head[0x3E] & 0xFF) << 16 | (head[0x3F] & 0xFF) << 24;
		return offset >= 0x40 && offset + 4 <= head.length
			&& head[offset] == 'P' && head[offset + 1] == 'E' && head[offset + 2] == 0 && head[offset + 3] == 0;
	}

	private static boolean startsWith(byte[] head, int... magic) {
		if (head.length < magic.length) {
			return false;
		}
		for (int i = 0; i < magic.length; i++) {
			if ((head[i] & 0xFF) != magic[i]) {
				return false;
			}
		}
		return true;
	}
}
