package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocketIoStatusResponse {
    private String sessionId;
    private String status;
    private List<SocketIoEventResponse> events;
}