package com.drone.api.service;

import com.drone.api.dto.request.AuditoriaRequest;
import com.drone.api.dto.response.AuditoriaResponse;
import com.drone.api.entity.Auditoria;
import com.drone.api.entity.NivelGravidade;
import com.drone.api.entity.OperacaoVoo;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.repository.AuditoriaRepository;
import com.drone.api.repository.OperacaoVooRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final OperacaoVooRepository operacaoVooRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listar(
            String busca,
            Integer operacaoVooCodigo,
            String empresaCnpj,
            String categoria,
            NivelGravidade nivelGravidade,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable
    ) {
        String empresaFiltro = tenantContext.resolverCnpjFiltro(empresaCnpj);
        String buscaTermo = normalizarTexto(busca);
        String categoriaFiltro = normalizarTexto(categoria);

        return auditoriaRepository.search(
                        empresaFiltro,
                        buscaTermo,
                        operacaoVooCodigo,
                        categoriaFiltro,
                        nivelGravidade,
                        inicio,
                        fim,
                        pageable
                )
                .map(AuditoriaResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditoriaResponse buscarPorCodigo(Integer codigo) {
        Auditoria auditoria = auditoriaRepository.findWithDetailsByCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Registro de auditoria não encontrado para o código informado."
                ));
        tenantContext.validarAcessoEmpresa(auditoria.getOperacaoVoo().getEmpresaOperadora().getCnpj());
        return AuditoriaResponse.from(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listarPorOperacao(Integer operacaoVooCodigo) {
        OperacaoVoo operacao = operacaoVooRepository.findWithDetailsByCodigo(operacaoVooCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Operação de voo não encontrada para o código informado."
                ));
        tenantContext.validarAcessoEmpresa(operacao.getEmpresaOperadora().getCnpj());
        return auditoriaRepository.findByOperacaoVooCodigoOrderByDataHoraEventoDesc(operacaoVooCodigo)
                .stream()
                .map(AuditoriaResponse::from)
                .toList();
    }

    @Transactional
    public AuditoriaResponse registrar(AuditoriaRequest request) {
        OperacaoVoo operacao = operacaoVooRepository.findWithDetailsByCodigo(request.operacaoVooCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Operação de voo não encontrada. Toda auditoria exige vínculo com um voo existente."
                ));
        tenantContext.validarAcessoEmpresa(operacao.getEmpresaOperadora().getCnpj());

        Auditoria novaAuditoria = Auditoria.builder()
                .operacaoVoo(operacao)
                .dataHoraEvento(request.dataHoraEvento())
                .categoria(request.categoria().trim())
                .nivelGravidade(request.nivelGravidade())
                .descricao(request.descricao().trim())
                .build();

        Auditoria salva = auditoriaRepository.save(novaAuditoria);
        return AuditoriaResponse.from(salva);
    }

    private String normalizarTexto(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
