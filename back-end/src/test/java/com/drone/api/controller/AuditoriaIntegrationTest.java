package com.drone.api.controller;

import com.drone.api.dto.request.AuditoriaRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.request.OperacaoVooRequest;
import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.NivelGravidade;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.repository.AuditoriaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

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
    private Integer operacaoCodigo;

    @BeforeEach
    void setUp() throws Exception {
        limparBase();

        RegisterRequest operador = new RegisterRequest(
                "Auditoria Técnica",
                "AeroLog Compliance Ltda",
                "27.419.830/0001-12",
                "Logística",
                "(11) 99888-1001",
                "auditoria@aerolog.com.br",
                "Aero@2026"
        );

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operador)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(regRes.getResponse().getContentAsString())
                .get("token").asText();
        this.bearerToken = "Bearer " + token;

        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DroneRequest(
                                "PR-401", "DJI", "Matrice 300 RTK", "SN-AUD-001",
                                new BigDecimal("9.00"), TipoUsoDrone.avancado,
                                LocalDate.now().plusMonths(6), "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotoRequest(
                                "328.491.720-11", "SP12345678", "Carlos Silva",
                                "carlos.aud@aerolog.com.br", "ANAC-RPAS-88412",
                                LocalDate.now().plusMonths(6), List.of("(11) 99120-4410"),
                                "27.419.830/0001-12"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClienteRequest(
                                "12.345.678/0001-90", "MG12345678", "Fazenda Boa Vista",
                                "27.419.830/0001-12", StatusContrato.Ativo,
                                LocalDate.of(2026, 1, 10), List.of("(62) 3341-2200")
                        ))))
                .andExpect(status().isCreated());

        MvcResult opRes = mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OperacaoVooRequest(
                                "27.419.830/0001-12",
                                "PR-401",
                                "328.491.720-11",
                                "12.345.678/0001-90",
                                "Voo de inspeção com auditoria",
                                "Hangar Norte",
                                "Setor Sul",
                                "Área agrícola controlada",
                                "Rural",
                                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                                60
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        this.operacaoCodigo = objectMapper.readTree(opRes.getResponse().getContentAsString())
                .get("codigo").asInt();
    }

    @AfterEach
    void tearDown() {
        limparBase();
    }

    private void limparBase() {
        auditoriaRepository.deleteAll();
        operacaoRepository.deleteAll();
        droneRepository.deleteAll();
        pilotoRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
        empresaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve bloquear acesso sem autenticação com 401")
    void deveBloquearSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/auditorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Fluxo completo MOD-BE 7: registro imutável, consulta por código, por operação e filtro por gravidade")
    void fluxoCompletoAuditorias() throws Exception {
        LocalDateTime evento1 = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime evento2 = evento1.plusMinutes(15);

        // 1. Registrar ocorrência MEDIO -> 201
        AuditoriaRequest desvio = new AuditoriaRequest(
                operacaoCodigo,
                evento1,
                "Desvio de Rota",
                NivelGravidade.MEDIO,
                "Aeronave ajustou trajetória lateral em 45m devido a rajada de vento no quadrante sul."
        );

        MvcResult res1 = mockMvc.perform(post("/api/v1/auditorias")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(desvio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").isNumber())
                .andExpect(jsonPath("$.operacaoVooCodigo").value(operacaoCodigo))
                .andExpect(jsonPath("$.nivelGravidade").value("MEDIO"))
                .andExpect(jsonPath("$.pilotoNome").value("Carlos Silva"))
                .andExpect(jsonPath("$.droneCodigo").value("PR-401"))
                .andReturn();

        int codigoAuditoria = objectMapper.readTree(res1.getResponse().getContentAsString())
                .get("codigo").asInt();

        // 2. Registrar ocorrência CRITICO -> 201
        AuditoriaRequest critico = new AuditoriaRequest(
                operacaoCodigo,
                evento2,
                "Incidente Aéreo",
                NivelGravidade.CRITICO,
                "Proximidade de aeronave tripulada em baixa altitude; protocolo de descida preventiva executado."
        );

        mockMvc.perform(post("/api/v1/auditorias")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(critico)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivelGravidade").value("CRITICO"));

        // 3. Consultar por código -> 200 e inexistente -> 404
        mockMvc.perform(get("/api/v1/auditorias/" + codigoAuditoria)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Desvio de Rota"));

        mockMvc.perform(get("/api/v1/auditorias/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 4. Listar ocorrências por operação -> 2 registros
        mockMvc.perform(get("/api/v1/auditorias/operacao/" + operacaoCodigo)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 5. Filtrar listagem paginada por nivelGravidade=CRITICO
        mockMvc.perform(get("/api/v1/auditorias")
                        .header("Authorization", bearerToken)
                        .param("nivelGravidade", "CRITICO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].categoria").value("Incidente Aéreo"));
    }

    @Test
    @DisplayName("Deve exigir operação de voo existente (404) e descrição detalhada mínima (400)")
    void deveValidarOperacaoExistenteEDescricao() throws Exception {
        // Operação inexistente -> 404
        AuditoriaRequest vooInexistente = new AuditoriaRequest(
                999999,
                LocalDateTime.now(),
                "Alerta Meteorológico",
                NivelGravidade.ALTO,
                "Formação repentina de tempestade convectiva na região de operação do drone."
        );

        mockMvc.perform(post("/api/v1/auditorias")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vooInexistente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Descrição muito curta (< 15 caracteres) -> 400
        AuditoriaRequest descricaoCurta = new AuditoriaRequest(
                operacaoCodigo,
                LocalDateTime.now(),
                "Inspeção",
                NivelGravidade.BAIXO,
                "Voo ok."
        );

        mockMvc.perform(post("/api/v1/auditorias")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(descricaoCurta)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
