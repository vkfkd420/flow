package com.flow.dto;

public record CustomExtension(Long id, String extension) {

	public static CustomExtension from(ExtensionPolicy policy) {
		return new CustomExtension(policy.id(), policy.extension());
	}
}
