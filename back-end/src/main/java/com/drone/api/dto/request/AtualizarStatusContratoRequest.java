package com.drone.api.dto.request;

import com.drone.api.entity.StatusContrato;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusContratoRequest(
        @NotNull(message = "Informe o novo status do contrato.")
        StatusContrato statusContrato
) {
}
