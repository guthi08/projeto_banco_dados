package com.drone.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OperacaoVooRequest(
        @NotBlank(message = "Informe o CPF ou CNPJ da empresa operadora responsável.")
        @Size(min = 11, max = 18, message = "Documento da empresa deve conter entre 11 e 18 caracteres.")
        String empresaOpeCnpj,

        @NotBlank(message = "Informe o código de registro ANAC do drone.")
        @Size(min = 5, max = 20, message = "Código do drone inválido.")
        String droneCodigo,

        @NotBlank(message = "Informe o CPF do piloto responsável.")
        @Size(min = 11, max = 14, message = "CPF do piloto inválido.")
        String pilotoCpf,

        @NotBlank(message = "Informe o CPF ou CNPJ do cliente solicitante.")
        @Size(min = 11, max = 18, message = "Documento do cliente inválido.")
        String clienteCpfRg,

        @NotBlank(message = "Informe o objetivo da operação de voo.")
        @Size(min = 5, max = 120, message = "O objetivo do voo deve ter entre 5 e 120 caracteres.")
        String objetivoVoo,

        @NotBlank(message = "Informe o local de origem da decolagem.")
        @Size(min = 3, max = 100, message = "A origem deve ter entre 3 e 100 caracteres.")
        String origem,

        @NotBlank(message = "Informe o local de destino/pouso.")
        @Size(min = 3, max = 100, message = "O destino deve ter entre 3 e 100 caracteres.")
        String destino,

        @NotBlank(message = "Informe a descrição da área sobrevoada.")
        @Size(min = 3, max = 120, message = "A descrição da área deve ter entre 3 e 120 caracteres.")
        String descricaoArea,

        @NotBlank(message = "Informe o tipo de local da operação.")
        @Size(max = 45, message = "O tipo de local deve ter no máximo 45 caracteres.")
        String tipoLocal,

        @NotNull(message = "Informe a data e hora planejadas da operação.")
        LocalDateTime dataHoraPlanejada,

        @Min(value = 15, message = "A duração estimada mínima é de 15 minutos.")
        @Max(value = 480, message = "A duração estimada máxima é de 480 minutos (8 horas).")
        Integer duracaoEstimadaMin
) {
}
