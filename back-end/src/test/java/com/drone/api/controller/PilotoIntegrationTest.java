package com.drone.api.controller;

import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.PilotoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PilotoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PilotoRepository pilotoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaOperadoraRepository empresaRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        pilotoRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        RegisterRequest operador = new RegisterRequest(
                "Coordenação Pilotos",
                "AeroLog Pilotos Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "pilotos@aerolog.com.br",
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
        pilotoRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve bloquear acesso sem autenticação com 401")
    void deveBloquearSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/pilotos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 4: cadastro, filtros por nome/CMA, edição de telefones e remoção")
    void fluxoCompletoCrudPilotos() throws Exception {
        LocalDate cmaValido = LocalDate.now().plusMonths(8);
        LocalDate cmaVencido = LocalDate.now().minusDays(15);

        // 1. Cadastrar piloto com CMA válido e 2 telefones
        PilotoRequest carlos = new PilotoRequest(
                "328.491.720-11",
                "sp12345678",
                "Carlos Silva",
                "carlos.silva@aerolog.com.br",
                "anac-rpas-88412",
                cmaValido,
                List.of("(11) 99120-4410", "(11) 97777-1111"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carlos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("328.491.720-11"))
                .andExpect(jsonPath("$.rg").value("SP12345678"))
                .andExpect(jsonPath("$.numRegistroLicenca").value("ANAC-RPAS-88412"))
                .andExpect(jsonPath("$.cmaValido").value(true))
                .andExpect(jsonPath("$.telefones", hasSize(2)))
                .andExpect(jsonPath("$.telefones", hasItem("(11) 99120-4410")))
                .andExpect(jsonPath("$.telefones", hasItem("(11) 97777-1111")));

        // 2. Cadastrar segundo piloto com CMA vencido
        PilotoRequest roberto = new PilotoRequest(
                "289.114.602-55",
                "SP45678912",
                "Roberto Cruz",
                "roberto.cruz@aerolog.com.br",
                "ANAC-RPAS-54908",
                cmaVencido,
                List.of("(11) 99310-8871"),
                "27419830000112"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roberto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cmaValido").value(false));

        // 3. Buscar por CPF (com ou sem máscara) -> 200 e inexistente -> 404
        mockMvc.perform(get("/api/v1/pilotos/32849172011")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Silva"));

        mockMvc.perform(get("/api/v1/pilotos/99999999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 4. Listagem filtrada por nome (índice idx_nome_piloto) e apenasCmaValido=true
        mockMvc.perform(get("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .param("nome", "Carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cpf").value("328.491.720-11"));

        mockMvc.perform(get("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .param("apenasCmaValido", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Carlos Silva"));

        // 5. Atualizar piloto com CMA renovado e novos telefones
        PilotoRequest robertoRenovado = new PilotoRequest(
                "289.114.602-55",
                "SP45678912",
                "Roberto Cruz Jr",
                "roberto.jr@aerolog.com.br",
                "ANAC-RPAS-54908",
                LocalDate.now().plusYears(1),
                List.of("(11) 3842-9100"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(put("/api/v1/pilotos/28911460255")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(robertoRenovado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Roberto Cruz Jr"))
                .andExpect(jsonPath("$.cmaValido").value(true))
                .andExpect(jsonPath("$.telefones", hasSize(1)))
                .andExpect(jsonPath("$.telefones[0]").value("(11) 3842-9100"));

        // 6. Remover piloto -> 204 e confirmar 404
        mockMvc.perform(delete("/api/v1/pilotos/28911460255")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pilotos/28911460255")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve validar duplicidades (409) e regras de CPF/telefone (422)")
    void deveValidarDuplicidadesERegras() throws Exception {
        LocalDate futuro = LocalDate.now().plusMonths(10);

        PilotoRequest base = new PilotoRequest(
                "419.805.233-04",
                "PR23456789",
                "Ana Souza",
                "ana.souza@aerolog.com.br",
                "ANAC-RPAS-77319",
                futuro,
                List.of("(41) 99844-1102"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(base)))
                .andExpect(status().isCreated());

        // CPF duplicado -> 409
        PilotoRequest cpfDuplicado = new PilotoRequest(
                "41980523304",
                "PR99999999",
                "Outro Nome",
                "outro@aerolog.com.br",
                "ANAC-RPAS-99999",
                futuro,
                List.of("(41) 99844-0000"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cpfDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Licença duplicada -> 409
        PilotoRequest licencaDuplicada = new PilotoRequest(
                "512.339.881-90",
                "GO34567891",
                "Marcos Lima",
                "marcos@aerolog.com.br",
                "ANAC-RPAS-77319",
                futuro,
                List.of("(62) 98412-5590"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(licencaDuplicada)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // CPF com quantidade inválida de dígitos (12 dígitos) -> 422
        PilotoRequest cpfInvalido = new PilotoRequest(
                "123456789012",
                "SP11223344",
                "Piloto Inválido",
                "invalido@aerolog.com.br",
                "ANAC-RPAS-11111",
                futuro,
                List.of("(11) 99999-1111"),
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cpfInvalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
