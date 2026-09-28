package net.luversof.web.gate.stock.service;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutImportRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendProfileClient;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendSnapshotClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 월배당 기준(프로필·지급이력) 화면이 쓰는 <b>데이터 적재와 모델 조립</b>.
 *
 * <p>관리 화면의 월배당 기준 탭과 그 탭의 저장·삭제·가져오기 폼들이 <b>같은 조립</b>을 쓴다. 컨트롤러 안에 있을 때는 그 사실이 드러나지 않아, 화면을 쪼개려 할
 * 때마다 어느 쪽으로 가져가야 할지 알 수 없었다.
 *
 * <p>여기 있는 것은 라우팅이 아니라 조회·검증·기본 폼 만들기다. 컨트롤러가 얇아지는 것보다, <b>두 화면이 같은 것을 쓴다</b>는 사실이 코드에 드러나는 것이 크다.
 */
@Component
public class MonthlyDividendReferenceSupport {

  /** 관리 화면의 월배당 기준 탭 이름. 되돌아갈 주소를 만들 때 쓴다. */
  public static final String DIVIDEND_TAB_MONTHLY_REFERENCE = "monthly-reference";

  /** 월배당 기준 종목을 가리는 태그. 종목 마스터의 태그와 같은 문자열이어야 한다. */
  private static final String MONTHLY_DIVIDEND_TAG = "월배당";

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(MonthlyDividendReferenceSupport.class);

  @org.springframework.beans.factory.annotation.Autowired private StockItemClient stockItemClient;

  @org.springframework.beans.factory.annotation.Autowired
  private MonthlyDividendProfileClient monthlyDividendProfileClient;

  @org.springframework.beans.factory.annotation.Autowired
  private MonthlyDividendPayoutClient monthlyDividendPayoutClient;

  @org.springframework.beans.factory.annotation.Autowired
  private MonthlyDividendCalculator monthlyDividendCalculator;

  @org.springframework.beans.factory.annotation.Autowired
  private MonthlyDividendViewSupport monthlyDividendViewSupport;

  @org.springframework.beans.factory.annotation.Autowired
  private TradeProfitClient tradeProfitClient;

  @org.springframework.beans.factory.annotation.Autowired
  private MonthlyDividendSnapshotClient monthlyDividendSnapshotClient;

  public void setTradeProfitClient(TradeProfitClient tradeProfitClient) {
    this.tradeProfitClient = tradeProfitClient;
  }

  public void setMonthlyDividendSnapshotClient(
      MonthlyDividendSnapshotClient monthlyDividendSnapshotClient) {
    this.monthlyDividendSnapshotClient = monthlyDividendSnapshotClient;
  }

  public void setStockItemClient(StockItemClient stockItemClient) {
    this.stockItemClient = stockItemClient;
  }

  public void setMonthlyDividendProfileClient(
      MonthlyDividendProfileClient monthlyDividendProfileClient) {
    this.monthlyDividendProfileClient = monthlyDividendProfileClient;
  }

  public void setMonthlyDividendPayoutClient(
      MonthlyDividendPayoutClient monthlyDividendPayoutClient) {
    this.monthlyDividendPayoutClient = monthlyDividendPayoutClient;
  }

  public void setMonthlyDividendCalculator(MonthlyDividendCalculator monthlyDividendCalculator) {
    this.monthlyDividendCalculator = monthlyDividendCalculator;
  }

  public void setMonthlyDividendViewSupport(MonthlyDividendViewSupport monthlyDividendViewSupport) {
    this.monthlyDividendViewSupport = monthlyDividendViewSupport;
  }

  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.httpexchange.DividendClient dividendClient;

  @org.springframework.beans.factory.annotation.Autowired
  private net.luversof.web.gate.stock.httpexchange.AccountClient accountClient;

  public void setDividendClient(
      net.luversof.web.gate.stock.httpexchange.DividendClient dividendClient) {
    this.dividendClient = dividendClient;
  }

  public void setAccountClient(
      net.luversof.web.gate.stock.httpexchange.AccountClient accountClient) {
    this.accountClient = accountClient;
  }

  /**
   * 한 종목이 최근 1 년 받은 배당을 과세이연 계좌 몫과 그 밖의 계좌 몫으로 나눈 것.
   *
   * <p>시뮬레이터의 과세표준 비중은 api-stock 이 원장 최근 1 년의 (과세금액 합 / 세전 합)을 <b>모든 계좌를 합쳐</b> 낸 값이다(2026-08-24
   * 결정: 계좌별 비과세·분리과세 반영). 과세이연 계좌(ISA · 연금저축)는 과세금액이 0 으로 적혀 비중을 끌어내린다 &mdash; 실측 2026-09-17: PLUS
   * 고배당주위클리고정커버드콜 · TIGER 코리아배당다우존스위클리커버드콜은 과세이연 계좌에서만 받아 0%, TIGER 리츠부동산인프라는 네 계좌 중 과세 계좌 한 곳만
   * 100% 라 합쳐서 13.42% 였다. 사용자는 "0% 인데 실제는 100%" 로 읽었다. 계산은 그대로 두고(사용자 결정) 표가 이 나눔을 함께 적는다.
   *
   * @param deferredGross 과세이연 계좌에서 받은 세전 합
   * @param taxableAccountGross 그 밖의 계좌에서 받은 세전 합
   * @param taxableAccountTaxable 그 밖의 계좌에서 받은 과세금액 합
   */
  public record TaxableRatioBasis(
      BigDecimal deferredGross, BigDecimal taxableAccountGross, BigDecimal taxableAccountTaxable) {

    /** 과세이연 계좌에서만 받았다 &mdash; 비중 0 은 "비과세 종목" 이 아니라 이 뜻이다. */
    public boolean deferredOnly() {
      return deferredGross.signum() > 0 && taxableAccountGross.signum() == 0;
    }

    /** 두 종류 계좌에서 모두 받았다 &mdash; 합친 비중은 과세 계좌만의 비중보다 낮게 나온다. */
    public boolean mixed() {
      return deferredGross.signum() > 0 && taxableAccountGross.signum() > 0;
    }

    /** 과세 계좌만 따진 비중(%). 과세 계좌에서 받은 것이 없으면 {@code null}. */
    public BigDecimal taxableAccountRatioPct() {
      return taxableAccountGross.signum() > 0
          ? taxableAccountTaxable
              .multiply(BigDecimal.valueOf(100))
              .divide(taxableAccountGross, 2, java.math.RoundingMode.HALF_UP)
          : null;
    }
  }

  /**
   * 종목별 {@link TaxableRatioBasis}. 기간은 api-stock 의 비중 계산과 같다(지금으로부터 365 일 전 이후).
   *
   * <p>조회에 실패하면 빈 맵 &mdash; 표기는 예전 그대로다(없는 나눔을 지어내지 않는다).
   */
  public Map<UUID, TaxableRatioBasis> loadTaxableRatioBasis(UUID userId) {
    if (userId == null) {
      return Map.of();
    }
    try {
      java.util.Set<UUID> deferredAccountIds = new java.util.HashSet<>();
      for (net.luversof.web.gate.stock.domain.Account account :
          accountClient.getAccountsByUserId(userId)) {
        if (account != null
            && account.jsonConfig() != null
            && Boolean.TRUE.equals(account.jsonConfig().get("isTaxDeferred"))) {
          deferredAccountIds.add(account.id());
        }
      }
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("userId", userId.toString());
      params.add(
          "startDate", java.time.Instant.now().minus(java.time.Duration.ofDays(365)).toString());
      return taxableRatioBasis(dividendClient.findDividends(params), deferredAccountIds);
    } catch (RuntimeException ex) {
      log.warn("과세표준 비중 나눔 조회 실패: userId={}", userId, ex);
      return Map.of();
    }
  }

  /** 배당 목록을 종목별로 과세이연 / 그 밖 계좌로 나눠 더한다. */
  public static Map<UUID, TaxableRatioBasis> taxableRatioBasis(
      List<net.luversof.web.gate.stock.dto.response.DividendResponse> dividends,
      java.util.Set<UUID> deferredAccountIds) {
    Map<UUID, BigDecimal[]> sums = new LinkedHashMap<>();
    if (dividends != null) {
      for (net.luversof.web.gate.stock.dto.response.DividendResponse dividend : dividends) {
        if (dividend == null || dividend.stockItemId() == null) {
          continue;
        }
        BigDecimal[] sum =
            sums.computeIfAbsent(
                dividend.stockItemId(),
                id -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal gross =
            dividend.grossAmount() != null ? dividend.grossAmount() : BigDecimal.ZERO;
        if (deferredAccountIds != null && deferredAccountIds.contains(dividend.accountId())) {
          sum[0] = sum[0].add(gross);
        } else {
          sum[1] = sum[1].add(gross);
          sum[2] =
              sum[2].add(
                  dividend.taxableAmount() != null ? dividend.taxableAmount() : BigDecimal.ZERO);
        }
      }
    }
    Map<UUID, TaxableRatioBasis> result = new LinkedHashMap<>();
    sums.forEach((id, sum) -> result.put(id, new TaxableRatioBasis(sum[0], sum[1], sum[2])));
    return result;
  }

  public void populateMonthlyDividendReferenceModel(
      Model model,
      String requestedSymbol,
      String requestedProfileSort,
      String requestedProfileDirection,
      LocalDate requestedPayoutRecordDate,
      LocalDate requestedPayoutPayDate) {
    List<StockItem> stockItems = loadMonthlyDividendStockItems();
    String profileSort = monthlyDividendViewSupport.resolveProfileSort(requestedProfileSort);
    String profileDirection =
        monthlyDividendViewSupport.resolveProfileDirection(profileSort, requestedProfileDirection);
    List<MonthlyDividendProfileResponse> profiles =
        monthlyDividendViewSupport.sortProfiles(
            loadMonthlyDividendProfiles(), profileSort, profileDirection);
    List<StockItem> selectableStockItems =
        mergeMonthlyDividendReferenceStockItems(stockItems, profiles);
    String selectedSymbol =
        resolveMonthlyDividendReferenceSymbol(
            model, requestedSymbol, profiles, selectableStockItems);
    MonthlyDividendProfileResponse selectedProfile =
        profiles.stream()
            .filter(
                profile ->
                    selectedSymbol.equalsIgnoreCase(
                        StockViewSupport.safeString(profile.stockItemSymbol())))
            .findFirst()
            .orElse(null);
    List<MonthlyDividendPayoutResponse> payouts = loadMonthlyDividendPayouts(selectedSymbol);
    MonthlyDividendPayoutResponse selectedPayout =
        resolveSelectedMonthlyDividendPayout(
            model, payouts, requestedPayoutRecordDate, requestedPayoutPayDate);

    model.addAttribute("stockItems", selectableStockItems);
    model.addAttribute("monthlyDividendProfiles", profiles);
    model.addAttribute("monthlyDividendProfileSort", profileSort);
    model.addAttribute("monthlyDividendProfileDirection", profileDirection);
    model.addAttribute("monthlyDividendPayouts", payouts);
    model.addAttribute("selectedMonthlyDividendSymbol", selectedSymbol);
    model.addAttribute(
        "selectedMonthlyDividendSourceUrl",
        selectedProfile != null ? StockViewSupport.safeString(selectedProfile.sourceUrl()) : "");
    model.addAttribute("monthlyDividendProfileExists", selectedProfile != null);
    model.addAttribute("monthlyDividendPayoutExists", selectedPayout != null);
    model.addAttribute(
        "selectedMonthlyDividendPayoutKey", buildMonthlyDividendPayoutKey(selectedPayout));
    model.addAttribute(
        "selectedMonthlyDividendPayoutRecordDate",
        selectedPayout != null ? String.valueOf(selectedPayout.recordDate()) : "");
    model.addAttribute(
        "selectedMonthlyDividendPayoutPayDate",
        selectedPayout != null ? String.valueOf(selectedPayout.payDate()) : "");
    model.addAttribute(
        "monthlyDividendReferenceSummary",
        monthlyDividendCalculator.buildReferenceSummary(selectedSymbol, payouts));

    if (!model.containsAttribute("monthlyDividendProfileForm")) {
      model.addAttribute(
          "monthlyDividendProfileForm",
          selectedProfile != null
              ? buildDefaultMonthlyDividendProfileForm(selectedProfile)
              : buildDefaultMonthlyDividendProfileForm(selectedSymbol, profiles));
    }
    if (!model.containsAttribute("monthlyDividendPayoutForm")) {
      model.addAttribute(
          "monthlyDividendPayoutForm",
          buildDefaultMonthlyDividendPayoutForm(selectedSymbol, selectedPayout));
    }
    if (!model.containsAttribute("monthlyDividendPayoutImportForm")) {
      model.addAttribute(
          "monthlyDividendPayoutImportForm",
          buildDefaultMonthlyDividendPayoutImportForm(selectedSymbol));
    }
    if (!model.containsAttribute("monthlyDividendReferenceErrorMessage")) {
      model.addAttribute("monthlyDividendReferenceErrorMessage", "");
    }
    if (!model.containsAttribute("monthlyDividendReferenceResult")) {
      model.addAttribute("monthlyDividendReferenceResult", "");
    }
  }

  private MonthlyDividendPayoutResponse resolveSelectedMonthlyDividendPayout(
      Model model,
      List<MonthlyDividendPayoutResponse> payouts,
      LocalDate requestedPayoutRecordDate,
      LocalDate requestedPayoutPayDate) {
    MonthlyDividendPayoutResponse selectedPayout =
        findMonthlyDividendPayout(payouts, requestedPayoutRecordDate, requestedPayoutPayDate);
    if (selectedPayout != null) {
      return selectedPayout;
    }

    Object payoutFormAttr = model.asMap().get("monthlyDividendPayoutForm");
    if (payoutFormAttr instanceof MonthlyDividendPayoutUpsertRequest payoutForm) {
      return findMonthlyDividendPayout(
          payouts, payoutForm.getRecordDate(), payoutForm.getPayDate());
    }

    return null;
  }

  private MonthlyDividendPayoutResponse findMonthlyDividendPayout(
      List<MonthlyDividendPayoutResponse> payouts, LocalDate recordDate, LocalDate payDate) {
    if (recordDate == null || payDate == null || payouts == null || payouts.isEmpty()) {
      return null;
    }

    return payouts.stream()
        .filter(row -> recordDate.equals(row.recordDate()) && payDate.equals(row.payDate()))
        .findFirst()
        .orElse(null);
  }

  private String buildMonthlyDividendPayoutKey(MonthlyDividendPayoutResponse payout) {
    return payout == null
        ? ""
        : buildMonthlyDividendPayoutKey(payout.recordDate(), payout.payDate());
  }

  private String buildMonthlyDividendPayoutKey(LocalDate recordDate, LocalDate payDate) {
    if (recordDate == null || payDate == null) {
      return "";
    }

    return recordDate + "|" + payDate;
  }

  public List<MonthlyDividendProfileResponse> loadMonthlyDividendProfiles() {
    List<MonthlyDividendProfileResponse> profiles =
        monthlyDividendProfileClient.findProfiles(new LinkedMultiValueMap<>());
    return profiles != null ? profiles : List.of();
  }

  /**
   * 종목코드 -> 종목 이름 순(사용자 요청 2026-09-21).
   *
   * <p>코드가 같은 종목은 없지만, 이름까지 비교해 두면 순서가 입력 순서에 흔들리지 않는다.
   */
  /**
   * 종목 이름 -> 코드 순(사용자 결정 2026-09-21).
   *
   * <p>처음에는 코드 순으로 뒀는데, 코드로는 운용사를 알 수 없어 목록에서 찾기 어려웠다(새 코드 0018C0 과 옛 코드 329200 이 섞인다). 이름 순이면
   * KODEX · PLUS · RISE · TIGER 가 저절로 묶인다. 표시는 그대로 "코드 · 이름" 이다.
   *
   * <p>이름이 같은 종목은 없지만, 코드까지 비교해 두면 순서가 입력 순서에 흔들리지 않는다.
   */
  public List<StockItem> sortSelectableStockItems(java.util.Collection<StockItem> stockItems) {
    if (stockItems == null) {
      return List.of();
    }

    return stockItems.stream()
        .filter(stockItem -> stockItem != null)
        .sorted(BY_NAME_THEN_SYMBOL)
        .toList();
  }

  private static final Comparator<StockItem> BY_NAME_THEN_SYMBOL =
      Comparator.comparing(
              (StockItem stockItem) -> StockViewSupport.safeString(stockItem.name()),
              String.CASE_INSENSITIVE_ORDER)
          .thenComparing(
              stockItem -> StockViewSupport.safeString(stockItem.symbol()),
              String.CASE_INSENSITIVE_ORDER);

  public List<StockItem> mergeMonthlyDividendReferenceStockItems(
      List<StockItem> stockItems, List<MonthlyDividendProfileResponse> profiles) {
    Map<String, StockItem> stockItemsBySymbol = new LinkedHashMap<>();

    if (stockItems != null) {
      for (StockItem stockItem : stockItems) {
        if (stockItem == null || !StringUtils.hasText(stockItem.symbol())) {
          continue;
        }

        String normalizedSymbol = stockItem.symbol().trim().toUpperCase(Locale.ROOT);
        stockItemsBySymbol.put(normalizedSymbol, stockItem);
      }
    }

    if (profiles != null) {
      profiles.stream()
          .filter(profile -> StringUtils.hasText(profile.stockItemSymbol()))
          .sorted(
              Comparator.comparing(
                  profile -> StockViewSupport.safeString(profile.stockItemSymbol()),
                  String.CASE_INSENSITIVE_ORDER))
          .forEach(
              profile -> {
                String normalizedSymbol = profile.stockItemSymbol().trim().toUpperCase(Locale.ROOT);
                stockItemsBySymbol.putIfAbsent(
                    normalizedSymbol,
                    new StockItem(
                        profile.stockItemId(),
                        normalizedSymbol,
                        StringUtils.hasText(profile.stockItemName())
                            ? profile.stockItemName().trim()
                            : normalizedSymbol,
                        null,
                        List.of(MONTHLY_DIVIDEND_TAG)));
              });
    }

    // 붙이는 순서(태그 종목 -> 프로필에만 있는 종목)를 그대로 두면 코드 순이 두 덩이로 끊긴다 - 합친 뒤 한 줄로 정렬한다.
    return sortSelectableStockItems(stockItemsBySymbol.values());
  }

  /**
   * 종목코드 -> 저장된 지급 이력으로 계산한 1년 평균 과세표준 비중.
   *
   * <p>시뮬레이터 표가 쓰는 값은 <b>스냅샷에 저장된</b> 비중이라, 지급 이력이 갱신돼도 사용자가 '이 값으로 시뮬레이터 채우기' 를 누르기 전까지 옛 값 그대로다.
   * 실측 2026-09-11: 8 종목 전부 달랐고 KODEX 한국부동산리츠인프라는 저장 17.35% 對 이력 계산 82.79%, TIGER 리츠부동산인프라는 13.42% 對
   * 100% 였다(주당 분배금은 8 종목 모두 일치했다 - 비중만 벌어졌다). 세후 예상 배당이 그만큼 달라지므로 화면이 그 차이를 알려 줄 수 있어야 한다.
   *
   * <p>계산은 관리 화면이 쓰는 {@code buildReferenceSummary} 를 그대로 불러 정의를 하나로 둔다.
   */
  public java.util.Map<String, BigDecimal> referenceTaxableRatioBySymbol() {
    return referenceTaxableRatioBySymbol(
        monthlyDividendPayoutClient.findPayouts(new LinkedMultiValueMap<>()));
  }

  /**
   * 이미 읽어 둔 지급이력으로 같은 값을 낸다.
   *
   * <p>배당 캘린더는 지급일 추정 때문에 지급이력을 이미 갖고 있다 &mdash; 같은 목록을 한 번 더 받아 올 이유가 없다.
   */
  public java.util.Map<String, BigDecimal> referenceTaxableRatioBySymbol(
      List<MonthlyDividendPayoutResponse> payouts) {
    if (payouts == null || payouts.isEmpty()) {
      return java.util.Map.of();
    }
    java.util.Map<String, List<MonthlyDividendPayoutResponse>> bySymbol =
        new java.util.LinkedHashMap<>();
    for (MonthlyDividendPayoutResponse payout : payouts) {
      if (payout.stockItemSymbol() == null) {
        continue;
      }
      bySymbol
          .computeIfAbsent(
              payout.stockItemSymbol().trim().toUpperCase(Locale.ROOT),
              key -> new java.util.ArrayList<>())
          .add(payout);
    }
    java.util.Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
    for (var entry : bySymbol.entrySet()) {
      var summary =
          monthlyDividendCalculator.buildReferenceSummary(entry.getKey(), entry.getValue());
      if (summary != null && summary.payoutCount() > 0) {
        result.put(entry.getKey(), summary.averageTaxableBaseRatio1y());
      }
    }
    return result;
  }

  public List<MonthlyDividendPayoutResponse> loadMonthlyDividendPayouts(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return List.of();
    }

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("symbol", symbol.trim().toUpperCase(Locale.ROOT));
    return monthlyDividendPayoutClient.findPayouts(params);
  }

  private String resolveMonthlyDividendReferenceSymbol(
      Model model,
      String requestedSymbol,
      List<MonthlyDividendProfileResponse> profiles,
      List<StockItem> stockItems) {
    String normalizedRequestedSymbol =
        normalizeMonthlyDividendReferenceSymbol(requestedSymbol, profiles, stockItems);
    if (normalizedRequestedSymbol != null) {
      return normalizedRequestedSymbol;
    }

    Object profileFormAttr = model.asMap().get("monthlyDividendProfileForm");
    if (profileFormAttr instanceof MonthlyDividendProfileUpsertRequest profileForm) {
      String normalizedProfileSymbol =
          normalizeMonthlyDividendReferenceSymbol(profileForm.getSymbol(), profiles, stockItems);
      if (normalizedProfileSymbol != null) {
        return normalizedProfileSymbol;
      }
    }

    Object payoutFormAttr = model.asMap().get("monthlyDividendPayoutForm");
    if (payoutFormAttr instanceof MonthlyDividendPayoutUpsertRequest payoutForm) {
      String normalizedPayoutSymbol =
          normalizeMonthlyDividendReferenceSymbol(payoutForm.getSymbol(), profiles, stockItems);
      if (normalizedPayoutSymbol != null) {
        return normalizedPayoutSymbol;
      }
    }

    Object payoutImportFormAttr = model.asMap().get("monthlyDividendPayoutImportForm");
    if (payoutImportFormAttr instanceof MonthlyDividendPayoutImportRequest payoutImportForm) {
      String normalizedPayoutImportSymbol =
          normalizeMonthlyDividendReferenceSymbol(
              payoutImportForm.getSymbol(), profiles, stockItems);
      if (normalizedPayoutImportSymbol != null) {
        return normalizedPayoutImportSymbol;
      }
    }

    if (!profiles.isEmpty() && StringUtils.hasText(profiles.get(0).stockItemSymbol())) {
      return profiles.get(0).stockItemSymbol().trim().toUpperCase(Locale.ROOT);
    }

    if (!stockItems.isEmpty() && StringUtils.hasText(stockItems.get(0).symbol())) {
      return stockItems.get(0).symbol().trim().toUpperCase(Locale.ROOT);
    }

    return "";
  }

  private String normalizeMonthlyDividendReferenceSymbol(
      String symbol, List<MonthlyDividendProfileResponse> profiles, List<StockItem> stockItems) {
    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    boolean existsInProfiles =
        profiles != null
            && profiles.stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    boolean existsInStockItems =
        stockItems != null
            && stockItems.stream()
                .map(StockItem::symbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    return existsInProfiles || existsInStockItems ? normalizedSymbol : null;
  }

  public MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(
      MonthlyDividendProfileResponse profile) {
    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    if (profile == null) {
      request.setActive(true);
      request.setDisplayOrder(1);
      request.setPayoutWindow("UNKNOWN");
      return request;
    }

    request.setSymbol(profile.stockItemSymbol());
    request.setSourceUrl(profile.sourceUrl());
    request.setPayoutWindow(profile.payoutWindow());
    request.setDisplayOrder(profile.displayOrder());
    request.setActive(profile.active());
    request.setNote(profile.note());
    request.setLastVerifiedDate(profile.lastVerifiedDate());
    // 저장은 전체 덮어쓰기다 - 폼에 안 실으면 저장할 때마다 지워진다.
    request.setTotalExpenseRatioPct(profile.totalExpenseRatioPct());
    request.setListingDate(profile.listingDate());
    return request;
  }

  public MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(
      String symbol, List<MonthlyDividendProfileResponse> profiles) {
    MonthlyDividendProfileUpsertRequest request = buildDefaultMonthlyDividendProfileForm(symbol);
    request.setDisplayOrder(resolveNextMonthlyDividendProfileDisplayOrder(profiles));
    return request;
  }

  public MonthlyDividendProfileUpsertRequest buildDefaultMonthlyDividendProfileForm(String symbol) {
    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setActive(true);
    request.setDisplayOrder(1);
    request.setPayoutWindow("UNKNOWN");
    return request;
  }

  public MonthlyDividendPayoutUpsertRequest buildDefaultMonthlyDividendPayoutForm(String symbol) {
    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setRecordDate(LocalDate.now());
    request.setPayDate(LocalDate.now());
    return request;
  }

  public MonthlyDividendPayoutUpsertRequest buildDefaultMonthlyDividendPayoutForm(
      String symbol, MonthlyDividendPayoutResponse payout) {
    if (payout == null) {
      return buildDefaultMonthlyDividendPayoutForm(symbol);
    }

    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    request.setRecordDate(payout.recordDate());
    request.setPayDate(payout.payDate());
    request.setDistributionRatePct(payout.distributionRatePct());
    request.setDividendAmountPerShare(payout.dividendAmountPerShare());
    request.setTaxableBasePerShare(payout.taxableBasePerShare());
    return request;
  }

  public MonthlyDividendPayoutImportRequest buildDefaultMonthlyDividendPayoutImportForm(
      String symbol) {
    MonthlyDividendPayoutImportRequest request = new MonthlyDividendPayoutImportRequest();
    request.setSymbol(StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null);
    return request;
  }

  public List<StockItem> loadStockItems() {
    return StockViewSupport.sortedBySymbol(stockItemClient.getStockItems());
  }

  public List<StockItem> loadMonthlyDividendStockItems() {
    List<StockItem> stockItems = stockItemClient.getStockItemsByTag(MONTHLY_DIVIDEND_TAG);
    if (stockItems == null || stockItems.isEmpty()) {
      return List.of();
    }

    return sortSelectableStockItems(stockItems);
  }

  public String normalizeMonthlyDividendSymbol(String symbol) {
    return StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null;
  }

  public void validateMonthlyDividendSymbol(String symbol) {
    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    if (!StringUtils.hasText(normalizedSymbol)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.required"));
    }

    if (normalizedSymbol.startsWith("HTTP://") || normalizedSymbol.startsWith("HTTPS://")) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.url"));
    }

    boolean knownStockSymbol =
        loadStockItems().stream()
                .map(StockItem::symbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals)
            || loadMonthlyDividendProfiles().stream()
                .map(MonthlyDividendProfileResponse::stockItemSymbol)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(normalizedSymbol::equals);
    if (!knownStockSymbol) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.unknown"));
    }
  }

  private int resolveNextMonthlyDividendProfileDisplayOrder(
      List<MonthlyDividendProfileResponse> profiles) {
    return profiles.stream()
        .map(MonthlyDividendProfileResponse::displayOrder)
        .filter(java.util.Objects::nonNull)
        .max(Integer::compareTo)
        .map(value -> value + 1)
        .orElse(1);
  }

  /** 조회에 실패하면 빈 맵이라 표시는 예전 그대로다(없는 값을 지어내지 않는다). */
  public CurrentHoldings loadCurrentHoldings(UUID userId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    params.add("groupBy", "STOCKITEM");
    Map<UUID, Integer> quantities = new HashMap<>();
    Map<UUID, BigDecimal> averageBuyPrices = new HashMap<>();
    java.time.LocalDate basisDate = null;
    try {
      List<TradeProfit> rows = tradeProfitClient.calculateProfit(params);
      basisDate =
          net.luversof.web.gate.stock.util.StockPriceBasisUtil.priceBasisDateWithFallback(rows);
      if (rows != null) {
        for (TradeProfit row : rows) {
          if (row.stockItemId() != null) {
            quantities.merge(row.stockItemId(), row.holdingQuantity(), Integer::sum);
            if (row.averageBuyPrice() != null) {
              averageBuyPrices.put(row.stockItemId(), row.averageBuyPrice());
            }
          }
        }
      }
    } catch (Exception ex) {
      log.warn("현재 보유 상태 조회 실패: userId={}", userId, ex);
      return CurrentHoldings.failed();
    }
    return new CurrentHoldings(quantities, averageBuyPrices, basisDate, false);
  }

  public List<MonthlyDividendSnapshotResponse> loadMonthlyDividendRows(UUID userId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    return monthlyDividendSnapshotClient.findSnapshots(params);
  }

  /** 원장의 현재 보유 수량(종목 단위, 계좌 합산). 조회에 실패하면 빈 맵이라 표시는 예전 그대로다. */
  /** 원장의 현재 보유 상태(종목 단위). 조회 한 번으로 수량과 평균단가를 함께 얻는다. */
  /**
   * 원장 기준 현재 보유 상태.
   *
   * <p>{@code priceBasisDate} 는 이 표의 '현재가' 가 어느 날 종가인지다. 월배당 표는 배당 기준값(스냅샷 저장 시점)과 시세(최근 종가) 두 시점을
   * 한 줄에 섞어 보여 주는데, 화면에는 앞의 날짜만 적혀 있었다 - 실측 2026-09-11: 스냅샷 기준일은 2026-08-19 인데 현재가는 2026-09-09
   * 종가였다.
   */
  public record CurrentHoldings(
      Map<UUID, Integer> quantities,
      Map<UUID, BigDecimal> averageBuyPrices,
      java.time.LocalDate priceBasisDate,
      boolean unavailable) {

    static CurrentHoldings empty() {
      return new CurrentHoldings(Map.of(), Map.of(), null, false);
    }

    /**
     * 조회 자체가 실패한 경우. 빈 맵과 반드시 구분해야 한다 - 이 표들은 원장과 어긋나는 줄에만 표시를 달므로, 빈 맵을 돌려주면 표시가 사라져 "원장과 같다" 로
     * 읽힌다.
     */
    static CurrentHoldings failed() {
      return new CurrentHoldings(Map.of(), Map.of(), null, true);
    }
  }
}
