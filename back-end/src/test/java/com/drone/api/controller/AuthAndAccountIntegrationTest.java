package com.drone.api.controller;

import com.drone.api.dto.request.ChangePasswordRequest;
import com.drone.api.dto.request.DeactivateAccountRequest;
import com.drone.api.dto.request.ForgotPasswordRequest;
import com.drone.api.dto.request.LoginRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.dto.request.UpdateProfileRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fluxo completo MOD-BE 1: register -> login -> profile -> update -> password -> deactivate")
    void fluxoCompletoAutenticacaoEConta() throws Exception {
        // 1. Acesso sem token deve retornar 401
        mockMvc.perform(get("/api/v1/account/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 2. Cadastro de nova operadora
        RegisterRequest registerReq = new RegisterRequest(
                "Ana Souza",
                "VetorGeo Integração Ltda",
                "41.092.514/0001-88",
                "Geodésia",
                "(41) 99844-1102",
                "ana.integracao@vetorgeo.com.br",
                "Vetor@2026"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value("ana.integracao@vetorgeo.com.br"))
                .andExpect(jsonPath("$.usuario.razaoSocial").value("VetorGeo Integração Ltda"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.get("token").asText();
        assertThat(token).isNotBlank();

        // 3. Login com senha errada retorna 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("ana.integracao@vetorgeo.com.br", "Errada@999"))))
                .andExpect(status().isUnauthorized());

        // 4. Consultar perfil autenticado
        mockMvc.perform(get("/api/v1/account/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.statusContrato").value("Ativo"));

        // 5. Atualizar perfil (primeiro falha com senha atual incorreta -> 422)
        UpdateProfileRequest updateWrongPass = new UpdateProfileRequest(
                "Ana Paula Souza",
                "VetorGeo Engenharia Aérea Ltda",
                "Energia",
                "(41) 3319-4420",
                "ana.integracao@vetorgeo.com.br",
                "SenhaErrada@1"
        );

        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateWrongPass)))
                .andExpect(status().isUnprocessableEntity());

        // Agora atualiza com a senha atual correta -> 200
        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "Ana Paula Souza",
                "VetorGeo Engenharia Aérea Ltda",
                "Energia",
                "(41) 3319-4420",
                "ana.integracao@vetorgeo.com.br",
                "Vetor@2026"
        );

        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Paula Souza"))
                .andExpect(jsonPath("$.setorAtuacao").value("Energia"))
                .andExpect(jsonPath("$.telefone").value("(41) 3319-4420"));

        // 6. Alterar senha (primeiro falha com senha atual errada -> 422)
        mockMvc.perform(patch("/api/v1/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Errada@123", "NovaSenha@2026"))))
                .andExpect(status().isUnprocessableEntity());

        // Agora altera com senha atual correta -> 200
        mockMvc.perform(patch("/api/v1/account/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Vetor@2026", "NovaSenha@2026"))))
                .andExpect(status().isOk());

        // Login com nova senha -> 200
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("ana.integracao@vetorgeo.com.br", "NovaSenha@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String newToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 7. Forgot password -> 200
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("ana.integracao@vetorgeo.com.br"))))
                .andExpect(status().isOk());

        // 8. Desativação lógica exige senha atual correta (errada -> 422, correta -> 200)
        mockMvc.perform(patch("/api/v1/account/deactivate")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeactivateAccountRequest("Errada@123"))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/account/deactivate")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeactivateAccountRequest("NovaSenha@2026"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/account/profile")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("ana.integracao@vetorgeo.com.br", "NovaSenha@2026"))))
                .andExpect(status().isForbidden());
    }
}
