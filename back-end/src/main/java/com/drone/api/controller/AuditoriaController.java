package com.drone.api.controller;

import com.drone.api.dto.request.AuditoriaRequest;
import com.drone.api.dto.response.AuditoriaResponse;
import com.drone.api.entity.NivelGravidade;
import com.drone.api.service.AuditoriaService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditorias")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 7 · Auditorias Técnicas", description = "Registro imutável de ocorrências e conformidade das operações de voo")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @Operation(summary = "Listar ocorrências de auditoria com filtros e paginação")
    public ResponseEntity<Page<AuditoriaResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer operacaoVooCodigo,
            @RequestParam(required = false) String empresaCnpj,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) NivelGravidade nivelGravidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @PageableDefault(size = 10, sort = "dataHoraEvento", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                auditoriaService.listar(busca, operacaoVooCodigo, empresaCnpj, categoria, nivelGravidade, inicio, fim, pageable)
        );
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar registro de auditoria por código")
    public ResponseEntity<AuditoriaResponse> buscarPorCodigo(@PathVariable Integer codigo) {
        return ResponseEntity.ok(auditoriaService.buscarPorCodigo(codigo));
    }

    @GetMapping("/operacao/{operacaoVooCodigo}")
    @Operation(summary = "Listar ocorrências de auditoria vinculadas a uma operação de voo")
    public ResponseEntity<List<AuditoriaResponse>> listarPorOperacao(@PathVariable Integer operacaoVooCodigo) {
        return ResponseEntity.ok(auditoriaService.listarPorOperacao(operacaoVooCodigo));
    }

    @PostMapping
    @Operation(summary = "Registrar nova ocorrência técnica imutável vinculada a uma operação de voo")
    public ResponseEntity<AuditoriaResponse> registrar(@Valid @RequestBody AuditoriaRequest request) {
        AuditoriaResponse criada = auditoriaService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }
}
