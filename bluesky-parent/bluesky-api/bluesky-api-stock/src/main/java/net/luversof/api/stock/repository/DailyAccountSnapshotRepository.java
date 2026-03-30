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
        @Query("DELETE FROM daily_account_snapshot WHERE user_id = :userId AND date >= :fromDate")
        void deleteByUserIdAndDateGreaterThanEqual(@Param("userId") UUID userId, @Param("fromDate") LocalDate fromDate);
}
