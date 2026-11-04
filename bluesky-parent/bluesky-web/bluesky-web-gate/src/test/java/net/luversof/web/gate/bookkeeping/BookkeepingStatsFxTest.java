package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingStatsViewController.StatsPage;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthTotal;
import net.luversof.web.gate.bookkeeping.support.StatsView;

/** 통계 기준통화 환산: 분류별 · 추이를 달마다 그 달 환율로, 없는 통화 · 달은 빼고 알림. */
class BookkeepingStatsFxTest {

  static final YearMonth OCT = YearMonth.of(2026, 10);
  static final YearMonth NOV = YearMonth.of(2026, 11);

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void convertsCategoryTotalsAndTrendPerMonth() {
    var food = BookkeepingStatsScreenTest.FOOD.id();
    var missing = new TreeSet<String>();
    var totals =
        StatsView.convert(
            List.of(
                new CategoryTotal(food, CategoryKind.EXPENSE, "KRW", new BigDecimal("10000")),
                new CategoryTotal(food, CategoryKind.EXPENSE, "USD", new BigDecimal("10")),
                new CategoryTotal(food, CategoryKind.EXPENSE, "JPY", new BigDecimal("100"))),
            "KRW",
            NOV,
            Map.of("USD", new BigDecimal("1400")),
            missing);
    assertThat(totals)
        .extracting(t -> t.currency() + " " + t.amount().toPlainString())
        .containsExactly("KRW 10000", "KRW 14000");
    var slices =
        StatsView.slices(
            totals, BookkeepingStatsScreenTest.CATS, "KRW", CategoryKind.EXPENSE, null);
    assertThat(slices)
        .singleElement()
        .satisfies(s -> assertThat(s.amount()).isEqualByComparingTo("24000"));

    var trend =
        StatsView.convertTrend(
            List.of(
                new MonthTotal(OCT, "KRW", BigDecimal.ZERO, new BigDecimal("5000")),
                new MonthTotal(OCT, "USD", BigDecimal.ZERO, new BigDecimal("2")),
                new MonthTotal(NOV, "KRW", new BigDecimal("100"), new BigDecimal("1000")),
                new MonthTotal(NOV, "USD", BigDecimal.ZERO, BigDecimal.ZERO)), // 0 이면 알릴 것 없음
            "KRW",
            Map.of(NOV, Map.of("USD", new BigDecimal("1400"))),
            missing);
    assertThat(trend)
        .extracting(
            t -> t.month() + " " + t.income().toPlainString() + "/" + t.expense().toPlainString())
        .containsExactly("2026-10 0/5000", "2026-11 100/1000");
    assertThat(missing).containsExactly("JPY 2026-11", "USD 2026-10");
  }

  @Test
  void convertedPageUsesBaseCurrencyAndShowsMissing() {
    var page =
        new StatsPage(
            BookkeepingScreenTest.book(),
            new MonthRange(NOV, LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24)),
            StatsView.CONVERTED,
            List.of("KRW", "USD"),
            CategoryKind.EXPENSE,
            null,
            StatsView.slices(
                BookkeepingStatsScreenTest.TOTALS,
                BookkeepingStatsScreenTest.CATS,
                "KRW",
                CategoryKind.EXPENSE,
                null),
            List.of(new MonthTotal(NOV, "KRW", BigDecimal.ZERO, new BigDecimal("100000"))),
            null,
            List.of("USD 2026-10"));
    assertThat(page.displayCurrency()).isEqualTo("KRW");
    var html = BookkeepingScreenTest.render("bookkeeping/stats.jte", Map.of("page", page));
    assertThat(html).contains("<option value=\"FX\" selected>기준통화(KRW) 환산</option>");
    assertThat(html).contains("환율이 없어 빠짐: USD 2026-10");
    assertThat(html).contains("currency=FX");
    assertThat(html).contains("60,000"); // 금액은 기준통화 자릿수로
  }
}
