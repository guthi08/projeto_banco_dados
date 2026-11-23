package com.drone.api.controller;

import com.drone.api.dto.request.ChangePasswordRequest;
import com.drone.api.dto.request.DeactivateAccountRequest;
import com.drone.api.dto.request.UpdateProfileRequest;
import com.drone.api.dto.response.ApiMessageResponse;
import com.drone.api.dto.response.AuthUserResponse;
import com.drone.api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "MOD-BE 1 · Conta do Operador", description = "Perfil, troca de senha e desativação lógica")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    @Operation(summary = "Consultar perfil do operador autenticado")
    public ResponseEntity<AuthUserResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(accountService.getProfile(principal.getUsername()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Atualizar dados cadastrais do perfil e operadora")
    public ResponseEntity<AuthUserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(accountService.updateProfile(principal.getUsername(), request));
    }

    @PatchMapping("/password")
    @Operation(summary = "Alterar senha da conta autenticada")
    public ResponseEntity<ApiMessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(accountService.changePassword(principal.getUsername(), request));
    }

    @PatchMapping("/deactivate")
    @Operation(summary = "Excluir/desativar logicamente a conta do operador mediante confirmação de senha")
    public ResponseEntity<ApiMessageResponse> deactivateAccount(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DeactivateAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.deactivateAccount(principal.getUsername(), request));
    }
}
