package com.flow.dto;

public record FixedExtension(String extension, boolean blocked) {

	public static FixedExtension from(ExtensionPolicy policy) {
		return new FixedExtension(policy.extension(), policy.blocked());
	}
}
