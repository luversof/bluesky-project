package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;

/**
 * 월배당 ETF 목록의 정렬 · 필터(표현 로직만).
 *
 * <p>사용자 요청 2026-09-21: "월배당 ETF 에 대해 등록된 데이터를 기준으로 정렬하고 검색할 수 있는 메뉴". 시뮬레이터 표와 달리 보유 수량 · 평단이 없는
 * 종목 단위 지표만 다룬다.
 */
@Component
public class MonthlyEtfViewSupport {

  /** 시세 기준일이 이보다 오래되면 화면에 "(오래됨)" 을 붙인다. */
  public static final int PRICE_STALE_DAYS = 30;

  public static final String SORT_DISPLAY_ORDER = "display-order";
  public static final String SORT_SYMBOL = "symbol";
  public static final String SORT_NAME = "name";
  public static final String SORT_PAYOUT_WINDOW = "payout-window";
  public static final String SORT_MONTHLY_DIVIDEND = "monthly-dividend";
  public static final String SORT_TAXABLE_BASE = "taxable-base";
  public static final String SORT_ANNUAL_YIELD = "annual-yield";
  public static final String SORT_PAYOUT_COUNT = "payout-count";

  /** 고른 기간의 가격 수익률 · 합산 수익률로도 정렬한다(사용자 결정 2026-09-21). */
  public static final String SORT_PERIOD_PRICE = "period-price";

  public static final String SORT_PERIOD_TOTAL = "period-total";

  /** 화면이 고를 수 있는 기간(개월). api-stock 이 내려보내는 기간과 같아야 한다. */
  public static final java.util.List<Integer> PERIODS = java.util.List.of(1, 3, 6, 12);

  /** 기본 기간. 한 해를 보면 분배금이 한 바퀴 돌아 합산이 뜻을 갖는다. */
  public static final int DEFAULT_PERIOD = 12;
  public static final String SORT_LATEST_PAY_DATE = "latest-pay-date";

  /** 보유 구분 - 전체 · 보유 중 · 미보유. */
  public static final String HOLDING_ALL = "all";

  public static final String HOLDING_HELD = "held";

  public static final String HOLDING_NOT_HELD = "not-held";

  /** 정렬 키 검증(모르는 값은 표시 순서). */
  public String resolveSort(String sort) {
    if (!StringUtils.hasText(sort)) {
      return SORT_DISPLAY_ORDER;
    }

    return switch (sort) {
      case SORT_DISPLAY_ORDER,
          SORT_SYMBOL,
          SORT_NAME,
          SORT_PAYOUT_WINDOW,
          SORT_MONTHLY_DIVIDEND,
          SORT_TAXABLE_BASE,
          SORT_ANNUAL_YIELD,
          SORT_PAYOUT_COUNT,
          SORT_LATEST_PAY_DATE,
          SORT_PERIOD_PRICE,
          SORT_PERIOD_TOTAL ->
          sort;
      default -> SORT_DISPLAY_ORDER;
    };
  }

  /**
   * 정렬 방향(미지정이면 키별 기본값).
   *
   * <p>이름 · 종목코드 · 표시 순서 · 지급 시기 · 과세 비중은 작은 값부터(찾아보는 순서), 수익률 · 분배금 · 이력 건수 · 최신 지급일은 큰 값부터(좋은
   * 것부터).
   */
  public String resolveDirection(String sort, String direction) {
    if ("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction)) {
      return direction.toLowerCase(Locale.ROOT);
    }

    return switch (sort) {
      case SORT_MONTHLY_DIVIDEND,
          SORT_ANNUAL_YIELD,
          SORT_PAYOUT_COUNT,
          SORT_LATEST_PAY_DATE,
          SORT_PERIOD_PRICE,
          SORT_PERIOD_TOTAL ->
          "desc";
      default -> "asc";
    };
  }

  /** 보유 구분 값 검증(모르는 값은 전체). */
  public String resolveHolding(String holding) {
    return HOLDING_HELD.equals(holding) || HOLDING_NOT_HELD.equals(holding) ? holding : HOLDING_ALL;
  }

  /** 지급 시기 필터 값 검증(모르는 값은 전체 - 빈 문자열). */
  public String resolvePayoutWindow(String payoutWindow) {
    if (!StringUtils.hasText(payoutWindow)) {
      return "";
    }

    return switch (payoutWindow) {
      case "MID_MONTH", "MONTH_END", "OTHER", "UNKNOWN" -> payoutWindow;
      default -> "";
    };
  }

  /** 검색어(종목코드 · 종목명) · 최소 연배당 수익률 · 지급 시기 · 보유 구분 필터. */
  public List<MonthlyEtfRowView> filterRows(
      List<MonthlyEtfRowView> rows,
      String keyword,
      BigDecimal minAnnualYield,
      String payoutWindow,
      String holding) {
    String resolvedHolding = resolveHolding(holding);
    String resolvedWindow = resolvePayoutWindow(payoutWindow);
    return rows.stream()
        .filter(row -> matchesKeyword(row, keyword))
        .filter(
            row ->
                minAnnualYield == null || safe(row.annualYieldPct()).compareTo(minAnnualYield) >= 0)
        .filter(row -> resolvedWindow.isEmpty() || resolvedWindow.equals(row.payoutWindow()))
        .filter(row -> matchesHolding(row, resolvedHolding))
        .toList();
  }

  /** 정렬(동률이면 종목코드 오름차순). */
  public List<MonthlyEtfRowView> sortRows(
      List<MonthlyEtfRowView> rows, String sort, String direction) {
    Comparator<MonthlyEtfRowView> comparator =
        switch (sort) {
          case SORT_SYMBOL ->
              Comparator.comparing(
                  row -> safeText(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER);
          case SORT_NAME ->
              Comparator.comparing(
                  row -> safeText(row.stockItemName()), String.CASE_INSENSITIVE_ORDER);
          case SORT_PAYOUT_WINDOW ->
              Comparator.comparing(
                  row -> safeText(row.payoutWindow()), String.CASE_INSENSITIVE_ORDER);
          case SORT_MONTHLY_DIVIDEND ->
              Comparator.comparing(row -> safe(row.averageDividendPerShare1y()));
          case SORT_TAXABLE_BASE ->
              Comparator.comparing(row -> safe(row.averageTaxableBaseRatio1y()));
          case SORT_ANNUAL_YIELD -> Comparator.comparing(row -> safe(row.annualYieldPct()));
          // 이력이 모자라 값이 없는 종목은 늘 뒤로 보낸다(빈 칸이 "가장 낮은 수익률" 로 읽히면 안 된다).
          case SORT_PERIOD_PRICE -> nullsLast(MonthlyEtfRowView::periodPriceReturnPct);
          case SORT_PERIOD_TOTAL -> nullsLast(MonthlyEtfRowView::periodTotalReturnPct);
          case SORT_PAYOUT_COUNT -> Comparator.comparing(MonthlyEtfRowView::payoutCount);
          case SORT_LATEST_PAY_DATE ->
              Comparator.comparing(
                  row -> row.latestPayDate() != null ? row.latestPayDate() : LocalDate.MIN);
          default ->
              Comparator.comparing(
                  row -> row.displayOrder() != null ? row.displayOrder() : Integer.MAX_VALUE);
        };

    if (!"asc".equals(direction)) {
      comparator = comparator.reversed();
    }

    // 값이 없는 행(이력이 기간을 못 덮는 종목)은 방향과 무관하게 뒤로 보낸다.
    // reversed() 는 nullsLast 까지 뒤집어 버려서, 내림차순일 때 빈 칸이 맨 위로 올라왔다(실측 2026-09-21).
    Comparator<MonthlyEtfRowView> missingLast =
        Comparator.comparingInt(row -> periodValueMissing(row, sort) ? 1 : 0);

    return rows.stream()
        .sorted(
            missingLast
                .thenComparing(comparator)
                .thenComparing(
                    row -> safeText(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /** 고른 기간의 값이 없는가(이력이 모자란 종목). 기간 정렬에서만 뜻이 있다. */
  private boolean periodValueMissing(MonthlyEtfRowView row, String sort) {
    if (SORT_PERIOD_PRICE.equals(sort)) {
      return row.periodPriceReturnPct() == null;
    }
    if (SORT_PERIOD_TOTAL.equals(sort)) {
      return row.periodTotalReturnPct() == null;
    }

    return false;
  }

  private boolean matchesKeyword(MonthlyEtfRowView row, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }

    String normalized = keyword.toLowerCase(Locale.ROOT);
    return safeText(row.stockItemSymbol()).toLowerCase(Locale.ROOT).contains(normalized)
        || safeText(row.stockItemName()).toLowerCase(Locale.ROOT).contains(normalized);
  }

  private boolean matchesHolding(MonthlyEtfRowView row, String holding) {
    if (HOLDING_ALL.equals(holding)) {
      return true;
    }

    return HOLDING_HELD.equals(holding) == row.held();
  }

  /**
   * 시세 기준일이 오래됐는가(30 일 초과).
   *
   * <p>실측 2026-09-21: 보유하지 않는 종목의 시세는 뒤처진다 &mdash; TIGER 코리아배당다우존스의 기준일이 2026-04-01 이었다(173 일 전).
   * 화면에 기준일만 적으면 최신 시세처럼 읽히므로 "(오래됨)" 을 함께 붙인다.
   *
   * <p>이 판정을 템플릿에 두지 않는 까닭: 글자 대조 가드는 {@code isBefore} 를 {@code isAfter} 로 뒤집는 변이를 통과시킨다(변이 실험
   * 2026-09-21). 뒤집히면 최신 시세마다 "(오래됨)" 이 붙고 정작 오래된 것에는 안 붙는다.
   */
  public static boolean priceStale(LocalDate priceDate, LocalDate today) {
    if (priceDate == null || today == null) {
      return false;
    }

    return priceDate.isBefore(today.minusDays(PRICE_STALE_DAYS));
  }

  /** 값이 없는 행을 뒤로 보내는 비교자. 정렬 방향을 뒤집어도 빈 칸은 계속 뒤에 남는다. */
  private Comparator<MonthlyEtfRowView> nullsLast(
      java.util.function.Function<MonthlyEtfRowView, BigDecimal> value) {
    return Comparator.comparing(value, Comparator.nullsLast(Comparator.naturalOrder()));
  }

  /** 모르는 기간은 기본값으로 되돌린다 - 주소에 아무 숫자나 쳐도 화면이 무너지지 않게. */
  public int resolvePeriod(Integer period) {
    return period != null && PERIODS.contains(period) ? period : DEFAULT_PERIOD;
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String safeText(String value) {
    return value != null ? value : "";
  }
}
