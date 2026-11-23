package com.drone.api.dto.response;

import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.StatusContrato;

import java.util.List;

public record EmpresaOperadoraResponse(
        String cnpj,
        String razaoSocial,
        String setorAtuacao,
        String email,
        List<String> telefones,
        StatusContrato statusContrato
) {
    public static EmpresaOperadoraResponse from(EmpresaOperadora entity) {
        List<String> telefonesFormatados = entity.getTelefones().stream()
                .map(t -> formatPhone(t.getId().getTelefone()))
                .toList();

        return new EmpresaOperadoraResponse(
                formatDocument(entity.getCnpj()),
                entity.getRazaoSocial(),
                entity.getSetorAtuacao(),
                entity.getEmail(),
                telefonesFormatados,
                entity.getStatusContrato()
        );
    }

    private static String formatDocument(String raw) {
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

    private static String formatPhone(String raw) {
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
