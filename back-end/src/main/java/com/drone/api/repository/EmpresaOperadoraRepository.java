package com.drone.api.repository;

import com.drone.api.entity.EmpresaOperadora;
import com.drone.api.entity.StatusContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmpresaOperadoraRepository extends JpaRepository<EmpresaOperadora, String> {

    boolean existsByCnpj(String cnpj);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndCnpjNot(String email, String cnpj);

    boolean existsByRazaoSocialIgnoreCase(String razaoSocial);

    boolean existsByRazaoSocialIgnoreCaseAndCnpjNot(String razaoSocial, String cnpj);

    Optional<EmpresaOperadora> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"telefones"})
    Optional<EmpresaOperadora> findWithTelefonesByCnpj(String cnpj);

    @EntityGraph(attributePaths = {"telefones"})
    @Query("""
            SELECT e FROM EmpresaOperadora e
            WHERE (:cnpjRestrito IS NULL OR :cnpjRestrito = '' OR e.cnpj = :cnpjRestrito)
              AND (:busca IS NULL OR :busca = ''
                   OR LOWER(e.razaoSocial) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(e.email) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR e.cnpj LIKE CONCAT('%', :buscaDigitos, '%'))
              AND (:setor IS NULL OR :setor = '' OR LOWER(e.setorAtuacao) = LOWER(:setor))
              AND (:status IS NULL OR e.statusContrato = :status)
            """)
    Page<EmpresaOperadora> search(
            @Param("cnpjRestrito") String cnpjRestrito,
            @Param("busca") String busca,
            @Param("buscaDigitos") String buscaDigitos,
            @Param("setor") String setor,
            @Param("status") StatusContrato status,
            Pageable pageable
    );
}
