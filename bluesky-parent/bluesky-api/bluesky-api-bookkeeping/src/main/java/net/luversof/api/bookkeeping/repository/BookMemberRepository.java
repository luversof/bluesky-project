package net.luversof.api.bookkeeping.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.domain.BookMember;

/** 복합 PK(book_id, user_id)라 data-jdbc repository 대신 JdbcClient 로 다룬다. */
@Repository
public class BookMemberRepository {

  private static final String SELECT =
      """
      SELECT "book_id", "user_id", "role", "joinedDate" FROM "BookMember"
      """;

  private final JdbcClient jdbcClient;

  public BookMemberRepository(@Qualifier("bookkeepingJdbcClient") JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Optional<BookMember> findByBookIdAndUserId(UUID bookId, UUID userId) {
    return jdbcClient
        .sql(SELECT + " WHERE \"book_id\" = ? AND \"user_id\" = ?")
        .params(bookId, userId)
        .query(BookMemberRepository::map)
        .optional();
  }

  /** OWNER 먼저, 그다음 들어온 순. */
  public List<BookMember> findByBookId(UUID bookId) {
    return jdbcClient
        .sql(SELECT + " WHERE \"book_id\" = ? ORDER BY \"role\" = 'OWNER' DESC, \"joinedDate\"")
        .param(bookId)
        .query(BookMemberRepository::map)
        .list();
  }

  public List<BookMember> findByUserId(UUID userId) {
    return jdbcClient
        .sql(SELECT + " WHERE \"user_id\" = ?")
        .param(userId)
        .query(BookMemberRepository::map)
        .list();
  }

  public void insert(BookMember member) {
    jdbcClient
        .sql(
            """
            INSERT INTO "BookMember" ("book_id", "user_id", "role", "joinedDate") VALUES (?, ?, ?, ?)
            """)
        .params(
            member.bookId(),
            member.userId(),
            member.role().name(),
            Timestamp.from(member.joinedDate()))
        .update();
  }

  /** 지운 행 수(0 또는 1). */
  public int delete(UUID bookId, UUID userId) {
    return jdbcClient
        .sql("DELETE FROM \"BookMember\" WHERE \"book_id\" = ? AND \"user_id\" = ?")
        .params(bookId, userId)
        .update();
  }

  public int deleteByBookId(UUID bookId) {
    return jdbcClient
        .sql("DELETE FROM \"BookMember\" WHERE \"book_id\" = ?")
        .param(bookId)
        .update();
  }

  private static BookMember map(ResultSet rs, int rowNum) throws SQLException {
    return new BookMember(
        rs.getObject("book_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        BookRole.valueOf(rs.getString("role")),
        rs.getTimestamp("joinedDate").toInstant());
  }
}
