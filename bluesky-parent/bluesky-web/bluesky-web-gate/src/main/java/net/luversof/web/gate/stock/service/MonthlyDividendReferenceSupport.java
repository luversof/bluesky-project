package net.luversof.web.gate.stock.service;

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

  private List<StockItem> mergeMonthlyDividendReferenceStockItems(
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

    return List.copyOf(stockItemsBySymbol.values());
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

    return stockItems.stream()
        .filter(stockItem -> stockItem != null)
        .sorted(
            Comparator.comparing(
                stockItem -> StockViewSupport.safeString(stockItem.symbol()),
                String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public String normalizeMonthlyDividendSymbol(String symbol) {
    return StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null;
  }

  public void validateMonthlyDividendSymbol(String symbol) {
    String normalizedSymbol = normalizeMonthlyDividendSymbol(symbol);
    if (!StringUtils.hasText(normalizedSymbol)) {
      throw new IllegalArgumentException("종목코드는 필수입니다.");
    }

    if (normalizedSymbol.startsWith("HTTP://") || normalizedSymbol.startsWith("HTTPS://")) {
      throw new IllegalArgumentException(
          "종목코드에는 URL이 아니라 종목 심볼을 입력해 주세요. 출처 URL은 출처 URL 칸에 넣으면 됩니다.");
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
      throw new IllegalArgumentException("등록되지 않은 종목코드입니다. 종목 심볼을 다시 확인해 주세요.");
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
    try {
      List<TradeProfit> rows = tradeProfitClient.calculateProfit(params);
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
      return CurrentHoldings.empty();
    }
    return new CurrentHoldings(quantities, averageBuyPrices);
  }

  public List<MonthlyDividendSnapshotResponse> loadMonthlyDividendRows(UUID userId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("userId", userId.toString());
    return monthlyDividendSnapshotClient.findSnapshots(params);
  }

  /** 원장의 현재 보유 수량(종목 단위, 계좌 합산). 조회에 실패하면 빈 맵이라 표시는 예전 그대로다. */
  /** 원장의 현재 보유 상태(종목 단위). 조회 한 번으로 수량과 평균단가를 함께 얻는다. */
  public record CurrentHoldings(
      Map<UUID, Integer> quantities, Map<UUID, BigDecimal> averageBuyPrices) {

    static CurrentHoldings empty() {
      return new CurrentHoldings(Map.of(), Map.of());
    }
  }
}
