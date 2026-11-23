package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusContratoRequest;
import com.drone.api.dto.request.ClienteRequest;
import com.drone.api.dto.response.ClienteResponse;
import com.drone.api.entity.StatusContrato;
import com.drone.api.service.ClienteService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 5 · Clientes", description = "Gestão de clientes tomadores de serviço, contratos e telefones")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Listar clientes com filtros e paginação")
    public ResponseEntity<Page<ClienteResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String empresaCnpj,
            @RequestParam(required = false) StatusContrato status,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(clienteService.listar(busca, nome, empresaCnpj, status, pageable));
    }

    @GetMapping("/{cpfRg}")
    @Operation(summary = "Consultar cliente por CPF ou CNPJ")
    public ResponseEntity<ClienteResponse> buscarPorCpfRg(@PathVariable String cpfRg) {
        return ResponseEntity.ok(clienteService.buscarPorCpfRg(cpfRg));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo cliente e formalizar contrato com operadora")
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = clienteService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{cpfRg}")
    @Operation(summary = "Atualizar dados cadastrais e telefones do cliente")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable String cpfRg,
            @Valid @RequestBody ClienteRequest request
    ) {
        return ResponseEntity.ok(clienteService.atualizar(cpfRg, request));
    }

    @PatchMapping("/{cpfRg}/status")
    @Operation(summary = "Alterar status do contrato do cliente (Ativo ou Inativo)")
    public ResponseEntity<ClienteResponse> alterarStatus(
            @PathVariable String cpfRg,
            @Valid @RequestBody AtualizarStatusContratoRequest request
    ) {
        return ResponseEntity.ok(clienteService.alterarStatus(cpfRg, request));
    }

    @DeleteMapping("/{cpfRg}")
    @Operation(summary = "Remover cliente")
    public ResponseEntity<Void> remover(@PathVariable String cpfRg) {
        clienteService.remover(cpfRg);
        return ResponseEntity.noContent().build();
    }
}
