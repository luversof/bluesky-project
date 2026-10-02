package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendCatalogClient;
import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport;
import net.luversof.web.gate.stock.service.MonthlyDividendReferenceSupport;
import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 월배당 ETF 목록 화면.
 *
 * <p>사용자 요청 2026-09-21: "보유하지 않은 종목 등록이 많아질 테니 월배당 ETF 정보를 보여 주는 메뉴가 따로 있어야 할 것 같다. 등록된 데이터를 기준으로
 * 정렬하고 검색할 수 있는 메뉴다." 시뮬레이터의 월배당 탭은 <b>내가 받을 배당금</b>을 다루므로 보유 종목만 두고, 성격이 다른 종목 정보는 이 화면이 맡는다.
 *
 * <p>줄은 등록된 월배당 프로필 전부다(보유 여부와 무관). 금액 · 수량은 적지 않고, 보유 중인지만 표시한다.
 */
@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockMonthlyEtfViewController {

  private static final Logger log = LoggerFactory.getLogger(StockMonthlyEtfViewController.class);

  @Autowired private MonthlyDividendCatalogClient monthlyDividendCatalogClient;

  @Autowired private MonthlyDividendReferenceSupport monthlyDividendReferenceSupport;

  @Autowired private MonthlyEtfViewSupport monthlyEtfViewSupport;

  @Autowired private MonthlyContributionPickSupport monthlyContributionPickSupport;

  @Autowired private net.luversof.web.gate.stock.support.StockAsyncSupport stockAsync;

  @BlueskyPreAuthorize
  @GetMapping("/monthly-etf")
  public String monthlyEtfPage(
      HttpServletRequest request,
      Model model,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BigDecimal minAnnualYield,
      @RequestParam(required = false) String payoutWindow,
      @RequestParam(required = false) String holding,
      @RequestParam(required = false) String account,
      @RequestParam(required = false) String view,
      @RequestParam(required = false) Integer period) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    UUID userId = UserUtil.getUserId();
    String resolvedSort = monthlyEtfViewSupport.resolveSort(sort);
    String resolvedDirection = monthlyEtfViewSupport.resolveDirection(resolvedSort, direction);
    String resolvedHolding = monthlyEtfViewSupport.resolveHolding(holding);
    String resolvedPayoutWindow = monthlyEtfViewSupport.resolvePayoutWindow(payoutWindow);
    String resolvedAccount = monthlyEtfViewSupport.resolveAccount(account);
    String resolvedKeyword = keyword != null ? keyword.trim() : "";

    int resolvedPeriod = monthlyEtfViewSupport.resolvePeriod(period);
    // 서로 의존이 없는 원격 호출 셋을 먼저 다 던지고 받는다. 예전에는 카탈로그 -> 원장 보유 -> 월배당 스냅샷을 차례로
    // 불러 카탈로그(~30ms)가 끝나야 다음이 나갔다(실측 2026-09-23: 둘째 호출이 +101ms 에 출발).
    var catalogFuture =
        stockAsync.supply(
            () -> monthlyDividendCatalogClient.findCatalog(new LinkedMultiValueMap<>()));
    var holdingsFuture =
        stockAsync.supply(() -> monthlyDividendReferenceSupport.loadCurrentHoldings(userId));
    List<MonthlyDividendCatalogResponse> catalog =
        net.luversof.web.gate.stock.support.StockAsyncSupport.join(catalogFuture);
    List<MonthlyEtfRowView> allRows =
        loadRows(
            resolvedPeriod,
            catalog,
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(holdingsFuture));
    // "이번 적립" 배지 - 시뮬레이터 월배당 탭(필터 없는 기본 화면)과 같은 답을 내야 한다(사용자 요청 2026-09-22 의 잇기).
    // 거르기 전에 낸다 - "이번 적립만 보기" 가 이것으로 거른다(사용자 요청 2026-09-23).
    Map<String, MonthlyContributionPickSupport.ContributionPick> contributionPicks =
        loadContributionPicks(catalog);
    String resolvedView = monthlyEtfViewSupport.resolveView(view);
    // 자리별 적립 추천 보기는 자리 차례(월중 → 월말, 위탁 → ISA/연금)로 놓는다(사용자 요청 2026-09-30). 전체 보기면 표시 순서.
    String viewSort = monthlyEtfViewSupport.resolveSortForView(resolvedSort, resolvedView);
    List<MonthlyEtfRowView> rows =
        monthlyEtfViewSupport.sortRows(
            monthlyEtfViewSupport.filterContribution(
                monthlyEtfViewSupport.filterRows(
                    allRows,
                    resolvedKeyword,
                    minAnnualYield,
                    resolvedPayoutWindow,
                    resolvedHolding,
                    resolvedAccount),
                contributionPicks.keySet(),
                resolvedView),
            viewSort,
            resolvedDirection,
            List.copyOf(contributionPicks.keySet()));

    model.addAttribute("monthlyEtfRows", rows);
    model.addAttribute("monthlyEtfContributionPicks", contributionPicks);
    model.addAttribute("monthlyEtfView", resolvedView);
    // 추천은 걸러 놓은 목록 안에서 고른다 - 화면에 안 보이는 종목을 추천하면 안 된다.
    model.addAttribute("monthlyEtfPicks", monthlyEtfViewSupport.pickRows(rows));
    model.addAttribute("monthlyEtfTotalCount", allRows.size());
    model.addAttribute(
        "monthlyEtfHeldCount", allRows.stream().filter(MonthlyEtfRowView::held).count());
    model.addAttribute(
        "monthlyEtfMissingPayoutCount",
        allRows.stream().filter(row -> row.payoutCount() <= 0).count());
    model.addAttribute("monthlyEtfSort", viewSort);
    model.addAttribute("monthlyEtfDirection", resolvedDirection);
    model.addAttribute("monthlyEtfKeyword", resolvedKeyword);
    model.addAttribute("monthlyEtfMinAnnualYield", minAnnualYield);
    model.addAttribute("monthlyEtfPayoutWindow", resolvedPayoutWindow);
    model.addAttribute("monthlyEtfHolding", resolvedHolding);
    model.addAttribute("monthlyEtfAccount", resolvedAccount);
    model.addAttribute("monthlyEtfPeriod", resolvedPeriod);
    model.addAttribute("monthlyEtfPeriods", MonthlyEtfViewSupport.PERIODS);
    model.addAttribute("pageTitle", StockViewSupport.msg("stock.page.monthly.etf.title"));
    return "stock/monthlyEtf";
  }

  /**
   * 종목코드 -> 이번 적립 자리. 등록된 종목 전부가 후보다(보유 여부와 상관없이 - 사용자 요청 2026-10-02). 실패해도 목록은 살린다 - 배지는 덧붙인 것이고,
   * 실패는 로그로 남긴다.
   */
  private Map<String, MonthlyContributionPickSupport.ContributionPick> loadContributionPicks(
      List<MonthlyDividendCatalogResponse> catalog) {
    try {
      Map<String, MonthlyContributionPickSupport.ContributionPick> bySymbol =
          new java.util.LinkedHashMap<>();
      for (var pick : monthlyContributionPickSupport.pickFromCatalog(catalog)) {
        bySymbol.putIfAbsent(pick.symbol(), pick);
      }
      return bySymbol;
    } catch (Exception ex) {
      log.warn("이번 적립 배지를 못 냈다(목록은 그대로 둔다)", ex);
      return Map.of();
    }
  }

  /** 등록된 프로필 전부 + 내 원장의 보유 여부. 보유 수량 · 금액은 화면에 싣지 않는다. */
  private List<MonthlyEtfRowView> loadRows(
      int period,
      List<MonthlyDividendCatalogResponse> catalog,
      MonthlyDividendReferenceSupport.CurrentHoldings holdings) {
    Map<UUID, Integer> heldQuantities = holdings.quantities();

    List<MonthlyEtfRowView> rows = new ArrayList<>();
    for (MonthlyDividendCatalogResponse row : catalog) {
      Integer quantity = row.stockItemId() != null ? heldQuantities.get(row.stockItemId()) : null;
      // 고른 기간의 수익률만 행에 싣는다(없으면 null - 화면이 "이력 시작일" 을 대신 적는다).
      MonthlyDividendCatalogResponse.PeriodReturnView periodReturn =
          row.periodReturns() == null
              ? null
              : row.periodReturns().stream()
                  .filter(one -> one.months() == period)
                  .findFirst()
                  .orElse(null);
      rows.add(
          new MonthlyEtfRowView(
              row.stockItemId(),
              row.stockItemSymbol(),
              row.stockItemName(),
              row.payoutWindow(),
              row.sourceUrl(),
              row.lastVerifiedDate(),
              row.active(),
              row.displayOrder(),
              row.payoutCount(),
              row.latestRecordDate(),
              row.latestPayDate(),
              row.latestDividendPerShare(),
              row.averageDividendPerShare1y(),
              row.averageTaxableBaseRatio1y(),
              row.averageTaxableBasePerShare1y(),
              row.currentPrice(),
              row.currentPriceDate(),
              row.monthlyYieldPct(),
              row.annualYieldPct(),
              row.priceHistoryStartDate(),
              periodReturn != null ? periodReturn.priceReturnPct() : null,
              periodReturn != null ? periodReturn.totalReturnPct() : null,
              periodReturn != null ? periodReturn.baseDate() : null,
              row.averageDividendPerShare3m(),
              row.payoutTrendPct(),
              row.maxDrawdownPct(),
              row.volatilityPct(),
              row.riskFromDate(),
              quantity != null && quantity > 0,
              row.totalExpenseRatioPct(),
              row.listingDate()));
    }
    return rows;
  }
}
