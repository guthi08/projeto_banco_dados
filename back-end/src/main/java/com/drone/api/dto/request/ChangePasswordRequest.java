package com.drone.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe sua senha atual.")
        @Size(min = 6, message = "A senha atual deve possuir no mínimo 6 caracteres.")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A nova senha deve possuir entre 8 e 72 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
                message = "A nova senha deve conter maiúscula, minúscula, número e caractere especial."
        )
        String novaSenha
) {
}
