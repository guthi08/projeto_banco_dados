package com.drone.api.repository;

import com.drone.api.entity.Auditoria;
import com.drone.api.entity.NivelGravidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

    @EntityGraph(attributePaths = {"operacaoVoo", "operacaoVoo.piloto", "operacaoVoo.drone", "operacaoVoo.empresaOperadora"})
    Optional<Auditoria> findWithDetailsByCodigo(Integer codigo);

    @EntityGraph(attributePaths = {"operacaoVoo", "operacaoVoo.piloto", "operacaoVoo.drone", "operacaoVoo.empresaOperadora"})
    List<Auditoria> findByOperacaoVooCodigoOrderByDataHoraEventoDesc(Integer operacaoVooCodigo);

    @EntityGraph(attributePaths = {"operacaoVoo", "operacaoVoo.piloto", "operacaoVoo.drone", "operacaoVoo.empresaOperadora"})
    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:empresaCnpj IS NULL OR :empresaCnpj = '' OR a.operacaoVoo.empresaOperadora.cnpj = :empresaCnpj)
              AND (:busca IS NULL OR :busca = ''
                   OR LOWER(a.categoria) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.operacaoVoo.piloto.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.operacaoVoo.drone.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.operacaoVoo.empresaOperadora.razaoSocial) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:operacaoVooCodigo IS NULL OR a.operacaoVoo.codigo = :operacaoVooCodigo)
              AND (:categoria IS NULL OR :categoria = '' OR LOWER(a.categoria) = LOWER(:categoria))
              AND (:nivelGravidade IS NULL OR a.nivelGravidade = :nivelGravidade)
              AND (:inicio IS NULL OR a.dataHoraEvento >= :inicio)
              AND (:fim IS NULL OR a.dataHoraEvento <= :fim)
            """)
    Page<Auditoria> search(
            @Param("empresaCnpj") String empresaCnpj,
            @Param("busca") String busca,
            @Param("operacaoVooCodigo") Integer operacaoVooCodigo,
            @Param("categoria") String categoria,
            @Param("nivelGravidade") NivelGravidade nivelGravidade,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );
}
