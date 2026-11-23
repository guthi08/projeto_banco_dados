package com.drone.api.repository;

import com.drone.api.entity.OperacaoVoo;
import com.drone.api.entity.StatusVoo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OperacaoVooRepository extends JpaRepository<OperacaoVoo, Integer> {

    @EntityGraph(attributePaths = {"drone", "piloto", "cliente", "empresaOperadora"})
    Optional<OperacaoVoo> findWithDetailsByCodigo(Integer codigo);

    // Aproveita o índice idx_status_voo para checagem de conflitos ativos do drone ou piloto
    @Query("""
            SELECT o FROM OperacaoVoo o
            WHERE o.statusVoo IN :statuses
              AND (UPPER(o.drone.codigo) = UPPER(:droneCodigo) OR o.piloto.cpf = :pilotoCpf)
              AND (:excludeCodigo IS NULL OR o.codigo <> :excludeCodigo)
            """)
    List<OperacaoVoo> findActiveForConflictCheck(
            @Param("statuses") Collection<StatusVoo> statuses,
            @Param("droneCodigo") String droneCodigo,
            @Param("pilotoCpf") String pilotoCpf,
            @Param("excludeCodigo") Integer excludeCodigo
    );

    @EntityGraph(attributePaths = {"drone", "piloto", "cliente", "empresaOperadora"})
    @Query("""
            SELECT o FROM OperacaoVoo o
            WHERE (:busca IS NULL OR :busca = ''
                   OR LOWER(o.objetivoVoo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.origem) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.destino) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.descricaoArea) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.drone.codigo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.piloto.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(o.cliente.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
              AND (:status IS NULL OR o.statusVoo = :status)
              AND (:empresaCnpj IS NULL OR :empresaCnpj = '' OR o.empresaOperadora.cnpj = :empresaCnpj)
              AND (:droneCodigo IS NULL OR :droneCodigo = '' OR UPPER(o.drone.codigo) = UPPER(:droneCodigo))
              AND (:pilotoCpf IS NULL OR :pilotoCpf = '' OR o.piloto.cpf = :pilotoCpf)
              AND (:clienteCpfRg IS NULL OR :clienteCpfRg = '' OR o.cliente.cpfRg = :clienteCpfRg)
              AND (:inicio IS NULL OR o.dataHoraPlanejada >= :inicio)
              AND (:fim IS NULL OR o.dataHoraPlanejada <= :fim)
            """)
    Page<OperacaoVoo> search(
            @Param("busca") String busca,
            @Param("status") StatusVoo status,
            @Param("empresaCnpj") String empresaCnpj,
            @Param("droneCodigo") String droneCodigo,
            @Param("pilotoCpf") String pilotoCpf,
            @Param("clienteCpfRg") String clienteCpfRg,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );
}
