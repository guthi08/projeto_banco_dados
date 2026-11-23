package com.drone.api.config;

import com.drone.api.entity.Auditoria;
import com.drone.api.entity.Cliente;
import com.drone.api.entity.Drone;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.NivelGravidade;
import com.drone.api.entity.OperacaoVoo;
import com.drone.api.entity.Piloto;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.StatusVoo;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.entity.Usuario;
import com.drone.api.repository.AuditoriaRepository;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.DroneRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.OperacaoVooRepository;
import com.drone.api.repository.PilotoRepository;
import com.drone.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class AuthDataInitializer implements ApplicationRunner {

    private final EmpresaOperadoraRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DroneRepository droneRepository;
    private final PilotoRepository pilotoRepository;
    private final ClienteRepository clienteRepository;
    private final OperacaoVooRepository operacaoVooRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);

        // 1. Empresas Operadoras
        EmpresaOperadora aerolog = criarEmpresa(
                "27419830000112",
                "AeroLog Brasil Ltda",
                "Logística",
                "operacoes@aerologbrasil.com.br",
                StatusContrato.Ativo,
                List.of("1138429100", "11998881001")
        );

        EmpresaOperadora vetorgeo = criarEmpresa(
                "41092514000188",
                "VetorGeo Engenharia Aérea",
                "Geodésia",
                "contato@vetorgeo.com.br",
                StatusContrato.Ativo,
                List.of("4133194420")
        );

        EmpresaOperadora horizonte = criarEmpresa(
                "18774205000145",
                "Horizonte AgroDrones S.A.",
                "Agricultura",
                "frota@horizonteagro.com.br",
                StatusContrato.Ativo,
                List.of("6232017780", "62984125590")
        );

        // 2. Usuário Administrador Inicial
        Usuario admin = Usuario.builder()
                .nome("Guthierrys S.")
                .email("operacoes@aerologbrasil.com.br")
                .senhaHash(passwordEncoder.encode("Aero@2026"))
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .empresa(aerolog)
                .build();
        usuarioRepository.save(admin);

        // 3. Drones da Frota (válidos, vencendo em < 30 dias e 1 vencido para demonstrar Trava ANAC)
        Drone pr401 = droneRepository.save(Drone.builder()
                .codigo("PR-401")
                .fabricante("DJI")
                .modelo("Matrice 300 RTK")
                .numSerie("SN-M300-88421")
                .pesoMaxDecolagem(new BigDecimal("9.00"))
                .tipoUso(TipoUsoDrone.avancado)
                .dataValidadeCadastro(hoje.plusMonths(9))
                .empresaOperadora(aerolog)
                .build());

        Drone pr305 = droneRepository.save(Drone.builder()
                .codigo("PR-305")
                .fabricante("DJI")
                .modelo("Inspire 3")
                .numSerie("SN-IN3-55209")
                .pesoMaxDecolagem(new BigDecimal("4.30"))
                .tipoUso(TipoUsoDrone.avancado)
                .dataValidadeCadastro(hoje.plusDays(12))
                .empresaOperadora(aerolog)
                .build());

        Drone pp209 = droneRepository.save(Drone.builder()
                .codigo("PP-209")
                .fabricante("DJI")
                .modelo("Mavic 3 Enterprise")
                .numSerie("SN-M3E-44210")
                .pesoMaxDecolagem(new BigDecimal("0.92"))
                .tipoUso(TipoUsoDrone.basico)
                .dataValidadeCadastro(hoje.plusMonths(6))
                .empresaOperadora(vetorgeo)
                .build());

        droneRepository.save(Drone.builder()
                .codigo("PS-882")
                .fabricante("DJI")
                .modelo("Agras T30")
                .numSerie("SN-T30-99104")
                .pesoMaxDecolagem(new BigDecimal("78.00"))
                .tipoUso(TipoUsoDrone.avancado)
                .dataValidadeCadastro(hoje.minusDays(6))
                .empresaOperadora(horizonte)
                .build());

        // 4. Pilotos (válidos, vencendo e 1 com CMA vencido para demonstrar Trava CMA)
        Piloto carlos = criarPiloto(
                "32849172011", "SP12345678", "Carlos Silva",
                "carlos.silva@aerologbrasil.com.br", "ANAC-RPAS-88412",
                hoje.plusMonths(8), aerolog, List.of("11991204410", "11977771111")
        );

        criarPiloto(
                "28911460255", "SP45678912", "Roberto Cruz",
                "roberto.cruz@aerologbrasil.com.br", "ANAC-RPAS-54908",
                hoje.minusDays(15), aerolog, List.of("11993108871")
        );

        Piloto ana = criarPiloto(
                "41980523304", "PR23456789", "Ana Souza",
                "ana.souza@vetorgeo.com.br", "ANAC-RPAS-77319",
                hoje.plusMonths(5), vetorgeo, List.of("41998441102")
        );

        Piloto juliana = criarPiloto(
                "60471290833", "PR56789123", "Juliana Mendes",
                "juliana.mendes@vetorgeo.com.br", "ANAC-RPAS-91207",
                hoje.plusDays(18), vetorgeo, List.of("41997023318")
        );

        // 5. Clientes
        Cliente fazenda = criarCliente(
                "12345678000190", "MG12345678", "Fazenda Boa Vista",
                aerolog, StatusContrato.Ativo, hoje.minusMonths(2), List.of("6233412200", "62991002200")
        );

        Cliente engel = criarCliente(
                "33812904000117", "RJ45678901", "Construtora Engel",
                aerolog, StatusContrato.Ativo, hoje.minusMonths(1), List.of("1130458900")
        );

        Cliente itaipu = criarCliente(
                "00395988000135", "PR34567890", "Usina Hidrelétrica Itaipu",
                vetorgeo, StatusContrato.Ativo, hoje.minusMonths(3), List.of("4535205252")
        );

        criarCliente(
                "67890123000189", "RS67890123", "Produtora Filme Brasil",
                horizonte, StatusContrato.Inativo, hoje.minusMonths(6), List.of("5132147700")
        );

        // 6. Operações de Voo
        OperacaoVoo opAndamento = operacaoVooRepository.save(OperacaoVoo.builder()
                .statusVoo(StatusVoo.EM_ANDAMENTO)
                .objetivoVoo("Monitoramento termográfico de pivôs centrais")
                .dataHoraPlanejada(agora.minusMinutes(30))
                .dataHoraReal(agora.minusMinutes(25))
                .duracaoEstimadaMin(90)
                .origem("Hangar Norte — Ribeirão Preto")
                .destino("Setor Sul — Fazenda Boa Vista")
                .descricaoArea("Pivôs centrais de irrigação — Setor Sul")
                .tipoLocal("Rural")
                .drone(pr401)
                .piloto(carlos)
                .cliente(fazenda)
                .empresaOperadora(aerolog)
                .build());

        OperacaoVoo opAgendada = operacaoVooRepository.save(OperacaoVoo.builder()
                .statusVoo(StatusVoo.AGENDADO)
                .objetivoVoo("Inspeção visual do vertedouro e comportas")
                .dataHoraPlanejada(agora.plusDays(1).withHour(14).withMinute(0))
                .duracaoEstimadaMin(60)
                .origem("Base Foz do Iguaçu")
                .destino("Vertedouro Central — Itaipu")
                .descricaoArea("Corredor técnico do vertedouro principal")
                .tipoLocal("Infraestrutura Crítica")
                .drone(pp209)
                .piloto(ana)
                .cliente(itaipu)
                .empresaOperadora(vetorgeo)
                .build());

        OperacaoVoo opConcluida = operacaoVooRepository.save(OperacaoVoo.builder()
                .statusVoo(StatusVoo.CONCLUIDO)
                .objetivoVoo("Levantamento aerofotogramétrico de canteiro")
                .dataHoraPlanejada(agora.minusDays(4).withHour(9).withMinute(0))
                .dataHoraReal(agora.minusDays(4).withHour(9).withMinute(5))
                .dataHoraPousoReal(agora.minusDays(4).withHour(10).withMinute(15))
                .duracaoEstimadaMin(75)
                .origem("Polo Logístico Anhanguera")
                .destino("Complexo Viário Engel — Lote 4")
                .descricaoArea("Perímetro de terraplanagem e viaduto sul")
                .tipoLocal("Urbano")
                .drone(pr305)
                .piloto(carlos)
                .cliente(engel)
                .empresaOperadora(aerolog)
                .build());

        OperacaoVoo opCancelada = operacaoVooRepository.save(OperacaoVoo.builder()
                .statusVoo(StatusVoo.CANCELADO)
                .objetivoVoo("Varredura preventiva de faixa elétrica 500kV")
                .dataHoraPlanejada(agora.minusDays(2).withHour(15).withMinute(0))
                .duracaoEstimadaMin(60)
                .origem("Subestação Oeste")
                .destino("Torre 114 — Setor Leste")
                .descricaoArea("Faixa de servidão elétrica 500kV")
                .tipoLocal("Industrial")
                .drone(pp209)
                .piloto(juliana)
                .cliente(itaipu)
                .empresaOperadora(vetorgeo)
                .build());

        // 7. Auditorias Técnicas
        auditoriaRepository.save(Auditoria.builder()
                .operacaoVoo(opAndamento)
                .dataHoraEvento(agora.minusMinutes(10))
                .categoria("Desvio de Rota")
                .nivelGravidade(NivelGravidade.MEDIO)
                .descricao("Aeronave ajustou trajetória lateral em 45m devido a rajada de vento acima de 32 km/h no quadrante sul.")
                .build());

        auditoriaRepository.save(Auditoria.builder()
                .operacaoVoo(opCancelada)
                .dataHoraEvento(agora.minusDays(2).withHour(14).withMinute(40))
                .categoria("Alerta Meteorológico")
                .nivelGravidade(NivelGravidade.ALTO)
                .descricao("Operação cancelada antes da decolagem devido à formação de tempestade convectiva e baixa visibilidade.")
                .build());

        auditoriaRepository.save(Auditoria.builder()
                .operacaoVoo(opConcluida)
                .dataHoraEvento(agora.minusDays(4).withHour(10).withMinute(25))
                .categoria("Conformidade Pré-Voo")
                .nivelGravidade(NivelGravidade.BAIXO)
                .descricao("Inspeção documental ANAC/CMA e checklist de zona de pouso concluídos sem ressalvas.")
                .build());

        auditoriaRepository.save(Auditoria.builder()
                .operacaoVoo(opAgendada)
                .dataHoraEvento(agora.minusHours(1))
                .categoria("Restrição de Espaço Aéreo")
                .nivelGravidade(NivelGravidade.CRITICO)
                .descricao("NOTAM temporário emitido para setor adjacente ao vertedouro; coordenação prévia com controle local registrada.")
                .build());

        log.info("Dados iniciais do AERO-MONITOR carregados no banco. Login padrão: operacoes@aerologbrasil.com.br / Aero@2026");
    }

    private EmpresaOperadora criarEmpresa(
            String cnpj,
            String razaoSocial,
            String setor,
            String email,
            StatusContrato status,
            List<String> telefones
    ) {
        return empresaRepository.findById(cnpj).orElseGet(() -> {
            EmpresaOperadora emp = EmpresaOperadora.builder()
                    .cnpj(cnpj)
                    .razaoSocial(razaoSocial)
                    .setorAtuacao(setor)
                    .email(email)
                    .statusContrato(status)
                    .build();
            emp.substituirTelefones(telefones);
            return empresaRepository.save(emp);
        });
    }

    private Piloto criarPiloto(
            String cpf,
            String rg,
            String nome,
            String email,
            String licenca,
            LocalDate validadeCma,
            EmpresaOperadora empresa,
            List<String> telefones
    ) {
        Piloto piloto = Piloto.builder()
                .cpf(cpf)
                .rg(rg)
                .nome(nome)
                .email(email)
                .numRegistroLicenca(licenca)
                .validadeCma(validadeCma)
                .empresaOperadora(empresa)
                .build();
        piloto.substituirTelefones(telefones);
        return pilotoRepository.save(piloto);
    }

    private Cliente criarCliente(
            String cpfRg,
            String rg,
            String nome,
            EmpresaOperadora empresa,
            StatusContrato status,
            LocalDate dataFormalizacao,
            List<String> telefones
    ) {
        Cliente cliente = Cliente.builder()
                .cpfRg(cpfRg)
                .rg(rg)
                .nome(nome)
                .empresaOperadora(empresa)
                .statusContrato(status)
                .dataFormalizacao(dataFormalizacao)
                .build();
        cliente.substituirTelefones(telefones);
        return clienteRepository.save(cliente);
    }
}
