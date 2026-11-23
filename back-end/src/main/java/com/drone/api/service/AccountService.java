package com.drone.api.service;

import com.drone.api.dto.request.ChangePasswordRequest;
import com.drone.api.dto.request.DeactivateAccountRequest;
import com.drone.api.dto.request.UpdateProfileRequest;
import com.drone.api.dto.response.ApiMessageResponse;
import com.drone.api.dto.response.AuthUserResponse;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.Usuario;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RecursoNaoEncontradoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaOperadoraRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthUserResponse getProfile(String authenticatedEmail) {
        Usuario usuario = findActiveUser(authenticatedEmail);
        return AuthUserResponse.from(usuario);
    }

    @Transactional
    public AuthUserResponse updateProfile(String authenticatedEmail, UpdateProfileRequest request) {
        Usuario usuario = findActiveUser(authenticatedEmail);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A senha atual informada está incorreta.");
        }
        EmpresaOperadora empresa = usuario.getEmpresa();

        String novoEmail = request.email().trim().toLowerCase();
        String novaRazao = request.razaoSocial().trim();
        String telefoneDigits = normalizeDigits(request.telefone());

        if (telefoneDigits.length() != 10 && telefoneDigits.length() != 11) {
            throw new RegraDeNegocioException("Informe um telefone válido com DDD (10 ou 11 dígitos).");
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(novoEmail, usuario.getId())
                || empresaRepository.existsByEmailIgnoreCaseAndCnpjNot(novoEmail, empresa.getCnpj())) {
            throw new ConflitoException("O e-mail informado já está em uso por outra conta.");
        }
        if (empresaRepository.existsByRazaoSocialIgnoreCaseAndCnpjNot(novaRazao, empresa.getCnpj())) {
            throw new ConflitoException("A razão social informada já está em uso por outra operadora.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(novoEmail);

        empresa.setRazaoSocial(novaRazao);
        empresa.setSetorAtuacao(request.setorAtuacao().trim());
        empresa.setEmail(novoEmail);
        empresa.substituirTelefones(List.of(telefoneDigits));

        empresaRepository.save(empresa);
        Usuario atualizado = usuarioRepository.save(usuario);
        return AuthUserResponse.from(atualizado);
    }

    @Transactional
    public ApiMessageResponse changePassword(String authenticatedEmail, ChangePasswordRequest request) {
        Usuario usuario = findActiveUser(authenticatedEmail);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A senha atual informada está incorreta.");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);

        return new ApiMessageResponse("Senha alterada com sucesso.");
    }

    @Transactional
    public ApiMessageResponse deactivateAccount(String authenticatedEmail, DeactivateAccountRequest request) {
        Usuario usuario = findActiveUser(authenticatedEmail);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("A senha atual informada está incorreta.");
        }
        usuario.setAtivo(false);
        usuario.setResetToken(null);
        usuario.setResetTokenExpiraEm(null);
        usuarioRepository.save(usuario);

        return new ApiMessageResponse("Conta excluída/desativada com sucesso.");
    }

    private Usuario findActiveUser(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta de usuário não encontrada."));
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta foi desativada.");
        }
        return usuario;
    }

    private String normalizeDigits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
