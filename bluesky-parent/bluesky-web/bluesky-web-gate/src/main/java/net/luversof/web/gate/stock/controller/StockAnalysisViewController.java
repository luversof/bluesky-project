package net.luversof.web.gate.stock.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
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

  @Autowired private StockAnalysisClient stockAnalysisClient;

  @BlueskyPreAuthorize
  @GetMapping("/analysis")
  public String analysisPage(
      HttpServletRequest request, @RequestParam(required = false) String keyword, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    try {
      model.addAttribute("summary", stockAnalysisClient.summary());
      String q = keyword == null ? "" : keyword.strip();
      model.addAttribute("keyword", q);
      model.addAttribute(
          "recent",
          stockAnalysisClient.recent(RECENT_DAYS).stream()
              .filter(p -> matches(q, p.name(), p.symbol()))
              .toList());
      StockAnalysisClient.AnalysisInput input = stockAnalysisClient.input(UserUtil.getUserId());
      model.addAttribute(
          "input",
          input == null
              ? null
              : new StockAnalysisClient.AnalysisInput(
                  input.generatedAt(),
                  input.market(),
                  input.items().stream().filter(i -> matches(q, i.name(), i.symbol())).toList()));
    } catch (RuntimeException e) {
      // 화면 껍데기는 그대로 그리고 안내만 한다 - 다른 stock 화면과 같은 규칙.
      log.warn("market analysis load failed", e);
      model.addAttribute("loadError", true);
    }
    return "stock/analysis";
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
