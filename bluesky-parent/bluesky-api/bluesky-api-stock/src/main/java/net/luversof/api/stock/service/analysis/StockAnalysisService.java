package net.luversof.api.stock.service.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemTag;
import net.luversof.api.stock.domain.StockNews;
import net.luversof.api.stock.domain.StockPrediction;
import net.luversof.api.stock.domain.StockPredictionGrade;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockItemTagRepository;
import net.luversof.api.stock.repository.StockNewsRepository;
import net.luversof.api.stock.repository.StockPredictionGradeRepository;
import net.luversof.api.stock.repository.StockPredictionRepository;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.service.analysis.PredictionMath.Bar;
import net.luversof.api.stock.service.analysis.PredictionMath.Direction;
import net.luversof.api.stock.service.analysis.PredictionMath.Indicators;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시황 분석: 대상 종목 · 분석 입력 · 예측 저장 · 채점 · 적중률.
 *
 * <p>예측 자체는 바깥(작업 스케줄러가 돌리는 {@code claude -p})이 만들고 여기는 받아서 검증 · 저장만 한다. 기준일과 기준가는 받은 값을 믿지 않고 DB
 * 에서 다시 정한다.
 */
@Service
public class StockAnalysisService {

  /** 보유 종목 말고도 분석하고 싶은 종목에 붙이는 태그. */
  public static final String ANALYSIS_TAG = "analysis";

  static final int NEWS_DAYS = 2;
  static final int NEWS_PER_ITEM = 15;
  static final int RECENT_CLOSES = 10;

  @Autowired private TradeRepository tradeRepository;
  @Autowired private StockItemRepository stockItemRepository;
  @Autowired private StockItemTagRepository stockItemTagRepository;
  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;
  @Autowired private StockNewsRepository stockNewsRepository;
  @Autowired private StockPredictionRepository stockPredictionRepository;
  @Autowired private StockPredictionGradeRepository stockPredictionGradeRepository;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private MarketContextService marketContextService;

  public record NewsLine(
      String source, String title, String summary, String url, Instant publishedDate) {}

  public record AnalysisItem(
      String symbol,
      String name,
      LocalDate baseDate,
      BigDecimal lastClose,
      List<String> tags,
      Indicators indicators,
      List<BigDecimal> recentCloses,
      List<NewsLine> news) {}

  /** market: 간밤 미국 · 환율 · 금리 · 국내 지수(모든 종목 공통, 2026-11-10 v2). */
  public record AnalysisInput(
      Instant generatedAt, MarketContextService.MarketContext market, List<AnalysisItem> items) {}

  public record PredictionInput(
      String symbol, Integer horizonDays, String direction, Integer confidence, String rationale) {}

  public record SubmitRequest(
      String modelId, String promptVersion, List<PredictionInput> predictions) {}

  /** runId: 이번 제출로 저장한 예측 묶음(분석 회차). */
  public record SubmitResult(UUID runId, int saved, List<String> rejected) {}

  public record GradeResult(int graded, int pending) {}

  public record DeleteResult(int deleted, int keptGraded) {}

  /** 현재 보유 종목 + analysis 태그 종목. 이름순. */
  public List<StockItem> findTargets() {
    Set<UUID> ids = new HashSet<>(tradeRepository.findCurrentlyHeldStockItemIds());
    stockItemTagRepository.findByTag(ANALYSIS_TAG).stream()
        .map(StockItemTag::getStockItemId)
        .forEach(ids::add);
    List<StockItem> items = new ArrayList<>();
    stockItemRepository.findAllById(ids).forEach(items::add);
    items.sort(Comparator.comparing(StockItem::getName));
    return items;
  }

  /** userId: KIS 키 주인(시장 상황을 KIS 로 가져온다). 없으면 시장 상황은 비고 unavailable 에 모두 적힌다. */
  public AnalysisInput buildInput(UUID userId) {
    List<StockItem> targets = findTargets();
    Map<UUID, List<StockNews>> newsByItem = recentNewsByItem();
    Map<UUID, List<String>> tagsByItem = tagsByItem(targets);
    List<AnalysisItem> items =
        targets.stream()
            .map(item ->
                buildItem(
                    item,
                    newsByItem.getOrDefault(item.getId(), List.of()),
                    tagsByItem.getOrDefault(item.getId(), List.of())))
            .toList();
    return new AnalysisInput(Instant.now(), marketContextService.fetch(userId), items);
  }

  /** 종목 태그(월배당 · 커버드콜 · 리츠 등) - ETF 의 기초자산을 가늠하는 단서. 분석 대상 표식(analysis)은 뺀다. */
  private Map<UUID, List<String>> tagsByItem(List<StockItem> items) {
    return stockItemTagRepository
        .findByStockItemIdIn(items.stream().map(StockItem::getId).toList())
        .stream()
        .filter(tag -> !ANALYSIS_TAG.equals(tag.getTag()))
        .collect(
            Collectors.groupingBy(
                StockItemTag::getStockItemId,
                Collectors.mapping(StockItemTag::getTag, Collectors.toList())));
  }

  private Map<UUID, List<StockNews>> recentNewsByItem() {
    Instant since = Instant.now().minus(NEWS_DAYS, ChronoUnit.DAYS);
    return stockNewsRepository.findByPublishedDateAfter(since).stream()
        .collect(Collectors.groupingBy(StockNews::getStockItemId));
  }

  private AnalysisItem buildItem(StockItem item, List<StockNews> news, List<String> tags) {
    List<StockPriceHistory> rows =
        new ArrayList<>(
            stockPriceHistoryRepository.findTop30ByStockItemIdOrderByTradeDateDesc(item.getId()));
    Collections.reverse(rows);
    List<Bar> bars =
        rows.stream()
            .filter(row -> row.getClosePrice() != null)
            .map(row -> new Bar(row.getTradeDate(), row.getClosePrice(), row.getVolume()))
            .toList();
    Bar last = bars.isEmpty() ? null : bars.get(bars.size() - 1);
    List<NewsLine> newsLines =
        news.stream()
            .sorted(Comparator.comparing(StockNews::getPublishedDate).reversed())
            .limit(NEWS_PER_ITEM)
            .map(n ->
                new NewsLine(
                    n.getSource(), n.getTitle(), n.getSummary(), n.getUrl(), n.getPublishedDate()))
            .toList();
    return new AnalysisItem(
        item.getSymbol(),
        item.getName(),
        last == null ? null : last.tradeDate(),
        last == null ? null : last.close(),
        tags,
        PredictionMath.indicators(bars),
        bars.stream().skip(Math.max(0, bars.size() - RECENT_CLOSES)).map(Bar::close).toList(),
        newsLines);
  }

  @Transactional
  public SubmitResult submit(SubmitRequest request) {
    if (request == null
        || isBlank(request.modelId())
        || request.modelId().length() > 100
        || isBlank(request.promptVersion())
        || request.promptVersion().length() > 50
        || request.predictions() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "modelId(<=100), promptVersion(<=50), predictions are required");
    }
    Map<String, StockItem> targets =
        findTargets().stream()
            .collect(Collectors.toMap(StockItem::getSymbol, Function.identity(), (a, b) -> a));
    Map<UUID, List<StockNews>> newsByItem = recentNewsByItem();
    Map<UUID, List<String>> tagsByItem = tagsByItem(List.copyOf(targets.values()));
    Map<String, AnalysisItem> builtItems = new HashMap<>();

    // 분석 회차(사용자 결정 2026-11-10): 하루 몇 번이든 돌릴 때마다 따로 쌓고, 회차끼리 적중률을 비교해 개선을 판단한다.
    UUID runId = UUID.randomUUID();
    int saved = 0;
    List<String> rejected = new ArrayList<>();
    for (PredictionInput p : request.predictions()) {
      String reason = validate(p, targets);
      if (reason != null) {
        rejected.add(reason);
        continue;
      }
      StockItem item = targets.get(p.symbol());
      AnalysisItem built =
          builtItems.computeIfAbsent(
              p.symbol(),
              s ->
                  buildItem(
                      item,
                      newsByItem.getOrDefault(item.getId(), List.of()),
                      tagsByItem.getOrDefault(item.getId(), List.of())));
      if (built.baseDate() == null) {
        rejected.add(p.symbol() + ": 시세 없음");
        continue;
      }
      StockPrediction prediction = new StockPrediction();
      prediction.setRunId(runId);
      prediction.setStockItemId(item.getId());
      prediction.setBaseDate(built.baseDate());
      prediction.setBasePrice(built.lastClose());
      prediction.setHorizonDays(p.horizonDays());
      prediction.setDirection(Direction.valueOf(p.direction()).name());
      prediction.setConfidence(p.confidence());
      prediction.setRationale(
          p.rationale() == null || p.rationale().length() <= 1000
              ? p.rationale()
              : p.rationale().substring(0, 1000));
      prediction.setModelId(request.modelId());
      prediction.setPromptVersion(request.promptVersion());
      @SuppressWarnings("unchecked")
      Map<String, Object> snapshot = new HashMap<>(jsonMapper.convertValue(built, Map.class));
      // 예측이 본 시장 상황도 함께 남긴다(아침 작업이 입력을 만들 때 가져온 것 - 30 분 캐시).
      snapshot.put(
          "market", jsonMapper.convertValue(marketContextService.lastFetched(), Map.class));
      prediction.setInputSnapshot(snapshot);
      stockPredictionRepository.save(prediction);
      saved++;
    }
    return new SubmitResult(saved == 0 ? null : runId, saved, rejected);
  }

  static String validate(PredictionInput p, Map<String, StockItem> targets) {
    if (p == null || p.symbol() == null) {
      return "symbol 없음";
    }
    if (!targets.containsKey(p.symbol())) {
      return p.symbol() + ": 분석 대상이 아님";
    }
    if (p.horizonDays() == null || (p.horizonDays() != 1 && p.horizonDays() != 5)) {
      return p.symbol() + ": horizonDays 는 1 또는 5";
    }
    if (p.direction() == null
        || java.util.Arrays.stream(Direction.values())
            .noneMatch(d -> d.name().equals(p.direction()))) {
      return p.symbol() + ": direction 은 UP/DOWN/FLAT";
    }
    if (p.confidence() == null || p.confidence() < 0 || p.confidence() > 100) {
      return p.symbol() + ": confidence 는 0~100";
    }
    return null;
  }

  /**
   * 기준일의 채점 전 예측을 지운다. 다시 예측하려면 PC 에서 아침 작업을 돌린다 - 예측은 그때 DB 의 마지막 종가 날짜로 정해지므로 지난 기준일은 다시 만들어지지
   * 않는다(만들어도 늦은 예측이라 통계에서 빠진다).
   */
  public DeleteResult deleteUngraded(LocalDate baseDate) {
    int kept = stockPredictionRepository.countGradedByBaseDate(baseDate);
    return new DeleteResult(stockPredictionRepository.deleteUngradedByBaseDate(baseDate), kept);
  }

  /** 만기 종가가 들어온 예측을 채점한다. 이미 채점한 것은 건드리지 않는다. */
  public GradeResult grade() {
    int graded = 0;
    int pending = 0;
    for (StockPrediction p : stockPredictionRepository.findUngraded()) {
      List<StockPriceHistory> after =
          stockPriceHistoryRepository
              .findTop10ByStockItemIdAndTradeDateGreaterThanOrderByTradeDateAsc(
                  p.getStockItemId(), p.getBaseDate());
      if (after.size() < p.getHorizonDays()) {
        pending++;
        continue;
      }
      StockPriceHistory target = after.get(p.getHorizonDays() - 1);
      List<StockPriceHistory> baseAndPrev =
          stockPriceHistoryRepository
              .findTop2ByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
                  p.getStockItemId(), p.getBaseDate());
      stockPredictionGradeRepository.save(
          gradeOf(p, target, after.get(0).getTradeDate(), baseAndPrev));
      graded++;
    }
    return new GradeResult(graded, pending);
  }

  // shortcut: 기준가는 예측 때의 수정 종가다 - 그 사이 분할 · 병합으로 수정 종가가 다시 매겨지면 변화율이 틀린다. 드물어서 그대로 둔다.
  static StockPredictionGrade gradeOf(
      StockPrediction p,
      StockPriceHistory target,
      LocalDate firstTradingDayAfterBase,
      List<StockPriceHistory> baseAndPrev) {
    BigDecimal band = PredictionMath.flatBandPct(p.getHorizonDays());
    BigDecimal change = PredictionMath.changePct(p.getBasePrice(), target.getClosePrice());
    Direction actual = PredictionMath.classify(change, band);

    Boolean prevDirectionHit = null;
    if (baseAndPrev.size() == 2) {
      BigDecimal prevChange =
          PredictionMath.changePct(
              baseAndPrev.get(1).getClosePrice(), baseAndPrev.get(0).getClosePrice());
      if (prevChange != null) {
        prevDirectionHit =
            PredictionMath.classify(prevChange, PredictionMath.flatBandPct(1)) == actual;
      }
    }

    StockPredictionGrade grade = new StockPredictionGrade();
    grade.setPredictionId(p.getId());
    grade.setTargetDate(target.getTradeDate());
    grade.setActualClose(target.getClosePrice());
    grade.setActualChangePct(change);
    grade.setActualDirection(actual.name());
    grade.setHit(actual.name().equals(p.getDirection()));
    grade.setLate(PredictionMath.isLate(p.getCreatedDate(), firstTradingDayAfterBase));
    grade.setAlwaysUpHit(actual == Direction.UP);
    grade.setPrevDirectionHit(prevDirectionHit);
    return grade;
  }

  // ---- 적중률 ----

  /**
   * 기준선(사용자 요청 2026-11-10: 적중률은 단순 규칙보다 높아야 의미가 있다).
   *
   * <ul>
   *   <li>alwaysUp: 항상 UP
   *   <li>trend: 직전 같은 기간(D+1 은 전일, D+5 는 직전 5 거래일) 방향이 이어진다 - 입력 기록의 recentCloses 로 계산
   *   <li>majority: 그 묶음에서 실제로 가장 많이 나온 방향 하나만 찍었을 때(사후에 정해지므로 단순 규칙의 상한)
   *   <li>edge: 적중률 - 세 기준선 중 가장 높은 것(%p). 0 이하면 단순 규칙보다 나을 게 없다.
   * </ul>
   */
  public record RateRow(
      String group,
      int horizonDays,
      int graded,
      BigDecimal hitRatePct,
      BigDecimal alwaysUpRatePct,
      BigDecimal trendRatePct,
      BigDecimal majorityRatePct,
      BigDecimal edgePct) {}

  public record Summary(
      int predictions,
      int pending,
      int late,
      List<RateRow> byHorizon,
      List<RateRow> byConfidence,
      List<RateRow> byPromptVersion,
      List<RateRow> byRun) {}

  public record RecentPrediction(
      String symbol,
      String name,
      LocalDate baseDate,
      int horizonDays,
      String direction,
      int confidence,
      String rationale,
      BigDecimal basePrice,
      String promptVersion,
      Instant createdDate,
      int runSeq,
      LocalDate targetDate,
      BigDecimal actualChangePct,
      String actualDirection,
      Boolean hit,
      Boolean late) {}

  record Graded(StockPrediction prediction, StockPredictionGrade grade) {}

  /** 늦은 예측은 뺀다(장중 정보가 섞여 적중률을 부풀린다). */
  public Summary summary() {
    List<StockPrediction> predictions =
        StreamSupport.stream(stockPredictionRepository.findAll().spliterator(), false).toList();
    Map<UUID, StockPredictionGrade> grades = gradesOf(predictions);
    List<Graded> onTime =
        predictions.stream()
            .filter(p -> grades.containsKey(p.getId()) && !grades.get(p.getId()).isLate())
            .map(p -> new Graded(p, grades.get(p.getId())))
            .toList();
    int late = (int) grades.values().stream().filter(StockPredictionGrade::isLate).count();
    Map<UUID, Integer> runSeqs = runSeqs(predictions);

    return new Summary(
        predictions.size(),
        predictions.size() - grades.size(),
        late,
        rates(onTime, g -> "all"),
        rates(onTime, g -> confidenceBucket(g.prediction().getConfidence())),
        rates(onTime, g -> g.prediction().getPromptVersion()),
        rates(
            predictions.stream()
                .filter(p -> grades.containsKey(p.getId()))
                .map(p -> new Graded(p, grades.get(p.getId())))
                .toList(),
            g -> runLabel(g, runSeqs.getOrDefault(runKey(g.prediction()), 0))));
  }

  private static final java.time.format.DateTimeFormatter RUN_DATE =
      java.time.format.DateTimeFormatter.ofPattern("MM-dd").withZone(PredictionMath.MARKET_ZONE_ID);
  private static final java.time.format.DateTimeFormatter RUN_TIME =
      java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(PredictionMath.MARKET_ZONE_ID);

  /**
   * 회차 이름: 분석 날짜(KST) · 그날 몇 번째 분석인지 · 시각 · 프롬프트 버전(예: 11-10 #2 20:18 v2). 장중(기준일 다음 거래일 09:00 이후)에
   * 돌린 회차는 장중 정보가 섞여 표시만 하고 다른 표에서는 뺀다.
   */
  static String runLabel(Graded g, int seq) {
    StockPrediction p = g.prediction();
    if (p.getCreatedDate() == null) {
      return "? " + p.getPromptVersion();
    }
    return RUN_DATE.format(p.getCreatedDate())
        + " #"
        + seq
        + " "
        + RUN_TIME.format(p.getCreatedDate())
        + " "
        + p.getPromptVersion()
        + (g.grade().isLate() ? " (late)" : "");
  }

  /** 회차 키. runId 가 없는 행(회차 기능 전 앱이 넣은 것)은 schema-alter 의 채우기와 같은 기준(모델 · 버전 · 분)으로 묶는다. */
  static UUID runKey(StockPrediction p) {
    if (p.getRunId() != null) {
      return p.getRunId();
    }
    String minute =
        p.getCreatedDate() == null
            ? ""
            : p.getCreatedDate().truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString();
    return UUID.nameUUIDFromBytes(
        (p.getModelId() + "|" + p.getPromptVersion() + "|" + minute)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** 회차 키 -> 분석 날짜(KST)에서 몇 번째 분석인지(1 부터, 회차의 첫 저장 시각 순). */
  static Map<UUID, Integer> runSeqs(List<StockPrediction> predictions) {
    Map<UUID, Instant> started = new HashMap<>();
    for (StockPrediction p : predictions) {
      if (p.getCreatedDate() != null) {
        started.merge(runKey(p), p.getCreatedDate(), (a, b) -> a.isBefore(b) ? a : b);
      }
    }
    Map<UUID, Integer> seqs = new HashMap<>();
    started.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getValue().atZone(PredictionMath.MARKET_ZONE_ID).toLocalDate()))
        .values()
        .forEach(runs -> {
          List<Map.Entry<UUID, Instant>> sorted =
              runs.stream().sorted(Map.Entry.comparingByValue()).toList();
          for (int i = 0; i < sorted.size(); i++) {
            seqs.put(sorted.get(i).getKey(), i + 1);
          }
        });
    return seqs;
  }

  static String confidenceBucket(int confidence) {
    if (confidence >= 85) {
      return "85-100";
    }
    if (confidence >= 70) {
      return "70-84";
    }
    if (confidence >= 50) {
      return "50-69";
    }
    return "0-49";
  }

  private static List<RateRow> rates(List<Graded> graded, Function<Graded, String> groupOf) {
    Map<String, Map<Integer, List<Graded>>> grouped =
        graded.stream()
            .collect(
                Collectors.groupingBy(
                    groupOf,
                    java.util.TreeMap::new,
                    Collectors.groupingBy(
                        g -> g.prediction().getHorizonDays(),
                        java.util.TreeMap::new,
                        Collectors.toList())));
    List<RateRow> rows = new ArrayList<>();
    grouped.forEach((group, byHorizon) ->
        byHorizon.forEach((horizon, list) -> {
          List<Graded> withTrend = list.stream().filter(g -> trendHit(g) != null).toList();
          BigDecimal hit = ratePct(list, g -> g.grade().isHit());
          BigDecimal alwaysUp = ratePct(list, g -> g.grade().isAlwaysUpHit());
          BigDecimal trend = ratePct(withTrend, StockAnalysisService::trendHit);
          BigDecimal majority = majorityRatePct(list);
          BigDecimal best =
              java.util.stream.Stream.of(alwaysUp, trend, majority)
                  .filter(java.util.Objects::nonNull)
                  .max(Comparator.naturalOrder())
                  .orElse(null);
          rows.add(
              new RateRow(
                  group,
                  horizon,
                  list.size(),
                  hit,
                  alwaysUp,
                  trend,
                  majority,
                  hit == null || best == null ? null : hit.subtract(best)));
        }));
    return rows;
  }

  /** 직전 같은 기간의 방향이 이어졌는가. 입력 기록이 없으면 D+1 만 채점 때 남긴 전일 방향으로 대신한다. */
  static Boolean trendHit(Graded g) {
    int horizon = g.prediction().getHorizonDays();
    Map<String, Object> snapshot = g.prediction().getInputSnapshot();
    Object raw = snapshot == null ? null : snapshot.get("recentCloses");
    if (raw instanceof List<?> closes && closes.size() > horizon) {
      BigDecimal from = decimal(closes.get(closes.size() - 1 - horizon));
      BigDecimal to = decimal(closes.get(closes.size() - 1));
      BigDecimal change = PredictionMath.changePct(from, to);
      if (change != null) {
        return PredictionMath.classify(change, PredictionMath.flatBandPct(horizon))
            .name()
            .equals(g.grade().getActualDirection());
      }
    }
    return horizon == 1 ? g.grade().getPrevDirectionHit() : null;
  }

  private static BigDecimal decimal(Object value) {
    return value == null ? null : new BigDecimal(String.valueOf(value));
  }

  static BigDecimal majorityRatePct(List<Graded> list) {
    if (list.isEmpty()) {
      return null;
    }
    long top =
        list.stream()
            .collect(
                Collectors.groupingBy(g -> g.grade().getActualDirection(), Collectors.counting()))
            .values()
            .stream()
            .max(Long::compare)
            .orElse(0L);
    return BigDecimal.valueOf(top * 100)
        .divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP);
  }

  static <T> BigDecimal ratePct(List<T> list, Predicate<T> hit) {
    if (list.isEmpty()) {
      return null;
    }
    long hits = list.stream().filter(hit).count();
    return BigDecimal.valueOf(hits * 100)
        .divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP);
  }

  public List<RecentPrediction> recent(int days) {
    LocalDate since = LocalDate.now(PredictionMath.MARKET_ZONE_ID).minusDays(days);
    List<StockPrediction> predictions =
        stockPredictionRepository.findByBaseDateGreaterThanEqual(since);
    Map<UUID, StockPredictionGrade> grades = gradesOf(predictions);
    // shortcut: 그날 몇 번째인지 세려고 전체 예측을 읽는다(하루 수십 건 규모), 수만 건을 넘으면 회차 표를 따로 둔다.
    Map<UUID, Integer> runSeqs =
        runSeqs(
            StreamSupport.stream(stockPredictionRepository.findAll().spliterator(), false)
                .toList());
    Map<UUID, StockItem> items = new LinkedHashMap<>();
    stockItemRepository
        .findAllById(
            predictions.stream().map(StockPrediction::getStockItemId).collect(Collectors.toSet()))
        .forEach(item -> items.put(item.getId(), item));
    return predictions.stream()
        .map(p -> {
          StockItem item = items.get(p.getStockItemId());
          StockPredictionGrade g = grades.get(p.getId());
          return new RecentPrediction(
              item == null ? null : item.getSymbol(),
              item == null ? null : item.getName(),
              p.getBaseDate(),
              p.getHorizonDays(),
              p.getDirection(),
              p.getConfidence(),
              p.getRationale(),
              p.getBasePrice(),
              p.getPromptVersion(),
              p.getCreatedDate(),
              runSeqs.getOrDefault(runKey(p), 0),
              g == null ? null : g.getTargetDate(),
              g == null ? null : g.getActualChangePct(),
              g == null ? null : g.getActualDirection(),
              g == null ? null : g.isHit(),
              g == null ? null : g.isLate());
        })
        .sorted(
            Comparator.comparing(
                    RecentPrediction::createdDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(r -> r.name() == null ? "" : r.name())
                .thenComparingInt(RecentPrediction::horizonDays))
        .toList();
  }

  private Map<UUID, StockPredictionGrade> gradesOf(List<StockPrediction> predictions) {
    if (predictions.isEmpty()) {
      return Map.of();
    }
    return stockPredictionGradeRepository
        .findByPredictionIdIn(predictions.stream().map(StockPrediction::getId).toList())
        .stream()
        .collect(Collectors.toMap(StockPredictionGrade::getPredictionId, Function.identity()));
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }
}
