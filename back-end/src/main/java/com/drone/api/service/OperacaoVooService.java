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
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.DroneRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.OperacaoVooRepository;
import com.drone.api.repository.PilotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperacaoVooService {

    private static final List<StatusVoo> STATUS_ATIVOS_CONFLITO = List.of(
            StatusVoo.AGENDADO,
            StatusVoo.EM_ANDAMENTO
    );
    private static final int DURACAO_PADRAO_MIN = 60;

    private final OperacaoVooRepository operacaoRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final DroneRepository droneRepository;
    private final PilotoRepository pilotoRepository;
    private final ClienteRepository clienteRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<OperacaoVooResponse> listar(
            String busca,
            StatusVoo status,
            String empresaCnpj,
            String droneCodigo,
            String pilotoCpf,
            String clienteCpfRg,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable
    ) {
        String buscaTermo = normalizarTexto(busca);
        String empresaFiltro = tenantContext.resolverCnpjFiltro(empresaCnpj);
        String droneFiltro = normalizarTexto(droneCodigo);
        String pilotoFiltro = normalizarDigitosOuNulo(pilotoCpf);
        String clienteFiltro = normalizarDigitosOuNulo(clienteCpfRg);

        return operacaoRepository.search(
                        buscaTermo,
                        status,
                        empresaFiltro,
                        droneFiltro,
                        pilotoFiltro,
                        clienteFiltro,
                        inicio,
                        fim,
                        pageable
                )
                .map(OperacaoVooResponse::from);
    }

    @Transactional(readOnly = true)
    public OperacaoVooResponse buscarPorCodigo(Integer codigo) {
        OperacaoVoo operacao = localizarOuFalhar(codigo);
        tenantContext.validarAcessoEmpresa(operacao.getEmpresaOperadora().getCnpj());
        return OperacaoVooResponse.from(operacao);
    }

    @Transactional
    public OperacaoVooResponse agendar(OperacaoVooRequest request) {
        int duracao = request.duracaoEstimadaMin() != null ? request.duracaoEstimadaMin() : DURACAO_PADRAO_MIN;
        validarOrigemDestino(request.origem(), request.destino());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());

        EmpresaOperadora empresa = localizarEmpresaAtiva(cnpjAutorizado);
        Drone drone = localizarDrone(request.droneCodigo());
        Piloto piloto = localizarPiloto(request.pilotoCpf());
        Cliente cliente = localizarCliente(request.clienteCpfRg());

        validarVinculosOperacionais(empresa, drone, piloto, cliente);
        validarTravasPreVoo(drone, piloto, cliente, request.dataHoraPlanejada().toLocalDate());
        validarConflitoHorario(
                drone.getCodigo(),
                piloto.getCpf(),
                request.dataHoraPlanejada(),
                request.dataHoraPlanejada().plusMinutes(duracao),
                null
        );

        OperacaoVoo novaOperacao = OperacaoVoo.builder()
                .statusVoo(StatusVoo.AGENDADO)
                .objetivoVoo(request.objetivoVoo().trim())
                .dataHoraPlanejada(request.dataHoraPlanejada())
                .duracaoEstimadaMin(duracao)
                .origem(request.origem().trim())
                .destino(request.destino().trim())
                .descricaoArea(request.descricaoArea().trim())
                .tipoLocal(request.tipoLocal().trim())
                .drone(drone)
                .piloto(piloto)
                .cliente(cliente)
                .empresaOperadora(empresa)
                .build();

        OperacaoVoo salva = operacaoRepository.save(novaOperacao);
        return OperacaoVooResponse.from(salva);
    }

    @Transactional
    public OperacaoVooResponse atualizar(Integer codigo, OperacaoVooRequest request) {
        OperacaoVoo existente = localizarOuFalhar(codigo);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        if (existente.getStatusVoo() != StatusVoo.AGENDADO) {
            throw new RegraDeNegocioException(
                    "Somente operações com status AGENDADO podem ter os dados planejados alterados."
            );
        }

        int duracao = request.duracaoEstimadaMin() != null ? request.duracaoEstimadaMin() : DURACAO_PADRAO_MIN;
        validarOrigemDestino(request.origem(), request.destino());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());

        EmpresaOperadora empresa = localizarEmpresaAtiva(cnpjAutorizado);
        Drone drone = localizarDrone(request.droneCodigo());
        Piloto piloto = localizarPiloto(request.pilotoCpf());
        Cliente cliente = localizarCliente(request.clienteCpfRg());

        validarVinculosOperacionais(empresa, drone, piloto, cliente);
        validarTravasPreVoo(drone, piloto, cliente, request.dataHoraPlanejada().toLocalDate());
        validarConflitoHorario(
                drone.getCodigo(),
                piloto.getCpf(),
                request.dataHoraPlanejada(),
                request.dataHoraPlanejada().plusMinutes(duracao),
                existente.getCodigo()
        );

        existente.setObjetivoVoo(request.objetivoVoo().trim());
        existente.setDataHoraPlanejada(request.dataHoraPlanejada());
        existente.setDuracaoEstimadaMin(duracao);
        existente.setOrigem(request.origem().trim());
        existente.setDestino(request.destino().trim());
        existente.setDescricaoArea(request.descricaoArea().trim());
        existente.setTipoLocal(request.tipoLocal().trim());
        existente.setDrone(drone);
        existente.setPiloto(piloto);
        existente.setCliente(cliente);
        existente.setEmpresaOperadora(empresa);

        OperacaoVoo atualizada = operacaoRepository.save(existente);
        return OperacaoVooResponse.from(atualizada);
    }

    @Transactional
    public OperacaoVooResponse alterarStatus(Integer codigo, AtualizarStatusVooRequest request) {
        OperacaoVoo operacao = localizarOuFalhar(codigo);
        tenantContext.validarAcessoEmpresa(operacao.getEmpresaOperadora().getCnpj());
        StatusVoo atual = operacao.getStatusVoo();
        StatusVoo proximo = request.statusVoo();

        if (atual == proximo) {
            return OperacaoVooResponse.from(operacao);
        }

        switch (atual) {
            case AGENDADO -> {
                if (proximo == StatusVoo.EM_ANDAMENTO) {
                    LocalDateTime agora = LocalDateTime.now();
                    int duracao = operacao.getDuracaoEstimadaMin() != null
                            ? operacao.getDuracaoEstimadaMin()
                            : DURACAO_PADRAO_MIN;

                    if (operacao.getEmpresaOperadora().getStatusContrato() != StatusContrato.Ativo) {
                        throw new RegraDeNegocioException(
                                "A empresa operadora não possui contrato ativo para iniciar a operação."
                        );
                    }
                    validarTravasPreVoo(
                            operacao.getDrone(),
                            operacao.getPiloto(),
                            operacao.getCliente(),
                            agora.toLocalDate()
                    );
                    validarConflitoHorario(
                            operacao.getDrone().getCodigo(),
                            operacao.getPiloto().getCpf(),
                            agora,
                            agora.plusMinutes(duracao),
                            operacao.getCodigo()
                    );
                    operacao.setStatusVoo(StatusVoo.EM_ANDAMENTO);
                    operacao.setDataHoraReal(agora);
                } else if (proximo == StatusVoo.CANCELADO) {
                    operacao.setStatusVoo(StatusVoo.CANCELADO);
                } else {
                    throw new RegraDeNegocioException(
                            "Uma operação AGENDADO só pode avançar para EM_ANDAMENTO ou CANCELADO."
                    );
                }
            }
            case EM_ANDAMENTO -> {
                if (proximo == StatusVoo.CONCLUIDO) {
                    operacao.setStatusVoo(StatusVoo.CONCLUIDO);
                    operacao.setDataHoraPousoReal(LocalDateTime.now());
                } else if (proximo == StatusVoo.CANCELADO) {
                    operacao.setStatusVoo(StatusVoo.CANCELADO);
                    operacao.setDataHoraPousoReal(LocalDateTime.now());
                } else {
                    throw new RegraDeNegocioException(
                            "Uma operação EM_ANDAMENTO só pode avançar para CONCLUIDO ou CANCELADO."
                    );
                }
            }
            case CONCLUIDO, CANCELADO -> throw new RegraDeNegocioException(
                    "Operações encerradas (" + atual + ") não podem ter o status alterado."
            );
        }

        OperacaoVoo salva = operacaoRepository.save(operacao);
        return OperacaoVooResponse.from(salva);
    }

    private OperacaoVoo localizarOuFalhar(Integer codigo) {
        return operacaoRepository.findWithDetailsByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Operação de voo não encontrada para o código informado."
                ));
    }

    private EmpresaOperadora localizarEmpresaAtiva(String cnpjInformado) {
        String cnpjDigitos = extrairDigitos(cnpjInformado);
        EmpresaOperadora empresa = empresaRepository.findById(cnpjDigitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Empresa operadora não encontrada para o documento informado."
                ));
        if (empresa.getStatusContrato() != StatusContrato.Ativo) {
            throw new RegraDeNegocioException(
                    "A empresa operadora está com contrato " + empresa.getStatusContrato()
                            + " e não pode agendar operações de voo."
            );
        }
        return empresa;
    }

    private Drone localizarDrone(String codigoInformado) {
        String codigo = codigoInformado == null ? "" : codigoInformado.trim().toUpperCase();
        return droneRepository.findWithEmpresaByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Drone não encontrado para o código informado."
                ));
    }

    private Piloto localizarPiloto(String cpfInformado) {
        String cpf = extrairDigitos(cpfInformado);
        return pilotoRepository.findWithDetailsByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Piloto não encontrado para o CPF informado."
                ));
    }

    private Cliente localizarCliente(String cpfRgInformado) {
        String doc = extrairDigitos(cpfRgInformado);
        return clienteRepository.findWithDetailsByCpfRg(doc)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado para o documento informado."
                ));
    }

    private void validarOrigemDestino(String origem, String destino) {
        if (origem.trim().equalsIgnoreCase(destino.trim())) {
            throw new RegraDeNegocioException(
                    "Origem e destino devem ser dois locais de operação distintos."
            );
        }
    }

    private void validarVinculosOperacionais(
            EmpresaOperadora empresa,
            Drone drone,
            Piloto piloto,
            Cliente cliente
    ) {
        String cnpj = empresa.getCnpj();
        if (!cnpj.equals(drone.getEmpresaOperadora().getCnpj())) {
            throw new RegraDeNegocioException(
                    "O drone informado não pertence à empresa operadora selecionada."
            );
        }
        if (!cnpj.equals(piloto.getEmpresaOperadora().getCnpj())) {
            throw new RegraDeNegocioException(
                    "O piloto informado não está vinculado à empresa operadora selecionada."
            );
        }
        if (!cnpj.equals(cliente.getEmpresaOperadora().getCnpj())) {
            throw new RegraDeNegocioException(
                    "O cliente informado não possui contrato vinculado à empresa operadora selecionada."
            );
        }
    }

    private void validarTravasPreVoo(Drone drone, Piloto piloto, Cliente cliente, LocalDate dataVoo) {
        if (!drone.isCadastroAnacValidoEm(dataVoo)) {
            throw new RegraDeNegocioException(
                    "O drone informado possui cadastro na ANAC expirado para a data da operação."
            );
        }
        if (!piloto.isCmaValidoEm(dataVoo)) {
            throw new RegraDeNegocioException(
                    "O piloto informado possui Certificado Médico Aeronáutico (CMA) vencido para a data da operação."
            );
        }
        if (!cliente.isContratoAtivo()) {
            throw new RegraDeNegocioException(
                    "O cliente informado está com contrato inativo e não pode solicitar operações de voo."
            );
        }
    }

    private void validarConflitoHorario(
            String droneCodigo,
            String pilotoCpf,
            LocalDateTime novoInicio,
            LocalDateTime novoFim,
            Integer excludeCodigo
    ) {
        List<OperacaoVoo> ativas = operacaoRepository.findActiveForConflictCheck(
                STATUS_ATIVOS_CONFLITO,
                droneCodigo,
                pilotoCpf,
                excludeCodigo
        );

        for (OperacaoVoo existente : ativas) {
            LocalDateTime existenteInicio = existente.calcularInicioEfetivo();
            LocalDateTime existenteFim = existente.calcularFimEfetivo();

            boolean sobrepoe = novoInicio.isBefore(existenteFim) && novoFim.isAfter(existenteInicio);
            if (!sobrepoe) {
                continue;
            }

            if (existente.getDrone().getCodigo().equalsIgnoreCase(droneCodigo)) {
                throw new ConflitoException(
                        "Conflito de agenda: o drone " + droneCodigo
                                + " já está alocado na operação #" + existente.getCodigo() + " neste horário."
                );
            }
            if (existente.getPiloto().getCpf().equals(pilotoCpf)) {
                throw new ConflitoException(
                        "Conflito de agenda: o piloto informado já está escalado na operação #"
                                + existente.getCodigo() + " neste horário."
                );
            }
        }
    }

    private String normalizarTexto(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizarDigitosOuNulo(String raw) {
        String digitos = extrairDigitos(raw);
        return digitos.isEmpty() ? null : digitos;
    }

    private String extrairDigitos(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
