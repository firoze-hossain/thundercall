package com.roze.thundercall.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseResponse<T> {
    private Boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    private Integer statusCode;

    public static <T> BaseResponse<T> success(T data) {
        return success(data, "Operation Completed Successfully");
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .statusCode(HttpStatus.OK.value())
                .build();
    }

    public static <T> BaseResponse<T> created(T data) {
        return created(data, "Resource Created SuccessFully");
    }

    public static <T> BaseResponse<T> created(T data, String message) {
        return BaseResponse.<T>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .success(true)
                .timestamp(Instant.now())
                .build();

    }

    public static BaseResponse<Void> noContent() {
        return noContent("No Content Available");
    }

    public static BaseResponse<Void> noContent(String message) {
        return BaseResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .statusCode(HttpStatus.NO_CONTENT.value())
                .build();

    }

    public static BaseResponse<Void> error(String message, HttpStatus status) {
        return BaseResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .statusCode(status.value())
                .build();
    }

    public static <T> BaseResponse<T> error(String message, HttpStatus status, T data) {
        return BaseResponse.<T>builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}
