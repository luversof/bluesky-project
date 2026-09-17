package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import net.luversof.web.gate.stock.dto.response.StockCashFlowResponse;

/**
 * 보유 기간과 연평균 수익률, 그리고 배당이 평가손실을 덮은 비율.
 *
 * <p>2026-09-14 까지 자산 현황에는 "배당이 평가손실을 얼마나 메웠나" 카드가 따로 있었다. 그 카드가 찍던 평가손익 · 누적배당 · 합산손익은 바로 아래 표에 이미
 * 열로 다 있었고, 막대는 줄 안에서만 견주는 정규화(큰 쪽이 100)라 <b>둘 중 하나가 항상 100%</b> 였다 &mdash; 막대 쌍이 나르는 정보는 비율 하나뿐이고
 * 그 비율은 옆 배지에 숫자로 적혀 있었다. 그래서 카드를 지우고, 고유했던 값(상쇄율)과 새 값(보유 기간 · 연평균)을 각각 자기 값이 있는 칸 안으로 옮겼다.
 *
 * <p><b>연평균은 XIRR</b> &mdash; 돈이 실제로 오간 날짜로 할인해 순현재가치를 0 으로 만드는 연 수익률이다(사용자 결정 2026-09-17). 그 전에는
 * {@code (1 + 합산 손익 / 보유 원가)^(365/보유일) - 1} 복리 환산이었는데, 이 식은 최초 매수일부터 지금 보유 원가 <b>전액</b>이 들어가 있었다고
 * 본다. 나눠 산 종목은 나중에 들어간 돈까지 처음부터 있었던 것으로 쳐서 낮게 나왔다(실측 2026-09-17: 삼성전자 17 번 분할 매수, 복리 환산 23.6% vs
 * XIRR <b>31.8%</b>; RISE 코리아밸류업 -20.4% vs -45.9%). 한 번에 사서 들고만 있는 종목은 두 식이 같다.
 *
 * <p>흐름은 api-stock 이 종목별로 날짜마다 더해 준다(매수 -(가격 x 수량 + 수수료 + 세금) · 매도 +(가격 x 수량 - 수수료 - 세금) · 배당 +(세전
 * - 세금 - 수수료)). 여기에 오늘의 평가액을 들어온 돈으로 더한다 &mdash; 그 합은 평가손익(Net) + 실현손익(Net) + 배당과 같다(실측 2026-09-17:
 * 보유 9 종목 0 원 차이). 단순 환산({@link StockYieldUtil})은 기간 1 회 수익인 배당수익률용이라 여러 해를 들고 있는 종목에 쓰면 과대평가된다.
 *
 * <p>보유 9 종목 중 7 종목이 1 년 미만(117~361 일)이라 연환산은 대부분 <b>확대된 값</b>이다(실측: 117 일 -6.0% &rarr; 연 -17.5%).
 * 그래서 1 년 미만인지를 {@link Row#shortTerm()} 로 알려 주고, 화면은 그 줄을 흐리게 + 근거 문구를 붙인다.
 */
public final class StockHoldingReturnUtil {

  private StockHoldingReturnUtil() {}

  /** 연환산이 확대로 읽히는 경계. 이 미만이면 화면이 약하게 표시한다. */
  public static final long SHORT_TERM_DAY_COUNT = 365L;

  /**
   * 한 종목.
   *
   * @param holdingDays 최초 매수일부터 오늘까지 일수. 최초 매수일을 모르면 0
   * @param years 보유 연수(표시용)
   * @param months 연수를 뺀 나머지 개월(표시용)
   * @param annualizedPct 연평균 수익률(XIRR, %). 낼 수 없으면 널
   * @param coveragePct 누적 배당 ÷ 평가손실 (%). 평가손실이 없으면 널
   * @param shortTerm 보유 1 년 미만
   */
  public record Row(
      long holdingDays,
      int years,
      int months,
      BigDecimal annualizedPct,
      Integer coveragePct,
      boolean shortTerm) {

    /** 보유 기간을 적을 수 있는지. 최초 매수일을 모르면 화면은 이 줄을 비운다. */
    public boolean hasPeriod() {
      return holdingDays > 0;
    }

    /** 1 년이 안 돼 연수/개월로는 못 적는 경우(표시는 일수로). */
    public boolean daysOnly() {
      return years == 0 && months == 0;
    }
  }

  /**
   * @param firstBuyDate 종목의 최초 매수일(널 허용 &mdash; 모르면 기간을 안 적는다)
   * @param today 기준일(요청 존의 오늘). 지금 평가액이 들어오는 날이다
   * @param cashFlows 그 종목의 하루치 순현금흐름(api-stock). 널이면 연평균을 안 낸다
   * @param currentValue 지금 평가액. 오늘 들어온 돈으로 친다
   * @param evaluationProfit 평가 손익. 음수일 때만 상쇄율을 낸다
   * @param dividendTotal 누적 배당(세후)
   */
  public static Row of(
      LocalDate firstBuyDate,
      LocalDate today,
      List<StockCashFlowResponse> cashFlows,
      BigDecimal currentValue,
      BigDecimal evaluationProfit,
      BigDecimal dividendTotal) {
    long days = 0L;
    int years = 0;
    int months = 0;
    if (firstBuyDate != null && today != null && !firstBuyDate.isAfter(today)) {
      days = ChronoUnit.DAYS.between(firstBuyDate, today);
      Period period = Period.between(firstBuyDate, today);
      years = period.getYears();
      months = period.getMonths();
    }
    return new Row(
        days,
        years,
        months,
        // 기간을 모르면(최초 매수일 없음 · 오늘 산 종목) 연평균도 적지 않는다 - 하루도 안 된 수익을 해로 펼치면 뜻 없는 큰 수가 된다.
        days > 0 ? annualizedPct(cashFlows, today, currentValue) : null,
        coveragePct(evaluationProfit, dividendTotal),
        days > 0 && days < SHORT_TERM_DAY_COUNT);
  }

  /** 근을 찾는 구간의 경계(연 수익률). 흐름의 부호가 여러 번 바뀌면 근이 여럿일 수 있어 촘촘히 나눠 부호가 바뀌는 구간을 모두 본다. */
  private static final double[] RATE_GRID = {
    -0.9999d, -0.999d, -0.99d, -0.95d, -0.9d, -0.8d, -0.6d, -0.4d, -0.2d, -0.1d, 0d, 0.1d, 0.2d,
    0.5d, 1d, 2d, 5d, 10d, 100d, 1e3d, 1e4d, 1e6d
  };

  /**
   * 연평균 수익률(XIRR, 소수 한 자리).
   *
   * <p>흐름 + 오늘의 평가액으로 {@code sum(금액 / (1 + r)^(첫 흐름부터 일수 / 365)) = 0} 인 r 을 찾는다. 들어온 돈과 나간 돈이 둘 다
   * 있어야 근이 있다 &mdash; 나간 돈만 있으면(평가액 0 · 받은 것 없음) 널이다. 억지로 -100% 를 적으면 "딱 전액 손실" 로 읽힌다. 근이 여럿이면(샀다
   * 팔았다 다시 산 종목) 0 에 가장 가까운 것을 쓴다(엑셀 XIRR 이 0.1 에서 출발해 가까운 근으로 가는 것과 같은 선택).
   */
  static BigDecimal annualizedPct(
      List<StockCashFlowResponse> cashFlows, LocalDate today, BigDecimal currentValue) {
    if (cashFlows == null || today == null) {
      return null;
    }
    TreeMap<LocalDate, Double> byDate = new TreeMap<>();
    for (StockCashFlowResponse flow : cashFlows) {
      // 오늘 뒤의 흐름은 아직 오간 돈이 아니다(미래 날짜로 적힌 기록).
      if (flow == null
          || flow.date() == null
          || flow.amount() == null
          || flow.date().isAfter(today)) {
        continue;
      }
      byDate.merge(flow.date(), flow.amount().doubleValue(), Double::sum);
    }
    if (currentValue != null && currentValue.signum() != 0) {
      byDate.merge(today, currentValue.doubleValue(), Double::sum);
    }
    if (byDate.isEmpty()) {
      return null;
    }
    LocalDate first = byDate.firstKey();
    double[] years = new double[byDate.size()];
    double[] amounts = new double[byDate.size()];
    boolean hasIn = false;
    boolean hasOut = false;
    int index = 0;
    for (var entry : byDate.entrySet()) {
      years[index] = ChronoUnit.DAYS.between(first, entry.getKey()) / 365d;
      amounts[index] = entry.getValue();
      hasIn |= amounts[index] > 0d;
      hasOut |= amounts[index] < 0d;
      index++;
    }
    if (!hasIn || !hasOut) {
      return null;
    }
    Double rate = null;
    for (int i = 0; i + 1 < RATE_GRID.length; i++) {
      double low = RATE_GRID[i];
      double high = RATE_GRID[i + 1];
      double lowValue = npv(low, years, amounts);
      double highValue = npv(high, years, amounts);
      if (!Double.isFinite(lowValue)
          || !Double.isFinite(highValue)
          || (lowValue > 0d) == (highValue > 0d)) {
        continue;
      }
      double root = bisect(low, high, lowValue, years, amounts);
      if (rate == null || Math.abs(root) < Math.abs(rate)) {
        rate = root;
      }
    }
    if (rate == null || !Double.isFinite(rate)) {
      return null;
    }
    return BigDecimal.valueOf(rate * 100d).setScale(1, RoundingMode.HALF_UP);
  }

  /** 순현재가치. 거듭제곱 대신 exp(-t * log1p(r)) 로 계산해 큰 r 에서 넘치지 않게 한다. */
  private static double npv(double rate, double[] years, double[] amounts) {
    double log = Math.log1p(rate);
    double sum = 0d;
    for (int i = 0; i < years.length; i++) {
      sum += amounts[i] * Math.exp(-years[i] * log);
    }
    return sum;
  }

  private static double bisect(
      double low, double high, double lowValue, double[] years, double[] amounts) {
    for (int step = 0; step < 200 && high - low > 1e-12 * Math.max(1d, Math.abs(low)); step++) {
      double middle = (low + high) / 2d;
      double middleValue = npv(middle, years, amounts);
      if (middleValue == 0d) {
        return middle;
      }
      if ((middleValue > 0d) == (lowValue > 0d)) {
        low = middle;
        lowValue = middleValue;
      } else {
        high = middle;
      }
    }
    return (low + high) / 2d;
  }

  /**
   * 종목별로 온 흐름을 한 목록으로 합친다 &mdash; 계좌처럼 여러 종목을 가로지르는 연평균에 쓴다(사용자 선택 2026-09-17: 계좌 상세에도 연평균 카드). 같은
   * 날의 흐름은 {@link #annualizedPct} 가 더한다. 널 목록 · 날짜나 금액이 없는 항목은 뺀다.
   */
  public static List<StockCashFlowResponse> combine(
      Map<UUID, List<StockCashFlowResponse>> cashFlowsByStockItem) {
    if (cashFlowsByStockItem == null) {
      return List.of();
    }
    return cashFlowsByStockItem.values().stream()
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(flow -> flow != null && flow.date() != null && flow.amount() != null)
        .sorted(Comparator.comparing(StockCashFlowResponse::date))
        .toList();
  }

  /** 흐름 중 가장 이른 날(계좌라면 최초 매수일 &mdash; 사기 전에는 팔거나 배당을 받을 수 없다). 흐름이 없으면 널. */
  public static LocalDate firstDate(List<StockCashFlowResponse> cashFlows) {
    if (cashFlows == null) {
      return null;
    }
    return cashFlows.stream()
        .filter(flow -> flow != null && flow.date() != null)
        .map(StockCashFlowResponse::date)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  /** 누적 배당이 평가손실의 몇 %를 덮었는지. 평가손실이 없으면 널(덮을 것이 없다). */
  static Integer coveragePct(BigDecimal evaluationProfit, BigDecimal dividendTotal) {
    if (evaluationProfit == null || evaluationProfit.signum() >= 0) {
      return null;
    }
    BigDecimal dividend = dividendTotal == null ? BigDecimal.ZERO : dividendTotal;
    if (dividend.signum() <= 0) {
      return 0;
    }
    return dividend
        .multiply(BigDecimal.valueOf(100))
        .divide(evaluationProfit.abs(), 0, RoundingMode.HALF_UP)
        .intValue();
  }
}
