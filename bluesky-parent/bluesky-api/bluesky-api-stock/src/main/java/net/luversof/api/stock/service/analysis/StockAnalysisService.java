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

  public record NewsLine(
      String source, String title, String summary, String url, Instant publishedDate) {}

  public record AnalysisItem(
      String symbol,
      String name,
      LocalDate baseDate,
      BigDecimal lastClose,
      Indicators indicators,
      List<BigDecimal> recentCloses,
      List<NewsLine> news) {}

  public record AnalysisInput(Instant generatedAt, List<AnalysisItem> items) {}

  public record PredictionInput(
      String symbol, Integer horizonDays, String direction, Integer confidence, String rationale) {}

  public record SubmitRequest(
      String modelId, String promptVersion, List<PredictionInput> predictions) {}

  public record SubmitResult(int saved, int skippedExisting, List<String> rejected) {}

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

  public AnalysisInput buildInput() {
    List<StockItem> targets = findTargets();
    Map<UUID, List<StockNews>> newsByItem = recentNewsByItem();
    List<AnalysisItem> items =
        targets.stream()
            .map(item -> buildItem(item, newsByItem.getOrDefault(item.getId(), List.of())))
            .toList();
    return new AnalysisInput(Instant.now(), items);
  }

  private Map<UUID, List<StockNews>> recentNewsByItem() {
    Instant since = Instant.now().minus(NEWS_DAYS, ChronoUnit.DAYS);
    return stockNewsRepository.findByPublishedDateAfter(since).stream()
        .collect(Collectors.groupingBy(StockNews::getStockItemId));
  }

  private AnalysisItem buildItem(StockItem item, List<StockNews> news) {
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
        PredictionMath.indicators(bars),
        bars.stream().skip(Math.max(0, bars.size() - RECENT_CLOSES)).map(Bar::close).toList(),
        newsLines);
  }

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
    Map<String, AnalysisItem> builtItems = new HashMap<>();

    int saved = 0;
    int skipped = 0;
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
              p.symbol(), s -> buildItem(item, newsByItem.getOrDefault(item.getId(), List.of())));
      if (built.baseDate() == null) {
        rejected.add(p.symbol() + ": 시세 없음");
        continue;
      }
      if (stockPredictionRepository.existsByStockItemIdAndBaseDateAndHorizonDays(
          item.getId(), built.baseDate(), p.horizonDays())) {
        skipped++;
        continue;
      }
      StockPrediction prediction = new StockPrediction();
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
      Map<String, Object> snapshot = jsonMapper.convertValue(built, Map.class);
      prediction.setInputSnapshot(snapshot);
      stockPredictionRepository.save(prediction);
      saved++;
    }
    return new SubmitResult(saved, skipped, rejected);
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

  public record RateRow(
      String group,
      int horizonDays,
      int graded,
      BigDecimal hitRatePct,
      BigDecimal alwaysUpRatePct,
      BigDecimal prevDirectionRatePct) {}

  public record Summary(
      int predictions,
      int pending,
      int late,
      List<RateRow> byHorizon,
      List<RateRow> byConfidence,
      List<RateRow> byPromptVersion) {}

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
      LocalDate targetDate,
      BigDecimal actualChangePct,
      String actualDirection,
      Boolean hit,
      Boolean late) {}

  private record Graded(StockPrediction prediction, StockPredictionGrade grade) {}

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

    return new Summary(
        predictions.size(),
        predictions.size() - grades.size(),
        late,
        rates(onTime, g -> "all"),
        rates(onTime, g -> confidenceBucket(g.prediction().getConfidence())),
        rates(onTime, g -> g.prediction().getPromptVersion()));
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
          List<Graded> withPrev =
              list.stream().filter(g -> g.grade().getPrevDirectionHit() != null).toList();
          rows.add(
              new RateRow(
                  group,
                  horizon,
                  list.size(),
                  ratePct(list, g -> g.grade().isHit()),
                  ratePct(list, g -> g.grade().isAlwaysUpHit()),
                  ratePct(withPrev, g -> g.grade().getPrevDirectionHit())));
        }));
    return rows;
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
              g == null ? null : g.getTargetDate(),
              g == null ? null : g.getActualChangePct(),
              g == null ? null : g.getActualDirection(),
              g == null ? null : g.isHit(),
              g == null ? null : g.isLate());
        })
        .sorted(
            Comparator.comparing(RecentPrediction::baseDate)
                .reversed()
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
