package com.drone.api.dto.request;

import com.drone.api.entity.StatusContrato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ClienteRequest(
        @NotBlank(message = "Informe o CPF ou CNPJ do cliente.")
        @Size(min = 11, max = 18, message = "O documento do cliente deve conter entre 11 e 18 caracteres.")
        String cpfRg,

        @NotBlank(message = "Informe o RG ou inscrição estadual do cliente.")
        @Size(min = 5, max = 12, message = "O RG deve ter entre 5 e 12 caracteres.")
        String rg,

        @NotBlank(message = "Informe o nome ou razão social do cliente.")
        @Size(min = 3, max = 60, message = "O nome do cliente deve ter entre 3 e 60 caracteres.")
        String nome,

        @NotBlank(message = "Informe o CPF ou CNPJ da empresa operadora contratada.")
        @Size(min = 11, max = 18, message = "Documento da empresa deve conter entre 11 e 18 caracteres.")
        String empresaOpeCnpj,

        @NotNull(message = "Informe o status do contrato (Ativo ou Inativo).")
        StatusContrato statusContrato,

        @NotNull(message = "Informe a data de formalização do contrato.")
        LocalDate dataFormalizacao,

        @NotEmpty(message = "Informe ao menos um telefone de contato do cliente.")
        List<@NotBlank(message = "Telefone não pode estar em branco.") String> telefones
) {
}
