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
public class TelefonePilotoId implements Serializable {

    @Column(name = "telefone_", length = 11, nullable = false)
    private String telefone;

    @Column(name = "piloto_cpf", length = 14, nullable = false)
    private String pilotoCpf;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefonePilotoId that)) return false;
        return Objects.equals(telefone, that.telefone)
                && Objects.equals(pilotoCpf, that.pilotoCpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telefone, pilotoCpf);
    }
}
