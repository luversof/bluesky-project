package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    public static BigDecimal ratePct(int hit, int graded) {
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
      @RequestParam(required = false) String version,
      @RequestParam(required = false) String slot,
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
    StockAnalysisClient.ReleaseNotes releases = null;
    try {
      // 버전 기록(사용자 요청 2026-11-21): 따로 받는다 - 못 받아도 나머지 화면은 그린다.
      releases = stockAnalysisClient.releases();
      model.addAttribute("releases", releases);
    } catch (RuntimeException e) {
      log.warn("market analysis releases load failed", e);
    }
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
      StockAnalysisClient.SymbolHistory history =
          selected == null ? null : stockAnalysisClient.history(selected);
      // 일자별 표 버전(사용자 요청 2026-11-22): 기본은 가장 최근 채택 버전 - 재분석으로 모든 날짜에 있다. 운영 우선은 고를 때만.
      List<String> versions = history == null ? List.of() : versionKeys(history.predictions());
      String adopted = latestAdopted(releases, versions);
      String dailyVersion =
          version != null && (versions.contains(version) || LIVE_FIRST.equals(version))
              ? version
              : adopted != null ? adopted : LIVE_FIRST;
      model.addAttribute("dailyVersions", versions);
      model.addAttribute("adoptedVersion", adopted);
      model.addAttribute("dailyVersion", dailyVersion);
      String dailySlot = slot != null && SLOTS.contains(slot) ? slot : DEFAULT_SLOT;
      model.addAttribute("slots", SLOTS);
      model.addAttribute("dailySlot", dailySlot);
      // 쪽 버튼은 이 화면을 다시 불러 그 카드만 바꾼다(htmxPagination 이 runsPage · dailyPage 를 붙인다).
      model.addAttribute(
          "pageUrl",
          UriComponentsBuilder.fromPath("/stock/analysis")
              .queryParamIfPresent("symbol", java.util.Optional.ofNullable(selected))
              .queryParam("period", p)
              .queryParam("version", dailyVersion)
              .queryParam("slot", dailySlot)
              .queryParamIfPresent("runDate", java.util.Optional.ofNullable(runDate))
              .queryParam("onlyDisagree", onlyDisagree)
              .encode()
              .toUriString());

      if (history != null) {
        LocalDate from = periodStart(p, LocalDate.now(KST));
        // 시점 고르기(2026-11-27): 버전은 같은 (기준일, 시점) 에서만 비교한다 - 일자별 표 · 버전 비교 · 확신도는 고른 시점만.
        List<RecentPrediction> ofSlot = ofSlot(history.predictions(), dailySlot);
        model.addAttribute("upcoming", upcoming(history.predictions()));
        model.addAttribute("slotHits", slotHits(history.predictions(), dailyVersion, from));
        List<DailyRow> daily = dailyRows(ofSlot, dailyVersion, from);
        Page<DailyRow> dailyRows = pageOf(daily, dailyPage);
        model.addAttribute("dailyHits", dailyHits(daily));
        model.addAttribute("confidenceCuts", confidenceCuts(daily));
        model.addAttribute("daily", dailyRows.getContent());
        model.addAttribute("dailyPagination", new Pagination(dailyRows));
        model.addAttribute("versionCompare", versionCompare(ofSlot, from));
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

  /**
   * 채점된 예측의 실제 종가(사용자 요청 2026-11-21: 적중 표시만 말고 주가가 실제로 어떻게 움직였는지): 기준가 x (1 + 변화율). 기준가나 변화율이
   * 없으면(채점 전) null.
   */
  public static BigDecimal actualClose(StockAnalysisClient.RecentPrediction p) {
    if (p == null || p.basePrice() == null || p.actualChangePct() == null) {
      return null;
    }
    return p.basePrice()
        .multiply(BigDecimal.ONE.add(p.actualChangePct().movePointLeft(2)))
        .setScale(0, RoundingMode.HALF_UP);
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
    return dailyRows(predictions, from, false);
  }

  /** includeLate: 장중 시점 적중을 따로 셀 때만 늦은 예측도 넣는다. */
  static List<DailyRow> dailyRows(
      List<RecentPrediction> predictions, LocalDate from, boolean includeLate) {
    Map<LocalDate, List<RecentPrediction>> byDate =
        predictions.stream()
            .filter(p -> includeLate || !Boolean.TRUE.equals(p.late()))
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

  /**
   * 분석 시점(api-stock AnalysisSlot, 2026-11-27): 16:00 종가 뒤 · 20:00 저녁 · 06:00 야간선물 뒤 · 08:30 장전가 뒤.
   * 고르기 기본은 PREMARKET - 그 전부터 쌓인 아침 분석과 같은 시점이다. all 은 시점을 섞는다(그날 운영 회차 우선).
   */
  static final List<String> SLOTS = List.of("PREMARKET", "NIGHT", "EVENING", "CLOSE", "all");

  static final String DEFAULT_SLOT = "PREMARKET";

  /** 시점별 적중 표의 순서. INTRADAY 는 장이 열린 뒤 만든 실시간 예측(늦은 예측)이라 다른 표에서는 빠지고 여기서만 센다. */
  static final List<String> SLOT_ORDER =
      List.of("CLOSE", "EVENING", "NIGHT", "PREMARKET", "INTRADAY");

  static List<RecentPrediction> ofSlot(List<RecentPrediction> predictions, String slot) {
    return "all".equals(slot)
        ? predictions
        : predictions.stream().filter(p -> slot.equals(p.slot())).toList();
  }

  public record SlotHits(String slot, DailyHits hits) {}

  /** 버전 하나의 시점별 적중(사용자 결정 2026-11-27: 언제 돌린 예측이 얼마나 맞나). 채점된 날이 없는 시점은 뺀다. */
  static List<SlotHits> slotHits(
      List<RecentPrediction> predictions, String version, LocalDate from) {
    List<RecentPrediction> ofVersion =
        LIVE_FIRST.equals(version)
            ? predictions
            : predictions.stream().filter(p -> compareKey(p).equals(version)).toList();
    return SLOT_ORDER.stream()
        .map(s ->
            new SlotHits(s, dailyHits(dailyRows(ofSlot(ofVersion, s), from, "INTRADAY".equals(s)))))
        .filter(h -> h.hits().d1Graded() + h.hits().d5Graded() > 0)
        .toList();
  }

  /** 다음 거래일 예측의 회차 하나: 만든 시각 · 시점 · 버전 키 · 실시간(replay 없음) 여부와 D+1 · D+5. */
  public record UpcomingRun(
      Instant createdDate,
      String slot,
      String version,
      boolean live,
      RecentPrediction d1,
      RecentPrediction d5) {}

  public record Upcoming(LocalDate baseDate, List<UpcomingRun> runs) {
    /** 예측 대상일(D+1): 채점됐으면 그 날, 아니면 기준일 다음 평일. */
    public LocalDate targetDate() {
      return runs.stream()
          .map(UpcomingRun::d1)
          .filter(p -> p != null && p.targetDate() != null)
          .map(RecentPrediction::targetDate)
          .findFirst()
          .orElseGet(() -> {
            // shortcut: 휴장일은 모른다 - 채점 전에는 평일만 건너뛴다
            LocalDate d = baseDate.plusDays(1);
            while (d.getDayOfWeek().getValue() > 5) {
              d = d.plusDays(1);
            }
            return d;
          });
    }
  }

  /**
   * 다음 거래일 예측(사용자 요청 2026-11-27: 전날 다음 날을 미리 본다): 가장 최근 기준일의 회차를 모두, 늦게 만든 것부터. 아무 때나 돌린 회차가 다 쌓여
   * 그날 예측이 시점마다 어떻게 바뀌었는지 보인다. 예측이 없으면 null.
   */
  static Upcoming upcoming(List<RecentPrediction> predictions) {
    LocalDate base =
        predictions.stream()
            .map(RecentPrediction::baseDate)
            .max(Comparator.naturalOrder())
            .orElse(null);
    if (base == null) {
      return null;
    }
    Map<String, List<RecentPrediction>> byRun =
        predictions.stream()
            .filter(p -> base.equals(p.baseDate()))
            .collect(
                Collectors.groupingBy(
                    p -> runDay(p) + "#" + p.runSeq(), LinkedHashMap::new, Collectors.toList()));
    List<UpcomingRun> runs =
        byRun.values().stream()
            .map(run -> {
              RecentPrediction first = run.get(0);
              return new UpcomingRun(
                  run.stream()
                      .map(RecentPrediction::createdDate)
                      .filter(Objects::nonNull)
                      .max(Comparator.naturalOrder())
                      .orElse(null),
                  first.slot(),
                  compareKey(first),
                  first.replay() == null,
                  horizon(run, 1),
                  horizon(run, 5));
            })
            .sorted(
                Comparator.comparing(
                    UpcomingRun::createdDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    return new Upcoming(base, runs);
  }

  /** 재분석 회차의 리플레이 이름 앞부분 - api-stock StockAnalysisService.REANALYSIS_PREFIX 와 같다. */
  static final String REANALYSIS_PREFIX = "재분석 ";

  /** 빠진 날 채움 회차의 리플레이 이름 - 운영 회차와 같은 버전으로 센다(morning.ps1 $CatchUpLabel). */
  static final String CATCH_UP_LABEL = "누락채움";

  /**
   * 같은 날짜 버전 비교(사용자 요청 2026-11-21: 개선한 버전으로 예전 날짜를 다시 분석해 나아졌는지). versions 의 첫째가 비교 기준(가장 늦게 나온
   * 프롬프트 버전). rows 는 두 버전 이상이 예측한 날만, 최근 날짜부터.
   */
  public record VersionCompare(
      List<String> versions, List<CompareRow> rows, List<PairHits> vsNewest) {}

  public record CompareRow(LocalDate baseDate, Map<String, DailyRow> cells) {}

  /** version 과 기준 버전이 둘 다 채점된 날만 센 적중 수. */
  public record PairHits(
      String version,
      int d1Days,
      int d1Hit,
      int d1NewestHit,
      int d5Days,
      int d5Hit,
      int d5NewestHit) {}

  /** 비교 열: 운영 · 빠진 날 채움은 프롬프트 버전, 재분석은 재분석한 버전, 그 밖의 리플레이(실험)는 버전 · 이름. */
  static String compareKey(RecentPrediction p) {
    if (p.replay() == null || CATCH_UP_LABEL.equals(p.replay())) {
      return p.promptVersion();
    }
    if (p.replay().startsWith(REANALYSIS_PREFIX)) {
      return p.replay().substring(REANALYSIS_PREFIX.length());
    }
    return p.promptVersion() + " · " + p.replay();
  }

  /** 일자별 표 버전 고르기에서 예전 동작(그날 운영 회차 우선, 버전 섞임)을 뜻하는 값. */
  static final String LIVE_FIRST = "live";

  /** 버전 기록(최신순)에서 가장 최근 채택된 버전 - 이름의 첫 낱말이 예측에 있는 버전 키인 것(예: "v10 확신도 보정" 은 v10). 없으면 null. */
  static String latestAdopted(StockAnalysisClient.ReleaseNotes notes, List<String> versions) {
    if (notes == null || notes.releases() == null) {
      return null;
    }
    return notes.releases().stream()
        .filter(r -> "ADOPTED".equals(r.verdict()) && r.version() != null)
        .map(r -> r.version().split(" ")[0])
        .filter(versions::contains)
        .findFirst()
        .orElse(null);
  }

  /** 일자별 표: version 이 LIVE_FIRST 면 모든 회차에서 운영 우선, 아니면 그 버전(compareKey) 회차만. */
  static List<DailyRow> dailyRows(
      List<RecentPrediction> predictions, String version, LocalDate from) {
    return dailyRows(
        LIVE_FIRST.equals(version)
            ? predictions
            : predictions.stream().filter(p -> compareKey(p).equals(version)).toList(),
        from);
  }

  /** 버전 키: 프롬프트 버전 · 실험 리플레이 · 규칙 순, 그 안에서 처음 나온 때가 늦은 것이 앞. */
  static List<String> versionKeys(List<RecentPrediction> predictions) {
    return versionKeys(
        predictions.stream()
            .collect(Collectors.groupingBy(StockAnalysisViewController::compareKey)));
  }

  private static List<String> versionKeys(Map<String, List<RecentPrediction>> byKey) {
    Map<String, Instant> firstSeen = new LinkedHashMap<>();
    byKey.forEach((k, v) ->
        firstSeen.put(
            k,
            v.stream()
                .map(RecentPrediction::createdDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(Instant.EPOCH)));
    return firstSeen.keySet().stream()
        .sorted(
            Comparator.comparingInt((String k) -> k.equals("rule") ? 2 : k.contains(" · ") ? 1 : 0)
                .thenComparing(
                    StockAnalysisViewController::versionNumber, Comparator.reverseOrder())
                .thenComparing(firstSeen::get, Comparator.reverseOrder()))
        .toList();
  }

  /** "v10" · "v9 · 뉴스없음" 의 번호, 없으면 -1. 재분석 회차는 만든 시각이 예전 날짜로 찍혀서 처음 나온 때만으로는 새 버전을 못 가린다. */
  private static int versionNumber(String key) {
    var m = java.util.regex.Pattern.compile("^v(\\d+)").matcher(key);
    return m.find() ? Integer.parseInt(m.group(1)) : -1;
  }

  static VersionCompare versionCompare(List<RecentPrediction> predictions, LocalDate from) {
    Map<String, List<RecentPrediction>> byKey =
        predictions.stream()
            .collect(Collectors.groupingBy(StockAnalysisViewController::compareKey));
    // 기준은 가장 새 프롬프트 버전(versionKeys 의 첫째).
    List<String> versions = versionKeys(byKey);
    Map<String, Map<LocalDate, DailyRow>> cells = new LinkedHashMap<>();
    for (String v : versions) {
      Map<LocalDate, DailyRow> rows = new LinkedHashMap<>();
      dailyRows(byKey.get(v), from).forEach(r -> rows.put(r.baseDate(), r));
      cells.put(v, rows);
    }
    List<CompareRow> rows =
        cells.values().stream()
            .flatMap(m -> m.keySet().stream())
            .distinct()
            .sorted(Comparator.reverseOrder())
            .map(d -> {
              Map<String, DailyRow> row = new LinkedHashMap<>();
              cells.forEach((v, m) -> {
                if (m.containsKey(d)) {
                  row.put(v, m.get(d));
                }
              });
              return new CompareRow(d, row);
            })
            .filter(r -> r.cells().size() >= 2)
            .toList();
    List<String> shown =
        versions.stream()
            .filter(v -> rows.stream().anyMatch(r -> r.cells().containsKey(v)))
            .toList();
    List<PairHits> vsNewest = new java.util.ArrayList<>();
    if (!shown.isEmpty()) {
      Map<LocalDate, DailyRow> newest = cells.get(shown.get(0));
      for (String v : shown.subList(1, shown.size())) {
        int[] c = new int[6];
        cells
            .get(v)
            .forEach((d, r) -> {
              DailyRow n = newest.get(d);
              if (n != null) {
                count(c, 0, r.d1(), n.d1());
                count(c, 3, r.d5(), n.d5());
              }
            });
        if (c[0] + c[3] > 0) { // 기준 버전과 같이 채점된 날이 없으면 비교할 것이 없다
          vsNewest.add(new PairHits(v, c[0], c[1], c[2], c[3], c[4], c[5]));
        }
      }
    }
    return new VersionCompare(shown, rows, vsNewest);
  }

  /** c[at]: 둘 다 채점된 날 수, c[at+1]: 이 버전 적중, c[at+2]: 기준 버전 적중. */
  private static void count(int[] c, int at, RecentPrediction mine, RecentPrediction newest) {
    if (mine == null || newest == null || mine.hit() == null || newest.hit() == null) {
      return;
    }
    c[at]++;
    c[at + 1] += mine.hit() ? 1 : 0;
    c[at + 2] += newest.hit() ? 1 : 0;
  }

  /** 확신도 기준(사용자 요청 2026-11-23: 확신 높은 날만 따르면 얼마나 맞나). 0 은 전체. */
  static final List<Integer> CONFIDENCE_CUTS = List.of(0, 50, 60, 70, 80);

  /** 확신도 min 이상인 채점된 날만 센 적중. d1Days 를 전체 채점 날 수와 견주면 따르는 날의 비율이다. */
  public record ConfidenceCut(int min, DailyHits hits) {}

  static List<ConfidenceCut> confidenceCuts(List<DailyRow> rows) {
    return CONFIDENCE_CUTS.stream()
        .map(min ->
            new ConfidenceCut(
                min,
                dailyHits(
                    rows.stream()
                        .map(r ->
                            new DailyRow(
                                r.baseDate(),
                                r.d1() != null && r.d1().confidence() >= min ? r.d1() : null,
                                r.d5() != null && r.d5().confidence() >= min ? r.d5() : null))
                        .toList())))
        // 그 기준 이상인 날이 하나도 없으면 줄을 뺀다(전체 줄은 남긴다)
        .filter(c -> c.min() == 0 || c.hits().d1Graded() + c.hits().d5Graded() > 0)
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
