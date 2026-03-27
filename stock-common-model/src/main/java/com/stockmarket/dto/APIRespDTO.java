package com.stockmarket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class APIRespDTO<T> {
    private final int status;
    private final String message;
    private final T data;

    // ── Static Factory Methods ────────────────────────────

    public static <T> APIRespDTO<T> success(T data) {
        return APIRespDTO.<T>builder()
                .status(1).message("Success").data(data).build();
    }

    public static <T> APIRespDTO<T> success(String message, T data) {
        return APIRespDTO.<T>builder()
                .status(1).message(message).data(data).build();
    }

    public static <T> APIRespDTO<T> failure(String message) {
        return APIRespDTO.<T>builder()
                .status(0).message(message).data(null).build();
    }

    public static <T> APIRespDTO<T> notFound(String resource) {
        return APIRespDTO.<T>builder()
                .status(0).message(resource + " not found").data(null).build();
    }
}
