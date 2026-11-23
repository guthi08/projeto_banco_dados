package com.drone.api.dto.response;

import com.drone.api.entity.Drone;
import com.drone.api.entity.TipoUsoDrone;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DroneResponse(
        String codigo,
        String fabricante,
        String modelo,
        String numSerie,
        BigDecimal pesoMaxDecolagem,
        TipoUsoDrone tipoUso,
        LocalDate dataValidadeCadastro,
        String empresaOpeCnpj,
        boolean anacValido
) {
    public static DroneResponse from(Drone entity) {
        return new DroneResponse(
                entity.getCodigo(),
                entity.getFabricante(),
                entity.getModelo(),
                entity.getNumSerie(),
                entity.getPesoMaxDecolagem(),
                entity.getTipoUso(),
                entity.getDataValidadeCadastro(),
                formatDocument(entity.getEmpresaOperadora().getCnpj()),
                entity.isCadastroAnacValidoEm(LocalDate.now())
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
