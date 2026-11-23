package com.drone.api.service;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.response.ClienteResponse;
import com.drone.api.entity.Cliente;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.StatusContrato;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.ClienteRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(
            String busca,
            String nome,
            String empresaCnpj,
            StatusContrato status,
            Pageable pageable
    ) {
        String buscaTermo = normalizarTexto(busca);
        String digitos = buscaTermo == null ? "" : extrairDigitos(buscaTermo);
        String buscaDigitos = digitos.isEmpty() ? buscaTermo : digitos;
        String nomeFiltro = normalizarTexto(nome);
        String empresaFiltro = tenantContext.resolverCnpjFiltro(empresaCnpj);

        return clienteRepository.search(buscaTermo, buscaDigitos, nomeFiltro, empresaFiltro, status, pageable)
                .map(ClienteResponse::from);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorCpfRg(String cpfRgInformado) {
        Cliente cliente = localizarOuFalhar(cpfRgInformado);
        tenantContext.validarAcessoEmpresa(cliente.getEmpresaOperadora().getCnpj());
        return ClienteResponse.from(cliente);
    }

    @Transactional
    public ClienteResponse cadastrar(ClienteRequest request) {
        String cpfRgDigitos = validarDocumento(request.cpfRg(), "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido para o cliente.");
        String rgNormalizado = request.rg().trim().toUpperCase();
        StatusContrato statusValidado = validarStatusCliente(request.statusContrato());
        List<String> telefonesNormalizados = validarTelefones(request.telefones());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());

        if (clienteRepository.existsByCpfRg(cpfRgDigitos)) {
            throw new ConflitoException("Já existe um cliente cadastrado com este CPF/CNPJ.");
        }
        if (clienteRepository.existsByRgIgnoreCase(rgNormalizado)) {
            throw new ConflitoException("Já existe um cliente cadastrado com este RG.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        Cliente novoCliente = Cliente.builder()
                .cpfRg(cpfRgDigitos)
                .rg(rgNormalizado)
                .nome(request.nome().trim())
                .empresaOperadora(empresa)
                .statusContrato(statusValidado)
                .dataFormalizacao(request.dataFormalizacao())
                .build();
        novoCliente.substituirTelefones(telefonesNormalizados);

        Cliente salvo = clienteRepository.save(novoCliente);
        return ClienteResponse.from(salvo);
    }

    @Transactional
    public ClienteResponse atualizar(String cpfRgInformado, ClienteRequest request) {
        Cliente existente = localizarOuFalhar(cpfRgInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());
        String chave = existente.getCpfRg();

        String rgNormalizado = request.rg().trim().toUpperCase();
        StatusContrato statusValidado = validarStatusCliente(request.statusContrato());
        List<String> telefonesNormalizados = validarTelefones(request.telefones());

        if (clienteRepository.existsByRgIgnoreCaseAndCpfRgNot(rgNormalizado, chave)) {
            throw new ConflitoException("O RG informado já pertence a outro cliente.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        existente.setRg(rgNormalizado);
        existente.setNome(request.nome().trim());
        existente.setEmpresaOperadora(empresa);
        existente.setStatusContrato(statusValidado);
        existente.setDataFormalizacao(request.dataFormalizacao());
        existente.substituirTelefones(telefonesNormalizados);

        Cliente atualizado = clienteRepository.save(existente);
        return ClienteResponse.from(atualizado);
    }

    @Transactional
    public ClienteResponse alterarStatus(String cpfRgInformado, AtualizarStatusContratoRequest request) {
        Cliente existente = localizarOuFalhar(cpfRgInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        StatusContrato statusValidado = validarStatusCliente(request.statusContrato());
        existente.setStatusContrato(statusValidado);
        Cliente atualizado = clienteRepository.save(existente);
        return ClienteResponse.from(atualizado);
    }

    @Transactional
    public void remover(String cpfRgInformado) {
        Cliente existente = localizarOuFalhar(cpfRgInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        clienteRepository.delete(existente);
    }

    private Cliente localizarOuFalhar(String cpfRgInformado) {
        String digitos = extrairDigitos(cpfRgInformado);
        return clienteRepository.findWithDetailsByCpfRg(digitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado para o documento informado."
                ));
    }

    private EmpresaOperadora localizarEmpresaOuFalhar(String cnpjInformado) {
        String digitos = validarDocumento(cnpjInformado, "Informe um CPF ou CNPJ válido para a empresa operadora.");
        return empresaRepository.findById(digitos)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Empresa operadora não encontrada para o documento informado."
                ));
    }

    private StatusContrato validarStatusCliente(StatusContrato status) {
        if (status != StatusContrato.Ativo && status != StatusContrato.Inativo) {
            throw new RegraDeNegocioException("O contrato de cliente aceita apenas os status Ativo ou Inativo.");
        }
        return status;
    }

    private String validarDocumento(String documento, String mensagemErro) {
        String digitos = extrairDigitos(documento);
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new RegraDeNegocioException(mensagemErro);
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
