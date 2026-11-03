package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.util.Currency;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;

/** 통화 코드 · 금액 검증. 금액 칼럼은 NUMERIC(19,4)(정수부 15자리, 소수 4자리). */
final class Money {

  private static final int MAX_SCALE = 4;
  private static final int MAX_INTEGER_DIGITS = 15;

  private Money() {}

  /** ISO 4217 코드(대문자 3자)인지 확인하고 그대로 돌려준다. */
  static String currency(String code) {
    if (code == null || !code.matches("[A-Z]{3}")) {
      throw BookkeepingErrorCode.INVALID_CURRENCY.exception();
    }
    try {
      Currency.getInstance(code);
    } catch (IllegalArgumentException e) {
      throw BookkeepingErrorCode.INVALID_CURRENCY.exception();
    }
    return code;
  }

  /** 내역 금액: 0 보다 크고 통화의 소수 자릿수 안. */
  static BigDecimal positive(BigDecimal amount, String currency) {
    if (amount == null || amount.signum() <= 0) {
      throw BookkeepingErrorCode.INVALID_AMOUNT.exception();
    }
    return checkDigits(amount, currency);
  }

  /** 자산 시작 잔액: 음수 · 0 허용(대출 · 카드), 자릿수만 검사. null 은 0. */
  static BigDecimal balance(BigDecimal amount, String currency) {
    return amount == null ? BigDecimal.ZERO : checkDigits(amount, currency);
  }

  private static BigDecimal checkDigits(BigDecimal amount, String currency) {
    // XAU 처럼 소수 자릿수가 정해지지 않은 통화(-1)는 칼럼 한도까지 허용
    int digits = Currency.getInstance(currency).getDefaultFractionDigits();
    int maxScale = digits < 0 ? MAX_SCALE : Math.min(digits, MAX_SCALE);
    var normalized = amount.stripTrailingZeros();
    if (Math.max(normalized.scale(), 0) > maxScale
        || normalized.precision() - normalized.scale() > MAX_INTEGER_DIGITS) {
      throw BookkeepingErrorCode.INVALID_AMOUNT.exception();
    }
    return amount;
  }
}
