package com.flow.dto;

import jakarta.validation.constraints.NotNull;

/** extension 형식 검사와 정규화는 서비스에서 ExtensionRule로 한다. */
public record CustomAddRequest(@NotNull String extension) {
}
