package com.roze.thundercall.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SocketIoEmitRequest(
        @NotBlank String event,
        // Free-form — sent as JSON if it parses as one, otherwise as a
        // plain string, so both `{"foo":"bar"}` and `hello` work.
        String data
) {
}