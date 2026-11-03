package net.luversof.api.bookkeeping.constant;

/** 내역 종류. 수입 · 지출은 분류 필수, 이체는 받는 자산 · 받는 금액 필수. */
public enum EntryType {
  INCOME,
  EXPENSE,
  TRANSFER;

  /** 이 내역 종류에 맞는 분류 종류. 이체는 분류가 없어 null. */
  public CategoryKind categoryKind() {
    return switch (this) {
      case INCOME -> CategoryKind.INCOME;
      case EXPENSE -> CategoryKind.EXPENSE;
      case TRANSFER -> null;
    };
  }
}
