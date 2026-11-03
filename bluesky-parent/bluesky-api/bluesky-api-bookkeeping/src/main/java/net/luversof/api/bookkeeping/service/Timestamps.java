package net.luversof.api.bookkeeping.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 저장할 시각. PostgreSQL timestamptz 는 마이크로초까지라, 잘라 두지 않으면 응답 값과 다시 읽은 값이 달라진다. */
final class Timestamps {

  private Timestamps() {}

  static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
