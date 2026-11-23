package com.drone.api.service;

import com.drone.api.config.security.JwtService;
import com.drone.api.dto.request.RegisterRequest;
import com.drone.api.dto.request.ResetPasswordRequest;
import com.drone.api.dto.response.ApiMessageResponse;
import com.drone.api.dto.response.TokenResponse;
import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.Usuario;
import com.drone.api.exception.ConflitoException;
import com.drone.api.exception.RegraDeNegocioException;
import com.drone.api.repository.EmpresaOperadoraRepository;
import com.drone.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaOperadoraRepository empresaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve cadastrar empresa operadora e usuário administrador retornando token JWT")
    void deveCadastrarOperadoraComSucesso() {
        RegisterRequest request = new RegisterRequest(
                "Carlos Silva",
                "SkyDrone Operações Ltda",
                "12.345.678/0001-95",
                "Logística",
                "(11) 98888-7766",
                "carlos@skydrone.com.br",
                "Senha@123"
        );

        when(usuarioRepository.existsByEmailIgnoreCase("carlos@skydrone.com.br")).thenReturn(false);
        when(empresaRepository.existsByEmailIgnoreCase("carlos@skydrone.com.br")).thenReturn(false);
        when(empresaRepository.existsByCnpj("12345678000195")).thenReturn(false);
        when(empresaRepository.existsByRazaoSocialIgnoreCase("SkyDrone Operações Ltda")).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$10$encoded");
        when(empresaRepository.save(any(EmpresaOperadora.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            u.setCriadoEm(Instant.now());
            return u;
        });
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-token-gerado");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        TokenResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token-gerado");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.usuario().email()).isEqualTo("carlos@skydrone.com.br");
        assertThat(response.usuario().razaoSocial()).isEqualTo("SkyDrone Operações Ltda");
        assertThat(response.usuario().empresaCnpj()).isEqualTo("12.345.678/0001-95");
        assertThat(response.usuario().telefone()).isEqualTo("(11) 98888-7766");
    }

    @Test
    @DisplayName("Deve lançar ConflitoException quando CNPJ já estiver cadastrado")
    void deveRejeitarCnpjDuplicado() {
        RegisterRequest request = new RegisterRequest(
                "Carlos Silva",
                "SkyDrone Operações Ltda",
                "12.345.678/0001-95",
                "Logística",
                "(11) 98888-7766",
                "carlos@skydrone.com.br",
                "Senha@123"
        );

        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(empresaRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(empresaRepository.existsByCnpj("12345678000195")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("CPF/CNPJ");
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando token de recuperação estiver expirado")
    void deveRejeitarTokenExpirado() {
        EmpresaOperadora empresa = EmpresaOperadora.builder()
                .cnpj("12345678000195")
                .razaoSocial("SkyDrone")
                .setorAtuacao("Energia")
                .email("a@b.com")
                .statusContrato(StatusContrato.Ativo)
                .build();

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Carlos")
                .email("a@b.com")
                .senhaHash("hash")
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .empresa(empresa)
                .resetToken("tok-expirado")
                .resetTokenExpiraEm(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(usuarioRepository.findByResetToken("tok-expirado")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("tok-expirado", "NovaSenha@1")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("expirado");

        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Deve redefinir senha com token válido")
    void deveRedefinirSenhaComSucesso() {
        EmpresaOperadora empresa = EmpresaOperadora.builder()
                .cnpj("12345678000195")
                .razaoSocial("SkyDrone")
                .setorAtuacao("Energia")
                .email("a@b.com")
                .statusContrato(StatusContrato.Ativo)
                .build();

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Carlos")
                .email("a@b.com")
                .senhaHash("hash-antigo")
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .empresa(empresa)
                .resetToken("tok-valido")
                .resetTokenExpiraEm(Instant.now().plus(20, ChronoUnit.MINUTES))
                .build();

        when(usuarioRepository.findByResetToken("tok-valido")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NovaSenha@1")).thenReturn("hash-novo");

        ApiMessageResponse response = authService.resetPassword(new ResetPasswordRequest("tok-valido", "NovaSenha@1"));

        assertThat(response.message()).contains("sucesso");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash-novo");
        assertThat(usuario.getResetToken()).isNull();
    }
}
