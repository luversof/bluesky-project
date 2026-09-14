package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * 날짜 선택기 하한 고르기. '가장 이른 기간으로'(«) 가 이 값을 목표로 삼는다.
 *
 * <p>고정하는 것은 넷이다. (1) 매매·배당 중 <b>이른 쪽</b>. (2) 한쪽만 있으면 그쪽. (3) 둘 다 없으면 빈 문자열(하한 없음). (4) 시간대를 적용해
 * 날짜로 바꾼다 - instant 를 UTC 로 자르면 KST 에서 하루 어긋난다.
 */
class StockFirstDateUtilTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Test
  void 매매와_배당_중_이른_쪽을_쓴다() {
    Instant trade = Instant.parse("2020-03-04T00:00:00Z");
    Instant dividend = Instant.parse("2020-05-19T00:00:00Z");

    assertThat(StockFirstDateUtil.earliestLocalDate(trade, dividend, SEOUL))
        .isEqualTo("2020-03-04");
    assertThat(StockFirstDateUtil.earliestLocalDate(dividend, trade, SEOUL))
        .isEqualTo("2020-03-04");
  }

  /** 배당만 있고 매매가 없는 종목이 있다(실측 2026-09-13: 43 종목 중 1 종목). 매매만 보면 하한이 빈다. */
  @Test
  void 한쪽만_있으면_그쪽을_쓴다() {
    Instant only = Instant.parse("2021-07-15T00:00:00Z");

    assertThat(StockFirstDateUtil.earliestLocalDate(null, only, SEOUL)).isEqualTo("2021-07-15");
    assertThat(StockFirstDateUtil.earliestLocalDate(only, null, SEOUL)).isEqualTo("2021-07-15");
  }

  @Test
  void 둘_다_없으면_빈_값이다() {
    assertThat(StockFirstDateUtil.earliestLocalDate(null, null, SEOUL)).isEmpty();
  }

  /** 시간대가 없으면 날짜를 정할 수 없다 - 서버 존으로 몰래 바꾸지 않는다. */
  @Test
  void 시간대가_없으면_빈_값이다() {
    assertThat(
            StockFirstDateUtil.earliestLocalDate(Instant.parse("2020-03-04T00:00:00Z"), null, null))
        .isEmpty();
  }

  /** instant 를 UTC 로 자르면 KST 에서 하루 어긋난다 - 같은 실수를 세 번 했다. */
  @Test
  void 시간대를_적용해_날짜로_바꾼다() {
    Instant lateUtc = Instant.parse("2020-03-04T16:30:00Z");

    assertThat(StockFirstDateUtil.earliestLocalDate(lateUtc, null, SEOUL))
        .as("서울은 이미 다음 날")
        .isEqualTo("2020-03-05");
    assertThat(StockFirstDateUtil.earliestLocalDate(lateUtc, null, ZoneId.of("UTC")))
        .isEqualTo("2020-03-04");
  }

  /**
   * '전체' 배지의 시작. 시계열은 평가액이 잡히는 날부터라 첫 거래보다 늦게 시작할 수 있다 - 실측 2026-09-13: 삼성전자 상세 배지가 2020-03-23 인데
   * 표에는 2020-03-04 매매가 있었고(19 일), 계좌도 한국투자증권 위탁 13 일 · 연금저축1 2 일 늦었다.
   */
  @Test
  void 배지_시작은_시계열과_최초일_중_이른_쪽이다() {
    LocalDate series = LocalDate.parse("2020-03-23");

    assertThat(StockFirstDateUtil.coveredStart(series, "2020-03-04"))
        .as("표에 있는 행보다 늦게 시작한다고 적으면 거짓말이다")
        .isEqualTo(LocalDate.parse("2020-03-04"));
  }

  /** 시계열이 더 이르면 그대로 둔다 - 데이터 최초일보다 앞선 평가 구간도 화면에 있다. */
  @Test
  void 시계열이_더_이르면_그대로다() {
    LocalDate series = LocalDate.parse("2019-01-01");

    assertThat(StockFirstDateUtil.coveredStart(series, "2020-03-04")).isEqualTo(series);
  }

  @Test
  void 한쪽이_없으면_있는_쪽을_쓴다() {
    LocalDate series = LocalDate.parse("2020-03-23");

    assertThat(StockFirstDateUtil.coveredStart(series, "")).isEqualTo(series);
    assertThat(StockFirstDateUtil.coveredStart(series, null)).isEqualTo(series);
    assertThat(StockFirstDateUtil.coveredStart(null, "2020-03-04"))
        .isEqualTo(LocalDate.parse("2020-03-04"));
    assertThat(StockFirstDateUtil.coveredStart(null, "")).isNull();
  }

  /** 못 읽는 날짜가 오면 시계열 쪽을 쓴다 - 배지를 비우거나 터뜨리지 않는다. */
  @Test
  void 못_읽는_날짜는_무시한다() {
    LocalDate series = LocalDate.parse("2020-03-23");

    assertThat(StockFirstDateUtil.coveredStart(series, "BOGUS")).isEqualTo(series);
    assertThat(StockFirstDateUtil.coveredStart(null, "BOGUS")).isNull();
  }

  /**
   * 시계열이 아예 없는 종목(배당만 있고 매매가 없는 경우)은 끝을 못 정해 배지가 물음표로 나갔다 - 실측 2026-09-13: 하나금융지주 상세가 "전체 ·
   * 2020-04-08 ~ ?" 였다.
   */
  @Test
  void 배지_끝은_시계열과_자료_중_늦은_쪽이다() {
    LocalDate series = LocalDate.parse("2026-09-13");
    LocalDate content = LocalDate.parse("2026-08-28");

    assertThat(StockFirstDateUtil.coveredEnd(series, content)).isEqualTo(series);
    assertThat(StockFirstDateUtil.coveredEnd(content, series)).isEqualTo(series);
  }

  @Test
  void 시계열이_없으면_자료의_마지막_날을_쓴다() {
    LocalDate content = LocalDate.parse("2020-08-06");

    assertThat(StockFirstDateUtil.coveredEnd(null, content)).isEqualTo(content);
    assertThat(StockFirstDateUtil.coveredEnd(content, null)).isEqualTo(content);
    assertThat(StockFirstDateUtil.coveredEnd(null, null)).isNull();
  }

  /** 매매·배당처럼 갈래가 둘일 때 늦은 쪽으로 합친다. */
  @Test
  void 늦은_쪽으로_합친다() {
    LocalDate a = LocalDate.parse("2020-04-08");
    LocalDate b = LocalDate.parse("2020-08-06");

    assertThat(StockFirstDateUtil.later(a, b)).isEqualTo(b);
    assertThat(StockFirstDateUtil.later(b, a)).isEqualTo(b);
    assertThat(StockFirstDateUtil.later(null, null)).isNull();
  }
}
