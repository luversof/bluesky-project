package net.luversof.api.bookkeeping.repository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 반복 틀에서 넣은 회차(PK recurring_id + occurrenceDate). 복합 PK 라 JdbcClient 로 다룬다. */
@Repository
public class RecurringDoneRepository {

  private final JdbcClient jdbcClient;

  public RecurringDoneRepository(@Qualifier("bookkeepingJdbcClient") JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /** 이 가계부에서 기간 안에 넣은 회차. 값은 key(recurringId, date). */
  public Set<String> findKeys(UUID bookId, LocalDate from, LocalDate to) {
    return new HashSet<>(
        jdbcClient
            .sql(
                """
                SELECT d."recurring_id", d."occurrenceDate" FROM "RecurringDone" d
                JOIN "BookRecurring" r ON r."id" = d."recurring_id"
                WHERE r."book_id" = ? AND d."occurrenceDate" BETWEEN ? AND ?
                """)
            .params(bookId, from, to)
            .query((rs, i) -> key(rs.getObject(1, UUID.class), rs.getObject(2, LocalDate.class)))
            .list());
  }

  /** 이미 있으면(같은 회차를 동시에 두 번) DuplicateKeyException. */
  public void insert(UUID recurringId, LocalDate date, UUID entryId) {
    jdbcClient
        .sql(
            """
            INSERT INTO "RecurringDone" ("recurring_id", "occurrenceDate", "entry_id")
            VALUES (?, ?, ?)
            """)
        .params(recurringId, date, entryId)
        .update();
  }

  /** 그 내역을 지우면 회차가 다시 도래한다. */
  public int deleteByEntryId(UUID entryId) {
    return jdbcClient
        .sql("DELETE FROM \"RecurringDone\" WHERE \"entry_id\" = ?")
        .param(entryId)
        .update();
  }

  public int deleteByRecurringId(UUID recurringId) {
    return jdbcClient
        .sql("DELETE FROM \"RecurringDone\" WHERE \"recurring_id\" = ?")
        .param(recurringId)
        .update();
  }

  /** BookRecurring 을 지우기 전에 부른다. */
  public int deleteByBookId(UUID bookId) {
    return jdbcClient
        .sql(
            """
            DELETE FROM "RecurringDone" WHERE "recurring_id" IN
            (SELECT "id" FROM "BookRecurring" WHERE "book_id" = ?)
            """)
        .param(bookId)
        .update();
  }

  public static String key(UUID recurringId, LocalDate date) {
    return recurringId + "|" + date;
  }
}
