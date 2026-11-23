package com.drone.api.dto.response;

import com.drone.api.entity.Cliente;
import com.drone.api.entity.StatusContrato;

import java.time.LocalDate;
import java.util.List;

public record ClienteResponse(
        String cpfRg,
        String rg,
        String nome,
        String empresaOpeCnpj,
        StatusContrato statusContrato,
        LocalDate dataFormalizacao,
        List<String> telefones
) {
    public static ClienteResponse from(Cliente entity) {
        List<String> telefonesFormatados = entity.getTelefones().stream()
                .map(t -> formatPhone(t.getId().getTelefone()))
                .toList();

        return new ClienteResponse(
                formatDocument(entity.getCpfRg()),
                entity.getRg(),
                entity.getNome(),
                formatDocument(entity.getEmpresaOperadora().getCnpj()),
                entity.getStatusContrato(),
                entity.getDataFormalizacao(),
                telefonesFormatados
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
