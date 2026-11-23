package com.drone.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(
        @NotBlank(message = "Informe sua senha atual para confirmar a exclusão da conta.")
        String senhaAtual
) {
}
