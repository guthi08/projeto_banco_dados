package com.drone.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "Operacao_voo",
        indexes = {
                @Index(name = "idx_status_voo", columnList = "status_voo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperacaoVoo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Codigo")
    private Integer codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_voo", length = 20, nullable = false)
    @Builder.Default
    private StatusVoo statusVoo = StatusVoo.AGENDADO;

    @Column(name = "Obetivo_voo", length = 120, nullable = false)
    private String objetivoVoo;

    @Column(name = "data_hora_planejada", nullable = false)
    private LocalDateTime dataHoraPlanejada;

    @Column(name = "duracao_estimada_min", nullable = false)
    @Builder.Default
    private Integer duracaoEstimadaMin = 60;

    @Column(name = "data_hora_real")
    private LocalDateTime dataHoraReal;

    @Column(name = "data_hora_pouso_real")
    private LocalDateTime dataHoraPousoReal;

    @Column(name = "origem", length = 100, nullable = false)
    private String origem;

    @Column(name = "destino", length = 100, nullable = false)
    private String destino;

    @Column(name = "descricao_area", length = 120, nullable = false)
    private String descricaoArea;

    @Column(name = "tipo_local", length = 45, nullable = false)
    private String tipoLocal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drone_Codigo", referencedColumnName = "Codigo", nullable = false)
    private Drone drone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "piloto_cpf", referencedColumnName = "cpf", nullable = false)
    private Piloto piloto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clientes_cpf_rg", referencedColumnName = "cpf_rg", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clientes_Empresa_Ope_cnpj", referencedColumnName = "cnpj", nullable = false)
    private EmpresaOperadora empresaOperadora;

    public LocalDateTime calcularInicioEfetivo() {
        return dataHoraReal != null ? dataHoraReal : dataHoraPlanejada;
    }

    public LocalDateTime calcularFimEfetivo() {
        int minutos = (duracaoEstimadaMin != null && duracaoEstimadaMin > 0) ? duracaoEstimadaMin : 60;
        return calcularInicioEfetivo().plusMinutes(minutos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperacaoVoo that)) return false;
        return codigo != null && Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
