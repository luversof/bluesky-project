package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 원장 날짜 읽기(2026-11-02 사용자 결정) - 원장은 날짜를 자정 UTC(= KST 09:00)로 저장하므로 UTC 서쪽 화면 존에서도 같은 날짜가 나와야 한다.
 */
class StockZoneUtilRecordedDateTest {

  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Test
  void 자정_UTC_원장_날짜는_존과_무관하게_그날() {
    Instant ledgerDate = Instant.parse("2026-09-02T00:00:00Z");
    assertThat(StockZoneUtil.recordedDate(ledgerDate, NEW_YORK))
        .as("예전엔 9 월 1 일로 앞당겨졌다")
        .isEqualTo(LocalDate.parse("2026-09-02"));
    assertThat(StockZoneUtil.recordedDate(ledgerDate, SEOUL))
        .isEqualTo(LocalDate.parse("2026-09-02"));
  }

  @Test
  void 실제_시각이_든_값은_화면_존을_따른다() {
    Instant realTime = Instant.parse("2026-09-02T15:30:00Z");
    assertThat(StockZoneUtil.recordedDate(realTime, SEOUL))
        .isEqualTo(LocalDate.parse("2026-09-03"));
    assertThat(StockZoneUtil.recordedDate(realTime, NEW_YORK))
        .isEqualTo(LocalDate.parse("2026-09-02"));
    assertThat(StockZoneUtil.recordedDate(null, SEOUL)).isNull();
  }

  @Test
  void 원장_오늘은_화면_존과_KST_중_늦은_날() {
    LocalDate seoulToday = LocalDate.now(SEOUL);
    assertThat(StockZoneUtil.ledgerToday(NEW_YORK)).isAfterOrEqualTo(seoulToday);
    assertThat(StockZoneUtil.ledgerToday(NEW_YORK)).isAfterOrEqualTo(LocalDate.now(NEW_YORK));
  }
}
