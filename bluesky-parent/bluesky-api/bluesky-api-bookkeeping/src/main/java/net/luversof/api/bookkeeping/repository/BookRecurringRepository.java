package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.BookRecurring;

public interface BookRecurringRepository extends CrudRepository<BookRecurring, UUID> {

  List<BookRecurring> findByBookIdOrderByCreatedDate(UUID bookId);

  long deleteByBookId(UUID bookId);
}
