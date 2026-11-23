package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.EmpresaOperadoraRequest;
import com.drone.api.dto.request.LoginRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.Usuario;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmpresaOperadoraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaOperadoraRepository empresaRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        RegisterRequest operador = new RegisterRequest(
                "Carlos Operador",
                "AeroLog Matriz Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "operador@aerolog.com.br",
                "Aero@2026"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operador)))
                .andExpect(status().isCreated());

        // Promove usuário do teste para ADMIN para exercitar o CRUD global de operadoras
        Usuario admin = usuarioRepository.findByEmailIgnoreCase("operador@aerolog.com.br").orElseThrow();
        admin.setRole(RoleUsuario.ADMIN);
        usuarioRepository.save(admin);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("operador@aerolog.com.br", "Aero@2026"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        this.bearerToken = "Bearer " + token;
    }

    @Test
    @DisplayName("Deve bloquear acesso sem autenticação com 401")
    void deveBloquearSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/empresas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 2: cadastro, consulta por CNPJ, paginação/filtros, edição e status")
    void fluxoCompletoCrudEmpresas() throws Exception {
        // 1. Cadastrar nova empresa operadora (201)
        EmpresaOperadoraRequest nova = new EmpresaOperadoraRequest(
                "18.774.205/0001-45",
                "Horizonte AgroDrones S.A.",
                "Agricultura",
                "frota@horizonteagro.com.br",
                List.of("(62) 3201-7780", "(62) 98412-5590"),
                StatusContrato.Ativo
        );

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nova)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("18.774.205/0001-45"))
                .andExpect(jsonPath("$.razaoSocial").value("Horizonte AgroDrones S.A."))
                .andExpect(jsonPath("$.setorAtuacao").value("Agricultura"))
                .andExpect(jsonPath("$.statusContrato").value("Ativo"))
                .andExpect(jsonPath("$.telefones", hasSize(2)))
                .andExpect(jsonPath("$.telefones", hasItem("(62) 3201-7780")))
                .andExpect(jsonPath("$.telefones", hasItem("(62) 98412-5590")));

        // 2. Buscar por CNPJ (apenas dígitos) -> 200
        mockMvc.perform(get("/api/v1/empresas/18774205000145")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("18.774.205/0001-45"))
                .andExpect(jsonPath("$.email").value("frota@horizonteagro.com.br"));

        // 3. Buscar empresa inexistente -> 404
        mockMvc.perform(get("/api/v1/empresas/99999999000199")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 4. Listagem paginada e filtrada (2 empresas no banco: AeroLog + Horizonte)
        mockMvc.perform(get("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.number").value(0));

        // Filtro por busca de CNPJ parcial + setor + status
        mockMvc.perform(get("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .param("busca", "18.774")
                        .param("setor", "Agricultura")
                        .param("status", "Ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Horizonte AgroDrones S.A."));

        // 5. Atualizar dados cadastrais -> 200
        EmpresaOperadoraRequest edicao = new EmpresaOperadoraRequest(
                "18.774.205/0001-45",
                "Horizonte AgroDrones Brasil S.A.",
                "Geodésia",
                "operacoes@horizonteagro.com.br",
                List.of("(62) 99100-2200"),
                StatusContrato.Ativo
        );

        mockMvc.perform(put("/api/v1/empresas/18774205000145")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Horizonte AgroDrones Brasil S.A."))
                .andExpect(jsonPath("$.setorAtuacao").value("Geodésia"))
                .andExpect(jsonPath("$.email").value("operacoes@horizonteagro.com.br"))
                .andExpect(jsonPath("$.telefones", hasSize(1)))
                .andExpect(jsonPath("$.telefones[0]").value("(62) 99100-2200"));

        // 6. Alterar status contratual -> Suspenso e depois Inativo -> 200
        mockMvc.perform(patch("/api/v1/empresas/18774205000145/status")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AtualizarStatusContratoRequest(StatusContrato.Suspenso))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusContrato").value("Suspenso"));

        mockMvc.perform(patch("/api/v1/empresas/18774205000145/status")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AtualizarStatusContratoRequest(StatusContrato.Inativo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusContrato").value("Inativo"));
    }

    @Test
    @DisplayName("Deve validar conflitos (409) e regras de documento/telefone (422)")
    void deveValidarConflitosERegrasDeNegocio() throws Exception {
        // CNPJ já cadastrado no setup -> 409
        EmpresaOperadoraRequest cnpjDuplicado = new EmpresaOperadoraRequest(
                "27.419.830/0001-12",
                "Outra Operadora Ltda",
                "Energia",
                "contato@outra.com.br",
                List.of("(11) 98888-1111"),
                StatusContrato.Ativo
        );

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cnpjDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // E-mail já cadastrado -> 409
        EmpresaOperadoraRequest emailDuplicado = new EmpresaOperadoraRequest(
                "11.222.333/0001-44",
                "Skyline Drones Ltda",
                "Energia",
                "operador@aerolog.com.br",
                List.of("(11) 98888-2222"),
                StatusContrato.Ativo
        );

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Documento inválido (12 dígitos) -> 422
        EmpresaOperadoraRequest documentoInvalido = new EmpresaOperadoraRequest(
                "123456789012",
                "Drone Teste Ltda",
                "Segurança",
                "teste@droneteste.com.br",
                List.of("(11) 98888-3333"),
                StatusContrato.Ativo
        );

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentoInvalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Telefone inválido (< 10 dígitos) -> 422
        EmpresaOperadoraRequest telefoneInvalido = new EmpresaOperadoraRequest(
                "11.222.333/0001-55",
                "Drone Telefone Ltda",
                "Segurança",
                "tel@droneteste.com.br",
                List.of("119999"),
                StatusContrato.Ativo
        );

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(telefoneInvalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
