package com.drone.api.dto.request;

import com.drone.api.entity.NivelGravidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AuditoriaRequest(
        @NotNull(message = "Informe o código da operação de voo vinculada.")
        @Positive(message = "Informe um código de operação de voo válido.")
        Integer operacaoVooCodigo,

        @NotNull(message = "Informe a data e hora do evento de auditoria.")
        LocalDateTime dataHoraEvento,

        @NotBlank(message = "Informe a categoria da ocorrência.")
        @Size(min = 3, max = 60, message = "A categoria deve ter entre 3 e 60 caracteres.")
        String categoria,

        @NotNull(message = "Informe o nível de gravidade (BAIXO, MEDIO, ALTO ou CRITICO).")
        NivelGravidade nivelGravidade,

        @NotBlank(message = "Informe a descrição detalhada da ocorrência.")
        @Size(min = 15, max = 500, message = "A descrição deve ter entre 15 e 500 caracteres.")
        String descricao
) {
}
