package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import net.luversof.api.stock.domain.StockPrediction;

public interface StockPredictionRepository extends CrudRepository<StockPrediction, UUID> {

  List<StockPrediction> findByBaseDateGreaterThanEqual(LocalDate baseDate);

  /** 아직 채점하지 않은 예측. */
  @Query(
      """
      SELECT p.* FROM "StockPrediction" p
      WHERE NOT EXISTS (SELECT 1 FROM "StockPredictionGrade" g WHERE g."prediction_id" = p."id")
      """)
  List<StockPrediction> findUngraded();

  /** 채점 전 예측만 지운다 - 채점된 것은 적중률의 근거라 남긴다(관리 화면, 사용자 결정 2026-11-09). */
  @Modifying
  @Query(
      """
      DELETE FROM "StockPrediction" p
      WHERE p."baseDate" = :baseDate
        AND NOT EXISTS (SELECT 1 FROM "StockPredictionGrade" g WHERE g."prediction_id" = p."id")
      """)
  int deleteUngradedByBaseDate(@Param("baseDate") LocalDate baseDate);

  @Query(
      """
      SELECT COUNT(*) FROM "StockPrediction" p
      WHERE p."baseDate" = :baseDate
        AND EXISTS (SELECT 1 FROM "StockPredictionGrade" g WHERE g."prediction_id" = p."id")
      """)
  int countGradedByBaseDate(@Param("baseDate") LocalDate baseDate);
}
