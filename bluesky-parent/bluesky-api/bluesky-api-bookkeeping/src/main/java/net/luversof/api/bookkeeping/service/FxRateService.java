package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.BookFxRate;
import net.luversof.api.bookkeeping.repository.BookFxRateRepository;
import net.luversof.api.bookkeeping.repository.BookRepository;

/**
 * 월 환율(손으로 넣는다): 외화 1 단위 = 기준통화 얼마. 화면이 월 합계 · 예산을 기준통화로 환산할 때 쓴다. MEMBER 이상.
 *
 * <p>기준통화 자신의 환율은 받지 않는다(늘 1). 기준통화를 바꿔도 넣어 둔 환율은 그대로 남는다(새 기준통화로 읽힌다).
 */
@Service
public class FxRateService {

  /** rate 가 비었거나 0 이면 그 환율을 지운다. */
  public record FxRateCommand(YearMonth month, String currency, BigDecimal rate) {}

  private final BookFxRateRepository bookFxRateRepository;
  private final BookRepository bookRepository;
  private final BookAccess bookAccess;

  public FxRateService(
      BookFxRateRepository bookFxRateRepository,
      BookRepository bookRepository,
      BookAccess bookAccess) {
    this.bookFxRateRepository = bookFxRateRepository;
    this.bookRepository = bookRepository;
    this.bookAccess = bookAccess;
  }

  public List<BookFxRate> list(UUID bookId, UUID userId, YearMonth month) {
    bookAccess.requireMember(bookId, userId);
    if (month == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    return bookFxRateRepository.findByBookIdAndMonth(bookId, month.toString());
  }

  /** 넣기 · 바꾸기 · 지우기. 바뀐 뒤 그 달 목록. */
  @Transactional(transactionManager = "bookkeepingTransactionManager")
  public List<BookFxRate> set(UUID bookId, UUID userId, FxRateCommand c) {
    bookAccess.requireMember(bookId, userId);
    if (c.month() == null) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    var currency = Money.currency(c.currency());
    var base =
        bookRepository
            .findById(bookId)
            .orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOK::exception)
            .baseCurrency();
    if (currency.equals(base)) {
      throw BookkeepingErrorCode.INVALID_CURRENCY.exception();
    }
    var month = c.month().toString();
    if (c.rate() == null || c.rate().signum() == 0) {
      bookFxRateRepository.delete(bookId, month, currency);
    } else {
      bookFxRateRepository.upsert(bookId, month, currency, Money.rate(c.rate()));
    }
    return bookFxRateRepository.findByBookIdAndMonth(bookId, month);
  }
}
