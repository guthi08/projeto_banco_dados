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
public class TelefoneClienteId implements Serializable {

    @Column(name = "telefone_cliente", length = 11, nullable = false)
    private String telefone;

    @Column(name = "clientes_cpf_rg", length = 14, nullable = false)
    private String clienteCpfRg;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefoneClienteId that)) return false;
        return Objects.equals(telefone, that.telefone)
                && Objects.equals(clienteCpfRg, that.clienteCpfRg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telefone, clienteCpfRg);
    }
}
