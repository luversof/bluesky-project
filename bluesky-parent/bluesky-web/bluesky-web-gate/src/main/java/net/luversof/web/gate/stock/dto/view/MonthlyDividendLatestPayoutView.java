package net.luversof.web.gate.stock.dto.view;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 종목 하나의 가장 최근 지급 기준일(관리 &gt; 월배당 기준 데이터 목록).
 *
 * <p>사용자 요청 2026-10-13: 목록에서 종목마다 마지막 기준일을 보면 지급 이력을 갱신했는지 한눈에 볼 수 있다. 월배당은 기준일이 한 달 간격이라 마지막 기준일이
 * {@link #STALE_AFTER_DAYS} 일보다 오래됐으면 한 달 치가 빠졌을 가능성이 크다 &mdash; 그 줄에 "갱신 확인" 표시를 단다(월중 15 일 기준 종목은
 * 다음 달 20 일, 월말 기준 종목은 두 달 뒤 4~5 일쯤 넘어간다).
 *
 * <p>2026-10-20: 기준일 규칙(월중 15 일 · 월말 마지막 거래일)이 선 종목은 그 규칙의 가장 최근 기준일({@code expectedRecordDate})이
 * 지났는데 이력에 없으면 바로 표시한다 &mdash; 35 일을 기다리면 월말 종목은 다음 달 4~5 일까지 표시가 늦었다. 규칙이 없는 종목은 예전 35 일 그대로.
 *
 * @param recordDate 가장 늦은 기준일
 * @param payDate 그 기준일 행의 지급일(없으면 null)
 * @param daysSinceRecord 오늘 - 기준일(일)
 * @param stale 규칙 종목은 {@code recordDate < expectedRecordDate}, 아니면 {@code daysSinceRecord >
 *     STALE_AFTER_DAYS}
 * @param expectedRecordDate 규칙상 오늘까지 지난 가장 최근 기준일(규칙 없는 종목은 null)
 */
public record MonthlyDividendLatestPayoutView(
    LocalDate recordDate,
    LocalDate payDate,
    long daysSinceRecord,
    boolean stale,
    LocalDate expectedRecordDate) {

  /** 이 일수를 넘기면 새 기준일이 들어와야 할 때를 지난 것으로 본다(한 달 + 지급·등록 여유). */
  public static final int STALE_AFTER_DAYS = 35;

  public static MonthlyDividendLatestPayoutView of(
      LocalDate recordDate, LocalDate payDate, LocalDate today) {
    return of(recordDate, payDate, today, null);
  }

  /** 기준일 규칙이 선 종목 - {@code expectedRecordDate} 가 있으면 그것으로 판정한다. */
  public static MonthlyDividendLatestPayoutView of(
      LocalDate recordDate, LocalDate payDate, LocalDate today, LocalDate expectedRecordDate) {
    long days = ChronoUnit.DAYS.between(recordDate, today);
    boolean stale =
        expectedRecordDate != null
            ? recordDate.isBefore(expectedRecordDate)
            : days > STALE_AFTER_DAYS;
    return new MonthlyDividendLatestPayoutView(
        recordDate, payDate, days, stale, expectedRecordDate);
  }
}
