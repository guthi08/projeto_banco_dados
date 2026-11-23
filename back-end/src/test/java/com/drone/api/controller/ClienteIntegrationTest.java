package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.StatusContrato;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaOperadoraRepository empresaRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        RegisterRequest operador = new RegisterRequest(
                "Comercial Operadora",
                "AeroLog Comercial Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "comercial@aerolog.com.br",
                "Aero@2026"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operador)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        this.bearerToken = "Bearer " + token;
    }

    @AfterEach
    void tearDown() {
        clienteRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve bloquear acesso sem autenticação com 401")
    void deveBloquearSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 5: cadastro, busca, filtros por nome/status, edição, PATCH status e remoção")
    void fluxoCompletoCrudClientes() throws Exception {
        // 1. Cadastrar cliente ativo com múltiplos telefones
        ClienteRequest fazenda = new ClienteRequest(
                "12.345.678/0001-90",
                "mg12345678",
                "Fazenda Boa Vista",
                "27.419.830/0001-12",
                StatusContrato.Ativo,
                LocalDate.of(2026, 1, 10),
                List.of("(62) 3341-2200", "(62) 99100-2200")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fazenda)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfRg").value("12.345.678/0001-90"))
                .andExpect(jsonPath("$.rg").value("MG12345678"))
                .andExpect(jsonPath("$.nome").value("Fazenda Boa Vista"))
                .andExpect(jsonPath("$.statusContrato").value("Ativo"))
                .andExpect(jsonPath("$.telefones", hasSize(2)))
                .andExpect(jsonPath("$.telefones", hasItem("(62) 3341-2200")))
                .andExpect(jsonPath("$.telefones", hasItem("(62) 99100-2200")));

        // 2. Cadastrar cliente inativo (pessoa física CPF)
        ClienteRequest produtor = new ClienteRequest(
                "321.654.987-00",
                "RS67890123",
                "Produtora Filme Brasil",
                "27419830000112",
                StatusContrato.Inativo,
                LocalDate.of(2025, 11, 10),
                List.of("(51) 3214-7700")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produtor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfRg").value("321.654.987-00"))
                .andExpect(jsonPath("$.statusContrato").value("Inativo"));

        // 3. Consultar por documento -> 200 e inexistente -> 404
        mockMvc.perform(get("/api/v1/clientes/12345678000190")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fazenda Boa Vista"));

        mockMvc.perform(get("/api/v1/clientes/99999999000199")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 4. Listagem filtrada por nome (índice idx_nome_cliente) e por status
        mockMvc.perform(get("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .param("nome", "Fazenda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cpfRg").value("12.345.678/0001-90"));

        mockMvc.perform(get("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .param("status", "Inativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Produtora Filme Brasil"));

        // 5. Editar cliente
        ClienteRequest fazendaEditada = new ClienteRequest(
                "12.345.678/0001-90",
                "MG12345678",
                "Fazenda Boa Vista Agro S.A.",
                "27.419.830/0001-12",
                StatusContrato.Ativo,
                LocalDate.of(2026, 2, 1),
                List.of("(62) 99999-8888")
        );

        mockMvc.perform(put("/api/v1/clientes/12345678000190")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fazendaEditada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fazenda Boa Vista Agro S.A."))
                .andExpect(jsonPath("$.telefones", hasSize(1)))
                .andExpect(jsonPath("$.telefones[0]").value("(62) 99999-8888"));

        // 6. Alterar status contratual (PATCH)
        mockMvc.perform(patch("/api/v1/clientes/12345678000190/status")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AtualizarStatusContratoRequest(StatusContrato.Inativo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusContrato").value("Inativo"));

        // 7. Remover cliente -> 204 e confirmar 404
        mockMvc.perform(delete("/api/v1/clientes/32165498700")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/clientes/32165498700")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve rejeitar status Suspenso para cliente (422) e duplicidades de documento/RG (409)")
    void deveValidarRegrasEDuplicidades() throws Exception {
        // Status Suspenso não é permitido para cliente -> 422
        ClienteRequest statusInvalido = new ClienteRequest(
                "33.812.904/0001-17",
                "RJ45678901",
                "Construtora Engel",
                "27.419.830/0001-12",
                StatusContrato.Suspenso,
                LocalDate.of(2026, 3, 12),
                List.of("(11) 3045-8900")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusInvalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Cadastra cliente válido
        ClienteRequest base = new ClienteRequest(
                "33.812.904/0001-17",
                "RJ45678901",
                "Construtora Engel",
                "27.419.830/0001-12",
                StatusContrato.Ativo,
                LocalDate.of(2026, 3, 12),
                List.of("(11) 3045-8900")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(base)))
                .andExpect(status().isCreated());

        // Documento duplicado -> 409
        ClienteRequest docDuplicado = new ClienteRequest(
                "33812904000117",
                "SP99998888",
                "Outro Cliente",
                "27.419.830/0001-12",
                StatusContrato.Ativo,
                LocalDate.of(2026, 3, 15),
                List.of("(11) 98210-8900")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // RG duplicado -> 409
        ClienteRequest rgDuplicado = new ClienteRequest(
                "46.379.400/0001-50",
                "RJ45678901",
                "Defesa Civil",
                "27.419.830/0001-12",
                StatusContrato.Ativo,
                LocalDate.of(2026, 3, 20),
                List.of("(11) 2193-8888")
        );

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rgDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
