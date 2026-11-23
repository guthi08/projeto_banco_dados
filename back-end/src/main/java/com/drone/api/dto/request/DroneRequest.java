package com.drone.api.dto.request;

import com.drone.api.entity.TipoUsoDrone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DroneRequest(
        @NotBlank(message = "Informe o código de registro ANAC do drone.")
        @Pattern(
                regexp = "^(?i)(PR|PP|PS)-[A-Z0-9]{3,6}$",
                message = "Use o padrão aeronáutico brasileiro: PR-XXX, PP-XXX ou PS-XXX (ex: PR-401)."
        )
        String codigo,

        @NotBlank(message = "Informe o fabricante da aeronave.")
        @Size(min = 2, max = 45, message = "O fabricante deve ter entre 2 e 45 caracteres.")
        String fabricante,

        @NotBlank(message = "Informe o modelo da aeronave.")
        @Size(min = 2, max = 45, message = "O modelo deve ter entre 2 e 45 caracteres.")
        String modelo,

        @NotBlank(message = "Informe o número de série.")
        @Size(min = 4, max = 45, message = "O número de série deve ter entre 4 e 45 caracteres.")
        String numSerie,

        @NotNull(message = "Informe o peso máximo de decolagem (kg).")
        @DecimalMin(value = "0.01", message = "O peso máximo de decolagem deve ser maior que zero.")
        @DecimalMax(value = "999.99", message = "O peso máximo de decolagem não pode exceder 999,99 kg.")
        BigDecimal pesoMaxDecolagem,

        @NotNull(message = "Informe o tipo de uso (basico ou avancado).")
        TipoUsoDrone tipoUso,

        @NotNull(message = "Informe a data de validade do cadastro na ANAC.")
        LocalDate dataValidadeCadastro,

        @NotBlank(message = "Informe o CPF ou CNPJ da empresa operadora responsável.")
        @Size(min = 11, max = 18, message = "Documento da empresa deve conter entre 11 e 18 caracteres.")
        String empresaOpeCnpj
) {
}
