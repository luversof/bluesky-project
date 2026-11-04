package net.luversof.web.gate.bookkeeping.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.FxRateClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthTotal;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.FxView;
import net.luversof.web.gate.bookkeeping.support.StatsView;
import net.luversof.web.gate.bookkeeping.support.StatsView.Slice;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 통계 화면: 한 달의 분류별 비중(대분류 → 소분류)과 최근 12개월 수입 · 지출. 한 통화씩 본다. */
@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingStatsViewController {

  private static final Logger log = LoggerFactory.getLogger(BookkeepingStatsViewController.class);

  static final int TREND_MONTHS = 12;

  /** 통계 화면 한 장. parent 가 있으면 그 대분류를 펼친 상태. budgets 가 있으면 표에 예산 열. */
  public record StatsPage(
      Book book,
      MonthRange range,
      String currency,
      List<String> currencies,
      CategoryKind kind,
      Category parent,
      List<Slice> slices,
      List<MonthTotal> trend,
      StatsView.Budgets budgets,
      List<String> fxMissing) {

    /** 기준통화 환산 보기인가. */
    public boolean converted() {
      return StatsView.CONVERTED.equals(currency);
    }

    /** 금액을 적을 통화(환산 보기면 기준통화). */
    public String displayCurrency() {
      return converted() ? book.baseCurrency() : currency;
    }
  }

  private final BookClient bookClient;
  private final CategoryClient categoryClient;
  private final StatsClient statsClient;
  private final BudgetClient budgetClient;
  private final FxRateClient fxRateClient;

  public BookkeepingStatsViewController(
      BookClient bookClient,
      CategoryClient categoryClient,
      StatsClient statsClient,
      BudgetClient budgetClient,
      FxRateClient fxRateClient) {
    this.bookClient = bookClient;
    this.categoryClient = categoryClient;
    this.statsClient = statsClient;
    this.budgetClient = budgetClient;
    this.fxRateClient = fxRateClient;
  }

  @GetMapping("/{bookId}/stats")
  public String stats(
      @PathVariable UUID bookId,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String currency,
      @RequestParam(required = false) CategoryKind kind,
      @RequestParam(required = false) UUID parent,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    var userId = UserUtil.requireUserId();
    Book book;
    try {
      book = bookClient.get(bookId, userId);
    } catch (BlueskyException e) {
      return BookkeepingViewController.forbidden(e, response, model);
    }
    var parsedMonth = BookkeepingView.parseMonth(month);
    var monthParam = parsedMonth == null ? null : parsedMonth.toString();
    var stats = statsClient.month(bookId, userId, monthParam);
    var trendAll =
        statsClient.trend(bookId, userId, stats.range().month().toString(), TREND_MONTHS);
    var categories = categoryClient.list(bookId, userId);
    var currencies = StatsView.currencies(stats.categories(), trendAll);
    var converted = StatsView.CONVERTED.equals(currency) && currencies.size() > 1;
    var picked =
        converted
            ? StatsView.CONVERTED
            : StatsView.pickCurrency(currency, currencies, book.baseCurrency());
    var base = book.baseCurrency();
    var monthTotals = stats.categories();
    var trendTotals = StatsView.trend(trendAll, picked);
    var sliceCurrency = picked;
    List<String> fxMissing = null;
    if (converted) {
      var rates = rates(bookId, userId, trendAll, stats.range().month());
      var missing = new TreeSet<String>();
      monthTotals =
          StatsView.convert(
              monthTotals,
              base,
              stats.range().month(),
              rates.getOrDefault(stats.range().month(), Map.of()),
              missing);
      trendTotals = StatsView.convertTrend(trendAll, base, rates, missing);
      sliceCurrency = base;
      fxMissing = List.copyOf(missing);
    }
    var pickedKind = kind == null ? CategoryKind.EXPENSE : kind;
    var parentCategory =
        parent == null
            ? null
            : categories.stream()
                .filter(
                    c -> c.id().equals(parent) && c.parentId() == null && c.kind() == pickedKind)
                .findFirst()
                .orElse(null);
    model.addAttribute(
        "page",
        new StatsPage(
            book,
            stats.range(),
            picked,
            currencies,
            pickedKind,
            parentCategory,
            StatsView.slices(
                monthTotals,
                categories,
                sliceCurrency,
                pickedKind,
                parentCategory == null ? null : parentCategory.id()),
            trendTotals,
            pickedKind == CategoryKind.EXPENSE
                    && parentCategory == null
                    && sliceCurrency.equals(base)
                ? budgets(bookId, userId)
                : null,
            fxMissing));
    return "bookkeeping/stats";
  }

  /** 곁가지 - 예산이 없거나 못 불러오면 열을 빼고 그린다. */
  private StatsView.Budgets budgets(UUID bookId, UUID userId) {
    try {
      var list = budgetClient.list(bookId, userId);
      return list.isEmpty() ? null : StatsView.Budgets.of(list);
    } catch (RuntimeException e) {
      log.warn("bookkeeping budgets unavailable", e);
      return null;
    }
  }

  /** 추이에 나온 달마다 그 달 환율. 못 불러온 달은 빈 표(그 달 외화는 빠진다). */
  private Map<YearMonth, Map<String, BigDecimal>> rates(
      UUID bookId, UUID userId, List<MonthTotal> trend, YearMonth month) {
    var months = new TreeSet<YearMonth>();
    months.add(month);
    trend.forEach(t -> months.add(t.month()));
    var result = new HashMap<YearMonth, Map<String, BigDecimal>>();
    for (var m : months) {
      try {
        result.put(m, FxView.rates(fxRateClient.list(bookId, userId, m.toString())));
      } catch (RuntimeException e) {
        log.warn("bookkeeping fx rates unavailable: {}", m, e);
        result.put(m, Map.of());
      }
    }
    return result;
  }
}
