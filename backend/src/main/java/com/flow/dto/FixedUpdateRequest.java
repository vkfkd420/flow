package com.flow.dto;

import jakarta.validation.constraints.NotNull;

public record FixedUpdateRequest(@NotNull Boolean blocked) {
}
