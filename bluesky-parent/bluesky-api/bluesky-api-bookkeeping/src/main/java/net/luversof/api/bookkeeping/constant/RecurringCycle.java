package net.luversof.api.bookkeeping.constant;

/**
 * 반복 주기. MONTHLY = 매월 dayOfMonth 일(그 달에 없으면 말일), WEEKLY = 매주 dayOfWeek(1 월 ~ 7 일), YEARLY = 매년
 * monthOfYear 월 dayOfMonth 일(없으면 그 달 말일).
 */
public enum RecurringCycle {
  MONTHLY,
  WEEKLY,
  YEARLY
}
