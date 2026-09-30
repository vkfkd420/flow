package com.flow.dto;

import java.util.List;

public record ExtensionPolicyResponse(
	List<FixedExtension> fixed,
	List<CustomExtension> custom,
	int customCount,
	int customLimit) {
}
