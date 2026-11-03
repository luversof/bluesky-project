package net.luversof.api.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.bookkeeping.constant.AssetKind;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.CategoryKind;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Category;

/** DB 없이 도는 규칙 시험: 월 범위 · 금액 · 내역 칼럼 조합. */
class RulesTest {

  static final UUID BOOK = UUID.randomUUID();
  static final Asset KRW = asset("KRW");
  static final Asset KRW2 = asset("KRW");
  static final Asset USD = asset("USD");
  static final Category FOOD = category(CategoryKind.EXPENSE);
  static final Category SALARY = category(CategoryKind.INCOME);

  // ---- 월 범위

  @Test
  void monthRangeStartDay1IsCalendarMonth() {
    var r = MonthRange.of(YearMonth.of(2026, 2), 1);
    assertThat(r.from()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(r.to()).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void monthRangeNamedByEndingMonth() {
    var r = MonthRange.of(YearMonth.of(2026, 11), 25);
    assertThat(r.from()).isEqualTo(LocalDate.of(2026, 10, 25));
    assertThat(r.to()).isEqualTo(LocalDate.of(2026, 11, 24));
  }

  @Test
  void monthRangeCrossesYearAndShortFebruary() {
    var jan = MonthRange.of(YearMonth.of(2027, 1), 28);
    assertThat(jan.from()).isEqualTo(LocalDate.of(2026, 12, 28));
    assertThat(jan.to()).isEqualTo(LocalDate.of(2027, 1, 27));
    var mar = MonthRange.of(YearMonth.of(2027, 3), 28);
    assertThat(mar.from()).isEqualTo(LocalDate.of(2027, 2, 28));
    assertThat(mar.to()).isEqualTo(LocalDate.of(2027, 3, 27));
  }

  @Test
  void monthOfMatchesRangeForEveryDayOfYear() {
    for (int startDay : new int[] {1, 2, 15, 25, 28}) {
      for (var d = LocalDate.of(2026, 1, 1); d.getYear() < 2028; d = d.plusDays(1)) {
        var r = MonthRange.of(MonthRange.monthOf(d, startDay), startDay);
        assertThat(d).as("startDay %d, %s", startDay, d).isBetween(r.from(), r.to());
      }
    }
  }

  // ---- 금액

  @Test
  void amountDigitsFollowCurrency() {
    assertThat(Money.positive(new BigDecimal("12000"), "KRW")).isEqualByComparingTo("12000");
    assertThat(Money.positive(new BigDecimal("12000.00"), "KRW")).isEqualByComparingTo("12000");
    assertThat(Money.positive(new BigDecimal("10.25"), "USD")).isEqualByComparingTo("10.25");
    assertError(
        () -> Money.positive(new BigDecimal("0.5"), "KRW"), BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(
        () -> Money.positive(new BigDecimal("1.001"), "USD"), BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(() -> Money.positive(BigDecimal.ZERO, "KRW"), BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(
        () -> Money.positive(new BigDecimal("-1"), "KRW"), BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(() -> Money.positive(null, "KRW"), BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(
        () -> Money.positive(new BigDecimal("1000000000000000"), "KRW"),
        BookkeepingErrorCode.INVALID_AMOUNT);
    assertThat(Money.balance(new BigDecimal("-500000"), "KRW")).isEqualByComparingTo("-500000");
    assertThat(Money.balance(null, "KRW")).isEqualByComparingTo("0");
  }

  @Test
  void currencyMustBeIso4217Upper() {
    assertThat(Money.currency("JPY")).isEqualTo("JPY");
    assertError(() -> Money.currency("krw"), BookkeepingErrorCode.INVALID_CURRENCY);
    assertError(() -> Money.currency("ZZZ"), BookkeepingErrorCode.INVALID_CURRENCY);
    assertError(() -> Money.currency(null), BookkeepingErrorCode.INVALID_CURRENCY);
  }

  // ---- 내역 칼럼 조합

  @Test
  void expenseNeedsExpenseCategory() {
    assertThat(EntryRules.check(expense(FOOD.id()), KRW, null, FOOD)).isNull();
    assertError(
        () -> EntryRules.check(expense(SALARY.id()), KRW, null, SALARY),
        BookkeepingErrorCode.CATEGORY_KIND_MISMATCH);
    assertError(
        () -> EntryRules.check(expense(null), KRW, null, null),
        BookkeepingErrorCode.INVALID_ENTRY_COLUMNS);
    assertError(
        () -> EntryRules.check(expense(UUID.randomUUID()), KRW, null, null),
        BookkeepingErrorCode.NOT_EXIST_CATEGORY);
    assertError(
        () -> EntryRules.check(expense(FOOD.id()), null, null, FOOD),
        BookkeepingErrorCode.NOT_EXIST_ASSET);
  }

  @Test
  void incomeOrExpenseRejectsTransferColumns() {
    var c =
        new EntryCommand(
            EntryType.INCOME,
            LocalDate.now(),
            null,
            BigDecimal.TEN,
            BigDecimal.TEN,
            KRW.id(),
            KRW2.id(),
            SALARY.id(),
            null,
            null);
    assertError(
        () -> EntryRules.check(c, KRW, KRW2, SALARY), BookkeepingErrorCode.INVALID_ENTRY_COLUMNS);
  }

  @Test
  void sameCurrencyTransferFillsOrChecksToAmount() {
    assertThat(EntryRules.check(transfer(KRW, KRW2, "5000", null), KRW, KRW2, null))
        .isEqualByComparingTo("5000");
    assertThat(EntryRules.check(transfer(KRW, KRW2, "5000", "5000"), KRW, KRW2, null))
        .isEqualByComparingTo("5000");
    assertError(
        () -> EntryRules.check(transfer(KRW, KRW2, "5000", "4000"), KRW, KRW2, null),
        BookkeepingErrorCode.TRANSFER_AMOUNT_MISMATCH);
  }

  @Test
  void crossCurrencyTransferNeedsToAmountInTargetDigits() {
    assertThat(EntryRules.check(transfer(KRW, USD, "140000", "100.25"), KRW, USD, null))
        .isEqualByComparingTo("100.25");
    assertError(
        () -> EntryRules.check(transfer(KRW, USD, "140000", null), KRW, USD, null),
        BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(
        () -> EntryRules.check(transfer(KRW, USD, "140000", "100.255"), KRW, USD, null),
        BookkeepingErrorCode.INVALID_AMOUNT);
  }

  @Test
  void transferRejectsSelfCategoryAndMissingTarget() {
    assertError(
        () -> EntryRules.check(transfer(KRW, KRW, "1", null), KRW, KRW, null),
        BookkeepingErrorCode.INVALID_ENTRY_COLUMNS);
    var withCategory =
        new EntryCommand(
            EntryType.TRANSFER,
            LocalDate.now(),
            null,
            BigDecimal.ONE,
            null,
            KRW.id(),
            KRW2.id(),
            FOOD.id(),
            null,
            null);
    assertError(
        () -> EntryRules.check(withCategory, KRW, KRW2, FOOD),
        BookkeepingErrorCode.INVALID_ENTRY_COLUMNS);
    assertError(
        () -> EntryRules.check(transfer(KRW, KRW2, "1", null), KRW, null, null),
        BookkeepingErrorCode.NOT_EXIST_ASSET);
  }

  @Test
  void textLengthAndRequiredFields() {
    var longContent =
        new EntryCommand(
            EntryType.EXPENSE,
            LocalDate.now(),
            null,
            BigDecimal.ONE,
            null,
            KRW.id(),
            null,
            FOOD.id(),
            "x".repeat(EntryRules.MAX_CONTENT + 1),
            null);
    assertError(
        () -> EntryRules.check(longContent, KRW, null, FOOD), BookkeepingErrorCode.INVALID_REQUEST);
    var noDate =
        new EntryCommand(
            EntryType.EXPENSE,
            null,
            null,
            BigDecimal.ONE,
            null,
            KRW.id(),
            null,
            FOOD.id(),
            null,
            null);
    assertError(
        () -> EntryRules.check(noDate, KRW, null, FOOD), BookkeepingErrorCode.INVALID_REQUEST);
  }

  // ---- 도우미

  static Asset asset(String currency) {
    return new Asset(
        UUID.randomUUID(),
        BOOK,
        AssetKind.BANK,
        currency,
        currency,
        BigDecimal.ZERO,
        null,
        0,
        false);
  }

  static Category category(CategoryKind kind) {
    return new Category(UUID.randomUUID(), BOOK, null, kind, kind.name(), 0, false);
  }

  static EntryCommand expense(UUID categoryId) {
    return new EntryCommand(
        EntryType.EXPENSE,
        LocalDate.now(),
        null,
        new BigDecimal("1000"),
        null,
        KRW.id(),
        null,
        categoryId,
        "lunch",
        null);
  }

  static EntryCommand transfer(Asset from, Asset to, String amount, String toAmount) {
    return new EntryCommand(
        EntryType.TRANSFER,
        LocalDate.now(),
        null,
        new BigDecimal(amount),
        toAmount == null ? null : new BigDecimal(toAmount),
        from.id(),
        to.id(),
        null,
        null,
        null);
  }

  static void assertError(ThrowingCallable call, BookkeepingErrorCode code) {
    assertThatThrownBy(call)
        .isInstanceOf(BlueskyException.class)
        .extracting(e -> ((BlueskyException) e).getErrorCode())
        .isEqualTo("BookkeepingErrorCode." + code.name());
  }
}
