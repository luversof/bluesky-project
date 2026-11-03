package net.luversof.api.bookkeeping.constant;

/** 가계부 멤버 역할. OWNER 는 가계부당 1명. */
public enum BookRole {
  /** 가계부 삭제 · 설정 · 멤버 관리. */
  OWNER,
  /** 내역 · 자산 · 분류 편집. */
  MEMBER
}
