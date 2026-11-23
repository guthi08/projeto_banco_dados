package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.EmpresaOperadoraRequest;
import com.drone.api.dto.response.EmpresaOperadoraResponse;
import com.drone.api.entity.StatusContrato;
import com.drone.api.service.EmpresaOperadoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 2 · Empresas Operadoras", description = "Consulta e manutenção cadastral das empresas operadoras")
public class EmpresaOperadoraController {

    private final EmpresaOperadoraService empresaService;

    @GetMapping
    @Operation(summary = "Listar empresas operadoras com filtros e paginação")
    public ResponseEntity<Page<EmpresaOperadoraResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String setor,
            @RequestParam(required = false) StatusContrato status,
            @PageableDefault(size = 10, sort = "razaoSocial", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(empresaService.listar(busca, setor, status, pageable));
    }

    @GetMapping("/{cnpj}")
    @Operation(summary = "Consultar empresa operadora por CPF ou CNPJ")
    public ResponseEntity<EmpresaOperadoraResponse> buscarPorCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(empresaService.buscarPorCnpj(cnpj));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova empresa operadora")
    public ResponseEntity<EmpresaOperadoraResponse> cadastrar(
            @Valid @RequestBody EmpresaOperadoraRequest request
    ) {
        EmpresaOperadoraResponse criada = empresaService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{cnpj}")
    @Operation(summary = "Atualizar dados cadastrais da empresa operadora")
    public ResponseEntity<EmpresaOperadoraResponse> atualizar(
            @PathVariable String cnpj,
            @Valid @RequestBody EmpresaOperadoraRequest request
    ) {
        return ResponseEntity.ok(empresaService.atualizar(cnpj, request));
    }

    @PatchMapping("/{cnpj}/status")
    @Operation(summary = "Alterar status contratual da empresa operadora")
    public ResponseEntity<EmpresaOperadoraResponse> alterarStatus(
            @PathVariable String cnpj,
            @Valid @RequestBody AtualizarStatusContratoRequest request
    ) {
        return ResponseEntity.ok(empresaService.alterarStatus(cnpj, request));
    }
}
