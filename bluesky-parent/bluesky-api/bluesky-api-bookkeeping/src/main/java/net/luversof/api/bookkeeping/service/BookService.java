package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.AssetKind;
import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.DefaultCategory;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Book;
import net.luversof.api.bookkeeping.domain.BookMember;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.BookFavoriteRepository;
import net.luversof.api.bookkeeping.repository.BookFxRateRepository;
import net.luversof.api.bookkeeping.repository.BookInviteRepository;
import net.luversof.api.bookkeeping.repository.BookMemberRepository;
import net.luversof.api.bookkeeping.repository.BookRecurringRepository;
import net.luversof.api.bookkeeping.repository.BookRepository;
import net.luversof.api.bookkeeping.repository.BudgetRepository;
import net.luversof.api.bookkeeping.repository.CategoryRepository;
import net.luversof.api.bookkeeping.repository.EntryRepository;
import net.luversof.api.bookkeeping.repository.RecurringDoneRepository;

/** 가계부 목록 · 만들기(기본 데이터 포함) · 수정 · 삭제. */
@Service
public class BookService {

  static final int MAX_NAME = 100;

  // 모듈이 ko_KR 만 지원한다(application.properties enabled-locales). 기본 데이터 이름은 한국어로 저장.
  private static final Locale DEFAULT_DATA_LOCALE = Locale.KOREA;

  /** 목록 한 줄: 가계부와 내 역할. */
  public record BookSummary(Book book, BookRole role) {}

  /** 만들기 · 수정 입력. monthStartDay 가 null 이면 1. */
  public record BookCommand(String name, String baseCurrency, Integer monthStartDay) {}

  private final BookRepository bookRepository;
  private final BookMemberRepository bookMemberRepository;
  private final AssetRepository assetRepository;
  private final CategoryRepository categoryRepository;
  private final EntryRepository entryRepository;
  private final BookInviteRepository bookInviteRepository;
  private final BudgetRepository budgetRepository;
  private final BookFavoriteRepository bookFavoriteRepository;
  private final BookFxRateRepository bookFxRateRepository;
  private final BookRecurringRepository bookRecurringRepository;
  private final RecurringDoneRepository recurringDoneRepository;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;
  private final MessageSource messageSource;

  public BookService(
      BookRepository bookRepository,
      BookMemberRepository bookMemberRepository,
      AssetRepository assetRepository,
      CategoryRepository categoryRepository,
      EntryRepository entryRepository,
      BookInviteRepository bookInviteRepository,
      BudgetRepository budgetRepository,
      BookFavoriteRepository bookFavoriteRepository,
      BookFxRateRepository bookFxRateRepository,
      BookRecurringRepository bookRecurringRepository,
      RecurringDoneRepository recurringDoneRepository,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess,
      MessageSource messageSource) {
    this.bookRepository = bookRepository;
    this.bookMemberRepository = bookMemberRepository;
    this.assetRepository = assetRepository;
    this.categoryRepository = categoryRepository;
    this.entryRepository = entryRepository;
    this.bookInviteRepository = bookInviteRepository;
    this.budgetRepository = budgetRepository;
    this.bookFavoriteRepository = bookFavoriteRepository;
    this.bookFxRateRepository = bookFxRateRepository;
    this.bookRecurringRepository = bookRecurringRepository;
    this.recurringDoneRepository = recurringDoneRepository;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
    this.messageSource = messageSource;
  }

  public List<BookSummary> list(UUID userId) {
    if (userId == null) {
      return List.of();
    }
    var roles =
        bookMemberRepository.findByUserId(userId).stream()
            .collect(Collectors.toMap(BookMember::bookId, BookMember::role));
    return bookRepository.findByMemberUserId(userId).stream()
        .map(book -> new BookSummary(book, roles.get(book.id())))
        .toList();
  }

  public Book get(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return findBook(bookId);
  }

  /** 가계부 + OWNER 멤버 + 기본 분류 12개 + "현금" 자산. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Book create(UUID userId, BookCommand command) {
    if (userId == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var now = Timestamps.now();
    var book =
        jdbcAggregateOperations.insert(
            new Book(
                uuidGenerator.create(),
                name(command.name()),
                Money.currency(command.baseCurrency()),
                monthStartDay(command.monthStartDay()),
                now));
    bookMemberRepository.insert(new BookMember(book.id(), userId, BookRole.OWNER, now));

    var categories = new ArrayList<Category>();
    var order = 0;
    for (var defaultCategory : DefaultCategory.values()) {
      categories.add(
          new Category(
              uuidGenerator.create(),
              book.id(),
              null,
              defaultCategory.getKind(),
              message(defaultCategory.messageCode(), defaultCategory.name()),
              order++,
              false));
    }
    jdbcAggregateOperations.insertAll(categories);
    jdbcAggregateOperations.insert(
        new Asset(
            uuidGenerator.create(),
            book.id(),
            AssetKind.CASH,
            message("bookkeeping.default.asset.CASH", "CASH"),
            book.baseCurrency(),
            BigDecimal.ZERO,
            null,
            0,
            false));
    return book;
  }

  /** OWNER 만. 기준통화는 언제든 바꿀 수 있다(1차는 표시용). */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Book update(UUID bookId, UUID userId, BookCommand command) {
    bookAccess.requireOwner(bookId, userId);
    var book = findBook(bookId);
    return bookRepository.save(
        new Book(
            book.id(),
            name(command.name()),
            Money.currency(command.baseCurrency()),
            monthStartDay(command.monthStartDay()),
            book.createdDate()));
  }

  /** OWNER 만. 가계부와 딸린 데이터를 모두 지운다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void delete(UUID bookId, UUID userId) {
    bookAccess.requireOwner(bookId, userId);
    entryRepository.deleteByBookId(bookId);
    budgetRepository.deleteByBookId(bookId);
    bookFavoriteRepository.deleteByBookId(bookId);
    bookFxRateRepository.deleteByBookId(bookId);
    recurringDoneRepository.deleteByBookId(bookId); // BookRecurring 보다 먼저(틀로 찾는다)
    bookRecurringRepository.deleteByBookId(bookId);
    categoryRepository.deleteByBookId(bookId);
    assetRepository.deleteByBookId(bookId);
    bookInviteRepository.deleteByBookId(bookId);
    bookMemberRepository.deleteByBookId(bookId);
    bookRepository.deleteById(bookId);
  }

  Book findBook(UUID bookId) {
    return bookRepository
        .findById(bookId)
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOK::exception);
  }

  static String name(String name) {
    var trimmed = name == null ? "" : name.strip();
    if (trimmed.isEmpty() || trimmed.length() > MAX_NAME) {
      throw BookkeepingErrorCode.INVALID_NAME.exception();
    }
    return trimmed;
  }

  private static int monthStartDay(Integer monthStartDay) {
    int day = monthStartDay == null ? 1 : monthStartDay;
    if (day < 1 || day > 28) {
      throw BookkeepingErrorCode.INVALID_MONTH_START_DAY.exception();
    }
    return day;
  }

  private String message(String code, String fallback) {
    return messageSource.getMessage(code, null, fallback, DEFAULT_DATA_LOCALE);
  }
}
