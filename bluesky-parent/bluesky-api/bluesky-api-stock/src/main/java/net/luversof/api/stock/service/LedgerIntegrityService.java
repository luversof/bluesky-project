package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import net.luversof.api.stock.web.dto.response.LedgerIntegrityFinding;
import net.luversof.api.stock.web.dto.response.LedgerIntegrityResponse;

/**
 * 원장에서 산술적으로 불가능한 기록을 찾는다.
 *
 * <p>이 앱의 원장은 증권사 화면을 사람이 옮겨 담은 것이라 실제로 잘못된 값이 들어와 있다(실측 2026-08-23: 배당 193 건 중 8 건이 세금 &gt; 과세표준
 * &mdash; KODEX 한국부동산리츠인프라. 기록된 과세표준이 정확히 "주당 세전 x 77 주"(29x77=2,233, 30x77=2,310)라 기록 수량 10,256 주와
 * 맞지 않는다. 8 건 모두 세율 기준 추정치의 1/84.9 이고 합계로는 1,512,500 원 과소 계상이다). 화면은 그 값을 그대로 더할 뿐이라 사용자가 스스로 눈치채지
 * 못하면 잘못된 값이 계속 합계에 섞인다.
 *
 * <p>규칙은 <b>도메인 지식이 필요 없는 산술 모순</b>만 넣는다. 예를 들어 "개별주식 매도인데 증권거래세가 0" 은 실제 문제지만(실측 12 건) ETF 인지를 알아야
 * 판정할 수 있고, 종목 태그가 21 종목에서 빠져 있어 그대로 쓰면 오탐이 난다. 잘못 울리는 경고는 아예 없는 것보다 나쁘므로 여기서는 다루지 않는다.
 *
 * <p>실측 기준선(2026-08-22, 배당 193 / 매매 250): 아래 11 개 규칙 중 {@code DIVIDEND_TAX_EXCEEDS_TAXABLE} 만 8 건이고
 * 나머지는 모두 0 건이다.
 */
@Service
public class LedgerIntegrityService {

  /**
   * 계좌 전체에 수수료가 없다고 볼 최소 거래 건수.
   *
   * <p>거래가 한두 건이면 공모주 청약만 있는 계좌일 수 있어 우연이다. 실측 2026-08-23 기준 이 원장에서 문제가 된 계좌는 26 건이고, 수수료가 붙는 계좌 중
   * 가장 작은 것도 25 건이다.
   */
  private static final int MIN_TRADES_FOR_FEELESS_ACCOUNT = 10;

  /** 예시로 담을 최대 건수. 전체를 실으면 응답 크기가 원장 크기를 따라간다. */
  /**
   * 기본 예시 개수. 관리 화면의 경고 블록이 길어지지 않을 만큼만 보여 준다.
   *
   * <p>다만 이 값만으로는 <b>조치할 수 없는</b> 발견이 생긴다 &mdash; 실측 2026-08-23: 발견 45 건 중 25 건(55%)이 예시 밖이라 화면에서
   * 어느 행인지 볼 수 없었다(수수료·거래세가 없는 매도 12 건 중 9 건 등). 그래서 호출자가 더 달라고 할 수 있게 했다.
   */
  private static final int DEFAULT_MAX_EXAMPLES = 3;

  /** 예시 개수의 상한. 응답이 무한정 커지지 않게 한다. */
  static final int MAX_EXAMPLES_LIMIT = 100;

  private static final ZoneId MARKET_ZONE_ID = ZoneId.of("Asia/Seoul");

  /**
   * 배당소득 원천징수 세율(소득세 14% + 지방소득세 1.4%). 과세표준에 이 비율을 곱한 값이 세금이다.
   *
   * <p>이 사용자의 원장에서도 그대로 확인된다 &mdash; 실측 2026-08-23: 세금과 과세표준이 모두 있는 배당 중 36 건이 정확히 15.40%, 4 건이
   * 15.39%, 2 건이 15.34%(반올림 차)였다.
   *
   * <p>같은 값의 상수가 이 클래스에 두 개 있었다(WITHHOLDING_RATE / DIVIDEND_WITHHOLDING_RATE). 세율이 바뀌면 한쪽만 고쳐질 자리라
   * 하나로 합쳤다(2026-08-23).
   */
  private static final BigDecimal WITHHOLDING_RATE = new BigDecimal("0.154");

  /** 참조 과세비율 이상치 판정에 필요한 최소 이력 수. 이보다 적으면 중앙값이 신뢰할 만하지 않다. */
  private static final int MIN_REFERENCE_ROWS_FOR_RATIO_MEDIAN = 6;

  /** 종목 중앙값이 이 값을 넘으면 원래 과세비율이 높은 종목(리츠형)이라 대상에서 뺀다. */
  private static final BigDecimal REFERENCE_TAXABLE_RATIO_ITEM_MEDIAN_CAP = new BigDecimal("50");

  /** 이 비율 이상이면 "배당금을 그대로 복사" 로 본다. */
  private static final BigDecimal REFERENCE_TAXABLE_RATIO_OUTLIER_FLOOR = new BigDecimal("95");

  /**
   * 계좌 수수료율 중앙값을 믿을 만큼의 최소 거래 건수. 한두 건이면 중앙값이 곧 그 값이라 의미가 없다.
   *
   * <p>이 중앙값은 <b>전체 이력</b>에서 뽑는다. 지급 지연 문턱은 같은 방식이 문제가 됐지만(주기가 분기→월로 바뀐 종목에서 옛 간격이 기준으로 남았다, {@code
   * DataStatusController.widestRecentGapDays} 참고) 수수료율은 그렇지 않다는 것을 확인했다 &mdash; 실측 2026-08-23: 계좌 5
   * 개 모두 전체 중앙값과 최근 12 건 중앙값이 소수 6 자리에서 거의 같고(예: 0.004181% vs 0.004184%), 이상치 배수도 152.5x vs 152.4x
   * 로 판정이 바뀌지 않는다. 요율은 0.0034~0.0045% 사이에서만 오르내려 구조적 전환이 없다.
   */
  private static final int MIN_TRADES_FOR_FEE_RATE_MEDIAN = 5;

  /**
   * 계좌 중앙값 대비 몇 배부터 이상으로 볼지.
   *
   * <p>실측 2026-08-23: 정상 계좌 4 개의 최대치가 1.0~1.2 배였고 이상 건은 152.5 배였다. 그 사이가 아주 넓어서 배수 선택이 결과를 가르지 않는다.
   * 10 배로 둔다.
   */
  private static final BigDecimal FEE_RATE_OUTLIER_MULTIPLE = BigDecimal.TEN;

  /**
   * 두 출처의 과세표준 비율이 이만큼(%p) 넘게 벌어지면 알린다.
   *
   * <p>같은 배당이라도 원천징수액은 원 단위로 반올림되므로 소수점 차이는 늘 난다. 실측 2026-08-23 기준 두 출처가 일치하는 종목은 0.04%p 차이였고, 어긋나는
   * 것은 10%p 이상이라 그 사이면 어디로 잡아도 결과가 같다.
   */
  private static final BigDecimal TAXABLE_RATIO_TOLERANCE_POINTS = new BigDecimal("5");

  private final TradeService tradeService;
  private final DividendService dividendService;
  private final StockItemRepository stockItemRepository;
  private final AccountRepository accountRepository;
  private final MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;
  private final MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  public LedgerIntegrityService(
      TradeService tradeService,
      DividendService dividendService,
      StockItemRepository stockItemRepository,
      AccountRepository accountRepository,
      MonthlyDividendPayoutRepository monthlyDividendPayoutRepository,
      MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository) {
    this.tradeService = tradeService;
    this.dividendService = dividendService;
    this.stockItemRepository = stockItemRepository;
    this.accountRepository = accountRepository;
    this.monthlyDividendPayoutRepository = monthlyDividendPayoutRepository;
    this.monthlyDividendSnapshotRepository = monthlyDividendSnapshotRepository;
  }

  static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  static boolean positive(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }

  static boolean negative(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) < 0;
  }

  /** 장이 서지 않는 요일인지. 원장 날짜는 전부 자정(UTC)이라 시장 존으로 환산해도 날짜가 그대로다. */
  private static boolean isWeekend(Instant instant) {
    DayOfWeek dayOfWeek = instant.atZone(MARKET_ZONE_ID).getDayOfWeek();
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
  }

  private static String weekdayLabel(Instant instant) {
    return instant
        .atZone(MARKET_ZONE_ID)
        .getDayOfWeek()
        .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
  }

  /**
   * 행 단위 색인. 한 행이 여러 규칙에 걸리는 것을 모아 둔다.
   *
   * <p>실측 2026-08-23: 발견 48 건이 실제로는 30 행이고, 그중 <b>10 행이 사유를 2~4 개씩</b> 달고 있었다. KODEX 한국부동산리츠인프라 배당
   * 8 행이 전부 여기 속하며 한 행은 4 가지(과세표준 3 종 + 예상 과세비율)에 걸렸다. 규칙별로만 보여 주면 사용자가 같은 행을 규칙 그룹마다 다시 만나고, 그 8
   * 행이 사실 한 가지 원인(과세표준을 옛 수량 77 주로 잡은 것)이라는 것도 드러나지 않는다.
   */
  static final class RowIndex {

    private final Map<String, Set<String>> codesByRow = new java.util.LinkedHashMap<>();
    private final Map<String, String> accountByRow = new java.util.LinkedHashMap<>();

    void add(String rowKey, String code, String accountName) {
      codesByRow.computeIfAbsent(rowKey, key -> new java.util.LinkedHashSet<>()).add(code);
      if (accountName != null) {
        accountByRow.putIfAbsent(rowKey, accountName);
      }
    }

    int size() {
      return codesByRow.size();
    }

    /** 사유가 둘 이상인 행만, 사유가 많은 순 &rarr; 날짜 내림차순으로. */
    List<LedgerIntegrityResponse.RowFindingSummary> multiReasonRows() {
      List<LedgerIntegrityResponse.RowFindingSummary> rows = new ArrayList<>();
      for (var entry : codesByRow.entrySet()) {
        if (entry.getValue().size() < 2) {
          continue;
        }
        String[] parts = entry.getKey().split("\\|", 2);
        rows.add(
            new LedgerIntegrityResponse.RowFindingSummary(
                parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null,
                parts.length > 1 ? parts[1] : null,
                accountByRow.get(entry.getKey()),
                List.copyOf(entry.getValue())));
      }
      rows.sort(
          Comparator.comparingInt(
                  (LedgerIntegrityResponse.RowFindingSummary row) -> row.codes().size())
              .reversed()
              .thenComparing(
                  LedgerIntegrityResponse.RowFindingSummary::date,
                  Comparator.nullsLast(Comparator.reverseOrder())));
      return rows;
    }
  }

  private static BigDecimal medianOf(List<BigDecimal> values) {
    List<BigDecimal> sorted = new ArrayList<>(values);
    sorted.sort(java.util.Comparator.naturalOrder());
    int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
      return sorted.get(middle);
    }
    return sorted
        .get(middle - 1)
        .add(sorted.get(middle))
        .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
  }

  private static String day(Instant instant) {
    return instant == null ? null : instant.atZone(MARKET_ZONE_ID).toLocalDate().toString();
  }

  public LedgerIntegrityResponse check(UUID userId) {
    return check(userId, DEFAULT_MAX_EXAMPLES);
  }

  /**
   * @param maxExamples 규칙마다 담을 예시 개수. 1 미만이면 기본값, {@link #MAX_EXAMPLES_LIMIT} 를 넘으면 그 값으로 자른다.
   */
  public LedgerIntegrityResponse check(UUID userId, int maxExamples) {
    int exampleLimit =
        maxExamples < 1 ? DEFAULT_MAX_EXAMPLES : Math.min(maxExamples, MAX_EXAMPLES_LIMIT);
    return checkInternal(userId, exampleLimit);
  }

  private LedgerIntegrityResponse checkInternal(UUID userId, int maxExamples) {
    // 한 행이 여러 규칙에 걸린다. 화면이 "45건" 만 보면 할 일이 실제보다 커 보이므로
    // 서로 다른 행 수를 함께 낸다. 예시 상한과 무관하게 <b>모든</b> 적중을 센다.
    RowIndex distinctRows = new RowIndex();
    // 계좌별 집계: 계좌명 -> (발견 수, 서로 다른 행 키). 규칙 헬퍼가 채운다.
    Map<String, long[]> accountFindingCounts = new java.util.LinkedHashMap<>();
    Map<String, Set<String>> accountDistinctRows = new java.util.LinkedHashMap<>();
    Map<UUID, String> accountNames = new HashMap<>();
    for (var account : accountRepository.findByUserId(userId)) {
      accountNames.put(account.getId(), account.getName());
    }

    List<Trade> trades = tradeService.findByUserId(userId);
    DividendSearchRequest dividendRequest = new DividendSearchRequest();
    dividendRequest.setUserId(userId);
    List<Dividend> dividends = dividendService.findDividends(dividendRequest);

    Map<UUID, String> names = new HashMap<>();
    for (StockItem stockItem : stockItemRepository.findAll()) {
      names.put(stockItem.getId(), stockItem.getName());
    }

    List<LedgerIntegrityFinding> findings = new ArrayList<>();

    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_TAX_EXCEEDS_TAXABLE",
        d -> positive(d.getTaxableAmount()) && nz(d.getTax()).compareTo(d.getTaxableAmount()) > 0,
        LedgerIntegrityService::taxableDetail,
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_TAXABLE_EXCEEDS_GROSS",
        d ->
            positive(d.getGrossAmount())
                && nz(d.getTaxableAmount()).compareTo(d.getGrossAmount()) > 0,
        d -> "taxable=" + nz(d.getTaxableAmount()) + ", gross=" + nz(d.getGrossAmount()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_PER_SHARE_MISMATCH",
        d ->
            d.getQuantity() != null
                && d.getQuantity() > 0
                && positive(d.getAmountPerShare())
                && nz(d.getAmountPerShare())
                        .multiply(BigDecimal.valueOf(d.getQuantity()))
                        .subtract(nz(d.getGrossAmount()))
                        .abs()
                        .compareTo(BigDecimal.ONE)
                    > 0,
        d ->
            "perShare="
                + nz(d.getAmountPerShare())
                + " x "
                + d.getQuantity()
                + " != gross="
                + nz(d.getGrossAmount()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_NEGATIVE_AMOUNT",
        d ->
            negative(d.getGrossAmount())
                || negative(d.getTax())
                || negative(d.getFee())
                || negative(d.getTaxableAmount()),
        d ->
            "gross="
                + nz(d.getGrossAmount())
                + ", tax="
                + nz(d.getTax())
                + ", fee="
                + nz(d.getFee())
                + ", taxable="
                + nz(d.getTaxableAmount()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_QUANTITY_NOT_POSITIVE",
        d -> (d.getQuantity() == null || d.getQuantity() <= 0) && positive(d.getGrossAmount()),
        d -> "quantity=" + d.getQuantity() + ", gross=" + nz(d.getGrossAmount()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 비과세 계좌인데 세금이 기록된 배당.
    //
    // 화면은 계좌 설정의 isTaxDeferred 가 켜져 있으면 "세후액 = 총액" 으로 표시한다(세금을 떼지 않는
    // 계좌라는 전제). 그런데 그 계좌의 배당에 세금이 기록돼 있으면 실제 받은 돈보다 크게 보인다.
    // 지금은 그런 건이 없지만(실측 2026-08-22: 비과세 3계좌의 기록 세금 합 0 원) 화면이 이 전제에
    // 기대고 있으므로, 전제가 깨지는 순간 알 수 있어야 한다.
    java.util.Set<UUID> taxDeferredAccountIds = new java.util.HashSet<>();
    for (var account : accountRepository.findByUserId(userId)) {
      var config = account.getJsonConfig();
      if (config != null && Boolean.TRUE.equals(config.get("isTaxDeferred"))) {
        taxDeferredAccountIds.add(account.getId());
      }
    }
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_TAX_IN_TAX_DEFERRED_ACCOUNT",
        d ->
            d.getAccountId() != null
                && taxDeferredAccountIds.contains(d.getAccountId())
                && positive(d.getTax()),
        d -> "tax=" + nz(d.getTax()) + ", gross=" + nz(d.getGrossAmount()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 참조 지급 이력의 과세비율이 그 종목 자신의 이력에서 튄다.
    //
    // 예상 과세표준은 참조의 최근 12 건 과세비율 평균으로 낸다. 그래서 참조 한 줄이 잘못 들어오면 그 종목의
    // 예상 과세표준이 통째로 틀어진다.
    //
    // "100% 면 이상" 으로 잡으면 안 된다 - 리츠·인프라형은 분배금이 거의 다 과세 대상이라 100% 가 정상이다
    // (실측 2026-08-23: TIGER 리츠부동산인프라는 57 건 전부 100%, KODEX 한국부동산리츠인프라는 28 건 중
    // 21 건이 100%). 그래서 <b>그 종목 자신의 중앙값</b>과 비교한다.
    //
    // 실측 2026-08-23: 그렇게 보면 5 건이 걸린다 - PLUS 고배당주위클리고정커버드콜 3 건(중앙값 14.0%),
    // TIGER 배당커버드콜액티브 2024-02-02(중앙값 3.5%), TIGER 코리아배당다우존스위클리커버드콜
    // 2026-02-03(중앙값 5.9%). 과세표준 칸을 못 가져와 배당금을 그대로 복사한 형태다.
    //
    // 그 대가도 쟀다. 최근 12 건 평균 과세비율이 이 이상치 때문에 부풀어 있다:
    //   PLUS 고배당주위클리고정커버드콜      25.18% -> 18.38% (6.80%p)
    //   TIGER 코리아배당다우존스위클리커버드콜 30.26% -> 22.51% (7.75%p)
    // 두 값 모두 지금 스냅샷에 저장된 값과 같다(즉 화면에 그대로 나가고 있다).
    //
    // <b>한계 - 이상치가 쌓이면 이 규칙은 그 종목에 눈먼다.</b> 판정 기준이 그 종목 자신의 중앙값이라,
    // 잘못된 행이 절반을 넘으면 중앙값이 100% 쪽으로 끌려가 대상에서 빠진다. 실측 2026-08-24 로 여유를
    // 쟀다 - 중앙값이 50% 를 넘기까지 필요한 추가 이상치 수:
    //   RISE 200위클리커버드콜 +29 / TIGER 배당커버드콜액티브 +24 / KODEX 200타겟위클리커버드콜 +19
    //   RISE 코리아밸류업위클리고정커버드콜 +10 / PLUS 고배당주위클리고정커버드콜 +9
    //   <b>TIGER 코리아배당다우존스위클리커버드콜 +5</b> (이력이 10 건뿐이라 가장 약하다)
    //
    // 그 상태가 곧 사각은 아니다. 원장에 원천징수가 찍혀 있으면 DIVIDEND_TAX_EXCEEDS_TAXABLE /
    // DIVIDEND_TAXABLE_DISAGREES_WITH_REFERENCE / DIVIDEND_TAXABLE_COMPUTED_WITH_OTHER_QUANTITY 가
    // 같은 행을 잡는다 - 실제로 KODEX 한국부동산리츠인프라(중앙값 100% 라 이 규칙 대상 밖)의 8 행이 그
    // 세 규칙에 걸려 있다. 이 규칙이 <b>유일한</b> 방어선인 경우는 원장에 세금 신호가 없는 종목
    // (과세이연 계좌만 보유하거나 그 달이 비과세였던 경우)이다.
    // 참조 표는 사용자와 무관한 전역 자료다. 그대로 내면 데이터가 하나도 없는 신규 사용자에게도 지적이
    // 나간다(실측 2026-08-23: 처음 만들었을 때 배당 0 / 매매 0 인데 지적 6 건이 나와 "신규 사용자 계약"
    // 불변식이 잡았다). 그래서 <b>이 사용자가 보는 종목</b>으로 좁힌다 - 예상 과세표준이 틀어지는 것도 그
    // 종목들뿐이다.
    Set<UUID> userMonthlyDividendItemIds = new java.util.HashSet<>();
    for (var snapshot :
        monthlyDividendSnapshotRepository.findByUserIdOrderByUpdatedDateDesc(userId)) {
      if (snapshot.getStockItemId() != null) {
        userMonthlyDividendItemIds.add(snapshot.getStockItemId());
      }
    }

    Map<UUID, List<BigDecimal>> referenceRatiosByItem = new java.util.LinkedHashMap<>();
    List<MonthlyDividendPayout> referenceRows = new ArrayList<>();
    for (var payout : monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc()) {
      if (payout.getStockItemId() == null || payout.getPayDate() == null) {
        continue;
      }
      if (!userMonthlyDividendItemIds.contains(payout.getStockItemId())) {
        continue;
      }
      BigDecimal perShare = nz(payout.getDividendAmountPerShare());
      if (perShare.signum() <= 0 || payout.getTaxableBasePerShare() == null) {
        continue;
      }
      referenceRows.add(payout);
      referenceRatiosByItem
          .computeIfAbsent(payout.getStockItemId(), key -> new ArrayList<>())
          .add(
              nz(payout.getTaxableBasePerShare())
                  .multiply(BigDecimal.valueOf(100))
                  .divide(perShare, 4, RoundingMode.HALF_UP));
    }

    List<LedgerIntegrityFinding.Example> ratioOutliers = new ArrayList<>();
    int ratioOutlierCount = 0;
    for (MonthlyDividendPayout payout : referenceRows) {
      List<BigDecimal> ratios = referenceRatiosByItem.get(payout.getStockItemId());
      if (ratios == null || ratios.size() < MIN_REFERENCE_ROWS_FOR_RATIO_MEDIAN) {
        continue;
      }
      BigDecimal median = medianOf(ratios);
      if (median.compareTo(REFERENCE_TAXABLE_RATIO_ITEM_MEDIAN_CAP) > 0) {
        // 종목 성격상 과세비율이 원래 높은 것(리츠형)은 대상이 아니다.
        continue;
      }
      BigDecimal ratio =
          nz(payout.getTaxableBasePerShare())
              .multiply(BigDecimal.valueOf(100))
              .divide(nz(payout.getDividendAmountPerShare()), 4, RoundingMode.HALF_UP);
      if (ratio.compareTo(REFERENCE_TAXABLE_RATIO_OUTLIER_FLOOR) < 0) {
        continue;
      }
      ratioOutlierCount++;
      String rowKey = payout.getPayDate() + "|" + names.get(payout.getStockItemId());
      distinctRows.add(rowKey, "REFERENCE_TAXABLE_RATIO_OUTLIER", null);
      if (ratioOutliers.size() < maxExamples) {
        ratioOutliers.add(
            new LedgerIntegrityFinding.Example(
                payout.getPayDate().toString(),
                names.get(payout.getStockItemId()),
                "참조 과세비율="
                    + ratio.setScale(2, RoundingMode.HALF_UP)
                    + "% (이 종목 중앙값 "
                    + median.setScale(2, RoundingMode.HALF_UP)
                    + "%), 주당 배당 "
                    + nz(payout.getDividendAmountPerShare())
                    + " / 주당 과세표준 "
                    + nz(payout.getTaxableBasePerShare())
                    + " - 과세표준 칸을 다시 가져올 것"));
      }
    }
    if (ratioOutlierCount > 0) {
      findings.add(
          new LedgerIntegrityFinding(
              "REFERENCE_TAXABLE_RATIO_OUTLIER", ratioOutlierCount, ratioOutliers));
    }

    // 배당을 받았는데 그 달의 참조 지급 이력이 없다.
    //
    // 월배당 참조(지급 이력)는 사람이 발행사 사이트에서 가져와야 하는 자료다. 그게 밀리면 예상 월배당의
    // 기준(최신 1개월)·12 개월 평균의 창·"다가올 배당"의 예상 지급일이 함께 어긋난다.
    //
    // 데이터 상태 화면에 이미 "밀림" 표시가 있지만 그건 <b>주기 추정</b>이다(경과일 > 그 종목의 과거 최대
    // 간격). 이 규칙은 <b>증거</b>로 판정한다 - 사용자 원장에 그 달 배당이 실제로 들어와 있으면 그 달의 참조
    // 행은 반드시 있어야 한다. 펀드가 한 달 거른 경우를 밀림으로 오판하지 않고, 주기 안이라 조용히 넘어가는
    // 누락도 잡는다.
    //
    // 실측 2026-08-23: 4 건이며 주기 추정이 짚은 4 종목과 정확히 같다 - KODEX 200타겟위클리커버드콜,
    // KODEX 한국부동산리츠인프라, PLUS 고배당주위클리고정커버드콜, RISE 코리아밸류업위클리고정커버드콜의
    // 2026-08 이다(넷 다 참조 마지막이 2026-07-20). 참조가 있는 8 종목 중 나머지 4 종목은 빠진 달이 없다.
    Set<String> referencedMonths = new java.util.HashSet<>();
    Set<UUID> referencedStockItemIds = new java.util.HashSet<>();
    for (var payout : monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc()) {
      if (payout.getStockItemId() == null || payout.getPayDate() == null) {
        continue;
      }
      referencedStockItemIds.add(payout.getStockItemId());
      referencedMonths.add(
          payout.getStockItemId() + "@" + java.time.YearMonth.from(payout.getPayDate()));
    }
    // 이 규칙의 주어는 "이 종목의 이 달 참조가 없다" 이지 개별 배당 행이 아니다. 원장 행으로 세면 같은
    // (종목, 달)이 계좌 수만큼 부풀어 할 일이 실제보다 많아 보인다 - 실측 2026-08-23: 원장 행으로는
    // 9 건인데 자동 가져오기는 4 번이면 끝난다(KODEX 한국부동산리츠인프라 4 계좌, PLUS 고배당주위클리고정
    // 커버드콜 3 계좌, KODEX 200타겟위클리커버드콜·RISE 코리아밸류업위클리고정커버드콜 각 1 계좌).
    // 그래서 (종목, 달)로 묶어 세고, 어느 계좌들이 걸려 있는지는 상세에 적는다.
    Map<String, java.util.TreeSet<String>> missingReferenceAccounts =
        new java.util.LinkedHashMap<>();
    Map<String, Dividend> missingReferenceSample = new java.util.LinkedHashMap<>();
    for (Dividend dividend : dividends) {
      if (dividend.getStockItemId() == null || dividend.getPayDate() == null) {
        continue;
      }
      if (!referencedStockItemIds.contains(dividend.getStockItemId())) {
        continue;
      }
      java.time.YearMonth month =
          java.time.YearMonth.from(dividend.getPayDate().atZone(MARKET_ZONE_ID).toLocalDate());
      if (referencedMonths.contains(dividend.getStockItemId() + "@" + month)) {
        continue;
      }
      String key = dividend.getStockItemId() + "@" + month;
      missingReferenceSample.putIfAbsent(key, dividend);
      String accountName = accountNames.get(dividend.getAccountId());
      if (accountName != null) {
        missingReferenceAccounts
            .computeIfAbsent(key, ignored -> new java.util.TreeSet<>())
            .add(accountName);
      } else {
        missingReferenceAccounts.computeIfAbsent(key, ignored -> new java.util.TreeSet<>());
      }
    }
    if (!missingReferenceSample.isEmpty()) {
      List<LedgerIntegrityFinding.Example> missingReferenceExamples = new ArrayList<>();
      for (var entry : missingReferenceSample.entrySet()) {
        Dividend sample = entry.getValue();
        String rowKey = day(sample.getPayDate()) + "|" + names.get(sample.getStockItemId());
        distinctRows.add(rowKey, "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH", null);
        if (missingReferenceExamples.size() >= maxExamples) {
          continue;
        }
        java.util.TreeSet<String> accounts = missingReferenceAccounts.get(entry.getKey());
        missingReferenceExamples.add(
            new LedgerIntegrityFinding.Example(
                day(sample.getPayDate()),
                names.get(sample.getStockItemId()),
                "원장에는 "
                    + java.time.YearMonth.from(
                        sample.getPayDate().atZone(MARKET_ZONE_ID).toLocalDate())
                    + " 배당이 있는데 참조 지급 이력에 그 달이 없다 (자동 가져오기 필요"
                    // 계좌를 모르는 행만 있으면 "계좌 0곳: " 같은 빈 꼬리가 남는다.
                    + (accounts.isEmpty()
                        ? ""
                        : ", 계좌 " + accounts.size() + "곳: " + String.join(", ", accounts))
                    + ")"));
      }
      findings.add(
          new LedgerIntegrityFinding(
              "MONTHLY_DIVIDEND_REFERENCE_MISSING_MONTH",
              missingReferenceSample.size(),
              missingReferenceExamples));
    }

    // 개별 주식 배당인데 원천징수가 없다.
    //
    // 국내 상장 주식의 현금배당은 예외 없이 15.4% 를 원천징수한다. 반면 국내주식형 ETF 의 분배금은
    // 재원이 매매차익이면 비과세라 세금 0 이 정상이다. 그래서 "세금 0" 만으로는 판정할 수 없고,
    // 개별 주식인지 ETF 인지를 가려야 한다.
    //
    // 그 구분을 원장에서 끌어낸다 - 증권거래세는 개별 주식 매도에만 붙고 ETF 매도에는 붙지 않는다.
    // 실측 2026-08-23 로 검증: 매도 거래세가 붙은 종목 6 개(HK이노엔·NAVER·SK텔레콤·삼성SDI·
    // 삼성전자·에스디바이오센서)는 전부 개별 주식이고, 매도가 있는데 거래세가 0 인 종목 4 개
    // (KODEX 한국부동산리츠인프라·PLUS 자사주매입고배당주·TIGER 리츠부동산인프라·
    // TIGER 코리아배당다우존스)는 전부 ETF 다. 오분류 0.
    //
    // 그렇게 가른 개별 주식의 배당 36 건 중 35 건이 15.34~15.40% 로 원천징수돼 있었고, 남은 한 건이
    // 이 규칙이 잡는 것이다 - HK이노엔 2022-04-22 세전 3,840 원에 세금 0(15.4% 면 591 원).
    //
    // 한 번도 판 적이 없는 종목은 이 방법으로 가릴 수 없어 대상에서 빠진다(하나금융지주가 그렇고,
    // 그 건은 DIVIDEND_WITHOUT_TRADE 가 이미 잡는다).
    java.util.Set<UUID> individualStockItemIds = new java.util.HashSet<>();
    for (Trade trade : trades) {
      if (trade.getType() == TradeType.SELL
          && trade.getStockItemId() != null
          && positive(trade.getTax())) {
        individualStockItemIds.add(trade.getStockItemId());
      }
    }
    dividendRule(
        findings,
        names,
        dividends,
        "STOCK_DIVIDEND_WITHOUT_WITHHOLDING",
        d ->
            d.getAccountId() != null
                && !taxDeferredAccountIds.contains(d.getAccountId())
                && d.getStockItemId() != null
                && individualStockItemIds.contains(d.getStockItemId())
                && positive(d.getGrossAmount())
                && !positive(d.getTax()),
        d ->
            "gross="
                + nz(d.getGrossAmount())
                + ", tax=0, "
                + WITHHOLDING_RATE.multiply(BigDecimal.valueOf(100)).stripTrailingZeros()
                + "% 기준 예상 세금="
                + nz(d.getGrossAmount())
                    .multiply(WITHHOLDING_RATE)
                    .setScale(0, RoundingMode.HALF_UP),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 매매 기록이 하나도 없는 종목의 배당.
    //
    // 원장상 취득한 적이 없는 주식으로 배당을 받을 수는 없다. 도메인 지식이 필요 없는 판정이고,
    // 실제로 한 건 있다(실측 2026-08-22: 하나금융지주 배당 2건·세후 2,100원, 매매 원장 0건).
    //
    // 조용히 어긋나는 곳: 손익 집계는 매매에서 파생되므로 그 종목이 아예 빠진다. 그래서 종목별
    // 시계열을 전부 더해도 전체와 배당이 2,100원 어긋난다 - 화면에서는 원인을 알 수 없다.
    java.util.Set<UUID> tradedStockItemIds = new java.util.HashSet<>();
    for (Trade trade : trades) {
      if (trade.getStockItemId() != null) {
        tradedStockItemIds.add(trade.getStockItemId());
      }
    }
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_WITHOUT_TRADE",
        d -> d.getStockItemId() != null && !tradedStockItemIds.contains(d.getStockItemId()),
        d -> "gross=" + nz(d.getGrossAmount()) + ", quantity=" + d.getQuantity(),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 배당 기준일에 그 종목을 하나도 들고 있지 않았던 배당.
    //
    // 조용히 어긋나는 곳: 배당수익률의 분모(기준일 원금)는 그 날 보유 스냅샷에서 나온다. 보유가 0 이면
    // 분모를 만들 수 없어 그 배당은 분자에서도 빠진다 - 즉 화면의 수익률이 대표하지 못하는 금액이
    // 생기는데, 합계만 보면 드러나지 않는다(실측 2026-08-23: 3 건 142,260 원).
    //
    // 대개는 입력 관례 때문이다 - 배당 기준일 자리에 지급일을 적으면, 기준일에는 들고 있었어도
    // 지급일에는 이미 판 종목이 여기 걸린다(실측: NAVER 2020-12 기준 배당을 2021-04-08 지급일로 기록,
    // 그 사이 2021-01-18 에 전량 매도).
    //
    // 거래가 아예 없는 종목은 DIVIDEND_WITHOUT_TRADE 가 이미 알리므로 여기서는 제외한다.
    Map<UUID, java.util.List<Trade>> tradesByStockItem = new java.util.HashMap<>();
    for (Trade trade : trades) {
      if (trade.getStockItemId() != null && trade.getTradeDate() != null) {
        tradesByStockItem
            .computeIfAbsent(trade.getStockItemId(), key -> new ArrayList<>())
            .add(trade);
      }
    }
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_WITHOUT_HOLDING_ON_BASIS_DATE",
        d ->
            d.getStockItemId() != null
                && tradedStockItemIds.contains(d.getStockItemId())
                && holdingQuantityAt(tradesByStockItem.get(d.getStockItemId()), basisDate(d)) <= 0,
        d ->
            "기준일="
                + day(basisDate(d))
                + ", 배당수량="
                + d.getQuantity()
                + ", 그 날 보유="
                + holdingQuantityAt(tradesByStockItem.get(d.getStockItemId()), basisDate(d))
                + lastHeldHint(
                    tradesByStockItem.get(d.getStockItemId()), d.getQuantity(), basisDate(d)),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 배당 수량이 그때까지 한 번이라도 보유했던 최대 수량을 넘는 배당.
    //
    // 위 두 규칙은 "거래가 아예 없다"와 "그 날 보유가 0"만 본다. 보유가 있는데 <b>수량이 과하게</b> 적힌
    // 경우는 어느 쪽에도 걸리지 않는다. 그러면 그 배당의 주당 배당금과 수익률 분모가 함께 틀어진다.
    //
    // 기준일이 비어 있어 지급일로 대신하는 자료가 많으므로, "그 날 보유"가 아니라 "그때까지의 최대 보유"와
    // 견준다 - 기말배당의 기준일·지급일 시차(서너 달)를 오탐으로 만들지 않으려는 것이다.
    //
    // 실측 2026-08-24: 배당 193 건을 (계좌·종목·지급일)로 묶은 188 묶음 중, 합계 수량이 직전 400 일의
    // 어느 보유 수량과도 맞지 않는 것은 하나금융지주 2 건뿐이고 그건 DIVIDEND_WITHOUT_TRADE 가 이미
    // 잡는다. 즉 지금 이 규칙의 발견은 0 이며, 앞으로 배당을 넣을 때를 위한 그물이다.
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_QUANTITY_ABOVE_EVER_HELD",
        d ->
            d.getStockItemId() != null
                && tradedStockItemIds.contains(d.getStockItemId())
                && d.getQuantity() > 0
                && d.getQuantity()
                    > maxHoldingQuantityUntil(
                        tradesByStockItem.get(d.getStockItemId()), basisDate(d)),
        d ->
            "배당수량="
                + d.getQuantity()
                + ", 그때까지 최대 보유="
                + maxHoldingQuantityUntil(tradesByStockItem.get(d.getStockItemId()), basisDate(d)),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 매도인데 수수료와 증권거래세가 <b>둘 다</b> 0 인 거래.
    //
    // 조용히 어긋나는 곳: 매도 원가와 실현손익이 그만큼 부풀어 오른다. 화면은 그 값을 그대로 쓰므로
    // 수익이 실제보다 좋아 보인다(실측 2026-08-23: 12 건, 매도금액 합 308,553,380 원).
    //
    // ETF 매도는 증권거래세가 면제라 세금만 0 인 것은 정상이다. 그래서 세금 하나만 보면 ETF 인지를
    // 알아야 하고 종목 태그는 빠진 것이 있어 오탐이 난다. 반면 <b>수수료는 ETF 에도 붙는다</b> &mdash;
    // 실측으로 세금만 0 인 매도 13 건은 전부 수수료가 있었고(최소 1 원), 둘 다 0 인 12 건은 전부
    // 개별주식이었다. 즉 "둘 다 0" 은 종목 종류를 몰라도 판정할 수 있다.
    //
    // 초소액 매도는 수수료가 반올림으로 0 이 될 수 있으므로, 이 원장에서 수수료가 실제로 붙은 가장 작은
    // 거래(실측 12,235 원)보다 작은 매도는 제외한다. 상수를 박지 않고 원장에서 스스로 보정한다.
    BigDecimal minFeeBearingAmount = null;
    for (Trade trade : trades) {
      if (positive(trade.getFee())) {
        BigDecimal amount = tradeAmount(trade);
        if (amount != null
            && (minFeeBearingAmount == null || amount.compareTo(minFeeBearingAmount) < 0)) {
          minFeeBearingAmount = amount;
        }
      }
    }
    // 같은 해에 실제로 관측된 거래세율. 원장 자신에서만 뽑는다(세법 연혁 같은 바깥 지식을 쓰지 않는다).
    //
    // 실측 2026-08-23: 세금이 기록된 매도 29 건의 연도별 중앙값은 2020년 0.2500% · 2021년 0.2300% ·
    // 2022년 0.2299% · 2023년 0.1997% · 2025년 0.1500% · 2026년 0.2000% 로 해마다 좁게 모인다.
    // 그래서 "그 해 다른 계좌는 이만큼 냈다"는 근거로 쓸 수 있다.
    Map<Integer, BigDecimal> observedTaxRateByYear = observedSellTaxRateByYear(trades);

    // 이 하한도 전체 이력에서 뽑는다. 실측 2026-08-23: 전체 최소는 12,235 원(2025-09-03 의 그 이상치 거래)이고
    // 최근 12 건 기준은 49,080 원인데, 대상이 되는 매도 12 건의 최소 거래대금이 316,000 원이라 어느 쪽을 써도
    // 판정이 같다. 지급 지연 문턱과 달리 여기서는 창을 좁힐 이유가 없다.
    BigDecimal feeThreshold = minFeeBearingAmount;
    if (feeThreshold != null) {
      tradeRule(
          findings,
          names,
          trades,
          "SELL_WITHOUT_FEE_AND_TAX",
          t ->
              t.getType() == TradeType.SELL
                  && !positive(t.getFee())
                  && !positive(t.getTax())
                  && tradeAmount(t) != null
                  && tradeAmount(t).compareTo(feeThreshold) >= 0,
          // 계좌는 tradeRule 이 문장 끝에 붙인다. 여기서 또 붙이면 화면에 두 번 나온다
          // (실측 2026-08-24: "… 거래세=0 [동양증권] (…) [동양증권]").
          t ->
              "매도금액="
                  + tradeAmount(t).setScale(0, RoundingMode.HALF_UP)
                  + ", 수수료=0, 거래세=0"
                  + missingTaxHint(t, observedTaxRateByYear),
          maxExamples,
          distinctRows,
          accountNames,
          accountFindingCounts,
          accountDistinctRows);
    }

    // 거래가 충분히 많은데 수수료가 <b>한 건도</b> 기록되지 않은 계좌.
    //
    // 조용히 어긋나는 곳: 그 계좌의 매수 원가는 낮게, 매도 실현손익은 높게 잡힌다. 계좌 단위로 전부
    // 빠져 있으면 화면 어디에서도 이상해 보이지 않는다 - 숫자가 그냥 조금씩 좋을 뿐이다.
    //
    // 매수만 보면 판정할 수 없다. 공모주 청약은 수수료가 없는 것이 정상이라(실측 2026-08-23: 한국투자증권
    // 위탁의 수수료 0 매수 14 건은 대부분 2020~2021 공모주 - 카카오게임즈·SK바이오사이언스·HK이노엔 등)
    // 건별 규칙은 오탐이 난다. 계좌 전체가 0 이면 그 설명이 성립하지 않는다.
    //
    // 실측 2026-08-23: 동양증권 26/26 건이 수수료 0(거래대금 584,010,880 원). 나머지 계좌는 88~100% 에
    // 수수료가 붙는다. 건수 하한을 두는 이유는 거래가 한두 건뿐인 계좌라면 우연일 수 있기 때문이다.
    Map<UUID, long[]> tradeCountByAccount = new HashMap<>();
    Map<UUID, BigDecimal> tradeAmountByAccount = new HashMap<>();
    Map<UUID, Instant> firstTradeByAccount = new HashMap<>();
    for (Trade trade : trades) {
      if (trade.getAccountId() == null) {
        continue;
      }
      long[] counts = tradeCountByAccount.computeIfAbsent(trade.getAccountId(), key -> new long[2]);
      counts[0]++;
      if (positive(trade.getFee())) {
        counts[1]++;
      }
      BigDecimal amount = tradeAmount(trade);
      if (amount != null) {
        tradeAmountByAccount.merge(trade.getAccountId(), amount, BigDecimal::add);
      }
      if (trade.getTradeDate() != null) {
        firstTradeByAccount.merge(
            trade.getAccountId(), trade.getTradeDate(), (a, b) -> a.isBefore(b) ? a : b);
      }
    }
    Map<UUID, List<Trade>> tradesByAccount = new HashMap<>();
    for (Trade trade : trades) {
      if (trade.getAccountId() != null) {
        tradesByAccount.computeIfAbsent(trade.getAccountId(), key -> new ArrayList<>()).add(trade);
      }
    }
    Map<Integer, BigDecimal> observedFeeRateFloorByYear = observedFeeRateFloorByYear(trades);
    List<LedgerIntegrityFinding.Example> feelessAccounts = new ArrayList<>();
    int feelessAccountCount = 0;
    for (Map.Entry<UUID, long[]> entry : tradeCountByAccount.entrySet()) {
      long total = entry.getValue()[0];
      long withFee = entry.getValue()[1];
      if (withFee > 0 || total < MIN_TRADES_FOR_FEELESS_ACCOUNT) {
        continue;
      }
      feelessAccountCount++;
      distinctRows.add(
          day(firstTradeByAccount.get(entry.getKey())) + "|" + accountNames.get(entry.getKey()),
          "ACCOUNT_WITHOUT_ANY_FEE",
          accountNames.get(entry.getKey()));
      if (feelessAccounts.size() < maxExamples) {
        BigDecimal amount =
            tradeAmountByAccount
                .getOrDefault(entry.getKey(), BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);
        feelessAccounts.add(
            new LedgerIntegrityFinding.Example(
                day(firstTradeByAccount.get(entry.getKey())),
                accountNames.get(entry.getKey()),
                "거래="
                    + total
                    + "건, 수수료 기록=0건, 거래대금="
                    + amount
                    + missingFeeHint(
                        tradesByAccount.getOrDefault(entry.getKey(), List.of()),
                        observedFeeRateFloorByYear)));
      }
    }
    if (feelessAccountCount > 0) {
      findings.add(
          new LedgerIntegrityFinding(
              "ACCOUNT_WITHOUT_ANY_FEE", feelessAccountCount, feelessAccounts));
    }

    // 같은 배당의 과세표준을 두 곳에서 알 수 있는데 서로 다른 경우.
    //
    //   (1) 증권사가 실제로 뗀 세금 -> 세금 / 15.4% (원천징수는 세전이 아니라 과세표준의 15.4% 다)
    //   (2) 운용사 지급 이력의 주당 과세표준(사람이 가져오는 참조 데이터)
    //
    // 화면의 예상 과세표준은 (2) 를 쓰므로 (2) 가 틀리면 사용자가 신고할 금액이 부풀거나 줄어든다.
    // (1) 은 실제로 뗀 돈이라 더 단단하다.
    //
    // 이 대조법이 믿을 만하다는 근거: 개별주식(삼성전자·SK텔레콤·NAVER·삼성SDI)은 (1) 로 역산하면
    // 99.6~100.0% 가 나온다 - 배당 전액이 과세 대상인 것과 정확히 맞는다. TIGER 리츠부동산인프라는
    // (1) 99.96% vs (2) 100.00% 로 두 출처가 0.04%p 이내로 일치한다.
    //
    // 실측 2026-08-23: 지급일로 짝지은 16 건 중 9 건이 5%p 넘게 어긋났고 전부 (2) 가 더 높았다.
    //
    // 그 9 건은 원인이 둘로 갈린다.
    //   - 7 건은 참조가 <b>정확히 100%</b> 다(KODEX 한국부동산리츠인프라, 원장 역산 63.77%). 주당 과세표준
    //     자리에 주당 배당금이 그대로 들어간 모양이다. 참조 전체 202 건 중 83 건이 이 모양이고, 그 종목만
    //     보면 28 건 중 21 건이다. 다만 같은 종목에도 62.86% / 48.65% / 0% 같은 제대로 된 값이 섞여 있어,
    //     "아직 확정되지 않은 달은 배당금을 그대로 싣는다" 는 설명과 맞는다.
    //   - 2 건은 100% 가 아니다(RISE 200위클리커버드콜 2026-04-02 참조 17.14% vs 원장 6.81%,
    //     KODEX 200타겟위클리커버드콜 2026-04-17 참조 16.41% vs 원장 5.52%). 이건 다른 사정이라 개별 확인이
    //     필요하다.
    //
    // 그래서 상세에 "참조가 100% 인가"를 함께 적는다. 100% 인 건은 한꺼번에 재수집하면 되고, 아닌 건은
    // 하나씩 봐야 한다 - 그 구분이 없으면 9 건을 전부 같은 문제로 착각한다.
    Map<String, BigDecimal[]> referenceTaxable = new HashMap<>();
    for (var payout : monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc()) {
      if (payout.getStockItemId() == null || payout.getPayDate() == null) {
        continue;
      }
      referenceTaxable.putIfAbsent(
          payout.getStockItemId() + "@" + payout.getPayDate(),
          new BigDecimal[] {
            nz(payout.getDividendAmountPerShare()), nz(payout.getTaxableBasePerShare())
          });
    }
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_TAXABLE_DISAGREES_WITH_REFERENCE",
        d ->
            taxableRatioGapPoints(d, referenceTaxable) != null
                && taxableRatioGapPoints(d, referenceTaxable)
                        .abs()
                        .compareTo(TAXABLE_RATIO_TOLERANCE_POINTS)
                    > 0,
        d -> {
          BigDecimal[] reference = referenceTaxable.get(referenceKey(d));
          BigDecimal referenceRatio =
              reference[1]
                  .multiply(BigDecimal.valueOf(100))
                  .divide(reference[0], 4, RoundingMode.HALF_UP);
          return "원천징수 역산="
              + impliedTaxableRatio(d).setScale(2, RoundingMode.HALF_UP)
              + "%, 참조="
              + referenceRatio.setScale(2, RoundingMode.HALF_UP)
              + "% (주당 배당 "
              + reference[0]
              + " / 주당 과세표준 "
              + reference[1]
              + "), 참조 기준 실효세율="
              + effectiveRateOnReference(d, referenceRatio).setScale(2, RoundingMode.HALF_UP)
              + "%(정상 15.40%)"
              + storedTaxableVerdict(d, reference);
        },
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 저장된 과세표준이 그 배당의 수량이 아닌 다른 수량으로 계산된 경우.
    //
    // 과세표준은 (수량 x 주당 과세표준) 이어야 한다. 그 비를 되돌려 보면 계산에 쓰인 수량이 나온다.
    //
    // 실측 2026-08-23: KODEX 한국부동산리츠인프라 8 건(2026-01 ~ 2026-08)이 모두 과세표준 / 주당
    // 과세표준 = 77 이었다. 실제 수량은 10,256 이다. 같은 종목의 2025 년 건(16,585 / 16,650 / 76)은
    // 수량과 정확히 일치하므로, 2026-01 부터 낡은 수량 77 이 고정으로 물려 들어간 것이다.
    // 그 결과 과세표준이 2,233 원으로 남았다(원천징수 29,210 원을 15.4% 로 역산하면 189,675 원).
    //
    // 위 DIVIDEND_TAXABLE_DISAGREES_WITH_REFERENCE 는 "비율이 참조와 다르다"까지만 알려 준다.
    // 어떤 수량으로 잘못 계산됐는지 짚어 줘야 사람이 바로 고칠 수 있다.
    dividendRule(
        findings,
        names,
        dividends,
        "DIVIDEND_TAXABLE_COMPUTED_WITH_OTHER_QUANTITY",
        d ->
            taxableImpliedQuantity(d) != null && !taxableImpliedQuantity(d).equals(d.getQuantity()),
        d ->
            "과세표준 "
                + nz(d.getTaxableAmount()).toPlainString()
                + " / 주당 과세표준 "
                + nz(d.getTaxPerShare()).toPlainString()
                + " = 수량 "
                + taxableImpliedQuantity(d)
                + " 인데 이 배당의 수량은 "
                + d.getQuantity()
                + " 이다 (수량 기준이면 "
                + nz(d.getTaxPerShare())
                    .multiply(BigDecimal.valueOf(d.getQuantity()))
                    .toPlainString()
                + ")",
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 예상 배당의 과세비율이 원장의 원천징수와 맞는지.
    //
    // 스냅샷의 averageTaxableBaseRatio1y 는 최근 1 년 '지급 이력 참조'의 (주당 과세표준 / 주당 배당) 평균이다
    // (MonthlyDividendPayoutService). 그래서 참조가 틀리면 원장뿐 아니라 예상 배당 화면의 과세표준까지 같이
    // 틀린다 - 위 DIVIDEND_TAXABLE_DISAGREES_WITH_REFERENCE 만 보고 있으면 그 전파를 놓친다.
    //
    // 기준은 실제 원천징수액을 15.4% 로 역산한 과세표준이다. 세금이 실제로 붙은 배당만 쓴다(비과세 계좌나
    // 세금 0 인 건은 비율을 알 수 없다).
    //
    // 실측 2026-08-23: 8 종목 중 비교 가능한 4 종목에서 KODEX 한국부동산리츠인프라만 크게 어긋났다
    // (예상 77.64% vs 원장 역산 63.64%, 14.00%p). 그 몫이 예상 과세표준 427,120 원에 그대로 들어가 있다.
    Map<UUID, BigDecimal[]> withheldByStockItem = new HashMap<>();
    for (Dividend dividend : dividends) {
      if (dividend.getStockItemId() == null
          || !positive(dividend.getTax())
          || !positive(dividend.getGrossAmount())) {
        continue;
      }
      BigDecimal[] totals =
          withheldByStockItem.computeIfAbsent(
              dividend.getStockItemId(),
              key -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      totals[0] =
          totals[0].add(dividend.getTax().divide(WITHHOLDING_RATE, 4, RoundingMode.HALF_UP));
      totals[1] = totals[1].add(dividend.getGrossAmount());
    }
    List<LedgerIntegrityFinding.Example> forecastExamples = new ArrayList<>();
    int forecastHits = 0;
    for (var snapshot :
        monthlyDividendSnapshotRepository.findByUserIdOrderByUpdatedDateDesc(userId)) {
      BigDecimal[] totals = withheldByStockItem.get(snapshot.getStockItemId());
      if (totals == null || totals[1].signum() <= 0) {
        continue;
      }
      BigDecimal ledgerRatio =
          totals[0].multiply(BigDecimal.valueOf(100)).divide(totals[1], 2, RoundingMode.HALF_UP);
      BigDecimal forecastRatio = nz(snapshot.getAverageTaxableBaseRatio1y());
      if (forecastRatio.subtract(ledgerRatio).abs().compareTo(TAXABLE_RATIO_TOLERANCE_POINTS)
          <= 0) {
        continue;
      }
      forecastHits++;
      distinctRows.add(
          (snapshot.getAsOfDate() == null ? "" : snapshot.getAsOfDate().toString())
              + "|"
              + names.get(snapshot.getStockItemId()),
          "FORECAST_TAXABLE_RATIO_DISAGREES_WITH_WITHHOLDING",
          null);
      if (forecastExamples.size() < maxExamples) {
        forecastExamples.add(
            new LedgerIntegrityFinding.Example(
                snapshot.getAsOfDate() == null ? null : snapshot.getAsOfDate().toString(),
                names.get(snapshot.getStockItemId()),
                "예상 과세비율="
                    + forecastRatio
                    + "%, 원장 원천징수 역산="
                    + ledgerRatio
                    + "% (예상 월배당 "
                    + nz(snapshot.getAverageMonthlyDividendPerShare1y())
                        .multiply(
                            BigDecimal.valueOf(
                                snapshot.getHeldQuantity() == null
                                    ? 0
                                    : snapshot.getHeldQuantity()))
                        .setScale(0, RoundingMode.HALF_UP)
                    + "원에 이 비율이 곱해진다)"));
      }
    }
    if (forecastHits > 0) {
      findings.add(
          new LedgerIntegrityFinding(
              "FORECAST_TAXABLE_RATIO_DISAGREES_WITH_WITHHOLDING", forecastHits, forecastExamples));
    }

    // 그 계좌의 평소 수수료율에서 크게 벗어난 거래.
    //
    // 수수료율은 계좌(증권사·약정)마다 정해져 있어 거의 일정하다. 한 건만 몇십 배로 튀면 입력이 잘못된
    // 것이다. 기준은 바깥에서 가져오지 않고 <b>그 계좌 자신의 중앙값</b>으로 잡는다.
    //
    // 실측 2026-08-23: 5 개 계좌 중 4 개는 최대치가 중앙값의 1.0~1.2 배에 머무는데, 한국투자증권 위탁만
    // 152.5 배인 거래가 하나 있다(2025-09-03 TIGER 코리아배당다우존스 1 주, 거래대금 12,235 · 수수료 78
    // = 0.6375%, 계좌 중앙값 0.0042%).
    //
    // "소액이라 최소수수료가 붙었다" 로는 설명되지 않는다 - 같은 계좌의 186,300 원 거래 수수료가 6 원이라
    // 78 원짜리 하한은 존재할 수 없다. 그래서 그 계좌의 최저 수수료를 함께 적어 판단 근거를 남긴다.
    Map<UUID, List<BigDecimal>> feeRatesByAccount = new HashMap<>();
    Map<UUID, BigDecimal> minFeeByAccount = new HashMap<>();
    for (Trade trade : trades) {
      BigDecimal amount = tradeAmount(trade);
      if (trade.getAccountId() == null
          || amount == null
          || amount.signum() <= 0
          || !positive(trade.getFee())) {
        continue;
      }
      feeRatesByAccount
          .computeIfAbsent(trade.getAccountId(), key -> new ArrayList<>())
          .add(
              trade
                  .getFee()
                  .multiply(BigDecimal.valueOf(100))
                  .divide(amount, 6, RoundingMode.HALF_UP));
      minFeeByAccount.merge(trade.getAccountId(), trade.getFee(), BigDecimal::min);
    }
    Map<UUID, BigDecimal> medianFeeRate = new HashMap<>();
    for (var entry : feeRatesByAccount.entrySet()) {
      if (entry.getValue().size() < MIN_TRADES_FOR_FEE_RATE_MEDIAN) {
        continue;
      }
      List<BigDecimal> sortedRates = new ArrayList<>(entry.getValue());
      sortedRates.sort(Comparator.naturalOrder());
      BigDecimal median = sortedRates.get(sortedRates.size() / 2);
      if (median.signum() > 0) {
        medianFeeRate.put(entry.getKey(), median);
      }
    }
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_FEE_RATE_FAR_ABOVE_ACCOUNT_MEDIAN",
        t ->
            feeRateMultiple(t, medianFeeRate) != null
                && feeRateMultiple(t, medianFeeRate).compareTo(FEE_RATE_OUTLIER_MULTIPLE) > 0,
        t ->
            "수수료율="
                + feeRate(t).setScale(4, RoundingMode.HALF_UP)
                + "%(거래대금 "
                + tradeAmount(t).setScale(0, RoundingMode.HALF_UP)
                + " · 수수료 "
                + nz(t.getFee()).setScale(0, RoundingMode.HALF_UP)
                + "), 이 계좌 중앙값 "
                + medianFeeRate.get(t.getAccountId()).setScale(4, RoundingMode.HALF_UP)
                + "% 의 "
                + feeRateMultiple(t, medianFeeRate).setScale(1, RoundingMode.HALF_UP)
                + "배. 이 계좌의 최저 수수료는 "
                + nz(minFeeByAccount.get(t.getAccountId())).setScale(0, RoundingMode.HALF_UP)
                + "원이라 최소수수료로는 설명되지 않는다",
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    // 한국 증시는 토·일에 열리지 않는다. 그런 날짜가 들어 있으면 입력 오류이고, 그 거래가 실제로
    // 언제였는지에 따라 기간별 손익 귀속이 달라진다.
    //
    // 실측 2026-08-23: 250 건 중 2 건 - 동양증권 한화오션 매수 2019-03-23(토) 460 주 @21,753 과
    // 매도 2019-04-21(일) 460 주 @22,850(실현손익 504,620). 두 거래 모두 수수료·거래세가 0 이라
    // 이미 ACCOUNT_WITHOUT_ANY_FEE 로도 걸리는 계좌지만, 날짜 오류는 별개 사안이라 따로 낸다.
    //
    // 공휴일은 판정하지 않는다 - 원장만으로는 휴장일 달력을 알 수 없어서, 주말처럼 확실한 것만 낸다.
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_ON_WEEKEND",
        t -> t.getTradeDate() != null && isWeekend(t.getTradeDate()),
        t ->
            "거래일="
                + day(t.getTradeDate())
                + "("
                + weekdayLabel(t.getTradeDate())
                + "), "
                + t.getQuantity()
                + "주 @"
                + nz(t.getPrice()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_NEGATIVE_FEE_OR_TAX",
        t -> negative(t.getFee()) || negative(t.getTax()),
        t -> "fee=" + nz(t.getFee()) + ", tax=" + nz(t.getTax()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_BUY_WITH_TAX",
        t -> t.getType() == TradeType.BUY && positive(t.getTax()),
        t -> "tax=" + nz(t.getTax()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_SELL_WITHOUT_REALIZED_PROFIT",
        t -> t.getType() == TradeType.SELL && t.getRealizedProfit() == null,
        t -> "quantity=" + t.getQuantity(),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    // 매수 행에 0 이 들어 있는 것은 정상 표현이다 - 조회 응답은 매수의 실현손익을 null 로 비워 보내지만
    // 저장값은 0 이다. 처음에 "null 이 아니면 이상" 으로 잡았다가 매수 196 건이 통째로 걸렸다.
    // 실제로 이상한 것은 매수인데 0 이 아닌 손익이 붙은 경우다.
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_BUY_WITH_REALIZED_PROFIT",
        t ->
            t.getType() == TradeType.BUY
                && t.getRealizedProfit() != null
                && t.getRealizedProfit().signum() != 0,
        t -> "realizedProfit=" + t.getRealizedProfit(),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_QUANTITY_NOT_POSITIVE",
        t -> t.getQuantity() <= 0,
        t -> "quantity=" + t.getQuantity(),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);
    tradeRule(
        findings,
        names,
        trades,
        "TRADE_NEGATIVE_PRICE",
        t -> negative(t.getPrice()),
        t -> "price=" + nz(t.getPrice()),
        maxExamples,
        distinctRows,
        accountNames,
        accountFindingCounts,
        accountDistinctRows);

    List<LedgerIntegrityResponse.AccountFindingSummary> accountSummary =
        accountFindingCounts.entrySet().stream()
            .map(
                entry ->
                    new LedgerIntegrityResponse.AccountFindingSummary(
                        entry.getKey(),
                        entry.getValue()[0],
                        accountDistinctRows.getOrDefault(entry.getKey(), Set.of()).size()))
            .sorted(
                Comparator.comparingLong(
                        LedgerIntegrityResponse.AccountFindingSummary::findingCount)
                    .reversed()
                    .thenComparing(LedgerIntegrityResponse.AccountFindingSummary::accountName))
            .toList();
    return new LedgerIntegrityResponse(
        dividends.size(),
        trades.size(),
        distinctRows.size(),
        accountSummary,
        distinctRows.multiReasonRows(),
        findings);
  }

  /**
   * 과세표준이 세금보다 작을 때, 무엇이 이상한지 행에서 바로 보이게 적는다.
   *
   * <p>예전에는 {@code tax=29210, taxable=2233} 처럼 두 숫자만 적었다. 사용자는 그것만 보고 어느 값을 고쳐야 할지 알 수 없다. 같은 행의 세전
   * 금액과 수량으로 <b>주당 금액</b>을 함께 적으면 원인이 드러난다 &mdash; 실측 2026-08-23: 8 건 모두 주당 세전 29~30 원인데 주당 과세표준이
   * 0.22 원이었고, 과세표준을 수량 10,256 주가 아니라 77 주로 계산한 값과 정확히 일치했다.
   *
   * <p>다만 "77 주로 계산했다" 고 단정하지는 않는다. 과세비율이 100% 가 아닌 달도 있어(이 종목의 2025-11 은 41.94%), 비율만 낮은 정상 행과 구분할
   * 근거가 행 안에는 없다. 사실만 적고 판단은 사람에게 남긴다.
   */
  /**
   * 어긋난 원인이 <b>과세표준</b>인지 <b>세금</b>인지 가른다.
   *
   * <p>저장된 과세표준을 참조(주당 과세표준 x 수량)와 직접 견주면 두 부류가 갈린다. 실측 2026-08-23 의 9 건:
   *
   * <ul>
   *   <li>7 건은 저장 과세표준이 참조와 <b>다르다</b>(KODEX 한국부동산리츠인프라: 2,233 vs 297,424). 그 과세표준으로 실효세율을 내면 1308%
   *       가 나온다 &mdash; 과세표준 쪽이 틀렸다는 뜻이다.
   *   <li>2 건은 저장 과세표준이 참조와 <b>정확히 같다</b>(RISE 200위클리커버드콜 138,420 = 3,845 x 36, KODEX 200타겟위클리커버드콜
   *       28,552 = 664 x 43). 그런데 세금이 그 과세표준의 6.12% / 5.18% 뿐이다 (정상 15.4%). 여기서는 과세표준이 아니라 세금 쪽을 봐야
   *       한다.
   * </ul>
   *
   * <p>이 구분이 없으면 9 건을 전부 "참조를 다시 가져오면 되는 문제" 로 착각한다.
   */
  private static String storedTaxableVerdict(Dividend dividend, BigDecimal[] reference) {
    BigDecimal stored = nz(dividend.getTaxableAmount());
    BigDecimal quantity =
        dividend.getQuantity() == null
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(dividend.getQuantity());
    BigDecimal referenceBase = reference[1].multiply(quantity);
    if (stored.signum() > 0
        && stored.subtract(referenceBase).abs().compareTo(BigDecimal.ONE) <= 0) {
      BigDecimal effective =
          nz(dividend.getTax())
              .multiply(BigDecimal.valueOf(100))
              .divide(stored, 2, RoundingMode.HALF_UP);
      return " — 저장 과세표준은 참조와 같다("
          + stored.setScale(0, RoundingMode.HALF_UP)
          + "). 그 과세표준 대비 실제 세율이 "
          + effective
          + "% 라 세금 쪽을 봐야 한다(정상 15.40%)";
    }
    return " — 저장 과세표준 "
        + stored.setScale(0, RoundingMode.HALF_UP)
        + " 이 참조 기준 "
        + referenceBase.setScale(0, RoundingMode.HALF_UP)
        + " 과 다르다"
        + (reference[1].compareTo(reference[0]) == 0
            ? "(참조가 정확히 100% 다 — 주당 과세표준에 주당 배당금이 그대로 들어간 모양)"
            : "");
  }

  private static String taxableDetail(Dividend dividend) {
    BigDecimal tax = nz(dividend.getTax());
    BigDecimal taxable = nz(dividend.getTaxableAmount());
    BigDecimal gross = nz(dividend.getGrossAmount());
    Integer quantity = dividend.getQuantity();
    StringBuilder detail = new StringBuilder("tax=" + tax + ", taxable=" + taxable);
    if (quantity != null && quantity > 0) {
      BigDecimal shares = BigDecimal.valueOf(quantity);
      detail
          .append(", 수량=")
          .append(quantity)
          .append(", 주당 세전=")
          .append(gross.divide(shares, 2, RoundingMode.HALF_UP))
          .append(", 주당 과세표준=")
          .append(taxable.divide(shares, 2, RoundingMode.HALF_UP));
    }
    // 무엇을 넣어야 하는지까지 알려준다. 원천징수액은 과세표준 x 15.4% 이므로 거꾸로 나누면 증권사가
    // 실제로 쓴 과세표준이 나온다(실측 2026-08-23: 8 건 모두 기록값의 84.9 배, 합계 1,512,500 원 과소).
    if (tax.signum() > 0) {
      detail
          .append(", 세율 15.4% 기준 추정 과세표준=")
          .append(tax.divide(WITHHOLDING_RATE, 0, RoundingMode.HALF_UP));
    }
    return detail.toString();
  }

  /** 거래대금(단가 x 수량). 둘 중 하나라도 없으면 {@code null}. */
  private static BigDecimal tradeAmount(Trade trade) {
    if (trade == null || trade.getPrice() == null) {
      return null;
    }
    return trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity()));
  }

  /**
   * 참조의 과세표준을 믿었다고 할 때 실제로 적용된 세율(%).
   *
   * <p>이 값이 15.40% 에 떨어지면 참조가 맞고, 그보다 낮으면 참조가 과세표준을 부풀린 것이다. 두 가지를 구분해 주는 값이라 지적에 함께 싣는다 &mdash;
   * "참조가 100% 라고 한다" 만으로는 100% 가 틀린 것인지 세율이 다른 것인지 알 수 없다.
   *
   * <p>실측 2026-08-23 (KODEX 한국부동산리츠인프라): 2025-10~12 는 참조 비율이 51.52% / 41.94% / 100.00% 로 제각각인데
   * 실효세율은 모두 정확히 15.40% 였다(참조가 맞다 - 100% 인 달도 정상이었다). 2026-01 부터 참조가 계속 100% 인데 실효세율이 9.82% 로 떨어진다
   * (그때부터 참조가 부풀려졌다).
   */
  private static BigDecimal effectiveRateOnReference(Dividend dividend, BigDecimal referenceRatio) {
    BigDecimal gross = nz(dividend.getGrossAmount());
    if (gross.signum() <= 0 || referenceRatio.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal referenceTaxableAmount =
        gross.multiply(referenceRatio).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    if (referenceTaxableAmount.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return nz(dividend.getTax())
        .multiply(BigDecimal.valueOf(100))
        .divide(referenceTaxableAmount, 4, RoundingMode.HALF_UP);
  }

  /** 배당을 참조 지급 이력과 짝짓는 키. 지급일로 맞춘다(원장의 기준일에는 지급일이 들어 있기도 하다). */
  private static String referenceKey(Dividend dividend) {
    if (dividend.getStockItemId() == null || dividend.getPayDate() == null) {
      return null;
    }
    return dividend.getStockItemId()
        + "@"
        + dividend.getPayDate().atZone(MARKET_ZONE_ID).toLocalDate();
  }

  /** 실제로 뗀 세금에서 되짚은 과세표준 비율(세전 대비 %). */
  /**
   * 저장된 과세표준이 어떤 수량으로 계산됐는지 되돌린다.
   *
   * <p>과세표준 / 주당 과세표준 이 정수로 딱 떨어질 때만 값을 낸다. 나누어떨어지지 않으면 수량이 아니라 다른 사정(비율 반올림 등)이므로 판단하지 않는다.
   *
   * @return 계산에 쓰인 것으로 보이는 수량, 판단할 수 없으면 {@code null}
   */
  private static Integer taxableImpliedQuantity(Dividend dividend) {
    if (dividend == null || dividend.getQuantity() == null) {
      return null;
    }
    BigDecimal taxable = nz(dividend.getTaxableAmount());
    BigDecimal perShare = nz(dividend.getTaxPerShare());
    if (taxable.signum() <= 0 || perShare.signum() <= 0) {
      return null;
    }
    BigDecimal[] divided = taxable.divideAndRemainder(perShare);
    if (divided[1].signum() != 0) {
      return null;
    }
    try {
      return divided[0].intValueExact();
    } catch (ArithmeticException ignored) {
      return null;
    }
  }

  /**
   * 세금이 기록된 매도에서 연도별 거래세율(중앙값)을 뽑는다.
   *
   * <p>평균이 아니라 중앙값을 쓰는 이유는 한 건의 이상값이 기준을 끌고 가지 않게 하기 위해서다.
   */
  private static Map<Integer, BigDecimal> observedSellTaxRateByYear(List<Trade> trades) {
    Map<Integer, List<BigDecimal>> rates = new HashMap<>();
    for (Trade trade : trades) {
      if (trade.getType() != TradeType.SELL || !positive(trade.getTax())) {
        continue;
      }
      BigDecimal amount = tradeAmount(trade);
      if (amount == null || amount.signum() <= 0 || trade.getTradeDate() == null) {
        continue;
      }
      rates
          .computeIfAbsent(
              trade.getTradeDate().atZone(MARKET_ZONE_ID).getYear(), key -> new ArrayList<>())
          .add(
              trade
                  .getTax()
                  .multiply(BigDecimal.valueOf(100))
                  .divide(amount, 6, RoundingMode.HALF_UP));
    }
    Map<Integer, BigDecimal> median = new HashMap<>();
    for (var entry : rates.entrySet()) {
      List<BigDecimal> sorted = new ArrayList<>(entry.getValue());
      sorted.sort(Comparator.naturalOrder());
      median.put(entry.getKey(), sorted.get(sorted.size() / 2));
    }
    return median;
  }

  /**
   * 그 해에 실제로 관측된 요율로 빠진 거래세를 되짚어 준다.
   *
   * <p>같은 해 표본이 없으면 아무 말도 하지 않는다 &mdash; 세법 연혁을 끌어와 추정하면 원장으로 확인할 수 없는 숫자를 단언하게 된다. 실측 2026-08-23:
   * 동양증권 12 건 중 같은 해 표본이 있는 것은 2020 년 2 건뿐이었다(합계 311,617 원).
   */
  private static String missingTaxHint(Trade trade, Map<Integer, BigDecimal> ratesByYear) {
    if (trade.getTradeDate() == null) {
      return "";
    }
    BigDecimal amount = tradeAmount(trade);
    if (amount == null) {
      return "";
    }
    int year = trade.getTradeDate().atZone(MARKET_ZONE_ID).getYear();
    BigDecimal rate = ratesByYear.get(year);
    if (rate != null) {
      return " (같은 해 관측 거래세율 "
          + rate.setScale(4, RoundingMode.HALF_UP)
          + "% 기준이면 "
          + amount.multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
          + "원이 빠졌다)";
    }
    // 같은 해 표본이 없으면 '가장 이른 관측 연도'의 세율로 하한을 낸다.
    //
    // 실측 거래세율은 해마다 낮아져 왔다(2020 0.25% -> 2021~22 0.23% -> 2023 0.20% -> 2025 0.15%).
    // 그래서 관측이 시작되는 해보다 오래된 매도에 그 해의 세율을 적용하면 실제보다 <b>낮게</b> 잡힌다 -
    // 단언이 아니라 하한이므로 원장 밖 지식을 끌어오지 않고도 "적어도 이만큼" 을 말할 수 있다.
    Integer earliestYear =
        ratesByYear.keySet().stream().min(java.util.Comparator.naturalOrder()).orElse(null);
    if (earliestYear == null || year > earliestYear) {
      return " (같은 해에 거래세가 기록된 매도가 없어 얼마가 빠졌는지 원장만으로는 알 수 없다)";
    }
    BigDecimal floorRate = ratesByYear.get(earliestYear);
    return " (같은 해 표본이 없다. 관측이 시작되는 "
        + earliestYear
        + "년 세율 "
        + floorRate.setScale(4, RoundingMode.HALF_UP)
        + "% 를 적용하면 최소 "
        + amount.multiply(floorRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
        + "원 — 세율은 해마다 낮아져 왔으므로 실제는 이보다 크다)";
  }

  private static BigDecimal impliedTaxableRatio(Dividend dividend) {
    BigDecimal gross = nz(dividend.getGrossAmount());
    if (gross.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return nz(dividend.getTax())
        .divide(WITHHOLDING_RATE, 10, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .divide(gross, 4, RoundingMode.HALF_UP);
  }

  /**
   * 두 출처의 과세표준 비율 차이(%p). 짝을 못 찾거나 세금이 없으면 {@code null}(비교 대상이 아니다).
   *
   * <p>세금이 0 인 배당(비과세 계좌)은 되짚을 수 없다 - 0/15.4% 는 언제나 0 이라 참조가 무엇이든 어긋난 것처럼 보인다.
   */
  private static BigDecimal taxableRatioGapPoints(
      Dividend dividend, Map<String, BigDecimal[]> referenceTaxable) {
    String key = referenceKey(dividend);
    if (key == null || !positive(dividend.getTax()) || !positive(dividend.getGrossAmount())) {
      return null;
    }
    BigDecimal[] reference = referenceTaxable.get(key);
    if (reference == null || reference[0].signum() <= 0) {
      return null;
    }
    BigDecimal referenceRatio =
        reference[1]
            .multiply(BigDecimal.valueOf(100))
            .divide(reference[0], 4, RoundingMode.HALF_UP);
    return impliedTaxableRatio(dividend).subtract(referenceRatio);
  }

  /** 배당의 기준일. 기준일이 없으면 지급일을 쓴다(화면·수익률 계산과 같은 규칙). */
  private static Instant basisDate(Dividend dividend) {
    return dividend.getRecordDate() != null ? dividend.getRecordDate() : dividend.getPayDate();
  }

  /**
   * 그 시점까지의 매매를 더한 보유 수량. 같은 날 거래는 포함한다(기준일 당일 매수도 그 날 보유로 본다).
   *
   * <p>이 계산은 화면의 보유 스냅샷과 같은 규칙이다 - 분할·병합 환산은 수량을 바꾸지 않는다(실측 확인).
   */
  /**
   * 기준일 <b>이전</b>에 그 배당 수량을 마지막으로 보유하고 있던 날을 원장에서 되짚는다.
   *
   * <p>기준일에 보유가 0 이면 대개 <b>지급일이 기준일 자리에 들어간</b> 것이다. 실측 2026-08-23 의 3 건이 모두 그랬다 &mdash; NAVER
   * 2021-04-08(300주, 마지막 보유 2021-01-17), 삼성SDI 2020-04-17(43주, 2020-01-27), HK이노엔 2022-04-22(12주,
   * 2022-04-21). 셋 다 그 직전 12-31 에는 해당 수량을 들고 있었다.
   *
   * <p>진짜 배당기준일이 언제인지는 원장이 알 수 없으므로 단언하지 않는다. 대신 "언제까지 들고 있었는지"를 알려 주면 사람이 바로 맞출 수 있다.
   *
   * <p>기준일 이후의 재매수는 보지 않는다. 나중에 다시 사서 지금도 들고 있다고 해서, 그 배당이 어느 시점의 것인지 되짚는 데 도움이 되지는 않는다.
   */
  private static String lastHeldHint(
      java.util.List<Trade> stockItemTrades, Integer quantity, Instant basis) {
    if (stockItemTrades == null || quantity == null || quantity <= 0 || basis == null) {
      return "";
    }
    java.util.List<Trade> ordered =
        stockItemTrades.stream()
            .filter(t -> t.getTradeDate() != null && !t.getTradeDate().isAfter(basis))
            .sorted(Comparator.comparing(Trade::getTradeDate))
            .toList();
    Instant lastHeld = null;
    int running = 0;
    for (Trade trade : ordered) {
      int before = running;
      running += trade.getType() == TradeType.SELL ? -trade.getQuantity() : trade.getQuantity();
      if (before >= quantity && running < quantity) {
        // 이 거래로 수량이 깨졌다. 그 전날까지는 들고 있었다.
        lastHeld = trade.getTradeDate().minus(java.time.Duration.ofDays(1));
      }
    }
    if (lastHeld == null) {
      return "";
    }
    return ", 이 수량을 마지막으로 보유한 날=" + day(lastHeld);
  }

  /** 연도별로 실제 관측된 <b>가장 낮은</b> 수수료율(%). 하한 추정에 쓰므로 중앙값이 아니라 최저값이다. */
  private static Map<Integer, BigDecimal> observedFeeRateFloorByYear(List<Trade> trades) {
    Map<Integer, BigDecimal> floors = new HashMap<>();
    for (Trade trade : trades) {
      BigDecimal amount = tradeAmount(trade);
      if (trade.getTradeDate() == null
          || amount == null
          || amount.signum() <= 0
          || !positive(trade.getFee())) {
        continue;
      }
      floors.merge(
          trade.getTradeDate().atZone(MARKET_ZONE_ID).getYear(), feeRate(trade), BigDecimal::min);
    }
    return floors;
  }

  /**
   * 수수료가 한 건도 없는 계좌에서 <b>적어도 얼마가</b> 빠졌는지 되짚는다.
   *
   * <p>거래마다 그 해에 실제로 관측된 최저 수수료율을 쓴다. 관측이 시작되는 해보다 오래된 거래에는 그 최초 연도의 값을 쓴다 &mdash; 수수료율은 해마다 낮아져
   * 왔으므로(실측 2019 0.004458% → 2026 0.002172%) 그보다 오래된 거래에는 하한이 된다.
   *
   * <p>실측 2026-08-23: 동양증권 26 건(거래대금 584,010,880) 중 그 해 관측이 있는 것이 6 건, 관측 이전이 20 건이었고 거래별 하한 합계는
   * 25,011 원이다. 계좌 전체에 최초 연도 요율 하나를 곱하면 26,036 원이 나오는데, 그건 최근 거래를 과대평가한 값이다.
   */
  private static String missingFeeHint(
      List<Trade> accountTrades, Map<Integer, BigDecimal> floorsByYear) {
    if (accountTrades.isEmpty() || floorsByYear.isEmpty()) {
      return "";
    }
    int earliestYear = floorsByYear.keySet().stream().min(Comparator.naturalOrder()).orElseThrow();
    BigDecimal total = BigDecimal.ZERO;
    int estimated = 0;
    for (Trade trade : accountTrades) {
      BigDecimal amount = tradeAmount(trade);
      if (trade.getTradeDate() == null || amount == null || amount.signum() <= 0) {
        continue;
      }
      int year = trade.getTradeDate().atZone(MARKET_ZONE_ID).getYear();
      BigDecimal rate = floorsByYear.get(year);
      if (rate == null && year < earliestYear) {
        rate = floorsByYear.get(earliestYear);
      }
      if (rate == null) {
        continue;
      }
      estimated++;
      total =
          total.add(amount.multiply(rate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }
    if (estimated == 0) {
      return "";
    }
    return " (거래마다 그 해 관측 최저 수수료율을 적용하면 최소 "
        + total.setScale(0, RoundingMode.HALF_UP)
        + "원 — 수수료율은 해마다 낮아져 왔으므로 실제는 이보다 크다)";
  }

  /** 그 거래의 수수료율(%). 거래대금이 없으면 0. */
  private static BigDecimal feeRate(Trade trade) {
    BigDecimal amount = tradeAmount(trade);
    if (amount == null || amount.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return nz(trade.getFee())
        .multiply(BigDecimal.valueOf(100))
        .divide(amount, 6, RoundingMode.HALF_UP);
  }

  /** 그 계좌 중앙값의 몇 배인지. 판단할 수 없으면 null. */
  private static BigDecimal feeRateMultiple(Trade trade, Map<UUID, BigDecimal> medianFeeRate) {
    if (trade.getAccountId() == null || !positive(trade.getFee())) {
      return null;
    }
    BigDecimal median = medianFeeRate.get(trade.getAccountId());
    if (median == null || median.signum() <= 0) {
      return null;
    }
    return feeRate(trade).divide(median, 2, RoundingMode.HALF_UP);
  }

  /**
   * 그 시점까지 <b>한 번이라도</b> 보유했던 최대 수량.
   *
   * <p>{@link #holdingQuantityAt} 는 "그 날의 보유" 라 배당에는 그대로 쓸 수 없다 &mdash; 기준일이 비어 있으면 지급일로 대신하는데,
   * 기말배당은 기준일과 지급일이 서너 달 떨어져 그 사이에 팔면 지급일 보유가 0 이 된다(실측: NAVER 2020-12 기준 배당의 지급일은 2021-04-08 인데
   * 2021-01-18 에 전량 매도). 그래서 "그때까지의 최대"로 보면 시차에 휘둘리지 않는다.
   *
   * <p>이 값보다 많은 수량에 배당이 붙었다면 시차로는 설명되지 않는다 &mdash; 배당 수량이 잘못됐거나 매수 기록이 빠진 것이다.
   */
  private static int maxHoldingQuantityUntil(java.util.List<Trade> stockItemTrades, Instant at) {
    if (stockItemTrades == null || at == null) {
      return 0;
    }
    java.util.List<Trade> ordered = new ArrayList<>();
    for (Trade trade : stockItemTrades) {
      if (trade.getTradeDate() != null && !trade.getTradeDate().isAfter(at)) {
        ordered.add(trade);
      }
    }
    ordered.sort(
        java.util.Comparator.comparing(Trade::getTradeDate)
            .thenComparing(trade -> trade.getType() == TradeType.SELL ? 1 : 0));
    int quantity = 0;
    int peak = 0;
    for (Trade trade : ordered) {
      quantity += trade.getType() == TradeType.SELL ? -trade.getQuantity() : trade.getQuantity();
      peak = Math.max(peak, quantity);
    }
    return peak;
  }

  private static int holdingQuantityAt(java.util.List<Trade> stockItemTrades, Instant at) {
    if (stockItemTrades == null || at == null) {
      return 0;
    }
    int quantity = 0;
    for (Trade trade : stockItemTrades) {
      if (trade.getTradeDate() == null || trade.getTradeDate().isAfter(at)) {
        continue;
      }
      quantity += trade.getType() == TradeType.SELL ? -trade.getQuantity() : trade.getQuantity();
    }
    return quantity;
  }

  /**
   * 발견에 계좌명을 붙인다.
   *
   * <p>계좌를 적어야 "한 계좌에 몰린 문제" 인지 "여기저기 흩어진 문제" 인지 보인다. 실측 2026-08-23: 규칙마다 발견이 <b>한 계좌에 100% 집중</b>돼
   * 있었다 &mdash; 과세표준 관련 배당 8 건은 전부 KB증권 위탁, 매매 없는 배당 2 건은 한국투자증권 위탁, 수수료·거래세가 없는 매도 12 건은 전부
   * 동양증권이다. 즉 각 결함이 한 계좌의 입력 경로에 묶여 있다.
   *
   * <p>계좌명이 없으면 같은 계좌의 다른 발견(예: {@code ACCOUNT_WITHOUT_ANY_FEE})과 이어지지 않는다.
   */
  /** 계좌별 발견 수와 서로 다른 행을 모은다. 계좌를 모르는 발견은 세지 않는다. */
  private static void recordAccount(
      UUID accountId,
      Map<UUID, String> accountNames,
      String rowKey,
      Map<String, long[]> accountFindingCounts,
      Map<String, Set<String>> accountDistinctRows) {
    if (accountId == null) {
      return;
    }
    String accountName = accountNames.getOrDefault(accountId, "?");
    accountFindingCounts.computeIfAbsent(accountName, key -> new long[1])[0]++;
    accountDistinctRows
        .computeIfAbsent(accountName, key -> new java.util.LinkedHashSet<>())
        .add(rowKey);
  }

  private static String accountSuffix(UUID accountId, Map<UUID, String> accountNames) {
    if (accountId == null) {
      return "";
    }
    return " [" + accountNames.getOrDefault(accountId, "?") + "]";
  }

  private void dividendRule(
      List<LedgerIntegrityFinding> findings,
      Map<UUID, String> names,
      List<Dividend> dividends,
      String code,
      Predicate<Dividend> broken,
      Function<Dividend, String> detail,
      int maxExamples,
      RowIndex distinctRows,
      Map<UUID, String> accountNames,
      Map<String, long[]> accountFindingCounts,
      Map<String, Set<String>> accountDistinctRows) {
    List<Dividend> hits = dividends.stream().filter(broken).toList();
    if (hits.isEmpty()) {
      return;
    }
    for (Dividend hit : hits) {
      String rowKey =
          day(hit.getPayDate() != null ? hit.getPayDate() : hit.getRecordDate())
              + "|"
              + names.get(hit.getStockItemId());
      distinctRows.add(rowKey, code, accountNames.get(hit.getAccountId()));
      recordAccount(
          hit.getAccountId(), accountNames, rowKey, accountFindingCounts, accountDistinctRows);
    }
    List<LedgerIntegrityFinding.Example> examples = new ArrayList<>();
    for (Dividend hit : hits.stream().limit(maxExamples).toList()) {
      examples.add(
          new LedgerIntegrityFinding.Example(
              day(hit.getPayDate() != null ? hit.getPayDate() : hit.getRecordDate()),
              names.get(hit.getStockItemId()),
              detail.apply(hit) + accountSuffix(hit.getAccountId(), accountNames)));
    }
    findings.add(new LedgerIntegrityFinding(code, hits.size(), examples));
  }

  private void tradeRule(
      List<LedgerIntegrityFinding> findings,
      Map<UUID, String> names,
      List<Trade> trades,
      String code,
      Predicate<Trade> broken,
      Function<Trade, String> detail,
      int maxExamples,
      RowIndex distinctRows,
      Map<UUID, String> accountNames,
      Map<String, long[]> accountFindingCounts,
      Map<String, Set<String>> accountDistinctRows) {
    // 예시는 최신순으로 고른다.
    //
    // 배당 규칙은 목록이 지급일 내림차순으로 와서 이미 최신순인데 매매만 오래된 순이었다. 그래서 같은 화면에서
    // 규칙마다 예시 기준이 달랐고, 무엇보다 오래된 건이 자리를 다 차지했다 - 실측 2026-08-23: 수수료·거래세가
    // 없는 매도 12 건의 예시 3 개가 모두 2010 년 건이라 "원장만으로는 알 수 없다"만 세 번 나왔고, 금액을
    // 되짚어 줄 수 있는 2020 년 2 건은 가려졌다.
    List<Trade> hits =
        trades.stream()
            .filter(broken)
            .sorted(
                Comparator.comparing(
                        Trade::getTradeDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        Trade::getId, Comparator.nullsLast(Comparator.comparing(UUID::toString))))
            .toList();
    if (hits.isEmpty()) {
      return;
    }
    for (Trade hit : hits) {
      String rowKey = day(hit.getTradeDate()) + "|" + names.get(hit.getStockItemId());
      distinctRows.add(rowKey, code, accountNames.get(hit.getAccountId()));
      recordAccount(
          hit.getAccountId(), accountNames, rowKey, accountFindingCounts, accountDistinctRows);
    }
    List<LedgerIntegrityFinding.Example> examples = new ArrayList<>();
    for (Trade hit : hits.stream().limit(maxExamples).toList()) {
      examples.add(
          new LedgerIntegrityFinding.Example(
              day(hit.getTradeDate()),
              names.get(hit.getStockItemId()),
              detail.apply(hit) + accountSuffix(hit.getAccountId(), accountNames)));
    }
    findings.add(new LedgerIntegrityFinding(code, hits.size(), examples));
  }
}
