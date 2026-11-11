package net.luversof.api.bookkeeping.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import net.luversof.api.bookkeeping.domain.BookFxRate;

/** 복합 PK(book_id, month, currency)라 JdbcClient 로 다룬다. */
@Repository
public class BookFxRateRepository {

  private final JdbcClient jdbcClient;

  public BookFxRateRepository(@Qualifier("bookkeepingJdbcClient") JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<BookFxRate> findByBookIdAndMonth(UUID bookId, String month) {
    return jdbcClient
        .sql(
            """
            SELECT "book_id", "month", "currency", "rate" FROM "BookFxRate"
             WHERE "book_id" = ? AND "month" = ? ORDER BY "currency"
            """)
        .params(bookId, month)
        .query(
            (rs, i) ->
                new BookFxRate(
                    rs.getObject("book_id", UUID.class),
                    rs.getString("month"),
                    rs.getString("currency"),
                    rs.getBigDecimal("rate")))
        .list();
  }

  public void upsert(UUID bookId, String month, String currency, BigDecimal rate) {
    jdbcClient
        .sql(
            """
            INSERT INTO "BookFxRate" ("book_id", "month", "currency", "rate") VALUES (?, ?, ?, ?)
            ON CONFLICT ("book_id", "month", "currency") DO UPDATE SET "rate" = EXCLUDED."rate"
            """)
        .params(bookId, month, currency, rate)
        .update();
  }

  public int delete(UUID bookId, String month, String currency) {
    return jdbcClient
        .sql(
            "DELETE FROM \"BookFxRate\" WHERE \"book_id\" = ? AND \"month\" = ? AND \"currency\" = ?")
        .params(bookId, month, currency)
        .update();
  }

  public int deleteByBookId(UUID bookId) {
    return jdbcClient
        .sql("DELETE FROM \"BookFxRate\" WHERE \"book_id\" = ?")
        .param(bookId)
        .update();
  }
}
