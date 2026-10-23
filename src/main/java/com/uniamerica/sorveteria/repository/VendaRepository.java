
package com.uniamerica.sorveteria.repository;

import com.uniamerica.sorveteria.entity.Produto;
import com.uniamerica.sorveteria.entity.StatusVenda;
import com.uniamerica.sorveteria.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

  List<Venda> findDistinctByItens_Produto(Produto produto);

  List<Venda> findByStatus(StatusVenda status);

  @Query("""
        SELECT COALESCE(SUM(v.valorTotal), 0.0)
        FROM Venda v
        WHERE v.status = :status
          AND v.dataHora >= :inicio
          AND v.dataHora < :fim
        """)
  Double somarVendasPorPeriodo(
    @Param("status") StatusVenda status,
    @Param("inicio") LocalDateTime inicio,
    @Param("fim") LocalDateTime fim
  );

  @Query("""
        SELECT COUNT(v)
        FROM Venda v
        WHERE v.status = :status
          AND v.dataHora >= :inicio
          AND v.dataHora < :fim
        """)
  Long contarVendasPorPeriodo(
    @Param("status") StatusVenda status,
    @Param("inicio") LocalDateTime inicio,
    @Param("fim") LocalDateTime fim
  );

  @Query("""
        SELECT CAST(v.dataHora AS date), SUM(v.valorTotal), COUNT(v)
        FROM Venda v
        WHERE v.status = :status
          AND v.dataHora >= :inicio
          AND v.dataHora < :fim
        GROUP BY CAST(v.dataHora AS date)
        ORDER BY CAST(v.dataHora AS date)
        """)
  List<Object[]> buscarVendasAgrupadasPorDataHora(
    @Param("status") StatusVenda status,
    @Param("inicio") LocalDateTime inicio,
    @Param("fim") LocalDateTime fim
  );
}
