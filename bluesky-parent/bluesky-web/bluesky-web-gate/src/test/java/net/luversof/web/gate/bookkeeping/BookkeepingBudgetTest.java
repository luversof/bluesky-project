package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.BookRole;
import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.BudgetView;

/** 예산: 대분류에 소분류 지출 합치기 · 기준통화만 · 넘침 · 월 화면 · 설정 입력 렌더. */
class BookkeepingBudgetTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;
  static final List<Budget> BUDGETS =
      List.of(
          new Budget(UUID.randomUUID(), BOOK, null, new BigDecimal("100000")),
          new Budget(
              UUID.randomUUID(), BOOK, BookkeepingScreenTest.FOOD.id(), new BigDecimal("30000")));
  static final List<CategoryTotal> TOTALS =
      List.of(
          new CategoryTotal(
              BookkeepingScreenTest.FOOD.id(),
              CategoryKind.EXPENSE,
              "KRW",
              new BigDecimal("20000")),
          new CategoryTotal(
              BookkeepingScreenTest.COFFEE.id(),
              CategoryKind.EXPENSE,
              "KRW",
              new BigDecimal("15000")),
          new CategoryTotal(
              BookkeepingScreenTest.COFFEE.id(), CategoryKind.EXPENSE, "USD", new BigDecimal("9")),
          new CategoryTotal(
              BookkeepingScreenTest.SALARY.id(),
              CategoryKind.INCOME,
              "KRW",
              new BigDecimal("3000000")));
  static final List<net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category> CATS =
      List.of(
          BookkeepingScreenTest.FOOD, BookkeepingScreenTest.COFFEE, BookkeepingScreenTest.SALARY);

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void parentBudgetCountsChildrenAndBaseCurrencyOnly() {
    var status = BudgetView.status(BUDGETS, TOTALS, CATS, "KRW");
    assertThat(status.failed()).isFalse();
    assertThat(status.lines())
        .extracting(
            l -> l.name() + " " + l.spent().toPlainString() + " " + l.percent() + " " + l.over())
        .containsExactly("전체 지출 35000 35 false", "식비 35000 116 true");
    var food = status.lines().get(1);
    assertThat(food.bar()).isEqualTo(100);
    assertThat(food.left()).isEqualByComparingTo("5000");
    assertThat(BudgetView.input(BUDGETS, null)).isEqualTo("100000");
    assertThat(BudgetView.input(BUDGETS, BookkeepingScreenTest.SALARY.id())).isEmpty();
  }

  @Test
  void monthShowsBudgetBarsOrUnavailableNote() {
    var base = BookkeepingScreenTest.page(List.of(), List.of(), EntryForm.closed());
    var html = renderMonth(with(base, BudgetView.status(BUDGETS, TOTALS, CATS, "KRW")));
    assertThat(html).contains("aria-labelledby=\"bk-budget-title\"");
    assertThat(html).contains("35,000 / 100,000").contains("65,000 남음");
    assertThat(html).contains("value=\"100\"").contains("5,000 초과");

    assertThat(renderMonth(base)).doesNotContain("bk-budget-title");
    assertThat(renderMonth(with(base, BudgetView.Status.unavailable())))
        .contains("예산을 불러오지 못했습니다.")
        .doesNotContain("bk-budget-title");
  }

  @Test
  void settingsBudgetFormListsTotalAndExpenseParents() {
    var page = BookkeepingSettingsScreenTest.page(BookRole.MEMBER, null, null, null, null);
    var html =
        BookkeepingSettingsScreenTest.render(
            new SettingsPage(
                page.book(),
                page.myRole(),
                page.categories(),
                page.assets(),
                page.members(),
                null,
                null,
                null,
                null,
                List.of(
                    BUDGETS.get(0),
                    new Budget(
                        UUID.randomUUID(),
                        BOOK,
                        BookkeepingSettingsScreenTest.FOOD.id(),
                        new BigDecimal("30000"))),
                List.of(),
                List.of()));
    assertThat(html).contains("action=\"/bookkeeping/" + BOOK + "/settings/budgets\"");
    assertThat(html).contains("name=\"budget.total\" value=\"100000\"");
    assertThat(html)
        .contains(
            "name=\"budget." + BookkeepingSettingsScreenTest.FOOD.id() + "\" value=\"30000\"");
    assertThat(html)
        .doesNotContain("name=\"budget." + BookkeepingSettingsScreenTest.COFFEE.id() + "\"");

    var failed =
        BookkeepingSettingsScreenTest.render(
            new SettingsPage(
                page.book(),
                page.myRole(),
                page.categories(),
                page.assets(),
                page.members(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()));
    assertThat(failed).contains("예산을 불러오지 못했습니다.").doesNotContain("/settings/budgets\"");
  }

  static MonthPage with(MonthPage p, BudgetView.Status budget) {
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
        BookkeepingView.categoriesById(p.categories()),
        p.form(),
        budget,
        p.favorites(),
        p.recurringDue(),
        p.fx());
  }

  static String renderMonth(MonthPage page) {
    return BookkeepingScreenTest.render("bookkeeping/month.jte", Map.of("page", page));
  }
}
