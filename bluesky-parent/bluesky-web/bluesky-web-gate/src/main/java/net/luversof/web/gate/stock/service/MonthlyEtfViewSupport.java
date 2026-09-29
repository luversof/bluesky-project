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

  /** 계좌를 가르는 규칙은 적립 추천이 쓰는 것을 그대로 쓴다 - 두 곳에 적으면 한쪽만 고쳐진다. */
  private final MonthlyContributionPickSupport monthlyContributionPickSupport;

  public MonthlyEtfViewSupport(MonthlyContributionPickSupport monthlyContributionPickSupport) {
    this.monthlyContributionPickSupport = monthlyContributionPickSupport;
  }

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
  /** 최근 1 년 위험 지표로도 정렬한다(사용자 결정 2026-09-22). */
  public static final String SORT_MAX_DRAWDOWN = "max-drawdown";

  public static final String SORT_VOLATILITY = "volatility";

  public static final String SORT_PERIOD_PRICE = "period-price";

  public static final String SORT_PERIOD_TOTAL = "period-total";

  /** 총보수(연)로도 정렬한다 - 싼 것부터가 기본(2026-09-28). */
  public static final String SORT_EXPENSE_RATIO = "expense-ratio";

  /** 점수(연배당 수익률 + 분배금이 줄어든 만큼 감점) - 사용자 요청 2026-09-30: "점수 기준으로 정렬해서 볼 수 있으면". */
  public static final String SORT_SCORE = "score";

  /**
   * 자리 차례(월중 → 월말, 그 안에서 위탁 → ISA/연금) - 자리별 적립 추천 보기에서만 뜻이 있다(사용자 요청 2026-09-30: "월중 월말 섞어서 나오는데
   * 기준이 뭐야?"). 전체 보기에서는 표시 순서로 되돌린다({@link #resolveSortForView}).
   */
  public static final String SORT_SLOT = "slot";

  /** 화면이 고를 수 있는 기간(개월). api-stock 이 내려보내는 기간과 같아야 한다. */
  public static final java.util.List<Integer> PERIODS = java.util.List.of(1, 3, 6, 12);

  /** 기본 기간. 한 해를 보면 분배금이 한 바퀴 돌아 합산이 뜻을 갖는다. */
  public static final int DEFAULT_PERIOD = 12;

  public static final String SORT_LATEST_PAY_DATE = "latest-pay-date";

  /** 보유 구분 - 전체 · 보유 중 · 미보유. */
  public static final String HOLDING_ALL = "all";

  public static final String HOLDING_HELD = "held";

  public static final String HOLDING_NOT_HELD = "not-held";

  /**
   * 계좌 대상 - 전체(빈 값) · 위탁 · 연금 &middot; ISA.
   *
   * <p>가르는 규칙은 적립 추천과 같은 곳을 쓴다({@link MonthlyContributionPickSupport#accountOf}): 과세표준 비중이 {@value
   * MonthlyContributionPickSupport#TAXABLE_LIMIT_PCT}% 이내면 위탁, 그보다 크면 연금 &middot; ISA 다. 두 곳에 따로
   * 적으면 한쪽만 고쳐질 수 있다.
   */
  public static final String ACCOUNT_ALL = "";

  /** 화면 위에 띄우는 추천은 셋까지. 더 늘리면 "골랐다" 는 뜻이 옅어진다. */
  public static final int PICK_LIMIT = 3;

  /**
   * 추천 한 줄 &mdash; 행과 그 점수.
   *
   * @param row 추천된 종목의 행(이름 · 연배당 · 추세를 화면이 그대로 쓴다)
   * @param score 적립 추천과 같은 셈으로 낸 점수
   */
  public record MonthlyEtfPick(MonthlyEtfRowView row, BigDecimal score) {}

  /**
   * 지금 화면에 걸린 조건 안에서 눈여겨볼 종목을 고른다(사용자 결정 2026-09-22).
   *
   * <p>셈은 적립 추천과 같은 것을 쓴다({@link MonthlyContributionPickSupport#scoreOf}) &mdash; 두 화면이 다른 말을 하면 어느
   * 쪽을 믿어야 할지 알 수 없다. <b>분배금 추세를 아직 모르는 종목은 뺀다</b>: 감점이 0 이라 이력이 짧은 종목이 연배당만으로 위로 올라온다(실측
   * 2026-09-22: 추세를 모르는 두 종목이 2 · 3 위를 차지했다).
   *
   * @param rows 이미 걸러 놓은 행들(검색어 · 계좌 · 지급 시기가 이미 반영된 것)
   * @return 점수 높은 차례, 같으면 종목코드 차례. 고를 것이 없으면 빈 목록
   */
  public List<MonthlyEtfPick> pickRows(List<MonthlyEtfRowView> rows) {
    return rows.stream()
        .filter(row -> row.annualYieldPct() != null && row.payoutTrendPct() != null)
        .map(
            row ->
                new MonthlyEtfPick(
                    row,
                    monthlyContributionPickSupport.scoreOf(
                        row.annualYieldPct(), row.payoutTrendPct())))
        .filter(pick -> pick.score() != null)
        .sorted(
            Comparator.comparing(MonthlyEtfPick::score)
                .reversed()
                .thenComparing(pick -> safeText(pick.row().stockItemSymbol())))
        .limit(PICK_LIMIT)
        .toList();
  }

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
          SORT_PERIOD_TOTAL,
          SORT_MAX_DRAWDOWN,
          SORT_VOLATILITY,
          SORT_EXPENSE_RATIO,
          SORT_SCORE,
          SORT_SLOT ->
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
          SORT_PERIOD_TOTAL,
          SORT_SCORE,
          // 낙폭은 음수다 - 큰 값(0 에 가까운 쪽)이 덜 빠진 것이므로 좋은 것부터 보려면 내림차순이다.
          SORT_MAX_DRAWDOWN ->
          "desc";
      default -> "asc";
    };
  }

  /** 보유 구분 값 검증(모르는 값은 전체). */
  public String resolveHolding(String holding) {
    return HOLDING_HELD.equals(holding) || HOLDING_NOT_HELD.equals(holding) ? holding : HOLDING_ALL;
  }

  /** "이번 적립만 보기" (사용자 요청 2026-09-23). 빈 값은 전체 보기다. */
  public static final String VIEW_CONTRIBUTION = "contribution";

  /** 보기 값 검증(모르는 값은 전체 - 빈 문자열). */
  public String resolveView(String view) {
    return VIEW_CONTRIBUTION.equals(view) ? VIEW_CONTRIBUTION : "";
  }

  /**
   * 이번 적립 보기면 적립 자리에 든 종목만 남긴다 &mdash; ETF 가 많아지면 네 자리만 보고 싶다(사용자 요청).
   *
   * @param pickedSymbols 시뮬레이터와 같은 답으로 낸 이번 적립 종목코드
   */
  public List<MonthlyEtfRowView> filterContribution(
      List<MonthlyEtfRowView> rows, java.util.Set<String> pickedSymbols, String view) {
    if (!VIEW_CONTRIBUTION.equals(view)) {
      return rows;
    }
    return rows.stream()
        .filter(row -> pickedSymbols.contains(safeText(row.stockItemSymbol())))
        .toList();
  }

  /** 계좌 대상 필터 값 검증(모르는 값은 전체 - 빈 문자열). */
  public String resolveAccount(String account) {
    if (!StringUtils.hasText(account)) {
      return ACCOUNT_ALL;
    }

    return switch (account) {
      case MonthlyContributionPickSupport.ACCOUNT_BROKERAGE,
          MonthlyContributionPickSupport.ACCOUNT_PENSION ->
          account;
      default -> ACCOUNT_ALL;
    };
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

  /** 검색어(종목코드 · 종목명) · 최소 연배당 수익률 · 지급 시기 · 보유 구분 · 계좌 대상 필터. */
  public List<MonthlyEtfRowView> filterRows(
      List<MonthlyEtfRowView> rows,
      String keyword,
      BigDecimal minAnnualYield,
      String payoutWindow,
      String holding,
      String account) {
    String resolvedHolding = resolveHolding(holding);
    String resolvedWindow = resolvePayoutWindow(payoutWindow);
    String resolvedAccount = resolveAccount(account);
    return rows.stream()
        .filter(row -> matchesKeyword(row, keyword))
        .filter(
            row ->
                minAnnualYield == null || safe(row.annualYieldPct()).compareTo(minAnnualYield) >= 0)
        .filter(row -> resolvedWindow.isEmpty() || resolvedWindow.equals(row.payoutWindow()))
        .filter(row -> matchesHolding(row, resolvedHolding))
        .filter(row -> matchesAccount(row, resolvedAccount))
        .toList();
  }

  /** 자리 차례는 자리별 적립 추천 보기에서만 쓴다 - 전체 보기면 표시 순서. */
  public String resolveSortForView(String sort, String view) {
    return SORT_SLOT.equals(sort) && !VIEW_CONTRIBUTION.equals(view) ? SORT_DISPLAY_ORDER : sort;
  }

  /**
   * 자리 차례 정렬. 추천은 자리마다 하나라 자리 차례가 곧 추천 차례다({@link MonthlyContributionPickSupport#pickHeld} 가 월중 먼저,
   * 그 안에서 위탁 먼저로 낸다). 방향은 없다 - 자리 차례를 거꾸로 볼 까닭이 없다.
   *
   * @param slotOrder 자리 차례대로 놓인 추천 종목코드
   */
  public List<MonthlyEtfRowView> sortRows(
      List<MonthlyEtfRowView> rows, String sort, String direction, List<String> slotOrder) {
    if (!SORT_SLOT.equals(sort)) {
      return sortRows(rows, sort, direction);
    }
    return rows.stream()
        .sorted(
            Comparator.comparingInt(
                    (MonthlyEtfRowView row) -> {
                      int index = slotOrder.indexOf(safeText(row.stockItemSymbol()));
                      return index < 0 ? Integer.MAX_VALUE : index;
                    })
                .thenComparing(
                    row -> safeText(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER))
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
          case SORT_MAX_DRAWDOWN -> nullsLast(MonthlyEtfRowView::maxDrawdownPct);
          case SORT_VOLATILITY -> nullsLast(MonthlyEtfRowView::volatilityPct);
          case SORT_PERIOD_TOTAL -> nullsLast(MonthlyEtfRowView::periodTotalReturnPct);
          case SORT_EXPENSE_RATIO -> nullsLast(MonthlyEtfRowView::totalExpenseRatioPct);
          case SORT_SCORE -> nullsLast(MonthlyEtfViewSupport::scoreOf);
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

  /**
   * 한 행의 점수 = 연배당 수익률 + min(분배금 추세, 0). "지금 눈여겨볼 종목" 과 같은 규칙이라 추세를 모르는 종목(지급 이력 6 회 미만)은 점수를 내지
   * 않는다(null) - 추세를 모르면 감점할 수 없어 삭감 중인 종목이 높게 나온다.
   */
  public static BigDecimal scoreOf(MonthlyEtfRowView row) {
    if (row == null || row.annualYieldPct() == null || row.payoutTrendPct() == null) {
      return null;
    }
    return MonthlyContributionPickSupport.score(row.annualYieldPct(), row.payoutTrendPct());
  }

  /** 고른 기간의 값이 없는가(이력이 모자란 종목). 기간 정렬에서만 뜻이 있다. */
  private boolean periodValueMissing(MonthlyEtfRowView row, String sort) {
    if (SORT_PERIOD_PRICE.equals(sort)) {
      return row.periodPriceReturnPct() == null;
    }
    if (SORT_PERIOD_TOTAL.equals(sort)) {
      return row.periodTotalReturnPct() == null;
    }
    if (SORT_MAX_DRAWDOWN.equals(sort)) {
      return row.maxDrawdownPct() == null;
    }
    if (SORT_VOLATILITY.equals(sort)) {
      return row.volatilityPct() == null;
    }
    // 점수를 못 내는 종목(지급 이력 6 회 미만이라 추세가 없거나 시세가 없는 종목)은 방향과 무관하게 뒤로.
    if (SORT_SCORE.equals(sort)) {
      return scoreOf(row) == null;
    }
    // 총보수를 모르는 종목이 "가장 싼(0%)" 으로 맨 위에 오면 안 된다 - 방향과 무관하게 뒤로.
    if (SORT_EXPENSE_RATIO.equals(sort)) {
      return row.totalExpenseRatioPct() == null;
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
   * 계좌 대상이 맞는가.
   *
   * <p>과세표준 비중을 모르는 종목은 어느 계좌인지도 모른다 - 전체로 볼 때만 남긴다. 모르는 것을 위탁으로 치면 "10% 이내" 라는 근거 없이 목록에 섞인다.
   */
  private boolean matchesAccount(MonthlyEtfRowView row, String account) {
    if (ACCOUNT_ALL.equals(account)) {
      return true;
    }

    return account.equals(
        monthlyContributionPickSupport.accountOf(row.averageTaxableBaseRatio1y()));
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
