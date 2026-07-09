package com.roze.thundercall.models;

public record RegisterRequest(

        String username,

        String email,

        String password
) {
}
