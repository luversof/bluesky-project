package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Book;

public interface BookRepository extends CrudRepository<Book, UUID> {

  /** 이 사용자가 멤버인 가계부(만든 순). */
  @Query(
      """
      SELECT b.* FROM "Book" b JOIN "BookMember" m ON m."book_id" = b."id"
       WHERE m."user_id" = :userId ORDER BY b."createdDate"
      """)
  List<Book> findByMemberUserId(UUID userId);
}
