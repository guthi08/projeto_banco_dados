package com.drone.api.controller;

import com.drone.api.dto.request.AtualizarStatusVooRequest;
import com.drone.api.dto.request.OperacaoVooRequest;
import com.drone.api.dto.response.OperacaoVooResponse;
import com.drone.api.entity.StatusVoo;
import com.drone.api.service.OperacaoVooService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/operacoes")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 6 · Operações de Voo", description = "Agendamento, despacho pré-voo e ciclo de vida das missões")
public class OperacaoVooController {

    private final OperacaoVooService operacaoService;

    @GetMapping
    @Operation(summary = "Listar operações de voo com filtros e paginação")
    public ResponseEntity<Page<OperacaoVooResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusVoo status,
            @RequestParam(required = false) String empresaCnpj,
            @RequestParam(required = false) String droneCodigo,
            @RequestParam(required = false) String pilotoCpf,
            @RequestParam(required = false) String clienteCpfRg,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @PageableDefault(size = 10, sort = "dataHoraPlanejada", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                operacaoService.listar(
                        busca,
                        status,
                        empresaCnpj,
                        droneCodigo,
                        pilotoCpf,
                        clienteCpfRg,
                        inicio,
                        fim,
                        pageable
                )
        );
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar operação de voo por código")
    public ResponseEntity<OperacaoVooResponse> buscarPorCodigo(@PathVariable Integer codigo) {
        return ResponseEntity.ok(operacaoService.buscarPorCodigo(codigo));
    }

    @PostMapping
    @Operation(summary = "Agendar nova operação de voo com validação pré-voo (ANAC, CMA e agenda)")
    public ResponseEntity<OperacaoVooResponse> agendar(@Valid @RequestBody OperacaoVooRequest request) {
        OperacaoVooResponse criada = operacaoService.agendar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Atualizar operação ainda agendada revalidando regras pré-voo")
    public ResponseEntity<OperacaoVooResponse> atualizar(
            @PathVariable Integer codigo,
            @Valid @RequestBody OperacaoVooRequest request
    ) {
        return ResponseEntity.ok(operacaoService.atualizar(codigo, request));
    }

    @PatchMapping("/{codigo}/status")
    @Operation(summary = "Atualizar status da operação de voo (EM_ANDAMENTO, CONCLUIDO ou CANCELADO)")
    public ResponseEntity<OperacaoVooResponse> alterarStatus(
            @PathVariable Integer codigo,
            @Valid @RequestBody AtualizarStatusVooRequest request
    ) {
        return ResponseEntity.ok(operacaoService.alterarStatus(codigo, request));
    }
}
