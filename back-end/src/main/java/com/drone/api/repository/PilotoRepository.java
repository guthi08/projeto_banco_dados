package com.drone.api.repository;

import com.drone.api.entity.Piloto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PilotoRepository extends JpaRepository<Piloto, String> {

    boolean existsByCpf(String cpf);

    boolean existsByRgIgnoreCase(String rg);

    boolean existsByRgIgnoreCaseAndCpfNot(String rg, String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndCpfNot(String email, String cpf);

    boolean existsByNumRegistroLicencaIgnoreCase(String numRegistroLicenca);

    boolean existsByNumRegistroLicencaIgnoreCaseAndCpfNot(String numRegistroLicenca, String cpf);

    // Aproveita o índice idx_nome_piloto na tabela piloto
    List<Piloto> findByNomeContainingIgnoreCase(String nome);

    @EntityGraph(attributePaths = {"telefones", "empresaOperadora"})
    Optional<Piloto> findWithDetailsByCpf(String cpf);

    @EntityGraph(attributePaths = {"telefones", "empresaOperadora"})
    @Query("""
            SELECT p FROM Piloto p
            WHERE (:busca IS NULL OR :busca = ''
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(p.email) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(p.numRegistroLicenca) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR p.cpf LIKE CONCAT('%', :buscaDigitos, '%'))
              AND (:nome IS NULL OR :nome = ''
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:empresaCnpj IS NULL OR :empresaCnpj = ''
                   OR p.empresaOperadora.cnpj = :empresaCnpj)
              AND (:dataMinimaCma IS NULL
                   OR p.validadeCma >= :dataMinimaCma)
            """)
    Page<Piloto> search(
            @Param("busca") String busca,
            @Param("buscaDigitos") String buscaDigitos,
            @Param("nome") String nome,
            @Param("empresaCnpj") String empresaCnpj,
            @Param("dataMinimaCma") LocalDate dataMinimaCma,
            Pageable pageable
    );
}
