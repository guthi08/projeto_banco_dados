package com.drone.api.dto.response;

import com.drone.api.entity.OperacaoVoo;
import com.drone.api.entity.StatusVoo;

import java.time.LocalDateTime;

public record OperacaoVooResponse(
        Integer codigo,
        StatusVoo statusVoo,
        String objetivoVoo,
        LocalDateTime dataHoraPlanejada,
        LocalDateTime dataHoraReal,
        LocalDateTime dataHoraPousoReal,
        Integer duracaoEstimadaMin,
        String origem,
        String destino,
        String descricaoArea,
        String tipoLocal,
        String droneCodigo,
        String droneModelo,
        String pilotoCpf,
        String pilotoNome,
        String clienteCpfRg,
        String clienteNome,
        String empresaOpeCnpj
) {
    public static OperacaoVooResponse from(OperacaoVoo entity) {
        return new OperacaoVooResponse(
                entity.getCodigo(),
                entity.getStatusVoo(),
                entity.getObjetivoVoo(),
                entity.getDataHoraPlanejada(),
                entity.getDataHoraReal(),
                entity.getDataHoraPousoReal(),
                entity.getDuracaoEstimadaMin(),
                entity.getOrigem(),
                entity.getDestino(),
                entity.getDescricaoArea(),
                entity.getTipoLocal(),
                entity.getDrone().getCodigo(),
                entity.getDrone().getFabricante() + " " + entity.getDrone().getModelo(),
                formatDocument(entity.getPiloto().getCpf()),
                entity.getPiloto().getNome(),
                formatDocument(entity.getCliente().getCpfRg()),
                entity.getCliente().getNome(),
                formatDocument(entity.getEmpresaOperadora().getCnpj())
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
