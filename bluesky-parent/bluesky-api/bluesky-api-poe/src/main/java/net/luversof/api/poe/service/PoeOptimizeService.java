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
  // 패시브 포인트 예산 — 레벨 90 기준 실제 획득량(레벨업 89 + 퀘스트 24 = 113). 이전엔 임의의 100 이었다.
  private static final int POINT_BUDGET = 113;
  private static final int MAX_SUPPORTS = 5;
  private static final int SUPPORT_SHORTLIST =
      24; // 48 로 넓혀도 결과 동일(평가만 +11%) — 상위 24 안에 최적 보조젬이 이미 있다
  // 라운드당 실측할 트리 후보 수. 12→64 로 넓히니 DPS +79%(3.34M→5.98M). 96 은 오히려 하락(그리디라 초반 선택이 바뀌며 다른 지역최적)
  private static final int TREE_ROUND_CANDIDATES = 64;
  private static final int TREE_MAX_ROUNDS = 30;
  // 슬롯당 실측할 아이템 후보 수. 10→64 로 DPS 5.98M→22.9M(+283%). 128 은 동일값(64 면 후보 풀을 이미 전부 커버)
  private static final int ITEM_CANDIDATES = 64;

  /** 전직 포인트 예산 (만렙 성역 8포인트, 시작 노드 제외) */
  private static final int ASCENDANCY_POINT_BUDGET = 8;

  /** 전직 8포인트 중 혈맹(2차 전직)에 배분 예약할 포인트 — 나머지는 직업 전직에 사용 */
  private static final int BLOODLINE_RESERVE = 2;

  /** 주얼 소켓용으로 트리 예산에서 예약하는 포인트 (너무 크면 트리 노터블 손실) */
  private static final int JEWEL_RESERVE = 10;

  /** 주얼 단계에서 평가할 최대 소켓 수 (가장 싸게 닿는 것부터) — 비용 제한 */
  private static final int JEWEL_MAX_SOCKETS = 5;

  /**
   * 노터블/키스톤 문신을 시험해 볼 자리 수 상한. 이 문신들은 "Limited to 1" 이라 (문신 × 자리) 를 전부 평가해야 하는데, 키스톤 풀만 45종이라 자리를 안
   * 막으면 평가 수가 곱으로 튄다.
   */
  private static final int TATTOO_MAX_SPOTS = 4;

  /** 자동으로 새로 찍어 볼 마스터리 후보 수 상한 — 후보마다 (효과 수)번 평가하므로 키워드 점수 상위만 본다. */
  private static final int MASTERY_MAX_NEW = 8;

  /** 방어 오라 최대 개수(마나 예약 한계로 실질 2~3개, PoB 가 초과 예약 시 효과를 깎아 greedy 가 자연 종료). */
  private static final int MAX_AURAS = 7;

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
          "Flesh and Stone",
          // 누락돼 있던 표준 오라 — 특히 Precision 은 명중이 중요한 공격 빌드에 핵심
          "Precision",
          "Haste",
          "Dread Banner",
          "Envy",
          "Clarity");

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

  /**
   * 레어 아이템 — 베이스 + 선택된 모드 패밀리. 기본은 모든 모드를 {@code tierFraction} 한 위치의 티어로 롤한다(0=최상위, 1=최하위). {@code
   * perFractions} 가 있으면 패밀리별로 다른 티어를 준다(어픽스 예산 모델: 필수 N개는 T1, 나머지는 중위).
   */
  private record RareItem(
      String baseType,
      List<PoeModPoolDataService.ModFamily> families,
      double tierFraction,
      List<Double> perFractions,
      // 엘드리치 임플리싯(총주교/포식자) — 영문(PoB Implicits 용) + 한글(표시용) 스탯 줄. 없으면 빈 목록.
      List<String> implicitLines,
      List<String> implicitLinesKo) {
    RareItem(String baseType, List<PoeModPoolDataService.ModFamily> families, double tierFraction) {
      this(baseType, families, tierFraction, null, List.of(), List.of());
    }

    RareItem(
        String baseType,
        List<PoeModPoolDataService.ModFamily> families,
        double tierFraction,
        List<Double> perFractions) {
      this(baseType, families, tierFraction, perFractions, List.of(), List.of());
    }

    /** i번째 패밀리에 적용할 티어 분수 — perFractions 가 있으면 그 값, 없으면 균일 tierFraction. */
    double fractionFor(int i) {
      return perFractions != null && i < perFractions.size() ? perFractions.get(i) : tierFraction;
    }
  }

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
  private final PoeEldritchDataService poeEldritchDataService;
  private final PoeModDataService poeModDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeClusterJewelDataService poeClusterJewelDataService;
  private final PoeSkillWeaponDataService poeSkillWeaponDataService;
  private final PoeTattooDataService poeTattooDataService;
  private final Path resultFile;
  private final String treeVersion;
  private final int parallelism;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger phaseDone = new AtomicInteger();
  private final AtomicInteger evalCount = new AtomicInteger();

  /**
   * 엔진 평가 실패 수. 실패한 후보는 -1 점이 되어 <b>조용히 탈락</b>하므로, 실패가 생기면 그리디가 다른 경로를 타 결과가 크게 달라진다(실행 간 최종 DPS
   * 2.4배 편차를 추적하다 계측을 넣었다). 보이지 않으면 원인을 못 잡는다.
   */
  private final AtomicInteger evalFailures = new AtomicInteger();

  private volatile String firstEvalError = null;
  private volatile int phaseTotal;
  private volatile String phase = "";

  /** 단계별 소요 계측 — 잡 완료 시 "단계별 소요" 로그로 요약한다(느린 단계를 찾으려면 눈에 보여야 한다). */
  private volatile long phaseEnteredAt;

  private final Map<String, Long> phaseDurations = new LinkedHashMap<>();

  private void enterPhase(String next) {
    long now = System.currentTimeMillis();
    synchronized (phaseDurations) {
      if (!phase.isEmpty() && phaseEnteredAt > 0) {
        phaseDurations.merge(phase, now - phaseEnteredAt, Long::sum);
      }
    }
    phaseEnteredAt = now;
    phase = next;
  }

  private String phaseSummary() {
    synchronized (phaseDurations) {
      return phaseDurations.entrySet().stream()
          .map(entry -> entry.getKey() + " " + Math.round(entry.getValue() / 1000.0) + "s")
          .collect(java.util.stream.Collectors.joining(" · "));
    }
  }

  private volatile Status lastStatus = Status.IDLE;
  private final Deque<String> logLines = new ArrayDeque<>();
  private volatile PoeOptimizeResult lastResult;

  public PoeOptimizeService(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoePobEngineService poePobEngineService,
      PoeTreeGraphService poeTreeGraphService,
      PoeModPoolDataService poeModPoolDataService,
      PoeEldritchDataService poeEldritchDataService,
      PoeModDataService poeModDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeClusterJewelDataService poeClusterJewelDataService,
      PoeSkillWeaponDataService poeSkillWeaponDataService,
      PoeTattooDataService poeTattooDataService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.sim.tree-version:3_28}") String treeVersion,
      @Value("${poe.sim.parallelism:0}") int parallelism,
      @Value("${poe.pob.src-dir:${user.home}/.poe-gamedata/work/pob-src}") String pobSourceDir) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poePobEngineService = poePobEngineService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeEldritchDataService = poeEldritchDataService;
    this.poeModDataService = poeModDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeClusterJewelDataService = poeClusterJewelDataService;
    this.poeSkillWeaponDataService = poeSkillWeaponDataService;
    this.poeTattooDataService = poeTattooDataService;
    this.resultFile = Path.of(dataDir, "sim", "optimize-last.json");
    this.treeVersion = treeVersion;
    this.pobSourceDir = pobSourceDir;
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

  // 이번 잡의 스킬 키워드 — XML 조립 시 유니크에 엘드리치 임플리싯을 고르는 데 쓴다(레어는 craft 시점에 이미 결정).
  private volatile List<String> currentKeywords = List.of();
  // 이 잡에서 아뮬렛에 걸 도유(잡마다 키워드가 정해질 때 1회 계산). null = 키워드에 맞는 노터블 없음
  private volatile AnointPick currentAnoint = null;
  // 이번 잡의 직업 — 레어 방어구의 속성 변형(힘=방어도/민첩=회피/지능=ES) 선택에 쓴다.
  private volatile String currentClassName = "";

  // 예약 초과로 제외된 오라(이름 → 부족 마나) — 결과 화면에서 "왜 오라가 이것뿐인지" 설명용
  private volatile Map<PoeGem, Integer> blockedAuraShortfall = new LinkedHashMap<>();

  // 사용자가 트리 에디터에서 확정한 트리(비어 있으면 탐색). 지정 시 트리 greedy 를 건너뛴다.
  private volatile Set<Integer> fixedTree = Set.of();

  /** 트리 에디터에서 확정한 마스터리 효과(노드 id → 효과 id). 고정 트리와 짝을 이룬다. */
  private volatile Map<Integer, Integer> fixedMasteries = Map.of();

  /** 트리 에디터에서 소켓에 꽂아둔 유니크 주얼(소켓 노드 id → slug). 최적화기는 나머지 소켓만 채운다. */
  private volatile Map<Integer, String> fixedJewels = Map.of();

  /**
   * 트리 에디터에서 꽂아둔 클러스터 주얼. 이게 없으면 고정 트리에 들어 있는 생성 노드(id ≥ 65536)를 PoB 가 <b>존재하지 않는 노드로 무시</b>해, 트리
   * 화면 수치보다 낮은 값으로 최적화가 돌아간다.
   */
  private volatile List<ClusterSpec> fixedClusters = List.of();

  /** 트리 에디터에서 패시브에 새긴 문신(노드 id → 문신 dn). 지정하면 그 노드는 문신 노드로 교체돼 계산된다. */
  private volatile Map<Integer, String> fixedTattoos = Map.of();

  // 트리 에디터에서 사용자가 고른 도유 노터블 id — 지정 시 자동 전수 스윕 대신 이것으로 고정(사용자 지정은 존중)
  private volatile Integer fixedAnoint = null;

  /** 최적화 잡 시작 — 클러스터 없이(옛 호출부 호환) */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills,
      String treeNodes,
      String masteries,
      String jewels) {
    return start(
        gemSlug,
        objective,
        scenario,
        buffs,
        className,
        ascendancy,
        uniques,
        skills,
        treeNodes,
        masteries,
        jewels,
        null);
  }

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
    return start(gemSlug, objective, scenario, buffs, className, ascendancy, uniques, skills, null);
  }

  /**
   * @param treeNodes 사용자가 트리 에디터에서 확정한 노드(콤마구분 id). 지정하면 트리 탐색을 건너뛰고 이 트리를 고정한 채 보조젬/주얼/장비/오라만
   *     최적화한다. 비어 있으면 기존처럼 트리도 탐색한다.
   */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills,
      String treeNodes) {
    return start(
        gemSlug,
        objective,
        scenario,
        buffs,
        className,
        ascendancy,
        uniques,
        skills,
        treeNodes,
        null,
        null);
  }

  /**
   * @param masteries 트리 에디터에서 고른 마스터리 효과("노드:효과,..."). 고정 트리와 함께 넘겨야 마스터리 스탯이 계산에 반영된다 — 안 넘기면 마스터리
   *     노드만 찍힌 셈이라 사용자가 설계한 트리보다 약하게 평가된다.
   */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills,
      String treeNodes,
      String masteries,
      String jewels,
      String clusters) {
    return start(
        gemSlug,
        objective,
        scenario,
        buffs,
        className,
        ascendancy,
        uniques,
        skills,
        treeNodes,
        masteries,
        jewels,
        clusters,
        null);
  }

  /**
   * @param tattoos 트리 에디터에서 패시브에 새긴 문신("노드:영문명|…"). 안 넘기면 화면엔 문신이 보이는데 최적화는 원래 패시브로 돌아 사용자가 설계한 것보다
   *     약한 트리를 평가한다(마스터리 효과와 같은 계열의 함정).
   */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills,
      String treeNodes,
      String masteries,
      String jewels,
      String clusters,
      String tattoos) {
    return start(
        gemSlug,
        objective,
        scenario,
        buffs,
        className,
        ascendancy,
        uniques,
        skills,
        treeNodes,
        masteries,
        jewels,
        clusters,
        tattoos,
        null);
  }

  /**
   * @param anoint 트리 에디터에서 고른 도유 노터블 id(없으면 null → 자동 전수 스윕). 문신/마스터리와 같은 "사용자 지정 존중" 원칙.
   */
  public boolean start(
      String gemSlug,
      String objective,
      String scenario,
      boolean buffs,
      String className,
      String ascendancy,
      String uniques,
      String skills,
      String treeNodes,
      String masteries,
      String jewels,
      String clusters,
      String tattoos,
      String anoint) {
    if (!isAvailable()) {
      return false;
    }
    this.fixedTree = parseNodeIds(treeNodes);
    this.fixedTattoos = parseTattoos(tattoos);
    this.fixedAnoint = anoint != null && anoint.matches("\\d+") ? Integer.valueOf(anoint) : null;
    this.fixedMasteries = parseMasteries(masteries);
    this.fixedJewels = parseJewels(jewels);
    this.fixedClusters = parseClusters(clusters);
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
    this.currentKeywords = List.of(); // 키워드 초기화(잡마다)
    this.currentAnoint = null; // 아뮬렛 도유 초기화(잡마다)
    this.currentClassName = ""; // 직업 초기화(잡마다)
    this.blockedAuraShortfall = new LinkedHashMap<>(); // 제외 오라 초기화(잡마다)
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
    evalFailures.set(0);
    firstEvalError = null;
    synchronized (phaseDurations) {
      phaseDurations.clear();
    }
    phaseEnteredAt = 0;
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
      this.currentKeywords = keywords;
      log(gem.name() + " / 목표 " + objectiveKey + " / 키워드 " + keywords);

      // ── 0) 직업 비교 프로브 — 직업별 (최적 전직 + 휴리스틱 8pt) 를 엔진 1회씩 평가해 최고 직업 선택 ──
      enterPhase("class");
      record ClassProbe(String probeClass, String probeAscendancy, Set<Integer> probeNodes) {}
      List<ClassProbe> probes = new ArrayList<>();
      // 직업 고정 시 그 직업만, 아니면 전 직업 프로브.
      // CLASS_IDS 는 Map.of 라 keySet() 순회가 실행마다 다르다 — 프로브 점수가 동점이면 승자가 실행마다
      // 갈리므로 classId 순으로 고정한다(표준 무기 건과 같은 SALT 계열).
      List<String> classPool =
          fixedClass != null
              ? List.of(fixedClass)
              : CLASS_IDS.entrySet().stream()
                  .sorted(Map.Entry.comparingByValue())
                  .map(Map.Entry::getKey)
                  .toList();
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
      // 프로브 입력 지문 — 프로세스 간 결과가 갈릴 때 "입력이 다른가, 엔진이 다른가"를 로그만으로 가른다.
      // 노드 수/합이 아니라 **XML 전체 해시**여야 확정적이다(합은 다른 집합에서도 같을 수 있다).
      for (ClassProbe probe : probes) {
        String probeXml =
            buildXml(
                gem,
                List.of(),
                probe.probeClass(),
                probe.probeAscendancy(),
                probe.probeNodes(),
                Set.of(),
                Map.of());
        // ⚠ `\R`(개행 매처)은 **문자 클래스 안에서는 불법**이라 `[^\R]` 로 쓰면 PatternSyntaxException 이 나고
        //    잡이 통째로 실패한다(실제로 그렇게 터뜨렸다). 개행 다음 한 줄은 그냥 [^\n\r] 로 잡는다.
        java.util.regex.Matcher nodesAttr =
            java.util.regex.Pattern.compile("nodes=\"([^\"]*)\"").matcher(probeXml);
        java.util.regex.Matcher weaponAttr =
            java.util.regex.Pattern.compile("Sim Weapon[\\r\\n]+([^\\r\\n]*)").matcher(probeXml);
        log(
            "프로브 입력 "
                + probe.probeClass()
                + " · "
                + probe.probeAscendancy()
                + " · 무기 "
                + (weaponAttr.find() ? weaponAttr.group(1) : "(없음)")
                + " · nodes="
                + (nodesAttr.find() ? nodesAttr.group(1) : "?"));
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
      this.currentClassName = className;
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

      enterPhase("baseline");
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
        enterPhase("ascendancy");
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
        enterPhase("bloodline");
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
        enterPhase("ascendancy");
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
        enterPhase("supports");
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

      // ── 2) 패시브 트리 — 사용자가 확정한 트리가 있으면 그대로 쓰고, 없으면 greedy 탐색 ──
      enterPhase("tree");
      // 주얼 단계까지 공유하는 할당/포인트 상태 — 고정 트리든 탐색이든 동일하게 쓴다
      Set<Integer> allocatedWithStart = new LinkedHashSet<>();
      allocatedWithStart.add(classStart);
      int points = 0;
      if (!fixedTree.isEmpty()) {
        // 트리 에디터에서 넘어온 확정 트리 — 전직/클래스 노드는 별도 관리되므로 일반 노드만 반영
        for (int id : fixedTree) {
          // 클러스터 주얼이 만든 노드(id ≥ 65536)는 트리 데이터에 없다 — node() 가 null 이라
          // 여기서 걸러지면 주얼을 아무리 끼워 넣어도 **할당이 안 돼 효과가 0** 이 된다.
          if (id >= 0x10000) {
            if (!fixedClusters.isEmpty()) {
              allocated.add(id);
            }
            continue;
          }
          PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(id);
          if (node != null && node.ascendancy() == null && !"class".equals(node.type())) {
            allocated.add(id);
          }
        }
        current =
            objectiveOf(
                poePobEngineService.calculateValues(
                    buildXml(
                        gem, supports, className, ascendancy, ascendancyNodes, allocated, items)),
                objectiveKey);
        allocatedWithStart.addAll(allocated);
        points = allocated.size();
        log("트리 고정(사용자 지정): " + allocated.size() + "노드 → " + format(current));
        // 고정 트리는 그 직업의 시작점에서 연결돼야 PoB 가 할당한다. 다른 직업 트리를 넘기면 대부분이
        // 연결되지 않아 조용히 버려지고(스탯 기여 0) 결과만 이상해진다 → 눈에 보이게 경고한다.
        Set<Integer> reachable = poeTreeGraphService.reachableFrom(classStart, allocatedWithStart);
        // 클러스터 생성 노드는 트리 그래프에 없어 항상 "도달 불가"로 나온다 — 주얼이 만든 노드라
        // 연결성은 PoB 가 서브그래프로 보장한다. 세면 매번 가짜 경고가 뜬다.
        int orphan =
            (int)
                allocated.stream()
                    .filter(id -> id < 0x10000)
                    .filter(id -> !reachable.contains(id))
                    .count();
        if (orphan > 0) {
          log(
              "⚠ 고정 트리 중 "
                  + orphan
                  + "노드가 "
                  + className
                  + " 시작점에서 연결되지 않아 계산에 반영되지 않습니다 (직업을 함께 지정했는지 확인)");
        }
      } else {
        Map<PoeTreeGraphService.TreeNode, Integer> candidateScores = new LinkedHashMap<>();
        for (PoeTreeGraphService.TreeNode node : poeTreeGraphService.searchCandidates()) {
          int score = score(node.stats(), keywords);
          if (score > 0) {
            candidateScores.put(node, score);
          }
        }
        log("트리 후보 노터블/키스톤: " + candidateScores.size() + "개");

        // 주얼 소켓 경로용으로 일부 예약.
        // (마스터리용 추가 예약도 재 봤지만 — 4점 예약 시 마스터리 4개 채택으로 잡 내부 이득 +43%,
        //  예약 없이 남는 점으로 1개 채택 시 +41% — 최종 DPS 는 2.44M vs 2.47M 로 차이가 없어 도입하지 않았다)
        int treeBudget = POINT_BUDGET - JEWEL_RESERVE;
        for (int round = 0; round < TREE_MAX_ROUNDS && points < treeBudget; round++) {
          record Reachable(
              PoeTreeGraphService.TreeNode node, List<Integer> path, double priority) {}
          List<Reachable> reachable = new ArrayList<>();
          for (Map.Entry<PoeTreeGraphService.TreeNode, Integer> entry :
              candidateScores.entrySet()) {
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
      }

      // ── 3) 주얼 소켓 greedy — 트리에 연결 가능한 소켓에 전역 유니크 주얼을 꽂는다 ──
      enterPhase("jewels");
      // 사용자가 트리 에디터에서 직접 꽂은 주얼을 먼저 확정한다(할당된 소켓만). 나머지 소켓은 아래 탐색이 채운다.
      for (Map.Entry<Integer, String> fixed : fixedJewels.entrySet()) {
        if (!allocated.contains(fixed.getKey())) {
          log("⚠ 지정 주얼 소켓 " + fixed.getKey() + " 이 트리에 없어 건너뜁니다");
          continue;
        }
        poeUniqueDataService
            .findBySlug(fixed.getValue())
            .ifPresent(
                unique -> {
                  jewels.put(fixed.getKey(), unique);
                  log("주얼 고정(사용자 지정): " + unique.name());
                });
      }
      // 타임리스는 반경 노드 변환 계산이 무거워(시드→노드 매핑 로드) 자동 탐색 풀에 넣으면 잡 전체가 느려진다.
      // 대신 uniqueItemText() 가 타임리스 문구를 붙여, **트리에서 직접 꽂거나 강제 장착할 때** 제대로 계산되게 했다.
      List<PoeUniqueItem> jewelCandidates = globalJewelCandidates(keywords);
      if (!jewelCandidates.isEmpty()) {
        log("주얼 후보 " + jewelCandidates.size() + "개 (자동 탐색; 타임리스는 소켓/강제 장착 시 반영)");
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
        int jewelBudget = 5; // 주얼은 포인트당 이득이 트리 말단보다 커(실측 소켓당 +13~14%) 상한을 5로
        for (SocketPath socketPath : reachableSockets) {
          if (jewels.size() >= jewelBudget || points + socketPath.path().size() > POINT_BUDGET) {
            break;
          }
          // 사용자가 직접 꽂은 소켓은 탐색 대상에서 뺀다 — 안 그러면 바로 아래에서 덮어써 고정이 무의미해진다
          if (fixedJewels.containsKey(socketPath.socketId())) {
            continue;
          }
          // 같은 유니크 주얼은 게임의 "Limited to: N" 만큼만 장착할 수 있다 — 이미 채운 수를 빼고 후보를 만든다.
          // (안 걸면 최적화기가 같은 주얼을 소켓마다 꽂아 실제로는 불가능한 빌드가 나온다 — 실측 Dissolution 4개)
          Map<String, Long> used =
              jewels.values().stream()
                  .collect(
                      java.util.stream.Collectors.groupingBy(
                          PoeUniqueItem::name, java.util.stream.Collectors.counting()));
          List<PoeUniqueItem> available =
              jewelCandidates.stream()
                  .filter(j -> used.getOrDefault(j.name(), 0L) < jewelLimit(j.name()))
                  .toList();
          if (available.isEmpty()) {
            continue;
          }
          // 이 소켓에 후보 주얼들을 꽂아 평가 (경로도 함께 할당)
          Map<PoeUniqueItem, Double> results =
              evalBatch(
                  executor,
                  available,
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
      enterPhase("items");
      // 무기를 마지막에 본다 — 첫 슬롯에서 평가하면 아직 장비가 없어 속성이 낮고, 요구치 검사에 걸려
      // 멀쩡한 무기가 전부 탈락한다(실측: 민첩 74 시점에 민첩 76 요구 베이스 전멸).
      List<Slot> slotOrder = new ArrayList<>(List.of(Slot.values()));
      slotOrder.remove(Slot.WEAPON);
      slotOrder.add(Slot.WEAPON);
      // 무기 뒤에 보조장비를 한 번 더 본다 — 활은 무기가 정해진 뒤에야 **화살통**이 후보가 되기 때문이다.
      // (첫 순회 때는 아직 무기가 없어 방패만 후보였고, 활 빌드는 보조장비 슬롯이 통째로 비어 있었다)
      slotOrder.add(Slot.OFFHAND);
      for (Slot slot : slotOrder) {
        if (items.containsKey(slot)) {
          continue; // 강제 장착 유니크가 이미 점유한 슬롯 — 탐색 생략(고정)
        }
        if (slot == Slot.WEAPON && "ehp".equals(objective)) {
          continue; // 무기 고유는 EHP 에 기여하지 않음 — 표준 무기 유지
        }
        List<Equipped> slotCandidates = new ArrayList<>();
        List<PoeUniqueItem> uniqueCandidates = itemCandidates(slot, gem, keywords, items);
        for (PoeUniqueItem unique : uniqueCandidates) {
          slotCandidates.add(Equipped.ofUnique(unique));
        }
        if (slot == Slot.WEAPON || slot == Slot.OFFHAND) {
          // 무기/보조장비는 젬·현재 무기에 따라 후보 종류가 달라진다 — 무엇이 후보였는지 남긴다
          long sceptres =
              uniqueCandidates.stream()
                  .filter(u -> u.baseType() != null && u.baseType().contains("Sceptre"))
                  .count();
          log(
              slot.ko
                  + " 후보 "
                  + uniqueCandidates.size()
                  + "개"
                  + (sceptres > 0 ? " (셉터 " + sceptres + ")" : ""));
        }
        RareItem rare = null;
        if (slot == Slot.WEAPON) {
          // 무기는 베이스마다 기본 피해/요구치가 달라 하나로 고정하면 안 된다 — 상위 베이스들을 후보로 깔고
          // 실제 속성으로 못 드는 것은 검증기가 걸러낸다(예: 카루이 대도끼 민첩 43 vs 바알 도끼 76).
          String category =
              (gem.tags() != null && gem.tags().contains("Attack"))
                  ? "weaponAttack"
                  : "weaponSpell";
          for (PoeBaseItem base : weaponBaseCandidates(gem, 6, items.get(Slot.OFFHAND))) {
            RareItem weaponRare = craftRare(category, base.name(), keywords, 0.0, false);
            if (weaponRare != null) {
              slotCandidates.add(Equipped.ofRare(weaponRare));
            }
          }
        } else {
          // 방어 변형(방어도/회피/ES)은 직업 주 속성 휴리스틱으로 하나만 만든다.
          // ⚠ 세 변형을 모두 후보로 깔아 실측 선택하게 해 봤으나 **그리디 경로 의존**으로 결과가 나빠졌다
          //   (위치 EHP 964,409 → 729,640). 후보가 늘면 초반 슬롯 선택이 바뀌고 뒤 슬롯 조합이 악화된다.
          rare = craftRare(slot, gem, keywords, 0.0);
          if (rare != null) {
            slotCandidates.add(Equipped.ofRare(rare));
          }
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
                  String xml =
                      buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          trial,
                          jewels);
                  // 최종 빌드엔 도유가 얹혀 있다 — 예산 축에서 빼면 "티어를 낮춰서" 가 아니라
                  // "도유가 빠져서" 낮아진 값이 섞여 곡선이 오염된다
                  AnointPick anoint = currentAnoint;
                  return anoint != null ? withAnoint(xml, anoint.name()) : xml;
                },
                objectiveKey,
                // 그 장비를 낀 상태의 실제 속성으로 요구치를 판정한다(장비가 주는 속성까지 포함되므로 순환이 풀린다)
                this::meetsRequirements);
        Map.Entry<Equipped, Double> best =
            results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (best != null && best.getValue() > current * 1.002) {
          // 양손 무기 ↔ 보조장비는 게임에서 동시 장착이 불가능하다. PoB 는 조용히 무시할 뿐이라
          // 그대로 두면 결과 화면에 "있지만 아무 일도 안 하는 방패"가 남는다.
          if (slot == Slot.OFFHAND && offhandBlocked(items)) {
            log("보조장비 건너뜀(활이 아닌 양손 무기 장착 중): " + equippedLabel(best.getKey()));
            continue;
          }
          items.put(slot, best.getKey());
          if (slot == Slot.WEAPON && offhandBlocked(items) && items.containsKey(Slot.OFFHAND)) {
            log("보조장비 해제(양손 무기 채택): " + equippedLabel(items.remove(Slot.OFFHAND)));
          }
          current = best.getValue();
          log("장비 채택: " + slot.ko + " = " + equippedLabel(best.getKey()) + " → " + format(current));
        }
      }

      // ── 4a') 속성 요구치 보정 — 부족하면 레어 접미어를 속성 모드로 교체/추가 ──
      // 아이템 단계 검증기는 자동 후보만 거른다 — **강제 장착 유니크**(공허 충전기 지능 245 등)는 사용자
      // 지정이라 못 거르고, 경고만 남던 마지막 구멍. 실제 게임의 해법(장비에 +속성 접미어)을 그대로 쓴다.
      // 모든 슬롯이 유니크라 붙일 레어가 없으면 포기하고 기존 경고 경로에 맡긴다(정직한 실패).
      repairAttributeShortfalls(
          items, gem, supports, className, ascendancy, ascendancyNodes, allocated, jewels);

      // ── 4b) 오라/헤럴드 greedy — 예약형 오라를 2번째 스킬 그룹으로 추가(방어+공격 모두 후보) ──
      // greedy 가 현재 목표에 이득 되는 오라만 채택: dps/balanced=데미지 오라, ehp=방어 오라.
      // ⚠️ 반드시 최종 빌드와 동일 컨텍스트(주얼 포함)로 평가해야 한다 — "화염의 주문" 같은 주얼은
      //    오라/헤럴드 개수에 비례해 데미지를 주므로, 주얼 없이 평가하면 오라가 손해로 보여 미채택됨.
      {
        enterPhase("auras");
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
              // 부족 마나 = 미예약 마나가 음수로 내려간 만큼(결과 화면에서 사유 설명)
              blockedAuraShortfall.put(cand.getKey(), (int) Math.ceil(-unreserved));
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

      // ── 4) 마스터리 효과 greedy — 찍은 마스터리 노드마다 효과 하나를 고른다 ──
      // 마스터리는 **효과를 골라야** 스탯이 붙는다(PoB Spec masteryEffects). 트리 greedy 는 노드만 찍으므로
      // 이 단계가 없으면 자동 탐색 트리의 마스터리가 전부 빈 껍데기로 평가된다(사용자 지정 트리만 효과를 갖고 있었다).
      // 문신 앞에 두는 이유: 룬 접합(마스터리 문신)이 "효과 있는 마스터리"와 제대로 비교돼야 교체 판단이 맞다.
      {
        java.util.function.Predicate<Integer> hasEffects =
            id -> {
              PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(id);
              return node != null
                  && node.masteryEffects() != null
                  && !node.masteryEffects().isEmpty();
            };
        List<Integer> masteryNodes =
            new ArrayList<>(
                allocated.stream()
                    .filter(id -> !fixedMasteries.containsKey(id)) // 트리 에디터에서 고른 것은 존중
                    .filter(hasEffects)
                    .toList());
        // 트리 greedy 는 노터블/키스톤만 노린다(searchCandidates) — **마스터리는 아예 후보에 없다**.
        // 그런데 마스터리 하나가 "양손 적중 피해 60% 증가" 급이라 1포인트 대비 이득이 노터블보다 큰 경우가 많다.
        // 할당 집합에 인접한 미할당 마스터리를 키워드 점수 상위로 추려 후보에 넣는다(효과까지 함께 평가).
        Set<Integer> newMasteries = new LinkedHashSet<>();
        if (points < POINT_BUDGET) {
          record Candidate(int id, int score) {}
          List<Candidate> scored = new ArrayList<>();
          for (int id : new ArrayList<>(allocated)) {
            for (int neighbor : poeTreeGraphService.neighbors(id)) {
              if (allocated.contains(neighbor)
                  || newMasteries.contains(neighbor)
                  || !hasEffects.test(neighbor)) {
                continue;
              }
              PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(neighbor);
              int best =
                  node.masteryEffects().stream()
                      .mapToInt(e -> score(e.stats(), keywords))
                      .max()
                      .orElse(0);
              newMasteries.add(neighbor);
              scored.add(new Candidate(neighbor, best));
            }
          }
          // 키워드 점수 높은 것부터, 예산 안에서 상한만큼
          scored.sort(java.util.Comparator.comparingInt(Candidate::score).reversed());
          newMasteries.clear();
          for (Candidate candidate : scored) {
            if (newMasteries.size() >= MASTERY_MAX_NEW
                || points + newMasteries.size() >= POINT_BUDGET) {
              break;
            }
            newMasteries.add(candidate.id());
          }
          masteryNodes.addAll(newMasteries);
        }
        if (!masteryNodes.isEmpty()) {
          enterPhase("masteries");
          phaseDone.set(0);
          log(
              "마스터리 후보 "
                  + masteryNodes.size()
                  + "개(효과 미선택 "
                  + (masteryNodes.size() - newMasteries.size())
                  + ", 신규 할당 후보 "
                  + newMasteries.size()
                  + ") · 현재 "
                  + format(current));
          for (int nodeId : masteryNodes) {
            PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(nodeId);
            // 신규 후보는 **노드 할당(1포인트)까지 포함**해 평가한다 — 이미 찍은 노드는 효과만 본다
            boolean isNew = newMasteries.contains(nodeId);
            if (isNew && points >= POINT_BUDGET) {
              continue;
            }
            Set<Integer> trialNodes = allocated;
            if (isNew) {
              trialNodes = new LinkedHashSet<>(allocated);
              trialNodes.add(nodeId);
            }
            // XML 조립은 공유 필드(fixedMasteries)를 쓰므로 **메인 스레드에서 미리** 만들어 둔다
            // (evalBatch 는 워커 스레드에서 xmlFor 를 호출한다 — 거기서 필드를 바꾸면 경쟁 상태)
            Map<Integer, String> xmlByEffect = new LinkedHashMap<>();
            Map<Integer, Integer> saved = fixedMasteries;
            for (PoeTreeGraphService.MasteryEffect effect : node.masteryEffects()) {
              Map<Integer, Integer> trial = new LinkedHashMap<>(saved);
              trial.put(nodeId, effect.id());
              fixedMasteries = trial;
              xmlByEffect.put(
                  effect.id(),
                  buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      trialNodes,
                      items,
                      jewels));
            }
            fixedMasteries = saved;
            List<Integer> effectIds = List.copyOf(xmlByEffect.keySet());
            Map<Integer, Double> results =
                evalBatch(executor, effectIds, xmlByEffect::get, objectiveKey);
            Map.Entry<Integer, Double> best =
                results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            // 이미 찍은 노드의 효과는 공짜라 조금이라도 나으면 채택. 신규 할당은 1포인트를 쓰니 문턱(+0.3%)을 둔다.
            double threshold = isNew ? current * 1.003 : current;
            if (best != null && best.getValue() > 0 && best.getValue() > threshold) {
              Map<Integer, Integer> merged = new LinkedHashMap<>(fixedMasteries);
              merged.put(nodeId, best.getKey());
              fixedMasteries = merged;
              if (isNew) {
                allocated.add(nodeId);
                points++;
              }
              current = best.getValue();
              PoeTreeGraphService.MasteryEffect chosen =
                  node.masteryEffects().stream()
                      .filter(e -> e.id() == best.getKey())
                      .findFirst()
                      .orElse(null);
              String effectText =
                  chosen == null
                      ? String.valueOf(best.getKey())
                      : (chosen.statsKo() != null && !chosen.statsKo().isEmpty()
                          ? chosen.statsKo().get(0)
                          : chosen.stats().isEmpty() ? "" : chosen.stats().get(0));
              log(
                  (isNew ? "마스터리 신규(+1pt): " : "마스터리: ")
                      + (node.nameKo() != null ? node.nameKo() : node.name())
                      + " → "
                      + effectText
                      + " → "
                      + format(current));
            } else if (!isNew) {
              // 방어 마스터리는 DPS 목표에서 지표가 안 움직인다 — 게임에선 효과 선택이 공짜라
              // 사용자가 트리 에디터에서 직접 고르면 되므로, 비워둔 사실을 알려 준다.
              log("마스터리 미선택(목표 이득 없음): " + (node.nameKo() != null ? node.nameKo() : node.name()));
            } else {
              // 신규 후보 탈락도 수치로 남긴다 — "시험은 했는데 얼마나 모자랐나"가 안 보이면
              // 문턱(+0.3%)이 과한지, 후보 선정이 빗나갔는지 판단할 수 없다.
              log(
                  "마스터리 신규 탈락: "
                      + (node.nameKo() != null ? node.nameKo() : node.name())
                      + " → 최고 "
                      + (best == null || best.getValue() <= 0 ? "평가불가" : format(best.getValue()))
                      + " (현재 "
                      + format(current)
                      + ", 문턱 +0.3%)");
            }
          }
        }
      }

      // ── 4) 문신 greedy — 할당한 소형 속성 패시브를 문신 노드로 교체 ──
      // 게임에선 문신이 그 패시브를 **통째로 갈아끼운다**(스탯 추가가 아니다). 그래서 손해일 수도 있고,
      // 반경 주얼(붉은 악몽 등)이 꽂혀 있으면 반경 안 저항 문신이 방어 확률로 변환돼 큰 이득이 되기도 한다.
      // 아이템/오라 다음에 두는 이유: 저항·속성이 확정돼야 "이 문신이 실제로 이득인지"가 제대로 나온다.
      if (poeTattooDataService.hasData()) {
        enterPhase("tattoos");
        phaseDone.set(0);
        // 문신은 속성 패시브를 **통째로 교체**해 지능/힘/민첩을 깎는다 — 아이템 단계에서 요구치를
        // 통과한 장비(공허 충전기 지능 245 등)가 문신 뒤 장착 불가가 되는 실사고가 났다.
        // 단계 시작 시점에 요구치가 충족돼 있을 때만 강제한다(강제 장착 유니크로 이미 미충족이면
        // 모든 후보가 탈락해 단계가 통째로 멎는 것을 막기 위함 — 그 경우는 기존 경고 경로가 알린다).
        boolean enforceItemReqs;
        {
          Map<String, Double> tattooBaseline =
              poePobEngineService.calculateValues(
                  buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      items,
                      jewels));
          evalCount.incrementAndGet();
          enforceItemReqs = allRequirementsMet(items, tattooBaseline);
          if (!enforceItemReqs) {
            log("문신 요구치 강제 꺼짐 — 단계 시작 시점에 이미 미충족 장비 존재(강제 장착 추정)");
          }
        }
        final java.util.function.BiPredicate<Object, Map<String, Double>> reqValidator =
            enforceItemReqs ? (candidate, values) -> allRequirementsMet(items, values) : null;
        // 후보 노드 묶기 — **반경 주얼 안쪽을 먼저 따로** 본다.
        // 반경 변환(붉은 악몽: 반경 내 저항 패시브 → 막기 확률)은 그 반경 안에서만 이득이라,
        // 트리 전체에 한 종류를 바르는 방식으론 "전체로는 손해, 반경 안에선 이득"인 저항 문신이 영영 안 뽑힌다.
        // 키는 "라벨|속성" — 라벨은 로그용, 속성은 후보 풀 선택용(힘 문신은 힘 소형에만).
        Map<String, List<Integer>> tattooTargets = new LinkedHashMap<>();
        Set<Integer> radiusCovered = new LinkedHashSet<>();
        // 이번 잡에서 자동으로 새긴 노드 — 아래 혼합(스왑) 패스의 대상(사용자 지정은 건드리지 않는다)
        List<Integer> autoInked = new ArrayList<>();
        for (Map.Entry<Integer, PoeUniqueItem> socketed : jewels.entrySet()) {
          double radius = jewelRadiusValue(socketed.getValue().radius());
          if (radius <= 0 || !allocated.contains(socketed.getKey())) {
            continue;
          }
          String label =
              socketed.getValue().nameKo() != null
                  ? socketed.getValue().nameKo()
                  : socketed.getValue().name();
          for (int nodeId : poeTreeGraphService.nodesWithinRadius(socketed.getKey(), radius)) {
            if (!allocated.contains(nodeId) || fixedTattoos.containsKey(nodeId)) {
              continue;
            }
            String attribute = smallAttributeOf(poeTreeGraphService.node(nodeId));
            if (attribute != null && radiusCovered.add(nodeId)) {
              tattooTargets
                  .computeIfAbsent("반경:" + label + "|" + attribute, key -> new ArrayList<>())
                  .add(nodeId);
            }
          }
        }
        for (int nodeId : allocated) {
          if (fixedTattoos.containsKey(nodeId) || radiusCovered.contains(nodeId)) {
            continue;
          }
          String attribute = smallAttributeOf(poeTreeGraphService.node(nodeId));
          if (attribute != null) {
            tattooTargets.computeIfAbsent("전체|" + attribute, key -> new ArrayList<>()).add(nodeId);
          }
        }
        phaseTotal =
            tattooTargets.keySet().stream()
                .mapToInt(
                    key ->
                        poeTattooDataService
                            .candidates("normal", key.substring(key.indexOf('|') + 1))
                            .size())
                .sum();
        if (!tattooTargets.isEmpty()) {
          log(
              "문신 후보 패시브 "
                  + tattooTargets.values().stream().mapToInt(List::size).sum()
                  + "개("
                  + tattooTargets.entrySet().stream()
                      .map(e -> e.getKey() + " " + e.getValue().size())
                      .collect(java.util.stream.Collectors.joining(", "))
                  + ") · 현재 "
                  + format(current));
        }
        for (Map.Entry<String, List<Integer>> group : tattooTargets.entrySet()) {
          // 발동형(트리거) 문신은 소환수 스킬을 물고 들어와 평가가 불안정하다 — 스탯형만 본다
          String groupAttribute = group.getKey().substring(group.getKey().indexOf('|') + 1);
          String groupLabel = group.getKey().substring(0, group.getKey().indexOf('|'));
          List<PoeTattooDataService.Tattoo> pool =
              poeTattooDataService.candidates("normal", groupAttribute).stream()
                  .filter(t -> !t.stats().isEmpty())
                  .filter(t -> t.stats().stream().noneMatch(line -> line.startsWith("Trigger ")))
                  .toList();
          if (pool.isEmpty()) {
            continue;
          }
          // 장착 한도("Limited to 1 …")가 그룹보다 작으면 남는 노드가 맨몸으로 남는다 —
          // 채택할 때마다 새긴 자리를 빼고 **남은 노드로 반복**해 2등 문신까지 섞는다(혼합의 실체).
          List<Integer> remaining = new ArrayList<>(group.getValue());
          for (int round = 0; round < 4 && !remaining.isEmpty(); round++) {
            // 이미 새겨진 문신의 남은 한도만큼만 시험(같은 문신을 한도 초과로 또 고르는 낭비 방지)
            Map<String, Long> usedCounts =
                fixedTattoos.values().stream()
                    .collect(
                        java.util.stream.Collectors.groupingBy(
                            dn -> dn, java.util.stream.Collectors.counting()));
            final List<Integer> targets = List.copyOf(remaining);
            List<PoeTattooDataService.Tattoo> roundPool =
                pool.stream()
                    .filter(t -> usedCounts.getOrDefault(t.dn(), 0L) < tattooLimit(t))
                    .toList();
            if (roundPool.isEmpty()) {
              break;
            }
            Map<PoeTattooDataService.Tattoo, Double> results =
                evalBatch(
                    executor,
                    roundPool,
                    tattoo -> {
                      // 장착 한도와 연결 수 규칙을 모두 지키는 자리에만 새긴다
                      Map<Integer, String> trial = new LinkedHashMap<>();
                      for (int nodeId : tattooSpots(tattoo, targets)) {
                        trial.put(nodeId, tattoo.dn());
                      }
                      return withTattoos(
                          buildXml(
                              gem,
                              supports,
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              items,
                              jewels),
                          trial,
                          allocated);
                    },
                    objectiveKey,
                    // 이 문신 조합으로 장비 요구치가 깨지면(속성 패시브 교체) 이득이 커도 탈락
                    reqValidator == null ? null : reqValidator::test);
            Map.Entry<PoeTattooDataService.Tattoo, Double> best =
                results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            if (best == null || best.getValue() <= current * 1.003) {
              break; // 더 이득이 없으면 이 그룹은 끝
            }
            PoeTattooDataService.Tattoo tattoo = best.getKey();
            List<Integer> spots = tattooSpots(tattoo, targets);
            Map<Integer, String> merged = new LinkedHashMap<>(fixedTattoos);
            for (int nodeId : spots) {
              merged.put(nodeId, tattoo.dn());
            }
            fixedTattoos = merged;
            autoInked.addAll(spots);
            remaining.removeAll(spots);
            current = best.getValue();
            log(
                "문신["
                    + groupLabel
                    + "]: "
                    + (tattoo.nameKo() != null ? tattoo.nameKo() : tattoo.dn())
                    + " ×"
                    + spots.size()
                    + (remaining.isEmpty() ? "" : " (남은 자리 " + remaining.size() + ")")
                    + " → "
                    + format(current));
          }
        }

        // ── 혼합(스왑) 패스 — 그룹 그리디는 "그룹당 한 종류"라, 노드별로 다른 문신이 더 나은 조합을 놓친다.
        // 자동으로 새긴 각 노드에 대해 (다른 문신 전부 + 제거) 를 시험해 이득이면 바꾼다. 1라운드만(비용 통제).
        if (!autoInked.isEmpty()) {
          double beforeSwap = current;
          // 같은 속성의 소형 노드는 스탯이 전부 동일(+10)이라 스왑 평가도 동일하다 —
          // (속성|현재 문신|연결선 수 계층) 시그니처당 대표 1회만 시험해 평가 수를 줄인다.
          Set<String> swapSeen = new LinkedHashSet<>();
          for (int nodeId : autoInked) {
            String attribute = smallAttributeOf(poeTreeGraphService.node(nodeId));
            if (attribute == null) {
              continue;
            }
            int linked = poeTreeGraphService.neighbors(nodeId).size();
            String signature =
                attribute
                    + "|"
                    + fixedTattoos.get(nodeId)
                    + "|"
                    + (linked >= 7 ? "hub" : linked <= 1 ? "leaf" : "mid");
            if (!swapSeen.add(signature)) {
              continue;
            }
            List<String> variants = new ArrayList<>();
            variants.add(""); // 빈 문자열 = 문신 제거(원래 패시브 복원)
            for (PoeTattooDataService.Tattoo tattoo :
                poeTattooDataService.candidates("normal", attribute)) {
              if (tattoo.stats().isEmpty()
                  || tattoo.stats().stream().anyMatch(line -> line.startsWith("Trigger "))
                  || tattoo.dn().equals(fixedTattoos.get(nodeId))
                  || !tattooFits(tattoo, nodeId)) {
                continue;
              }
              // 장착 한도 — 이미 다른 노드에 한도만큼 새겨져 있으면 이 노드로는 못 바꾼다
              long used =
                  fixedTattoos.entrySet().stream()
                      .filter(e -> e.getKey() != nodeId && e.getValue().equals(tattoo.dn()))
                      .count();
              if (used >= tattooLimit(tattoo)) {
                continue;
              }
              variants.add(tattoo.dn());
            }
            // 잡 경로 XML(buildXml)이 fixedTattoos 를 이미 싣는다 — 스왑 시험 동안 잠시 비우고 trial 만 넣는다
            Map<Integer, String> savedTattoos = fixedTattoos;
            Map<String, Double> swapResults;
            try {
              fixedTattoos = Map.of();
              String baseXml =
                  buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      items,
                      jewels);
              swapResults =
                  evalBatch(
                      executor,
                      variants,
                      dn -> {
                        Map<Integer, String> trial = new LinkedHashMap<>(savedTattoos);
                        if (dn.isEmpty()) {
                          trial.remove(nodeId);
                        } else {
                          trial.put(nodeId, dn);
                        }
                        return withTattoos(baseXml, trial, allocated);
                      },
                      objectiveKey,
                      reqValidator == null ? null : reqValidator::test);
            } finally {
              fixedTattoos = savedTattoos;
            }
            Map.Entry<String, Double> bestSwap =
                swapResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            if (bestSwap != null && bestSwap.getValue() > current * 1.003) {
              Map<Integer, String> merged = new LinkedHashMap<>(fixedTattoos);
              String dn = bestSwap.getKey();
              if (dn.isEmpty()) {
                merged.remove(nodeId);
              } else {
                merged.put(nodeId, dn);
              }
              fixedTattoos = merged;
              current = bestSwap.getValue();
              PoeTreeGraphService.TreeNode spot = poeTreeGraphService.node(nodeId);
              log(
                  "문신 스왑: "
                      + (spot != null && spot.nameKo() != null
                          ? spot.nameKo()
                          : String.valueOf(nodeId))
                      + " → "
                      + (dn.isEmpty()
                          ? "제거"
                          : poeTattooDataService
                              .findByDn(dn)
                              .map(t -> t.nameKo() != null ? t.nameKo() : t.dn())
                              .orElse(dn))
                      + " → "
                      + format(current));
            }
          }
          if (current > beforeSwap) {
            log("혼합 패스 이득: " + format(beforeSwap) + " → " + format(current));
          }
        }

        // 노터블/키스톤 문신 — 대부분 "Limited to 1" 이라 **어느 노드에 새길지**까지 골라야 한다.
        // (소형처럼 한 종류를 전부에 바르는 방식으론 노드 선택이 첫 번째로 고정돼 버린다)
        // 마스터리 문신(룬 접합)도 같은 방식 — 마스터리 효과를 버리고 룬 접합 모드를 얻는 교환이라
        // (실측: 생명력 마스터리 +30 이 사라지고 룬 접합이 붙는다) 이득일 때만 그리디가 채택한다.
        for (String nodeType : List.of("notable", "keystone", "mastery")) {
          List<Integer> spots =
              allocated.stream()
                  .filter(id -> !fixedTattoos.containsKey(id))
                  .filter(
                      id -> {
                        PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(id);
                        // 전직 노드는 문신을 새길 수 없다(전용 승천 문신은 별도 종류)
                        return node != null
                            && nodeType.equals(node.type())
                            && node.ascendancy() == null;
                      })
                  .limit(TATTOO_MAX_SPOTS)
                  .toList();
          List<PoeTattooDataService.Tattoo> pool =
              poeTattooDataService.candidates(nodeType, null).stream()
                  .filter(t -> !t.stats().isEmpty())
                  .filter(t -> t.stats().stream().noneMatch(line -> line.startsWith("Trigger ")))
                  .toList();
          if (spots.isEmpty() || pool.isEmpty()) {
            continue;
          }
          record Ink(int nodeId, PoeTattooDataService.Tattoo tattoo) {}
          List<Ink> trials = new ArrayList<>();
          for (PoeTattooDataService.Tattoo tattoo : pool) {
            for (int nodeId : spots) {
              if (tattooFits(tattoo, nodeId)) {
                trials.add(new Ink(nodeId, tattoo));
              }
            }
          }
          if (trials.isEmpty()) {
            continue;
          }
          Map<Ink, Double> results =
              evalBatch(
                  executor,
                  trials,
                  ink ->
                      withTattoos(
                          buildXml(
                              gem,
                              supports,
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              items,
                              jewels),
                          Map.of(ink.nodeId(), ink.tattoo().dn()),
                          allocated),
                  objectiveKey);
          Map.Entry<Ink, Double> best =
              results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (best != null && best.getValue() > current * 1.003) {
            Map<Integer, String> merged = new LinkedHashMap<>(fixedTattoos);
            merged.put(best.getKey().nodeId(), best.getKey().tattoo().dn());
            fixedTattoos = merged;
            current = best.getValue();
            PoeTreeGraphService.TreeNode spot = poeTreeGraphService.node(best.getKey().nodeId());
            PoeTattooDataService.Tattoo tattoo = best.getKey().tattoo();
            log(
                "문신: "
                    + (tattoo.nameKo() != null ? tattoo.nameKo() : tattoo.dn())
                    + " → "
                    + (spot != null && spot.nameKo() != null
                        ? spot.nameKo()
                        : String.valueOf(best.getKey().nodeId()))
                    + " 자리 → "
                    + format(current));
          }
        }
      }

      // ── 4) 레어 슬롯 티어 비교 — 채택된 레어를 T1/중/하 티어로 재계산 ──
      enterPhase("tiers");
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
                  String xml =
                      buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          trial,
                          jewels);
                  // 최종 빌드엔 도유가 얹혀 있다 — 예산 축에서 빼면 "티어를 낮춰서" 가 아니라
                  // "도유가 빠져서" 낮아진 값이 섞여 곡선이 오염된다
                  AnointPick anoint = currentAnoint;
                  return anoint != null ? withAnoint(xml, anoint.name()) : xml;
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
      enterPhase("finish");
      phaseDone.set(0);
      phaseTotal = 0;
      // 무기 합법화 패스 — 무기가 목표(특히 EHP)에 기여하지 않으면 그리디는 무기를 끝내 채택하지 않고
      // XML 의 fallback 표준 무기가 남는데, 그건 요구 속성을 무시하므로 결과가 조용히 불법이 된다.
      // 최종 속성으로 "실제로 들 수 있는" 최고 베이스를 골라 채워 넣는다.
      if (!items.containsKey(Slot.WEAPON)) {
        Map<String, Double> preValues =
            poePobEngineService.calculateValues(
                buildXml(
                    gem,
                    supports,
                    className,
                    ascendancy,
                    ascendancyNodes,
                    allocated,
                    items,
                    jewels));
        evalCount.incrementAndGet();
        String weaponCategory =
            (gem.tags() != null && gem.tags().contains("Attack")) ? "weaponAttack" : "weaponSpell";
        for (PoeBaseItem base : weaponBaseCandidates(gem, 12, items.get(Slot.OFFHAND))) {
          if (!meets(base.reqStr(), preValues.get("Str"))
              || !meets(base.reqDex(), preValues.get("Dex"))
              || !meets(base.reqInt(), preValues.get("Int"))) {
            continue;
          }
          // EHP 목표처럼 키워드가 방어 위주면 무기 모드가 하나도 안 잡혀 craftRare 가 null 을 준다.
          // 그래도 "모드 없는 맨 베이스"가 불법 무기보다 낫다 — 빈 모드로라도 합법 무기를 채운다.
          RareItem legalWeapon = craftRare(weaponCategory, base.name(), keywords, 0.0, false);
          if (legalWeapon == null) {
            legalWeapon = new RareItem(base.name(), List.of(), 0.0);
          }
          items.put(Slot.WEAPON, Equipped.ofRare(legalWeapon));
          log("무기 합법화: " + base.name() + " (요구치 충족 베이스로 교체)");
          break;
        }
      }
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

      // ── 아뮬렛 도유 ──
      // 실제 빌드는 예외 없이 아뮬렛에 도유를 건다(노터블 하나를 공짜로 얻는 셈) — 빼고 계산하면 실전보다 약하게 나온다.
      // 후보 전부를 **실제로 평가해서** 고른다. 키워드 점수는 조건절("방패를 들고 있는 동안")을 못 읽고,
      // 컷은 점수 밖의 진짜 1위(오라 효과 등 간접 기여)를 놓친다(둘 다 실측). 완성 XML 에 한 줄만 얹어
      // 재평가하므로 탐색 경로가 흔들리지 않는다.
      Integer pinnedAnoint = fixedAnoint;
      if (pinnedAnoint != null) {
        // 트리 에디터에서 고른 도유 — 자동 스윕 대신 고정(문신/마스터리와 같은 존중 원칙)
        PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(pinnedAnoint);
        if (node != null && node.anoint() != null && !node.anoint().isEmpty()) {
          currentAnoint =
              new AnointPick(
                  node.id(),
                  node.name(),
                  node.nameKo() != null && !node.nameKo().isBlank() ? node.nameKo() : node.name());
          finalXml = withAnoint(finalXml, node.name());
          finalValues = poePobEngineService.calculateValues(finalXml);
          evalCount.incrementAndGet();
          log("아뮬렛 도유(사용자 고정): " + currentAnoint.nameKo() + " (" + node.name() + ")");
        } else {
          log("아뮬렛 도유(사용자 고정) 무시 — 도유 불가 노드 id " + pinnedAnoint);
          pinnedAnoint = null;
        }
      }
      List<PoeTreeGraphService.TreeNode> anointPool =
          pinnedAnoint != null ? List.of() : anointCandidates();
      if (!anointPool.isEmpty()) {
        enterPhase("anoint");
        final String baseXml = finalXml;
        Map<String, Double> anointResults =
            evalBatch(
                executor,
                anointPool.stream().map(PoeTreeGraphService.TreeNode::name).toList(),
                name -> withAnoint(baseXml, name),
                objectiveKey);
        double anointBase = displayMetric(finalValues, objective);
        String bestName = null;
        double bestValue = anointBase;
        for (PoeTreeGraphService.TreeNode node : anointPool) { // id 오름차순 순회 — 동점은 id 낮은 쪽(결정성)
          Double value = anointResults.get(node.name());
          if (value != null && value > bestValue) {
            bestValue = value;
            bestName = node.name();
          }
        }
        if (bestName != null) {
          final String picked = bestName;
          PoeTreeGraphService.TreeNode node =
              anointPool.stream().filter(n -> n.name().equals(picked)).findFirst().orElseThrow();
          currentAnoint =
              new AnointPick(
                  node.id(),
                  node.name(),
                  node.nameKo() != null && !node.nameKo().isBlank() ? node.nameKo() : node.name());
          finalXml = withAnoint(finalXml, bestName);
          finalValues = poePobEngineService.calculateValues(finalXml);
          evalCount.incrementAndGet();
          log(
              "아뮬렛 도유: "
                  + currentAnoint.nameKo()
                  + " ("
                  + bestName
                  + ") — "
                  + format(anointBase)
                  + " → "
                  + format(bestValue)
                  + " (후보 "
                  + anointPool.size()
                  + "개 평가)");
        } else {
          log("아뮬렛 도유: 후보 " + anointPool.size() + "개 모두 이득 없음 — 도유 없음");
        }
      }

      // ── 오라 예약 최종 검증 ──
      // 오라 단계 이후(마스터리·문신·도유 등)가 최대 마나/예약 효율을 바꿔 최종적으로 예약이 초과될 수 있다.
      // 인게임에서 못 띄우는 오라가 결과에 남지 않도록, 초과 시 **한계 이득이 가장 작은 마지막 채택분부터** 해제한다.
      {
        double unreservedFinal = finalValues.getOrDefault("ManaUnreserved", 0d);
        while (unreservedFinal < MIN_UNRESERVED_MANA && !selectedAuras.isEmpty()) {
          PoeGem dropped = selectedAuras.remove(selectedAuras.size() - 1);
          log(
              "오라 최종 예약 초과 — 해제: "
                  + dropped.name()
                  + " (미예약 마나 "
                  + Math.round(unreservedFinal)
                  + ")");
          finalXml =
              buildXml(
                  gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels);
          if (currentAnoint != null) {
            finalXml = withAnoint(finalXml, currentAnoint.name());
          }
          finalValues = poePobEngineService.calculateValues(finalXml);
          evalCount.incrementAndGet();
          unreservedFinal = finalValues.getOrDefault("ManaUnreserved", 0d);
        }
      }

      // ── 어픽스 예산 축(현실적 제작) 재평가 ──
      // 기본 결과는 모든 레어 모드를 T1 로 가정한다(낙관적). 실제로는 보통 필수 옵션 2~4개만 T1 이고
      // 나머지는 중위 티어다. 레어를 전부 "필수 N T1 + 나머지 중위"로 바꿔 N∈{2,3,4} 을 병렬 재계산해,
      // "필수 옵션 수"에 따른 DPS 곡선을 사용자에게 보여준다(전부 T1 = finalValue = 상한).
      List<PoeOptimizeResult.AffixBudgetPoint> affixBudget = new ArrayList<>();
      boolean hasRare = items.values().stream().anyMatch(equipped -> !equipped.isUnique());
      if (hasRare) {
        enterPhase("budget");
        List<Integer> essentialCounts = List.of(2, 3, 4);
        Map<Integer, Double> budgetResults =
            evalBatch(
                executor,
                essentialCounts,
                essential -> {
                  Map<Slot, Equipped> trial = new EnumMap<>(items);
                  for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
                    if (!entry.getValue().isUnique()) {
                      trial.put(
                          entry.getKey(),
                          Equipped.ofRare(
                              budgetVariant(entry.getValue().rare(), essential, BUDGET_FILLER)));
                    }
                  }
                  String xml =
                      buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          trial,
                          jewels);
                  // 최종 빌드엔 도유가 얹혀 있다 — 예산 축에서 빼면 "티어를 낮춰서" 가 아니라
                  // "도유가 빠져서" 낮아진 값이 섞여 곡선이 오염된다
                  AnointPick anoint = currentAnoint;
                  return anoint != null ? withAnoint(xml, anoint.name()) : xml;
                },
                objectiveKey);
        for (int essential : essentialCounts) {
          Double value = budgetResults.get(essential);
          if (value != null && value > 0) {
            affixBudget.add(new PoeOptimizeResult.AffixBudgetPoint(essential, format(value)));
          }
        }
        if (!affixBudget.isEmpty()) {
          log(
              "어픽스 예산 축: "
                  + affixBudget.stream()
                      .map(point -> "필수" + point.essentialCount() + "=" + point.value())
                      .collect(java.util.stream.Collectors.joining(" · "))
                  + " (전체 T1="
                  + format(displayMetric(finalValues, objective))
                  + ")");
        }
      }

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
      // 클러스터 주얼이 얹은 노터블은 트리 그래프에 없어(생성 노드) 위 루프에 안 걸린다 —
      // 목록에서 빠지면 "무슨 노터블을 쓰는 빌드인지"가 결과 화면에서 사라진다.
      for (ClusterSpec spec : fixedClusters) {
        notables.addAll(spec.notables());
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
              blockedAuraShortfall.entrySet().stream()
                  .map(
                      e ->
                          new PoeOptimizeResult.BlockedAura(
                              e.getKey().name(), e.getKey().nameKo(), e.getValue()))
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
              // 무기 유니크가 안 뽑히면 XML 에 표준 무기가 주입된다 — 목록에도 넣어야 화면과 실제 계산이 일치한다
              // (없으면 사용자는 "무기 없는 근접 빌드"로 오해한다)
              java.util.stream.Stream.concat(
                      items.entrySet().stream()
                          .map(entry -> itemPick(entry.getKey(), entry.getValue())),
                      standardWeaponPick(gem, items).stream())
                  .toList(),
              unmetRequirements(items, finalValues, standardWeapon(gem)),
              tierComparisons,
              scenarioMatrix,
              defenseHits,
              poePobEngineService.formatStats(finalValues),
              format(displayMetric(baselineValues, objective)),
              format(displayMetric(finalValues, objective)),
              affixBudget,
              encodePobCode(finalXml),
              System.currentTimeMillis() - startedAt,
              evalCount.get(),
              // 트리 링크(c=/j=)로 그대로 되돌아갈 수 있게 구성을 문자열로 남긴다
              serializeClusters(fixedClusters),
              jewels.entrySet().stream()
                  .map(entry -> entry.getKey() + ":" + entry.getValue().slug())
                  .collect(java.util.stream.Collectors.joining(",")),
              fixedTattoos.entrySet().stream()
                  .map(entry -> entry.getKey() + ":" + entry.getValue())
                  .collect(java.util.stream.Collectors.joining("|")),
              // 최종 XML 에 실린 마스터리 선택 그대로 — 트리 링크가 이걸 잃으면 표시≠실제가 된다
              fixedMasteries.entrySet().stream()
                  .filter(entry -> allocated.contains(entry.getKey()))
                  .map(entry -> entry.getKey() + ":" + entry.getValue())
                  .collect(java.util.stream.Collectors.joining(",")),
              masteryLabels(fixedMasteries, allocated),
              tattooLabels(fixedTattoos),
              // 트리 링크(an=)로 도유까지 되돌아가게 — 없으면 트리 화면 수치가 결과보다 약하게 나온다
              currentAnoint != null ? currentAnoint.nodeId() : null);

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
              + "회"
              + (evalFailures.get() > 0 ? ", 실패 " + evalFailures.get() + "회" : "")
              + ")");
      // 실패가 있었다면 결과를 그대로 믿으면 안 된다 — 실패한 후보는 점수 -1 로 탈락해 탐색 경로가 바뀐다
      if (evalFailures.get() > 0) {
        log(
            "⚠ 엔진 평가 "
                + evalFailures.get()
                + "회 실패 — 그만큼의 후보가 평가 없이 탈락했습니다 (첫 오류: "
                + firstEvalError
                + ")");
      }
      enterPhase(""); // 마지막 단계 소요 마감
      log("단계별 소요: " + phaseSummary());
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
    return evalBatch(executor, candidates, xmlFor, objectiveKey, null);
  }

  /**
   * @param validator 계산된 원시 스탯으로 후보를 걸러내는 검증기(null 이면 무검증). 장비 요구 속성처럼 <b>그 후보를 장착한 상태의 값</b>이 있어야
   *     판정 가능한 조건에 쓴다 — 이미 돌린 계산 결과를 재사용하므로 엔진 호출이 늘지 않는다. 탈락은 -1 로 표시.
   */
  private <T> Map<T, Double> evalBatch(
      ExecutorService executor,
      List<T> candidates,
      Function<T, String> xmlFor,
      String objectiveKey,
      java.util.function.BiPredicate<T, Map<String, Double>> validator) {
    phaseTotal = candidates.size();
    phaseDone.set(0);
    Map<T, Future<Double>> futures = new LinkedHashMap<>();
    for (T candidate : candidates) {
      futures.put(
          candidate,
          executor.submit(
              () -> {
                try {
                  Map<String, Double> values =
                      poePobEngineService.calculateValues(xmlFor.apply(candidate));
                  if (validator != null && !validator.test(candidate, values)) {
                    return -1d;
                  }
                  return objectiveOf(values, objectiveKey);
                } catch (Exception e) {
                  // 실패를 숨기지 않는다 — 몇 번, 왜 실패했는지 잡 로그로 드러낸다
                  evalFailures.incrementAndGet();
                  if (firstEvalError == null) {
                    firstEvalError = phase + " 단계: " + e;
                  }
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
    enterPhase("skill");
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
              + budget // 혈맹이 예산 일부를 가져가므로 상수가 아니라 실제 예산을 찍는다(6/8 로 보여 낭비로 오독했음)
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

  /** "12,34,56" → 노드 id 집합. 숫자가 아닌 토큰은 무시. */
  private Set<Integer> parseNodeIds(String csv) {
    if (csv == null || csv.isBlank()) {
      return Set.of();
    }
    Set<Integer> ids = new LinkedHashSet<>();
    for (String token : csv.split(",")) {
      String trimmed = token.trim();
      if (trimmed.matches("[0-9]+")) {
        ids.add(Integer.valueOf(trimmed));
      }
    }
    return ids;
  }

  /** "노드:효과,노드:효과" → 마스터리 맵. 같은 효과는 게임 규칙상 1회뿐이라 중복은 첫 것만 남긴다. */
  private Map<Integer, Integer> parseMasteries(String csv) {
    if (csv == null || csv.isBlank()) {
      return Map.of();
    }
    Map<Integer, Integer> picks = new LinkedHashMap<>();
    Set<Integer> seenEffects = new LinkedHashSet<>();
    for (String pair : csv.split(",")) {
      String[] kv = pair.trim().split(":");
      if (kv.length == 2
          && kv[0].matches("[0-9]+")
          && kv[1].matches("[0-9]+")
          && seenEffects.add(Integer.valueOf(kv[1]))) {
        picks.put(Integer.valueOf(kv[0]), Integer.valueOf(kv[1]));
      }
    }
    return picks;
  }

  /** "소켓노드:slug,..." → 주얼 맵. 없는 slug 는 잡 시작 시 조용히 걸러진다. */
  /** ClusterSpec 목록 → 트리 URL 의 c= 형식 문자열(가져오기/링크 왕복용). */
  private String serializeClusters(List<ClusterSpec> specs) {
    return specs.stream()
        .map(
            spec ->
                spec.socket()
                    + ":"
                    + spec.sizeName()
                    + ":"
                    + spec.nodeCount()
                    + ":"
                    + spec.skillKey()
                    + (spec.notables().isEmpty() && spec.socketCount() == 0
                        ? ""
                        : ":" + String.join("|", spec.notables()))
                    + (spec.socketCount() > 0 ? ":" + spec.socketCount() : ""))
        .collect(java.util.stream.Collectors.joining(","));
  }

  /** clusters = "소켓:크기:노드수:스킬키:노터블|노터블:소켓수,..." — 트리 에디터/URL 과 같은 형식. */
  private List<ClusterSpec> parseClusters(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    List<ClusterSpec> specs = new ArrayList<>();
    for (String entry : csv.split(",")) {
      String[] parts = entry.trim().split(":");
      if (parts.length < 3 || !parts[0].matches("[0-9]+") || !parts[2].matches("[0-9]+")) {
        continue;
      }
      List<String> notables =
          parts.length > 4 && !parts[4].isBlank()
              ? java.util.Arrays.stream(parts[4].split(java.util.regex.Pattern.quote("|")))
                  .map(String::trim)
                  .filter(n -> !n.isEmpty())
                  .toList()
              : List.of();
      specs.add(
          new ClusterSpec(
              Integer.parseInt(parts[0]),
              parts[1],
              Integer.parseInt(parts[2]),
              parts.length > 3 ? parts[3] : "",
              notables,
              parts.length > 5 && parts[5].matches("[0-9]+") ? Integer.parseInt(parts[5]) : 0));
    }
    return List.copyOf(specs);
  }

  private Map<Integer, String> parseJewels(String csv) {
    if (csv == null || csv.isBlank()) {
      return Map.of();
    }
    Map<Integer, String> picks = new LinkedHashMap<>();
    for (String pair : csv.split(",")) {
      String[] kv = pair.trim().split(":", 2);
      if (kv.length == 2 && kv[0].matches("[0-9]+") && !kv[1].isBlank()) {
        picks.put(Integer.valueOf(kv[0]), kv[1].trim());
      }
    }
    return picks;
  }

  /**
   * 문신을 새길 수 있는 소형 <b>속성</b> 패시브인지 — 맞으면 "Strength"/"Dexterity"/"Intelligence". 게임은 소형 패시브의 속성 종류까지
   * 구분한다(힘 소형엔 힘 문신). 속성이 아닌 소형(예: 피해 증가)엔 새길 문신이 없다.
   */
  private String smallAttributeOf(PoeTreeGraphService.TreeNode node) {
    if (node == null || !"normal".equals(node.type()) || node.stats() == null) {
      return null;
    }
    for (String line : node.stats()) {
      String trimmed = line.trim();
      if (trimmed.endsWith("to Strength")) {
        return "Strength";
      }
      if (trimmed.endsWith("to Dexterity")) {
        return "Dexterity";
      }
      if (trimmed.endsWith("to Intelligence")) {
        return "Intelligence";
      }
    }
    return null;
  }

  /**
   * 이 문신을 이 패시브에 새길 수 있는지 — 마캉가 계열처럼 <b>연결된 패시브 수</b> 조건이 붙은 문신이 11종 있다(7~8개 이상, 또는 1개 이하).
   *
   * <p>기준은 PoB 의 문신 목록 필터(`TreeTab.lua:854` {@code node.MinimumConnected <= numLinkedNodes})와 맞춘다 —
   * 거기서 쓰는 numLinkedNodes 는 <b>할당 여부와 무관한 그 노드의 연결선 수</b>다. 계산 엔진이 PoB 인 이상 판정도 PoB 기준이어야 화면과 수치가
   * 어긋나지 않는다. (조건부 스탯 "인접 8개 할당 시" 부분은 PoB 계산이 알아서 처리한다)
   */
  private boolean tattooFits(PoeTattooDataService.Tattoo tattoo, int nodeId) {
    if (tattoo.minConnected() <= 0 && tattoo.maxConnected() >= 100) {
      return true; // 대부분은 제약이 없다 — 이웃 세는 비용도 아낀다
    }
    int linked = poeTreeGraphService.neighbors(nodeId).size();
    return linked >= tattoo.minConnected() && linked <= tattoo.maxConnected();
  }

  /** 이 문신을 실제로 새길 노드들 — 연결 수 규칙에 맞고 장착 한도 안쪽인 자리만. */
  private List<Integer> tattooSpots(PoeTattooDataService.Tattoo tattoo, List<Integer> nodes) {
    return nodes.stream()
        .filter(nodeId -> tattooFits(tattoo, nodeId))
        .limit(tattooLimit(tattoo))
        .toList();
  }

  /** 반경 라벨 → 월드 단위 반경(3.16+ 트리 값). 반경 모드가 없는 주얼(라벨 없음)은 0. */
  private double jewelRadiusValue(String label) {
    if (label == null) {
      return 0;
    }
    return switch (label) {
      case "Small" -> 960;
      case "Medium" -> 1440;
      case "Large" -> 1800;
      case "Very Large" -> 2400;
      case "Massive" -> 2880;
      default -> 0;
    };
  }

  /** 표시용 마스터리 요약 — "마스터리명 — 고른 효과 첫 줄". 자동 채택된 효과가 결과 화면에 보여야 표시=실제다. */
  private List<String> masteryLabels(Map<Integer, Integer> picks, Set<Integer> allocated) {
    List<String> labels = new ArrayList<>();
    for (Map.Entry<Integer, Integer> entry : picks.entrySet()) {
      if (!allocated.contains(entry.getKey())) {
        continue;
      }
      PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(entry.getKey());
      if (node == null || node.masteryEffects() == null) {
        continue;
      }
      PoeTreeGraphService.MasteryEffect effect =
          node.masteryEffects().stream()
              .filter(e -> e.id() == entry.getValue())
              .findFirst()
              .orElse(null);
      if (effect == null) {
        continue;
      }
      String effectText =
          effect.statsKo() != null && !effect.statsKo().isEmpty()
              ? effect.statsKo().get(0)
              : effect.stats().isEmpty() ? "" : effect.stats().get(0);
      labels.add((node.nameKo() != null ? node.nameKo() : node.name()) + " — " + effectText);
    }
    return labels;
  }

  /** 표시용 문신 요약 — 같은 문신을 여러 패시브에 새기므로 "한글명 ×N" 으로 묶는다. */
  private List<String> tattooLabels(Map<Integer, String> picks) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String dn : picks.values()) {
      String label =
          poeTattooDataService
              .findByDn(dn)
              .map(t -> t.nameKo() != null ? t.nameKo() : t.dn())
              .orElse(dn);
      counts.merge(label, 1, Integer::sum);
    }
    return counts.entrySet().stream().map(e -> e.getKey() + " ×" + e.getValue()).toList();
  }

  /** 문신 종류별 장착 한도 — 스탯 줄의 "Limited to N …" 문구가 근거(없으면 소형 문신처럼 사실상 무제한). */
  private int tattooLimit(PoeTattooDataService.Tattoo tattoo) {
    for (String line : tattoo.stats()) {
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("^Limited to (\\d+) ").matcher(line.trim());
      if (matcher.find()) {
        return Integer.parseInt(matcher.group(1));
      }
    }
    return Integer.MAX_VALUE;
  }

  /** 문신 지정 파싱 — "노드:영문명" 을 '|'(권장, 이름에 공백이 있어서) 또는 ',' 로 이은 문자열. */
  private Map<Integer, String> parseTattoos(String text) {
    if (text == null || text.isBlank()) {
      return Map.of();
    }
    Map<Integer, String> picks = new LinkedHashMap<>();
    for (String pair : text.split("[|,]")) {
      String[] kv = pair.trim().split(":", 2);
      if (kv.length == 2 && kv[0].matches("[0-9]+") && !kv[1].isBlank()) {
        picks.put(Integer.valueOf(kv[0]), kv[1].trim());
      }
    }
    return picks;
  }

  /**
   * 유니크 주얼 장착 한도("Limited to: N"). 게임 데이터 추출본엔 이 값이 없어 PoB 소스의 {@code Data/Uniques/jewel.lua} 에서
   * 읽는다(엔진용으로 이미 받아둔 소스라 추가 의존이 없다). 목록에 없으면 1 — PoE 유니크 주얼은 사실상 전부 한도가 있다.
   */
  private final String pobSourceDir;

  private volatile Map<String, Integer> jewelLimits;

  private int jewelLimit(String name) {
    Map<String, Integer> limits = jewelLimits;
    if (limits == null) {
      limits = loadJewelLimits();
      jewelLimits = limits;
    }
    return limits.getOrDefault(name, 1);
  }

  private Map<String, Integer> loadJewelLimits() {
    Map<String, Integer> limits = new LinkedHashMap<>();
    java.nio.file.Path file =
        java.nio.file.Path.of(pobSourceDir, "src", "Data", "Uniques", "jewel.lua");
    if (!java.nio.file.Files.isReadable(file)) {
      logger.warn("주얼 한도 파일을 못 읽어 전부 1개로 제한합니다: {}", file);
      return limits;
    }
    try {
      String name = null;
      for (String line : java.nio.file.Files.readAllLines(file)) {
        String trimmed = line.trim();
        if (trimmed.equals("[[") || trimmed.endsWith("[[")) {
          name = null; // 다음 줄이 이름
        } else if (name == null
            && !trimmed.isEmpty()
            && !trimmed.startsWith("--")
            && !trimmed.startsWith("]]")) {
          name = trimmed;
        } else if (trimmed.startsWith("Limited to:") && name != null) {
          String value = trimmed.substring("Limited to:".length()).trim();
          if (value.matches("[0-9]+")) {
            limits.put(name, Integer.valueOf(value));
          }
        } else if (trimmed.startsWith("]]")) {
          name = null;
        }
      }
    } catch (java.io.IOException e) {
      logger.warn("주얼 한도 파싱 실패: {}", e.toString());
    }
    return limits;
  }

  /**
   * 최종 빌드의 속성(힘/민첩/지능)으로 장착 불가능한 유니크를 찾아낸다. PoB 는 요구치 미달이어도 스탯을 그대로 계산하므로, 걸러내지 않으면 게임에서 입을 수 없는
   * 장비가 낀 결과가 나온다(실측: 민첩 74 캐릭터에 민첩 170 요구 장비).
   */
  private List<PoeOptimizeResult.UnmetRequirement> unmetRequirements(
      Map<Slot, Equipped> items, Map<String, Double> finalValues, String standardWeaponBase) {
    int str = (int) Math.round(finalValues.getOrDefault("Str", 0d));
    int dex = (int) Math.round(finalValues.getOrDefault("Dex", 0d));
    int intel = (int) Math.round(finalValues.getOrDefault("Int", 0d));
    List<PoeOptimizeResult.UnmetRequirement> unmet = new ArrayList<>();
    // 무기 유니크가 없으면 XML 에 표준 무기가 주입된다 — 그 무기도 요구치 검사를 받아야 한다.
    // (후보 무기는 검사에 걸려 탈락하는데 주입 무기만 무사통과하면 결과가 조용히 불법이 된다)
    if (!items.containsKey(Slot.WEAPON) && standardWeaponBase != null) {
      poeBaseItemDataService
          .findByName(standardWeaponBase)
          .ifPresent(
              base -> {
                if (base.reqStr() > str) {
                  unmet.add(unmetOf(base, "str", base.reqStr(), str));
                }
                if (base.reqDex() > dex) {
                  unmet.add(unmetOf(base, "dex", base.reqDex(), dex));
                }
                if (base.reqInt() > intel) {
                  unmet.add(unmetOf(base, "int", base.reqInt(), intel));
                }
              });
    }
    for (Equipped equipped : items.values()) {
      // 유니크는 자체 요구치, 레어는 베이스 아이템 요구치(우리가 베이스+모드로 조합하므로)
      String name;
      String nameKo;
      Integer reqStr;
      Integer reqDex;
      Integer reqInt;
      if (equipped.isUnique()) {
        PoeUniqueItem unique = equipped.unique();
        name = unique.name();
        nameKo = unique.nameKo();
        reqStr = unique.reqStr();
        reqDex = unique.reqDex();
        reqInt = unique.reqInt();
      } else {
        PoeBaseItem base =
            poeBaseItemDataService.findByName(equipped.rare().baseType()).orElse(null);
        if (base == null) {
          continue;
        }
        name = base.name();
        nameKo = base.nameKo();
        reqStr = base.reqStr();
        reqDex = base.reqDex();
        reqInt = base.reqInt();
      }
      record Req(String attribute, Integer required, int actual) {}
      for (Req req :
          List.of(
              new Req("str", reqStr, str),
              new Req("dex", reqDex, dex),
              new Req("int", reqInt, intel))) {
        if (req.required() != null && req.required() > req.actual()) {
          unmet.add(
              new PoeOptimizeResult.UnmetRequirement(
                  name, nameKo, req.attribute(), req.required(), req.actual()));
        }
      }
    }
    // 장비별 검사에 안 걸렸는데 총 요구치(장비+젬)가 미달이면 젬 요구치 미달이다 — 항목으로 드러낸다
    for (Map.Entry<String, String> attrEntry :
        Map.of("str", "Str", "dex", "Dex", "int", "Int").entrySet()) {
      int required = (int) Math.round(finalValues.getOrDefault("Req" + attrEntry.getValue(), 0d));
      int actual = (int) Math.round(finalValues.getOrDefault(attrEntry.getValue(), 0d));
      boolean coveredByItem =
          unmet.stream()
              .anyMatch(u -> u.attribute().equals(attrEntry.getKey()) && u.required() >= required);
      if (required > actual && !coveredByItem) {
        unmet.add(
            new PoeOptimizeResult.UnmetRequirement(
                "Skill gem total", "스킬 젬 포함 총 요구치", attrEntry.getKey(), required, actual));
      }
    }
    if (!unmet.isEmpty()) {
      log(
          "⚠ 속성 부족으로 장착 불가한 장비 "
              + unmet.size()
              + "건: "
              + unmet.stream()
                  .map(
                      u ->
                          u.name()
                              + "("
                              + u.attribute()
                              + " "
                              + u.required()
                              + ">"
                              + u.actual()
                              + ")")
                  .collect(java.util.stream.Collectors.joining(", ")));
    }
    return unmet;
  }

  /**
   * 후보 장비가 그 빌드의 속성으로 실제 장착 가능한지. 유니크는 자체 요구치를, 레어는 <b>베이스 아이템의 요구치</b>를 본다 (레어는 우리가 베이스+모드로 조합하므로
   * 요구치는 베이스가 결정한다 — 예: 우주의 판금 갑옷 = 힘 180).
   */
  /**
   * 속성 요구치 보정 — 부족한 속성을 레어 장비의 접미어(+60 속성 T1)로 채운다.
   *
   * <p>대상 레어는 슬롯 순서대로 첫 후보(속성 패밀리 허용 슬롯 && 같은 속성 미보유). 접미어 3개가 차 있으면 마지막(키워드 점수 최하) 접미어를 교체하고, 여유가
   * 있으면 추가한다 — 3접두+3접미 합법성이 유지된다. 이미 이 보정으로 넣은 속성 접미어는 교체 대상에서 제외(핑퐁 방지).
   */
  private void repairAttributeShortfalls(
      Map<Slot, Equipped> items,
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> allocated,
      Map<Integer, PoeUniqueItem> jewels) {
    Set<String> attrKeys = Set.of("str", "dex", "int");
    for (int guard = 0; guard < 6; guard++) {
      Map<String, Double> values =
          poePobEngineService.calculateValues(
              buildXml(
                  gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels));
      evalCount.incrementAndGet();
      // 가장 부족한 속성 하나 — PoB 총계(ReqStr/Dex/Int = 장비+젬 합산)로 판정한다.
      // 아이템별 검사만 쓰면 스킬 젬 요구치(연쇄 번개 20레벨 = 지능 111)가 그물을 빠져나간다.
      String worstAttr = null;
      int worstShort = 0;
      for (Map.Entry<String, String> attrEntry :
          Map.of("str", "Str", "dex", "Dex", "int", "Int").entrySet()) {
        int shortfall =
            (int)
                Math.ceil(
                    values.getOrDefault("Req" + attrEntry.getValue(), 0d)
                        - values.getOrDefault(attrEntry.getValue(), 0d));
        if (shortfall > worstShort) {
          worstShort = shortfall;
          worstAttr = attrEntry.getKey();
        }
      }
      if (worstAttr == null) {
        return; // 전부 충족
      }
      final String attr = worstAttr; // 람다 캡처용
      // 우선순위: ① 빈 슬롯에 속성 레어 신규 장착(기존 모드 손실 0) ② 기존 레어에 추가/교체(+60 단일)
      // ③ 전체 속성(+35, 장신구) — 실측에서 +60×2 뒤 1 부족으로 멈춘 사례의 잔여분 커버.
      Slot targetSlot = null;
      Slot emptySlot = null;
      PoeModPoolDataService.ModFamily attrFamily = null;
      for (String familyKey : List.of(attr, "allattr")) {
        PoeModPoolDataService.ModFamily candidate = null;
        for (PoeModPoolDataService.ModFamily family : poeModPoolDataService.families()) {
          if (family.key().equals(familyKey)) {
            candidate = family;
            break;
          }
        }
        if (candidate == null) {
          continue;
        }
        final PoeModPoolDataService.ModFamily fam = candidate;
        // ① 비어 있는 슬롯(오프핸드 제외 — 양손 무기와 충돌)
        for (Slot slot : Slot.values()) {
          if (items.containsKey(slot)
              || slot.modSlots.isEmpty()
              || slot == Slot.OFFHAND
              || slot.rareBase == null) {
            continue;
          }
          if (fam.slots().contains(slot.modSlots.get(0))) {
            emptySlot = slot;
            attrFamily = fam;
            break;
          }
        }
        if (emptySlot != null) {
          break;
        }
        // ② 기존 레어
        for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
          if (entry.getValue().isUnique() || entry.getKey().modSlots.isEmpty()) {
            continue;
          }
          String category = entry.getKey().modSlots.get(0);
          if (!fam.slots().contains(category)) {
            continue;
          }
          boolean hasIt =
              entry.getValue().rare().families().stream().anyMatch(f -> f.key().equals(fam.key()));
          if (!hasIt) {
            targetSlot = entry.getKey();
            attrFamily = fam;
            break;
          }
        }
        if (targetSlot != null) {
          break;
        }
      }
      if (emptySlot != null) {
        items.put(
            emptySlot, Equipped.ofRare(new RareItem(emptySlot.rareBase, List.of(attrFamily), 0.0)));
        log(
            "속성 보정: "
                + emptySlot.ko
                + " 신규 레어(+"
                + attrFamily.key()
                + ") 장착 (부족 "
                + worstShort
                + ")");
        continue;
      }
      if (targetSlot == null) {
        log("속성 보정 불가 — " + worstAttr + " " + worstShort + " 부족인데 붙일 레어 슬롯이 없음(전 슬롯 유니크/보유)");
        return;
      }
      RareItem rare = items.get(targetSlot).rare();
      List<PoeModPoolDataService.ModFamily> families = new ArrayList<>(rare.families());
      long suffixCount = families.stream().filter(f -> "suffix".equals(f.gen())).count();
      String action;
      if (suffixCount < 3) {
        families.add(attrFamily);
        action = "추가";
      } else {
        // 마지막 접미어(키워드 점수 최하)를 교체 — 단 이 보정이 넣은 속성 접미어는 건너뛴다(핑퐁 방지)
        int replaceAt = -1;
        for (int i = families.size() - 1; i >= 0; i--) {
          if ("suffix".equals(families.get(i).gen()) && !attrKeys.contains(families.get(i).key())) {
            replaceAt = i;
            break;
          }
        }
        if (replaceAt < 0) {
          log("속성 보정 불가 — " + targetSlot.ko + " 접미어가 전부 속성(더 갈 곳 없음)");
          return;
        }
        action = families.get(replaceAt).key() + " 교체";
        families.set(replaceAt, attrFamily);
      }
      items.put(
          targetSlot,
          Equipped.ofRare(
              new RareItem(
                  rare.baseType(),
                  List.copyOf(families),
                  rare.tierFraction(),
                  null, // perFractions 는 패밀리 인덱스 기반이라 구성이 바뀌면 무효 — 균일 분수로 재설정
                  rare.implicitLines(),
                  rare.implicitLinesKo())));
      log(
          "속성 보정: "
              + targetSlot.ko
              + " "
              + action
              + " → +"
              + worstAttr
              + " (부족 "
              + worstShort
              + ")");
    }
  }

  /** 장착한 **모든** 장비의 속성 요구치가 계산값으로 충족되는가 — 문신 등 속성을 깎는 후행 단계의 검증기. */
  private boolean allRequirementsMet(Map<Slot, Equipped> items, Map<String, Double> values) {
    for (Equipped equipped : items.values()) {
      if (!meetsRequirements(equipped, values)) {
        return false;
      }
    }
    return true;
  }

  private boolean meetsRequirements(Equipped candidate, Map<String, Double> values) {
    if (candidate == null) {
      return true;
    }
    if (candidate.isUnique()) {
      PoeUniqueItem unique = candidate.unique();
      return meets(unique.reqStr(), values.get("Str"))
          && meets(unique.reqDex(), values.get("Dex"))
          && meets(unique.reqInt(), values.get("Int"));
    }
    return poeBaseItemDataService
        .findByName(candidate.rare().baseType())
        .map(
            base ->
                meets(base.reqStr(), values.get("Str"))
                    && meets(base.reqDex(), values.get("Dex"))
                    && meets(base.reqInt(), values.get("Int")))
        .orElse(true);
  }

  private static boolean meets(int required, Double actual) {
    return meets(Integer.valueOf(required), actual);
  }

  private static boolean meets(Integer required, Double actual) {
    return required == null || required <= 0 || (actual != null && actual >= required);
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
  /**
   * 무궁한(타임리스) 주얼 정의 — PoB `Data/Uniques/jewel.lua` 기준.
   *
   * <p>이 주얼들은 <b>시드</b>에 따라 반경 내 패시브가 통째로 바뀐다. PoB 가 그 변환을 계산하려면 아이템 문구에 ① {@code Radius: Large} ②
   * {@code Passives in radius are Conquered by the <리그>} ③ 시드 줄이 <b>모두</b> 있어야 한다(하나라도 빠지면 조용히 무시 —
   * 실측).
   */
  private record TimelessJewel(
      String league,
      String prefix,
      String suffix,
      int seedMin,
      int seedMax,
      List<String> conquerors) {}

  private static final Map<String, TimelessJewel> TIMELESS_JEWELS =
      Map.of(
          "Brutal Restraint",
              new TimelessJewel(
                  "Maraketh",
                  "Denoted service of ",
                  " dekhara in the akhara of ",
                  500,
                  8000,
                  List.of("Asenath", "Deshret", "Nasima", "Balbala")),
          "Lethal Pride",
              new TimelessJewel(
                  "Karui",
                  "Commanded leadership over ",
                  " warriors under ",
                  10000,
                  18000,
                  List.of("Kaom", "Kiloava", "Rakiata", "Akoya")),
          "Glorious Vanity",
              new TimelessJewel(
                  "Vaal",
                  "Bathed in the blood of ",
                  " sacrificed in the name of ",
                  100,
                  8000,
                  List.of("Doryani", "Xibaqua", "Zerphi", "Ahuana")),
          "Militant Faith",
              new TimelessJewel(
                  "Templars",
                  "Carved to glorify ",
                  " new faithful converted by High Templar ",
                  2000,
                  10000,
                  List.of("Avarius", "Dominus", "Venarius", "Maxarius")),
          "Elegant Hubris",
              new TimelessJewel(
                  "Eternal Empire",
                  "Commissioned ",
                  " coins to commemorate ",
                  2000,
                  160000,
                  List.of("Cadiro", "Chitus", "Victario", "Caspiro")),
          "Heroic Tragedy",
              new TimelessJewel(
                  "Kalguur",
                  "Remembrancing ",
                  " songworthy deeds by the line of ",
                  100,
                  8000,
                  List.of("Vorana", "Uhtred", "Medved")));

  /**
   * 타임리스 주얼에 <b>정복자·시드</b>를 지정한 사본을 만든다 — 같은 주얼이라도 이 둘에 따라 반경 패시브 변환 결과가 완전히 달라진다.
   *
   * <p>시드는 유효 범위로 클램프한다(범위 밖이면 PoB 가 데이터를 못 찾아 계산이 비어 버린다).
   */
  private PoeUniqueItem withTimeless(PoeUniqueItem item, String conqueror, String seedText) {
    TimelessJewel def = TIMELESS_JEWELS.get(item.name());
    if (def == null) {
      return item;
    }
    String chosen =
        def.conquerors().stream()
            .filter(c -> c.equalsIgnoreCase(conqueror))
            .findFirst()
            .orElse(def.conquerors().get(0));
    int seed = (def.seedMin() + def.seedMax()) / 2;
    if (seedText != null && seedText.matches("[0-9]+")) {
      seed = Math.max(def.seedMin(), Math.min(def.seedMax(), Integer.parseInt(seedText)));
    }
    List<String> lines = new ArrayList<>(item.explicits());
    lines.add("Passives in radius are Conquered by the " + def.league());
    lines.add(def.prefix() + seed + def.suffix() + chosen);
    return new PoeUniqueItem(
        item.name() + " (" + chosen + " " + seed + ")",
        item.nameKo() != null ? item.nameKo() + " (" + chosen + " " + seed + ")" : null,
        item.slug(),
        item.baseType(),
        item.baseTypeKo(),
        item.category(),
        item.requiredLevel(),
        item.league(),
        item.radius(),
        item.implicits(),
        item.implicitsKo(),
        lines,
        item.explicitsKo(),
        item.reqStr(),
        item.reqDex(),
        item.reqInt(),
        item.iconKey());
  }

  /** 타임리스 주얼이면 PoB 가 반경 변환을 계산하도록 문구 3종을 붙인다(없으면 조용히 무시). 첫 정복자·중앙 시드 기본값. */
  private List<String> timelessLines(PoeUniqueItem item) {
    // withTimeless() 로 이미 지정된 사본이면(이름에 정복자/시드가 붙고 문구도 들어 있다) 다시 붙이지 않는다
    if (item.explicits().stream()
        .anyMatch(line -> line.startsWith("Passives in radius are Conquered"))) {
      return List.of();
    }
    TimelessJewel def = TIMELESS_JEWELS.get(item.name());
    if (def == null) {
      return List.of();
    }
    int seed = (def.seedMin() + def.seedMax()) / 2;
    String conqueror = def.conquerors().get(0);
    return List.of(
        // 반경 라벨은 이제 고유 데이터(item.radius())가 들고 있다 — 중복 방지로 여기선 붙이지 않는다
        "Passives in radius are Conquered by the " + def.league(),
        def.prefix() + seed + def.suffix() + conqueror);
  }

  private List<PoeUniqueItem> globalJewelCandidates(List<String> keywords) {
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "jewel", null).stream()
        .filter(item -> item.requiredLevel() == null || item.requiredLevel() <= LEVEL)
        // 트리 소켓에 **꽂을 수 없는** 주얼을 후보에서 뺀다. PoB 는 소켓 종류를 검증하지 않으므로
        // 그냥 두면 게임에서 만들 수 없는 빌드가 더 높은 점수를 받는다(사이클 110 의 클러스터 소켓과 같은 계열).
        //  · Cluster/Timeless : 전용 소켓·전용 취급
        //  · "… Eye Jewel"    : 어비스 주얼 — **장비의 심연 소켓 전용**이라 트리엔 못 넣는다(4종)
        .filter(
            item ->
                item.baseType() == null
                    || (!item.baseType().contains("Cluster")
                        && !item.baseType().contains("Timeless")
                        && !item.baseType().endsWith("Eye Jewel")))
        // 반경 주얼은 **제외하지 않는다** — PoB 는 소켓 위치 기준으로 반경 안 패시브를 실제로 계산한다.
        // 실측(소켓을 트리에 연결한 상태): 비옥한 정신 지능 82→102, 효율적 훈련 힘 24→44.
        // 예전엔 "반경/근처" 문구를 통째로 걸러 유니크 주얼 179개 중 81개가 후보에서 빠져 있었다.
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
      if (slot == Slot.OFFHAND && offhandBlocked(items)) {
        log("강제 유니크 배치 불가(양손 무기와 동시 장착 불가): " + label);
        continue;
      }
      items.put(slot, Equipped.ofUnique(unique));
      if (slot == Slot.WEAPON
          && isTwoHandedUnique(unique)
          && !"bow".equals(unique.category())
          && items.containsKey(Slot.OFFHAND)) {
        log("보조장비 해제(양손 유니크 강제 장착): " + equippedLabel(items.remove(Slot.OFFHAND)));
      }
      log("강제 장착: " + slot.ko + " = " + label);
    }
  }

  private List<PoeUniqueItem> itemCandidates(
      Slot slot, PoeGem gem, List<String> keywords, Map<Slot, Equipped> equipped) {
    // 활을 들면 보조장비는 **화살통**이다(방패는 못 든다). 활 빌드에서 화살통을 아예 후보에서 빼면
    // 게임에선 당연히 쓰는 슬롯을 통째로 비우게 된다.
    List<String> categories =
        slot == Slot.WEAPON
            ? weaponCategories(gem)
            : slot == Slot.OFFHAND && weaponIsBow(equipped) ? List.of("quiver") : slot.categories;
    Set<String> equippedSlugs = new LinkedHashSet<>();
    for (Map.Entry<Slot, Equipped> entry : equipped.entrySet()) {
      if (entry.getKey() != slot && entry.getValue().isUnique()) {
        equippedSlugs.add(entry.getValue().unique().slug());
      }
    }
    // 셉터는 데이터상 category 가 "mace" 라, 주문 빌드 화이트리스트(wand/staff/dagger)로는 **한 자루도** 안 잡힌다.
    // 도리아니의 촉매 같은 대표 캐스터 무기가 통째로 빠지므로 베이스 이름으로 따로 열어 준다.
    boolean spellWeapon =
        slot == Slot.WEAPON
            && (poeSkillWeaponDataService.allowsSceptre(gem.name())
                || (poeSkillWeaponDataService.categories(gem.name()).isEmpty()
                    && categories.contains("wand")));
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "all", null).stream()
        .filter(
            item ->
                categories.contains(item.category())
                    || (spellWeapon
                        && item.baseType() != null
                        && item.baseType().contains("Sceptre")))
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

  /**
   * 무기 슬롯 후보 카테고리.
   *
   * <p>스킬에 <b>무기 제한</b>이 있으면 그걸 그대로 쓴다(PoB 스킬 데이터). 젬 태그로는 알 수 없다 — 마력 착취는 태그가 [Critical, Attack,
   * Projectile] 뿐인데 완드 전용이라, 태그 추정으로는 도끼/검을 쥐여 주게 된다.
   */
  private List<String> weaponCategories(PoeGem gem) {
    if (poeSkillWeaponDataService.requiresUnarmed(gem.name())) {
      return List.of(); // 맨손 전용 — 무기 후보 없음
    }
    List<String> restricted = poeSkillWeaponDataService.categories(gem.name());
    if (!restricted.isEmpty()) {
      return restricted;
    }
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
    // 고정 클러스터 주얼은 이 경로(잡 전용)에서만 끼운다 — 트리 평가는 자기 인자로 따로 넣는다
    return buildXmlWithClusters(
        buildXmlAuras(
            gem,
            supports,
            className,
            ascendancy,
            ascendancyNodes,
            treeNodes,
            items,
            jewels,
            auras,
            // 트리 에디터에서 확정한 마스터리 효과 — 안 넘기면 마스터리 노드만 찍히고 스탯은 0 이 된다
            fixedMasteries),
        treeNodes);
  }

  /**
   * 잡 경로용 XML — 고정 클러스터 주얼을 항상 끼워 넣는다. 트리 평가(evaluateTree)는 자기 인자로 따로 넣으므로 이 경로를 타지 않는다(잡 상태 오염
   * 방지).
   */
  private String buildXmlWithClusters(String xml, Set<Integer> treeNodes) {
    return withTattoos(
        fixedClusters.isEmpty() ? xml : withClusterJewels(xml, fixedClusters, treeNodes),
        fixedTattoos,
        treeNodes);
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
    // 최적화 잡의 현재 설정(공유 필드)을 그대로 쓰는 기본 경로
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
        masteryEffects,
        enemyScenario,
        combatBuffs,
        additionalSkills,
        secondaryAscendId);
  }

  /**
   * 잡 설정을 명시적으로 받는 본체. 트리 평가처럼 최적화와 무관한 호출은 이쪽을 써야 한다 — 공유 필드(적 시나리오/전투 버프/추가 스킬/2차 전직)를 읽으면 <b>직전에
   * 돌아간 최적화 잡의 설정이 그대로 섞여</b> 같은 트리가 다른 결과를 낸다(실측 DPS 4,021 ↔ 5,473).
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
      Map<Integer, Integer> masteryEffects,
      String enemyScenario,
      boolean combatBuffs,
      List<PoeGem> additionalSkills,
      int secondaryAscendId) {
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
      // 유니크도 엘드리치 부여 대상 슬롯이면 임플리싯을 받는다 — 레어에만 주면 그 슬롯에서 레어가 부당하게 유리해진다.
      // (아뮬렛 도유는 탐색이 끝난 뒤 withAnoint 로 얹는다 — 후보를 실제로 평가해 고르므로 여기서 붙이지 않는다)
      String itemText =
          equipped.isUnique()
              ? uniqueItemText(equipped.unique(), eldritchForSlot(entry.getKey()))
              : rareItemText(equipped.rare());
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
    // 방패 필요 스킬(방패 강타·방패 돌진 등)은 방패가 없으면 PoB 가 스킬을 통째로 비활성 처리한다
    // (실측: 방패 강타 기준값 0 · 최종 0). 아직 보조장비가 없으면 표준 방패를 지급한다.
    if (!items.containsKey(Slot.OFFHAND) && poeSkillWeaponDataService.requiresShield(gem.name())) {
      itemId++;
      xml.append("<Item id=\"")
          .append(itemId)
          .append(
              "\">\nRarity: RARE\nSim Shield\nColossal Tower Shield\nItem Level: 84\nImplicits: 0\n+300 to Armour\n+100 to maximum Life\n</Item>");
      slots.append("<Slot name=\"Weapon 2\" itemId=\"").append(itemId).append("\"/>");
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
    // 장비가 하나도 없어도 주얼만 있으면 ItemSet 을 내보내야 한다 — PoB 는 ItemSet 이 없으면
    // Items 섹션 자체를 활성화하지 않아 트리 Sockets 로 연결한 주얼이 통째로 무시된다(트리 평가 경로에서 발견).
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
    // 참고: 보스 적 저항(Boss 40/25, Pinnacle·Uber 50/30)은 enemyIsBoss placeholder 경로로 헤드리스에서도
    // 정상 반영된다 — 엔진 격리 실험으로 확인(화염 스킬: res0 5,022 / Pinnacle 2,511 = 명시 Input 50 과 동일).
    // 순수 물리 빌드(별의 탄생 등)는 원소 저항 무관이라 None/Boss/Pinnacle DPS 가 같게 나오는 것이 **정상**이다.
    if (buffs) {
      config
          .append("<Input name=\"usePowerCharges\" boolean=\"true\"/>")
          .append("<Input name=\"useFrenzyCharges\" boolean=\"true\"/>")
          .append("<Input name=\"useEnduranceCharges\" boolean=\"true\"/>") // 실측 DPS +2.17%, EHP
          // +14.24%
          .append("<Input name=\"buffOnslaught\" boolean=\"true\"/>")
          // 격노/방어상승은 엔진이 스스로 게이트한다 — 원천(광전사 전직, 보조젬 등) 없는 빌드에선 0.00% 실측.
          // 원천이 있으면 실전처럼 최대치 가정(전투 버프 토글의 기존 철학과 동일).
          .append("<Input name=\"multiplierRage\" number=\"30\"/>")
          .append("<Input name=\"buffFortify\" boolean=\"true\"/>")
          // 적 상태 이상 조건 — 자기 빌드가 걸 수 있을 때만 효과가 붙는다(감전 능력 없는 빌드 0.00% 실측).
          // 효과 수치 override 는 넣지 않는다(50% 수동 가정 시 +36% — 근거 없는 낙관이 된다).
          .append("<Input name=\"conditionEnemyShocked\" boolean=\"true\"/>")
          .append("<Input name=\"conditionEnemyChilled\" boolean=\"true\"/>")
          .append("<Input name=\"conditionEnemyIgnited\" boolean=\"true\"/>")
          .append("<Input name=\"conditionEnemyPoisoned\" boolean=\"true\"/>")
          .append("<Input name=\"conditionEnemyBleeding\" boolean=\"true\"/>");
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
    // 맨손 전용 스킬(독성/폭발 혼합물)은 무기를 주면 안 된다 — 주는 순간 PoB 가 스킬을 비활성 처리해
    // 수치가 통째로 0 이 된다(실측: 독성 혼합물 기준값 0 · 최종 0).
    if (poeSkillWeaponDataService.requiresUnarmed(gem.name())) {
      return null;
    }
    // 스킬에 무기 제한이 있으면 **그 종류의 표준 무기**를 준다. 안 그러면 탐색 내내 스킬이 비활성이라
    // 기준값이 0 이 되고(실측: 마력 착취 프로브 0), 트리·보조젬 단계가 아무 신호 없이 돌아간다.
    List<String> restricted = poeSkillWeaponDataService.itemClasses(gem.name());
    if (!restricted.isEmpty()) {
      String base =
          STANDARD_WEAPON_BY_CLASS.entrySet().stream()
              .filter(entry -> restricted.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);
      if (base != null) {
        return base;
      }
    }
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    if (tags.contains("Bow")) {
      return "Thicket Bow";
    }
    if (tags.contains("Attack")) {
      return "Vaal Axe";
    }
    return null;
  }

  /** 무기 종류별 표준(고티어) 베이스 — 무기 제한이 있는 스킬의 기본 무기로 쓴다. */
  /**
   * 무기 제한 스킬에 지급할 표준 무기 — 선언 순서가 곧 **선택 우선순위**다(여러 종류를 쓸 수 있는 스킬은 첫 매치 지급).
   *
   * <p>⚠ {@code new LinkedHashMap<>(Map.ofEntries(...))} 로 만들면 안 된다 — 자바 불변 맵의 순회 순서는 JVM 실행마다
   * 무작위(SALT)라, 그 순서를 물려받은 LinkedHashMap 도 실행마다 다르다. 사이클론이 프로세스마다 Vaal Rapier / Imperial Maul /
   * Imperial Claw 를 번갈아 받아 **최적화 결과 전체가 실행마다 갈렸다**(직업 프로브 지문으로 확정). 반드시 put 으로 선언 순서를 고정한다.
   */
  private static final Map<String, String> STANDARD_WEAPON_BY_CLASS = standardWeaponByClass();

  private static Map<String, String> standardWeaponByClass() {
    Map<String, String> map = new java.util.LinkedHashMap<>();
    map.put("Bow", "Thicket Bow");
    map.put("Wand", "Prophecy Wand");
    map.put("Sceptre", "Void Sceptre");
    map.put("Staff", "Judgement Staff");
    map.put("Warstaff", "Eclipse Staff");
    map.put("Claw", "Imperial Claw");
    map.put("Dagger", "Platinum Kris");
    map.put("Rune Dagger", "Platinum Kris");
    map.put("Two Hand Axe", "Vaal Axe");
    map.put("Two Hand Sword", "Exquisite Blade");
    map.put("Two Hand Mace", "Imperial Maul");
    map.put("One Hand Axe", "Siege Axe");
    map.put("One Hand Sword", "Midnight Blade");
    map.put("Thrusting One Hand Sword", "Vaal Rapier");
    map.put("One Hand Mace", "Behemoth Mace");
    return map;
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
    // 방어구는 속성 변형(str/dex/int)마다 붙는 **로컬 방어 모드가 다르다**(방어도=힘, 회피=민첩, ES=지능).
    // 큐레이션 풀은 슬롯 단위라 구분이 없어, 그대로 두면 지능 베이스(소서러 장갑)에 "방어도 +500" 같은
    // 게임에 존재할 수 없는 아이템이 계산된다. 직업 주 속성에 맞는 베이스를 고르고 그 변형의 로컬만 남긴다.
    Map<String, String> variantBases = ARMOUR_VARIANT_BASE.getOrDefault(category, Map.of());
    String wanted = defenceTypeFor(currentClassName);
    // 그 슬롯에 원하는 변형 베이스가 없으면(방패의 회피) 방어도 → ES 순으로 폴백
    String variantBase = variantBases.get(wanted);
    if (variantBase == null) {
      variantBase = variantBases.getOrDefault("armour", variantBases.get("es"));
    }
    final String chosenBase = variantBase != null ? variantBase : rareBase;
    rareBase = chosenBase;
    // 합법성은 **고른 베이스 자체**로 게임 데이터에 물어본다 — 방어도/회피/ES 뿐 아니라 모든 모드가 속성 변형
    // (힘/민첩/지능)마다 붙고 안 붙고가 갈리므로, 하드코딩 대신 전체 풀(mods.json)의 (클래스×변형) 스폰 여부로
    // 판정한다. 베이스를 못 찾거나 그 풀을 모르면 판정을 보류해 기존 동작을 유지한다.
    PoeBaseItem chosenBaseItem = poeBaseItemDataService.findByName(chosenBase).orElse(null);
    final String baseClass = chosenBaseItem != null ? chosenBaseItem.itemClass() : null;
    final String baseVariant = chosenBaseItem != null ? attributeVariantOf(chosenBaseItem) : null;
    List<PoeModPoolDataService.ModFamily> pool =
        poeModPoolDataService.familiesForSlot(category).stream()
            .filter(
                f ->
                    baseClass == null
                        || poeModDataService.canSpawn(baseClass, baseVariant, f.pattern()))
            .toList();
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
    // 엘드리치 임플리싯(총주교 1 + 포식자 1) — 방어구/목걸이 슬롯이면 스킬 키워드에 맞는 최상위 티어를 얹는다.
    EldritchPick eldritch = eldritchImplicits(category, keywords);
    return new RareItem(rareBase, chosen, tierFraction, null, eldritch.en(), eldritch.ko());
  }

  /** 엘드리치 임플리싯 선택 결과 — 영문(XML)·한글(표시) 스탯 줄. */
  private record EldritchPick(List<String> en, List<String> ko) {}

  // 모드 풀 슬롯 카테고리 → 엘드리치 아이템 클래스. 엘드리치 대상 아닌 슬롯은 없음.
  private static final Map<String, String> ELDRITCH_CLASS_BY_CATEGORY =
      Map.of(
          "body", "Body Armour",
          "helmet", "Helmet",
          "gloves", "Gloves",
          "boots", "Boots",
          "amulet", "Amulet");

  /**
   * 이 슬롯에 붙일 엘드리치 임플리싯 영문 스탯 줄 — 총주교 1개 + 포식자 1개(각 팩션에서 스킬 키워드 점수 최상위 계열의 최상위 티어). 대상 슬롯이 아니거나 데이터가
   * 없으면 빈 목록.
   */
  private EldritchPick eldritchImplicits(String category, List<String> keywords) {
    String itemClass = ELDRITCH_CLASS_BY_CATEGORY.get(category);
    if (itemClass == null || !poeEldritchDataService.hasData()) {
      return new EldritchPick(List.of(), List.of());
    }
    PoeEldritchDataService.ClassEldritch pools = poeEldritchDataService.forItemClass(itemClass);
    if (pools == null) {
      return new EldritchPick(List.of(), List.of());
    }
    List<String> en = new ArrayList<>();
    List<String> ko = new ArrayList<>();
    for (var pool : List.of(pools.exarch(), pools.eater())) {
      var tier = bestEldritchTier(pool, keywords);
      if (tier != null) {
        en.addAll(tier.en());
        ko.addAll(tier.ko());
      }
    }
    return new EldritchPick(en, ko);
  }

  /** 한 팩션 계열들에서 키워드 점수 최상위 계열의 최상위 티어(tiers[0]=강함). 점수 0이면(무관) null. */
  private PoeEldritchDataService.EldritchTier bestEldritchTier(
      List<PoeEldritchDataService.EldritchFamily> families, List<String> keywords) {
    PoeEldritchDataService.EldritchTier best = null;
    int bestScore = 0;
    for (PoeEldritchDataService.EldritchFamily family : families) {
      if (family.tiers().isEmpty()) {
        continue;
      }
      int s = score(family.tiers().get(0).en(), keywords);
      if (s > bestScore) {
        bestScore = s;
        best = family.tiers().get(0);
      }
    }
    return best;
  }

  /**
   * 방어구 슬롯의 속성 변형별 레어 베이스 — 로컬 방어 모드는 변형에만 붙으므로(방어도=힘/회피=민첩/ES=지능) 쓰려는 방어 타입에 맞는 베이스를 골라야 게임에 실재하는
   * 아이템이 된다. 동시에 그 직업이 실제로 장착 가능한 요구 속성이 된다.
   */
  private static final Map<String, Map<String, String>> ARMOUR_VARIANT_BASE =
      Map.of(
          "body",
              Map.of(
                  "armour", "Glorious Plate", "evasion", "Assassin's Garb", "es", "Vaal Regalia"),
          "helmet",
              Map.of("armour", "Royal Burgonet", "evasion", "Lion Pelt", "es", "Hubris Circlet"),
          "gloves",
              Map.of(
                  "armour", "Titan Gauntlets", "evasion", "Slink Gloves", "es", "Sorcerer Gloves"),
          "boots",
              Map.of("armour", "Titan Greaves", "evasion", "Slink Boots", "es", "Sorcerer Boots"),
          // 방패는 순수 민첩(회피) 베이스가 없다 — 힘/지능만 두고, 회피 요청은 아래에서 힘으로 폴백시킨다.
          // (값이 겹치면 베이스→방어타입 역산이 비결정적이 되므로 중복 값을 두지 않는다)
          "shield", Map.of("armour", "Colossal Tower Shield", "es", "Titanium Spirit Shield"));

  /** 직업 주 속성 → 방어 타입. 그 직업이 실제로 입는 방어구 계열과 일치시킨다(미지/사이온은 ES). */
  private static String defenceTypeFor(String className) {
    if (className == null) {
      return "es";
    }
    return switch (className) {
      case "Marauder", "Duelist", "Templar" -> "armour";
      case "Ranger", "Shadow" -> "evasion";
      default -> "es"; // Witch, Scion, 미지정
    };
  }

  /** 베이스의 속성 변형 태그를 요구 속성으로 추론한다 — mods.json 의 (클래스×변형) 풀 키와 맞춘다. 요구 속성이 없으면 빈 문자열(변형 없는 슬롯). */
  private static String attributeVariantOf(PoeBaseItem item) {
    boolean str = item.reqStr() > 0;
    boolean dex = item.reqDex() > 0;
    boolean intel = item.reqInt() > 0;
    if (str && dex && intel) {
      return "str_dex_int_armour";
    }
    if (str && dex) {
      return "str_dex_armour";
    }
    if (str && intel) {
      return "str_int_armour";
    }
    if (dex && intel) {
      return "dex_int_armour";
    }
    if (str) {
      return "str_armour";
    }
    if (dex) {
      return "dex_armour";
    }
    if (intel) {
      return "int_armour";
    }
    return "";
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

  private String rareItemText(RareItem rare) {
    return rareItemText(rare, List.of());
  }

  /**
   * 레어 → PoB 아이템 텍스트 (각 패밀리를 tierFraction 위치의 티어 최대 롤로)
   *
   * @param extraImplicits 임플리싯 뒤에 덧붙일 줄(아뮬렛 도유). Implicits 개수에 함께 반영해야 PoB 가 옳게 읽는다.
   */
  private String rareItemText(RareItem rare, List<String> extraImplicits) {
    StringBuilder text = new StringBuilder();
    text.append("Rarity: RARE\nSim Craft\n").append(rare.baseType()).append("\n");
    // 엘드리치 임플리싯이 있으면 Implicits 개수에 반영하고 곧바로 그 줄들을 넣는다(PoB 는 Implicits:N 뒤 N줄을 임플리싯으로 읽는다).
    List<String> implicits =
        new ArrayList<>(rare.implicitLines() != null ? rare.implicitLines() : List.of());
    implicits.addAll(extraImplicits != null ? extraImplicits : List.of());
    text.append("Item Level: 86\nImplicits: ").append(implicits.size()).append("\n");
    for (String line : implicits) {
      text.append(line).append("\n");
    }
    // 공격 무기엔 명중을 기본 전제로 얹는다(표준 무기 fallback 과 동일 조건).
    // accuracyLocal 패밀리가 생긴 뒤 이 줄을 빼고 실측했더니 DPS 6,490,767 → 4,008,572(−38%) 였다 —
    // 크래프트로 얻는 명중(+360)은 턱없이 부족한데 fallback 무기엔 +2000 이 남아 비교가 불공정해진다.
    // 근본 해결은 명중을 트리/오라(정밀함)까지 모델링하는 것. 그 전까지는 양쪽 같은 전제를 유지한다.
    if (isAttackWeaponBase(rare.baseType())) {
      text.append("+2000 to Accuracy Rating\n");
    }
    for (int i = 0; i < rare.families().size(); i++) {
      for (String line : tierAt(rare.families().get(i), rare.fractionFor(i)).en()) {
        text.append(line).append("\n");
      }
    }
    return text.toString();
  }

  private PoeOptimizeResult.UnmetRequirement unmetOf(
      PoeBaseItem base, String attribute, int required, int actual) {
    return new PoeOptimizeResult.UnmetRequirement(
        base.name(), base.nameKo(), attribute, required, actual);
  }

  /** 양손 무기 판정 — 보조장비(방패)와 동시에 들 수 없다. */
  private static final Set<String> TWO_HANDED_CLASSES =
      Set.of("Two Hand Axe", "Two Hand Sword", "Two Hand Mace", "Staff", "Bow");

  /**
   * 유니크가 양손 무기인지 — 활/지팡이는 카테고리만으로 확정, 나머지는 베이스의 itemClass 로 판정.
   *
   * <p>PoB 는 양손 무기를 든 상태의 보조장비를 계산에서 무시한다(실측). 그래서 스탯이 부풀지는 않지만, 결과 화면엔 <b>실제로는 아무 일도 안 하는 방패가 장착된
   * 것처럼</b> 표시되고 평가 비용도 낭비된다.
   */
  private boolean isTwoHandedUnique(PoeUniqueItem unique) {
    if (unique == null) {
      return false;
    }
    String category = unique.category();
    if ("bow".equals(category) || "staff".equals(category)) {
      return true;
    }
    return unique.baseType() != null
        && poeBaseItemDataService
            .findByName(unique.baseType())
            .map(base -> TWO_HANDED_CLASSES.contains(base.itemClass()))
            .orElse(false);
  }

  /** 현재 무기가 활인지 — 활은 양손이지만 <b>화살통</b>은 함께 든다(방패만 불가). */
  private boolean weaponIsBow(Map<Slot, Equipped> items) {
    Equipped weapon = items.get(Slot.WEAPON);
    if (weapon == null) {
      return false;
    }
    if (weapon.isUnique()) {
      return "bow".equals(weapon.unique().category());
    }
    return weapon.rare() != null
        && weapon.rare().baseType() != null
        && poeBaseItemDataService
            .findByName(weapon.rare().baseType())
            .map(base -> "Bow".equals(base.itemClass()))
            .orElse(false);
  }

  /** 보조장비 슬롯을 아예 못 쓰는 상태인지 — 활이 아닌 양손 무기일 때만 막는다. */
  private boolean offhandBlocked(Map<Slot, Equipped> items) {
    return weaponIsTwoHanded(items) && !weaponIsBow(items);
  }

  /** 현재 무기가 양손인지(유니크/레어 공통) */
  private boolean weaponIsTwoHanded(Map<Slot, Equipped> items) {
    Equipped weapon = items.get(Slot.WEAPON);
    if (weapon == null) {
      return false;
    }
    if (weapon.isUnique()) {
      return isTwoHandedUnique(weapon.unique());
    }
    return weapon.rare() != null
        && weapon.rare().baseType() != null
        && poeBaseItemDataService
            .findByName(weapon.rare().baseType())
            .map(base -> TWO_HANDED_CLASSES.contains(base.itemClass()))
            .orElse(false);
  }

  /** 젬 종류에 맞는 무기 베이스 중 기본 DPS 상위 N개. 요구 속성은 후보 평가 단계에서 실제 값으로 걸러진다. */
  private List<PoeBaseItem> weaponBaseCandidates(PoeGem gem, int limit, Equipped offhand) {
    if (poeSkillWeaponDataService.requiresUnarmed(gem.name())) {
      return List.of(); // 맨손 전용 — 크래프트 무기도 주지 않는다
    }
    List<String> restrictedClasses = poeSkillWeaponDataService.itemClasses(gem.name());
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    List<String> classes;
    if (!restrictedClasses.isEmpty()) {
      classes = restrictedClasses;
    } else if (tags.contains("Bow")) {
      classes = List.of("Bow");
    } else if (tags.contains("Attack")) {
      classes =
          List.of(
              "Two Hand Axe",
              "Two Hand Sword",
              "Two Hand Mace",
              "One Hand Axe",
              "One Hand Sword",
              "One Hand Mace",
              "Claw",
              "Dagger",
              "Staff");
    } else {
      classes = List.of("Wand", "Sceptre");
    }
    // 보조장비를 이미 낀 상태면 양손 무기 제외 — 게임에서 방패와 양손 무기는 동시에 못 든다
    boolean offhandOccupied = offhand != null;
    return poeBaseItemDataService.search(null, null).stream()
        .filter(base -> base.weapon() != null && classes.contains(base.itemClass()))
        .filter(base -> !offhandOccupied || !TWO_HANDED_CLASSES.contains(base.itemClass()))
        .filter(base -> base.dropLevel() <= LEVEL)
        .sorted(
            Comparator.comparingDouble(
                    (PoeBaseItem base) ->
                        (base.weapon().damageMin() + base.weapon().damageMax())
                            / 2.0
                            * base.weapon().attacksPerSecond())
                .reversed())
        .limit(limit)
        .toList();
  }

  /**
   * 트리 XML 에 클러스터 주얼을 꽂아 넣는다. PoB 는 <b>소켓에 꽂힌 주얼 아이템 문구</b>를 보고 서브트리를 생성하므로, 우리가 생성한 노드 id(≥65536)가
   * Spec 의 nodes 에 있어도 <b>주얼이 없으면 그 노드는 존재하지 않아 무시된다</b>. 아이템 id 는 장비(1..N)/유니크 주얼(900번대)과 겹치지 않게
   * 800번대를 쓴다.
   */
  private String withClusterJewels(String xml, List<ClusterSpec> specs, Set<Integer> nodes) {
    if (specs == null || specs.isEmpty() || !poeClusterJewelDataService.hasData()) {
      return xml;
    }
    StringBuilder items = new StringBuilder();
    StringBuilder sockets = new StringBuilder();
    int itemId = 800;
    for (ClusterSpec spec : specs) {
      if (!nodes.contains(spec.socket())) {
        continue; // 할당되지 않은 소켓의 클러스터는 게임에서도 효과가 없다
      }
      var text =
          poeClusterJewelDataService.itemText(
              spec.sizeName(),
              spec.nodeCount(),
              spec.skillKey(),
              spec.notables(),
              spec.socketCount());
      if (text.isEmpty()) {
        continue;
      }
      items
          .append("<Item id=\"")
          .append(itemId)
          .append("\">" + System.lineSeparator())
          .append(text.get())
          .append("</Item>");
      sockets
          .append("<Socket nodeId=\"")
          .append(spec.socket())
          .append("\" itemId=\"")
          .append(itemId)
          .append("\"/>");
      itemId++;
    }
    if (sockets.length() == 0) {
      return xml;
    }
    String out = xml;
    if (out.contains("<Sockets>")) {
      out = out.replace("<Sockets>", "<Sockets>" + sockets);
    } else {
      out = out.replace("</Spec>", "<Sockets>" + sockets + "</Sockets></Spec>");
    }
    return out.replace("<Items activeItemSet=\"1\">", "<Items activeItemSet=\"1\">" + items);
  }

  /**
   * 트리 평가용 도유 — 아뮬렛 아이템의 {@code Allocates <노터블>} 로 전달한다(PoB 의 도유 시맨틱).
   *
   * <p>노드 id 를 nodes 목록에 끼우는 방식은 안 된다: PoB 가 시작점과 연결되지 않은 노드를 로드시 **해제
   * 처리**해서(BuildAllDependsAndPaths) 스탯에 반영되지 않는다(실측: 고립 노터블 38706 을 nodes 에 넣어도 생명력 불변). 베이스는 임플리싯
   * 줄을 안 넣으므로 부가 스탯 없음.
   */
  private String withAnointAmulet(String xml, Integer anointNodeId) {
    if (anointNodeId == null) {
      return xml;
    }
    PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(anointNodeId);
    if (node == null || node.anoint() == null || node.anoint().isEmpty()) {
      return xml; // 도유 불가 노드 id — 조용히 무시(손댄 URL)
    }
    int maxId = 0;
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("<Item id=\"(\\d+)\">").matcher(xml);
    while (m.find()) {
      maxId = Math.max(maxId, Integer.parseInt(m.group(1)));
    }
    int itemId = maxId + 1;
    String item =
        "<Item id=\""
            + itemId
            + "\">"
            + System.lineSeparator()
            + "Rarity: MAGIC\nSim Anoint Amulet\nJade Amulet\nItem Level: 86\nImplicits: 0\nAllocates "
            + node.name()
            + "\n</Item>";
    // 슬롯은 반드시 <ItemSet> **안**에 — 느슨한 Slot(Items 직속)은 PoB 가 무시해서
    // 아뮬렛이 장착되지 않고 GrantedPassive 도 조용히 사라진다(디버그로 확정한 함정).
    String slot = "<Slot name=\"Amulet\" itemId=\"" + itemId + "\"/>";
    return xml.replace("<Items activeItemSet=\"1\">", "<Items activeItemSet=\"1\">" + item)
        .replace("<ItemSet id=\"1\">", "<ItemSet id=\"1\">" + slot);
  }

  /**
   * 문신(패시브 교체)을 XML 조립 후 Spec 에 끼운다 — PoB 는 {@code <Overrides><Override nodeId dn/></Overrides>} 를
   * 읽어 그 노드를 {@code tree.tattoo.nodes[dn]} 으로 통째로 갈아끼운다.
   *
   * <p>할당되지 않은 노드의 문신은 게임에서도 효과가 없으므로 버린다. 알 수 없는 dn 은 PoB 가 무시하지만(에러 로그만) 우리 쪽에서 미리 걸러 낸다.
   */
  private String withTattoos(String xml, Map<Integer, String> tattooDns, Set<Integer> nodes) {
    if (tattooDns == null || tattooDns.isEmpty() || !poeTattooDataService.hasData()) {
      return xml;
    }
    StringBuilder overrides = new StringBuilder();
    for (Map.Entry<Integer, String> entry : tattooDns.entrySet()) {
      if (!nodes.contains(entry.getKey())) {
        continue;
      }
      var tattoo = poeTattooDataService.findByDn(entry.getValue()).orElse(null);
      if (tattoo == null) {
        continue;
      }
      overrides
          .append("<Override nodeId=\"")
          .append(entry.getKey())
          .append("\" dn=\"")
          .append(escapeXml(tattoo.dn()))
          .append("\" icon=\"")
          .append(escapeXml(tattoo.icon()))
          .append("\" activeEffectImage=\"\"/>");
    }
    if (overrides.length() == 0) {
      return xml;
    }
    return xml.replace("</Spec>", "<Overrides>" + overrides + "</Overrides></Spec>");
  }

  private static String escapeXml(String value) {
    return value == null
        ? ""
        : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
  }

  /** 공격용 무기 베이스인지 — 표준 무기와 동일 전제(명중)를 줄지 판단. */
  private boolean isAttackWeaponBase(String baseType) {
    // 베이스 데이터의 category 는 세부 무기종이 아니라 "weapon" 이다 — WEAPON_CATEGORIES 로 판정하면 항상 false 가 돼
    // 명중이 안 붙고, 크래프트 무기가 표준 무기(명중 +2000)를 영원히 못 이긴다(이 함정으로 한 사이클 낭비).
    return poeBaseItemDataService
        .findByName(baseType)
        .map(base -> base.weapon() != null)
        .orElse(false);
  }

  /** 레어의 한국어 모드 라인 (표시용) */
  private List<String> rareModLinesKo(RareItem rare) {
    List<String> lines = new ArrayList<>();
    // 엘드리치 임플리싯(총주교/포식자)을 맨 위에 — 게임에서도 임플리싯이 익스플리싯 위에 뜬다
    if (rare.implicitLinesKo() != null) {
      for (String line : rare.implicitLinesKo()) {
        lines.add("(엘드리치) " + line);
      }
    }
    for (int i = 0; i < rare.families().size(); i++) {
      lines.addAll(tierAt(rare.families().get(i), rare.fractionFor(i)).ko());
    }
    // rareItemText 가 공격 무기에 얹는 명중 전제도 함께 보여준다 — XML 엔 들어가는데 목록에만 없으면
    // 화면의 모드 합이 실제 계산과 달라진다(표시=실제 원칙).
    if (isAttackWeaponBase(rare.baseType())) {
      lines.add("명중 +2000 (시뮬 가정)");
    }
    return lines;
  }

  /** 어픽스 예산: 최적화기가 고른 순서(우선순위) 앞 essentialCount 개는 T1, 나머지는 fillerFraction 티어. */
  private static final int BUDGET_ESSENTIAL = 3;

  private static final double BUDGET_FILLER = 0.5;

  /**
   * 레어를 "현실적 제작" 변형으로 — 필수(우선순위 상위) 몇 개만 최상위 티어, 나머지는 중위 티어. 최적화기의 기본 가정("모든 모드 T1")은 과하게 낙관적이라, 보통
   * 확보하는 필수 옵션 수만 T1 로 잡은 값을 함께 보여주기 위한 것.
   */
  private RareItem budgetVariant(RareItem rare, int essentialCount, double fillerFraction) {
    if (rare.families().isEmpty()) {
      return rare;
    }
    List<Double> fractions = new ArrayList<>();
    for (int i = 0; i < rare.families().size(); i++) {
      fractions.add(i < essentialCount ? 0.0 : fillerFraction);
    }
    return new RareItem(
        rare.baseType(),
        rare.families(),
        0.0,
        fractions,
        rare.implicitLines(),
        rare.implicitLinesKo());
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

  /** XML 에 주입되는 표준 무기를 결과 목록용 항목으로 (무기 유니크가 채택된 경우 빈 값). */
  private java.util.Optional<PoeOptimizeResult.ItemPick> standardWeaponPick(
      PoeGem gem, Map<Slot, Equipped> items) {
    if (items.containsKey(Slot.WEAPON)) {
      return java.util.Optional.empty();
    }
    String base = standardWeapon(gem);
    if (base == null) {
      return java.util.Optional.empty();
    }
    String nameKo = poeBaseItemDataService.findByName(base).map(PoeBaseItem::nameKo).orElse(null);
    return java.util.Optional.of(
        new PoeOptimizeResult.ItemPick(
            Slot.WEAPON.pobName,
            Slot.WEAPON.ko,
            "RARE",
            null,
            base + " (시뮬 표준 무기)",
            (nameKo != null ? nameKo : base) + " (시뮬 표준 무기)",
            List.of("물리 피해 60-120 추가", "명중 +2000")));
  }

  private PoeOptimizeResult.ItemPick itemPick(Slot slot, Equipped equipped) {
    if (equipped.isUnique()) {
      PoeUniqueItem unique = equipped.unique();
      // XML 에 붙는 엘드리치 임플리싯을 표시에도 함께 — 없으면 화면의 모드 합이 실제 계산과 달라진다(표시=실제)
      List<String> lines = new ArrayList<>();
      for (String line : anointLineKo(slot)) {
        lines.add(line);
      }
      for (String line : eldritchForSlotKo(slot)) {
        lines.add("(엘드리치) " + line);
      }
      lines.addAll(uniqueModLines(unique));
      return new PoeOptimizeResult.ItemPick(
          slot.pobName, slot.ko, "UNIQUE", unique.slug(), unique.name(), unique.nameKo(), lines);
    }
    RareItem rare = equipped.rare();
    // 레어도 베이스 한글명을 채운다 — 유니크만 한글로 나오고 레어는 영문이라 목록이 뒤죽박죽이었다
    String baseNameKo =
        poeBaseItemDataService.findByName(rare.baseType()).map(PoeBaseItem::nameKo).orElse(null);
    List<String> rareLines = new ArrayList<>(anointLineKo(slot));
    rareLines.addAll(rareModLinesKo(rare));
    return new PoeOptimizeResult.ItemPick(
        slot.pobName, slot.ko, "RARE", null, rare.baseType(), baseNameKo, rareLines);
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
  /** 이 슬롯이 엘드리치 대상이면 이번 잡 키워드로 고른 총주교/포식자 임플리싯 영문 줄, 아니면 빈 목록. */
  private List<String> eldritchForSlot(Slot slot) {
    if (slot.modSlots.isEmpty()) {
      return List.of();
    }
    return eldritchImplicits(slot.modSlots.get(0), currentKeywords).en();
  }

  /** 아뮬렛에 실제로 얹은 도유 — 결과 표시용(표시=실제). 도유가 없거나 다른 슬롯이면 빈 목록. */
  private List<String> anointLineKo(Slot slot) {
    AnointPick pick = currentAnoint;
    return slot == Slot.AMULET && pick != null
        ? List.of("(도유) " + pick.nameKo() + " 할당")
        : List.of();
  }

  /** eldritchForSlot 의 한글판 — 결과 표시용. */
  private List<String> eldritchForSlotKo(Slot slot) {
    if (slot.modSlots.isEmpty()) {
      return List.of();
    }
    return eldritchImplicits(slot.modSlots.get(0), currentKeywords).ko();
  }

  /** 아뮬렛 도유 선택 결과 — 노터블 영문/한글 이름. */
  private record AnointPick(int nodeId, String name, String nameKo) {}

  /**
   * 도유 후보 — 도유 가능 노터블 **전부**(470개), id 오름차순(결정성).
   *
   * <p>키워드 점수 컷은 두 번 실패했다: ① 점수 1위 "공격적인 보루"는 조건절("방패를 들고 있는 동안")을 못 읽어 기여 0.00%, ② 상위 24 컷은 진짜 1위
   * "반사신경"(+2.65%, 점수순위 45)을 놓쳤고, EHP 3위 "영향력"(오라 효과 — 방어 오라 간접 증폭)은 점수 0 이라 컷을 아무리 넓혀도 안 잡힌다. 전수
   * 실평가만이 정답을 보장한다(전수 470건 실측 213초/HTTP·8병렬 — 인프로세스 풀은 더 빠르다, 1회성).
   */
  private List<PoeTreeGraphService.TreeNode> anointCandidates() {
    if (!poeTreeGraphService.hasData()) {
      return List.of();
    }
    return poeTreeGraphService.anointableNotables(); // 이미 id 오름차순
  }

  /**
   * 완성된 빌드 XML 의 아뮬렛에 도유 한 줄을 얹는다 — PoB 는 {@code Allocates <노터블>} 을 GrantedPassive 로 읽는다.
   *
   * <p>줄을 넣으면 그 아이템의 {@code Implicits: N} 도 함께 올려야 PoB 가 뒤 줄들을 임플리싯으로 오독하지 않는다. 아이템 블록 단위로만 손대므로 병렬
   * 평가에서 안전하다(공유 상태 없음).
   */
  private static String withAnoint(String xml, String notableName) {
    java.util.regex.Matcher slot =
        java.util.regex.Pattern.compile("<Slot name=\"Amulet\" itemId=\"(\\d+)\"").matcher(xml);
    if (!slot.find()) {
      return xml;
    }
    String itemId = slot.group(1);
    java.util.regex.Matcher item =
        java.util.regex.Pattern.compile(
                "<Item id=\"" + itemId + "\">(.*?)</Item>", java.util.regex.Pattern.DOTALL)
            .matcher(xml);
    if (!item.find()) {
      return xml;
    }
    String body = item.group(1);
    java.util.regex.Matcher implicits =
        java.util.regex.Pattern.compile("Implicits: (\\d+)\n").matcher(body);
    if (!implicits.find()) {
      return xml;
    }
    int count = Integer.parseInt(implicits.group(1)) + 1;
    String newBody =
        body.substring(0, implicits.start())
            + "Implicits: "
            + count
            + "\nAllocates "
            + notableName
            + "\n"
            + body.substring(implicits.end());
    return xml.substring(0, item.start(1)) + newBody + xml.substring(item.end(1));
  }

  private String uniqueItemText(PoeUniqueItem item) {
    return uniqueItemText(item, List.of());
  }

  /**
   * @param extraImplicits 유니크 자체 임플리싯 뒤에 덧붙일 줄(엘드리치). Implicits 개수에 함께 반영해야 PoB 가 옳게 읽는다.
   */
  private String uniqueItemText(PoeUniqueItem item, List<String> extraImplicits) {
    StringBuilder text = new StringBuilder();
    text.append("Rarity: UNIQUE\n")
        .append(item.name())
        .append("\n")
        .append(item.baseType())
        .append("\n");
    // 반경 라벨은 implicit 앞 **아이템 속성** 줄이다 — 빠지면 "…in Radius" 모드를 PoB 가 통째로 무시한다
    // (붉은 악몽 실측: 라벨 없이는 반경 내 저항 패시브가 방어 확률로 전혀 바뀌지 않았다)
    if (item.radius() != null && !item.radius().isBlank()) {
      text.append("Radius: ").append(item.radius()).append("\n");
    }
    List<String> extras = extraImplicits != null ? extraImplicits : List.of();
    text.append("Implicits: ").append(item.implicits().size() + extras.size()).append("\n");
    for (String implicit : item.implicits()) {
      text.append(implicit).append("\n");
    }
    for (String implicit : extras) {
      text.append(implicit).append("\n");
    }
    for (String explicit : item.explicits()) {
      text.append(explicit).append("\n");
    }
    // 타임리스(무궁한) 주얼은 시드 줄이 있어야 PoB 가 반경 패시브 변환을 계산한다(없으면 평범한 스탯 주얼).
    for (String line : timelessLines(item)) {
      text.append(line).append("\n");
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
      long durationMs,
      List<TreeJewel> jewels,
      /** 공격 스킬은 무기 없이 계산이 성립하지 않아 표준 무기를 가정한다(주문은 null). */
      String assumedWeapon,
      String assumedWeaponKo) {}

  /** 트리 에디터에서 꽂은 클러스터 주얼 (소켓 노드 id + 크기/노드수/작은패시브 스킬키) */
  /** notables = "1 Added Passive Skill is X" 로 얹을 클러스터 노터블 영문 이름들(순서 무관, PoB 가 정렬한다). */
  public record ClusterSpec(
      int socket,
      String sizeName,
      int nodeCount,
      String skillKey,
      List<String> notables,
      int socketCount) {}

  /** 트리 평가에 실제로 장착된 주얼 (요청 slug 중 존재하고 소켓이 할당된 것만) */
  public record TreeJewel(String slug, String name, String nameKo) {}

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
      Map<Integer, Integer> masteryEffects,
      Map<Integer, String> jewelSlugs,
      List<ClusterSpec> clusterSpecs,
      Map<Integer, String> tattooDns,
      Integer anointNodeId) {
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
    // 마스터리 효과는 게임 규칙상 트리 전체에서 1회만 고를 수 있다 — 같은 효과가 여러 노드에 들어오면
    // (손댄 URL·옛 링크) 첫 것만 남긴다. 안 그러면 같은 효과가 중복 적용돼 스탯이 부풀려진다.
    Map<Integer, Integer> effects = new LinkedHashMap<>();
    if (masteryEffects != null) {
      Set<Integer> seenEffects = new LinkedHashSet<>();
      for (Map.Entry<Integer, Integer> entry : masteryEffects.entrySet()) {
        if (seenEffects.add(entry.getValue())) {
          effects.put(entry.getKey(), entry.getValue());
        }
      }
    }
    // 주얼: 소켓 노드가 실제 할당돼 있고 slug 가 존재하는 유니크일 때만 장착한다.
    // (미할당 소켓에 주얼을 꽂으면 PoB 가 무시하는 게 아니라 스탯이 그대로 들어가 과대평가된다)
    Map<Integer, PoeUniqueItem> jewels = new LinkedHashMap<>();
    if (jewelSlugs != null) {
      for (Map.Entry<Integer, String> entry : jewelSlugs.entrySet()) {
        if (!nodes.contains(entry.getKey())) {
          continue;
        }
        // 값은 "slug" 또는 타임리스 지정 "slug:정복자:시드" — 정복자/시드는 반경 변환 결과를 바꾼다
        String[] spec = entry.getValue().split(":");
        poeUniqueDataService
            .findBySlug(spec[0].trim())
            .ifPresent(
                u ->
                    jewels.put(
                        entry.getKey(),
                        spec.length >= 2
                            ? withTimeless(
                                u, spec[1].trim(), spec.length >= 3 ? spec[2].trim() : null)
                            : u));
      }
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
            jewels,
            List.of(),
            effects,
            // 트리 평가는 최적화 잡과 독립이어야 한다 — 기본 가정(핀나클 보스 + 전투 버프)을 명시적으로 넘긴다
            "Pinnacle",
            true,
            List.of(),
            0);
    // 클러스터 주얼은 XML 조립 후 삽입한다 — PoB 는 주얼 아이템 문구로 서브트리를 만들므로
    // 생성 노드 id 가 nodes 에 있어도 주얼이 없으면 그 노드는 존재하지 않는 것으로 취급된다.
    xml = withClusterJewels(xml, clusterSpecs, nodes);
    xml = withTattoos(xml, tattooDns, nodes);
    xml = withAnointAmulet(xml, anointNodeId);
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
        result.durationMs(),
        jewels.values().stream().map(u -> new TreeJewel(u.slug(), u.name(), u.nameKo())).toList(),
        standardWeapon(gem),
        standardWeapon(gem) == null
            ? null
            : poeBaseItemDataService
                .findByName(standardWeapon(gem))
                .map(PoeBaseItem::nameKo)
                .orElse(null));
  }
}
