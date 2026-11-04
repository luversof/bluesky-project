package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.CategoryKind;
import net.luversof.api.bookkeeping.domain.Budget;
import net.luversof.api.bookkeeping.repository.BookRepository;
import net.luversof.api.bookkeeping.repository.BudgetRepository;

/**
 * 월 예산: 전체 지출 하나 + 지출 대분류마다 하나, 기준통화 금액. 쓴 돈 계산은 화면이 월 합계로 한다. MEMBER 이상.
 *
 * <p>기준통화를 바꿔도 금액은 그대로 둔다(숫자만 새 통화로 읽힌다).
 */
@Service
public class BudgetService {

  /** amount 가 비었거나 0 이면 그 예산을 지운다. */
  public record BudgetCommand(UUID categoryId, BigDecimal amount) {}

  private final BudgetRepository budgetRepository;
  private final BookRepository bookRepository;
  private final CategoryService categoryService;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;

  public BudgetService(
      BudgetRepository budgetRepository,
      BookRepository bookRepository,
      CategoryService categoryService,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess) {
    this.budgetRepository = budgetRepository;
    this.bookRepository = bookRepository;
    this.categoryService = categoryService;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
  }

  public List<Budget> list(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return budgetRepository.findByBookId(bookId);
  }

  /** 넣기 · 바꾸기 · 지우기를 한 번에. 바뀐 뒤 목록을 돌려준다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public List<Budget> set(UUID bookId, UUID userId, BudgetCommand command) {
    bookAccess.requireMember(bookId, userId);
    var book =
        bookRepository.findById(bookId).orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOK::exception);
    var categoryId = command.categoryId();
    if (categoryId != null) {
      var category = categoryService.find(bookId, categoryId);
      if (category.parentId() != null) {
        throw BookkeepingErrorCode.CATEGORY_DEPTH_EXCEEDED.exception();
      }
      if (category.kind() != CategoryKind.EXPENSE) {
        throw BookkeepingErrorCode.CATEGORY_KIND_MISMATCH.exception();
      }
    }
    var existing =
        budgetRepository.findByBookId(bookId).stream()
            .filter(b -> Objects.equals(b.categoryId(), categoryId))
            .findFirst();
    var amount = command.amount();
    if (amount == null || amount.signum() == 0) {
      existing.ifPresent(budgetRepository::delete);
    } else {
      var checked = Money.positive(amount, book.baseCurrency());
      if (existing.isPresent()) {
        budgetRepository.save(new Budget(existing.get().id(), bookId, categoryId, checked));
      } else {
        jdbcAggregateOperations.insert(
            new Budget(uuidGenerator.create(), bookId, categoryId, checked));
      }
    }
    return budgetRepository.findByBookId(bookId);
  }
}
