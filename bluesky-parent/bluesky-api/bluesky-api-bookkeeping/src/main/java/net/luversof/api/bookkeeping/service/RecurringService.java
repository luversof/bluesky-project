package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.constant.RecurringCycle;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.BookRecurring;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.BookRecurringRepository;
import net.luversof.api.bookkeeping.repository.CategoryRepository;
import net.luversof.api.bookkeeping.repository.RecurringDoneRepository;

/**
 * 반복 내역: 틀 목록 · 추가 · 삭제, 기간 안의 아직 안 넣은 회차, 회차 넣기. MEMBER 이상.
 *
 * <p>자동으로 넣지 않는다. 넣은 회차는 RecurringDone 에 남고, 그 내역을 지우면 회차가 다시 보인다. 틀을 지워도 이미 넣은 내역은 남는다.
 */
@Service
public class RecurringService {

  public record RecurringCommand(
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo,
      RecurringCycle cycle,
      Integer dayOfMonth,
      Integer dayOfWeek,
      Integer monthOfYear,
      LocalDate startDate,
      LocalDate endDate) {}

  /** 아직 넣지 않은 회차 하나. */
  public record Due(BookRecurring recurring, LocalDate date) {}

  private final BookRecurringRepository bookRecurringRepository;
  private final RecurringDoneRepository recurringDoneRepository;
  private final AssetRepository assetRepository;
  private final CategoryRepository categoryRepository;
  private final EntryService entryService;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;

  public RecurringService(
      BookRecurringRepository bookRecurringRepository,
      RecurringDoneRepository recurringDoneRepository,
      AssetRepository assetRepository,
      CategoryRepository categoryRepository,
      EntryService entryService,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess) {
    this.bookRecurringRepository = bookRecurringRepository;
    this.recurringDoneRepository = recurringDoneRepository;
    this.assetRepository = assetRepository;
    this.categoryRepository = categoryRepository;
    this.entryService = entryService;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
  }

  public List<BookRecurring> list(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return bookRecurringRepository.findByBookIdOrderByCreatedDate(bookId);
  }

  /** 내역과 같은 규칙으로 검증한다(시작일을 날짜로 본다). */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public BookRecurring create(UUID bookId, UUID userId, RecurringCommand c) {
    bookAccess.requireMember(bookId, userId);
    if (c.startDate() == null || (c.endDate() != null && c.endDate().isBefore(c.startDate()))) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var cycle =
        RecurringRules.cycleColumns(c.cycle(), c.dayOfMonth(), c.dayOfWeek(), c.monthOfYear());
    var entry = entryCommand(c, c.startDate());
    var toAmount =
        EntryRules.check(
            entry,
            asset(bookId, c.assetId()),
            asset(bookId, c.toAssetId()),
            category(bookId, c.categoryId()));
    return jdbcAggregateOperations.insert(
        new BookRecurring(
            uuidGenerator.create(),
            bookId,
            c.type(),
            c.amount(),
            toAmount,
            c.assetId(),
            c.toAssetId(),
            c.categoryId(),
            blankToNull(c.content()),
            blankToNull(c.memo()),
            c.cycle(),
            cycle[0],
            cycle[1],
            cycle[2],
            c.startDate(),
            c.endDate(),
            userId,
            Timestamps.now()));
  }

  /** 틀만 지운다. 이미 넣은 내역은 남는다. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void delete(UUID bookId, UUID userId, UUID recurringId) {
    bookAccess.requireMember(bookId, userId);
    var r = find(bookId, recurringId);
    recurringDoneRepository.deleteByRecurringId(r.id());
    bookRecurringRepository.delete(r);
  }

  /** from ~ to(양 끝 포함, 최대 400 일) 안의 아직 안 넣은 회차, 날짜순. */
  public List<Due> due(UUID bookId, UUID userId, LocalDate from, LocalDate to) {
    bookAccess.requireMember(bookId, userId);
    if (from == null
        || to == null
        || from.isAfter(to)
        || ChronoUnit.DAYS.between(from, to) > RecurringRules.MAX_RANGE_DAYS) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var done = recurringDoneRepository.findKeys(bookId, from, to);
    var result = new ArrayList<Due>();
    for (var r : bookRecurringRepository.findByBookIdOrderByCreatedDate(bookId)) {
      for (var d : RecurringRules.occurrences(r, from, to)) {
        if (!done.contains(RecurringDoneRepository.key(r.id(), d))) {
          result.add(new Due(r, d));
        }
      }
    }
    result.sort(Comparator.comparing(Due::date));
    return result;
  }

  /** 그 회차를 내역으로 넣는다. 회차가 아니면 NOT_RECURRING_OCCURRENCE, 이미 넣었으면 RECURRING_ALREADY_DONE. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public Entry confirm(UUID bookId, UUID userId, UUID recurringId, LocalDate date) {
    bookAccess.requireMember(bookId, userId);
    var r = find(bookId, recurringId);
    if (date == null || !RecurringRules.isOccurrence(r, date)) {
      throw BookkeepingErrorCode.NOT_RECURRING_OCCURRENCE.exception();
    }
    if (recurringDoneRepository
        .findKeys(bookId, date, date)
        .contains(RecurringDoneRepository.key(r.id(), date))) {
      throw BookkeepingErrorCode.RECURRING_ALREADY_DONE.exception();
    }
    var entry =
        entryService.create(
            bookId,
            userId,
            entryCommand(
                new RecurringCommand(
                    r.type(),
                    r.amount(),
                    r.toAmount(),
                    r.assetId(),
                    r.toAssetId(),
                    r.categoryId(),
                    r.content(),
                    r.memo(),
                    r.cycle(),
                    null,
                    null,
                    null,
                    r.startDate(),
                    r.endDate()),
                date));
    try {
      recurringDoneRepository.insert(r.id(), date, entry.id());
    } catch (DuplicateKeyException e) {
      // 같은 회차를 동시에 두 번 - 트랜잭션이 되돌리며 방금 넣은 내역도 사라진다
      throw BookkeepingErrorCode.RECURRING_ALREADY_DONE.exception();
    }
    return entry;
  }

  private BookRecurring find(UUID bookId, UUID recurringId) {
    return bookRecurringRepository
        .findById(recurringId)
        .filter(r -> r.bookId().equals(bookId))
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_RECURRING::exception);
  }

  /** 다른 가계부의 자산 · 분류는 없는 것으로 본다(EntryRules 가 거절). */
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
            .filter(o -> o.bookId().equals(bookId))
            .orElse(null);
  }

  private static EntryCommand entryCommand(RecurringCommand c, LocalDate date) {
    return new EntryCommand(
        c.type(),
        date,
        null,
        c.amount(),
        c.toAmount(),
        c.assetId(),
        c.toAssetId(),
        c.categoryId(),
        c.content(),
        c.memo());
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
