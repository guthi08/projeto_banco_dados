package com.drone.api.service;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.EmpresaOperadoraRequest;
import com.drone.api.dto.response.EmpresaOperadoraResponse;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.StatusContrato;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.EmpresaOperadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaOperadoraService {

    private final EmpresaOperadoraRepository empresaRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<EmpresaOperadoraResponse> listar(
            String busca,
            String setor,
            StatusContrato status,
            Pageable pageable
    ) {
        String cnpjRestrito = tenantContext.resolverCnpjFiltro(null);
        String termo = busca == null ? null : busca.trim();
        String digitos = termo == null ? "" : extrairDigitos(termo);
        String buscaDigitos = digitos.isEmpty() ? termo : digitos;
        String setorFiltro = setor == null ? null : setor.trim();

        return empresaRepository.search(cnpjRestrito, termo, buscaDigitos, setorFiltro, status, pageable)
                .map(EmpresaOperadoraResponse::from);
    }

    @Transactional(readOnly = true)
    public EmpresaOperadoraResponse buscarPorCnpj(String cnpjInformado) {
        tenantContext.validarAcessoEmpresa(cnpjInformado);
        EmpresaOperadora empresa = localizarOuFalhar(cnpjInformado);
        return EmpresaOperadoraResponse.from(empresa);
    }

    @Transactional
    public EmpresaOperadoraResponse cadastrar(EmpresaOperadoraRequest request) {
        tenantContext.exigirAdmin("cadastrar novas empresas operadoras");
        String cnpjDigitos = validarDocumento(request.cnpj());
        String emailNormalizado = request.email().trim().toLowerCase();
        String razaoNormalizada = request.razaoSocial().trim();
        List<String> telefonesNormalizados = validarTelefones(request.telefones());

        if (empresaRepository.existsByCnpj(cnpjDigitos)) {
            throw new ConflitoException("Já existe uma empresa operadora cadastrada com este CPF/CNPJ.");
        }
        if (empresaRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe uma empresa operadora cadastrada com este e-mail.");
        }
        if (empresaRepository.existsByRazaoSocialIgnoreCase(razaoNormalizada)) {
            throw new ConflitoException("Já existe uma empresa operadora cadastrada com esta razão social.");
        }

        EmpresaOperadora novaEmpresa = EmpresaOperadora.builder()
                .cnpj(cnpjDigitos)
                .razaoSocial(razaoNormalizada)
                .setorAtuacao(request.setorAtuacao().trim())
                .email(emailNormalizado)
                .statusContrato(request.statusContrato())
                .build();
        novaEmpresa.substituirTelefones(telefonesNormalizados);

        EmpresaOperadora salva = empresaRepository.save(novaEmpresa);
        return EmpresaOperadoraResponse.from(salva);
    }

    @Transactional
    public EmpresaOperadoraResponse atualizar(String cnpjInformado, EmpresaOperadoraRequest request) {
        tenantContext.validarAcessoEmpresa(cnpjInformado);
        EmpresaOperadora existente = localizarOuFalhar(cnpjInformado);
        String cnpjChave = existente.getCnpj();

        String emailNormalizado = request.email().trim().toLowerCase();
        String razaoNormalizada = request.razaoSocial().trim();
        List<String> telefonesNormalizados = validarTelefones(request.telefones());

        if (empresaRepository.existsByEmailIgnoreCaseAndCnpjNot(emailNormalizado, cnpjChave)) {
            throw new ConflitoException("O e-mail informado já pertence a outra empresa operadora.");
        }
        if (empresaRepository.existsByRazaoSocialIgnoreCaseAndCnpjNot(razaoNormalizada, cnpjChave)) {
            throw new ConflitoException("A razão social informada já pertence a outra empresa operadora.");
        }

        existente.setRazaoSocial(razaoNormalizada);
        existente.setSetorAtuacao(request.setorAtuacao().trim());
        existente.setEmail(emailNormalizado);
        if (tenantContext.isAdmin()) {
            existente.setStatusContrato(request.statusContrato());
        }
        existente.substituirTelefones(telefonesNormalizados);

        EmpresaOperadora atualizada = empresaRepository.save(existente);
        return EmpresaOperadoraResponse.from(atualizada);
    }

    @Transactional
    public EmpresaOperadoraResponse alterarStatus(
            String cnpjInformado,
            AtualizarStatusContratoRequest request
    ) {
        tenantContext.exigirAdmin("alterar o status contratual de empresas operadoras");
        EmpresaOperadora existente = localizarOuFalhar(cnpjInformado);
        existente.setStatusContrato(request.statusContrato());
        EmpresaOperadora atualizada = empresaRepository.save(existente);
        return EmpresaOperadoraResponse.from(atualizada);
    }

    private EmpresaOperadora localizarOuFalhar(String cnpjInformado) {
        String cnpjDigitos = extrairDigitos(cnpjInformado);
        return empresaRepository.findWithTelefonesByCnpj(cnpjDigitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Empresa operadora não encontrada para o documento informado."
                ));
    }

    private String validarDocumento(String documento) {
        String digitos = extrairDigitos(documento);
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new RegraDeNegocioException("Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.");
        }
        return digitos;
    }

    private List<String> validarTelefones(List<String> telefones) {
        List<String> limpos = telefones.stream()
                .map(this::extrairDigitos)
                .distinct()
                .toList();

        if (limpos.isEmpty()) {
            throw new RegraDeNegocioException("Informe ao menos um telefone de contato.");
        }
        for (String tel : limpos) {
            if (tel.length() != 10 && tel.length() != 11) {
                throw new RegraDeNegocioException("Todos os telefones devem possuir DDD e 10 ou 11 dígitos.");
            }
        }
        return limpos;
    }

    private String extrairDigitos(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
