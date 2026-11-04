package com.datatrust.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Email @Size(max = 320) String email) {
}
