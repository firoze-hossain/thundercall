package com.roze.thundercall.ui.models;

import lombok.Data;

@Data
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int status;
}