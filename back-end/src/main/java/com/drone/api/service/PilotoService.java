package com.drone.api.service;

import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.response.PilotoResponse;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.Piloto;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.PilotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PilotoService {

    private final PilotoRepository pilotoRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<PilotoResponse> listar(
            String busca,
            String nome,
            String empresaCnpj,
            Boolean apenasCmaValido,
            Pageable pageable
    ) {
        String buscaTermo = normalizarTexto(busca);
        String digitos = buscaTermo == null ? "" : extrairDigitos(buscaTermo);
        String buscaDigitos = digitos.isEmpty() ? buscaTermo : digitos;
        String nomeFiltro = normalizarTexto(nome);
        String empresaFiltro = tenantContext.resolverCnpjFiltro(empresaCnpj);
        LocalDate dataMinimaCma = Boolean.TRUE.equals(apenasCmaValido) ? LocalDate.now() : null;

        return pilotoRepository.search(buscaTermo, buscaDigitos, nomeFiltro, empresaFiltro, dataMinimaCma, pageable)
                .map(PilotoResponse::from);
    }

    @Transactional(readOnly = true)
    public PilotoResponse buscarPorCpf(String cpfInformado) {
        Piloto piloto = localizarOuFalhar(cpfInformado);
        tenantContext.validarAcessoEmpresa(piloto.getEmpresaOperadora().getCnpj());
        return PilotoResponse.from(piloto);
    }

    @Transactional
    public PilotoResponse cadastrar(PilotoRequest request) {
        String cpfDigitos = validarCpf(request.cpf());
        String rgNormalizado = request.rg().trim().toUpperCase();
        String emailNormalizado = request.email().trim().toLowerCase();
        String licencaNormalizada = request.numRegistroLicenca().trim().toUpperCase();
        List<String> telefonesNormalizados = validarTelefones(request.telefones());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());

        if (pilotoRepository.existsByCpf(cpfDigitos)) {
            throw new ConflitoException("Já existe um piloto cadastrado com este CPF.");
        }
        if (pilotoRepository.existsByRgIgnoreCase(rgNormalizado)) {
            throw new ConflitoException("Já existe um piloto cadastrado com este RG.");
        }
        if (pilotoRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe um piloto cadastrado com este e-mail.");
        }
        if (pilotoRepository.existsByNumRegistroLicencaIgnoreCase(licencaNormalizada)) {
            throw new ConflitoException("Já existe um piloto cadastrado com este número de registro/licença.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        Piloto novoPiloto = Piloto.builder()
                .cpf(cpfDigitos)
                .rg(rgNormalizado)
                .nome(request.nome().trim())
                .email(emailNormalizado)
                .numRegistroLicenca(licencaNormalizada)
                .validadeCma(request.validadeCma())
                .empresaOperadora(empresa)
                .build();
        novoPiloto.substituirTelefones(telefonesNormalizados);

        Piloto salvo = pilotoRepository.save(novoPiloto);
        return PilotoResponse.from(salvo);
    }

    @Transactional
    public PilotoResponse atualizar(String cpfInformado, PilotoRequest request) {
        Piloto existente = localizarOuFalhar(cpfInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());
        String cpfChave = existente.getCpf();

        String rgNormalizado = request.rg().trim().toUpperCase();
        String emailNormalizado = request.email().trim().toLowerCase();
        String licencaNormalizada = request.numRegistroLicenca().trim().toUpperCase();
        List<String> telefonesNormalizados = validarTelefones(request.telefones());

        if (pilotoRepository.existsByRgIgnoreCaseAndCpfNot(rgNormalizado, cpfChave)) {
            throw new ConflitoException("O RG informado já pertence a outro piloto.");
        }
        if (pilotoRepository.existsByEmailIgnoreCaseAndCpfNot(emailNormalizado, cpfChave)) {
            throw new ConflitoException("O e-mail informado já pertence a outro piloto.");
        }
        if (pilotoRepository.existsByNumRegistroLicencaIgnoreCaseAndCpfNot(licencaNormalizada, cpfChave)) {
            throw new ConflitoException("O número de registro/licença informado já pertence a outro piloto.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        existente.setRg(rgNormalizado);
        existente.setNome(request.nome().trim());
        existente.setEmail(emailNormalizado);
        existente.setNumRegistroLicenca(licencaNormalizada);
        existente.setValidadeCma(request.validadeCma());
        existente.setEmpresaOperadora(empresa);
        existente.substituirTelefones(telefonesNormalizados);

        Piloto atualizado = pilotoRepository.save(existente);
        return PilotoResponse.from(atualizado);
    }

    @Transactional
    public void remover(String cpfInformado) {
        Piloto existente = localizarOuFalhar(cpfInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        pilotoRepository.delete(existente);
    }

    private Piloto localizarOuFalhar(String cpfInformado) {
        String cpfDigitos = extrairDigitos(cpfInformado);
        return pilotoRepository.findWithDetailsByCpf(cpfDigitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Piloto não encontrado para o CPF informado."
                ));
    }

    private EmpresaOperadora localizarEmpresaOuFalhar(String cnpjInformado) {
        String digitos = extrairDigitos(cnpjInformado);
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new RegraDeNegocioException("Informe um CPF ou CNPJ válido para a empresa operadora.");
        }
        return empresaRepository.findById(digitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Empresa operadora não encontrada para o documento informado."
                ));
    }

    private String validarCpf(String cpf) {
        String digitos = extrairDigitos(cpf);
        if (digitos.length() != 11) {
            throw new RegraDeNegocioException("Informe um CPF válido com 11 dígitos.");
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

    private String normalizarTexto(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extrairDigitos(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
