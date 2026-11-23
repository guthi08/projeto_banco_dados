package com.drone.api.repository;

import com.drone.api.entity.Drone;
import com.drone.api.entity.TipoUsoDrone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DroneRepository extends JpaRepository<Drone, String> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByNumSerieIgnoreCase(String numSerie);

    boolean existsByNumSerieIgnoreCaseAndCodigoNot(String numSerie, String codigo);

    @EntityGraph(attributePaths = {"empresaOperadora"})
    Optional<Drone> findWithEmpresaByCodigoIgnoreCase(String codigo);

    // Aproveita o índice idx_modelo_drone na tabela drone
    List<Drone> findByModeloContainingIgnoreCase(String modelo);

    @EntityGraph(attributePaths = {"empresaOperadora"})
    @Query("""
            SELECT d FROM Drone d
            WHERE (:busca IS NULL OR :busca = ''
                   OR LOWER(d.modelo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(d.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(d.fabricante) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(d.numSerie) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:modelo IS NULL OR :modelo = ''
                   OR LOWER(d.modelo) LIKE LOWER(CONCAT('%', :modelo, '%')))
              AND (:tipoUso IS NULL OR d.tipoUso = :tipoUso)
              AND (:empresaCnpj IS NULL OR :empresaCnpj = ''
                   OR d.empresaOperadora.cnpj = :empresaCnpj)
              AND (:dataMinimaValidade IS NULL
                   OR d.dataValidadeCadastro >= :dataMinimaValidade)
            """)
    Page<Drone> search(
            @Param("busca") String busca,
            @Param("modelo") String modelo,
            @Param("tipoUso") TipoUsoDrone tipoUso,
            @Param("empresaCnpj") String empresaCnpj,
            @Param("dataMinimaValidade") LocalDate dataMinimaValidade,
            Pageable pageable
    );
}
