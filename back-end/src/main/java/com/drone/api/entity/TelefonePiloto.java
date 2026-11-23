package com.drone.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "telefone_piloto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelefonePiloto {

    @EmbeddedId
    private TelefonePilotoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("pilotoCpf")
    @JoinColumn(name = "piloto_cpf", referencedColumnName = "cpf", nullable = false)
    private Piloto piloto;

    @Column(name = "piloto_Empresa_Ope_cnpj", length = 18, nullable = false)
    private String pilotoEmpresaOpeCnpj;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefonePiloto that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
