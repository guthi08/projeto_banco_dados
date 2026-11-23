package com.drone.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o nome do responsável.")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres.")
        String nomeResponsavel,

        @NotBlank(message = "Informe a razão social da empresa operadora.")
        @Size(min = 3, max = 80, message = "A razão social deve ter entre 3 e 80 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Informe o CPF ou CNPJ da operadora.")
        @Size(min = 11, max = 18, message = "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.")
        String cnpjCpf,

        @NotBlank(message = "Informe o setor de atuação.")
        @Size(max = 45, message = "O setor de atuação deve ter no máximo 45 caracteres.")
        String setorAtuacao,

        @NotBlank(message = "Informe um telefone de contato com DDD.")
        @Size(min = 10, max = 20, message = "Informe um telefone válido com DDD.")
        String telefone,

        @NotBlank(message = "Informe o e-mail corporativo.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 80, message = "O e-mail deve ter no máximo 80 caracteres.")
        String email,

        @NotBlank(message = "Informe a senha de acesso.")
        @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
                message = "A senha deve conter maiúscula, minúscula, número e caractere especial."
        )
        String senha
) {
}
