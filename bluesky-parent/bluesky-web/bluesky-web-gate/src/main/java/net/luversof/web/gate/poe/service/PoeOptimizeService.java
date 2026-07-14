package net.luversof.web.gate.poe.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.Deflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 시뮬레이터 2단계 — 최적 조합 탐색.
 *
 * <p>선택한 스킬젬 + 목표(DPS/유효 체력)에 대해 <b>보조젬 → 패시브 트리 → 고유 아이템</b> 순서의 단계별 greedy 탐색을 수행한다. 모든 후보는 PoB
 * 엔진 실계산으로 평가한다(회당 ~1.5초, 병렬 실행). 트리는 젬/목표 관련 키워드로 노터블·키스톤 후보를 추리고, BFS 최단 경로 비용 대비 실측 이득이 가장 큰 경로를
 * 반복 할당한다(포인트 예산 100). 결과는 PoB 공유 코드로 내보낸다.
 */
@Service
public class PoeOptimizeService {

  private static final Logger logger = LoggerFactory.getLogger(PoeOptimizeService.class);

  private static final int LOG_LIMIT = 200;
  private static final int LEVEL = 90;
  private static final int POINT_BUDGET = 100;
  private static final int MAX_SUPPORTS = 5;
  private static final int SUPPORT_SHORTLIST = 24;
  private static final int TREE_ROUND_CANDIDATES = 12;
  private static final int TREE_MAX_ROUNDS = 30;
  private static final int ITEM_CANDIDATES = 10;

  private static final Map<String, Integer> CLASS_IDS =
      Map.of(
          "Scion", 0,
          "Marauder", 1,
          "Ranger", 2,
          "Witch", 3,
          "Duelist", 4,
          "Templar", 5,
          "Shadow", 6);

  private static final Map<String, String> CLASS_KO =
      Map.of(
          "Scion", "사이온",
          "Marauder", "머라우더",
          "Ranger", "레인저",
          "Witch", "위치",
          "Duelist", "듀얼리스트",
          "Templar", "템플러",
          "Shadow", "섀도우");

  public enum Status {
    IDLE,
    SUCCESS,
    FAILED
  }

  /** 장비 슬롯 (PoB 슬롯명 + 한국어 + 고유 아이템 category 매핑) */
  private enum Slot {
    WEAPON("Weapon 1", "무기"),
    BODY("Body Armour", "갑옷", "body"),
    HELMET("Helmet", "투구", "helmet"),
    GLOVES("Gloves", "장갑", "gloves"),
    BOOTS("Boots", "장화", "boots"),
    AMULET("Amulet", "목걸이", "amulet"),
    RING1("Ring 1", "반지 1", "ring"),
    RING2("Ring 2", "반지 2", "ring"),
    BELT("Belt", "허리띠", "belt");

    final String pobName;
    final String ko;
    final List<String> categories;

    Slot(String pobName, String ko, String... categories) {
      this.pobName = pobName;
      this.ko = ko;
      this.categories = List.of(categories);
    }
  }

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoePobEngineService poePobEngineService;
  private final PoeTreeGraphService poeTreeGraphService;
  private final Path resultFile;
  private final String treeVersion;
  private final int parallelism;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger phaseDone = new AtomicInteger();
  private final AtomicInteger evalCount = new AtomicInteger();
  private volatile int phaseTotal;
  private volatile String phase = "";
  private volatile Status lastStatus = Status.IDLE;
  private final Deque<String> logLines = new ArrayDeque<>();
  private volatile PoeOptimizeResult lastResult;

  public PoeOptimizeService(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoePobEngineService poePobEngineService,
      PoeTreeGraphService poeTreeGraphService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.sim.tree-version:3_28}") String treeVersion,
      @Value("${poe.sim.parallelism:6}") int parallelism) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poePobEngineService = poePobEngineService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.resultFile = Path.of(dataDir, "sim", "optimize-last.json");
    this.treeVersion = treeVersion;
    this.parallelism = parallelism;
    loadLastResult();
  }

  private void loadLastResult() {
    if (!Files.exists(resultFile)) {
      return;
    }
    try {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      this.lastResult = jsonMapper.readValue(Files.readString(resultFile), PoeOptimizeResult.class);
      logger.info("PoE 최적화 결과 로드: {} ({})", resultFile, lastResult.gemName());
    } catch (Exception e) {
      logger.warn("PoE 최적화 결과 로드 실패: {}", resultFile, e);
    }
  }

  public boolean isAvailable() {
    return poePobEngineService.isAvailable()
        && poeGemDataService.hasData()
        && poeTreeGraphService.hasData()
        && poeUniqueDataService.totalCount() > 0;
  }

  public boolean isRunning() {
    return running.get();
  }

  public Status lastStatus() {
    return lastStatus;
  }

  public String phase() {
    return phase;
  }

  public int phaseDone() {
    return phaseDone.get();
  }

  public int phaseTotal() {
    return phaseTotal;
  }

  public int evalCount() {
    return evalCount.get();
  }

  public PoeOptimizeResult lastResult() {
    return lastResult;
  }

  public synchronized List<String> logTail() {
    return List.copyOf(logLines);
  }

  private synchronized void log(String line) {
    logLines.addLast(line);
    while (logLines.size() > LOG_LIMIT) {
      logLines.removeFirst();
    }
  }

  /** 최적화 잡 시작 — 이미 실행 중이거나 젬이 없으면 false */
  public boolean start(String gemSlug, String objective) {
    if (!isAvailable()) {
      return false;
    }
    PoeGem gem = poeGemDataService.findBySlug(gemSlug).orElse(null);
    if (gem == null || gem.isSupport()) {
      return false;
    }
    String normalizedObjective = "ehp".equals(objective) ? "ehp" : "dps";
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    synchronized (this) {
      logLines.clear();
    }
    evalCount.set(0);
    phaseDone.set(0);
    phaseTotal = 0;
    Thread thread = new Thread(() -> runJob(gem, normalizedObjective), "poe-optimize");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private void runJob(PoeGem gem, String objective) {
    long startedAt = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    try {
      String objectiveKey = "ehp".equals(objective) ? "TotalEHP" : "CombinedDPS";
      String className = classFor(gem);
      Integer classStart = poeTreeGraphService.classStart(className);
      if (classStart == null) {
        throw new IllegalStateException("트리 시작 노드 없음: " + className);
      }
      List<String> keywords = keywords(gem, objective);
      log(gem.name() + " / 목표 " + objectiveKey + " / 직업 " + className + " / 키워드 " + keywords);

      List<PoeGem> supports = new ArrayList<>();
      Set<Integer> allocated = new LinkedHashSet<>();
      Map<Slot, PoeUniqueItem> items = new EnumMap<>(Slot.class);

      phase = "baseline";
      double baseline =
          objectiveOf(
              poePobEngineService.calculateValues(
                  buildXml(gem, supports, className, allocated, items)),
              objectiveKey);
      evalCount.incrementAndGet();
      double current = baseline;
      log("기준값(젬 단독): " + format(baseline));

      // ── 1) 보조젬 greedy (EHP 목표에서는 의미가 없어 생략) ──
      if ("dps".equals(objective)) {
        phase = "supports";
        List<PoeGem> candidates =
            poeGemDataService.search(null, "support", "all").stream()
                .filter(support -> !support.levels().isEmpty())
                .toList();
        log("보조젬 1라운드: 후보 " + candidates.size() + "개");
        Map<PoeGem, Double> firstRound =
            evalBatch(
                executor,
                candidates,
                support -> buildXml(gem, joined(supports, support), className, allocated, items),
                objectiveKey);

        List<PoeGem> shortlist =
            firstRound.entrySet().stream()
                .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                .limit(SUPPORT_SHORTLIST)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        while (supports.size() < MAX_SUPPORTS && !shortlist.isEmpty()) {
          Map<PoeGem, Double> round =
              supports.isEmpty()
                  ? firstRound
                  : evalBatch(
                      executor,
                      shortlist,
                      support ->
                          buildXml(gem, joined(supports, support), className, allocated, items),
                      objectiveKey);
          Map.Entry<PoeGem, Double> best =
              round.entrySet().stream()
                  .filter(entry -> shortlist.contains(entry.getKey()))
                  .max(Map.Entry.comparingByValue())
                  .orElse(null);
          if (best == null || best.getValue() <= current * 1.005) {
            break;
          }
          supports.add(best.getKey());
          shortlist.remove(best.getKey());
          current = best.getValue();
          log("보조젬 채택: " + koName(best.getKey()) + " → " + format(current));
        }
      }

      // ── 2) 패시브 트리 greedy — 관련 노터블/키스톤에 경로 비용 대비 최대 이득 순 할당 ──
      phase = "tree";
      Map<PoeTreeGraphService.TreeNode, Integer> candidateScores = new LinkedHashMap<>();
      for (PoeTreeGraphService.TreeNode node : poeTreeGraphService.searchCandidates()) {
        int score = score(node.stats(), keywords);
        if (score > 0) {
          candidateScores.put(node, score);
        }
      }
      log("트리 후보 노터블/키스톤: " + candidateScores.size() + "개");

      Set<Integer> allocatedWithStart = new LinkedHashSet<>();
      allocatedWithStart.add(classStart);
      int points = 0;
      for (int round = 0; round < TREE_MAX_ROUNDS && points < POINT_BUDGET; round++) {
        record Reachable(PoeTreeGraphService.TreeNode node, List<Integer> path, double priority) {}
        List<Reachable> reachable = new ArrayList<>();
        for (Map.Entry<PoeTreeGraphService.TreeNode, Integer> entry : candidateScores.entrySet()) {
          List<Integer> path =
              poeTreeGraphService.shortestPath(allocatedWithStart, entry.getKey().id());
          if (path == null || path.isEmpty() || points + path.size() > POINT_BUDGET) {
            continue;
          }
          reachable.add(
              new Reachable(entry.getKey(), path, entry.getValue() / (double) path.size()));
        }
        if (reachable.isEmpty()) {
          break;
        }
        List<Reachable> topCandidates =
            reachable.stream()
                .sorted(Comparator.comparingDouble(Reachable::priority).reversed())
                .limit(TREE_ROUND_CANDIDATES)
                .toList();
        double currentBeforeRound = current;
        Map<Reachable, Double> round0 =
            evalBatch(
                executor,
                topCandidates,
                candidate -> {
                  Set<Integer> trial = new LinkedHashSet<>(allocated);
                  trial.addAll(candidate.path());
                  return buildXml(gem, supports, className, trial, items);
                },
                objectiveKey);
        Reachable best = null;
        double bestGainPerPoint = 0;
        for (Map.Entry<Reachable, Double> entry : round0.entrySet()) {
          double gainPerPoint =
              (entry.getValue() - currentBeforeRound) / entry.getKey().path().size();
          if (gainPerPoint > bestGainPerPoint) {
            bestGainPerPoint = gainPerPoint;
            best = entry.getKey();
          }
        }
        if (best == null) {
          topCandidates.forEach(candidate -> candidateScores.remove(candidate.node()));
          continue;
        }
        allocated.addAll(best.path());
        allocatedWithStart.addAll(best.path());
        points += best.path().size();
        current = round0.get(best);
        candidateScores.remove(best.node());
        log(
            "트리 할당: "
                + (best.node().nameKo() != null ? best.node().nameKo() : best.node().name())
                + " (+"
                + best.path().size()
                + "pt, "
                + points
                + "/"
                + POINT_BUDGET
                + ") → "
                + format(current));
      }

      // ── 3) 고유 아이템 greedy (슬롯 순회) ──
      phase = "items";
      for (Slot slot : Slot.values()) {
        if (slot == Slot.WEAPON && "ehp".equals(objective)) {
          continue; // 무기 고유는 EHP 에 기여하지 않음 — 표준 무기 유지
        }
        List<PoeUniqueItem> slotCandidates = itemCandidates(slot, gem, keywords, items);
        if (slotCandidates.isEmpty()) {
          continue;
        }
        Map<PoeUniqueItem, Double> results =
            evalBatch(
                executor,
                slotCandidates,
                candidate -> {
                  Map<Slot, PoeUniqueItem> trial = new EnumMap<>(items);
                  trial.put(slot, candidate);
                  return buildXml(gem, supports, className, allocated, trial);
                },
                objectiveKey);
        Map.Entry<PoeUniqueItem, Double> best =
            results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (best != null && best.getValue() > current * 1.002) {
          items.put(slot, best.getKey());
          current = best.getValue();
          log(
              "장비 채택: "
                  + slot.ko
                  + " = "
                  + (best.getKey().nameKo() != null ? best.getKey().nameKo() : best.getKey().name())
                  + " → "
                  + format(current));
        }
      }

      // ── 마무리: 최종 계산 + PoB 코드 ──
      phase = "finish";
      phaseDone.set(0);
      phaseTotal = 0;
      String finalXml = buildXml(gem, supports, className, allocated, items);
      Map<String, Double> finalValues;
      try {
        finalValues = poePobEngineService.calculateValues(finalXml);
      } catch (IllegalStateException e) {
        log("최종 계산 재시도: " + e.getMessage());
        finalValues = poePobEngineService.calculateValues(finalXml);
      }
      evalCount.incrementAndGet();

      List<String> notables = new ArrayList<>();
      for (int nodeId : allocated) {
        PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(nodeId);
        if (node != null && ("notable".equals(node.type()) || "keystone".equals(node.type()))) {
          notables.add(node.nameKo() != null ? node.nameKo() : node.name());
        }
      }
      PoeOptimizeResult result =
          new PoeOptimizeResult(
              gem.slug(),
              gem.name(),
              gem.nameKo(),
              objective,
              className,
              CLASS_KO.getOrDefault(className, className),
              supports.stream()
                  .map(
                      support ->
                          new PoeOptimizeResult.SupportPick(
                              support.slug(), support.name(), support.nameKo()))
                  .toList(),
              List.copyOf(allocated),
              notables,
              items.entrySet().stream()
                  .map(
                      entry ->
                          new PoeOptimizeResult.ItemPick(
                              entry.getKey().pobName,
                              entry.getKey().ko,
                              entry.getValue().slug(),
                              entry.getValue().name(),
                              entry.getValue().nameKo()))
                  .toList(),
              poePobEngineService.formatStats(finalValues),
              format(baseline),
              format(objectiveOf(finalValues, objectiveKey)),
              encodePobCode(finalXml),
              System.currentTimeMillis() - startedAt,
              evalCount.get());

      Files.createDirectories(resultFile.getParent());
      JsonMapper jsonMapper = JsonMapper.builder().build();
      Files.writeString(resultFile, jsonMapper.writeValueAsString(result), StandardCharsets.UTF_8);
      this.lastResult = result;
      log(
          "완료: "
              + format(baseline)
              + " → "
              + format(objectiveOf(finalValues, objectiveKey))
              + " ("
              + (System.currentTimeMillis() - startedAt) / 1000
              + "초, 평가 "
              + evalCount.get()
              + "회)");
      lastStatus = Status.SUCCESS;
    } catch (Exception e) {
      lastStatus = Status.FAILED;
      log("실패: " + e);
      logger.warn("PoE 최적화 잡 실패", e);
    } finally {
      running.set(false);
      executor.shutdown();
      try {
        executor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  // ── 평가 헬퍼 ────────────────────────────────────────────

  /** 후보들을 병렬 평가해 목표 스탯 값을 돌려준다. 실패한 후보는 -1 (자연 탈락). */
  private <T> Map<T, Double> evalBatch(
      ExecutorService executor,
      List<T> candidates,
      Function<T, String> xmlFor,
      String objectiveKey) {
    phaseTotal = candidates.size();
    phaseDone.set(0);
    Map<T, Future<Double>> futures = new LinkedHashMap<>();
    for (T candidate : candidates) {
      futures.put(
          candidate,
          executor.submit(
              () -> {
                try {
                  return objectiveOf(
                      poePobEngineService.calculateValues(xmlFor.apply(candidate)), objectiveKey);
                } catch (Exception e) {
                  return -1d;
                } finally {
                  evalCount.incrementAndGet();
                  phaseDone.incrementAndGet();
                }
              }));
    }
    Map<T, Double> results = new LinkedHashMap<>();
    for (Map.Entry<T, Future<Double>> entry : futures.entrySet()) {
      try {
        results.put(entry.getKey(), entry.getValue().get());
      } catch (Exception e) {
        results.put(entry.getKey(), -1d);
      }
    }
    return results;
  }

  private double objectiveOf(Map<String, Double> values, String objectiveKey) {
    return values.getOrDefault(objectiveKey, 0d);
  }

  private List<PoeGem> joined(List<PoeGem> supports, PoeGem extra) {
    List<PoeGem> result = new ArrayList<>(supports);
    result.add(extra);
    return result;
  }

  private String koName(PoeGem gem) {
    return gem.nameKo() != null ? gem.nameKo() : gem.name();
  }

  private String format(double value) {
    return String.format(Locale.ROOT, "%,.0f", value);
  }

  // ── 후보 선정 ────────────────────────────────────────────

  /** 젬 색상(주 능력치) 기준 직업 선택 */
  private String classFor(PoeGem gem) {
    if ("red".equals(gem.color())) {
      return "Marauder";
    }
    if ("green".equals(gem.color())) {
      return "Ranger";
    }
    if ("blue".equals(gem.color())) {
      return "Witch";
    }
    return "Scion";
  }

  /** 트리/아이템 후보 선별용 키워드 — 젬 태그와 목표에서 유도 */
  private List<String> keywords(PoeGem gem, String objective) {
    if ("ehp".equals(objective)) {
      return List.of("maximum life", "energy shield", "armour", "evasion", "resistance", "block");
    }
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    List<String> keywords = new ArrayList<>(List.of("damage"));
    Map<String, List<String>> tagKeywords =
        Map.ofEntries(
            Map.entry("Fire", List.of("fire")),
            Map.entry("Cold", List.of("cold")),
            Map.entry("Lightning", List.of("lightning")),
            Map.entry("Chaos", List.of("chaos")),
            Map.entry("Physical", List.of("physical")),
            Map.entry("Spell", List.of("spell", "cast speed")),
            Map.entry("Attack", List.of("attack", "accuracy")),
            Map.entry("Projectile", List.of("projectile")),
            Map.entry("AoE", List.of("area")),
            Map.entry("Minion", List.of("minion")),
            Map.entry("Critical", List.of("critical")),
            Map.entry("Duration", List.of("damage over time")),
            Map.entry("Totem", List.of("totem")),
            Map.entry("Trap", List.of("trap")),
            Map.entry("Mine", List.of("mine")),
            Map.entry("Brand", List.of("brand")));
    for (String tag : tags) {
      List<String> mapped = tagKeywords.get(tag);
      if (mapped != null) {
        keywords.addAll(mapped);
      }
    }
    return keywords;
  }

  private int score(List<String> lines, List<String> keywords) {
    if (lines == null) {
      return 0;
    }
    int score = 0;
    for (String line : lines) {
      String lower = line.toLowerCase(Locale.ROOT);
      for (String keyword : keywords) {
        if (lower.contains(keyword)) {
          score++;
        }
      }
    }
    return score;
  }

  /** 슬롯별 고유 아이템 후보 — 카테고리 매칭 + 키워드 점수 상위 N */
  private List<PoeUniqueItem> itemCandidates(
      Slot slot, PoeGem gem, List<String> keywords, Map<Slot, PoeUniqueItem> equipped) {
    List<String> categories = slot == Slot.WEAPON ? weaponCategories(gem) : slot.categories;
    PoeUniqueItem other = slot == Slot.RING2 ? equipped.get(Slot.RING1) : null;
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "all").stream()
        .filter(item -> categories.contains(item.category()))
        .filter(item -> item.requiredLevel() == null || item.requiredLevel() <= LEVEL)
        .filter(item -> other == null || !item.slug().equals(other.slug()))
        .map(
            item -> {
              List<String> lines = new ArrayList<>(item.implicits());
              lines.addAll(item.explicits());
              return new Scored(item, score(lines, keywords));
            })
        .filter(scored -> scored.score() > 0)
        .sorted(Comparator.comparingInt(Scored::score).reversed())
        .limit(ITEM_CANDIDATES)
        .map(Scored::item)
        .toList();
  }

  /** 무기 슬롯 후보 카테고리 — 젬 태그 기준 */
  private List<String> weaponCategories(PoeGem gem) {
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    if (tags.contains("Bow")) {
      return List.of("bow");
    }
    if (tags.contains("Attack")) {
      return List.of("axe", "sword", "mace", "claw", "dagger");
    }
    return List.of("wand", "staff", "dagger");
  }

  // ── 빌드 XML / PoB 코드 ──────────────────────────────────

  private String buildXml(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      Set<Integer> treeNodes,
      Map<Slot, PoeUniqueItem> items) {
    StringBuilder xml = new StringBuilder();
    xml.append("<PathOfBuilding>")
        .append("<Build level=\"")
        .append(LEVEL)
        .append("\" targetVersion=\"3_0\" className=\"")
        .append(className)
        .append("\" ascendClassName=\"None\" mainSocketGroup=\"1\"/>")
        .append("<Skills activeSkillSet=\"1\"><SkillSet id=\"1\">")
        .append("<Skill mainActiveSkill=\"1\" enabled=\"true\" slot=\"Body Armour\">")
        .append("<Gem nameSpec=\"")
        .append(gem.name())
        .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/>");
    for (PoeGem support : supports) {
      // PoB 의 보조젬 이름은 "Support" 접미사가 없다 ("Spell Echo Support" 는 미인식 → 계산 무효)
      xml.append("<Gem nameSpec=\"")
          .append(support.name().replaceFirst(" Support$", ""))
          .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/>");
    }
    xml.append("</Skill></SkillSet></Skills>")
        .append("<Tree activeSpec=\"1\"><Spec treeVersion=\"")
        .append(treeVersion)
        .append("\" classId=\"")
        .append(CLASS_IDS.getOrDefault(className, 0))
        .append("\" ascendClassId=\"0\" nodes=\"")
        .append(String.join(",", treeNodes.stream().map(String::valueOf).toList()))
        .append("\"/></Tree>");

    xml.append("<Items activeItemSet=\"1\">");
    StringBuilder slots = new StringBuilder();
    int itemId = 0;
    for (Map.Entry<Slot, PoeUniqueItem> entry : items.entrySet()) {
      itemId++;
      xml.append("<Item id=\"")
          .append(itemId)
          .append("\">\n")
          .append(uniqueItemText(entry.getValue()))
          .append("</Item>");
      slots
          .append("<Slot name=\"")
          .append(entry.getKey().pobName)
          .append("\" itemId=\"")
          .append(itemId)
          .append("\"/>");
    }
    // 무기 고유가 안 뽑혔으면 랭킹 배치와 동일한 표준 무기 지급 (공격 젬 명중 보정 포함)
    if (!items.containsKey(Slot.WEAPON)) {
      String standardWeapon = standardWeapon(gem);
      if (standardWeapon != null) {
        itemId++;
        xml.append("<Item id=\"")
            .append(itemId)
            .append("\">\nRarity: RARE\nSim Weapon\n")
            .append(standardWeapon)
            .append(
                "\nItem Level: 84\nImplicits: 0\nAdds 60 to 120 Physical Damage\n+2000 to Accuracy Rating\n</Item>");
        slots.append("<Slot name=\"Weapon 1\" itemId=\"").append(itemId).append("\"/>");
      }
    }
    if (itemId > 0) {
      xml.append("<ItemSet id=\"1\">").append(slots).append("</ItemSet>");
    }
    xml.append("</Items></PathOfBuilding>");
    return xml.toString();
  }

  private String standardWeapon(PoeGem gem) {
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    if (tags.contains("Bow")) {
      return "Thicket Bow";
    }
    if (tags.contains("Attack")) {
      return "Vaal Axe";
    }
    return null;
  }

  /** 고유 아이템 → PoB 아이템 텍스트 (모드 범위 "(20-30)" 표기는 PoB 파서가 그대로 이해한다) */
  private String uniqueItemText(PoeUniqueItem item) {
    StringBuilder text = new StringBuilder();
    text.append("Rarity: UNIQUE\n")
        .append(item.name())
        .append("\n")
        .append(item.baseType())
        .append("\n");
    text.append("Implicits: ").append(item.implicits().size()).append("\n");
    for (String implicit : item.implicits()) {
      text.append(implicit).append("\n");
    }
    for (String explicit : item.explicits()) {
      text.append(explicit).append("\n");
    }
    return text.toString();
  }

  /** PoB 공유 코드 인코딩 (zlib deflate → base64url) */
  private String encodePobCode(String xml) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    deflater.setInput(xml.getBytes(StandardCharsets.UTF_8));
    deflater.finish();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8 * 1024];
    while (!deflater.finished()) {
      output.write(buffer, 0, deflater.deflate(buffer));
    }
    deflater.end();
    return Base64.getEncoder()
        .encodeToString(output.toByteArray())
        .replace('+', '-')
        .replace('/', '_');
  }
}
