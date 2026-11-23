package com.drone.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelefoneEmpresaId implements Serializable {

    @Column(name = "telefone", length = 11, nullable = false)
    private String telefone;

    @Column(name = "Empresa_Ope_cnpj", length = 18, nullable = false)
    private String empresaOpeCnpj;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefoneEmpresaId that)) return false;
        return Objects.equals(telefone, that.telefone)
                && Objects.equals(empresaOpeCnpj, that.empresaOpeCnpj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telefone, empresaOpeCnpj);
    }
}
