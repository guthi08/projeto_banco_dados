package com.drone.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Informe o e-mail cadastrado.")
        @Email(message = "Informe um endereço de e-mail válido.")
        String email
) {
}
