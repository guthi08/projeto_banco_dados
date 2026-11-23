package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusVooRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.request.OperacaoVooRequest;
import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.StatusVoo;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.DroneRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.OperacaoVooRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperacaoVooIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperacaoVooRepository operacaoRepository;

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private PilotoRepository pilotoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaOperadoraRepository empresaRepository;

    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        limparBase();

        RegisterRequest operador = new RegisterRequest(
                "Despacho Central",
                "AeroLog Operações Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "despacho@aerolog.com.br",
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

        // Cadastra drone válido e drone vencido
        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DroneRequest(
                                "PR-401", "DJI", "Matrice 300 RTK", "SN-OP-001",
                                new BigDecimal("9.00"), TipoUsoDrone.avancado,
                                LocalDate.now().plusMonths(6), "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DroneRequest(
                                "PS-882", "DJI", "Agras T30", "SN-OP-002",
                                new BigDecimal("78.00"), TipoUsoDrone.avancado,
                                LocalDate.now().minusDays(5), "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        // Cadastra piloto válido e piloto com CMA vencido
        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotoRequest(
                                "328.491.720-11", "SP12345678", "Carlos Silva",
                                "carlos.op@aerolog.com.br", "ANAC-RPAS-88412",
                                LocalDate.now().plusMonths(6), List.of("(11) 99120-4410"),
                                "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotoRequest(
                                "289.114.602-55", "SP45678912", "Roberto Cruz",
                                "roberto.op@aerolog.com.br", "ANAC-RPAS-54908",
                                LocalDate.now().minusDays(3), List.of("(11) 99310-8871"),
                                "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        // Cadastra cliente ativo
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClienteRequest(
                                "12.345.678/0001-90", "MG12345678", "Fazenda Boa Vista",
                                "27.419.830/0001-12", StatusContrato.Ativo,
                                LocalDate.of(2026, 1, 10), List.of("(62) 3341-2200")
                        ))))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        limparBase();
    }

    private void limparBase() {
        operacaoRepository.deleteAll();
        droneRepository.deleteAll();
        pilotoRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 6: agendar -> conflito 409 -> iniciar -> concluir -> filtros")
    void fluxoCompletoOperacaoVoo() throws Exception {
        LocalDateTime horario = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);

        OperacaoVooRequest req = new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PR-401",
                "328.491.720-11",
                "12.345.678/0001-90",
                "Inspeção termográfica de lavoura",
                "Hangar Norte — Ribeirão Preto",
                "Setor Sul — Fazenda Boa Vista",
                "Pivôs centrais de irrigação",
                "Rural",
                horario,
                60
        );

        // 1. Agendar voo -> 201
        MvcResult createRes = mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").isNumber())
                .andExpect(jsonPath("$.statusVoo").value("AGENDADO"))
                .andExpect(jsonPath("$.droneCodigo").value("PR-401"))
                .andExpect(jsonPath("$.pilotoNome").value("Carlos Silva"))
                .andExpect(jsonPath("$.clienteNome").value("Fazenda Boa Vista"))
                .andReturn();

        int codigo = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("codigo").asInt();

        // 2. Tentar agendar no mesmo intervalo -> 409 Conflict
        OperacaoVooRequest sobreposto = new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PR-401",
                "328.491.720-11",
                "12.345.678/0001-90",
                "Outro voo conflitante",
                "Base A",
                "Base B",
                "Setor Leste",
                "Rural",
                horario.plusMinutes(30),
                60
        );

        mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sobreposto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 3. Filtrar por status AGENDADO (usa idx_status_voo)
        mockMvc.perform(get("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .param("status", "AGENDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigo").value(codigo));

        // 4. Iniciar voo (AGENDADO -> EM_ANDAMENTO) -> grava dataHoraReal
        mockMvc.perform(patch("/api/v1/operacoes/" + codigo + "/status")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AtualizarStatusVooRequest(StatusVoo.EM_ANDAMENTO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVoo").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dataHoraReal").isNotEmpty());

        // 5. Concluir voo (EM_ANDAMENTO -> CONCLUIDO) -> grava dataHoraPousoReal
        mockMvc.perform(patch("/api/v1/operacoes/" + codigo + "/status")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AtualizarStatusVooRequest(StatusVoo.CONCLUIDO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVoo").value("CONCLUIDO"))
                .andExpect(jsonPath("$.dataHoraPousoReal").isNotEmpty());
    }

    @Test
    @DisplayName("Deve bloquear via HTTP 422 drone com ANAC vencida e piloto com CMA vencido")
    void deveBloquearTravasSegurancaHttp() throws Exception {
        LocalDateTime horario = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        // Drone PS-882 com ANAC expirada -> 422
        OperacaoVooRequest reqDroneVencido = new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PS-882",
                "328.491.720-11",
                "12.345.678/0001-90",
                "Pulverização agrícola",
                "Hangar Norte",
                "Talhão Sul",
                "Área de cultivo",
                "Rural",
                horario,
                60
        );

        mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDroneVencido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // Piloto Roberto com CMA vencido -> 422
        OperacaoVooRequest reqPilotoVencido = new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PR-401",
                "289.114.602-55",
                "12.345.678/0001-90",
                "Pulverização agrícola",
                "Hangar Norte",
                "Talhão Sul",
                "Área de cultivo",
                "Rural",
                horario,
                60
        );

        mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqPilotoVencido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }
}
