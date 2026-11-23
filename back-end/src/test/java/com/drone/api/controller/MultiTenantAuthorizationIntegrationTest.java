package com.drone.api.controller;

import com.drone.api.dto.request.AuditoriaRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.request.EmpresaOperadoraRequest;
import com.drone.api.dto.request.LoginRequest;
import com.drone.api.dto.request.OperacaoVooRequest;
import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.entity.NivelGravidade;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.entity.Usuario;
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
class MultiTenantAuthorizationIntegrationTest {

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

    @BeforeEach
    void setUp() {
        limparBase();
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
    @DisplayName("Deve isolar dados entre empresas (OPERADOR) e liberar visão global apenas para ADMIN")
    void deveIsolarOperadoresELiberarAdmin() throws Exception {
        // 1. Registra Empresa A (perfil OPERADOR)
        MvcResult regA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Gestor Alpha", "Alpha Drones Ltda", "11.111.111/0001-11",
                                "Logística", "(11) 98888-1111", "alpha@drones.com.br", "Alpha@2026"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.role").value("OPERADOR"))
                .andReturn();
        String tokenA = "Bearer " + objectMapper.readTree(regA.getResponse().getContentAsString()).get("token").asText();

        // 2. Registra Empresa B (perfil OPERADOR)
        MvcResult regB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Gestor Beta", "Beta Drones Ltda", "22.222.222/0001-22",
                                "Geodésia", "(41) 98888-2222", "beta@drones.com.br", "Beta@2026"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.role").value("OPERADOR"))
                .andReturn();
        String tokenB = "Bearer " + objectMapper.readTree(regB.getResponse().getContentAsString()).get("token").asText();

        // 3. OPERADOR A vê apenas sua própria empresa e NÃO pode cadastrar outra operadora (403)
        mockMvc.perform(get("/api/v1/empresas").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cnpj").value("11.111.111/0001-11"));

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmpresaOperadoraRequest(
                                "33.333.333/0001-33", "Gamma Ltda", "Energia",
                                "gamma@drones.com.br", List.of("(11) 98888-3333"), StatusContrato.Ativo
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 4. OPERADOR A cadastra Drone, Piloto, Cliente, Operação e Auditoria da Empresa A
        mockMvc.perform(post("/api/v1/drones")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DroneRequest(
                                "PR-101", "DJI", "Matrice 300", "SN-ALPHA-01",
                                new BigDecimal("9.00"), TipoUsoDrone.avancado,
                                LocalDate.now().plusMonths(6), "11.111.111/0001-11"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pilotos")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PilotoRequest(
                                "111.222.333-44", "SP11122233", "Piloto Alpha",
                                "piloto@alpha.com.br", "ANAC-ALPHA-01",
                                LocalDate.now().plusMonths(6), List.of("(11) 97777-1111"),
                                "11.111.111/0001-11"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClienteRequest(
                                "44.444.444/0001-44", "SP44445555", "Cliente Alpha",
                                "11.111.111/0001-11", StatusContrato.Ativo,
                                LocalDate.of(2026, 1, 15), List.of("(11) 3000-4444")
                        ))))
                .andExpect(status().isCreated());

        MvcResult opA = mockMvc.perform(post("/api/v1/operacoes")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OperacaoVooRequest(
                                "11.111.111/0001-11", "PR-101", "111.222.333-44", "44.444.444/0001-44",
                                "Voo exclusivo da Empresa Alpha", "Base Alpha", "Destino Alpha",
                                "Área privada Alpha", "Rural",
                                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
                                60
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        int opCodigoA = objectMapper.readTree(opA.getResponse().getContentAsString()).get("codigo").asInt();

        mockMvc.perform(post("/api/v1/auditorias")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuditoriaRequest(
                                opCodigoA, LocalDateTime.now(), "Inspeção de Segurança",
                                NivelGravidade.BAIXO, "Checklist completo realizado pela equipe Alpha sem desvios."
                        ))))
                .andExpect(status().isCreated());

        // 5. OPERADOR B NÃO enxerga nenhum dado da Empresa A nas listagens nem por acesso direto (403)
        mockMvc.perform(get("/api/v1/drones").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/pilotos").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/clientes").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/operacoes").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/auditorias").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/drones/PR-101").header("Authorization", tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/operacoes/" + opCodigoA).header("Authorization", tokenB))
                .andExpect(status().isForbidden());

        // 6. Promove usuário B para ADMIN e valida visão global de todo o sistema
        Usuario usuarioAdmin = usuarioRepository.findByEmailIgnoreCase("beta@drones.com.br").orElseThrow();
        usuarioAdmin.setRole(RoleUsuario.ADMIN);
        usuarioRepository.save(usuarioAdmin);

        MvcResult loginAdmin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("beta@drones.com.br", "Beta@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.role").value("ADMIN"))
                .andReturn();
        String tokenAdmin = "Bearer " + objectMapper.readTree(loginAdmin.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/v1/empresas").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/v1/drones").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/operacoes").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/auditorias").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
