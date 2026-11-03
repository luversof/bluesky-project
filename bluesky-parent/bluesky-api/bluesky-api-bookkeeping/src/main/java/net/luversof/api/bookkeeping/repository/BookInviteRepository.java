package net.luversof.api.bookkeeping.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.BookInvite;

public interface BookInviteRepository extends CrudRepository<BookInvite, String> {

  long deleteByBookId(UUID bookId);

  /** 아직 안 썼고 만료 전일 때만 사용 처리한다. 1 이면 이 요청이 차지했다(동시 수락 방지). */
  @Modifying
  @Query(
      """
      UPDATE "BookInvite" SET "usedBy" = :userId, "usedDate" = :now
       WHERE "token" = :token AND "usedBy" IS NULL AND "expiresAt" > :now
      """)
  int markUsed(String token, UUID userId, Instant now);
}
