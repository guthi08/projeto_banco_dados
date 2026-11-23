package com.drone.api.service;

import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.response.DroneResponse;
import com.drone.api.entity.Drone;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.DroneRepository;
import com.drone.api.repository.EmpresaOperadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DroneService {

    private static final Pattern PADRAO_ANAC = Pattern.compile("^(PR|PP|PS)-[A-Z0-9]{3,6}$");

    private final DroneRepository droneRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final TenantContextService tenantContext;

    @Transactional(readOnly = true)
    public Page<DroneResponse> listar(
            String busca,
            String modelo,
            TipoUsoDrone tipoUso,
            String empresaCnpj,
            Boolean apenasValidos,
            Pageable pageable
    ) {
        String buscaTermo = normalizarTexto(busca);
        String modeloFiltro = normalizarTexto(modelo);
        String empresaFiltro = tenantContext.resolverCnpjFiltro(empresaCnpj);
        LocalDate dataMinima = Boolean.TRUE.equals(apenasValidos) ? LocalDate.now() : null;

        return droneRepository.search(buscaTermo, modeloFiltro, tipoUso, empresaFiltro, dataMinima, pageable)
                .map(DroneResponse::from);
    }

    @Transactional(readOnly = true)
    public DroneResponse buscarPorCodigo(String codigoInformado) {
        Drone drone = localizarOuFalhar(codigoInformado);
        tenantContext.validarAcessoEmpresa(drone.getEmpresaOperadora().getCnpj());
        return DroneResponse.from(drone);
    }

    @Transactional
    public DroneResponse cadastrar(DroneRequest request) {
        String codigoNormalizado = normalizarEValidarCodigo(request.codigo());
        String numSerieNormalizado = request.numSerie().trim();
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());

        if (droneRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new ConflitoException("Já existe um drone cadastrado com o prefixo ANAC " + codigoNormalizado + ".");
        }
        if (droneRepository.existsByNumSerieIgnoreCase(numSerieNormalizado)) {
            throw new ConflitoException("Já existe uma aeronave cadastrada com este número de série.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        Drone novoDrone = Drone.builder()
                .codigo(codigoNormalizado)
                .fabricante(request.fabricante().trim())
                .modelo(request.modelo().trim())
                .numSerie(numSerieNormalizado)
                .pesoMaxDecolagem(request.pesoMaxDecolagem())
                .tipoUso(request.tipoUso())
                .dataValidadeCadastro(request.dataValidadeCadastro())
                .empresaOperadora(empresa)
                .build();

        Drone salvo = droneRepository.save(novoDrone);
        return DroneResponse.from(salvo);
    }

    @Transactional
    public DroneResponse atualizar(String codigoInformado, DroneRequest request) {
        Drone existente = localizarOuFalhar(codigoInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        String cnpjAutorizado = tenantContext.resolverCnpjParaEscrita(request.empresaOpeCnpj());
        String numSerieNormalizado = request.numSerie().trim();

        if (droneRepository.existsByNumSerieIgnoreCaseAndCodigoNot(numSerieNormalizado, existente.getCodigo())) {
            throw new ConflitoException("O número de série informado já pertence a outra aeronave.");
        }

        EmpresaOperadora empresa = localizarEmpresaOuFalhar(cnpjAutorizado);

        existente.setFabricante(request.fabricante().trim());
        existente.setModelo(request.modelo().trim());
        existente.setNumSerie(numSerieNormalizado);
        existente.setPesoMaxDecolagem(request.pesoMaxDecolagem());
        existente.setTipoUso(request.tipoUso());
        existente.setDataValidadeCadastro(request.dataValidadeCadastro());
        existente.setEmpresaOperadora(empresa);

        Drone atualizado = droneRepository.save(existente);
        return DroneResponse.from(atualizado);
    }

    @Transactional
    public void remover(String codigoInformado) {
        Drone existente = localizarOuFalhar(codigoInformado);
        tenantContext.validarAcessoEmpresa(existente.getEmpresaOperadora().getCnpj());
        droneRepository.delete(existente);
    }

    private Drone localizarOuFalhar(String codigoInformado) {
        String codigo = codigoInformado == null ? "" : codigoInformado.trim().toUpperCase();
        return droneRepository.findWithEmpresaByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Drone não encontrado para o código informado."
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

    private String normalizarEValidarCodigo(String codigoInformado) {
        String codigo = codigoInformado == null ? "" : codigoInformado.trim().toUpperCase();
        if (!PADRAO_ANAC.matcher(codigo).matches()) {
            throw new RegraDeNegocioException(
                    "Código de drone fora do padrão ANAC brasileiro (prefixos PR, PP ou PS)."
            );
        }
        return codigo;
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
