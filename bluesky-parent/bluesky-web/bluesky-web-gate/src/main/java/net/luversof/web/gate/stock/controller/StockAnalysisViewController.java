package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.RecentPrediction;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 시황 분석 화면: 한 종목(기본 삼성전자)을 골라 기간 안의 일자별 예측 적중, 확신도 · 버전별 적중률, 회차 비교, 그 종목 지표 · 뉴스를 본다 (사용자 요청
 * 2026-11-20: 삼성전자만 분석하는데 화면은 전 종목을 보여줬고, 회차별 · 기간별 적중률은 표본이 1 건 수준이라 의미가 없었다). 데이터는 작업 스케줄러 스크립트가
 * 쌓는다(api-stock tools/market-analysis).
 */
@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockAnalysisViewController {

  private static final Logger log = LoggerFactory.getLogger(StockAnalysisViewController.class);

  /** 회차 비교 · 일자별 적중 한 쪽의 줄 수(사용자 요청 2026-11-15: 표가 너무 길다). */
  static final int PAGE_SIZE = 20;

  static final String DEFAULT_HISTORY_SYMBOL = "005930";

  /** 일자별 적중의 기간 고르기: 값 -> 화면 이름 메시지 키(all = 전부). */
  static final Map<String, String> PERIODS = new java.util.LinkedHashMap<>();

  static {
    PERIODS.put("1m", "stock.analysis.period.1m");
    PERIODS.put("3m", "stock.analysis.period.3m");
    PERIODS.put("6m", "stock.analysis.period.6m");
    PERIODS.put("1y", "stock.analysis.period.1y");
    PERIODS.put("all", "stock.analysis.period.all");
  }

  static final String DEFAULT_PERIOD = "3m";

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Autowired private StockAnalysisClient stockAnalysisClient;

  /** 한 기준일의 예측: 그날 고른 회차(운영 회차 우선)의 D+1 · D+5. 없는 기간은 null. */
  public record DailyRow(LocalDate baseDate, RecentPrediction d1, RecentPrediction d5) {

    /** 회차 정보는 D+1 · D+5 가 같은 회차라 있는 쪽에서 읽는다. */
    public RecentPrediction run() {
      return d1 != null ? d1 : d5;
    }
  }

  /** 일자별 적중 표의 기간별 적중 수 · 채점 수. 적중률은 채점한 것이 없으면 null. */
  public record DailyHits(int d1Hit, int d1Graded, int d5Hit, int d5Graded) {

    public BigDecimal d1RatePct() {
      return ratePct(d1Hit, d1Graded);
    }

    public BigDecimal d5RatePct() {
      return ratePct(d5Hit, d5Graded);
    }

    private static BigDecimal ratePct(int hit, int graded) {
      return graded == 0
          ? null
          : BigDecimal.valueOf(hit * 100L)
              .divide(BigDecimal.valueOf(graded), 1, RoundingMode.HALF_UP);
    }
  }

  @BlueskyPreAuthorize
  @GetMapping("/analysis")
  public String analysisPage(
      HttpServletRequest request,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) String period,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate runDate,
      @RequestParam(defaultValue = "false") boolean onlyDisagree,
      @RequestParam(defaultValue = "1") int runsPage,
      @RequestParam(defaultValue = "1") int dailyPage,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    String p = period != null && PERIODS.containsKey(period) ? period : DEFAULT_PERIOD;
    model.addAttribute("period", p);
    model.addAttribute("periods", PERIODS);
    model.addAttribute("onlyDisagree", onlyDisagree);
    try {
      StockAnalysisClient.AnalysisInput input = stockAnalysisClient.input(UserUtil.getUserId());
      List<StockAnalysisClient.AnalysisItem> items = input == null ? List.of() : input.items();
      // 종목 고르기: symbol, 없으면 검색어(예전 링크 ?keyword=)가 가리키는 한 종목, 그것도 없으면 삼성전자.
      StockAnalysisClient.AnalysisItem byKeyword = focusOf(keyword, items);
      String selected =
          historySymbolOf(
              symbol != null ? symbol : (byKeyword == null ? null : byKeyword.symbol()), items);
      model.addAttribute("symbolOptions", items);
      model.addAttribute("symbol", selected);
      model.addAttribute(
          "focus",
          items.stream().filter(i -> i.symbol().equals(selected)).findFirst().orElse(null));
      model.addAttribute("summary", stockAnalysisClient.summary(selected));
      // 쪽 버튼은 이 화면을 다시 불러 그 카드만 바꾼다(htmxPagination 이 runsPage · dailyPage 를 붙인다).
      model.addAttribute(
          "pageUrl",
          UriComponentsBuilder.fromPath("/stock/analysis")
              .queryParamIfPresent("symbol", java.util.Optional.ofNullable(selected))
              .queryParam("period", p)
              .queryParamIfPresent("runDate", java.util.Optional.ofNullable(runDate))
              .queryParam("onlyDisagree", onlyDisagree)
              .encode()
              .toUriString());

      if (selected != null) {
        StockAnalysisClient.SymbolHistory history = stockAnalysisClient.history(selected);
        List<DailyRow> daily = dailyRows(history.predictions(), periodStart(p, LocalDate.now(KST)));
        Page<DailyRow> dailyRows = pageOf(daily, dailyPage);
        model.addAttribute("dailyHits", dailyHits(daily));
        model.addAttribute("daily", dailyRows.getContent());
        model.addAttribute("dailyPagination", new Pagination(dailyRows));
      }

      // 회차 비교: 그날의 회차들을 기간별로 나란히(사용자 요청 2026-11-10), 고른 종목만.
      StockAnalysisClient.RunComparison runs = stockAnalysisClient.runs(runDate, selected);
      if (runs != null) {
        Page<StockAnalysisClient.RunRow> runRows =
            pageOf(
                runs.rows().stream().filter(r -> !onlyDisagree || r.disagree()).toList(), runsPage);
        runs =
            new StockAnalysisClient.RunComparison(
                runs.date(), runs.dates(), runs.runs(), runRows.getContent());
        model.addAttribute("runsPagination", new Pagination(runRows));
      }
      model.addAttribute("runs", runs);

      if (input != null) {
        // 종목 현황도 고른 종목만(사용자 요청 2026-11-20).
        input =
            new StockAnalysisClient.AnalysisInput(
                input.generatedAt(),
                input.market(),
                items.stream().filter(i -> i.symbol().equals(selected)).toList());
      }
      model.addAttribute("input", input);
    } catch (RuntimeException e) {
      // 화면 껍데기는 그대로 그리고 안내만 한다 - 다른 stock 화면과 같은 규칙.
      log.warn("market analysis load failed", e);
      model.addAttribute("loadError", true);
    }
    return "stock/analysis";
  }

  /** 기간의 첫 기준일. all 이면 null(전부). */
  static LocalDate periodStart(String period, LocalDate today) {
    return switch (period) {
      case "1m" -> today.minusMonths(1);
      case "3m" -> today.minusMonths(3);
      case "6m" -> today.minusMonths(6);
      case "1y" -> today.minusYears(1);
      default -> null;
    };
  }

  /**
   * 기준일마다 한 줄, 최근 날짜부터. 그날 회차가 여럿이면 운영 회차(리플레이 아님) 중 마지막, 운영 회차가 없으면 리플레이 중 마지막을 쓴다. 늦은 예측(장 시작 뒤)은
   * 적중률에서 빼므로 여기서도 뺀다.
   */
  static List<DailyRow> dailyRows(List<RecentPrediction> predictions, LocalDate from) {
    Map<LocalDate, List<RecentPrediction>> byDate =
        predictions.stream()
            .filter(p -> !Boolean.TRUE.equals(p.late()))
            .filter(p -> from == null || !p.baseDate().isBefore(from))
            .collect(
                Collectors.groupingBy(
                    RecentPrediction::baseDate, TreeMap::new, Collectors.toList()));
    Comparator<RecentPrediction> preferred =
        Comparator.comparing((RecentPrediction p) -> p.replay() == null)
            .thenComparing(
                RecentPrediction::createdDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparingInt(RecentPrediction::runSeq);
    return ((TreeMap<LocalDate, List<RecentPrediction>>) byDate)
        .descendingMap().entrySet().stream()
            .map(e -> {
              RecentPrediction pick = e.getValue().stream().max(preferred).orElseThrow();
              // 회차는 만든 날(KST)과 그날의 회차 번호로 가린다 - 한 회차의 D+1 · D+5 도 createdDate 가 밀리초씩 다르다.
              List<RecentPrediction> run =
                  e.getValue().stream()
                      .filter(p ->
                          Objects.equals(runDay(p), runDay(pick)) && p.runSeq() == pick.runSeq())
                      .toList();
              return new DailyRow(e.getKey(), horizon(run, 1), horizon(run, 5));
            })
            .toList();
  }

  static DailyHits dailyHits(List<DailyRow> rows) {
    int d1Hit = 0, d1Graded = 0, d5Hit = 0, d5Graded = 0;
    for (DailyRow r : rows) {
      if (r.d1() != null && r.d1().hit() != null) {
        d1Graded++;
        d1Hit += r.d1().hit() ? 1 : 0;
      }
      if (r.d5() != null && r.d5().hit() != null) {
        d5Graded++;
        d5Hit += r.d5().hit() ? 1 : 0;
      }
    }
    return new DailyHits(d1Hit, d1Graded, d5Hit, d5Graded);
  }

  private static LocalDate runDay(RecentPrediction p) {
    return p.createdDate() == null ? null : LocalDate.ofInstant(p.createdDate(), KST);
  }

  private static RecentPrediction horizon(List<RecentPrediction> run, int days) {
    return run.stream().filter(p -> p.horizonDays() == days).findFirst().orElse(null);
  }

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
