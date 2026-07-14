package com.ole.turapp.dto;

public record UserRegistrationRequest(
        String email,
        String password,
        String displayName
) {
}
