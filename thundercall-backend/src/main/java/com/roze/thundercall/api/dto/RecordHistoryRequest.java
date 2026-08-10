package com.roze.thundercall.api.dto;

/** Carries a completed request/response pair to the record-history
 * endpoint — used when the actual HTTP call already happened on the
 * client (see ClientHttpExecutor on the frontend), so the backend
 * just needs both halves to log it, not to execute anything itself. */
public record RecordHistoryRequest(
        ApiRequest request,
        ApiResponse response
) {
}