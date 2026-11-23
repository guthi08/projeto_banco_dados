package com.drone.api.service;

import com.drone.api.dto.request.AtualizarStatusVooRequest;
import com.drone.api.dto.request.OperacaoVooRequest;
import com.drone.api.dto.response.OperacaoVooResponse;
import com.drone.api.entity.Cliente;
import com.drone.api.entity.Drone;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.OperacaoVoo;
import com.drone.api.entity.Piloto;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.StatusVoo;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.DroneRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.OperacaoVooRepository;
import com.drone.api.repository.PilotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacaoVooServiceTest {

    @Mock
    private OperacaoVooRepository operacaoRepository;
    @Mock
    private EmpresaOperadoraRepository empresaRepository;
    @Mock
    private DroneRepository droneRepository;
    @Mock
    private PilotoRepository pilotoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TenantContextService tenantContext;

    @InjectMocks
    private OperacaoVooService service;

    private EmpresaOperadora empresa;
    private Drone droneValido;
    private Piloto pilotoValido;
    private Cliente clienteAtivo;
    private LocalDateTime dataFutura;

    @BeforeEach
    void setUp() {
        dataFutura = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        empresa = EmpresaOperadora.builder()
                .cnpj("27419830000112")
                .razaoSocial("AeroLog Brasil Ltda")
                .setorAtuacao("Logística")
                .email("operacoes@aerolog.com.br")
                .statusContrato(StatusContrato.Ativo)
                .build();

        droneValido = Drone.builder()
                .codigo("PR-401")
                .fabricante("DJI")
                .modelo("Matrice 300 RTK")
                .numSerie("SN-M300-88421")
                .pesoMaxDecolagem(new BigDecimal("9.00"))
                .tipoUso(TipoUsoDrone.avancado)
                .dataValidadeCadastro(LocalDate.now().plusMonths(6))
                .empresaOperadora(empresa)
                .build();

        pilotoValido = Piloto.builder()
                .cpf("32849172011")
                .rg("SP12345678")
                .nome("Carlos Silva")
                .email("carlos@aerolog.com.br")
                .numRegistroLicenca("ANAC-RPAS-88412")
                .validadeCma(LocalDate.now().plusMonths(6))
                .empresaOperadora(empresa)
                .build();

        clienteAtivo = Cliente.builder()
                .cpfRg("12345678000190")
                .rg("MG12345678")
                .nome("Fazenda Boa Vista")
                .statusContrato(StatusContrato.Ativo)
                .dataFormalizacao(LocalDate.of(2026, 1, 10))
                .empresaOperadora(empresa)
                .build();
    }

    private OperacaoVooRequest buildRequest(LocalDateTime dataHora) {
        return new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PR-401",
                "328.491.720-11",
                "12.345.678/0001-90",
                "Monitoramento termográfico de pivôs",
                "Hangar Norte",
                "Setor Sul — Fazenda",
                "Pivôs centrais de irrigação",
                "Rural",
                dataHora,
                60
        );
    }

    private void mockDependenciasPadrao(Drone drone, Piloto piloto, Cliente cliente) {
        when(tenantContext.resolverCnpjParaEscrita(any())).thenAnswer(inv -> inv.getArgument(0));
        when(empresaRepository.findById("27419830000112")).thenReturn(Optional.of(empresa));
        when(droneRepository.findWithEmpresaByCodigoIgnoreCase("PR-401")).thenReturn(Optional.of(drone));
        when(pilotoRepository.findWithDetailsByCpf("32849172011")).thenReturn(Optional.of(piloto));
        when(clienteRepository.findWithDetailsByCpfRg("12345678000190")).thenReturn(Optional.of(cliente));
    }

    @Test
    @DisplayName("Deve bloquear agendamento quando cadastro ANAC do drone estiver vencido na data do voo")
    void deveBloquearDroneComAnacExpirada() {
        droneValido.setDataValidadeCadastro(dataFutura.toLocalDate().minusDays(1));
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);

        assertThatThrownBy(() -> service.agendar(buildRequest(dataFutura)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ANAC expirado");
    }

    @Test
    @DisplayName("Deve bloquear agendamento quando CMA do piloto estiver vencido na data do voo")
    void deveBloquearPilotoComCmaVencido() {
        pilotoValido.setValidadeCma(dataFutura.toLocalDate().minusDays(1));
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);

        assertThatThrownBy(() -> service.agendar(buildRequest(dataFutura)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CMA");
    }

    @Test
    @DisplayName("Deve bloquear agendamento quando origem e destino forem iguais")
    void deveBloquearOrigemIgualDestino() {
        OperacaoVooRequest req = new OperacaoVooRequest(
                "27.419.830/0001-12",
                "PR-401",
                "328.491.720-11",
                "12.345.678/0001-90",
                "Voo de inspeção",
                "Hangar Norte",
                "hangar norte",
                "Área industrial",
                "Industrial",
                dataFutura,
                60
        );

        assertThatThrownBy(() -> service.agendar(req))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("distintos");
    }

    @Test
    @DisplayName("Deve bloquear agendamento quando cliente estiver com contrato inativo")
    void deveBloquearClienteInativo() {
        clienteAtivo.setStatusContrato(StatusContrato.Inativo);
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);

        assertThatThrownBy(() -> service.agendar(buildRequest(dataFutura)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    @DisplayName("Deve bloquear sobreposição de horário para o mesmo drone com ConflitoException (409)")
    void deveBloquearConflitoHorarioDrone() {
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);

        OperacaoVoo existente = OperacaoVoo.builder()
                .codigo(50)
                .statusVoo(StatusVoo.AGENDADO)
                .dataHoraPlanejada(dataFutura.plusMinutes(30))
                .duracaoEstimadaMin(60)
                .drone(droneValido)
                .piloto(Piloto.builder().cpf("99988877766").build())
                .cliente(clienteAtivo)
                .empresaOperadora(empresa)
                .build();

        when(operacaoRepository.findActiveForConflictCheck(any(), eq("PR-401"), eq("32849172011"), eq(null)))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.agendar(buildRequest(dataFutura)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("drone PR-401");
    }

    @Test
    @DisplayName("Deve bloquear sobreposição de horário para o mesmo piloto com ConflitoException (409)")
    void deveBloquearConflitoHorarioPiloto() {
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);

        OperacaoVoo existente = OperacaoVoo.builder()
                .codigo(51)
                .statusVoo(StatusVoo.EM_ANDAMENTO)
                .dataHoraPlanejada(dataFutura.minusMinutes(20))
                .dataHoraReal(dataFutura.minusMinutes(20))
                .duracaoEstimadaMin(60)
                .drone(Drone.builder().codigo("PP-209").build())
                .piloto(pilotoValido)
                .cliente(clienteAtivo)
                .empresaOperadora(empresa)
                .build();

        when(operacaoRepository.findActiveForConflictCheck(any(), eq("PR-401"), eq("32849172011"), eq(null)))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.agendar(buildRequest(dataFutura)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("piloto");
    }

    @Test
    @DisplayName("Deve agendar operação com sucesso quando todas as travas pré-voo forem atendidas")
    void deveAgendarComSucesso() {
        mockDependenciasPadrao(droneValido, pilotoValido, clienteAtivo);
        when(operacaoRepository.findActiveForConflictCheck(any(), eq("PR-401"), eq("32849172011"), eq(null)))
                .thenReturn(List.of());
        when(operacaoRepository.save(any(OperacaoVoo.class))).thenAnswer(inv -> {
            OperacaoVoo op = inv.getArgument(0);
            op.setCodigo(1001);
            return op;
        });

        OperacaoVooResponse res = service.agendar(buildRequest(dataFutura));

        assertThat(res.codigo()).isEqualTo(1001);
        assertThat(res.statusVoo()).isEqualTo(StatusVoo.AGENDADO);
        assertThat(res.droneCodigo()).isEqualTo("PR-401");
        assertThat(res.pilotoNome()).isEqualTo("Carlos Silva");
    }

    @Test
    @DisplayName("Deve impedir transição direta de AGENDADO para CONCLUIDO")
    void deveImpedirTransicaoInvalidaDeStatus() {
        OperacaoVoo agendada = OperacaoVoo.builder()
                .codigo(1001)
                .statusVoo(StatusVoo.AGENDADO)
                .dataHoraPlanejada(dataFutura)
                .duracaoEstimadaMin(60)
                .drone(droneValido)
                .piloto(pilotoValido)
                .cliente(clienteAtivo)
                .empresaOperadora(empresa)
                .build();

        when(operacaoRepository.findWithDetailsByCodigo(1001)).thenReturn(Optional.of(agendada));

        assertThatThrownBy(() -> service.alterarStatus(1001, new AtualizarStatusVooRequest(StatusVoo.CONCLUIDO)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("EM_ANDAMENTO ou CANCELADO");
    }
}
