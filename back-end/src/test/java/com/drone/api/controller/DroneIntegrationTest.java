package com.drone.api.controller;

import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.repository.DroneRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

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
class DroneIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaOperadoraRepository empresaRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        droneRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();

        RegisterRequest operador = new RegisterRequest(
                "Marcos Frota",
                "AeroLog Frota Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "frota@aerolog.com.br",
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
        droneRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve bloquear acesso sem autenticação com 401")
    void deveBloquearSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/drones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 3: cadastro, filtros ANAC/modelo, edição e remoção")
    void fluxoCompletoCrudDrones() throws Exception {
        LocalDate futuro = LocalDate.now().plusMonths(6);
        LocalDate vencido = LocalDate.now().minusDays(10);

        // 1. Cadastrar drone válido (prefixo em minúscula deve ser normalizado para maiúscula)
        DroneRequest valido = new DroneRequest(
                "pr-401",
                "DJI",
                "Matrice 300 RTK",
                "SN-M300-88421",
                new BigDecimal("9.00"),
                TipoUsoDrone.avancado,
                futuro,
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("PR-401"))
                .andExpect(jsonPath("$.modelo").value("Matrice 300 RTK"))
                .andExpect(jsonPath("$.tipoUso").value("avancado"))
                .andExpect(jsonPath("$.empresaOpeCnpj").value("27.419.830/0001-12"))
                .andExpect(jsonPath("$.anacValido").value(true));

        // 2. Cadastrar segundo drone com cadastro ANAC expirado
        DroneRequest expirado = new DroneRequest(
                "PS-882",
                "DJI",
                "Agras T30",
                "SN-T30-99104",
                new BigDecimal("78.00"),
                TipoUsoDrone.avancado,
                vencido,
                "27419830000112"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expirado)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("PS-882"))
                .andExpect(jsonPath("$.anacValido").value(false));

        // 3. Buscar por código -> 200 e inexistente -> 404
        mockMvc.perform(get("/api/v1/drones/PR-401")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSerie").value("SN-M300-88421"));

        mockMvc.perform(get("/api/v1/drones/PP-999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 4. Listagem filtrada por modelo (índice idx_modelo_drone) e por apenasValidos=true
        mockMvc.perform(get("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .param("modelo", "Matrice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigo").value("PR-401"));

        mockMvc.perform(get("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .param("apenasValidos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigo").value("PR-401"));

        // 5. Atualizar drone expirado renovando a validade ANAC -> anacValido passa para true
        DroneRequest renovado = new DroneRequest(
                "PS-882",
                "DJI",
                "Agras T30 Pro",
                "SN-T30-99104",
                new BigDecimal("78.50"),
                TipoUsoDrone.avancado,
                LocalDate.now().plusYears(1),
                "27.419.830/0001-12"
        );

        mockMvc.perform(put("/api/v1/drones/PS-882")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renovado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Agras T30 Pro"))
                .andExpect(jsonPath("$.anacValido").value(true));

        // 6. Remover drone -> 204 e confirmar 404 após remoção
        mockMvc.perform(delete("/api/v1/drones/PS-882")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/drones/PS-882")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve rejeitar prefixo fora do padrão ANAC (400), duplicidades (409) e empresa inexistente (404)")
    void deveValidarRegrasEDuplicidades() throws Exception {
        LocalDate futuro = LocalDate.now().plusMonths(12);

        // Prefixo inválido (PT não é aceito) -> 400 Bean Validation
        DroneRequest prefixoInvalido = new DroneRequest(
                "PT-123",
                "DJI",
                "Mavic 3",
                "SN-MV3-0001",
                new BigDecimal("0.92"),
                TipoUsoDrone.basico,
                futuro,
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefixoInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Tentar cadastrar drone vinculado a outra empresa sendo OPERADOR -> 403 Forbidden
        DroneRequest empresaDeTerceiro = new DroneRequest(
                "PP-209",
                "DJI",
                "Mavic 3 Enterprise",
                "SN-M3E-44210",
                new BigDecimal("0.92"),
                TipoUsoDrone.basico,
                futuro,
                "99.999.999/0001-99"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empresaDeTerceiro)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Cadastro válido inicial
        DroneRequest base = new DroneRequest(
                "PP-209",
                "DJI",
                "Mavic 3 Enterprise",
                "SN-M3E-44210",
                new BigDecimal("0.92"),
                TipoUsoDrone.basico,
                futuro,
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(base)))
                .andExpect(status().isCreated());

        // Código duplicado -> 409
        DroneRequest codigoDuplicado = new DroneRequest(
                "PP-209",
                "DJI",
                "Outro Modelo",
                "SN-OUTRO-9999",
                new BigDecimal("1.50"),
                TipoUsoDrone.basico,
                futuro,
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codigoDuplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Número de série duplicado -> 409
        DroneRequest serieDuplicada = new DroneRequest(
                "PR-999",
                "DJI",
                "Outro Modelo",
                "SN-M3E-44210",
                new BigDecimal("1.50"),
                TipoUsoDrone.basico,
                futuro,
                "27.419.830/0001-12"
        );

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serieDuplicada)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
