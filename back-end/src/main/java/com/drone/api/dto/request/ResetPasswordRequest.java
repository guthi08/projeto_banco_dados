package com.drone.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Informe o token de recuperação.")
        String token,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
                message = "A senha deve conter maiúscula, minúscula, número e caractere especial."
        )
        String novaSenha
) {
}
