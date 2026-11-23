package com.drone.api.dto.request;

import com.drone.api.entity.StatusVoo;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusVooRequest(
        @NotNull(message = "Informe o novo status da operação de voo.")
        StatusVoo statusVoo
) {
}
