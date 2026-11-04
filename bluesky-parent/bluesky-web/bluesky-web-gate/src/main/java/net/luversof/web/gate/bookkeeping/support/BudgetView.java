package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;

/** 예산 대비 지출. 외화 지출은 그 달 환율로 기준통화 환산해 더하고, 환율이 없는 통화는 빼고 missing 에 남긴다. 대분류 예산은 소분류 지출까지 더한다. */
public final class BudgetView {

  /** 한 줄. percent 는 내림한 정수(넘으면 100 보다 크다). */
  public record Line(String name, BigDecimal budget, BigDecimal spent, int percent) {

    public boolean over() {
      return spent.compareTo(budget) > 0;
    }

    /** 남은 돈(넘으면 초과액, 양수). */
    public BigDecimal left() {
      return budget.subtract(spent).abs();
    }

    /** progress 막대 값(0~100). */
    public int bar() {
      return Math.min(percent, 100);
    }
  }

  /** lines 가 비면 예산 없음. failed 면 예산을 불러오지 못했다(월 화면은 그대로 뜬다). */
  public record Status(List<Line> lines, boolean failed, List<String> missing) {

    public static Status none() {
      return new Status(List.of(), false, List.of());
    }

    public static Status unavailable() {
      return new Status(List.of(), true, List.of());
    }
  }

  private BudgetView() {}

  /** 전체 예산이 먼저, 다음은 분류 목록 순서. */
  public static Status status(
      List<Budget> budgets,
      List<CategoryTotal> totals,
      List<Category> categories,
      String baseCurrency) {
    return status(budgets, totals, categories, baseCurrency, Map.of());
  }

  /** rates: 그 달 환율(외화 1 = 기준통화 rate). */
  public static Status status(
      List<Budget> budgets,
      List<CategoryTotal> totals,
      List<Category> categories,
      String baseCurrency,
      Map<String, BigDecimal> rates) {
    Map<UUID, Category> byId = BookkeepingView.categoriesById(categories);
    var expense = new ArrayList<CategoryTotal>();
    var missing = new TreeSet<String>();
    for (var t : totals) {
      if (t.kind() != CategoryKind.EXPENSE) {
        continue;
      }
      var amount = FxView.toBase(t.amount(), t.currency(), baseCurrency, rates);
      if (amount == null) {
        missing.add(t.currency());
      } else {
        expense.add(new CategoryTotal(t.categoryId(), t.kind(), baseCurrency, amount));
      }
    }
    var lines = new ArrayList<Line>();
    budgets.stream()
        .filter(b -> b.categoryId() == null)
        .findFirst()
        .ifPresent(
            b ->
                lines.add(
                    line(
                        BookkeepingView.msg("bookkeeping.budget.total"),
                        b.amount(),
                        sum(expense, t -> true))));
    for (var c : categories) {
      budgets.stream()
          .filter(b -> c.id().equals(b.categoryId()))
          .findFirst()
          .ifPresent(
              b ->
                  lines.add(
                      line(
                          c.name(),
                          b.amount(),
                          sum(expense, t -> c.id().equals(topId(byId, t.categoryId()))))));
    }
    return new Status(lines, false, List.copyOf(missing));
  }

  /** 설정 입력 칸 값(없으면 빈 칸). */
  public static String input(List<Budget> budgets, UUID categoryId) {
    return budgets.stream()
        .filter(b -> Objects.equals(b.categoryId(), categoryId))
        .findFirst()
        .map(b -> b.amount().stripTrailingZeros().toPlainString())
        .orElse("");
  }

  static Line line(String name, BigDecimal budget, BigDecimal spent) {
    int percent =
        spent.multiply(BigDecimal.valueOf(100)).divide(budget, 0, RoundingMode.DOWN).intValue();
    return new Line(name, budget, spent, percent);
  }

  private static UUID topId(Map<UUID, Category> byId, UUID categoryId) {
    var c = categoryId == null ? null : byId.get(categoryId);
    return c == null ? categoryId : c.parentId() == null ? c.id() : c.parentId();
  }

  private static BigDecimal sum(
      List<CategoryTotal> totals, java.util.function.Predicate<CategoryTotal> filter) {
    return totals.stream()
        .filter(filter)
        .map(CategoryTotal::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
