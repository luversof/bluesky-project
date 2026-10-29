package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 배당 화면의 "변동 요인 (전기 대비)" 비교 구간은 목록과 같은 기간에서 나와야 한다.
 *
 * <p>실측 2026-09-10, 결함 두 가지.
 *
 * <p>(1) 역순 입력(2026-09-10 ~ 2026-06-10)에서 전기가 <b>2026-12-10 ~ 2026-09-09</b> 로 나왔다. 미래이자 역순이다
 * &mdash; {@code durationDays} 가 -91 이 되어 {@code startLocal.minusDays(-91)} 이 미래로 갔다. 목록 자체는 {@code
 * effectiveRange} 로 바로잡혀 2026-06-10 ~ 2026-09-09 를 보여주므로, 화면에 기간 두 개가 서로 다른 말을 했다.
 *
 * <p>(2) 날짜 없이 들어오는 경로(파라미터 없음 · rangeMode=3/ytd/mtd)에서는 전기가 아예 없어 "변동 요인 (전기 대비)" 섹션 전체가 빠졌다. 파라미터
 * 없는 진입은 메뉴로 들어오는 기본 경로다.
 */
class DividendPreviousPeriodTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private Instant kst(String date) {
    return LocalDate.parse(date).atStartOfDay(KST).toInstant();
  }

  @Test
  void 역순_구간도_정순과_같은_전기를_준다() {
    var forward =
        StockDividendHtmxController.resolvePreviousPeriod(
            kst("2026-06-10"), kst("2026-09-10"), null, KST);
    var reversed =
        StockDividendHtmxController.resolvePreviousPeriod(
            kst("2026-09-10"), kst("2026-06-10"), null, KST);

    assertThat(reversed).isEqualTo(forward);
  }

  @Test
  void 전기는_선택_구간보다_앞이다() {
    var reversed =
        StockDividendHtmxController.resolvePreviousPeriod(
            kst("2026-09-10"), kst("2026-06-10"), null, KST);

    assertThat(reversed).isNotNull();
    assertThat(reversed.start()).isBefore(reversed.end());
    assertThat(reversed.end()).isBefore(LocalDate.parse("2026-06-10"));
  }

  @Test
  void 프리셋도_전기를_준다() {
    for (String mode : new String[] {"3", "ytd", "mtd"}) {
      var previous =
          StockDividendHtmxController.resolvePreviousPeriod(
              kst("2026-06-10"), kst("2026-09-10"), mode, KST);

      assertThat(previous).as("rangeMode=" + mode).isNotNull();
      assertThat(previous.start()).as("rangeMode=" + mode).isBefore(previous.end());
    }
  }

  /** 결함 (2) 는 메서드가 아니라 호출부에 있었다. 원본 파라미터를 넘기면 프리셋으로 채워진 기본 기간이 전기 계산에 닿지 않는다. */
  @Test
  void 호출부는_보정된_기간을_넘긴다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockDividendHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(source)
        .as("원본 startDate/endDate 를 넘기면 역순 교정과 프리셋 기본 기간이 전기에 반영되지 않는다")
        .contains(
            "resolvePreviousPeriod(startInstant, inclusiveEnd(endInstant), rangeMode, earlyZone)")
        .doesNotContain("resolvePreviousPeriod(startDate, endDate");
  }

  /**
   * 목록 기간의 끝은 배타(다음 날 0 시)다 - inclusiveEnd 를 거치면 직접 지정 1/1 ~ 1/31 의 전기는 같은 31 일(검토 지적 2026-10-28:
   * 32 일이었다).
   */
  @Test
  void 배타_끝을_포함_날짜로_바꿔_같은_길이의_전기를_준다() {
    var previous =
        StockDividendHtmxController.resolvePreviousPeriod(
            kst("2026-01-01"),
            StockDividendHtmxController.inclusiveEnd(kst("2026-02-01")),
            null,
            KST);

    assertThat(previous.start()).isEqualTo(LocalDate.parse("2025-12-01"));
    assertThat(previous.end()).isEqualTo(LocalDate.parse("2025-12-31"));
    assertThat(StockDividendHtmxController.inclusiveEnd(null)).isNull();
  }

  /** 이번 달 · 올해: 전기(완전한 전월 · 전년)는 그대로, 그중 오늘과 같은 날까지를 따로 낸다(사용자 결정 2026-10-29). */
  @Test
  void 올해와_이번_달은_전기_중_같은_날까지를_낸다() {
    var ytdPrevious =
        new StockDividendHtmxController.PreviousPeriod(
            LocalDate.parse("2025-01-01"), LocalDate.parse("2025-12-31"));
    var ytd =
        StockDividendHtmxController.resolvePreviousToDate(
            ytdPrevious, kst("2026-10-30"), "ytd", KST);
    assertThat(ytd.start()).isEqualTo(LocalDate.parse("2025-01-01"));
    assertThat(ytd.end()).as("오늘 10-29 의 작년 같은 날").isEqualTo(LocalDate.parse("2025-10-29"));

    var mtdPrevious =
        new StockDividendHtmxController.PreviousPeriod(
            LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));
    assertThat(
            StockDividendHtmxController.resolvePreviousToDate(
                    mtdPrevious, kst("2026-10-30"), "mtd", KST)
                .end())
        .isEqualTo(LocalDate.parse("2026-09-29"));
    assertThat(
            StockDividendHtmxController.resolvePreviousToDate(
                mtdPrevious, kst("2026-10-31"), "mtd", KST))
        .as("10-30 이면 9 월은 이미 다 지났다 - 같은 길이라 줄이 필요 없다")
        .isNull();
    var febPrevious =
        new StockDividendHtmxController.PreviousPeriod(
            LocalDate.parse("2027-02-01"), LocalDate.parse("2027-02-28"));
    assertThat(
            StockDividendHtmxController.resolvePreviousToDate(
                    febPrevious, kst("2027-03-28"), "mtd", KST)
                .end())
        .isEqualTo(LocalDate.parse("2027-02-27"));
    assertThat(
            StockDividendHtmxController.resolvePreviousToDate(
                ytdPrevious, kst("2026-10-30"), "1y", KST))
        .as("달력 프리셋이 아니면 없다")
        .isNull();
    assertThat(
            StockDividendHtmxController.resolvePreviousToDate(null, kst("2026-10-30"), "ytd", KST))
        .isNull();
  }
}
