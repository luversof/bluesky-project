package net.luversof.api.bookkeeping.constant;

/** 자산 종류. 체크카드는 두지 않는다(지출은 연결 은행 자산에 바로 기록). */
public enum AssetKind {
  CASH,
  BANK,
  /** 사용액이 쌓여 잔액이 음수(부채)가 되고, 결제는 은행 → 카드 이체. */
  CREDIT_CARD,
  INVESTMENT,
  LOAN,
  ETC
}
