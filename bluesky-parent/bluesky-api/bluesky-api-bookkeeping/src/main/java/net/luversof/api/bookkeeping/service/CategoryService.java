package net.luversof.api.bookkeeping.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.CategoryKind;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.repository.BudgetRepository;
import net.luversof.api.bookkeeping.repository.CategoryRepository;
import net.luversof.api.bookkeeping.repository.EntryQuery;

/** 분류(대 · 소 2단계) 목록 · 추가 · 이름/숨김 수정 · 삭제(쓰이면 숨김) · 순서. MEMBER 이상. */
@Service
public class CategoryService {

  /** 추가 입력. parentId 가 있으면 소분류이고 kind 는 비워도 부모를 따른다. */
  public record CategoryCommand(UUID parentId, CategoryKind kind, String name, Boolean hidden) {}

  private final CategoryRepository categoryRepository;
  private final BudgetRepository budgetRepository;
  private final EntryQuery entryQuery;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;

  public CategoryService(
      CategoryRepository categoryRepository,
      BudgetRepository budgetRepository,
      EntryQuery entryQuery,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess) {
    this.categoryRepository = categoryRepository;
    this.budgetRepository = budgetRepository;
    this.entryQuery = entryQuery;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
  }

  /** 숨긴 분류도 포함한다(화면이 거른다). */
  public List<Category> list(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return categoryRepository.findByBookIdOrderByDisplayOrder(bookId);
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Category create(UUID bookId, UUID userId, CategoryCommand command) {
    bookAccess.requireMember(bookId, userId);
    var kind = command.kind();
    if (command.parentId() != null) {
      var parent = find(bookId, command.parentId());
      if (parent.parentId() != null) {
        throw BookkeepingErrorCode.CATEGORY_DEPTH_EXCEEDED.exception();
      }
      if (kind != null && kind != parent.kind()) {
        throw BookkeepingErrorCode.CATEGORY_KIND_MISMATCH.exception();
      }
      kind = parent.kind();
    }
    if (kind == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var siblingKind = kind;
    var order =
        categoryRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
                .filter(c -> sameGroup(c, command.parentId(), siblingKind))
                .mapToInt(Category::displayOrder)
                .max()
                .orElse(-1)
            + 1;
    return jdbcAggregateOperations.insert(
        new Category(
            uuidGenerator.create(),
            bookId,
            command.parentId(),
            kind,
            BookService.name(command.name()),
            order,
            Boolean.TRUE.equals(command.hidden())));
  }

  /** 이름 · 숨김만 바꾼다(종류 · 부모는 고정). */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Category update(UUID bookId, UUID userId, UUID categoryId, CategoryCommand command) {
    bookAccess.requireMember(bookId, userId);
    var c = find(bookId, categoryId);
    return categoryRepository.save(
        new Category(
            c.id(),
            bookId,
            c.parentId(),
            c.kind(),
            BookService.name(command.name()),
            c.displayOrder(),
            command.hidden() == null ? c.hidden() : command.hidden()));
  }

  /** 내역이 없고 소분류도 없으면 지우고 true. 아니면 자신과 소분류를 숨기고 false. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public boolean delete(UUID bookId, UUID userId, UUID categoryId) {
    bookAccess.requireMember(bookId, userId);
    var c = find(bookId, categoryId);
    var targets = new ArrayList<Category>();
    targets.add(c);
    categoryRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
        .filter(child -> categoryId.equals(child.parentId()))
        .forEach(targets::add);
    if (targets.size() == 1 && !entryQuery.existsByCategoryIdIn(List.of(categoryId))) {
      budgetRepository.deleteByCategoryId(categoryId);
      categoryRepository.delete(c);
      return true;
    }
    targets.stream()
        .filter(t -> !t.hidden())
        .forEach(t -> categoryRepository.save(withOrder(t, t.displayOrder(), true)));
    return false;
  }

  /** 같은 부모 · 같은 종류 안에서 한 칸 위(up) / 아래로. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void move(UUID bookId, UUID userId, UUID categoryId, boolean up) {
    bookAccess.requireMember(bookId, userId);
    var c = find(bookId, categoryId);
    var group =
        new ArrayList<>(
            categoryRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
                .filter(o -> sameGroup(o, c.parentId(), c.kind()))
                .toList());
    if (!Ordering.swap(group, Category::id, categoryId, up)) {
      return;
    }
    for (int i = 0; i < group.size(); i++) {
      var o = group.get(i);
      if (o.displayOrder() != i) {
        categoryRepository.save(withOrder(o, i, o.hidden()));
      }
    }
  }

  /** 이 분류와 소분류 id(검색 · 통계에서 대분류를 고르면 소분류까지). */
  public List<UUID> selfAndChildIds(UUID bookId, UUID categoryId) {
    var ids = new ArrayList<UUID>();
    ids.add(find(bookId, categoryId).id());
    categoryRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
        .filter(o -> categoryId.equals(o.parentId()))
        .forEach(o -> ids.add(o.id()));
    return ids;
  }

  Category find(UUID bookId, UUID categoryId) {
    return categoryRepository
        .findById(categoryId)
        .filter(o -> o.bookId().equals(bookId))
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_CATEGORY::exception);
  }

  private static boolean sameGroup(Category c, UUID parentId, CategoryKind kind) {
    return Objects.equals(c.parentId(), parentId) && c.kind() == kind;
  }

  private static Category withOrder(Category c, int order, boolean hidden) {
    return new Category(c.id(), c.bookId(), c.parentId(), c.kind(), c.name(), order, hidden);
  }
}
