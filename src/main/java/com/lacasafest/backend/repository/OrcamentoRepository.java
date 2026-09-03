package com.lacasafest.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lacasafest.backend.entity.Orcamento;
import com.lacasafest.backend.entity.Orcamento.Status;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    /**
     * Verifica se já existe orçamento PENDENTE ou CONFIRMADO na mesma data.
     * CANCELADO não bloqueia a data.
     */
    boolean existsByDataEventoAndStatusIn(LocalDate dataEvento, List<Status> statuses);

    /**
     * Lista todas as datas que estão bloqueadas (pendentes ou confirmadas).
     * Útil para o front mostrar no calendário quais dias não estão disponíveis.
     */
    @Query("SELECT o.dataEvento FROM Orcamento o WHERE o.status IN :statuses")
    List<LocalDate> findDatasBloqueadas(@Param("statuses") List<Status> statuses);
}
