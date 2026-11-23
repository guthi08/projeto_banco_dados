package com.drone.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Informe o nome do responsável.")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres.")
        String nome,

        @NotBlank(message = "Informe a razão social da empresa operadora.")
        @Size(min = 3, max = 80, message = "A razão social deve ter entre 3 e 80 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Informe o setor de atuação.")
        @Size(max = 45, message = "O setor de atuação deve ter no máximo 45 caracteres.")
        String setorAtuacao,

        @NotBlank(message = "Informe o telefone de contato.")
        @Size(min = 10, max = 20, message = "Informe um telefone válido com DDD.")
        String telefone,

        @NotBlank(message = "Informe o e-mail corporativo.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 80, message = "O e-mail deve ter no máximo 80 caracteres.")
        String email,

        @NotBlank(message = "Informe sua senha atual para confirmar a edição do perfil.")
        String senhaAtual
) {
}
