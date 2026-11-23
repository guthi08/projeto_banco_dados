package com.drone.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
        name = "drone",
        indexes = {
                @Index(name = "idx_modelo_drone", columnList = "modelo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drone {

    @Id
    @Column(name = "Codigo", length = 20, nullable = false, unique = true)
    private String codigo;

    @Column(name = "peso_max_decolagem", precision = 5, scale = 2, nullable = false)
    private BigDecimal pesoMaxDecolagem;

    @Column(name = "Num_serie", length = 45, nullable = false, unique = true)
    private String numSerie;

    @Column(name = "modelo", length = 45, nullable = false)
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_uso", length = 25, nullable = false)
    private TipoUsoDrone tipoUso;

    @Column(name = "fabricante", length = 45, nullable = false)
    private String fabricante;

    @Column(name = "data_validade_cadastro", nullable = false)
    private LocalDate dataValidadeCadastro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Empresa_Ope_cnpj", referencedColumnName = "cnpj", nullable = false)
    private EmpresaOperadora empresaOperadora;

    public boolean isCadastroAnacValidoEm(LocalDate dataReferencia) {
        if (dataValidadeCadastro == null || dataReferencia == null) {
            return false;
        }
        return !dataValidadeCadastro.isBefore(dataReferencia);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Drone that)) return false;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
