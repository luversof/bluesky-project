package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import net.luversof.api.stock.domain.DailyAccountSnapshot;

public interface DailyAccountSnapshotRepository extends CrudRepository<DailyAccountSnapshot, UUID> {

  @Modifying
  @Query(
      "DELETE FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"date\" >= :fromDate")
  void deleteByUserIdAndDateGreaterThanEqual(
      @Param("userId") UUID userId, @Param("fromDate") LocalDate fromDate);

  /**
   * 수정주가 재조정으로 인해 특정 stockItemId가 포함된 모든 스냅샷을 무효화합니다. wmaState JSON 컬럼에서 해당 stockItemId를 포함한 스냅샷을
   * 삭제합니다.
   */
  @Modifying
  @Query(
      "DELETE FROM \"DailyAccountSnapshot\" WHERE \"wmaState\" IS NOT NULL AND \"wmaState\"::text LIKE CONCAT('%', :stockItemId, '%')")
  void deleteByWmaStateContainingStockItemId(@Param("stockItemId") String stockItemId);

  @Query(
      "SELECT * FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"date\" >= :startDate AND \"date\" <= :endDate ORDER BY \"date\" ASC")
  java.util.List<DailyAccountSnapshot> findByUserIdAndDateBetween(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT * FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"account_id\" IS NULL AND \"date\" < :date ORDER BY \"date\" DESC LIMIT 1")
  net.luversof.api.stock.domain.DailyAccountSnapshot findTopByUserIdAndDateLessThanOrderByDateDesc(
      @Param("userId") UUID userId, @Param("date") LocalDate date);

  @Query(
      "SELECT * FROM \"DailyAccountSnapshot\" WHERE \"account_id\" = :accountId AND \"date\" < :date ORDER BY \"date\" DESC LIMIT 1")
  net.luversof.api.stock.domain.DailyAccountSnapshot
      findTopByAccountIdAndDateLessThanOrderByDateDesc(
          @Param("accountId") UUID accountId, @Param("date") LocalDate date);

  @Query(
      "SELECT COUNT(1) > 0 FROM \"DailyAccountSnapshot\" WHERE \"account_id\" = :accountId AND \"date\" = :date")
  boolean existsByAccountIdAndDate(
      @Param("accountId") UUID accountId, @Param("date") LocalDate date);

  @Query(
      "SELECT COUNT(1) > 0 FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"account_id\" IS NULL AND \"date\" = :date")
  boolean existsByUserIdAndAccountIdIsNullAndDate(
      @Param("userId") UUID userId, @Param("date") LocalDate date);

  @Query(
      "SELECT \"date\" FROM \"DailyAccountSnapshot\" WHERE \"account_id\" = :accountId AND \"date\" >= :startDate AND \"date\" <= :endDate")
  java.util.List<LocalDate> findDatesByAccountIdAndDateBetween(
      @Param("accountId") UUID accountId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT \"date\" FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"account_id\" IS NULL AND \"date\" >= :startDate AND \"date\" <= :endDate")
  java.util.List<LocalDate> findDatesByUserIdAndAccountIdIsNullAndDateBetween(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT * FROM \"DailyAccountSnapshot\" WHERE \"user_id\" = :userId AND \"account_id\" IS NULL AND \"date\" <= :date ORDER BY \"date\" DESC LIMIT 1")
  java.util.Optional<DailyAccountSnapshot> findLatestByUserIdAndAccountIdIsNullOnOrBefore(
      @Param("userId") UUID userId, @Param("date") LocalDate date);
}
