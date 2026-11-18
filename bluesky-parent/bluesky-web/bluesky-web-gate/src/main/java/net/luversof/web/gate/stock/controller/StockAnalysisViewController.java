package net.luversof.web.gate.stock.controller;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.common.menu.domain.Pagination;
import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 시황 분석 화면: 예측 적중률(기준선 대비), 최근 예측과 채점, 종목별 지표 · 뉴스. 데이터는 작업 스케줄러 스크립트가 쌓는다 (api-stock
 * tools/market-analysis).
 */
@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockAnalysisViewController {

  private static final Logger log = LoggerFactory.getLogger(StockAnalysisViewController.class);

  static final int RECENT_DAYS = 14;

  /** 최근 예측 · 회차 비교 · 종목 현황 한 쪽의 줄 수(사용자 요청 2026-11-15: 표가 너무 길다). */
  static final int PAGE_SIZE = 20;

  @Autowired private StockAnalysisClient stockAnalysisClient;

  @BlueskyPreAuthorize
  @GetMapping("/analysis")
  public String analysisPage(
      HttpServletRequest request,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate runDate,
      @RequestParam(defaultValue = "false") boolean onlyDisagree,
      @RequestParam(defaultValue = "1") int recentPage,
      @RequestParam(defaultValue = "1") int runsPage,
      @RequestParam(defaultValue = "1") int itemsPage,
      @RequestParam(required = false) String historySymbol,
      @RequestParam(defaultValue = "1") int historyPage,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    try {
      String q = keyword == null ? "" : keyword.strip();
      model.addAttribute("keyword", q);
      StockAnalysisClient.AnalysisInput input = stockAnalysisClient.input(UserUtil.getUserId());
      // 검색이 분석 대상 한 종목만 가리키면 적중률 · 회차 비교 통계도 그 종목 기준(사용자 요청 2026-11-15).
      StockAnalysisClient.AnalysisItem focus = input == null ? null : focusOf(q, input.items());
      String focusSymbol = focus == null ? null : focus.symbol();
      model.addAttribute("focus", focus);
      model.addAttribute("summary", stockAnalysisClient.summary(focusSymbol));
      List<StockAnalysisClient.RecentPrediction> recent =
          stockAnalysisClient.recent(RECENT_DAYS).stream()
              .filter(p -> matches(q, p.name(), p.symbol()))
              .toList();
      Page<StockAnalysisClient.RecentPrediction> recentRows = pageOf(recent, recentPage);
      model.addAttribute("recent", recentRows.getContent());
      model.addAttribute("recentPagination", new Pagination(recentRows));
      // 쪽 버튼은 이 화면을 다시 불러 그 카드만 바꾼다(htmxPagination 이 recentPage · runsPage · itemsPage 를 붙인다).
      model.addAttribute(
          "recentPageUrl",
          UriComponentsBuilder.fromPath("/stock/analysis")
              .queryParam("keyword", q)
              .queryParamIfPresent("runDate", java.util.Optional.ofNullable(runDate))
              .queryParam("onlyDisagree", onlyDisagree)
              .queryParamIfPresent("historySymbol", java.util.Optional.ofNullable(historySymbol))
              .encode()
              .toUriString());
      // 회차 비교: 그날의 회차들을 종목 · 기간별로 나란히(사용자 요청 2026-11-10). 종목 검색 · 불일치만 보기를 함께 건다.
      StockAnalysisClient.RunComparison runs = stockAnalysisClient.runs(runDate, focusSymbol);
      model.addAttribute("onlyDisagree", onlyDisagree);
      if (runs != null) {
        Page<StockAnalysisClient.RunRow> runRows =
            pageOf(
                runs.rows().stream()
                    .filter(r -> matches(q, r.name(), r.symbol()))
                    .filter(r -> !onlyDisagree || r.disagree())
                    .toList(),
                runsPage);
        runs =
            new StockAnalysisClient.RunComparison(
                runs.date(), runs.dates(), runs.runs(), runRows.getContent());
        model.addAttribute("runsPagination", new Pagination(runRows));
      }
      model.addAttribute("runs", runs);
      if (input != null) {
        // 종목별 예측(사용자 요청 2026-11-15): 고른 종목의 일자별 예측 · 적중률. 기본은 삼성전자, 없으면 첫 대상.
        String symbol =
            historySymbolOf(historySymbol != null ? historySymbol : focusSymbol, input.items());
        model.addAttribute("historyOptions", input.items());
        model.addAttribute("historySymbol", symbol);
        if (symbol != null) {
          StockAnalysisClient.SymbolHistory history = stockAnalysisClient.history(symbol);
          Page<StockAnalysisClient.RecentPrediction> historyRows =
              pageOf(history.predictions(), historyPage);
          model.addAttribute("historyRates", history.byVersion());
          model.addAttribute("history", historyRows.getContent());
          model.addAttribute("historyPagination", new Pagination(historyRows));
        }
        Page<StockAnalysisClient.AnalysisItem> itemRows =
            pageOf(
                input.items().stream().filter(i -> matches(q, i.name(), i.symbol())).toList(),
                itemsPage);
        input =
            new StockAnalysisClient.AnalysisInput(
                input.generatedAt(), input.market(), itemRows.getContent());
        model.addAttribute("itemsPagination", new Pagination(itemRows));
      }
      model.addAttribute("input", input);
    } catch (RuntimeException e) {
      // 화면 껍데기는 그대로 그리고 안내만 한다 - 다른 stock 화면과 같은 규칙.
      log.warn("market analysis load failed", e);
      model.addAttribute("loadError", true);
    }
    return "stock/analysis";
  }

  static final String DEFAULT_HISTORY_SYMBOL = "005930";

  /** 검색어가 분석 대상 하나에만 맞으면 그 종목, 아니면(빈 검색 · 여러 종목) null. 코드가 정확히 같으면 그것을 고른다. */
  static StockAnalysisClient.AnalysisItem focusOf(
      String keyword, List<StockAnalysisClient.AnalysisItem> items) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    List<StockAnalysisClient.AnalysisItem> hits =
        items.stream().filter(i -> matches(keyword, i.name(), i.symbol())).toList();
    if (hits.size() == 1) {
      return hits.get(0);
    }
    return hits.stream().filter(i -> keyword.strip().equals(i.symbol())).findFirst().orElse(null);
  }

  /** 고른 종목이 분석 대상이면 그것, 아니면 삼성전자, 그것도 없으면 첫 대상. */
  static String historySymbolOf(String wanted, List<StockAnalysisClient.AnalysisItem> items) {
    for (String s : new String[] {wanted, DEFAULT_HISTORY_SYMBOL}) {
      if (s != null && items.stream().anyMatch(i -> s.equals(i.symbol()))) {
        return s;
      }
    }
    return items.isEmpty() ? null : items.get(0).symbol();
  }

  /** 1 부터 세는 쪽 번호. 범위를 벗어나면 가장 가까운 쪽. */
  static <T> Page<T> pageOf(List<T> all, int page) {
    int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    int index = Math.clamp(page, 1, pages) - 1;
    int from = index * PAGE_SIZE;
    return new PageImpl<>(
        all.subList(from, Math.min(from + PAGE_SIZE, all.size())),
        PageRequest.of(index, PAGE_SIZE),
        all.size());
  }

  /** 종목 검색: 종목명 · 코드에 검색어가 들어 있으면(대소문자 무시). 검색어가 비면 전부. */
  static boolean matches(String keyword, String name, String symbol) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    String q = keyword.toLowerCase(java.util.Locale.ROOT);
    return (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(q))
        || (symbol != null && symbol.toLowerCase(java.util.Locale.ROOT).contains(q));
  }
}
