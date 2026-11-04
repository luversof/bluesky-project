package net.luversof.api.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.bookkeeping.repository.EntryQuery.CardExpense;

/** 카드 결제 예정액: 전달 일시불 + 할부 회차 몫(끝전은 첫 회차). */
class CardBillsTest {

  static final UUID CARD = UUID.randomUUID();
  static final UUID USD_CARD = UUID.randomUUID();

  static CardExpense e(UUID card, String date, String amount, Integer months) {
    return new CardExpense(card, LocalDate.parse(date), new BigDecimal(amount), months);
  }

  static String bill(String month, List<CardExpense> expenses) {
    return CardBills.bills(YearMonth.parse(month), Map.of(CARD, "KRW"), expenses).stream()
        .map(b -> b.lumpSum().toPlainString() + "+" + b.installments().toPlainString())
        .findFirst()
        .orElseThrow();
  }

  @Test
  void lumpSumIsLastMonthUsageAndInstallmentsSplitWithRemainderFirst() {
    var expenses =
        List.of(
            e(CARD, "2026-10-05", "30000", null),
            e(CARD, "2026-10-31", "20000", null),
            e(CARD, "2026-11-01", "99999", null), // 다음 달 결제
            e(CARD, "2026-10-10", "100000", 3)); // 33334 · 33333 · 33333
    assertThat(bill("2026-11", expenses)).isEqualTo("50000+33334");
    assertThat(bill("2026-12", expenses)).isEqualTo("99999+33333");
    assertThat(bill("2027-01", expenses)).isEqualTo("0+33333");
    assertThat(bill("2027-02", expenses)).isEqualTo("0+0");
    assertThat(bill("2026-10", expenses)).isEqualTo("0+0"); // 산 달에는 안 나간다
  }

  @Test
  void sharesSumToAmountAndRespectCurrencyDigits() {
    for (int n = 2; n <= 36; n++) {
      var amount = new BigDecimal("1234567");
      var sum = CardBills.share(amount, n, true, "KRW");
      for (int k = 2; k <= n; k++) {
        sum = sum.add(CardBills.share(amount, n, false, "KRW"));
      }
      assertThat(sum).isEqualByComparingTo(amount);
    }
    assertThat(CardBills.share(new BigDecimal("100"), 3, false, "USD"))
        .isEqualByComparingTo("33.33");
    assertThat(CardBills.share(new BigDecimal("100"), 3, true, "USD"))
        .isEqualByComparingTo("33.34");
    // 모르는 카드(지워졌거나 카드가 아님)는 뺀다
    assertThat(
            CardBills.bills(
                YearMonth.of(2026, 11),
                Map.of(USD_CARD, "USD"),
                List.of(e(CARD, "2026-10-01", "1", null))))
        .singleElement()
        .satisfies(b -> assertThat(b.total()).isEqualByComparingTo("0"));
  }
}
