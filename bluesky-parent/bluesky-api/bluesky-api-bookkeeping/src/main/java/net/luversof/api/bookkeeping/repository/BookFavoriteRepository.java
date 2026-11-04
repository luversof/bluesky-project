package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.BookFavorite;

public interface BookFavoriteRepository extends CrudRepository<BookFavorite, UUID> {

  List<BookFavorite> findByBookIdOrderByDisplayOrder(UUID bookId);

  long deleteByBookId(UUID bookId);
}
