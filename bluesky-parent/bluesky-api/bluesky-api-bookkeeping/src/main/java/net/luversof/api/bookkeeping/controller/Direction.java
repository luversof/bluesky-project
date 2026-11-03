package net.luversof.api.bookkeeping.controller;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;

/** 순서 바꾸기 방향 파라미터(up · down). */
final class Direction {

  private Direction() {}

  static boolean isUp(String direction) {
    return switch (direction) {
      case "up" -> true;
      case "down" -> false;
      default -> throw BookkeepingErrorCode.INVALID_REQUEST.exception();
    };
  }
}
