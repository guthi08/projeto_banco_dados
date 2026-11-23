package com.drone.api.controller;

import com.drone.api.dto.request.PilotoRequest;
import com.drone.api.dto.response.PilotoResponse;
import com.drone.api.service.PilotoService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pilotos")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 4 · Pilotos", description = "Gestão de pilotos, licenças de voo e validade do CMA")
public class PilotoController {

    private final PilotoService pilotoService;

    @GetMapping
    @Operation(summary = "Listar pilotos com filtros e paginação")
    public ResponseEntity<Page<PilotoResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String empresaCnpj,
            @RequestParam(required = false) Boolean apenasCmaValido,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                pilotoService.listar(busca, nome, empresaCnpj, apenasCmaValido, pageable)
        );
    }

    @GetMapping("/{cpf}")
    @Operation(summary = "Consultar piloto por CPF")
    public ResponseEntity<PilotoResponse> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(pilotoService.buscarPorCpf(cpf));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo piloto")
    public ResponseEntity<PilotoResponse> cadastrar(@Valid @RequestBody PilotoRequest request) {
        PilotoResponse criado = pilotoService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{cpf}")
    @Operation(summary = "Atualizar dados cadastrais, telefones e validade do CMA do piloto")
    public ResponseEntity<PilotoResponse> atualizar(
            @PathVariable String cpf,
            @Valid @RequestBody PilotoRequest request
    ) {
        return ResponseEntity.ok(pilotoService.atualizar(cpf, request));
    }

    @DeleteMapping("/{cpf}")
    @Operation(summary = "Remover piloto")
    public ResponseEntity<Void> remover(@PathVariable String cpf) {
        pilotoService.remover(cpf);
        return ResponseEntity.noContent().build();
    }
}
