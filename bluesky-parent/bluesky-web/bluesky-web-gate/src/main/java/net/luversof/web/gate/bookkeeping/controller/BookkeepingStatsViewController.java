package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

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
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthTotal;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.StatsView;
import net.luversof.web.gate.bookkeeping.support.StatsView.Slice;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 통계 화면: 한 달의 분류별 비중(대분류 → 소분류)과 최근 12개월 수입 · 지출. 한 통화씩 본다. */
@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingStatsViewController {

  static final int TREND_MONTHS = 12;

  /** 통계 화면 한 장. parent 가 있으면 그 대분류를 펼친 상태. */
  public record StatsPage(
      Book book,
      MonthRange range,
      String currency,
      List<String> currencies,
      CategoryKind kind,
      Category parent,
      List<Slice> slices,
      List<MonthTotal> trend) {}

  private final BookClient bookClient;
  private final CategoryClient categoryClient;
  private final StatsClient statsClient;

  public BookkeepingStatsViewController(
      BookClient bookClient, CategoryClient categoryClient, StatsClient statsClient) {
    this.bookClient = bookClient;
    this.categoryClient = categoryClient;
    this.statsClient = statsClient;
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
    var picked = StatsView.pickCurrency(currency, currencies, book.baseCurrency());
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
                stats.categories(),
                categories,
                picked,
                pickedKind,
                parentCategory == null ? null : parentCategory.id()),
            StatsView.trend(trendAll, picked)));
    return "bookkeeping/stats";
  }
}
