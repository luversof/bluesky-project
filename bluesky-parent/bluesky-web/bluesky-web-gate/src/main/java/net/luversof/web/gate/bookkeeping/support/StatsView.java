package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.CategoryTotal;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthTotal;

/**
 * 통계 화면 집계: 분류별 비중(대분류로 묶기 · 한 대분류 펼치기)과 월별 추이. 한 통화씩 본다(환산 없음).
 *
 * <p>API 의 분류별 합은 내역이 가리킨 분류 그대로라, 소분류 합을 부모로 올려 대분류 비중을 만든다.
 */
public final class StatsView {

  private StatsView() {}

  /**
   * 한 조각. drillParentId 가 있으면 그 대분류를 펼쳐 볼 수 있다. ownOfParent 면 "대분류에 직접 쓴 것".
   *
   * @param percent 소수 첫째 자리까지
   */
  public record Slice(
      UUID categoryId,
      String name,
      BigDecimal amount,
      BigDecimal percent,
      UUID drillParentId,
      boolean ownOfParent) {}

  /** 통화 선택지 "기준통화 환산"의 값. */
  public static final String CONVERTED = "FX";

  /**
   * 분류별 합을 기준통화로. 환율이 없는 통화는 빼고 missing 에 "USD 2026-11" 꼴로 남긴다.
   *
   * @param rates 그 달 환율(외화 1 = 기준통화 rate)
   */
  public static List<CategoryTotal> convert(
      List<CategoryTotal> totals,
      String base,
      YearMonth month,
      Map<String, BigDecimal> rates,
      Set<String> missing) {
    var result = new ArrayList<CategoryTotal>();
    for (var t : totals) {
      var amount = FxView.toBase(t.amount(), t.currency(), base, rates);
      if (amount == null) {
        missing.add(t.currency() + " " + month);
      } else {
        result.add(new CategoryTotal(t.categoryId(), t.kind(), base, amount));
      }
    }
    return result;
  }

  /** 월별 합을 달마다 그 달 환율로 기준통화 한 줄로 합친다. 환율이 없는 통화 · 달은 빼고 missing 에. */
  public static List<MonthTotal> convertTrend(
      List<MonthTotal> trend,
      String base,
      Map<YearMonth, Map<String, BigDecimal>> rates,
      Set<String> missing) {
    var sums = new TreeMap<YearMonth, BigDecimal[]>();
    for (var t : trend) {
      var sum =
          sums.computeIfAbsent(t.month(), m -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      var monthRates = rates.getOrDefault(t.month(), Map.of());
      var income = FxView.toBase(t.income(), t.currency(), base, monthRates);
      var expense = FxView.toBase(t.expense(), t.currency(), base, monthRates);
      if (income == null) {
        if (t.income().signum() != 0 || t.expense().signum() != 0) {
          missing.add(t.currency() + " " + t.month());
        }
        continue;
      }
      sum[0] = sum[0].add(income);
      sum[1] = sum[1].add(expense);
    }
    var result = new ArrayList<MonthTotal>();
    sums.forEach((m, s) -> result.add(new MonthTotal(m, base, s[0], s[1])));
    return result;
  }

  /** 통계 표의 예산 열: 대분류별 · 전체. 지출 · 대분류 단계 · 기준통화일 때만 화면이 싣는다. */
  public record Budgets(Map<UUID, BigDecimal> byCategory, BigDecimal total) {

    public static Budgets of(List<BudgetClient.Budget> budgets) {
      var byCategory = new LinkedHashMap<UUID, BigDecimal>();
      BigDecimal total = null;
      for (var b : budgets) {
        if (b.categoryId() == null) {
          total = b.amount();
        } else {
          byCategory.put(b.categoryId(), b.amount());
        }
      }
      return new Budgets(byCategory, total);
    }

    public BigDecimal of(UUID categoryId) {
      return byCategory.get(categoryId);
    }
  }

  /** 분류별 합에 나온 통화(이름 순). */
  public static List<String> currencies(List<CategoryTotal> totals, List<MonthTotal> trend) {
    var set = new TreeSet<String>();
    totals.forEach(t -> set.add(t.currency()));
    trend.forEach(t -> set.add(t.currency()));
    return new ArrayList<>(set);
  }

  /** 고른 통화가 있으면 그것, 없으면 기준통화(있으면) 또는 첫 통화. 하나도 없으면 기준통화. */
  public static String pickCurrency(
      String requested, List<String> currencies, String baseCurrency) {
    if (requested != null && currencies.contains(requested)) {
      return requested;
    }
    if (currencies.contains(baseCurrency) || currencies.isEmpty()) {
      return baseCurrency;
    }
    return currencies.get(0);
  }

  /**
   * 조각 목록(큰 것 먼저). parent 가 null 이면 대분류별, 있으면 그 대분류의 소분류별(+ 대분류에 직접 쓴 몫).
   *
   * @param categories 가계부의 모든 분류(숨긴 것 포함 - 지난 내역이 가리킬 수 있다)
   */
  public static List<Slice> slices(
      List<CategoryTotal> totals,
      List<Category> categories,
      String currency,
      CategoryKind kind,
      UUID parent) {
    Map<UUID, Category> byId = new LinkedHashMap<>();
    categories.forEach(c -> byId.put(c.id(), c));
    var sums = new LinkedHashMap<UUID, BigDecimal>();
    for (var t : totals) {
      if (!t.currency().equals(currency) || t.kind() != kind) {
        continue;
      }
      var c = byId.get(t.categoryId());
      var root = c == null || c.parentId() == null ? t.categoryId() : c.parentId();
      if (parent == null) {
        sums.merge(root, t.amount(), BigDecimal::add);
      } else if (parent.equals(root)) {
        sums.merge(t.categoryId(), t.amount(), BigDecimal::add);
      }
    }
    var total = sums.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    var result = new ArrayList<Slice>();
    sums.forEach(
        (id, amount) -> {
          var c = byId.get(id);
          boolean own = parent != null && parent.equals(id);
          // 표와 차트 범례가 같은 이름을 쓰도록 여기서 정한다
          var name =
              c == null
                  ? "?"
                  : own ? BookkeepingView.msg("bookkeeping.stats.parentOwn", c.name()) : c.name();
          var hasChildren = categories.stream().anyMatch(o -> id.equals(o.parentId()));
          result.add(
              new Slice(
                  id,
                  name,
                  amount,
                  percent(amount, total),
                  parent == null && hasChildren ? id : null,
                  own));
        });
    result.sort(Comparator.comparing(Slice::amount).reversed());
    return result;
  }

  public static BigDecimal sum(List<Slice> slices) {
    return slices.stream().map(Slice::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** 이 통화의 월별 합. */
  public static List<MonthTotal> trend(List<MonthTotal> all, String currency) {
    return all.stream().filter(t -> t.currency().equals(currency)).toList();
  }

  /** 차트 데이터(JSON). 숫자는 원래 자릿수 그대로, 이름은 JSON 이스케이프. HTML 속성에 넣을 때는 jte 가 다시 이스케이프한다. */
  public static String breakdownJson(List<Slice> slices) {
    var labels = new ArrayList<String>();
    var values = new ArrayList<String>();
    for (var s : slices) {
      labels.add(jsonString(s.name()));
      values.add(s.amount().stripTrailingZeros().toPlainString());
    }
    return "{\"labels\":["
        + String.join(",", labels)
        + "],\"values\":["
        + String.join(",", values)
        + "]}";
  }

  public static String trendJson(List<MonthTotal> trend, String incomeLabel, String expenseLabel) {
    var labels = new ArrayList<String>();
    var income = new ArrayList<String>();
    var expense = new ArrayList<String>();
    for (var t : trend) {
      labels.add(jsonString(t.month().toString()));
      income.add(t.income().stripTrailingZeros().toPlainString());
      expense.add(t.expense().stripTrailingZeros().toPlainString());
    }
    return "{\"labels\":["
        + String.join(",", labels)
        + "],\"series\":[{\"label\":"
        + jsonString(incomeLabel)
        + ",\"values\":["
        + String.join(",", income)
        + "]},{\"label\":"
        + jsonString(expenseLabel)
        + ",\"values\":["
        + String.join(",", expense)
        + "]}]}";
  }

  static BigDecimal percent(BigDecimal amount, BigDecimal total) {
    return total.signum() == 0
        ? BigDecimal.ZERO
        : amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
  }

  static String jsonString(String s) {
    var b = new StringBuilder("\"");
    for (char ch : s.toCharArray()) {
      switch (ch) {
        case '"' -> b.append("\\\"");
        case '\\' -> b.append("\\\\");
        case '\n' -> b.append("\\n");
        case '\r' -> b.append("\\r");
        case '\t' -> b.append("\\t");
        default -> {
          if (ch < 0x20) {
            b.append(String.format("\\u%04x", (int) ch));
          } else {
            b.append(ch);
          }
        }
      }
    }
    return b.append('"').toString();
  }
}
