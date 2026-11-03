package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.CategoryTotal;
import net.luversof.api.bookkeeping.domain.DailyTotal;
import net.luversof.api.bookkeeping.repository.BookRepository;
import net.luversof.api.bookkeeping.repository.EntryQuery;

/** 월 화면 · 통계 화면 합계. 통화별로 따로 내고 환산하지 않는다(1차). MEMBER 이상. */
@Service
public class StatsService {

  static final int MAX_TREND_MONTHS = 36;

  /** 한 달: 기간, 날짜별 합(달력), 분류별 합(원 그래프). */
  public record MonthStats(
      MonthRange range, List<DailyTotal> daily, List<CategoryTotal> categories) {}

  /** 월 · 통화별 수입 · 지출 합(막대 그래프). */
  public record MonthTotal(
      YearMonth month, String currency, BigDecimal income, BigDecimal expense) {}

  private final BookRepository bookRepository;
  private final EntryQuery entryQuery;
  private final BookAccess bookAccess;

  public StatsService(BookRepository bookRepository, EntryQuery entryQuery, BookAccess bookAccess) {
    this.bookRepository = bookRepository;
    this.entryQuery = entryQuery;
    this.bookAccess = bookAccess;
  }

  /** month 가 null 이면 오늘이 속한 가계부 월. */
  public MonthStats month(UUID bookId, UUID userId, YearMonth month) {
    bookAccess.requireMember(bookId, userId);
    var range = range(bookId, month);
    return new MonthStats(
        range,
        entryQuery.dailyTotals(bookId, range.from(), range.to()),
        entryQuery.categoryTotals(bookId, range.from(), range.to()));
  }

  /** end 까지 months 개월(1~36). 내역이 있는 통화마다 모든 달을 채운다(없는 달은 0). */
  public List<MonthTotal> trend(UUID bookId, UUID userId, YearMonth end, int months) {
    bookAccess.requireMember(bookId, userId);
    if (months < 1 || months > MAX_TREND_MONTHS) {
      throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    }
    int startDay = startDay(bookId);
    var last = range(startDay, end);
    var first = MonthRange.of(last.month().minusMonths(months - 1L), startDay);

    var sums = new HashMap<String, BigDecimal[]>(); // 값: [0] 수입, [1] 지출
    var currencies = new TreeSet<String>();
    for (var d : entryQuery.dailyTotals(bookId, first.from(), last.to())) {
      currencies.add(d.currency());
      var sum =
          sums.computeIfAbsent(
              key(MonthRange.monthOf(d.date(), startDay), d.currency()),
              k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      sum[0] = sum[0].add(d.income());
      sum[1] = sum[1].add(d.expense());
    }
    var result = new ArrayList<MonthTotal>();
    for (var m = first.month(); !m.isAfter(last.month()); m = m.plusMonths(1)) {
      for (var currency : currencies) {
        var sum = sums.get(key(m, currency));
        result.add(
            sum == null
                ? new MonthTotal(m, currency, BigDecimal.ZERO, BigDecimal.ZERO)
                : new MonthTotal(m, currency, sum[0], sum[1]));
      }
    }
    return result;
  }

  private MonthRange range(UUID bookId, YearMonth month) {
    return range(startDay(bookId), month);
  }

  private static MonthRange range(int startDay, YearMonth month) {
    var m = month == null ? MonthRange.monthOf(LocalDate.now(), startDay) : month;
    return MonthRange.of(m, startDay);
  }

  private int startDay(UUID bookId) {
    return bookRepository
        .findById(bookId)
        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOK::exception)
        .monthStartDay();
  }

  private static String key(YearMonth month, String currency) {
    return month + "|" + currency;
  }
}
