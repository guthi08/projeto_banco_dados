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
@Table(name = "telefone_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelefoneCliente {

    @EmbeddedId
    private TelefoneClienteId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("clienteCpfRg")
    @JoinColumn(name = "clientes_cpf_rg", referencedColumnName = "cpf_rg", nullable = false)
    private Cliente cliente;

    @Column(name = "clientes_Empresa_Ope_cnpj", length = 18, nullable = false)
    private String clienteEmpresaOpeCnpj;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefoneCliente that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
