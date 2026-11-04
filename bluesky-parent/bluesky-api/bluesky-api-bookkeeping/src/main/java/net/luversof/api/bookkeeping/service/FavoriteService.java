package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.BookFavorite;
import net.luversof.api.bookkeeping.repository.BookFavoriteRepository;
import net.luversof.api.bookkeeping.repository.BookRepository;

/**
 * 즐겨찾기(내역 틀) 목록 · 추가 · 삭제. MEMBER 이상.
 *
 * <p>값은 비워 둘 수 있고, 있으면 내역과 같은 규칙으로 본다(같은 가계부의 자산 · 분류, 분류 종류, 통화 자릿수). 종류에 맞지 않는 칸은 버린다(이체면 분류, 수입
 * · 지출이면 받는 자산 · 받는 금액).
 */
@Service
public class FavoriteService {

  public record FavoriteCommand(
      String name,
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo) {}

  private final BookFavoriteRepository bookFavoriteRepository;
  private final BookRepository bookRepository;
  private final AssetService assetService;
  private final CategoryService categoryService;
  private final JdbcAggregateOperations jdbcAggregateOperations;
  private final UuidGenerator uuidGenerator;
  private final BookAccess bookAccess;

  public FavoriteService(
      BookFavoriteRepository bookFavoriteRepository,
      BookRepository bookRepository,
      AssetService assetService,
      CategoryService categoryService,
      JdbcAggregateOperations jdbcAggregateOperations,
      UuidGenerator uuidGenerator,
      BookAccess bookAccess) {
    this.bookFavoriteRepository = bookFavoriteRepository;
    this.bookRepository = bookRepository;
    this.assetService = assetService;
    this.categoryService = categoryService;
    this.jdbcAggregateOperations = jdbcAggregateOperations;
    this.uuidGenerator = uuidGenerator;
    this.bookAccess = bookAccess;
  }

  public List<BookFavorite> list(UUID bookId, UUID userId) {
    bookAccess.requireMember(bookId, userId);
    return bookFavoriteRepository.findByBookIdOrderByDisplayOrder(bookId);
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public BookFavorite create(UUID bookId, UUID userId, FavoriteCommand c) {
    bookAccess.requireMember(bookId, userId);
    if (c.type() == null
        || length(c.content()) > EntryRules.MAX_CONTENT
        || length(c.memo()) > EntryRules.MAX_MEMO) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    boolean transfer = c.type() == EntryType.TRANSFER;
    var asset = c.assetId() == null ? null : assetService.find(bookId, c.assetId());
    var toAssetId = transfer ? c.toAssetId() : null;
    var toAsset = toAssetId == null ? null : assetService.find(bookId, toAssetId);
    var categoryId = transfer ? null : c.categoryId();
    if (categoryId != null
        && categoryService.find(bookId, categoryId).kind() != c.type().categoryKind()) {
      throw BookkeepingErrorCode.CATEGORY_KIND_MISMATCH.exception();
    }
    var currency =
        asset != null
            ? asset.currency()
            : bookRepository
                .findById(bookId)
                .orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOK::exception)
                .baseCurrency();
    var amount = c.amount() == null ? null : Money.nonNegative(c.amount(), currency);
    var toAmount =
        !transfer || c.toAmount() == null
            ? null
            : Money.positive(c.toAmount(), toAsset != null ? toAsset.currency() : currency);
    int order =
        bookFavoriteRepository.findByBookIdOrderByDisplayOrder(bookId).stream()
                .mapToInt(BookFavorite::displayOrder)
                .max()
                .orElse(-1)
            + 1;
    return jdbcAggregateOperations.insert(
        new BookFavorite(
            uuidGenerator.create(),
            bookId,
            BookService.name(c.name()),
            c.type(),
            amount,
            toAmount,
            c.assetId(),
            toAssetId,
            categoryId,
            blankToNull(c.content()),
            blankToNull(c.memo()),
            order,
            userId,
            Timestamps.now()));
  }

  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public void delete(UUID bookId, UUID userId, UUID favoriteId) {
    bookAccess.requireMember(bookId, userId);
    var favorite =
        bookFavoriteRepository
            .findById(favoriteId)
            .filter(f -> f.bookId().equals(bookId))
            .orElseThrow(BookkeepingErrorCode.INVALID_REQUEST::exception);
    bookFavoriteRepository.delete(favorite);
  }

  private static int length(String s) {
    return s == null ? 0 : s.length();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
