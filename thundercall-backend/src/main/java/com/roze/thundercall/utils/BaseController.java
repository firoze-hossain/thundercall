package com.roze.thundercall.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseController {
    protected <T> ResponseEntity<BaseResponse<T>> ok(T data) {
        return ResponseEntity.ok(BaseResponse.success(data));
    }

    protected <T> ResponseEntity<BaseResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(BaseResponse.success(data, message));
    }

    protected <T> ResponseEntity<BaseResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(data));
    }

    protected <T> ResponseEntity<BaseResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(data, message));
    }

    protected ResponseEntity<BaseResponse<Void>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(BaseResponse.noContent());
    }

    protected ResponseEntity<BaseResponse<Void>> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(BaseResponse.noContent(message));
    }

    protected ResponseEntity<BaseResponse<Void>> error(String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(BaseResponse.error(message, status));
    }

    protected <T> ResponseEntity<BaseResponse<T>> error(String message, HttpStatus status, T data) {
        return ResponseEntity.status(status)
                .body(BaseResponse.error(message, status, data));
    }
}
