package com.drone.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail corporativo.")
        @Email(message = "Informe um endereço de e-mail válido.")
        String email,

        @NotBlank(message = "Informe a senha de acesso.")
        @Size(min = 6, message = "A senha deve possuir no mínimo 6 caracteres.")
        String senha
) {
}
