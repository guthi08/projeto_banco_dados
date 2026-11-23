package com.drone.api.service;

import com.drone.api.config.security.JwtService;
import com.drone.api.dto.request.ForgotPasswordRequest;
import com.drone.api.dto.request.LoginRequest;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.dto.request.ResetPasswordRequest;
import com.drone.api.dto.response.ApiMessageResponse;
import com.drone.api.dto.response.AuthUserResponse;
import com.drone.api.dto.response.TokenResponse;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.Usuario;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailNormalizado, request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta está desativada.");
        }

        String token = jwtService.generateToken(usuario);
        return TokenResponse.bearer(token, jwtService.getExpirationMs(), AuthUserResponse.from(usuario));
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        String cnpjDigits = normalizeDigits(request.cnpjCpf());
        String telefoneDigits = normalizeDigits(request.telefone());

        if (cnpjDigits.length() != 11 && cnpjDigits.length() != 14) {
            throw new RegraDeNegocioException("Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.");
        }
        if (telefoneDigits.length() != 10 && telefoneDigits.length() != 11) {
            throw new RegraDeNegocioException("Informe um telefone válido com DDD (10 ou 11 dígitos).");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)
                || empresaRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe um cadastro utilizando este endereço de e-mail.");
        }
        if (empresaRepository.existsByCnpj(cnpjDigits)) {
            throw new ConflitoException("Já existe uma empresa operadora cadastrada com este CPF/CNPJ.");
        }
        if (empresaRepository.existsByRazaoSocialIgnoreCase(request.razaoSocial().trim())) {
            throw new ConflitoException("Já existe uma empresa operadora cadastrada com esta razão social.");
        }

        EmpresaOperadora empresa = EmpresaOperadora.builder()
                .cnpj(cnpjDigits)
                .razaoSocial(request.razaoSocial().trim())
                .setorAtuacao(request.setorAtuacao().trim())
                .email(emailNormalizado)
                .statusContrato(StatusContrato.Ativo)
                .build();
        empresa.adicionarTelefone(telefoneDigits);
        EmpresaOperadora empresaSalva = empresaRepository.save(empresa);

        Usuario usuario = Usuario.builder()
                .nome(request.nomeResponsavel().trim())
                .email(emailNormalizado)
                .senhaHash(passwordEncoder.encode(request.senha()))
                .role(RoleUsuario.OPERADOR)
                .ativo(true)
                .empresa(empresaSalva)
                .build();
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuarioSalvo);
        return TokenResponse.bearer(token, jwtService.getExpirationMs(), AuthUserResponse.from(usuarioSalvo));
    }

    @Transactional
    public ApiMessageResponse forgotPassword(ForgotPasswordRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        usuarioRepository.findByEmailIgnoreCase(emailNormalizado).ifPresent(usuario -> {
            if (usuario.isAtivo()) {
                String token = UUID.randomUUID().toString();
                usuario.setResetToken(token);
                usuario.setResetTokenExpiraEm(Instant.now().plus(30, ChronoUnit.MINUTES));
                usuarioRepository.save(usuario);
                log.info("Token de recuperação de senha gerado para {}: {}", emailNormalizado, token);
            }
        });

        return new ApiMessageResponse(
                "Se o e-mail informado estiver cadastrado e ativo, enviaremos as instruções de recuperação."
        );
    }

    @Transactional
    public ApiMessageResponse resetPassword(ResetPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByResetToken(request.token().trim())
                .orElseThrow(() -> new RegraDeNegocioException("Token de recuperação inválido."));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta está desativada.");
        }

        if (usuario.getResetTokenExpiraEm() == null || usuario.getResetTokenExpiraEm().isBefore(Instant.now())) {
            usuario.setResetToken(null);
            usuario.setResetTokenExpiraEm(null);
            usuarioRepository.save(usuario);
            throw new RegraDeNegocioException("Token de recuperação expirado. Solicite um novo.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiraEm(null);
        usuarioRepository.save(usuario);

        return new ApiMessageResponse("Senha redefinida com sucesso.");
    }

    private String normalizeDigits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
