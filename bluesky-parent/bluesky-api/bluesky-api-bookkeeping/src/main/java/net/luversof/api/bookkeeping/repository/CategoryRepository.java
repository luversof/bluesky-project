package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Category;

public interface CategoryRepository extends CrudRepository<Category, UUID> {

  List<Category> findByBookIdOrderByDisplayOrder(UUID bookId);

  long deleteByBookId(UUID bookId);
}
