package com.drone.api.repository;

import com.drone.api.entity.Cliente;
import com.drone.api.entity.StatusContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    boolean existsByCpfRg(String cpfRg);

    boolean existsByRgIgnoreCase(String rg);

    boolean existsByRgIgnoreCaseAndCpfRgNot(String rg, String cpfRg);

    // Aproveita o índice idx_nome_cliente na tabela clientes
    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    @EntityGraph(attributePaths = {"telefones", "empresaOperadora"})
    Optional<Cliente> findWithDetailsByCpfRg(String cpfRg);

    @EntityGraph(attributePaths = {"telefones", "empresaOperadora"})
    @Query("""
            SELECT c FROM Cliente c
            WHERE (:busca IS NULL OR :busca = ''
                   OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(c.rg) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR c.cpfRg LIKE CONCAT('%', :buscaDigitos, '%'))
              AND (:nome IS NULL OR :nome = ''
                   OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:empresaCnpj IS NULL OR :empresaCnpj = ''
                   OR c.empresaOperadora.cnpj = :empresaCnpj)
              AND (:status IS NULL OR c.statusContrato = :status)
            """)
    Page<Cliente> search(
            @Param("busca") String busca,
            @Param("buscaDigitos") String buscaDigitos,
            @Param("nome") String nome,
            @Param("empresaCnpj") String empresaCnpj,
            @Param("status") StatusContrato status,
            Pageable pageable
    );
}
