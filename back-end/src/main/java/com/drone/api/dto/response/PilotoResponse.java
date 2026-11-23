package com.drone.api.dto.response;

import com.drone.api.entity.Piloto;

import java.time.LocalDate;
import java.util.List;

public record PilotoResponse(
        String cpf,
        String rg,
        String nome,
        String email,
        String numRegistroLicenca,
        LocalDate validadeCma,
        List<String> telefones,
        String empresaOpeCnpj,
        boolean cmaValido
) {
    public static PilotoResponse from(Piloto entity) {
        List<String> telefonesFormatados = entity.getTelefones().stream()
                .map(t -> formatPhone(t.getId().getTelefone()))
                .toList();

        return new PilotoResponse(
                formatDocument(entity.getCpf()),
                entity.getRg(),
                entity.getNome(),
                entity.getEmail(),
                entity.getNumRegistroLicenca(),
                entity.getValidadeCma(),
                telefonesFormatados,
                formatDocument(entity.getEmpresaOperadora().getCnpj()),
                entity.isCmaValidoEm(LocalDate.now())
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
