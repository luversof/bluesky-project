package net.luversof.api.poe.service;

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

  /** 전직 포인트 예산 (만렙 성역 8포인트, 시작 노드 제외) */
  private static final int ASCENDANCY_POINT_BUDGET = 8;

  /** 전직 8포인트 중 혈맹(2차 전직)에 배분 예약할 포인트 — 나머지는 직업 전직에 사용 */
  private static final int BLOODLINE_RESERVE = 2;

  /** 주얼 소켓용으로 트리 예산에서 예약하는 포인트 (너무 크면 트리 노터블 손실) */
  private static final int JEWEL_RESERVE = 6;

  /** 주얼 단계에서 평가할 최대 소켓 수 (가장 싸게 닿는 것부터) — 비용 제한 */
  private static final int JEWEL_MAX_SOCKETS = 5;

  /** 방어 오라 최대 개수(마나 예약 한계로 실질 2~3개, PoB 가 초과 예약 시 효과를 깎아 greedy 가 자연 종료). */
  private static final int MAX_AURAS = 4;

  /**
   * 오라 예약 현실성 신호 — PoB 는 ManaReservedPercent 를 100 에서 클램프하므로 초과 판정에 못 쓴다. 실제 초과 신호는
   * ManaUnreserved(미예약 마나)가 음수가 되는 것이다(예약이 최대 마나를 초과). 미예약 마나가 이 값 미만이면 인게임에서 못 띄우는 조합으로 보고 채택하지
   * 않는다(약간의 여유 허용).
   */
  private static final double MIN_UNRESERVED_MANA = -1.0;

  /**
   * 예약형 오라/헤럴드 후보(메인 스킬 외 별도 그룹). 방어 오라(EHP)+오펜스 오라(DPS)를 모두 넣고, greedy 가 현재 목표(dps/balanced/ehp)에
   * 이득 되는 것만 채택 — 젬·빌드별로 데이터 기반 선택.
   */
  private static final Set<String> AURA_NAMES =
      Set.of(
          // 방어
          "Determination",
          "Grace",
          "Discipline",
          "Defiance Banner",
          "Vitality",
          "Purity of Elements",
          "Purity of Fire",
          "Purity of Ice",
          "Purity of Lightning",
          // 공격
          "Anger",
          "Hatred",
          "Wrath",
          "Malevolence",
          "Pride",
          "Zealotry",
          "Herald of Ash",
          "Herald of Ice",
          "Herald of Thunder",
          "Herald of Purity",
          "Flesh and Stone");

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

  /**
   * 장비 슬롯. categories = 고유 아이템 category 매핑, modSlot = 레어 모드 풀 슬롯 카테고리(없으면 레어 미생성), rareBase = 레어
   * 크래프팅 베이스 이름(PoB 가 아는 실제 베이스).
   */
  private enum Slot {
    WEAPON("Weapon 1", "무기", null, null, List.of()),
    OFFHAND("Weapon 2", "보조장비", "shield", "Titanium Spirit Shield", List.of("shield")),
    BODY("Body Armour", "갑옷", "body", "Vaal Regalia", List.of("body")),
    HELMET("Helmet", "투구", "helmet", "Hubris Circlet", List.of("helmet")),
    GLOVES("Gloves", "장갑", "gloves", "Sorcerer Gloves", List.of("gloves")),
    BOOTS("Boots", "장화", "boots", "Sorcerer Boots", List.of("boots")),
    AMULET("Amulet", "목걸이", "amulet", "Onyx Amulet", List.of("amulet")),
    RING1("Ring 1", "반지 1", "ring", "Coral Ring", List.of("ring")),
    RING2("Ring 2", "반지 2", "ring", "Coral Ring", List.of("ring")),
    BELT("Belt", "허리띠", "belt", "Leather Belt", List.of("belt")),
    FLASK1("Flask 1", "플라스크 1", "flask", null, List.of()),
    FLASK2("Flask 2", "플라스크 2", "flask", null, List.of()),
    FLASK3("Flask 3", "플라스크 3", "flask", null, List.of()),
    FLASK4("Flask 4", "플라스크 4", "flask", null, List.of()),
    FLASK5("Flask 5", "플라스크 5", "flask", null, List.of());

    final String pobName;
    final String ko;
    final List<String> categories;
    final String rareBase;
    final List<String> modSlots;

    Slot(String pobName, String ko, String category, String rareBase, List<String> modSlots) {
      this.pobName = pobName;
      this.ko = ko;
      this.categories = category != null ? List.of(category) : List.of();
      this.rareBase = rareBase;
      this.modSlots = modSlots;
    }
  }

  /** 레어 아이템 — 베이스 + 선택된 모드 패밀리(문장은 tierFraction 으로 롤). tierFraction: 0=최상위 티어, 1=최하위 */
  private record RareItem(
      String baseType, List<PoeModPoolDataService.ModFamily> families, double tierFraction) {}

  /** 슬롯에 장착된 것 — 유니크 또는 레어 (배타) */
  private record Equipped(PoeUniqueItem unique, RareItem rare) {
    static Equipped ofUnique(PoeUniqueItem item) {
      return new Equipped(item, null);
    }

    static Equipped ofRare(RareItem item) {
      return new Equipped(null, item);
    }

    boolean isUnique() {
      return unique != null;
    }
  }

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoePobEngineService poePobEngineService;
  private final PoeTreeGraphService poeTreeGraphService;
  private final PoeModPoolDataService poeModPoolDataService;
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
      PoeModPoolDataService poeModPoolDataService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.sim.tree-version:3_28}") String treeVersion,
      @Value("${poe.sim.parallelism:0}") int parallelism) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poePobEngineService = poePobEngineService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.resultFile = Path.of(dataDir, "sim", "optimize-last.json");
    this.treeVersion = treeVersion;
    // 병렬성 미지정(≤0)이면 엔진 워커 풀 크기와 일치시킨다(executor 스레드마다 워커 1개 배정 →
    // 스레드가 워커를 못 잡고 대기하는 낭비 없음). 워커 풀은 코어/RAM 자동 산정. 다른 PC 이식성.
    this.parallelism = parallelism > 0 ? parallelism : poePobEngineService.poolSize();
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

  /** 적 시나리오(PoB enemyIsBoss) — 잡 전체에 고정이라 필드로 관리(단일 실행 보장). 기본 Pinnacle. */
  private volatile String enemyScenario = "Pinnacle";

  /** PoB config 값 → 한국어 라벨 */
  private static final Map<String, String> SCENARIO_KO =
      Map.of("None", "일반 몬스터", "Boss", "표준 보스", "Pinnacle", "핀나클 보스", "Uber", "우버 보스");

  /** 전투 버프 가정(충전+돌격) — 엔드게임 빌드가 흔히 전제하는 상태 */
  private volatile boolean combatBuffs = true;

  /** 직업 고정 — null 이면 7직업 프로브로 자동 선택, 지정되면 그 직업으로만 계산 */
  private volatile String fixedClass;

  /** 전직 고정 — 지정 시 해당 전직만 사용(렐리쿼리언 등 특정 전직 최적화용). null 이면 pickAscendancy 자동 선택 */
  private volatile String fixedAscendancy;

  /** 강제 장착 유니크 — 사용자가 선택한 유니크를 해당 슬롯에 고정하고 나머지 슬롯만 최적화. 비면 자동 탐색. */
  private volatile List<PoeUniqueItem> fixedUniques = new ArrayList<>();

  /**
   * 추가 스킬 — 사용자가 메인 외 함께 쓰려고 선택한 액티브 스킬(오라/커스/헤럴드/가드 등). 각자 소켓그룹으로 emit 되어 PoB 가 역할대로 자동
   * 반영(오라=예약+버프, 커스=적 약화 등). 메인 DPS 는 이들의 버프를 받은 값.
   */
  private volatile List<PoeGem> additionalSkills = new ArrayList<>();

  /** 선택된 혈맹의 PoB secondaryAscendClassId — 최종 빌드 XML 에만 반영(탐색 중엔 0, 노드 id 로 계산됨) */
  private volatile int secondaryAscendId;

  /** 방어 오라 스테이지에서 채택된 오라 — 최종/일반 buildXml 이 2번째 스킬 그룹으로 emit(트라이얼은 명시 전달). */
  private volatile List<PoeGem> selectedAuras = new ArrayList<>();

  /** 최적화 잡 시작 — 이미 실행 중이거나 젬이 없으면 false */
  public boolean start(
      String gemSlug, String objective, String scenario, boolean buffs, String className) {
    return start(gemSlug, objective, scenario, buffs, className, null);
  }

  /** 최적화 잡 시작 — ascendancy 지정 시 그 전직으로 고정(자동 선택 대신) */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy) {
    return start(gemSlug, objective, scenario, buffs, className, ascendancy, null);
  }

  /** 최적화 잡 시작 — uniques(콤마구분 slug)를 지정하면 그 유니크들을 해당 슬롯에 강제 장착하고 나머지만 최적화 */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques) {
    return start(gemSlug, objective, scenario, buffs, className, ascendancy, uniques, null);
  }

  /** 최적화 잡 시작 — skills(콤마구분 slug)를 지정하면 메인 외 그 액티브 스킬들을 빌드에 함께 배치(오라/커스/버프 반영) */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills) {
    if (!isAvailable()) {
      return false;
    }
    PoeGem gem = poeGemDataService.findBySlug(gemSlug).orElse(null);
    List<PoeUniqueItem> resolvedUniques = resolveFixedUniques(uniques);
    List<PoeGem> resolvedSkills = resolveAdditionalSkills(skills, gemSlug);
    // slug 미지정이면 선택된 스킬 중 최고를 메인으로(없으면 유니크 기준 전체에서). 스킬·유니크 다 없으면 실패.
    boolean autoPickSkill =
        gem == null && (!resolvedSkills.isEmpty() || !resolvedUniques.isEmpty());
    if ((gem == null && !autoPickSkill) || (gem != null && gem.isSupport())) {
      return false;
    }
    String normalizedObjective =
        "ehp".equals(objective) || "balanced".equals(objective) ? objective : "dps";
    this.enemyScenario = SCENARIO_KO.containsKey(scenario) ? scenario : "Pinnacle"; // 화이트리스트
    this.combatBuffs = buffs;
    this.secondaryAscendId = 0; // 혈맹 선택 초기화(잡마다)
    this.selectedAuras = new ArrayList<>(); // 방어 오라 초기화(잡마다)
    // 직업 고정 — 유효한 직업명만 채택, 그 외(빈값/auto/미지)는 null(자동 프로브)
    this.fixedClass = className != null && CLASS_IDS.containsKey(className) ? className : null;
    // 전직만 선택해도 직업을 도출 — 직업 미지정 + 유효 전직이면 그 전직의 소속 직업으로 고정
    if (this.fixedClass == null && ascendancy != null && !ascendancy.isBlank()) {
      String derived = poeTreeGraphService.classForAscendancy(ascendancy);
      if (derived != null && CLASS_IDS.containsKey(derived)) {
        this.fixedClass = derived;
      }
    }
    // 전직 고정 — 지정 전직이 (고정)직업의 전직 목록에 있을 때만 채택, 그 외엔 null(자동)
    this.fixedAscendancy =
        ascendancy != null
                && !ascendancy.isBlank()
                && this.fixedClass != null
                && poeTreeGraphService.ascendancies(this.fixedClass).contains(ascendancy)
            ? ascendancy
            : null;
    // 강제 장착 유니크 (위에서 해석한 것 채택, 잡마다 초기화)
    this.fixedUniques = resolvedUniques;
    // 추가 스킬 (위에서 해석한 것 채택). slug 미지정 시 이 중 최고가 메인이 되고 나머지가 추가로 남음(runJob).
    this.additionalSkills = new ArrayList<>(resolvedSkills);
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    synchronized (this) {
      logLines.clear();
    }
    evalCount.set(0);
    phaseDone.set(0);
    phaseTotal = 0;
    // gem 이 null 이면(고유템 anchor) runJob 시작 시 스킬 프로브로 결정
    Thread thread = new Thread(() -> runJob(gem, normalizedObjective), "poe-optimize");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private void runJob(PoeGem gemArg, String objective) {
    long startedAt = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    try {
      String objectiveKey = objective; // objectiveOf 가 objective 문자열을 해석 (dps/ehp/balanced)
      // 메인 스킬 결정: slug 지정이면 그것, 아니면 (선택된 스킬 중 최고) 또는 (없으면 전체 데미지스킬 중 최고).
      PoeGem resolved = gemArg;
      if (resolved == null) {
        List<PoeGem> pool = additionalSkills.stream().filter(this::isDamageSkill).toList();
        if (pool.isEmpty() && !additionalSkills.isEmpty()) {
          pool = additionalSkills; // 선택된 게 전부 비데미지면 그중에서
        }
        if (pool.isEmpty()) {
          pool = allDamageSkills(); // 스킬 미선택(유니크 anchor) → 전체 데미지 스킬
        }
        resolved = pickBestSkill(executor, objectiveKey, pool);
        if (resolved == null) {
          log("스킬 자동선택 실패 — 후보 없음");
          lastStatus = Status.FAILED;
          return;
        }
        // 메인으로 뽑힌 스킬은 추가 스킬 목록에서 제외(중복 emit 방지)
        final PoeGem picked = resolved;
        this.additionalSkills =
            additionalSkills.stream()
                .filter(g -> !g.slug().equals(picked.slug()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
      }
      final PoeGem gem = resolved; // 이후 람다에서 참조되므로 final 고정
      List<String> keywords = keywords(gem, objective);
      log(gem.name() + " / 목표 " + objectiveKey + " / 키워드 " + keywords);

      // ── 0) 직업 비교 프로브 — 직업별 (최적 전직 + 휴리스틱 8pt) 를 엔진 1회씩 평가해 최고 직업 선택 ──
      phase = "class";
      record ClassProbe(String probeClass, String probeAscendancy, Set<Integer> probeNodes) {}
      List<ClassProbe> probes = new ArrayList<>();
      // 직업 고정 시 그 직업만, 아니면 전 직업 프로브
      Set<String> classPool = fixedClass != null ? Set.of(fixedClass) : CLASS_IDS.keySet();
      for (String candidateClass : classPool) {
        if (poeTreeGraphService.classStart(candidateClass) == null) {
          continue;
        }
        String candidateAscendancy = chooseAscendancy(candidateClass, keywords);
        probes.add(
            new ClassProbe(
                candidateClass,
                candidateAscendancy,
                heuristicAscendancyNodes(candidateAscendancy, keywords)));
      }
      Map<ClassProbe, Double> probeResults =
          evalBatch(
              executor,
              probes,
              probe ->
                  buildXml(
                      gem,
                      List.of(),
                      probe.probeClass(),
                      probe.probeAscendancy(),
                      probe.probeNodes(),
                      Set.of(),
                      Map.of()),
              objectiveKey);
      ClassProbe bestProbe =
          probeResults.entrySet().stream()
              .filter(entry -> entry.getValue() >= 0)
              .max(Map.Entry.comparingByValue())
              .map(Map.Entry::getKey)
              .orElse(null);
      String className = bestProbe != null ? bestProbe.probeClass() : classFor(gem);
      String ascendancy =
          bestProbe != null ? bestProbe.probeAscendancy() : chooseAscendancy(className, keywords);
      for (Map.Entry<ClassProbe, Double> entry : probeResults.entrySet()) {
        log(
            "직업 프로브: "
                + entry.getKey().probeClass()
                + " · "
                + entry.getKey().probeAscendancy()
                + " → "
                + format(entry.getValue()));
      }
      log("직업 선택: " + className + " · " + ascendancy);

      Integer classStart = poeTreeGraphService.classStart(className);
      if (classStart == null) {
        throw new IllegalStateException("트리 시작 노드 없음: " + className);
      }

      List<PoeGem> supports = new ArrayList<>();
      Set<Integer> allocated = new LinkedHashSet<>();
      Set<Integer> ascendancyNodes = new LinkedHashSet<>();
      Map<Slot, Equipped> items = new EnumMap<>(Slot.class);
      // 강제 장착 유니크를 미리 배치 → 모든 스테이지(트리/보조/오라/아이템)가 이 아이템 스탯을 반영
      placeFixedUniques(items);
      Map<Integer, PoeUniqueItem> jewels = new LinkedHashMap<>(); // 소켓 노드 id → 유니크 주얼

      phase = "baseline";
      Map<String, Double> baselineValues =
          poePobEngineService.calculateValues(
              buildXml(gem, supports, className, ascendancy, ascendancyNodes, allocated, items));
      double baseline = objectiveOf(baselineValues, objectiveKey);
      evalCount.incrementAndGet();
      double current = baseline;
      log("기준값(젬 단독): " + format(baseline) + " / 전직 " + ascendancy);

      // ── 1) 전직 노드 greedy (예산 8포인트) ──
      Integer ascendancyStart =
          ascendancy != null ? poeTreeGraphService.ascendancyStart(ascendancy) : null;
      // 전직 8포인트 중 일부(BLOODLINE_RESERVE)를 혈맹에 배분하기 위해 직업 전직 예산을 줄인다
      int classAscBudget = ASCENDANCY_POINT_BUDGET - BLOODLINE_RESERVE;
      if (ascendancyStart != null) {
        phase = "ascendancy";
        current =
            greedyAscendancy(
                executor,
                gem,
                supports,
                className,
                ascendancy,
                ascendancyNodes,
                allocated,
                items,
                objectiveKey,
                classAscBudget,
                current);
      }

      // ── 1b) 혈맹(2차 전직) 선택 — 예약 포인트로 최적 혈맹을 뽑아 전직 노드에 병합 ──
      // 각 혈맹을 휴리스틱 노드셋으로 1회씩 실측 평가(비용 제한) → 개선폭 최대 혈맹 채택.
      String chosenBloodline = null;
      List<String> bloodlineOptions = poeTreeGraphService.bloodlines();
      if (BLOODLINE_RESERVE > 0 && !bloodlineOptions.isEmpty()) {
        phase = "bloodline";
        record BloodlineProbe(String id, Set<Integer> nodes) {}
        List<BloodlineProbe> blProbes = new ArrayList<>();
        for (String bl : bloodlineOptions) {
          Set<Integer> nodes = heuristicAscendancyNodes(bl, keywords, BLOODLINE_RESERVE);
          if (nodes.size() > 1) { // 시작 노드 외 실제 노드가 있어야 의미
            blProbes.add(new BloodlineProbe(bl, nodes));
          }
        }
        if (!blProbes.isEmpty()) {
          Map<BloodlineProbe, Double> results =
              evalBatch(
                  executor,
                  blProbes,
                  probe -> {
                    Set<Integer> trial = new LinkedHashSet<>(ascendancyNodes);
                    trial.addAll(probe.nodes());
                    return buildXml(gem, supports, className, ascendancy, trial, allocated, items);
                  },
                  objectiveKey);
          Map.Entry<BloodlineProbe, Double> best =
              results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (best != null && best.getValue() > current * 1.002) {
            ascendancyNodes.addAll(best.getKey().nodes());
            secondaryAscendId = poeTreeGraphService.secondaryAscendClassId(best.getKey().id());
            chosenBloodline = best.getKey().id();
            current = best.getValue();
            log(
                "혈맹 선택: "
                    + chosenBloodline
                    + " (+"
                    + (best.getKey().nodes().size() - 1)
                    + "노드) → "
                    + format(current));
          }
        }
      }

      // 혈맹 미채택 시 예약해 둔 포인트를 직업 전직에 회수 배분(6→8) — 노-혈맹 빌드가 약해지지 않도록
      if (chosenBloodline == null
          && ascendancyStart != null
          && classAscBudget < ASCENDANCY_POINT_BUDGET) {
        phase = "ascendancy";
        current =
            greedyAscendancy(
                executor,
                gem,
                supports,
                className,
                ascendancy,
                ascendancyNodes,
                allocated,
                items,
                objectiveKey,
                ASCENDANCY_POINT_BUDGET,
                current);
      }

      // ── 2) 보조젬 greedy (순수 EHP 목표에서만 생략 — dps/balanced 는 실행) ──
      if (!"ehp".equals(objective)) {
        phase = "supports";
        List<PoeGem> candidates =
            poeGemDataService.search(null, "support", "all", null).stream()
                .filter(support -> !support.levels().isEmpty())
                .filter(this::isProvidedSupport)
                .filter(support -> supportCompatible(gem, support))
                .toList();
        long awakenedRemaining =
            candidates.stream()
                .filter(c -> c.name() != null && c.name().startsWith("Awakened"))
                .count();
        logger.info("보조젬 후보(각성 필터 후): {}개, 남은 각성={}", candidates.size(), awakenedRemaining);
        log("보조젬 1라운드: 후보 " + candidates.size() + "개");
        Map<PoeGem, Double> firstRound =
            evalBatch(
                executor,
                candidates,
                support ->
                    buildXml(
                        gem,
                        joined(supports, support),
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items),
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
                          buildXml(
                              gem,
                              joined(supports, support),
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              items),
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
      int treeBudget = POINT_BUDGET - JEWEL_RESERVE; // 주얼 소켓 경로용으로 일부 예약
      for (int round = 0; round < TREE_MAX_ROUNDS && points < treeBudget; round++) {
        record Reachable(PoeTreeGraphService.TreeNode node, List<Integer> path, double priority) {}
        List<Reachable> reachable = new ArrayList<>();
        for (Map.Entry<PoeTreeGraphService.TreeNode, Integer> entry : candidateScores.entrySet()) {
          List<Integer> path =
              poeTreeGraphService.shortestPath(allocatedWithStart, entry.getKey().id());
          if (path == null || path.isEmpty() || points + path.size() > treeBudget) {
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
                  return buildXml(
                      gem, supports, className, ascendancy, ascendancyNodes, trial, items);
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

      // ── 3) 주얼 소켓 greedy — 트리에 연결 가능한 소켓에 전역 유니크 주얼을 꽂는다 ──
      phase = "jewels";
      List<PoeUniqueItem> jewelCandidates = globalJewelCandidates(keywords);
      if (!jewelCandidates.isEmpty()) {
        Set<Integer> jewelReach = new LinkedHashSet<>(allocated);
        jewelReach.add(classStart);
        // 현재 트리에서 가장 싸게 닿는 소켓 순으로 정렬
        record SocketPath(int socketId, List<Integer> path) {}
        List<SocketPath> reachableSockets = new ArrayList<>();
        for (int socketId : poeTreeGraphService.jewelSockets()) {
          if (allocated.contains(socketId)) {
            reachableSockets.add(new SocketPath(socketId, List.of()));
            continue;
          }
          List<Integer> path = poeTreeGraphService.shortestPath(jewelReach, socketId);
          if (path != null && !path.isEmpty() && points + path.size() <= POINT_BUDGET) {
            reachableSockets.add(new SocketPath(socketId, path));
          }
        }
        reachableSockets.sort(Comparator.comparingInt(sp -> sp.path().size()));
        // 가장 싸게 닿는 소켓 몇 개만 평가 (비용 제한)
        if (reachableSockets.size() > JEWEL_MAX_SOCKETS) {
          reachableSockets = new ArrayList<>(reachableSockets.subList(0, JEWEL_MAX_SOCKETS));
        }
        int jewelBudget = 3; // 최대 3개 주얼
        for (SocketPath socketPath : reachableSockets) {
          if (jewels.size() >= jewelBudget || points + socketPath.path().size() > POINT_BUDGET) {
            break;
          }
          // 이 소켓에 후보 주얼들을 꽂아 평가 (경로도 함께 할당)
          Map<PoeUniqueItem, Double> results =
              evalBatch(
                  executor,
                  jewelCandidates,
                  jewel -> {
                    Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
                    trialNodes.addAll(socketPath.path());
                    Map<Integer, PoeUniqueItem> trialJewels = new LinkedHashMap<>(jewels);
                    trialJewels.put(socketPath.socketId(), jewel);
                    return buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        trialNodes,
                        items,
                        trialJewels);
                  },
                  objectiveKey);
          Map.Entry<PoeUniqueItem, Double> best =
              results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (best != null && best.getValue() > current * 1.003) {
            allocated.addAll(socketPath.path());
            points += socketPath.path().size();
            jewels.put(socketPath.socketId(), best.getKey());
            current = best.getValue();
            log(
                "주얼 소켓: "
                    + (best.getKey().nameKo() != null
                        ? best.getKey().nameKo()
                        : best.getKey().name())
                    + " (+"
                    + socketPath.path().size()
                    + "pt) → "
                    + format(current));
          }
        }
      }

      // ── 4) 아이템 greedy (슬롯 순회) — 고유 후보 + 생성 레어(최상위 티어) 를 함께 평가 ──
      phase = "items";
      for (Slot slot : Slot.values()) {
        if (items.containsKey(slot)) {
          continue; // 강제 장착 유니크가 이미 점유한 슬롯 — 탐색 생략(고정)
        }
        if (slot == Slot.WEAPON && "ehp".equals(objective)) {
          continue; // 무기 고유는 EHP 에 기여하지 않음 — 표준 무기 유지
        }
        List<Equipped> slotCandidates = new ArrayList<>();
        for (PoeUniqueItem unique : itemCandidates(slot, gem, keywords, items)) {
          slotCandidates.add(Equipped.ofUnique(unique));
        }
        RareItem rare = craftRare(slot, gem, keywords, 0.0);
        if (rare != null) {
          slotCandidates.add(Equipped.ofRare(rare));
        }
        // 실전형 방어 레어(생명+저항+데미지)도 후보로 — 밸런스/생존 목표에서 유니크와 공정 경쟁
        RareItem defensiveRare = craftDefensiveRare(slot, gem, keywords);
        if (defensiveRare != null
            && (rare == null || !defensiveRare.families().equals(rare.families()))) {
          slotCandidates.add(Equipped.ofRare(defensiveRare));
        }
        if (slotCandidates.isEmpty()) {
          continue;
        }
        Map<Equipped, Double> results =
            evalBatch(
                executor,
                slotCandidates,
                candidate -> {
                  Map<Slot, Equipped> trial = new EnumMap<>(items);
                  trial.put(slot, candidate);
                  return buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      trial,
                      jewels);
                },
                objectiveKey);
        Map.Entry<Equipped, Double> best =
            results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (best != null && best.getValue() > current * 1.002) {
          items.put(slot, best.getKey());
          current = best.getValue();
          log("장비 채택: " + slot.ko + " = " + equippedLabel(best.getKey()) + " → " + format(current));
        }
      }

      // ── 4b) 오라/헤럴드 greedy — 예약형 오라를 2번째 스킬 그룹으로 추가(방어+공격 모두 후보) ──
      // greedy 가 현재 목표에 이득 되는 오라만 채택: dps/balanced=데미지 오라, ehp=방어 오라.
      // ⚠️ 반드시 최종 빌드와 동일 컨텍스트(주얼 포함)로 평가해야 한다 — "화염의 주문" 같은 주얼은
      //    오라/헤럴드 개수에 비례해 데미지를 주므로, 주얼 없이 평가하면 오라가 손해로 보여 미채택됨.
      {
        phase = "auras";
        List<PoeGem> auraPool =
            poeGemDataService.search(null, "active", "all", null).stream()
                .filter(a -> AURA_NAMES.contains(a.name()))
                .filter(a -> !a.levels().isEmpty())
                // 사용자가 추가 스킬로 이미 넣은 오라는 자동 선택에서 제외(중복 emit 방지)
                .filter(a -> additionalSkills.stream().noneMatch(x -> x.slug().equals(a.slug())))
                .toList();
        // 오라 비교 기준 = 주얼 포함 · 오라 없는 빌드(current 는 주얼 제외 아이템 값이라 부적합)
        double auraCurrent =
            objectiveOf(
                poePobEngineService.calculateValues(
                    buildXmlAuras(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items,
                        jewels,
                        List.of())),
                objectiveKey);
        log("오라 후보: " + auraPool.size() + "개 (기준 " + format(auraCurrent) + ")");
        // 예약 상한(MAX_RESERVE_PCT)을 넘겨 인게임에서 못 띄우는 오라는 차단 목록에 넣어 재선택에서 제외
        Set<PoeGem> reserveBlocked = new java.util.HashSet<>();
        boolean improved = true;
        while (improved && selectedAuras.size() < MAX_AURAS) {
          improved = false;
          List<PoeGem> chosen = selectedAuras;
          List<PoeGem> remaining =
              auraPool.stream()
                  .filter(a -> !chosen.contains(a) && !reserveBlocked.contains(a))
                  .toList();
          if (remaining.isEmpty()) break;
          Map<PoeGem, Double> round =
              evalBatch(
                  executor,
                  remaining,
                  aura ->
                      buildXmlAuras(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          items,
                          jewels,
                          joined(chosen, aura)),
                  objectiveKey);
          // 이득 큰 순으로 후보를 보되, 예약 상한을 넘는 것은 건너뛰고 그 다음으로 이득 큰 실현 가능 오라를 채택
          List<Map.Entry<PoeGem, Double>> ranked =
              round.entrySet().stream()
                  .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                  .toList();
          for (Map.Entry<PoeGem, Double> cand : ranked) {
            if (cand.getValue() <= auraCurrent * 1.003) {
              break; // 이득 없음 — 남은 후보는 더 낮으므로 종료
            }
            Map<String, Double> trialValues =
                poePobEngineService.calculateValues(
                    buildXmlAuras(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items,
                        jewels,
                        joined(chosen, cand.getKey())));
            double unreserved = trialValues.getOrDefault("ManaUnreserved", 0d);
            if (unreserved < MIN_UNRESERVED_MANA) {
              reserveBlocked.add(cand.getKey());
              log(
                  "오라 예약 초과 제외: "
                      + cand.getKey().name()
                      + " (미예약 마나 "
                      + Math.round(unreserved)
                      + ")");
              continue;
            }
            selectedAuras.add(cand.getKey());
            auraCurrent = cand.getValue();
            improved = true;
            log(
                "오라 채택: "
                    + cand.getKey().name()
                    + " → "
                    + format(auraCurrent)
                    + " (미예약 마나 "
                    + Math.round(unreserved)
                    + ")");
            break;
          }
        }
      }

      // ── 4) 레어 슬롯 티어 비교 — 채택된 레어를 T1/중/하 티어로 재계산 ──
      phase = "tiers";
      List<PoeOptimizeResult.SlotTierCompare> tierComparisons = new ArrayList<>();
      for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
        if (entry.getValue().isUnique()) {
          continue;
        }
        Slot slot = entry.getKey();
        record TierProbe(String label, double fraction) {}
        List<TierProbe> tierProbes =
            List.of(new TierProbe("T1", 0.0), new TierProbe("중간", 0.5), new TierProbe("하위", 1.0));
        Map<TierProbe, Double> tierResults =
            evalBatch(
                executor,
                tierProbes,
                probe -> {
                  Map<Slot, Equipped> trial = new EnumMap<>(items);
                  trial.put(
                      slot, Equipped.ofRare(craftRare(slot, gem, keywords, probe.fraction())));
                  return buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      trial,
                      jewels);
                },
                objectiveKey);
        List<PoeOptimizeResult.TierRow> rows = new ArrayList<>();
        for (TierProbe probe : tierProbes) {
          Double value = tierResults.get(probe);
          rows.add(new PoeOptimizeResult.TierRow(probe.label(), format(value != null ? value : 0)));
        }
        tierComparisons.add(new PoeOptimizeResult.SlotTierCompare(slot.pobName, slot.ko, rows));
      }

      // ── 마무리: 최종 계산 + PoB 코드 ──
      phase = "finish";
      phaseDone.set(0);
      phaseTotal = 0;
      String finalXml =
          buildXml(gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels);
      Map<String, Double> finalValues;
      try {
        finalValues = poePobEngineService.calculateValues(finalXml);
      } catch (IllegalStateException e) {
        log("최종 계산 재시도: " + e.getMessage());
        finalValues = poePobEngineService.calculateValues(finalXml);
      }
      evalCount.incrementAndGet();

      // ── 가정별 성능 매트릭스: 최종 빌드를 적 시나리오 4종 × 버프 off/on 으로 재평가 ──
      // 완성된 XML 의 <Config> 만 바꿔 병렬 재계산(luajit 프로세스는 서로 독립). EHP 는 적/버프 무관이라 DPS 만.
      List<PoeOptimizeResult.ScenarioCell> scenarioMatrix =
          computeScenarioMatrix(finalXml, executor);

      // ── 방어: 유형별 최대 피격 생존(단일 히트) — 빌드가 각 데미지 유형을 얼마까지 버티는지 ──
      List<PoeOptimizeResult.DefenseHit> defenseHits = defenseHits(finalValues);

      // 트리 링크/노터블 목록에는 전직 노드도 포함한다
      Set<Integer> allNodes = new LinkedHashSet<>(ascendancyNodes);
      allNodes.addAll(allocated);
      List<String> notables = new ArrayList<>();
      for (int nodeId : allNodes) {
        PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(nodeId);
        if (node != null && ("notable".equals(node.type()) || "keystone".equals(node.type()))) {
          notables.add(node.nameKo() != null ? node.nameKo() : node.name());
        }
      }
      String ascendancyKo = ascendancy;
      if (ascendancyStart != null) {
        PoeTreeGraphService.TreeNode startNode = poeTreeGraphService.node(ascendancyStart);
        if (startNode != null && startNode.nameKo() != null) {
          ascendancyKo = startNode.nameKo();
        }
      }
      // 혈맹 표시명 — 시작 노드 이름("Aul Bloodline"/한국어) 사용
      String bloodlineName = null;
      String bloodlineNameKo = null;
      if (chosenBloodline != null) {
        Integer blStart = poeTreeGraphService.ascendancyStart(chosenBloodline);
        PoeTreeGraphService.TreeNode blNode =
            blStart != null ? poeTreeGraphService.node(blStart) : null;
        bloodlineName = blNode != null ? blNode.name() : chosenBloodline;
        bloodlineNameKo = blNode != null ? blNode.nameKo() : null;
      }
      PoeOptimizeResult result =
          new PoeOptimizeResult(
              gem.slug(),
              gem.name(),
              gem.nameKo(),
              objective,
              enemyScenario,
              SCENARIO_KO.getOrDefault(enemyScenario, enemyScenario),
              combatBuffs,
              className,
              CLASS_KO.getOrDefault(className, className),
              ascendancy,
              ascendancyKo,
              bloodlineName,
              bloodlineNameKo,
              supports.stream()
                  .map(
                      support ->
                          new PoeOptimizeResult.SupportPick(
                              support.slug(), support.name(), support.nameKo()))
                  .toList(),
              selectedAuras.stream()
                  .map(
                      aura ->
                          new PoeOptimizeResult.SupportPick(
                              aura.slug(), aura.name(), aura.nameKo()))
                  .toList(),
              additionalSkills.stream()
                  .map(
                      extra ->
                          new PoeOptimizeResult.SupportPick(
                              extra.slug(), extra.name(), extra.nameKo()))
                  .toList(),
              List.copyOf(allNodes),
              notables,
              jewels.values().stream()
                  .map(
                      jewel ->
                          new PoeOptimizeResult.SupportPick(
                              jewel.slug(), jewel.name(), jewel.nameKo()))
                  .toList(),
              items.entrySet().stream()
                  .map(entry -> itemPick(entry.getKey(), entry.getValue()))
                  .toList(),
              tierComparisons,
              scenarioMatrix,
              defenseHits,
              poePobEngineService.formatStats(finalValues),
              format(displayMetric(baselineValues, objective)),
              format(displayMetric(finalValues, objective)),
              encodePobCode(finalXml),
              System.currentTimeMillis() - startedAt,
              evalCount.get());

      Files.createDirectories(resultFile.getParent());
      JsonMapper jsonMapper = JsonMapper.builder().build();
      Files.writeString(resultFile, jsonMapper.writeValueAsString(result), StandardCharsets.UTF_8);
      this.lastResult = result;
      log(
          "완료: "
              + format(displayMetric(baselineValues, objective))
              + " → "
              + format(displayMetric(finalValues, objective))
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

  /** 밸런스 목표의 생존 하한 (이 유효 체력 미만이면 DPS 점수를 비례 감쇠) */
  private static final double EHP_FLOOR = 40000d;

  /**
   * greedy 점수 — objective 별:
   *
   * <ul>
   *   <li>dps: CombinedDPS
   *   <li>ehp: TotalEHP
   *   <li>balanced: CombinedDPS × min(1, EHP/하한) — 유리대포 방지(생존 확보 전엔 DPS 가치 낮춤)
   * </ul>
   */
  private double objectiveOf(Map<String, Double> values, String objective) {
    if ("ehp".equals(objective)) {
      return values.getOrDefault("TotalEHP", 0d);
    }
    double dps = values.getOrDefault("CombinedDPS", 0d);
    if ("balanced".equals(objective)) {
      double ehp = values.getOrDefault("TotalEHP", 0d);
      return dps * Math.min(1.0, ehp / EHP_FLOOR);
    }
    return dps;
  }

  /** 표시용 대표 수치 — ehp 는 유효 체력, 그 외(dps/balanced)는 DPS (혼합점수 대신 실제 값) */
  private double displayMetric(Map<String, Double> values, String objective) {
    return "ehp".equals(objective)
        ? values.getOrDefault("TotalEHP", 0d)
        : values.getOrDefault("CombinedDPS", 0d);
  }

  /**
   * 이번 시즌 실제 제공되는 보조젬인지. 각성한(Awakened) 보조젬은 더 이상 제공되지 않고, 특출난(Exceptional: 향상/강화/계몽) 계열만 각성판이 남는다.
   * 즉 이름이 "Awakened" 로 시작하면서 Exceptional 태그가 없는 젬(각성 화염 추가 등)은 후보에서 제외한다.
   */
  private boolean isProvidedSupport(PoeGem support) {
    String name = support.name();
    if (name == null || !name.startsWith("Awakened")) {
      return true;
    }
    List<String> tags = support.tags();
    return tags != null && tags.contains("Exceptional");
  }

  /** 하드 아키타입 태그 — 보조젬이 가졌는데 메인 스킬이 없으면 PoB 가 효과를 적용하지 않는다(평가 낭비). */
  private static final List<String> ARCHETYPE_TAGS =
      List.of("Minion", "Trap", "Mine", "Totem", "Brand", "Warcry", "Bow");

  /**
   * 보조젬이 메인 스킬에 적용될 여지가 있는지(성능용 사전 필터). PoB 가 어차피 0 이득 처리하는 조합만 제외하므로 품질 손실이 없다: (1) 아키타입
   * 태그(미니언/덫/기뢰/토템/브랜드/함성/활)를 스킬이 없으면 제외, (2) 주문 전용 보조젬 ↔ 순수 공격 스킬(상호 배타) 제외. 태그가 없으면 보수적으로 통과.
   */
  private boolean supportCompatible(PoeGem skill, PoeGem support) {
    List<String> st = support.tags() == null ? List.of() : support.tags();
    List<String> gt = skill.tags() == null ? List.of() : skill.tags();
    if (st.isEmpty() || gt.isEmpty()) {
      return true; // 태그 정보 없으면 안전하게 통과
    }
    for (String arch : ARCHETYPE_TAGS) {
      if (st.contains(arch) && !gt.contains(arch)) {
        return false;
      }
    }
    boolean supSpell = st.contains("Spell");
    boolean supAttack = st.contains("Attack");
    boolean skillSpell = gt.contains("Spell");
    boolean skillAttack = gt.contains("Attack");
    if (supSpell && !supAttack && skillAttack && !skillSpell) {
      return false; // 주문 전용 보조젬 on 순수 공격 스킬
    }
    if (supAttack && !supSpell && skillSpell && !skillAttack) {
      return false; // 공격 전용 보조젬 on 순수 주문 스킬
    }
    return true;
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

  /**
   * 고유템 anchor 모드 — 강제 장착 유니크를 낀 최소 빌드로 데미지 스킬 후보를 엔진 프로브해 최고 objective 스킬 선택. 클래스는 스킬별 색상
   * 휴리스틱(classFor) 또는 고정 직업. 선택 스킬로 이후 풀 최적화(직업/전직은 다시 정밀 프로브).
   */
  private List<PoeGem> allDamageSkills() {
    return poeGemDataService.search(null, "active", "all", null).stream()
        .filter(g -> !g.isSupport())
        .filter(g -> !g.levels().isEmpty())
        .filter(this::isDamageSkill)
        .toList();
  }

  private PoeGem pickBestSkill(
      ExecutorService executor, String objectiveKey, List<PoeGem> candidates) {
    phase = "skill";
    if (candidates.isEmpty()) {
      return null;
    }
    if (candidates.size() == 1) {
      return candidates.get(0); // 후보 하나면 프로브 불필요
    }
    Map<Slot, Equipped> baseItems = new EnumMap<>(Slot.class);
    placeFixedUniques(baseItems);
    log("스킬 자동선택: 후보 " + candidates.size() + "개 (강제유니크 " + fixedUniques.size() + "개)");
    Map<PoeGem, Double> results =
        evalBatch(
            executor,
            candidates,
            g -> {
              String cls = fixedClass != null ? fixedClass : classFor(g);
              List<String> kw = keywords(g, objectiveKey);
              String asc = chooseAscendancy(cls, kw);
              Set<Integer> ascNodes = heuristicAscendancyNodes(asc, kw);
              return buildXml(g, List.of(), cls, asc, ascNodes, Set.of(), baseItems);
            },
            objectiveKey);
    PoeGem best =
        results.entrySet().stream()
            .filter(e -> e.getValue() >= 0)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    if (best != null) {
      log("스킬 자동선택 결과: " + koName(best) + " (" + format(results.get(best)) + ")");
    }
    return best;
  }

  /** 메인 DPS 후보가 될 데미지 스킬인지 (공격/주문이면서 순수 오라/헤럴드/커스가 아님). */
  private boolean isDamageSkill(PoeGem gem) {
    List<String> tags = gem.tags() == null ? List.of() : gem.tags();
    boolean damage = tags.contains("Attack") || tags.contains("Spell");
    boolean utility = tags.contains("Aura") || tags.contains("Herald") || tags.contains("Curse");
    return damage && !utility;
  }

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

  /** 전직 결정 — 고정 전직(fixedAscendancy)이 이 직업의 전직 목록에 있으면 그것, 아니면 자동 선택 */
  private String chooseAscendancy(String className, List<String> keywords) {
    if (fixedAscendancy != null
        && poeTreeGraphService.ascendancies(className).contains(fixedAscendancy)) {
      return fixedAscendancy;
    }
    return pickAscendancy(className, keywords);
  }

  /** 전직 선택 — 각 전직 노터블의 키워드 점수 합이 가장 높은 전직 (전부 0점이면 첫 번째) */
  private String pickAscendancy(String className, List<String> keywords) {
    List<String> options = poeTreeGraphService.ascendancies(className);
    if (options.isEmpty()) {
      return null;
    }
    String best = options.get(0);
    int bestScore = -1;
    for (String option : options) {
      if (poeTreeGraphService.ascendancyStart(option) == null) {
        continue; // 시작 노드가 없는 전직(데이터 이상)은 제외
      }
      int total = 0;
      for (PoeTreeGraphService.TreeNode node : poeTreeGraphService.ascendancyCandidates(option)) {
        total += score(node.stats(), keywords);
      }
      if (total > bestScore) {
        bestScore = total;
        best = option;
      }
    }
    return best;
  }

  /**
   * 직업 전직 노드 greedy 할당 — 시작 노드 포함, budget 포인트까지 경로 비용 대비 실측 이득 최대 노드를 반복 채택. ascendancyNodes 를 직접
   * 변형하며, 이미 배분된 만큼(size-1)부터 이어서 채운다(예약 회수 재호출 지원). 새 목표값 반환.
   */
  private double greedyAscendancy(
      ExecutorService executor,
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> allocated,
      Map<Slot, Equipped> items,
      String objectiveKey,
      int budget,
      double current) {
    Integer start = poeTreeGraphService.ascendancyStart(ascendancy);
    if (start == null) {
      return current;
    }
    ascendancyNodes.add(start);
    List<PoeTreeGraphService.TreeNode> remaining =
        poeTreeGraphService.ascendancyCandidates(ascendancy).stream()
            .filter(n -> !ascendancyNodes.contains(n.id()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    int points = ascendancyNodes.size() - 1; // 이미 배분된 노드 수(시작 제외)
    while (points < budget && !remaining.isEmpty()) {
      record AscendancyReachable(PoeTreeGraphService.TreeNode node, List<Integer> path) {}
      List<AscendancyReachable> reachable = new ArrayList<>();
      for (PoeTreeGraphService.TreeNode candidate : remaining) {
        List<Integer> path =
            poeTreeGraphService.shortestPathInAscendancy(
                ascendancyNodes, candidate.id(), ascendancy);
        if (path == null || path.isEmpty() || points + path.size() > budget) {
          continue;
        }
        reachable.add(new AscendancyReachable(candidate, path));
      }
      if (reachable.isEmpty()) {
        break;
      }
      double currentBeforeRound = current;
      Map<AscendancyReachable, Double> results =
          evalBatch(
              executor,
              reachable,
              candidate -> {
                Set<Integer> trial = new LinkedHashSet<>(ascendancyNodes);
                trial.addAll(candidate.path());
                return buildXml(gem, supports, className, ascendancy, trial, allocated, items);
              },
              objectiveKey);
      AscendancyReachable best = null;
      double bestGainPerPoint = 0;
      for (Map.Entry<AscendancyReachable, Double> entry : results.entrySet()) {
        double gainPerPoint =
            (entry.getValue() - currentBeforeRound) / entry.getKey().path().size();
        if (gainPerPoint > bestGainPerPoint) {
          bestGainPerPoint = gainPerPoint;
          best = entry.getKey();
        }
      }
      if (best == null) {
        break;
      }
      ascendancyNodes.addAll(best.path());
      points += best.path().size();
      current = results.get(best);
      remaining.remove(best.node());
      log(
          "전직 할당: "
              + (best.node().nameKo() != null ? best.node().nameKo() : best.node().name())
              + " (+"
              + best.path().size()
              + "pt, "
              + points
              + "/"
              + ASCENDANCY_POINT_BUDGET
              + ") → "
              + format(current));
    }
    return current;
  }

  /** 직업 프로브용 — 엔진 없이 키워드 점수/경로 비용 greedy 로 전직 8pt 를 대략 할당 (시작 노드 포함) */
  private Set<Integer> heuristicAscendancyNodes(String ascendancy, List<String> keywords) {
    return heuristicAscendancyNodes(ascendancy, keywords, ASCENDANCY_POINT_BUDGET);
  }

  /** budget 포인트만큼 휴리스틱 할당 (혈맹 프로브에서 작은 예산으로 재사용) */
  private Set<Integer> heuristicAscendancyNodes(
      String ascendancy, List<String> keywords, int budget) {
    Set<Integer> nodes = new LinkedHashSet<>();
    Integer start = ascendancy != null ? poeTreeGraphService.ascendancyStart(ascendancy) : null;
    if (start == null) {
      return nodes;
    }
    nodes.add(start);
    List<PoeTreeGraphService.TreeNode> remaining =
        new ArrayList<>(poeTreeGraphService.ascendancyCandidates(ascendancy));
    int points = 0;
    while (points < budget && !remaining.isEmpty()) {
      PoeTreeGraphService.TreeNode best = null;
      List<Integer> bestPath = null;
      double bestPriority = 0;
      for (PoeTreeGraphService.TreeNode candidate : remaining) {
        List<Integer> path =
            poeTreeGraphService.shortestPathInAscendancy(nodes, candidate.id(), ascendancy);
        if (path == null || path.isEmpty() || points + path.size() > budget) {
          continue;
        }
        double priority = (score(candidate.stats(), keywords) + 1) / (double) path.size();
        if (priority > bestPriority) {
          bestPriority = priority;
          best = candidate;
          bestPath = path;
        }
      }
      if (best == null) {
        break;
      }
      nodes.addAll(bestPath);
      points += bestPath.size();
      remaining.remove(best);
    }
    return nodes;
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

  /** 슬롯별 고유 아이템 후보 — 카테고리 매칭 + 키워드 점수 상위 N. 이미 장착한 고유는 제외(플라스크 등 동일 고유 중복 불가) */
  /**
   * 전역 유니크 주얼 후보 — 반경/변형/클러스터/타임리스 제외(효과가 주변 패시브·서브그래프 의존이라 단순 소켓 평가 부적합), 키워드 점수 상위 N. 소켓에 꽂으면 스탯이
   * 전역 적용된다.
   */
  private List<PoeUniqueItem> globalJewelCandidates(List<String> keywords) {
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "jewel", null).stream()
        .filter(item -> item.requiredLevel() == null || item.requiredLevel() <= LEVEL)
        .filter(
            item ->
                item.baseType() == null
                    || (!item.baseType().contains("Cluster")
                        && !item.baseType().contains("Timeless")))
        .filter(
            item ->
                item.explicits().stream()
                    .noneMatch(
                        line -> {
                          String lower = line.toLowerCase(Locale.ROOT);
                          return lower.contains("radius")
                              || lower.contains("in radius")
                              || lower.contains("nearby");
                        }))
        .map(item -> new Scored(item, score(item.explicits(), keywords)))
        .filter(scored -> scored.score() > 0)
        .sorted(Comparator.comparingInt(Scored::score).reversed())
        .limit(ITEM_CANDIDATES)
        .map(Scored::item)
        .toList();
  }

  /** 유니크 카테고리 → 고정 슬롯 (반지/플라스크/무기는 별도 처리) */
  private static final Map<String, Slot> CATEGORY_SLOT =
      Map.ofEntries(
          Map.entry("helmet", Slot.HELMET),
          Map.entry("body", Slot.BODY),
          Map.entry("gloves", Slot.GLOVES),
          Map.entry("boots", Slot.BOOTS),
          Map.entry("amulet", Slot.AMULET),
          Map.entry("belt", Slot.BELT),
          Map.entry("shield", Slot.OFFHAND),
          Map.entry("quiver", Slot.OFFHAND));

  private static final Set<String> WEAPON_CATEGORIES =
      Set.of("sword", "mace", "staff", "axe", "bow", "wand", "dagger", "claw", "sceptre");

  /** 콤마구분 slug → 존재하는 유니크 목록(중복/미존재 제거). */
  private List<PoeUniqueItem> resolveFixedUniques(String uniques) {
    if (uniques == null || uniques.isBlank()) {
      return new ArrayList<>();
    }
    List<PoeUniqueItem> resolved = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String slug : uniques.split(",")) {
      String s = slug.trim();
      if (s.isEmpty() || !seen.add(s)) {
        continue;
      }
      poeUniqueDataService.findBySlug(s).ifPresent(resolved::add);
    }
    return resolved;
  }

  /** 콤마구분 slug → 메인젬 제외한 존재하는 액티브 스킬 목록(보조젬/중복/미존재 제거). */
  private List<PoeGem> resolveAdditionalSkills(String skills, String mainSlug) {
    if (skills == null || skills.isBlank()) {
      return new ArrayList<>();
    }
    List<PoeGem> resolved = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String slug : skills.split(",")) {
      String s = slug.trim();
      if (s.isEmpty() || s.equals(mainSlug) || !seen.add(s)) {
        continue;
      }
      poeGemDataService.findBySlug(s).filter(g -> !g.isSupport()).ifPresent(resolved::add);
    }
    return resolved;
  }

  /** 강제 장착 유니크를 카테고리 기준으로 슬롯에 배치. 반지 2·플라스크 5 는 순서대로, 무기류는 WEAPON. 슬롯 없음/중복은 건너뜀. */
  private void placeFixedUniques(Map<Slot, Equipped> items) {
    if (fixedUniques.isEmpty()) {
      return;
    }
    Deque<Slot> rings = new ArrayDeque<>(List.of(Slot.RING1, Slot.RING2));
    Deque<Slot> flasks =
        new ArrayDeque<>(List.of(Slot.FLASK1, Slot.FLASK2, Slot.FLASK3, Slot.FLASK4, Slot.FLASK5));
    for (PoeUniqueItem unique : fixedUniques) {
      String cat = unique.category();
      Slot slot;
      if ("ring".equals(cat)) {
        slot = rings.poll();
      } else if ("flask".equals(cat)) {
        slot = flasks.poll();
      } else if (cat != null && WEAPON_CATEGORIES.contains(cat)) {
        slot = Slot.WEAPON;
      } else {
        slot = CATEGORY_SLOT.get(cat);
      }
      String label = unique.nameKo() != null ? unique.nameKo() : unique.name();
      if (slot == null || items.containsKey(slot)) {
        log("강제 유니크 배치 불가(지원 슬롯 없음/중복): " + label + " [" + cat + "]");
        continue;
      }
      items.put(slot, Equipped.ofUnique(unique));
      log("강제 장착: " + slot.ko + " = " + label);
    }
  }

  private List<PoeUniqueItem> itemCandidates(
      Slot slot, PoeGem gem, List<String> keywords, Map<Slot, Equipped> equipped) {
    List<String> categories = slot == Slot.WEAPON ? weaponCategories(gem) : slot.categories;
    Set<String> equippedSlugs = new LinkedHashSet<>();
    for (Map.Entry<Slot, Equipped> entry : equipped.entrySet()) {
      if (entry.getKey() != slot && entry.getValue().isUnique()) {
        equippedSlugs.add(entry.getValue().unique().slug());
      }
    }
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "all", null).stream()
        .filter(item -> categories.contains(item.category()))
        .filter(item -> item.requiredLevel() == null || item.requiredLevel() <= LEVEL)
        .filter(item -> !equippedSlugs.contains(item.slug()))
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

  /** 주얼 없는 빌드 (보조젬/전직/트리 단계에서 사용) */
  private String buildXml(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> treeNodes,
      Map<Slot, Equipped> items) {
    return buildXml(
        gem, supports, className, ascendancy, ascendancyNodes, treeNodes, items, Map.of());
  }

  private String buildXml(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> treeNodes,
      Map<Slot, Equipped> items,
      Map<Integer, PoeUniqueItem> jewels) {
    // 탐색/최종 빌드는 현재까지 채택된 방어 오라를 함께 반영(오라 스테이지 트라이얼은 buildXmlAuras 로 명시 전달)
    return buildXmlAuras(
        gem,
        supports,
        className,
        ascendancy,
        ascendancyNodes,
        treeNodes,
        items,
        jewels,
        selectedAuras);
  }

  private String buildXmlAuras(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> treeNodes,
      Map<Slot, Equipped> items,
      Map<Integer, PoeUniqueItem> jewels,
      List<PoeGem> auras) {
    return buildXmlAuras(
        gem,
        supports,
        className,
        ascendancy,
        ascendancyNodes,
        treeNodes,
        items,
        jewels,
        auras,
        Map.of());
  }

  /**
   * @param masteryEffects 마스터리 노드 id → 선택한 효과 id. PoB 는 어떤 효과를 골랐는지 알아야 계산에 반영한다 (Spec 의 {@code
   *     masteryEffects="{노드,효과},…"} 속성). 비어 있으면 마스터리는 스탯 없이 노드만 찍힌 셈이 된다.
   */
  private String buildXmlAuras(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> treeNodes,
      Map<Slot, Equipped> items,
      Map<Integer, PoeUniqueItem> jewels,
      List<PoeGem> auras,
      Map<Integer, Integer> masteryEffects) {
    Set<Integer> specNodes = new LinkedHashSet<>(ascendancyNodes);
    specNodes.addAll(treeNodes);
    StringBuilder xml = new StringBuilder();
    xml.append("<PathOfBuilding>")
        .append("<Build level=\"")
        .append(LEVEL)
        .append("\" targetVersion=\"3_0\" className=\"")
        .append(className)
        .append("\" ascendClassName=\"")
        .append(ascendancy != null ? ascendancy : "None")
        .append("\" mainSocketGroup=\"1\"/>")
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
    // 주얼: 소켓 노드는 nodes 에 포함되어야 하고, 주얼 아이템 id 는 장비(1..N)와 안 겹치게 900번대
    for (int socketNode : jewels.keySet()) {
      specNodes.add(socketNode);
    }
    StringBuilder sockets = new StringBuilder();
    if (!jewels.isEmpty()) {
      int jewelId = 900;
      for (int socketNode : jewels.keySet()) {
        sockets
            .append("<Socket nodeId=\"")
            .append(socketNode)
            .append("\" itemId=\"")
            .append(jewelId++)
            .append("\"/>");
      }
    }
    xml.append("</Skill>");
    // 오라 = 별도 스킬 그룹(메인 아님) — PoB 가 예약/버프로 DPS·EHP 에 반영한다.
    // Enlighten(예약 감소)을 함께 넣어 마나 예약 여유를 확보(실제 빌드 관례) → 오라가 메인 스킬을 굶기지 않게.
    // 각성한 계몽 5레벨 = 예약 감소 최대. 실측: 계몽3 → 4 → 각성5 로 갈수록 미예약 마나 136 → 151 → 165 로 늘어
    // 오라가 1~2개 더 들어간다(본섀터 DPS 1,513,597 → 1,580,649). 나머지 젬 가정(20/20, 최상위 레어)과 같은 엔드게임 전제.
    if (auras != null && !auras.isEmpty()) {
      xml.append("<Skill enabled=\"true\" slot=\"Helmet\">")
          .append(
              "<Gem nameSpec=\"Awakened Enlighten\" level=\"5\" quality=\"0\" enabled=\"true\"/>");
      for (PoeGem aura : auras) {
        xml.append("<Gem nameSpec=\"")
            .append(aura.name())
            .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/>");
      }
      xml.append("</Skill>");
    }
    // 추가 스킬(사용자 지정) — 각자 별도 그룹으로 emit. PoB 가 오라=예약+버프, 커스=적약화, 헤럴드/가드 등 역할대로 반영.
    for (PoeGem extra : additionalSkills) {
      xml.append("<Skill enabled=\"true\" slot=\"Gloves\"><Gem nameSpec=\"")
          .append(extra.name())
          .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/></Skill>");
    }
    xml.append("</SkillSet></Skills>")
        .append("<Tree activeSpec=\"1\"><Spec treeVersion=\"")
        .append(treeVersion)
        .append("\" classId=\"")
        .append(CLASS_IDS.getOrDefault(className, 0))
        .append("\" ascendClassId=\"")
        .append(ascendancy != null ? poeTreeGraphService.ascendClassId(className, ascendancy) : 0)
        .append(secondaryAscendId > 0 ? "\" secondaryAscendClassId=\"" + secondaryAscendId : "")
        .append("\" nodes=\"")
        .append(String.join(",", specNodes.stream().map(String::valueOf).toList()))
        .append(
            masteryEffects.isEmpty()
                ? ""
                : "\" masteryEffects=\""
                    + masteryEffects.entrySet().stream()
                        .filter(e -> specNodes.contains(e.getKey()))
                        .map(e -> "{" + e.getKey() + "," + e.getValue() + "}")
                        .collect(java.util.stream.Collectors.joining(",")))
        .append("\">")
        .append(sockets.length() > 0 ? "<Sockets>" + sockets + "</Sockets>" : "")
        .append("</Spec></Tree>");

    xml.append("<Items activeItemSet=\"1\">");
    StringBuilder slots = new StringBuilder();
    int itemId = 0;
    for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
      itemId++;
      Equipped equipped = entry.getValue();
      String itemText =
          equipped.isUnique() ? uniqueItemText(equipped.unique()) : rareItemText(equipped.rare());
      xml.append("<Item id=\"").append(itemId).append("\">\n").append(itemText).append("</Item>");
      slots
          .append("<Slot name=\"")
          .append(entry.getKey().pobName)
          .append("\" itemId=\"")
          .append(itemId)
          // 플라스크는 active 여야 PoB 가 효과를 계산에 넣는다
          .append(entry.getKey().pobName.startsWith("Flask") ? "\" active=\"true\"/>" : "\"/>");
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
    // 주얼 아이템 (900번대 id, 소켓의 itemId 와 일치) — ItemSet 슬롯에는 넣지 않는다(트리 Sockets 로 연결)
    int jewelId = 900;
    for (PoeUniqueItem jewel : jewels.values()) {
      xml.append("<Item id=\"")
          .append(jewelId++)
          .append("\">\n")
          .append(uniqueItemText(jewel))
          .append("</Item>");
    }
    if (itemId > 0) {
      xml.append("<ItemSet id=\"1\">").append(slots).append("</ItemSet>");
    }
    xml.append("</Items>");
    // 적 시나리오 (DPS/EHP 계산에 반영) + 전투 버프 가정(충전+돌격)
    xml.append(configBlock(enemyScenario, combatBuffs));
    xml.append("</PathOfBuilding>");
    return xml.toString();
  }

  /** PoB {@code <Config>} 블록 — 적 시나리오/전투 버프 가정을 반영. 가정별 매트릭스 재평가에서 재사용. */
  private String configBlock(String scenario, boolean buffs) {
    StringBuilder config = new StringBuilder();
    config.append("<Config><Input name=\"enemyIsBoss\" string=\"").append(scenario).append("\"/>");
    if (buffs) {
      config
          .append("<Input name=\"usePowerCharges\" boolean=\"true\"/>")
          .append("<Input name=\"useFrenzyCharges\" boolean=\"true\"/>")
          .append("<Input name=\"buffOnslaught\" boolean=\"true\"/>");
    }
    config.append("</Config>");
    return config.toString();
  }

  /** 완성된 빌드 XML 의 {@code <Config>} 블록만 다른 가정으로 교체 (매트릭스 재평가용). */
  private String withConfig(String xml, String scenario, boolean buffs) {
    return CONFIG_BLOCK
        .matcher(xml)
        .replaceFirst(java.util.regex.Matcher.quoteReplacement(configBlock(scenario, buffs)));
  }

  private static final java.util.regex.Pattern CONFIG_BLOCK =
      java.util.regex.Pattern.compile("<Config>.*?</Config>", java.util.regex.Pattern.DOTALL);

  /** 가정별 성능 매트릭스 적 시나리오 순서 (표시 순서와 동일) */
  private static final List<String> MATRIX_SCENARIOS = List.of("None", "Boss", "Pinnacle", "Uber");

  /**
   * 완성된 빌드를 적 시나리오 4종 × 전투 버프 off/on 로 재평가한 DPS 매트릭스를 만든다. 각 셀은 {@code <Config>} 만 바꾼 XML 을
   * executor 로 병렬 계산한다. 계산 실패 셀은 "-" 로 둔다.
   */
  private List<PoeOptimizeResult.ScenarioCell> computeScenarioMatrix(
      String finalXml, ExecutorService executor) {
    // 셀별 Future 제출 (4×2). 키: scenario|buffs
    Map<String, java.util.concurrent.Future<Double>> futures = new LinkedHashMap<>();
    for (String scenario : MATRIX_SCENARIOS) {
      for (boolean buffs : new boolean[] {false, true}) {
        String xml = withConfig(finalXml, scenario, buffs);
        futures.put(
            scenario + "|" + buffs,
            executor.submit(
                () -> {
                  Map<String, Double> values = poePobEngineService.calculateValues(xml);
                  evalCount.incrementAndGet();
                  return values.getOrDefault("CombinedDPS", 0d);
                }));
      }
    }
    List<PoeOptimizeResult.ScenarioCell> matrix = new ArrayList<>();
    for (String scenario : MATRIX_SCENARIOS) {
      matrix.add(
          new PoeOptimizeResult.ScenarioCell(
              scenario,
              SCENARIO_KO.getOrDefault(scenario, scenario),
              matrixCell(futures.get(scenario + "|false")),
              matrixCell(futures.get(scenario + "|true"))));
    }
    return matrix;
  }

  private String matrixCell(java.util.concurrent.Future<Double> future) {
    try {
      return format(future.get());
    } catch (Exception e) {
      return "-";
    }
  }

  /** 유형별 최대 피격 생존(PoB *MaximumHitTaken) → 표시용 목록. 값이 하나도 없으면 빈 목록. */
  private List<PoeOptimizeResult.DefenseHit> defenseHits(Map<String, Double> values) {
    String[][] types = {
      {"physical", "PhysicalMaximumHitTaken"},
      {"fire", "FireMaximumHitTaken"},
      {"cold", "ColdMaximumHitTaken"},
      {"lightning", "LightningMaximumHitTaken"},
      {"chaos", "ChaosMaximumHitTaken"},
    };
    List<PoeOptimizeResult.DefenseHit> hits = new ArrayList<>();
    for (String[] type : types) {
      Double value = values.get(type[1]);
      if (value != null && value > 0) {
        hits.add(new PoeOptimizeResult.DefenseHit(type[0], format(value)));
      }
    }
    return hits;
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

  // ── 레어 생성 ────────────────────────────────────────────

  /**
   * 슬롯에 맞는 레어 아이템을 생성한다 — 모드 풀에서 키워드 관련 접두 최대 3 + 접미 최대 3 선택. tierFraction 0=최상위 티어, 1=최하위. 모드 풀
   * 없거나 슬롯이 레어 미지원(무기/플라스크)이면 null.
   */
  private RareItem craftRare(Slot slot, PoeGem gem, List<String> keywords, double tierFraction) {
    if (!poeModPoolDataService.hasData()) {
      return null;
    }
    // 무기는 젬 종류로 카테고리/베이스를 동적 결정 (주문 = 완드, 공격 = 표준 무기)
    String category;
    String rareBase;
    if (slot == Slot.WEAPON) {
      List<String> tags = gem.tags() != null ? gem.tags() : List.of();
      if (tags.contains("Attack")) {
        category = "weaponAttack";
        rareBase = tags.contains("Bow") ? "Thicket Bow" : "Vaal Axe";
      } else {
        category = "weaponSpell";
        rareBase = "Imbued Wand";
      }
    } else {
      if (slot.rareBase == null || slot.modSlots.isEmpty()) {
        return null;
      }
      category = slot.modSlots.get(0);
      rareBase = slot.rareBase;
    }
    return craftRare(category, rareBase, keywords, tierFraction, false);
  }

  /**
   * 레어 생성 코어. defensive=true 면 실전 BiS 레어처럼 생명(접두 1) + 저항(접미 2) 을 우선 확보한 뒤 남는 슬롯을 데미지 모드로 채운다(밸런스/생존
   * 목표에서 유니크와 공정 경쟁). defensive=false 면 기존처럼 데미지 모드만(순수 DPS 극대화용).
   */
  private RareItem craftRare(
      String category,
      String rareBase,
      List<String> keywords,
      double tierFraction,
      boolean defensive) {
    record Scored(PoeModPoolDataService.ModFamily family, int score) {}
    List<PoeModPoolDataService.ModFamily> pool = poeModPoolDataService.familiesForSlot(category);
    List<PoeModPoolDataService.ModFamily> prefixPool =
        pool.stream().filter(f -> "prefix".equals(f.gen())).toList();
    List<PoeModPoolDataService.ModFamily> suffixPool =
        pool.stream().filter(f -> "suffix".equals(f.gen())).toList();

    java.util.function.BiFunction<
            List<PoeModPoolDataService.ModFamily>,
            java.util.function.Predicate<PoeModPoolDataService.ModFamily>,
            PoeModPoolDataService.ModFamily>
        firstMatch = (list, pred) -> list.stream().filter(pred).findFirst().orElse(null);

    List<PoeModPoolDataService.ModFamily> prefixes = new ArrayList<>();
    List<PoeModPoolDataService.ModFamily> suffixes = new ArrayList<>();
    if (defensive) {
      // 생명 접두 1개 + 저항 접미 2개를 먼저 확보 (실전 레어의 방어 기반)
      PoeModPoolDataService.ModFamily life =
          firstMatch.apply(prefixPool, f -> hasKeyword(f, "life"));
      if (life != null) {
        prefixes.add(life);
      }
      suffixPool.stream().filter(f -> hasKeyword(f, "resistance")).limit(2).forEach(suffixes::add);
    }
    // 남는 접두/접미 슬롯(각 3개까지)을 데미지 키워드 상위로 채움
    prefixPool.stream()
        .filter(f -> !prefixes.contains(f))
        .map(f -> new Scored(f, score(f.keywords(), keywords)))
        .filter(s -> s.score() > 0)
        .sorted(Comparator.comparingInt(Scored::score).reversed())
        .map(Scored::family)
        .forEach(
            f -> {
              if (prefixes.size() < 3) prefixes.add(f);
            });
    suffixPool.stream()
        .filter(f -> !suffixes.contains(f))
        .map(f -> new Scored(f, score(f.keywords(), keywords)))
        .filter(s -> s.score() > 0)
        .sorted(Comparator.comparingInt(Scored::score).reversed())
        .map(Scored::family)
        .forEach(
            f -> {
              if (suffixes.size() < 3) suffixes.add(f);
            });
    List<PoeModPoolDataService.ModFamily> chosen = new ArrayList<>(prefixes);
    chosen.addAll(suffixes);
    if (chosen.isEmpty()) {
      return null;
    }
    return new RareItem(rareBase, chosen, tierFraction);
  }

  /** 슬롯의 카테고리/베이스만 뽑아 defensive 레어를 생성 (없으면 null) */
  private RareItem craftDefensiveRare(Slot slot, PoeGem gem, List<String> keywords) {
    String category;
    String rareBase;
    if (slot == Slot.WEAPON) {
      return null; // 무기는 방어 기반 무의미
    }
    if (slot.rareBase == null || slot.modSlots.isEmpty()) {
      return null;
    }
    category = slot.modSlots.get(0);
    rareBase = slot.rareBase;
    return craftRare(category, rareBase, keywords, 0.0, true);
  }

  private boolean hasKeyword(PoeModPoolDataService.ModFamily family, String keyword) {
    return family.keywords() != null && family.keywords().contains(keyword);
  }

  /** 레어 → PoB 아이템 텍스트 (각 패밀리를 tierFraction 위치의 티어 최대 롤로) */
  private String rareItemText(RareItem rare) {
    StringBuilder text = new StringBuilder();
    text.append("Rarity: RARE\nSim Craft\n").append(rare.baseType()).append("\n");
    text.append("Item Level: 86\nImplicits: 0\n");
    for (PoeModPoolDataService.ModFamily family : rare.families()) {
      for (String line : tierAt(family, rare.tierFraction()).en()) {
        text.append(line).append("\n");
      }
    }
    return text.toString();
  }

  /** 레어의 한국어 모드 라인 (표시용) */
  private List<String> rareModLinesKo(RareItem rare) {
    List<String> lines = new ArrayList<>();
    for (PoeModPoolDataService.ModFamily family : rare.families()) {
      lines.addAll(tierAt(family, rare.tierFraction()).ko());
    }
    return lines;
  }

  /** tierFraction(0=best..1=worst) → 해당 패밀리 티어 (tiers 는 best-first) */
  private PoeModPoolDataService.ModTier tierAt(
      PoeModPoolDataService.ModFamily family, double fraction) {
    List<PoeModPoolDataService.ModTier> tiers = family.tiers();
    int index = (int) Math.round(fraction * (tiers.size() - 1));
    return tiers.get(Math.min(Math.max(index, 0), tiers.size() - 1));
  }

  private String equippedLabel(Equipped equipped) {
    if (equipped.isUnique()) {
      PoeUniqueItem unique = equipped.unique();
      return unique.nameKo() != null ? unique.nameKo() : unique.name();
    }
    return "레어 " + equipped.rare().baseType();
  }

  private PoeOptimizeResult.ItemPick itemPick(Slot slot, Equipped equipped) {
    if (equipped.isUnique()) {
      PoeUniqueItem unique = equipped.unique();
      return new PoeOptimizeResult.ItemPick(
          slot.pobName,
          slot.ko,
          "UNIQUE",
          unique.slug(),
          unique.name(),
          unique.nameKo(),
          uniqueModLines(unique));
    }
    RareItem rare = equipped.rare();
    return new PoeOptimizeResult.ItemPick(
        slot.pobName, slot.ko, "RARE", null, rare.baseType(), null, rareModLinesKo(rare));
  }

  /** 고유 아이템 표시용 모드 라인 (임플리싯 + 익스플리싯, 한국어 우선·없으면 영문). 레어와 표시 일관성. */
  private List<String> uniqueModLines(PoeUniqueItem unique) {
    List<String> lines = new ArrayList<>();
    mergeLocaleLines(lines, unique.implicits(), unique.implicitsKo());
    mergeLocaleLines(lines, unique.explicits(), unique.explicitsKo());
    return lines;
  }

  private void mergeLocaleLines(List<String> out, List<String> en, List<String> ko) {
    if (en == null) {
      return;
    }
    for (int i = 0; i < en.size(); i++) {
      String korean = ko != null && i < ko.size() ? ko.get(i) : null;
      out.add(korean != null && !korean.isBlank() ? korean : en.get(i));
    }
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

  /** 트리 에디터에서 직접 찍은 트리를 PoB 엔진으로 실계산한 결과. */
  public record TreeEvaluation(
      String className,
      String classNameKo,
      String ascendancy,
      String gemName,
      String gemNameKo,
      int nodeCount,
      List<PoeBuild.PlayerStat> stats,
      String pobCode,
      long durationMs) {}

  /**
   * 사용자가 트리 에디터에서 찍은 노드 집합을 그대로 평가한다(장비/보조젬 없음). 최적화기와 달리 탐색을 하지 않으므로 엔진 1회 호출로 끝난다.
   *
   * @param classId GGG classId (0=Scion..6=Shadow)
   * @param ascendancy 전직 영문명(없으면 null)
   * @param nodes 할당 노드 id (클래스/전직 시작 노드 포함 가능 — PoB 가 무시)
   * @param gemSlug 주 스킬 젬 slug(없으면 방어 스탯만 의미 있음)
   * @param masteryEffects 마스터리 노드 id → 선택 효과 id (없으면 마스터리 스탯 미반영)
   */
  public TreeEvaluation evaluateTree(
      int classId,
      String ascendancy,
      Set<Integer> nodes,
      String gemSlug,
      Map<Integer, Integer> masteryEffects) {
    String className =
        CLASS_IDS.entrySet().stream()
            .filter(e -> e.getValue() == classId)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("Scion");
    PoeGem gem = null;
    if (gemSlug != null && !gemSlug.isBlank()) {
      gem = poeGemDataService.findBySlug(gemSlug).orElse(null);
    }
    if (gem == null) {
      // 스킬 미지정 — 계산이 성립하도록 표준 스킬 하나를 끼운다(방어 스탯 확인용)
      gem = allDamageSkills().stream().findFirst().orElse(null);
    }
    if (gem == null) {
      throw new IllegalStateException("평가에 쓸 스킬 젬이 없습니다 (젬 데이터 미로드)");
    }
    String xml =
        buildXmlAuras(
            gem,
            List.of(),
            className,
            ascendancy,
            Set.of(),
            nodes,
            Map.of(),
            Map.of(),
            List.of(),
            masteryEffects == null ? Map.of() : masteryEffects);
    PoePobEngineService.EngineResult result = poePobEngineService.recalculate(xml);
    return new TreeEvaluation(
        className,
        CLASS_KO.get(className),
        ascendancy,
        gem.name(),
        gem.nameKo(),
        nodes.size(),
        result.stats(),
        encodePobCode(xml),
        result.durationMs());
  }
}
