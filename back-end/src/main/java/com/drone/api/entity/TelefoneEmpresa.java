package com.drone.api.entity;

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
@Table(name = "telefone_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelefoneEmpresa {

    @EmbeddedId
    private TelefoneEmpresaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("empresaOpeCnpj")
    @JoinColumn(name = "Empresa_Ope_cnpj", referencedColumnName = "cnpj", nullable = false)
    private EmpresaOperadora empresa;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefoneEmpresa that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
