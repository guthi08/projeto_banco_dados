package com.drone.api.controller;

import com.drone.api.dto.request.DroneRequest;
import com.drone.api.dto.response.DroneResponse;
import com.drone.api.entity.TipoUsoDrone;
import com.drone.api.service.DroneService;
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
@RequestMapping("/api/v1/drones")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 3 · Drones da Frota", description = "Gestão de aeronaves e controle de validade do cadastro ANAC")
public class DroneController {

    private final DroneService droneService;

    @GetMapping
    @Operation(summary = "Listar drones da frota com filtros e paginação")
    public ResponseEntity<Page<DroneResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) TipoUsoDrone tipoUso,
            @RequestParam(required = false) String empresaCnpj,
            @RequestParam(required = false) Boolean apenasValidos,
            @PageableDefault(size = 10, sort = "modelo", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                droneService.listar(busca, modelo, tipoUso, empresaCnpj, apenasValidos, pageable)
        );
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar drone por código de registro ANAC")
    public ResponseEntity<DroneResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(droneService.buscarPorCodigo(codigo));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo drone na frota")
    public ResponseEntity<DroneResponse> cadastrar(@Valid @RequestBody DroneRequest request) {
        DroneResponse criado = droneService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Atualizar dados cadastrais e validade ANAC do drone")
    public ResponseEntity<DroneResponse> atualizar(
            @PathVariable String codigo,
            @Valid @RequestBody DroneRequest request
    ) {
        return ResponseEntity.ok(droneService.atualizar(codigo, request));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Remover drone da frota")
    public ResponseEntity<Void> remover(@PathVariable String codigo) {
        droneService.remover(codigo);
        return ResponseEntity.noContent().build();
    }
}
