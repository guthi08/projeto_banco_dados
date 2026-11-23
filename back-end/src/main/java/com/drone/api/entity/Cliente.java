package com.drone.api.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "clientes",
        indexes = {
                @Index(name = "idx_nome_cliente", columnList = "nome")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @Column(name = "cpf_rg", length = 14, nullable = false, unique = true)
    private String cpfRg;

    @Column(name = "rg", length = 12, nullable = false, unique = true)
    private String rg;

    @Column(name = "nome", length = 60, nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Empresa_Ope_cnpj", referencedColumnName = "cnpj", nullable = false)
    private EmpresaOperadora empresaOperadora;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_contrato", length = 20, nullable = false)
    @Builder.Default
    private StatusContrato statusContrato = StatusContrato.Ativo;

    @Column(name = "data_", nullable = false)
    private LocalDate dataFormalizacao;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TelefoneCliente> telefones = new ArrayList<>();

    public boolean isContratoAtivo() {
        return this.statusContrato == StatusContrato.Ativo;
    }

    public void adicionarTelefone(String numeroNormalizado) {
        String empresaCnpj = this.empresaOperadora != null ? this.empresaOperadora.getCnpj() : "";
        TelefoneClienteId telId = new TelefoneClienteId(numeroNormalizado, this.cpfRg);
        TelefoneCliente tel = new TelefoneCliente(telId, this, empresaCnpj);
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
        if (!(o instanceof Cliente that)) return false;
        return Objects.equals(cpfRg, that.cpfRg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cpfRg);
    }
}
