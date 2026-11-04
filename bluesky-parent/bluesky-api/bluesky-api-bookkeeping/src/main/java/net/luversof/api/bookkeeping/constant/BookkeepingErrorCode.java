package net.luversof.api.bookkeeping.constant;

import io.github.luversof.boot.exception.BlueskyErrorCode;

/**
 * 가계부 오류 코드. 메시지 키는 {@code BlueskyException.BookkeepingErrorCode.<이름>}.
 *
 * <p>상태를 바꿀 땐 {@code throw X.exceptionWithStatus(403)} 을 쓴다. {@code throwExceptionWithStatus(int)}
 * 는 상태를 메시지 인자로 넘겨 400 이 된다.
 */
public enum BookkeepingErrorCode implements BlueskyErrorCode<BookkeepingErrorCode> {
  // 공통
  INVALID_REQUEST,
  INVALID_NAME,

  // 가계부 · 권한
  NOT_EXIST_BOOK,
  NOT_BOOK_MEMBER,
  NOT_BOOK_OWNER,
  INVALID_CURRENCY,
  INVALID_MONTH_START_DAY,
  INVALID_FX_RATE,

  // 멤버 · 초대
  ALREADY_BOOK_MEMBER,
  NOT_EXIST_MEMBER,
  OWNER_CANNOT_LEAVE,
  NOT_EXIST_INVITE,
  EXPIRED_INVITE,
  USED_INVITE,

  // 자산 · 분류
  NOT_EXIST_ASSET,
  ASSET_CURRENCY_LOCKED,
  NOT_EXIST_CATEGORY,
  CATEGORY_DEPTH_EXCEEDED,
  CATEGORY_KIND_MISMATCH,

  // 내역
  NOT_EXIST_ENTRY,
  INVALID_ENTRY_COLUMNS,
  INVALID_AMOUNT,
  TRANSFER_AMOUNT_MISMATCH,
  INVALID_INSTALLMENT,
  IMPORT_ROW_INVALID,

  // 반복
  NOT_EXIST_RECURRING,
  INVALID_RECURRING_CYCLE,
  NOT_RECURRING_OCCURRENCE,
  RECURRING_ALREADY_DONE,
}
