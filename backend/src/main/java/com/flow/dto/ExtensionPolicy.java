package com.flow.dto;

/** FILE_EXTENSION_POLICY 한 행 */
public record ExtensionPolicy(Long id, String extension, String type, boolean blocked) {

	public static final String FIXED = "FIXED";
	public static final String CUSTOM = "CUSTOM";
}
