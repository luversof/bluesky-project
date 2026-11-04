package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Budget;

public interface BudgetRepository extends CrudRepository<Budget, UUID> {

  List<Budget> findByBookId(UUID bookId);

  long deleteByBookId(UUID bookId);

  long deleteByCategoryId(UUID categoryId);
}
