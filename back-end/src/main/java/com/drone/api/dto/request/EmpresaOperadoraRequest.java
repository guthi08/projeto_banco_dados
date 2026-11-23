package com.drone.api.dto.request;

import com.drone.api.entity.StatusContrato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmpresaOperadoraRequest(
        @NotBlank(message = "Informe o CPF ou CNPJ da empresa operadora.")
        @Size(min = 11, max = 18, message = "Documento deve conter entre 11 e 18 caracteres.")
        String cnpj,

        @NotBlank(message = "Informe a razão social ou nome do responsável.")
        @Size(min = 3, max = 80, message = "A razão social deve ter entre 3 e 80 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Informe o setor de atuação.")
        @Size(max = 45, message = "O setor de atuação deve ter no máximo 45 caracteres.")
        String setorAtuacao,

        @NotBlank(message = "Informe o e-mail corporativo.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 80, message = "O e-mail deve ter no máximo 80 caracteres.")
        String email,

        @NotEmpty(message = "Informe ao menos um telefone de contato.")
        List<@NotBlank(message = "Telefone não pode estar em branco.") String> telefones,

        @NotNull(message = "Informe o status do contrato.")
        StatusContrato statusContrato
) {
}
