package com.drone.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "Auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo")
    private Integer codigo;

    @Column(name = "data_hora_evento", nullable = false)
    private LocalDateTime dataHoraEvento;

    @Column(name = "categoria", length = 60, nullable = false)
    private String categoria;

    @Column(name = "descricao", length = 500, nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_gravidade", length = 20, nullable = false)
    private NivelGravidade nivelGravidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Operacao_voo_Codigo", referencedColumnName = "Codigo", nullable = false)
    private OperacaoVoo operacaoVoo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Auditoria that)) return false;
        return codigo != null && Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
