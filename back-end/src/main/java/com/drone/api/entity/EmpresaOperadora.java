package com.drone.api.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Empresa_Ope")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaOperadora {

    @Id
    @Column(name = "cnpj", length = 18, nullable = false, unique = true)
    private String cnpj;

    @Column(name = "razao_social", length = 80, nullable = false, unique = true)
    private String razaoSocial;

    @Column(name = "setor_atuacao", length = 45, nullable = false)
    private String setorAtuacao;

    @Column(name = "email", length = 80, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_contrato", length = 20, nullable = false)
    @Builder.Default
    private StatusContrato statusContrato = StatusContrato.Ativo;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TelefoneEmpresa> telefones = new ArrayList<>();

    public void adicionarTelefone(String numeroNormalizado) {
        TelefoneEmpresaId telId = new TelefoneEmpresaId(numeroNormalizado, this.cnpj);
        TelefoneEmpresa tel = new TelefoneEmpresa(telId, this);
        if (!this.telefones.contains(tel)) {
            this.telefones.add(tel);
        }
    }

    public void substituirTelefones(List<String> numerosNormalizados) {
        this.telefones.clear();
        for (String numero : numerosNormalizados) {
            adicionarTelefone(numero);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmpresaOperadora that)) return false;
        return Objects.equals(cnpj, that.cnpj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cnpj);
    }
}
