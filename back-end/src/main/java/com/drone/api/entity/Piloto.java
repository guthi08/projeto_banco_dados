package com.drone.api.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "piloto",
        indexes = {
                @Index(name = "idx_nome_piloto", columnList = "nome")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Piloto {

    @Id
    @Column(name = "cpf", length = 14, nullable = false, unique = true)
    private String cpf;

    @Column(name = "rg", length = 12, nullable = false, unique = true)
    private String rg;

    @Column(name = "emial", length = 80, nullable = false, unique = true)
    private String email;

    @Column(name = "validade_CMA", nullable = false)
    private LocalDate validadeCma;

    @Column(name = "num_registro_licenca", length = 20, nullable = false, unique = true)
    private String numRegistroLicenca;

    @Column(name = "nome", length = 45, nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Empresa_Ope_cnpj", referencedColumnName = "cnpj", nullable = false)
    private EmpresaOperadora empresaOperadora;

    @OneToMany(mappedBy = "piloto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TelefonePiloto> telefones = new ArrayList<>();

    public boolean isCmaValidoEm(LocalDate dataReferencia) {
        if (validadeCma == null || dataReferencia == null) {
            return false;
        }
        return !validadeCma.isBefore(dataReferencia);
    }

    public void adicionarTelefone(String numeroNormalizado) {
        String empresaCnpj = this.empresaOperadora != null ? this.empresaOperadora.getCnpj() : "";
        TelefonePilotoId telId = new TelefonePilotoId(numeroNormalizado, this.cpf);
        TelefonePiloto tel = new TelefonePiloto(telId, this, empresaCnpj);
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
        if (!(o instanceof Piloto that)) return false;
        return Objects.equals(cpf, that.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cpf);
    }
}
