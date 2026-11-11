package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.api.bookkeeping.repository.EntryQuery.CardExpense;

/**
 * 카드 결제 예정액(순수 함수). 결제 달 M 에 나가는 돈 = (M-1 달 일시불 사용액) + (할부 중인 건의 이번 회차 몫).
 *
 * <p>할부 N 개월이면 산 달 다음 달부터 N 번 나눠 낸다. 한 회차 몫은 통화 자릿수에서 내림하고, 남는 끝전은 첫 회차에 더한다.
 *
 * <p>shortcut: 사용 기간을 달력 월(1 일 ~ 말일)로 본다. 카드사마다 다른 결제일별 이용 기간(예: 전월 15 일 ~ 당월 14 일)이 필요해지면 자산에 기준일을
 * 더한다.
 */
final class CardBills {

  /** 한 카드의 결제 달 M 예정액. */
  record Bill(UUID assetId, YearMonth month, BigDecimal lumpSum, BigDecimal installments) {

    BigDecimal total() {
      return lumpSum.add(installments);
    }
  }

  private CardBills() {}

  /** 결제 달 M 을 계산하려면 M-36 달 1 일 ~ M-1 달 말일의 카드 지출이 필요하다. */
  static YearMonth firstUsageMonth(YearMonth billMonth) {
    return billMonth.minusMonths(EntryRules.MAX_INSTALLMENT);
  }

  /** currencies: 카드 자산 id → 통화. 지출이 없는 카드도 0 으로 나온다. */
  static List<Bill> bills(
      YearMonth billMonth, Map<UUID, String> currencies, List<CardExpense> expenses) {
    var lump = new HashMap<UUID, BigDecimal>();
    var inst = new HashMap<UUID, BigDecimal>();
    for (var e : expenses) {
      var currency = currencies.get(e.assetId());
      if (currency == null) {
        continue;
      }
      long k = ChronoUnit.MONTHS.between(YearMonth.from(e.entryDate()), billMonth);
      if (e.installmentMonths() == null) {
        if (k == 1) {
          lump.merge(e.assetId(), e.amount(), BigDecimal::add);
        }
      } else if (k >= 1 && k <= e.installmentMonths()) {
        inst.merge(
            e.assetId(),
            share(e.amount(), e.installmentMonths(), k == 1, currency),
            BigDecimal::add);
      }
    }
    return currencies.keySet().stream()
        .map(id ->
            new Bill(
                id,
                billMonth,
                lump.getOrDefault(id, BigDecimal.ZERO),
                inst.getOrDefault(id, BigDecimal.ZERO)))
        .toList();
  }

  /** 한 회차 몫. 첫 회차는 끝전까지. */
  static BigDecimal share(BigDecimal amount, int months, boolean first, String currency) {
    int digits =
        Math.max(0, Math.min(Currency.getInstance(currency).getDefaultFractionDigits(), 4));
    var each = amount.divide(BigDecimal.valueOf(months), digits, RoundingMode.DOWN);
    return first ? amount.subtract(each.multiply(BigDecimal.valueOf(months - 1L))) : each;
  }
}
