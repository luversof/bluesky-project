package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.luversof.web.gate.bookkeeping.httpexchange.FxRateClient.FxRate;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView.CurrencyTotal;

/**
 * 기준통화 환산(손으로 넣은 월 환율). 환율이 없는 통화는 빼고 더하며 missing 에 남긴다 - 화면이 "환율 없음"으로 알린다.
 *
 * <p>통화별 합을 환율로 곱한 뒤 기준통화 자릿수에서 반올림한다.
 */
public final class FxView {

  /** 월 화면 환산 줄. foreign 은 이 달 합계에 나온 외화(환율 입력 칸), rates 는 이 달 환율, failed 면 환율을 못 불러왔다(환산 안 함). */
  public record Summary(
      String base,
      List<String> foreign,
      Map<String, BigDecimal> rates,
      BigDecimal income,
      BigDecimal expense,
      List<String> missing,
      boolean failed) {

    public BigDecimal net() {
      return income.subtract(expense);
    }

    /** 입력 칸 값(없으면 빈 칸). */
    public String input(String currency) {
      var rate = rates.get(currency);
      return rate == null ? "" : rate.stripTrailingZeros().toPlainString();
    }
  }

  private FxView() {}

  public static Map<String, BigDecimal> rates(List<FxRate> rates) {
    return rates.stream().collect(Collectors.toMap(FxRate::currency, FxRate::rate));
  }

  /** 외화 합계가 없으면 null(보여 줄 것이 없다). rates 가 null 이면 못 불러온 것. */
  public static Summary summary(
      List<CurrencyTotal> totals, String base, Map<String, BigDecimal> rates) {
    var foreign =
        totals.stream().map(CurrencyTotal::currency).filter(c -> !c.equals(base)).toList();
    if (foreign.isEmpty()) {
      return null;
    }
    if (rates == null) {
      return new Summary(base, foreign, Map.of(), BigDecimal.ZERO, BigDecimal.ZERO, foreign, true);
    }
    var income = BigDecimal.ZERO;
    var expense = BigDecimal.ZERO;
    var missing = new ArrayList<String>();
    for (var t : totals) {
      var in = toBase(t.income(), t.currency(), base, rates);
      var out = toBase(t.expense(), t.currency(), base, rates);
      if (in == null) {
        missing.add(t.currency());
        continue;
      }
      income = income.add(in);
      expense = expense.add(out);
    }
    return new Summary(base, foreign, rates, income, expense, missing, false);
  }

  /** 기준통화 금액. 기준통화면 그대로, 환율이 없으면 null. */
  public static BigDecimal toBase(
      BigDecimal amount, String currency, String base, Map<String, BigDecimal> rates) {
    if (currency.equals(base)) {
      return amount;
    }
    var rate = rates.get(currency);
    if (rate == null) {
      return null;
    }
    int digits = Math.max(0, Currency.getInstance(base).getDefaultFractionDigits());
    return amount.multiply(rate).setScale(digits, RoundingMode.HALF_UP);
  }
}
