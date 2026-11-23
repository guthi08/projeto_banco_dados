package com.drone.api.dto.response;

import com.drone.api.entity.Auditoria;
import com.drone.api.entity.NivelGravidade;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Integer codigo,
        Integer operacaoVooCodigo,
        LocalDateTime dataHoraEvento,
        String categoria,
        String descricao,
        NivelGravidade nivelGravidade,
        String pilotoNome,
        String droneCodigo,
        String empresaOpeCnpj,
        String empresaRazaoSocial
) {
    public static AuditoriaResponse from(Auditoria entity) {
        return new AuditoriaResponse(
                entity.getCodigo(),
                entity.getOperacaoVoo().getCodigo(),
                entity.getDataHoraEvento(),
                entity.getCategoria(),
                entity.getDescricao(),
                entity.getNivelGravidade(),
                entity.getOperacaoVoo().getPiloto().getNome(),
                entity.getOperacaoVoo().getDrone().getCodigo(),
                formatDocument(entity.getOperacaoVoo().getEmpresaOperadora().getCnpj()),
                entity.getOperacaoVoo().getEmpresaOperadora().getRazaoSocial()
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
}
