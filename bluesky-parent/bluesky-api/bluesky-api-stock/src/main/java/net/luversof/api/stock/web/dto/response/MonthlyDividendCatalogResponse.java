package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 월배당 ETF 한 종목의 <b>종목 단위</b> 정보. 보유 수량 · 평단 같은 사용자 값이 들어가지 않는다.
 *
 * <p>사용자 요청 2026-09-21: "월배당 ETF 에 대해 등록된 데이터를 기준으로 정렬하고 검색할 수 있는 메뉴". 시뮬레이터의 월배당 탭은 <b>내가 받을
 * 배당</b>을 보여 주는 곳이라 보유 종목만 다루고, 안 가진 종목까지 견주는 일은 이 목록이 맡는다.
 *
 * <p>수익률은 주당 배당 &divide; 현재가라 수량이 필요 없다 &mdash; 보유하지 않은 종목도 그대로 계산된다.
 */
public record MonthlyDividendCatalogResponse(
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    /** 프로필: 지급 시기(MID_MONTH · MONTH_END · OTHER · UNKNOWN) · 출처 · 검증일 · 활성 여부 · 표시 순서. */
    String payoutWindow,
    String sourceUrl,
    LocalDate lastVerifiedDate,
    boolean active,
    Integer displayOrder,
    /** 지급 이력: 저장된 건수와 최신 기준일 · 지급일(데이터가 얼마나 최신인지). */
    int payoutCount,
    LocalDate latestRecordDate,
    LocalDate latestPayDate,
    /** 최근 1 년 기준 주당 분배금(최신 · 평균)과 과세표준 비중(%) · 평균 주당 과세표준액. */
    BigDecimal latestDividendPerShare,
    BigDecimal averageDividendPerShare1y,
    BigDecimal averageTaxableBaseRatio1y,
    BigDecimal averageTaxableBasePerShare1y,
    /** 현재가(마지막으로 수집된 종가)와 그 거래일. */
    BigDecimal currentPrice,
    LocalDate currentPriceDate,
    /** 현재가 기준 월 · 연 배당 수익률(%). 평균 주당 분배금 기준. */
    BigDecimal monthlyYieldPct,
    BigDecimal annualYieldPct,
    /** 시세 이력의 첫 날. 기간 수익률이 비어 있을 때 화면이 "언제부터 있는지" 를 말할 수 있게 함께 보낸다. */
    LocalDate priceHistoryStartDate,
    /** 1 · 3 · 6 · 12 개월 수익률. 이력이 그 기간을 못 덮으면 그 기간은 목록에서 빠진다(지어내지 않는다). */
    List<PeriodReturnView> periodReturns,
    /**
     * 분배금 추세: 최근 3 회 평균 주당 분배금과, 그것이 최근 12 회 평균보다 얼마나 늘거나 줄었는지(%).
     *
     * <p>지급 이력이 모자라면 둘 다 {@code null} 이다 &mdash; 0 을 보내면 "안정적" 으로 읽힌다.
     */
    BigDecimal averageDividendPerShare3m,
    BigDecimal payoutTrendPct,
    /**
     * 최근 1 년 위험 지표: 전고점 대비 최대 낙폭(%) · 연환산 변동성(%) · 실제로 센 첫 거래일.
     *
     * <p>합산 수익률만으로는 "어떻게 벌었는지" 를 못 본다(실측 2026-09-22: 합산 +165% 인 종목이 그 사이 43.1% 빠졌다). 이력이 1 년을 못 덮으면
     * 있는 만큼으로 내고 {@code riskFromDate} 가 언제부터인지 말한다.
     */
    BigDecimal maxDrawdownPct,
    BigDecimal volatilityPct,
    LocalDate riskFromDate,
    /**
     * 프로필의 총보수(연, %) · 상장일(2026-09-28). 운용사에서 못 가져온 종목은 {@code null} 이다 &mdash; 0 을 보내면 "보수 없음" 으로
     * 읽힌다.
     */
    BigDecimal totalExpenseRatioPct,
    LocalDate listingDate) {

  /** 한 기간의 가격 · 합산 수익률(사용자 결정 2026-09-21). */
  public record PeriodReturnView(
      int months, LocalDate baseDate, BigDecimal priceReturnPct, BigDecimal totalReturnPct) {}
}
