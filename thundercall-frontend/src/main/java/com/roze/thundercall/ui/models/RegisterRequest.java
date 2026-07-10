package com.roze.thundercall.ui.models;

public record RegisterRequest(

        String username,

        String email,

        String password
) {
}
