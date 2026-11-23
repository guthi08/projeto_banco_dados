package com.drone.api.dto.response;

import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.RoleUsuario;
import com.drone.api.entity.StatusContrato;
import com.drone.api.entity.Usuario;

import java.time.Instant;

public record AuthUserResponse(
        String id,
        String nome,
        String email,
        RoleUsuario role,
        String empresaCnpj,
        String razaoSocial,
        String setorAtuacao,
        String telefone,
        StatusContrato statusContrato,
        boolean ativo,
        Instant criadoEm
) {
    public static AuthUserResponse from(Usuario usuario) {
        EmpresaOperadora empresa = usuario.getEmpresa();
        String telefonePrincipal = empresa.getTelefones().stream()
                .findFirst()
                .map(tel -> formatarTelefone(tel.getId().getTelefone()))
                .orElse("");

        return new AuthUserResponse(
                String.valueOf(usuario.getId()),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                formatarDocumento(empresa.getCnpj()),
                empresa.getRazaoSocial(),
                empresa.getSetorAtuacao(),
                telefonePrincipal,
                empresa.getStatusContrato(),
                usuario.isAtivo(),
                usuario.getCriadoEm()
        );
    }

    private static String formatarDocumento(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (digits.length() == 14) {
            return digits.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return raw;
    }

    private static String formatarTelefone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        }
        if (digits.length() == 10) {
            return digits.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
        return raw;
    }
}
