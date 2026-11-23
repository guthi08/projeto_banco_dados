package com.drone.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PilotoRequest(
        @NotBlank(message = "Informe o CPF do piloto.")
        @Size(min = 11, max = 14, message = "O CPF deve conter entre 11 e 14 caracteres.")
        String cpf,

        @NotBlank(message = "Informe o RG do piloto.")
        @Size(min = 5, max = 12, message = "O RG deve ter entre 5 e 12 caracteres.")
        String rg,

        @NotBlank(message = "Informe o nome completo do piloto.")
        @Size(min = 3, max = 45, message = "O nome do piloto deve ter entre 3 e 45 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail corporativo do piloto.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 80, message = "O e-mail deve ter no máximo 80 caracteres.")
        String email,

        @NotBlank(message = "Informe o número de registro/licença de voo.")
        @Size(min = 5, max = 20, message = "O registro/licença deve ter entre 5 e 20 caracteres.")
        String numRegistroLicenca,

        @NotNull(message = "Informe a data de validade do CMA.")
        LocalDate validadeCma,

        @NotEmpty(message = "Informe ao menos um telefone de contato do piloto.")
        List<@NotBlank(message = "Telefone não pode estar em branco.") String> telefones,

        @NotBlank(message = "Informe o CPF ou CNPJ da empresa operadora vinculada.")
        @Size(min = 11, max = 18, message = "Documento da empresa deve conter entre 11 e 18 caracteres.")
        String empresaOpeCnpj
) {
}
