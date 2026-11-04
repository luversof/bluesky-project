package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingStatsViewController.StatsPage;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthTotal;
import net.luversof.web.gate.bookkeeping.support.StatsView;

/** 통계: 대분류로 묶기 · 펼치기 · 통화 고르기 · 차트 JSON · 렌더. */
class BookkeepingStatsScreenTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;
  static final Category FOOD =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.EXPENSE, "식비", 0, false);
  static final Category COFFEE =
      new Category(UUID.randomUUID(), BOOK, FOOD.id(), CategoryKind.EXPENSE, "커피", 0, false);
  static final Category TAXI =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.EXPENSE, "교통 \"택시\"", 1, false);
  static final Category SALARY =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.INCOME, "급여", 0, false);
  static final List<Category> CATS = List.of(FOOD, COFFEE, TAXI, SALARY);
  static final List<CategoryTotal> TOTALS =
      List.of(
          total(FOOD, "KRW", "30000"),
          total(COFFEE, "KRW", "10000"),
          total(TAXI, "KRW", "60000"),
          total(SALARY, "KRW", "3000000"),
          total(COFFEE, "USD", "3.5"));

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void rollsChildrenUpToParentAndSortsBySize() {
    var slices = StatsView.slices(TOTALS, CATS, "KRW", CategoryKind.EXPENSE, null);
    assertThat(slices)
        .extracting(
            s -> s.name() + " " + s.amount().toPlainString() + " " + s.percent().toPlainString())
        .containsExactly("교통 \"택시\" 60000 60.0", "식비 40000 40.0");
    assertThat(slices.get(1).drillParentId()).isEqualTo(FOOD.id()); // 소분류가 있어 펼칠 수 있다
    assertThat(slices.get(0).drillParentId()).isNull();
  }

  @Test
  void drillDownShowsChildrenAndParentOwnShare() {
    var slices = StatsView.slices(TOTALS, CATS, "KRW", CategoryKind.EXPENSE, FOOD.id());
    assertThat(slices)
        .extracting(
            s -> s.name() + (s.ownOfParent() ? "(own)" : "") + " " + s.amount().toPlainString())
        .containsExactly("식비 (직접)(own) 30000", "커피 10000");
    assertThat(StatsView.slices(TOTALS, CATS, "USD", CategoryKind.EXPENSE, null))
        .extracting(s -> s.name() + " " + s.amount().toPlainString())
        .containsExactly("식비 3.5");
  }

  @Test
  void currencyPickPrefersRequestedThenBase() {
    var currencies = StatsView.currencies(TOTALS, List.of());
    assertThat(currencies).containsExactly("KRW", "USD");
    assertThat(StatsView.pickCurrency("USD", currencies, "KRW")).isEqualTo("USD");
    assertThat(StatsView.pickCurrency("EUR", currencies, "KRW")).isEqualTo("KRW");
    assertThat(StatsView.pickCurrency(null, List.of("USD"), "KRW")).isEqualTo("USD");
    assertThat(StatsView.pickCurrency(null, List.of(), "KRW")).isEqualTo("KRW");
  }

  @Test
  void chartJsonEscapesNames() {
    var json =
        StatsView.breakdownJson(StatsView.slices(TOTALS, CATS, "KRW", CategoryKind.EXPENSE, null));
    assertThat(json).isEqualTo("{\"labels\":[\"교통 \\\"택시\\\"\",\"식비\"],\"values\":[60000,40000]}");
  }

  @Test
  void statsPageRenders() {
    var trend =
        List.of(
            new MonthTotal(YearMonth.of(2026, 10), "KRW", BigDecimal.ZERO, new BigDecimal("5000")),
            new MonthTotal(
                YearMonth.of(2026, 11),
                "KRW",
                new BigDecimal("3000000"),
                new BigDecimal("100000")));
    var page =
        new StatsPage(
            BookkeepingScreenTest.book(),
            new MonthRange(
                YearMonth.of(2026, 11), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24)),
            "KRW",
            List.of("KRW", "USD"),
            CategoryKind.EXPENSE,
            null,
            StatsView.slices(TOTALS, CATS, "KRW", CategoryKind.EXPENSE, null),
            trend,
            StatsView.Budgets.of(
                List.of(
                    new net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget(
                        UUID.randomUUID(),
                        BookkeepingScreenTest.BOOK,
                        null,
                        new BigDecimal("200000")),
                    new net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget(
                        UUID.randomUUID(),
                        BookkeepingScreenTest.BOOK,
                        FOOD.id(),
                        new BigDecimal("50000")))),
            null);
    var html = BookkeepingScreenTest.render("bookkeeping/stats.jte", Map.of("page", page));
    assertThat(html).contains("<title>우리집 통계 · Bluesky 가계부</title>");
    assertThat(html).contains("지출 분류별").contains("60.0%").contains("100,000");
    assertThat(html).contains(">예산</th>").contains("50,000").contains("200,000"); // 예산 열
    assertThat(html).contains("parent=" + FOOD.id()); // 식비만 펼치기 링크
    assertThat(html).doesNotContain("parent=" + TAXI.id());
    assertThat(html)
        .contains("data-bk-chart=\"doughnut\"")
        .contains("data-bk-chart=\"bar\"")
        .contains("role=\"img\"");
    assertThat(html)
        .contains("\\&#34;택시\\&#34;") // JSON 이스케이프(\") 위에 HTML 속성 이스케이프(&#34;)
        .doesNotContain("data-bk-data=\"{\"labels"); // 속성이 따옴표에서 끊기지 않는다
    assertThat(html).contains("name=\"currency\"").contains("/js/vendor/chart.umd.min.js");
  }

  static CategoryTotal total(Category c, String currency, String amount) {
    return new CategoryTotal(c.id(), c.kind(), currency, new BigDecimal(amount));
  }
}
