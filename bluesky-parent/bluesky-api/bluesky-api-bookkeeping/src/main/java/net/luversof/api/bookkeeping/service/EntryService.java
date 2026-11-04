package net.luversof.api.bookkeeping.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.CategoryRepository;
import net.luversof.api.bookkeeping.repository.EntryQuery;
import net.luversof.api.bookkeeping.repository.EntryRepository;
import net.luversof.api.bookkeeping.repository.RecurringDoneRepository;

/** 내역 검색 · 추가 · 수정 · 삭제. MEMBER 이상. 검증 규칙은 {@link EntryRules}. */
@Service
public class EntryService {

  // shortcut: 한 번에 최대 2000건, 화면이 페이지를 넘기게 되면 그때 페이지 단위로 바꾼다.
  static final int SEARCH_LIMIT = 2000;

  /** 검색 조건. from · to 필수(양 끝 포함), 나머지는 null 이면 조건 없음. */
  public record EntrySearch(
      LocalDate from, LocalDate to, String q, UUID categoryId, UUID assetId) {}

  private final EntryRepository entryRepository;
  private final EntryQuery entryQuery;
  private final AssetRepository assetRepository;
  private final CategoryRepository categoryRepository;
  private final CategoryService categoryService;
  private final RecurringDoneRepository recurringDoneRepository;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;
  private final MessageSource messageSource;

  public EntryService(
      EntryRepository entryRepository,
      EntryQuery entryQuery,
      AssetRepository assetRepository,
      CategoryRepository categoryRepository,
      CategoryService categoryService,
      RecurringDoneRepository recurringDoneRepository,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess,
      MessageSource messageSource) {
    this.entryRepository = entryRepository;
    this.entryQuery = entryQuery;
    this.assetRepository = assetRepository;
    this.categoryRepository = categoryRepository;
    this.categoryService = categoryService;
    this.recurringDoneRepository = recurringDoneRepository;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
    this.messageSource = messageSource;
  }

  public List<Entry> search(UUID bookId, UUID userId, EntrySearch s) {
    bookAccess.requireMember(bookId, userId);
    if (s.from() == null || s.to() == null || s.from().isAfter(s.to())) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var categoryIds =
        s.categoryId() == null ? null : categoryService.selfAndChildIds(bookId, s.categoryId());
    return entryQuery.search(
        bookId, s.from(), s.to(), s.q(), categoryIds, s.assetId(), SEARCH_LIMIT);
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Entry create(UUID bookId, UUID userId, EntryCommand command) {
    bookAccess.requireMember(bookId, userId);
    return jdbcAggregateOperations.insert(
        toEntry(bookId, uuidGenerator.create(), command, userId, Timestamps.now()));
  }

  /**
   * 한꺼번에 넣기(CSV 가져오기). 최대 2000 건, 하나라도 틀리면 아무것도 넣지 않는다 - IMPORT_ROW_INVALID 에 몇 번째(1 부터)와 그 줄의 사유를
   * 싣는다.
   */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public int importAll(UUID bookId, UUID userId, List<EntryCommand> commands) {
    bookAccess.requireMember(bookId, userId);
    if (commands == null || commands.isEmpty() || commands.size() > SEARCH_LIMIT) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var now = Timestamps.now();
    for (int i = 0; i < commands.size(); i++) {
      Entry entry;
      try {
        entry = toEntry(bookId, uuidGenerator.create(), commands.get(i), userId, now);
      } catch (BlueskyException e) {
        throw BookkeepingErrorCode.IMPORT_ROW_INVALID.exception(i + 1, reason(e));
      }
      jdbcAggregateOperations.insert(entry);
    }
    return commands.size();
  }

  /** 기록자 · 기록 시각은 처음 값을 유지한다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Entry update(UUID bookId, UUID userId, UUID entryId, EntryCommand command) {
    bookAccess.requireMember(bookId, userId);
    var entry = find(bookId, entryId);
    return entryRepository.save(
        toEntry(bookId, entry.id(), command, entry.createdBy(), entry.createdDate()));
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void delete(UUID bookId, UUID userId, UUID entryId) {
    bookAccess.requireMember(bookId, userId);
    var entry = find(bookId, entryId);
    recurringDoneRepository.deleteByEntryId(entry.id()); // 반복 회차였으면 다시 도래한다
    entryRepository.delete(entry);
  }

  private Entry toEntry(UUID bookId, UUID id, EntryCommand c, UUID createdBy, Instant createdDate) {
    var toAmount =
        EntryRules.check(
            c,
            asset(bookId, c.assetId()),
            asset(bookId, c.toAssetId()),
            category(bookId, c.categoryId()));
    return new Entry(
        id,
        bookId,
        c.type(),
        c.entryDate(),
        c.entryTime(),
        c.amount(),
        toAmount,
        c.assetId(),
        c.toAssetId(),
        c.categoryId(),
        blankToNull(c.content()),
        blankToNull(c.memo()),
        c.installmentMonths(),
        createdBy,
        createdDate);
  }

  private String reason(BlueskyException e) {
    return messageSource.getMessage(
        "BlueskyException." + e.getErrorCode(),
        e.getErrorMessageArgs(),
        e.getErrorCode(),
        LocaleContextHolder.getLocale());
  }

  private Entry find(UUID bookId, UUID entryId) {
    return entryRepository
        .findById(entryId)
        .filter(e -> e.bookId().equals(bookId))
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ENTRY::exception);
  }

  /** 다른 가계부의 자산은 없는 것으로 본다. */
  private Asset asset(UUID bookId, UUID assetId) {
    return assetId == null
        ? null
        : assetRepository.findById(assetId).filter(a -> a.bookId().equals(bookId)).orElse(null);
  }

  private Category category(UUID bookId, UUID categoryId) {
    return categoryId == null
        ? null
        : categoryRepository
            .findById(categoryId)
            .filter(c -> c.bookId().equals(bookId))
            .orElse(null);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
