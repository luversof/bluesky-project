package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView;
import net.luversof.web.gate.stock.httpexchange.MonthlyDividendCatalogClient;
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

  @Autowired private MonthlyDividendCatalogClient monthlyDividendCatalogClient;

  @Autowired private MonthlyDividendReferenceSupport monthlyDividendReferenceSupport;

  @Autowired private MonthlyEtfViewSupport monthlyEtfViewSupport;

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
      @RequestParam(required = false) Integer period) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }

    UUID userId = UserUtil.getUserId();
    String resolvedSort = monthlyEtfViewSupport.resolveSort(sort);
    String resolvedDirection = monthlyEtfViewSupport.resolveDirection(resolvedSort, direction);
    String resolvedHolding = monthlyEtfViewSupport.resolveHolding(holding);
    String resolvedPayoutWindow = monthlyEtfViewSupport.resolvePayoutWindow(payoutWindow);
    String resolvedKeyword = keyword != null ? keyword.trim() : "";

    int resolvedPeriod = monthlyEtfViewSupport.resolvePeriod(period);
    List<MonthlyEtfRowView> allRows = loadRows(userId, resolvedPeriod);
    List<MonthlyEtfRowView> rows =
        monthlyEtfViewSupport.sortRows(
            monthlyEtfViewSupport.filterRows(
                allRows, resolvedKeyword, minAnnualYield, resolvedPayoutWindow, resolvedHolding),
            resolvedSort,
            resolvedDirection);

    model.addAttribute("monthlyEtfRows", rows);
    model.addAttribute("monthlyEtfTotalCount", allRows.size());
    model.addAttribute(
        "monthlyEtfHeldCount", allRows.stream().filter(MonthlyEtfRowView::held).count());
    model.addAttribute(
        "monthlyEtfMissingPayoutCount",
        allRows.stream().filter(row -> row.payoutCount() <= 0).count());
    model.addAttribute("monthlyEtfSort", resolvedSort);
    model.addAttribute("monthlyEtfDirection", resolvedDirection);
    model.addAttribute("monthlyEtfKeyword", resolvedKeyword);
    model.addAttribute("monthlyEtfMinAnnualYield", minAnnualYield);
    model.addAttribute("monthlyEtfPayoutWindow", resolvedPayoutWindow);
    model.addAttribute("monthlyEtfHolding", resolvedHolding);
    model.addAttribute("monthlyEtfPeriod", resolvedPeriod);
    model.addAttribute("monthlyEtfPeriods", MonthlyEtfViewSupport.PERIODS);
    model.addAttribute("pageTitle", StockViewSupport.msg("stock.page.monthly.etf.title"));
    return "stock/monthlyEtf";
  }

  /** 등록된 프로필 전부 + 내 원장의 보유 여부. 보유 수량 · 금액은 화면에 싣지 않는다. */
  private List<MonthlyEtfRowView> loadRows(UUID userId, int period) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    List<MonthlyDividendCatalogResponse> catalog = monthlyDividendCatalogClient.findCatalog(params);
    Map<UUID, Integer> heldQuantities =
        monthlyDividendReferenceSupport.loadCurrentHoldings(userId).quantities();

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
              quantity != null && quantity > 0));
    }
    return rows;
  }
}
