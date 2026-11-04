package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView.CurrencyTotal;
import net.luversof.web.gate.bookkeeping.support.BudgetView;
import net.luversof.web.gate.bookkeeping.support.FxView;

/** 기준통화 환산: 합계 · 환율 없음 · 예산 환산 · 월 화면 렌더. */
class BookkeepingFxTest {

  static final List<CurrencyTotal> TOTALS =
      List.of(
          new CurrencyTotal("JPY", new BigDecimal("0"), new BigDecimal("1000")),
          new CurrencyTotal("KRW", new BigDecimal("3000000"), new BigDecimal("12000")),
          new CurrencyTotal("USD", new BigDecimal("10.5"), new BigDecimal("3.33")));

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void convertsWithRatesAndListsMissing() {
    var fx = FxView.summary(TOTALS, "KRW", Map.of("USD", new BigDecimal("1380.5")));
    // USD 10.5 * 1380.5 = 14495.25 -> 14495, 3.33 * 1380.5 = 4597.065 -> 4597, JPY 는 빠짐
    assertThat(fx.income()).isEqualByComparingTo("3014495");
    assertThat(fx.expense()).isEqualByComparingTo("16597");
    assertThat(fx.missing()).containsExactly("JPY");
    assertThat(fx.foreign()).containsExactly("JPY", "USD");
    assertThat(fx.input("USD")).isEqualTo("1380.5");
    assertThat(fx.input("JPY")).isEmpty();

    assertThat(FxView.summary(List.of(TOTALS.get(1)), "KRW", Map.of())).isNull(); // 외화 없음
    assertThat(FxView.summary(TOTALS, "KRW", null).failed()).isTrue();
  }

  @Test
  void budgetAddsConvertedForeignSpending() {
    var food = BookkeepingScreenTest.FOOD;
    var totals =
        List.of(
            new CategoryTotal(food.id(), CategoryKind.EXPENSE, "KRW", new BigDecimal("10000")),
            new CategoryTotal(food.id(), CategoryKind.EXPENSE, "USD", new BigDecimal("10")),
            new CategoryTotal(food.id(), CategoryKind.EXPENSE, "JPY", new BigDecimal("500")));
    var status =
        BudgetView.status(
            List.of(new Budget(UUID.randomUUID(), food.bookId(), null, new BigDecimal("50000"))),
            totals,
            List.of(food),
            "KRW",
            Map.of("USD", new BigDecimal("1400")));
    assertThat(status.lines().get(0).spent()).isEqualByComparingTo("24000");
    assertThat(status.missing()).containsExactly("JPY");
  }

  @Test
  void monthShowsConvertedTotalsAndRateForm() {
    var fx = FxView.summary(TOTALS, "KRW", Map.of("USD", new BigDecimal("1380.5")));
    var html = BookkeepingRecurringTest.render(with(fx));
    assertThat(html).contains("기준통화(KRW) 환산").contains("3,014,495").contains("16,597");
    assertThat(html).contains("환율 없음: JPY");
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BookkeepingScreenTest.BOOK + "/fx-rates\"")
        .contains("name=\"rate.USD\" value=\"1380.5\"")
        .contains("name=\"rate.JPY\" value=\"\"")
        .contains("<details class=\"mt-2\" open>");
    assertThat(BookkeepingRecurringTest.render(with(null))).doesNotContain("bk-fx-title");
    assertThat(BookkeepingRecurringTest.render(with(FxView.summary(TOTALS, "KRW", null))))
        .contains("환율을 불러오지 못했습니다.");
  }

  static MonthPage with(FxView.Summary fx) {
    var p = BookkeepingScreenTest.page(List.of(), List.of(), EntryForm.closed());
    return new MonthPage(
        p.book(),
        p.range(),
        p.totals(),
        p.weeks(),
        p.entries(),
        p.day(),
        p.q(),
        p.categoryId(),
        p.assetId(),
        p.assets(),
        p.assetsById(),
        p.categories(),
        p.categoriesById(),
        p.form(),
        p.budget(),
        p.favorites(),
        p.recurringDue(),
        fx);
  }
}
