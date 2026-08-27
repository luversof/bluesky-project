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
import java.util.HashMap;
import java.util.HashSet;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
  // 비교 대상(ninja 대표 실빌드·중앙값)이 전부 레벨 100 이라 전제를 맞춘다 — 90 으로 두면 패시브 10점을
  // 덜 쓴 빌드를 100 레벨 빌드와 견주게 되어 "메타 하회"가 구조적으로 나온다(트리 잎 제거 로직 주석의
  // "정공법은 LEVEL 파리티(실빌드 L100 = +10pt)" 가 가리키던 그 지점).
  private static final int LEVEL = 100;
  // 패시브 포인트 예산 — 레벨 100 기준 실제 획득량(레벨업 99 + 퀘스트 24 = 123).
  private static final int POINT_BUDGET = 123;
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

  /**
   * 주얼 단계에서 평가할 최대 소켓 수 (가장 싸게 닿는 것부터) — 비용 제한.
   *
   * <p>⚠ 8 로 올리고 예약을 16 으로 늘려 봤지만 **오히려 -18.1%**(RF 1,517,354 → 1,242,819)였다. 소켓은 그래도 5개만 잡혔고(상한이
   * 병목이 아니다) 예약 포인트만 트리에서 빠져나갔다. 병목은 "닿는 소켓의 한계 이득"이지 이 상한이 아니다.
   */
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
   * 생명력 예약 하한 — Blood Magic 류(마나 0, 오라가 생명력을 예약)에선 마나 신호가 늘 통과라 무의미하다. 인게임은 미예약 생명력을 0 이하로 만드는 예약을
   * 허용하지 않으므로 LifeUnreserved 에 같은 검사를 건다. CI 빌드(Life=1, 예약 없으면 LifeUnreserved=1)가 통과하도록 하한은 1 미만.
   */
  private static final double MIN_UNRESERVED_LIFE = 0.5;

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
    FAILED,
    CANCELLED
  }

  /** 사용자 중지 요청으로 잡을 조기 종료할 때 던지는 신호(정상 취소 — FAILED 와 구분). */
  private static class JobCancelledException extends RuntimeException {
    JobCancelledException() {
      super("사용자 중지");
    }
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
  private final PoeEssenceDataService poeEssenceDataService;
  private final PoeTradeStatDataService poeTradeStatDataService;
  private final PoeEldritchDataService poeEldritchDataService;
  private final PoeModDataService poeModDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeClusterJewelDataService poeClusterJewelDataService;
  private final PoeFoulbornDataService poeFoulbornDataService;
  private final PoeSkillWeaponDataService poeSkillWeaponDataService;
  private final PoeTattooDataService poeTattooDataService;
  private final PoeModTranslateService poeModTranslateService;

  /** 아틀라스 패시브 이름(영문→한글) 원본 — 아틀라스 트리는 API 서비스로 안 올라와 파일에서 직접 읽는다. */
  private final Path atlasTreeFile;

  private final Path resultFile;
  // 완료된 결과를 최근 N건까지 남기는 이력 디렉터리(파일명 = 저장 시각 epochMs). optimize-last.json 은 그대로 "마지막 1건" 용도.
  private final Path historyDir;
  // poe.ninja 시드 파일(엔진 벤치는 같은 폴더) — 데이터 갱신 후 reloadNinja() 로 다시 읽는다.
  private final Path ninjaSeedFile;
  // 최근 결과 보관 상한 — 오래된 것부터 정리. 결과 JSON 한 건이 크지 않아 30건이면 화면 목록으로 충분.
  private static final int HISTORY_LIMIT = 30;
  private final String treeVersion;
  private final int parallelism;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger phaseDone = new AtomicInteger();
  private final AtomicInteger evalCount = new AtomicInteger();

  // 사용자 중지 — 잡 스레드 참조 + 취소 플래그. 페이즈/라운드 진입 시 checkCancel() 로 조기 종료.
  private volatile Thread jobThread;
  private volatile boolean cancelRequested = false;

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
    checkCancel(); // 페이즈 경계마다 취소 확인 — 사용자 중지 시 여기서 조기 종료
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
      PoeEssenceDataService poeEssenceDataService,
      PoeEldritchDataService poeEldritchDataService,
      PoeModDataService poeModDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeClusterJewelDataService poeClusterJewelDataService,
      PoeFoulbornDataService poeFoulbornDataService,
      PoeSkillWeaponDataService poeSkillWeaponDataService,
      PoeTattooDataService poeTattooDataService,
      PoeModTranslateService poeModTranslateService,
      PoeTradeStatDataService poeTradeStatDataService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir,
      @Value("${poe.sim.tree-version:3_29}") String treeVersion,
      @Value("${poe.sim.parallelism:0}") int parallelism,
      @Value("${poe.pob.src-dir:${user.home}/.poe-gamedata/work/pob-src}") String pobSourceDir) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poePobEngineService = poePobEngineService;
    this.poeTreeGraphService = poeTreeGraphService;
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeEssenceDataService = poeEssenceDataService;
    this.poeEldritchDataService = poeEldritchDataService;
    this.poeModDataService = poeModDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeClusterJewelDataService = poeClusterJewelDataService;
    this.poeFoulbornDataService = poeFoulbornDataService;
    this.poeSkillWeaponDataService = poeSkillWeaponDataService;
    this.poeModTranslateService = poeModTranslateService;
    this.poeTattooDataService = poeTattooDataService;
    this.poeTradeStatDataService = poeTradeStatDataService;
    this.resultFile = Path.of(dataDir, "sim", "optimize-last.json");
    this.atlasTreeFile = Path.of(dataDir, "atlas-tree.json");
    this.historyDir = Path.of(dataDir, "sim", "history");
    this.treeVersion = treeVersion;
    this.pobSourceDir = pobSourceDir;
    // 병렬성 미지정(≤0)이면 엔진 워커 풀 크기와 일치시킨다(executor 스레드마다 워커 1개 배정 →
    // 스레드가 워커를 못 잡고 대기하는 낭비 없음). 워커 풀은 코어/RAM 자동 산정. 다른 PC 이식성.
    this.parallelism = parallelism > 0 ? parallelism : poePobEngineService.poolSize();
    loadLastResult();
    this.ninjaSeedFile = Path.of(dataDir, "ninja", "ninja-archetypes.json");
    // 감시자의 눈 모드 풀은 **데이터 루트**에 있다(ninja/ 아래가 아니다 — 처음에 seed 파일 부모로 잡아 못 찾았다)
    this.watchersEyeFile = Path.of(dataDir, "watchers-eye.json");
    loadNinjaSeeds(ninjaSeedFile);
  }

  /**
   * poe.ninja 시드·엔진 벤치를 다시 읽는다 — 데이터 갱신이 fetch-ninja-builds/calibrate-archetypes 로 파일을 새로 만들므로, 재시작
   * 없이 그 결과가 판정에 반영되게 한다(빠지면 갱신해도 옛 시드로 계속 비교).
   */
  public void reloadNinja() {
    loadNinjaSeeds(ninjaSeedFile);
  }

  /**
   * poe.ninja 실빌드 아키타입 시드 로드 — balanced 목표의 생존 목표치를 실측 중앙값으로. 파일 없으면 조용히 스킵(무회귀). 산출:
   * tools/poe-extract/fetch-ninja-builds.mjs.
   */
  private void loadNinjaSeeds(Path file) {
    if (!Files.exists(file)) {
      logger.info("poe.ninja 시드 없음(정적 floor 사용): {}", file);
      return;
    }
    try {
      JsonNode root = JsonMapper.builder().build().readTree(Files.readString(file));
      // 재로드 대비 초기화 — put 병합만 하면 지난 리그의 아키타입 키가 남아 새 시드와 섞인다.
      //   (읽기·파싱 성공 후에 비워야 깨진 파일 한 번에 멀쩡한 시드를 잃지 않는다)
      ninjaSeedByKey.clear();
      ninjaSeedBySkill.clear();
      ninjaBenchByKey.clear();
      ninjaBenchBySkill.clear();
      ninjaFacetNodeByKey.clear();
      ninjaFacetNodeBySkill.clear();
      // (전직|스킬) 정확 매칭 시드
      JsonNode arr = root.get("archetypes");
      // 특화 판정 기준선 = 전체 아키타입의 **컬럼별 중앙값**(리그별 갱신). ⚠ 단순 평균 금지 — 중앙값이라 개별 이상치에
      //   강건하고, 여기에 더해 **저표본(sample<SPECIALIZE_MIN_SAMPLE) 아키타입은 제외**해 "값이 너무 다른" 소수 노이즈를
      //   거른다(사용자 요구: 가중·이상치 필터). 개별 벤치의 lean/specializations 산정 전에 먼저 계산.
      if (arr != null && arr.isArray()) {
        ninjaColMedian.clear();
        for (String col : SPECIALIZE_COLS) {
          java.util.List<Double> vals = new java.util.ArrayList<>();
          for (JsonNode a : arr) {
            if (a.path("sample").asInt() < SPECIALIZE_MIN_SAMPLE) {
              continue; // 저표본 아키타입 제외(이상치 필터)
            }
            double v = a.path(col).asDouble();
            if (v > 0) {
              vals.add(v);
            }
          }
          if (!vals.isEmpty()) {
            java.util.Collections.sort(vals);
            ninjaColMedian.put(col, vals.get(vals.size() / 2));
          }
        }
        ninjaGlobalMedianDps = ninjaColMedian.getOrDefault("medianDPS", 2_500_000d);
        ninjaGlobalMedianEhp = ninjaColMedian.getOrDefault("medianEHP", 70_000d);
      }
      if (arr != null && arr.isArray()) {
        for (JsonNode a : arr) {
          String asc = a.path("ascendancy").asText();
          String skill = a.path("mainSkill").asText();
          NinjaSeed seed = seedOf(a);
          if (seed == null || skill == null || skill.isEmpty() || asc.isEmpty()) {
            continue;
          }
          ninjaSeedByKey.put(asc + "|" + skill, seed);
          ninjaBenchByKey.put(asc + "|" + skill, benchOf(a));
          if (a.path("facets").isObject()) {
            ninjaFacetNodeByKey.put(asc + "|" + skill, a.path("facets"));
          }
        }
      }
      // 스킬 단위 폴백 시드(전 전직 통합 실median — 자동전직 잡, 전직별 저표본 노이즈 회피).
      JsonNode skillArr = root.get("skillArchetypes");
      if (skillArr != null && skillArr.isArray()) {
        for (JsonNode a : skillArr) {
          String skill = a.path("mainSkill").asText();
          NinjaSeed seed = seedOf(a);
          if (seed == null || skill == null || skill.isEmpty()) {
            continue;
          }
          ninjaSeedBySkill.put(skill, seed);
          ninjaBenchBySkill.put(skill, benchOf(a));
          if (a.path("facets").isObject()) {
            ninjaFacetNodeBySkill.put(skill, a.path("facets"));
          }
        }
      }
      logger.info(
          "poe.ninja 시드 로드: {} 아키타입키 / {} 스킬", ninjaSeedByKey.size(), ninjaSeedBySkill.size());
    } catch (Exception e) {
      logger.warn("poe.ninja 시드 로드 실패: {}", file, e);
    }
    // 감시자의 눈 모드 풀 — PoB 가 코드로 생성하는 유니크라 우리 유니크 데이터엔 없다(Special/WatchersEye.lua).
    //   "…while affected by <오라>" 형태라 **지금 낀 오라**에 맞는 모드만 골라 합성해야 값이 산다.
    try {
      Path weFile = watchersEyeFile;
      if (Files.exists(weFile)) {
        JsonNode we = JsonMapper.builder().build().readTree(Files.readString(weFile));
        watchersEyeMods.clear();
        for (JsonNode mod : we.path("mods")) {
          String aura = mod.path("aura").asText(null);
          if (aura == null || aura.isBlank()) {
            continue;
          }
          watchersEyeMods
              .computeIfAbsent(aura, k -> new ArrayList<>())
              .add(new WatchersEyeMod(mod.path("en").asText(), mod.path("ko").asText()));
        }
        logger.info("감시자의 눈 모드 풀 로드: 오라 {}종", watchersEyeMods.size());
      }
    } catch (Exception e) {
      logger.warn("감시자의 눈 모드 풀 로드 실패", e);
    }
    // 엔진 벤치(calibrate-archetypes.mjs) — 대표 실빌드를 **우리 엔진으로 재계산한** 지표 정합 정답값.
    //   belowMeta 판정이 ninja 표기 지표 대신 이걸 우선 사용(gross/net 불일치 우회). 없으면 기존 경로.
    try {
      Path ebFile = file.getParent().resolve("ninja-engine-bench.json");
      if (Files.exists(ebFile)) {
        JsonNode eb = JsonMapper.builder().build().readTree(Files.readString(ebFile));
        ninjaEngineBench.clear();
        for (Map.Entry<String, JsonNode> e : eb.properties()) {
          ninjaEngineBench.put(e.getKey(), e.getValue());
        }
        logger.info("poe.ninja 엔진 벤치 로드: {} 아키타입(지표 정합 정답값)", ninjaEngineBench.size());
      }
    } catch (Exception e) {
      logger.warn("poe.ninja 엔진 벤치 로드 실패(기존 경로 사용): {}", file, e);
    }
    // 원시 빌드(캐릭터 단위) — 멀티스킬 **조합** 벤치/시드용. 사용자 요구: RF+화염덫 선택 시 두 스킬을 모두 쓰는
    //   캐릭터만으로 집계해야지, 첫 스킬 아키타입(RF 전체)을 보여주면 안 된다. 사전 집계 파일엔 조합 키가 없으므로
    //   원시 빌드를 들고 있다가 요청 시 조합 필터→이상치 제거→중앙값을 즉석 계산한다(~3.4k행, 메모리/시간 무시 가능).
    try {
      Path buildsFile = file.getParent().resolve("ninja-builds.json");
      if (Files.exists(buildsFile)) {
        JsonNode broot = JsonMapper.builder().build().readTree(Files.readString(buildsFile));
        JsonNode barr = broot.get("builds");
        List<JsonNode> list = new ArrayList<>();
        if (barr != null && barr.isArray()) {
          barr.forEach(list::add);
        }
        this.ninjaBuilds = list;
        logger.info("poe.ninja 원시 빌드 로드: {} 캐릭터(조합 벤치용)", list.size());
      }
    } catch (Exception e) {
      logger.warn("poe.ninja 원시 빌드 로드 실패(조합 벤치 비활성): {}", file, e);
    }
  }

  /** 아키타입 JSON 노드 → NinjaSeed(생존 목표치). 목표 max hit·EHP 둘 다 없으면 null. */
  private NinjaSeed seedOf(JsonNode a) {
    // 흔한 4유형(물리+3원소) 최대피격의 최솟값 = balancedSurvival 의 weakestCommon 정의와 동일(카오스 제외).
    double target =
        min4nonzero(
            a.path("medianPhysicalMax").asDouble(),
            a.path("medianFireMax").asDouble(),
            a.path("medianColdMax").asDouble(),
            a.path("medianLightningMax").asDouble());
    double ehp = a.path("medianEHP").asDouble();
    if (target <= 0 && ehp <= 0) {
      return null;
    }
    // 상위 키스톤(count>=2, 최대 3) — 실빌드가 흔히 쓰는 키스톤(방어 포함)을 트리 후보 시드로.
    List<String> keystones = new ArrayList<>();
    JsonNode ks = a.path("topKeystones");
    if (ks.isArray()) {
      for (JsonNode k : ks) {
        if (keystones.size() >= 3) {
          break;
        }
        String name = k.path("name").asText();
        if (name != null && !name.isEmpty() && k.path("count").asInt() >= 2) {
          keystones.add(name);
        }
      }
    }
    return new NinjaSeed(
        target,
        ehp,
        a.path("sample").asInt(),
        keystones,
        (int) a.path("medianFireRes").asDouble(),
        (int) a.path("medianColdRes").asDouble(),
        (int) a.path("medianLightningRes").asDouble(),
        (int) a.path("medianChaosRes").asDouble(),
        a.path("medianLifeRegen").asDouble(),
        (int) a.path("medianSuppress").asDouble(),
        (int) a.path("medianSpellBlock").asDouble());
  }

  /** 아키타입 JSON 노드 → 벤치마크(표시용 전체 중앙값 프로파일). */
  private ArchetypeBenchmark benchOf(JsonNode a) {
    return new ArchetypeBenchmark(
        a.path("ascendancy").asText(),
        a.path("mainSkill").asText(),
        a.path("sample").asInt(),
        (long) a.path("medianLife").asDouble(),
        (long) a.path("medianES").asDouble(),
        (long) a.path("medianEHP").asDouble(),
        (long) a.path("medianDPS").asDouble(),
        (long) a.path("medianPhysicalMax").asDouble(),
        (long) a.path("medianFireMax").asDouble(),
        (long) a.path("medianColdMax").asDouble(),
        (long) a.path("medianLightningMax").asDouble(),
        (long) a.path("medianChaosMax").asDouble(),
        (int) a.path("medianFireRes").asDouble(),
        (int) a.path("medianColdRes").asDouble(),
        (int) a.path("medianLightningRes").asDouble(),
        (int) a.path("medianChaosRes").asDouble(),
        (long) a.path("medianLifeRegen").asDouble(),
        (long) a.path("medianArmour").asDouble(),
        (long) a.path("medianEvasion").asDouble(),
        (int) a.path("medianBlock").asDouble(),
        (int) a.path("medianSuppress").asDouble(),
        (long) a.path("medianLowestMax").asDouble(),
        (long) a.path("medianWard").asDouble(),
        (long) a.path("medianMana").asDouble(),
        (int) a.path("medianItemRarity").asDouble(),
        (int) a.path("medianMovementSpeed").asDouble(),
        (int) a.path("medianSpellBlock").asDouble(),
        (int) a.path("medianSpellDodge").asDouble(),
        (int) a.path("medianPhysTakenAs").asDouble(),
        (int) a.path("medianStr").asDouble(),
        (int) a.path("medianDex").asDouble(),
        (int) a.path("medianInt").asDouble(),
        (int) a.path("medianEnduranceCharges").asDouble(),
        (int) a.path("medianFrenzyCharges").asDouble(),
        (int) a.path("medianPowerCharges").asDouble(),
        (int) a.path("medianClusterJewels").asDouble(),
        (int) a.path("medianLargeCluster").asDouble(),
        (int) a.path("medianMediumCluster").asDouble(),
        (int) a.path("medianSmallCluster").asDouble(),
        (int) a.path("medianUniqueEquip").asDouble(),
        (int) a.path("medianMirroredItems").asDouble(),
        (int) a.path("medianMirroredWeapons").asDouble(),
        (int) a.path("medianMirroredArmours").asDouble(),
        topNames(a.path("topKeystones"), 6),
        topNames(a.path("topCoSkills"), 6),
        topNames(a.path("topMasteries"), 6),
        leanOf(a.path("medianDPS").asDouble(), a.path("medianEHP").asDouble()),
        specializationsOf(a),
        skillDpsOf(a.path("skillDps")),
        a.path("facets").path("total").asLong(),
        facetsOf(a.path("facets").path("groups")));
  }

  /** 패싯 이름 사전(영문 → 한글) — 문신 + 트리 노드(키스톤·노터블·마스터리). 최초 1회만 만든다. */
  private volatile Map<String, String> facetNameKoCache;

  private Map<String, String> facetNameKoMap() {
    Map<String, String> cached = facetNameKoCache;
    if (cached != null) {
      return cached;
    }
    Map<String, String> map = new LinkedHashMap<>();
    for (PoeTattooDataService.Tattoo t : poeTattooDataService.all()) {
      if (t.name() != null && t.nameKo() != null && !t.nameKo().isBlank()) {
        map.putIfAbsent(t.name(), t.nameKo());
      }
    }
    // 키스톤·노터블(도유 포함)·마스터리는 트리 데이터에 한글명이 있다 — 패싯의 keypassives/anointed 차원이 여기서 잡힌다.
    List<PoeTreeGraphService.TreeNode> treeNodes = new ArrayList<>();
    treeNodes.addAll(poeTreeGraphService.searchCandidates());
    treeNodes.addAll(poeTreeGraphService.anointableNotables());
    treeNodes.addAll(poeTreeGraphService.masteryNodes());
    for (PoeTreeGraphService.TreeNode n : treeNodes) {
      if (n.name() != null && n.nameKo() != null && !n.nameKo().isBlank()) {
        map.putIfAbsent(n.name(), n.nameKo());
      }
    }
    // 마스터리 패싯은 **노드 이름이 아니라 고른 효과의 스탯 문장**이 항목으로 온다
    // (예: "Regenerate 1 Life per second for each 1% Uncapped Fire Resistance").
    // 이름만 사전에 넣으면 이 차원은 계속 영문으로 남는다 — 효과별 stats↔statsKo 도 함께 넣는다.
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.masteryNodes()) {
      if (n.masteryEffects() == null) {
        continue;
      }
      for (PoeTreeGraphService.MasteryEffect effect : n.masteryEffects()) {
        List<String> en = effect.stats();
        List<String> ko = effect.statsKo();
        if (en == null || ko == null) {
          continue;
        }
        for (int i = 0; i < en.size() && i < ko.size(); i++) {
          if (en.get(i) != null && ko.get(i) != null && !ko.get(i).isBlank()) {
            map.putIfAbsent(en.get(i), ko.get(i));
          }
        }
      }
    }
    // 아틀라스 패시브 패싯은 아틀라스 트리 노드 이름으로 온다(예: "Mounting Modifiers").
    // 이 트리는 게이트 화면 전용이라 API 서비스에 안 올라와 있어, 파일에서 이름만 뽑아 쓴다.
    if (Files.exists(atlasTreeFile)) {
      try {
        JsonNode atlas = JsonMapper.builder().build().readTree(Files.readString(atlasTreeFile));
        for (JsonNode node : atlas.path("nodes")) {
          String en = node.path("name").asText(null);
          String ko = node.path("nameKo").asText(null);
          if (en != null && ko != null && !ko.isBlank()) {
            map.putIfAbsent(en, ko);
          }
        }
      } catch (Exception e) {
        logger.warn("아틀라스 트리 이름 사전 로드 실패: {}", atlasTreeFile, e);
      }
    }
    facetNameKoCache = map;
    return map;
  }

  /**
   * 패싯 항목의 한국어 표기 — ① 이름 사전(문신·트리 노드) ② 문장형은 모드 번역 사전. 못 찾으면 null(영문 유지).
   *
   * <p>판테온·산적·무기 구성·장비 등은 우리 데이터에 한글 원본이 없어 영문으로 남는다 — 억지 번역보다 정직하다.
   */
  private String facetNameKo(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    String direct = facetNameKoOne(name);
    if (direct != null) {
      return direct;
    }
    // poe.ninja 는 마스터리 효과의 여러 스탯을 ", " 로 이어 **한 항목**으로 보낸다
    // (실측: "+12% to Fire Damage over Time Multiplier, 50% increased Ignite Duration on you").
    // 통짜로는 사전에 없으니 조각별로 해석하되, **전부 성공할 때만** 합쳐 쓴다 — 한 조각만 번역되면
    // 한글·영문이 섞인 줄이 나와 오히려 읽기 나쁘다.
    if (name.contains(", ")) {
      String[] parts = name.split(", ");
      if (parts.length > 1) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
          String one = facetNameKoOne(part.trim());
          if (one == null) {
            return null;
          }
          if (joined.length() > 0) {
            joined.append(", ");
          }
          joined.append(one);
        }
        return joined.toString();
      }
    }
    return null;
  }

  /** 단일 문자열 해석 — 이름 사전 → 모드 번역 사전. 못 찾으면 null. */
  private String facetNameKoOne(String name) {
    String byName = facetNameKoMap().get(name);
    if (byName != null) {
      return byName;
    }
    String translated = poeModTranslateService.translate(name);
    return translated != null && !translated.equals(name) ? translated : null;
  }

  /**
   * 패싯 groups 오브젝트 → {그룹명: [{name,count,nameKo}]}. 없으면 null(구 데이터).
   *
   * <p>한글 로케일인데 이 섹션만 영문으로 남아 있었다(실측: 마스터리·룬크래프트·문신 전부 영문). 문신은 nameKo 가 데이터에 있고, 스탯 문장형
   * 항목(마스터리·룬크래프트·아틀라스 등)은 모드 번역 사전(804개)이 그대로 잡는다 — 있는 걸 안 쓰고 있던 셈이라 여기서 해석해 내려보낸다.
   */
  private Map<String, List<FacetEntry>> facetsOf(JsonNode groups) {
    if (groups == null || !groups.isObject() || groups.isEmpty()) {
      return null;
    }
    Map<String, List<FacetEntry>> out = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> e : groups.properties()) {
      List<FacetEntry> list = new ArrayList<>();
      for (JsonNode item : e.getValue()) {
        String name = item.path("name").asText();
        list.add(new FacetEntry(name, item.path("count").asInt(), facetNameKo(name)));
      }
      if (!list.isEmpty()) {
        out.put(e.getKey(), list);
      }
    }
    return out.isEmpty() ? null : out;
  }

  /**
   * P3 메타 기준선 게이트 — balanced 잡의 최종 DPS·EHP 가 같은 (전직×스킬조합) 실빌드 중앙값을 <b>둘 다</b> 하회하면 true(지배당함 — 결과
   * 화면 경고). 비-balanced/벤치 없음이면 null(판정 보류). "실빌드보다 나은 조합" 목적의 하한 신호.
   */
  /**
   * 메타 하회 판정의 여유 폭 — 대표는 **한 명**이라 몇 %差 는 표본 잡음이다. 실측: 번개 화살 4,989,100 vs 대표 5,223,102(**95.5%**)
   * 인데 경고가 떴다 — 사용자에겐 "당신 빌드가 실빌드보다 못하다" 로 읽히지만 실제로는 ninja 중앙값의 156% 다. 5% 이내는 동급으로 본다(그 이상 벌어질 때만
   * 경고).
   */
  private static final double BELOW_META_MARGIN = 0.95;

  private Boolean belowMetaVerdict(
      String objective, PoeGem gem, String ascendancy, Map<String, Double> finalValues) {
    if (!"balanced".equals(objective)) {
      return null;
    }
    List<String> names = new ArrayList<>();
    names.add(gem.name());
    for (PoeGem g : additionalSkills) {
      if (isDamageSkill(g) && !names.contains(g.name())) {
        names.add(g.name());
      }
    }
    double myDps = effectiveDps(finalValues);
    double myEhp = finalValues.getOrDefault("TotalEHP", 0d);
    // 엔진 벤치 우선 — 대표 실빌드를 우리 엔진으로 재계산한 값이라 지표가 정합(ninja 표기는 gross/표본 지표라
    //   과대·과소가 섞임). 없으면 기존 ninja 중앙값 경로.
    JsonNode eb = null;
    if (ascendancy != null && !ascendancy.isEmpty()) {
      // CI 혼재 아키타입(PB 등)은 서브그룹 벤치(|ci, |life)가 있으면 우리 최종 빌드의 체계(Life<=1=CI)와
      //   같은 서브그룹 대표와 비교 — 단일 중앙값 대표는 혼재 모집단에서 어느 쪽도 대표하지 못한다.
      String benchBase = ascendancy + "|" + gem.name();
      String subgroup = finalValues.getOrDefault("Life", 0d) <= 1d ? "|ci" : "|life";
      eb = ninjaEngineBench.get(benchBase + subgroup);
      if (eb != null) {
        log("메타 벤치: 서브그룹 대표(" + benchBase + subgroup + ")와 비교");
      } else {
        eb = ninjaEngineBench.get(benchBase);
      }
    }
    // 벤치가 스스로 "못 믿겠다"고 표시한 항목은 판정에서 뺀다(calibrate 의 reliable=false).
    //   대표 1인 빌드는 발라 버스트가 메인이거나 트리거 그룹이 잡혀 자릿수가 어긋나는 일이 잦다
    //   (실측: RF 중앙값 대비 11.9x, 혼의 균열|ci 0.02x). 못 믿을 기준으로 "메타 하회" 를 찍으면
    //   사용자에게도 거짓말이고 개선 방향도 틀어진다 → 이럴 땐 아래 ninja 중앙값 경로로 내려간다.
    if (eb != null && eb.has("reliable") && !eb.path("reliable").asBoolean()) {
      log("메타 벤치: 신뢰도 낮음(중앙값 대비 " + eb.path("ratio").asDouble() + "x) — 중앙값 기준으로 판정");
      eb = null;
    }
    if (eb != null && eb.path("dps").asDouble() > 0 && eb.path("ehp").asDouble() > 0) {
      boolean below =
          myDps < eb.path("dps").asDouble() * BELOW_META_MARGIN
              && myEhp < eb.path("ehp").asDouble() * BELOW_META_MARGIN;
      if (below) {
        log(
            String.format(
                "⚠ 메타 하회(엔진 기준): DPS %,.0f < %,.0f · EHP %,.0f < %,.0f — 대표 실빌드(동일 엔진 재계산)가 공격·생존 모두 우세",
                myDps, eb.path("dps").asDouble(), myEhp, eb.path("ehp").asDouble()));
      }
      return below;
    }
    ArchetypeBenchmark bench = ninjaComboBenchmark(ascendancy, names);
    if (bench == null || bench.dps() <= 0 || bench.ehp() <= 0) {
      return null;
    }
    boolean below =
        myDps < bench.dps() * BELOW_META_MARGIN && myEhp < bench.ehp() * BELOW_META_MARGIN;
    if (below) {
      log(
          String.format(
              "⚠ 메타 하회: DPS %,.0f < %,d · EHP %,.0f < %,d — 실빌드 중앙값이 공격·생존 모두 우세",
              myDps, bench.dps(), myEhp, bench.ehp()));
    }
    return below;
  }

  /** 조합 벤치 skillDps 배열 → SkillDpsEntry 목록. 없으면 null(단일 스킬 벤치). */
  private static List<SkillDpsEntry> skillDpsOf(JsonNode arr) {
    if (arr == null || !arr.isArray() || arr.isEmpty()) {
      return null;
    }
    List<SkillDpsEntry> out = new ArrayList<>();
    for (JsonNode s : arr) {
      out.add(
          new SkillDpsEntry(
              s.path("name").asText(), (long) s.path("dps").asDouble(), s.path("count").asInt()));
    }
    return out;
  }

  private static List<String> topNames(JsonNode arr, int n) {
    List<String> out = new ArrayList<>();
    if (arr != null && arr.isArray()) {
      for (JsonNode k : arr) {
        if (out.size() >= n) {
          break;
        }
        String name = k.path("name").asText();
        if (name != null && !name.isEmpty()) {
          out.add(name);
        }
      }
    }
    return out;
  }

  /** poe.ninja 실빌드 벤치마크 조회 — 정확 키(전직|스킬) 우선, 없으면 스킬 폴백. 없으면 null. */
  public ArchetypeBenchmark ninjaBenchmark(String ascendancy, String skill) {
    if (skill == null || skill.isEmpty()) {
      return null;
    }
    ArchetypeBenchmark b = null;
    if (ascendancy != null && !ascendancy.isEmpty()) {
      b = ninjaBenchByKey.get(ascendancy + "|" + skill);
    }
    return b != null ? b : ninjaBenchBySkill.get(skill);
  }

  // 원시 빌드(캐릭터 단위, ninja-builds.json) — 조합 벤치 즉석 집계용. loadNinjaSeeds 에서 채운다.
  private volatile List<JsonNode> ninjaBuilds = List.of();

  // 패싯 원본 노드(사이드바 집계) — 조합 벤치가 메인 스킬의 패싯을 물려받을 때 사용. loadNinjaSeeds 에서 채운다.
  private final Map<String, JsonNode> ninjaFacetNodeByKey = new HashMap<>();
  private final Map<String, JsonNode> ninjaFacetNodeBySkill = new HashMap<>();

  // 엔진 벤치("전직|스킬" → {dps,ehp,netRegen,life}) — 대표 실빌드의 우리-엔진 재계산값(지표 정합 정답).
  private final Map<String, JsonNode> ninjaEngineBench = new HashMap<>();

  /** 감시자의 눈 모드 — 오라 이름 → 그 오라 조건부 모드 목록. */
  private record WatchersEyeMod(String en, String ko) {}

  private final Map<String, List<WatchersEyeMod>> watchersEyeMods = new HashMap<>();

  private final Path watchersEyeFile;

  // ── P1 메타 마스터리 웜스타트 — 아키타입 패싯에서 채택률 META_MASTERY_ADOPTION 이상인 마스터리 효과의
  //   정규화 텍스트. balanced 잡의 setSurvivalTargets 에서만 채워짐 → dps/ehp 잡(기준선 포함)은 항상 빈 집합.
  private volatile Set<String> metaMasteries = Set.of();
  private static final double META_MASTERY_ADOPTION = 0.4;
  // 메타 마스터리용 트리 포인트 예약 — 마스터리 단계가 경로(1~5pt) 후보를 시험할 여유. 메타 없으면 0.
  private static final int META_MASTERY_RESERVE = 8;

  /** 자동 클러스터 예약(balanced) — Large 1개 표준 비용(경로 ~2 + 소켓 1 + 8노드). 미채택 시 환급 greedy 로 소진. */
  private static final int CLUSTER_RESERVE = 11;

  // ── P1② 메타 무기 구성 — 패싯 weaponmode 최다(점유율 50%+)로 무기 후보를 제약. balanced 전용, 잡마다 리셋.
  //   실측: RF 치프틴 실빌드 96%가 Mace/Shield 인데 시뮬은 자유 선택이라 방패 방어층(막기 75%·방어도 12k)이 통째 빠짐.
  private volatile Set<String> metaWeaponClasses = Set.of(); // 허용 무기 itemClass(빈=제약 없음)
  private volatile boolean metaOffhandShield = false;

  // 추가 스킬(화염덫 등) 전용 보조젬 — slug → 선발 서포트(1b 패스). 잡별 상태, start() 리셋.
  private final Map<String, List<PoeGem>> additionalSkillSupports =
      new java.util.concurrent.ConcurrentHashMap<>();

  /** poe.ninja weaponmode 라벨 → 허용 무기 itemClass 집합. 미지 라벨은 빈 집합(제약 없음 폴백). */
  private static Set<String> weaponModeClasses(String mode) {
    String w = mode.startsWith("Dual ") ? mode.substring(5) : mode.split(" / ")[0].trim();
    return switch (w) {
      case "Mace" -> Set.of("One Hand Mace", "Sceptre"); // ninja 는 셉터를 Mace 로 묶는다(RF 치프틴 실사용은 셉터)
      case "Sword" -> Set.of("One Hand Sword", "Thrusting One Hand Sword");
      case "Axe" -> Set.of("One Hand Axe");
      case "Dagger" -> Set.of("Dagger", "Rune Dagger");
      case "Claw" -> Set.of("Claw");
      case "Wand" -> Set.of("Wand");
      case "Staff" -> Set.of("Staff", "Warstaff");
      case "Bow" -> Set.of("Bow");
      case "Two-Handed Mace", "Two Hand Mace" -> Set.of("Two Hand Mace");
      case "Two-Handed Sword", "Two Hand Sword" -> Set.of("Two Hand Sword");
      case "Two-Handed Axe", "Two Hand Axe" -> Set.of("Two Hand Axe");
      default -> Set.of();
    };
  }

  // ── P1③ 메타 판테온 — 패싯 pantheon 최다(메이저/마이너 각 1, 점유율 15%+)를 PoB Config 로 반영.
  //   시뮬이 판테온을 아예 미모델하던 갭. balanced 전용, 잡마다 리셋. ""=미설정(Config 미출력 → 기준선 불변).
  private volatile String metaPantheonMajor = "";
  private volatile String metaPantheonMinor = "";
  private static final Set<String> PANTHEON_MAJORS =
      Set.of("Brine King", "The Brine King", "Lunaris", "Solaris", "Arakaali");

  /** ninja 판테온 라벨 → PoB Config id ("Brine King"→TheBrineKing, 그 외 공백 제거). */
  private static String pantheonId(String label) {
    return label.contains("Brine King") ? "TheBrineKing" : label.replace(" ", "");
  }

  // 무기 itemClass → 유니크 category(굵은 분류) — 유니크 후보 필터용
  private static final Map<String, String> WEAPON_CLASS_TO_CATEGORY =
      Map.ofEntries(
          Map.entry("One Hand Mace", "mace"),
          Map.entry("Sceptre", "mace"),
          Map.entry("Two Hand Mace", "mace"),
          Map.entry("One Hand Sword", "sword"),
          Map.entry("Thrusting One Hand Sword", "sword"),
          Map.entry("Two Hand Sword", "sword"),
          Map.entry("One Hand Axe", "axe"),
          Map.entry("Two Hand Axe", "axe"),
          Map.entry("Dagger", "dagger"),
          Map.entry("Rune Dagger", "dagger"),
          Map.entry("Claw", "claw"),
          Map.entry("Wand", "wand"),
          Map.entry("Staff", "staff"),
          Map.entry("Warstaff", "staff"),
          Map.entry("Bow", "bow"));

  /** 스탯 텍스트 정규화 — 줄 구분/공백/구두점 차이를 무시하고 비교(ninja 패싯 라벨 ↔ 트리 masteryEffects). */
  private static String normStat(String s) {
    return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9%+]", "");
  }

  private boolean isMetaMasteryEffect(PoeTreeGraphService.MasteryEffect e) {
    if (!SEED_MASTERY_ENABLED
        || metaMasteries.isEmpty()
        || e == null
        || e.stats() == null
        || e.stats().isEmpty()) {
      return false;
    }
    return metaMasteries.contains(normStat(String.join("", e.stats())));
  }

  // 조합 집계용: median* 출력 키 ↔ ninja-builds.json 캐릭터 필드 매핑(스샷 전 41 컬럼).
  //   페처 NUM_FIELDS 와 동일 집합 — 이상치 판정·중앙값 계산에 모두 사용.
  private static final String[][] COMBO_COLS = {
    {"medianLevel", "level"},
    {"medianLife", "life"},
    {"medianES", "energyShield"},
    {"medianWard", "ward"},
    {"medianMana", "mana"},
    {"medianEHP", "ehp"},
    {"medianDPS", "dps"},
    {"medianLifeRegen", "lifeRegen"},
    {"medianItemRarity", "itemRarity"},
    {"medianMovementSpeed", "movementSpeed"},
    {"medianFireRes", "fireRes"},
    {"medianColdRes", "coldRes"},
    {"medianLightningRes", "lightningRes"},
    {"medianChaosRes", "chaosRes"},
    {"medianArmour", "armour"},
    {"medianEvasion", "evasion"},
    {"medianBlock", "block"},
    {"medianSpellBlock", "spellBlock"},
    {"medianSpellDodge", "spellDodge"},
    {"medianSuppress", "suppress"},
    {"medianPhysTakenAs", "physTakenAs"},
    {"medianStr", "str"},
    {"medianDex", "dex"},
    {"medianInt", "int"},
    {"medianEnduranceCharges", "enduranceCharges"},
    {"medianFrenzyCharges", "frenzyCharges"},
    {"medianPowerCharges", "powerCharges"},
    {"medianClusterJewels", "clusterJewels"},
    {"medianLargeCluster", "largeCluster"},
    {"medianMediumCluster", "mediumCluster"},
    {"medianSmallCluster", "smallCluster"},
    {"medianUniqueEquip", "uniqueEquip"},
    {"medianMirroredItems", "mirroredItems"},
    {"medianMirroredWeapons", "mirroredWeapons"},
    {"medianMirroredArmours", "mirroredArmours"},
    {"medianPhysicalMax", "physicalMax"},
    {"medianFireMax", "fireMax"},
    {"medianColdMax", "coldMax"},
    {"medianLightningMax", "lightningMax"},
    {"medianChaosMax", "chaosMax"},
    {"medianLowestMax", "lowestMax"},
  };

  // 조합 집계도 페처와 동일 규칙: 96+ 절단 → 캐릭터 단위 이상치 제거(평균 절대 z > 1.75) → 중앙값.
  private static final double COMBO_OUTLIER_MEAN_Z = 1.75;

  /**
   * 멀티스킬 조합 집계 — 선택 스킬을 <b>전부</b> 액티브로 쓰는 캐릭터만 골라, 96+ 절단 → 이상치 캐릭터 제거 → 중앙값 프로파일(median* 41 컬럼 +
   * topKeystones/topCoSkills)을 ObjectNode 로 반환(benchOf/seedOf 재사용 목적). 표본 부족(&lt;MIN_SEED_SAMPLE)이면
   * null.
   */
  /**
   * CI 서브그룹 생존 중앙값 — (전직|스킬) 풀에서 CI 키스톤 보유·96+ 필터 후 EHP 중앙값(표본<5면 0). 혼재 모집단(PB: CI 45/비CI 53)의 통합
   * 중앙값은 CI 문맥에서 EHP 목표를 낮게 잡아 balanced 생존 팩터가 조기 포화 → Aegis Aurora 류 EHP 소스(실측 +21.1%)가 스코어에서 못
   * 이긴다(48단계).
   */
  private double ciSubgroupMedianEhp(String ascendancy, String skillName) {
    if (ninjaBuilds.isEmpty() || skillName == null) {
      return 0;
    }
    List<Double> ehps = new ArrayList<>();
    for (JsonNode b : ninjaBuilds) {
      if (ascendancy != null
          && !ascendancy.isEmpty()
          && !ascendancy.equals(b.path("ascendancy").asText())) {
        continue;
      }
      if (!skillName.equals(b.path("mainSkill").asText()) || b.path("level").asInt() < 96) {
        continue;
      }
      boolean hasCi = false;
      for (JsonNode k : b.path("keystones")) {
        if ("Chaos Inoculation".equals(k.asText())) {
          hasCi = true;
          break;
        }
      }
      if (hasCi && b.path("ehp").isNumber()) {
        ehps.add(b.path("ehp").asDouble());
      }
    }
    if (ehps.size() < 5) {
      return 0;
    }
    ehps.sort(null);
    return ehps.get(ehps.size() / 2);
  }

  private JsonNode comboAggregate(String ascendancy, List<String> skills) {
    List<JsonNode> src = ninjaBuilds;
    if (src.isEmpty() || skills == null || skills.size() < 2) {
      return null;
    }
    List<JsonNode> matched = new ArrayList<>();
    for (JsonNode b : src) {
      if (ascendancy != null
          && !ascendancy.isEmpty()
          && !ascendancy.equals(b.path("ascendancy").asText())) {
        continue;
      }
      JsonNode actives = b.path("activeSkills");
      if (!actives.isArray()) {
        continue;
      }
      Set<String> set = new HashSet<>();
      actives.forEach(s -> set.add(s.asText()));
      if (set.containsAll(skills)) {
        matched.add(b);
      }
    }
    if (matched.size() < MIN_SEED_SAMPLE) {
      return null;
    }
    // 96+ 엔드게임 절단(표본 유지 폴백 포함) — 페처 endgameSubset 과 동일 규칙
    List<JsonNode> endgame = matched.stream().filter(b -> b.path("level").asInt() >= 96).toList();
    List<JsonNode> arr = endgame.size() >= MIN_SEED_SAMPLE ? endgame : matched;
    // (1) 컬럼별 코호트 평균·표준편차
    double[] mean = new double[COMBO_COLS.length];
    double[] std = new double[COMBO_COLS.length];
    for (int c = 0; c < COMBO_COLS.length; c++) {
      double sum = 0;
      int n = 0;
      for (JsonNode b : arr) {
        JsonNode v = b.path(COMBO_COLS[c][1]);
        if (v.isNumber()) {
          sum += v.asDouble();
          n++;
        }
      }
      mean[c] = n > 0 ? sum / n : 0;
      double vr = 0;
      for (JsonNode b : arr) {
        JsonNode v = b.path(COMBO_COLS[c][1]);
        if (v.isNumber()) {
          vr += Math.pow(v.asDouble() - mean[c], 2);
        }
      }
      std[c] = n > 0 ? Math.sqrt(vr / n) : 0;
    }
    // (2) 캐릭터별 평균 절대 z → 임계 초과 캐릭터 통째 제거(과다 제거 방지 floor)
    record Scored(JsonNode b, double mz) {}
    List<Scored> scored = new ArrayList<>();
    for (JsonNode b : arr) {
      double sum = 0;
      int n = 0;
      for (int c = 0; c < COMBO_COLS.length; c++) {
        if (std[c] <= 0) {
          continue;
        }
        JsonNode v = b.path(COMBO_COLS[c][1]);
        if (!v.isNumber()) {
          continue;
        }
        sum += Math.abs((v.asDouble() - mean[c]) / std[c]);
        n++;
      }
      scored.add(new Scored(b, n > 0 ? sum / n : 0));
    }
    List<JsonNode> kept =
        scored.stream().filter(s -> s.mz() <= COMBO_OUTLIER_MEAN_Z).map(Scored::b).toList();
    int floor = Math.max(3, (int) Math.ceil(scored.size() * 0.5));
    if (kept.size() < floor) {
      kept =
          scored.stream()
              .sorted(Comparator.comparingDouble(Scored::mz))
              .limit(floor)
              .map(Scored::b)
              .toList();
    }
    // (3) 생존 캐릭터들의 중앙값 프로파일
    JsonMapper mapper = JsonMapper.builder().build();
    ObjectNode agg = mapper.createObjectNode();
    for (String[] col : COMBO_COLS) {
      List<Double> vals = new ArrayList<>();
      for (JsonNode b : kept) {
        JsonNode v = b.path(col[1]);
        if (v.isNumber()) {
          vals.add(v.asDouble());
        }
      }
      java.util.Collections.sort(vals);
      agg.put(col[0], vals.isEmpty() ? 0d : vals.get(vals.size() / 2));
    }
    // DPS 의미 통일 — b.dps 는 수집 쿼리에 따라 "그 스킬 전용"이라 혼합 중앙값이 오염된다(화염덫 3.2M 가 RF 벤치
    //   2.0M 로 둔갑하던 버그, 사용자 발견). 조합 벤치 DPS = **메인(첫 선택) 스킬 전용 DPS** 중앙값(dpsBySkill),
    //   시뮬 표시 지표(메인 스킬 CombinedDPS)와 같은 의미라 결과 화면의 "실빌드 vs 내" 비교가 성립한다.
    //   스킬별 전용 DPS 는 skillDps 브레이크다운으로 함께 노출.
    ArrayNode skillDps = mapper.createArrayNode();
    for (String s : skills) {
      List<Double> vals = new ArrayList<>();
      for (JsonNode b : kept) {
        JsonNode v = b.path("dpsBySkill").path(s);
        if (v.isNumber()) {
          vals.add(v.asDouble());
        }
      }
      java.util.Collections.sort(vals);
      if (!vals.isEmpty()) {
        ObjectNode o = mapper.createObjectNode();
        o.put("name", s);
        o.put("dps", vals.get(vals.size() / 2));
        o.put("count", vals.size());
        skillDps.add(o);
      }
    }
    agg.set("skillDps", skillDps);
    if (!skillDps.isEmpty() && skills.get(0).equals(skillDps.get(0).path("name").asText())) {
      double mainDps = skillDps.get(0).path("dps").asDouble();
      if (mainDps > 0) {
        agg.put("medianDPS", mainDps);
      }
    }
    agg.put("ascendancy", ascendancy == null ? "" : ascendancy);
    agg.put("mainSkill", String.join(" + ", skills));
    agg.put("sample", kept.size());
    // 키스톤/함께 쓰는 스킬/마스터리 빈도(선택 스킬 제외, 마스터리는 샘플링된 캐릭터만 기여)
    Map<String, Integer> keyCnt = new HashMap<>();
    Map<String, Integer> coCnt = new HashMap<>();
    Map<String, Integer> mastCnt = new HashMap<>();
    for (JsonNode b : kept) {
      b.path("keystones").forEach(k -> keyCnt.merge(k.asText(), 1, Integer::sum));
      b.path("masteries").forEach(m -> mastCnt.merge(m.asText(), 1, Integer::sum));
      b.path("activeSkills")
          .forEach(
              s -> {
                String name = s.asText();
                if (!skills.contains(name)) {
                  coCnt.merge(name, 1, Integer::sum);
                }
              });
    }
    agg.set("topKeystones", topCountNode(mapper, keyCnt, 6));
    agg.set("topCoSkills", topCountNode(mapper, coCnt, 8));
    agg.set("topMasteries", topCountNode(mapper, mastCnt, 8));
    // 패싯은 조합 필터로 즉석 계산 불가(모집단 집계는 서버측) — 메인(첫 선택) 스킬의 정밀/스킬 패싯을 물려받는다.
    JsonNode facets =
        ninjaFacetNodeByKey.get((ascendancy == null ? "" : ascendancy) + "|" + skills.get(0));
    if (facets == null) {
      facets = ninjaFacetNodeBySkill.get(skills.get(0));
    }
    if (facets != null) {
      agg.set("facets", facets);
    }
    return agg;
  }

  private static ArrayNode topCountNode(JsonMapper mapper, Map<String, Integer> counter, int n) {
    ArrayNode arr = mapper.createArrayNode();
    counter.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(n)
        .forEach(
            e -> {
              ObjectNode o = mapper.createObjectNode();
              o.put("name", e.getKey());
              o.put("count", e.getValue());
              arr.add(o);
            });
    return arr;
  }

  /**
   * 멀티스킬 조합 벤치마크 — 선택 스킬을 전부 쓰는 캐릭터만 재집계(사용자 요구: RF+화염덫이면 그 조합 캐릭터 기준). 조합 표본 부족 시 전직 무시로 재시도, 그래도
   * 없으면 첫 스킬 단일 벤치 폴백(mainSkill 표기로 구분 가능).
   */
  public ArchetypeBenchmark ninjaComboBenchmark(String ascendancy, List<String> skills) {
    List<String> names =
        skills == null
            ? List.of()
            : skills.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    if (names.isEmpty()) {
      return null;
    }
    if (names.size() == 1) {
      return ninjaBenchmark(ascendancy, names.get(0));
    }
    JsonNode agg = comboAggregate(ascendancy, names);
    if (agg == null && ascendancy != null && !ascendancy.isEmpty()) {
      agg = comboAggregate("", names);
    }
    return agg != null ? benchOf(agg) : ninjaBenchmark(ascendancy, names.get(0));
  }

  private static double min4nonzero(double a, double b, double c, double d) {
    double m = Double.MAX_VALUE;
    for (double v : new double[] {a, b, c, d}) {
      if (v > 0d) {
        m = Math.min(m, v);
      }
    }
    return m == Double.MAX_VALUE ? 0d : m;
  }

  /**
   * balanced 잡의 생존 목표치를 아키타입(전직×메인스킬) 실측 중앙값으로 설정. 매칭 없으면 정적 floor 유지. 정확 키(전직+스킬) 우선, 없으면 스킬 폴백.
   * 클램프로 극단치 방지(maxhit 0.3~8×, ehp 0.3~8× floor).
   */
  private void setSurvivalTargets(String ascendancy, String skill) {
    setSurvivalTargets(ascendancy, skill, null);
  }

  /**
   * comboSkills(2개 이상)가 주어지면 <b>그 스킬 전부를 쓰는 캐릭터만</b> 즉석 집계한 조합 시드를 최우선 채택(사용자 요구: RF+화염덫이면 그 조합
   * 기준으로 목표를 잡아야 함 — RF 전체 아키타입이 아니라). 조합 표본 부족 시 기존 (전직|스킬)→스킬 폴백.
   */
  private void setSurvivalTargets(String ascendancy, String skill, List<String> comboSkills) {
    this.targetMaxHit = MAXHIT_FLOOR;
    this.targetEhp = EHP_FLOOR;
    this.targetFireRes = 75;
    this.targetColdRes = 75;
    this.targetLightRes = 75;
    this.seededKeystones = List.of();
    this.esArchetype = false;
    this.targetEs = 0d;
    this.targetLifeRegen = 0d;
    this.targetChaosRes = 0;
    this.targetSpellSuppress = 0;
    this.targetSpellBlock = 0;
    this.metaMasteries = Set.of();
    this.metaWeaponClasses = Set.of();
    this.metaOffhandShield = false;
    this.metaPantheonMajor = "";
    this.metaPantheonMinor = "";
    if (!ninjaSeedEnabled || skill == null || skill.isEmpty()) {
      return;
    }
    seedLogPending = true; // 아래에서 시드가 확정되면 내역 한 줄 — 진단 때마다 토글 실험을 반복하지 않도록
    // P1 메타 마스터리 웜스타트 — 아키타입 패싯(전체 모집단)에서 채택률 40%+ 마스터리 효과를 수집.
    //   마스터리 단계에서 후보 우선 편입 + "최고 대비 1% 이내면 메타 채택" + 신규 문턱 완화에 쓰인다.
    {
      JsonNode facetNode = null;
      if (ascendancy != null && !ascendancy.isEmpty()) {
        facetNode = ninjaFacetNodeByKey.get(ascendancy + "|" + skill);
      }
      if (facetNode == null) {
        facetNode = ninjaFacetNodeBySkill.get(skill);
      }
      if (facetNode != null) {
        long total = facetNode.path("total").asLong();
        if (total > 0) {
          Set<String> metas = new LinkedHashSet<>();
          List<String> labels = new ArrayList<>();
          for (JsonNode m : facetNode.path("groups").path("masteries")) {
            if (m.path("count").asDouble() / total >= META_MASTERY_ADOPTION) {
              metas.add(normStat(m.path("name").asText()));
              labels.add(
                  m.path("name").asText()
                      + "("
                      + Math.round(m.path("count").asDouble() * 100 / total)
                      + "%)");
            }
          }
          if (!metas.isEmpty()) {
            this.metaMasteries = metas;
            log("poe.ninja 메타 마스터리 " + metas.size() + "개(채택률 40%+): " + String.join(" · ", labels));
          }
          // P1② 메타 무기 구성 — weaponmode 최다가 점유율 50%+ 면 무기 후보를 그 클래스로 제약.
          JsonNode wm = facetNode.path("groups").path("weaponmode");
          if (wm.isArray() && !wm.isEmpty()) {
            JsonNode topW = wm.get(0);
            double share = topW.path("count").asDouble() / total;
            if (share >= 0.5) {
              String mode = topW.path("name").asText();
              Set<String> classes = weaponModeClasses(mode);
              if (!classes.isEmpty()) {
                this.metaWeaponClasses = classes;
                this.metaOffhandShield = mode.contains("/ Shield");
                log(
                    "poe.ninja 메타 무기 구성: "
                        + mode
                        + " ("
                        + Math.round(share * 100)
                        + "%) — 무기 후보 제약"
                        + (metaOffhandShield ? " + 방패 보조장비" : ""));
              }
            }
          }
          // P1③ 메타 판테온 — 메이저/마이너 각 최다(점유율 15%+, "None" 제외). 패싯은 count 내림차순 정렬됨.
          JsonNode pj = facetNode.path("groups").path("pantheon");
          if (pj.isArray()) {
            String maj = null;
            String min = null;
            double majS = 0;
            double minS = 0;
            for (JsonNode p : pj) {
              String nm = p.path("name").asText();
              double share = p.path("count").asDouble() / total;
              if (share < 0.15 || "None".equals(nm)) {
                continue;
              }
              if (PANTHEON_MAJORS.contains(nm)) {
                if (maj == null) {
                  maj = nm;
                  majS = share;
                }
              } else if (min == null) {
                min = nm;
                minS = share;
              }
            }
            if (maj != null || min != null) {
              this.metaPantheonMajor = maj != null ? pantheonId(maj) : "";
              this.metaPantheonMinor = min != null ? pantheonId(min) : "";
              log(
                  "poe.ninja 메타 판테온: "
                      + (maj != null ? maj + "(" + Math.round(majS * 100) + "%)" : "-")
                      + " / "
                      + (min != null ? min + "(" + Math.round(minS * 100) + "%)" : "-"));
            }
          }
        }
      }
    }
    // ES/CI(에너지실드 스태킹) 아키타입 감지 — ES 서브시스템(트리/장비 시드)의 게이트. 선택 동작은 후속 단계에서.
    if (isEsArchetype(ascendancy, skill)) {
      this.esArchetype = true;
      ArchetypeBenchmark b = ninjaBenchmark(ascendancy, skill);
      this.targetEs = b != null ? b.energyShield() : 0d;
      log(
          String.format(
              "poe.ninja ES/CI 아키타입 감지(%s/%s): ES 중앙값≈%.0f (CI/ES 스태킹 경로)",
              ascendancy == null || ascendancy.isEmpty() ? "any" : ascendancy, skill, targetEs));
    }
    NinjaSeed seed = null;
    String seedSource = skill;
    // 조합 시드 최우선 — 선택 스킬(데미지) 2개 이상이면 그 조합을 전부 쓰는 캐릭터만 즉석 집계.
    if (comboSkills != null && comboSkills.size() >= 2) {
      JsonNode agg = comboAggregate(ascendancy, comboSkills);
      if (agg == null && ascendancy != null && !ascendancy.isEmpty()) {
        agg = comboAggregate("", comboSkills);
      }
      NinjaSeed comboSeed = agg != null ? seedOf(agg) : null;
      if (comboSeed != null && comboSeed.sample() >= MIN_SEED_SAMPLE) {
        seed = comboSeed;
        seedSource = String.join(" + ", comboSkills);
        log("poe.ninja 조합 시드: [" + seedSource + "] 전부 사용 캐릭터 n=" + comboSeed.sample());
      }
    }
    if (seed == null && ascendancy != null && !ascendancy.isEmpty()) {
      NinjaSeed exact = ninjaSeedByKey.get(ascendancy + "|" + skill);
      // 정확키(전직|스킬)는 표본이 충분할 때만 채택 — 희귀 전직×스킬(n<MIN)은 저표본 노이즈라
      //   견고한 스킬폴백(전 전직 통합)을 쓴다. 표시용 벤치마크는 이 가드 없이 정확키 그대로.
      if (exact != null && exact.sample() >= MIN_SEED_SAMPLE) {
        seed = exact;
      }
    }
    if (seed == null) {
      seed = ninjaSeedBySkill.get(skill);
    }
    if (seed == null) {
      return;
    }
    this.seededKeystones = seed.keystones() != null ? seed.keystones() : List.of();
    // ⚠ ninja 목표는 정적 floor 위로만 조정(상향만) — floor **아래로 낮추면** 옵티마이저가 방어를 못 쌓는
    //   아키타입(ES/CI 등)에서 오히려 유리대포(저EHP·미캡)를 승인한다(Penance Brand Elementalist 실측:
    //   목표 11k로 낮추자 EHP 13.7k 번개저항43 유리대포 채택). 사용자 요구=유리대포 배제 → 하한은 정적 floor.
    if (seed.targetMaxHit() > 0) {
      // 하한은 **시드가 있을 때** 그 아키타입 실측 중앙값을 존중한다(절대 바닥 MAXHIT_SEED_FLOOR 만 지킴).
      //   예전엔 아키타입 무관 상수 15,000 이 하한이라, 실메타가 얇은 회피·억제형에서 메타의 2배 생존을 강제하며
      //   DPS 예산을 잡아먹었다 — 실측: 번개 화살 실빌드 100건 최약최대피격 중앙값 7,500 인데 우리 결과는 20,195,
      //   하한만 7,500 으로 낮추자 3,864,028(벤치 74%) → 4,989,100(**벤치 96%**), 생존은 7,312 로 메타와 일치.
      //   시드가 없으면(매칭 실패) 종전대로 MAXHIT_FLOOR 가 하한 — 근거 없는 유리대포 방지.
      this.targetMaxHit = clampD(seed.targetMaxHit(), MAXHIT_SEED_FLOOR, MAXHIT_FLOOR * 8);
    }
    if (seed.targetEhp() > 0) {
      // 최약최대피격과 같은 함정 — 시드가 있어도 하한 40,000 에 눌려 아키타입 실측을 못 쓴다
      //   (번개 화살 시드 25,000 → 40,000 으로 상향돼 실메타의 1.6배 EHP 를 목표로 잡고 있었다).
      this.targetEhp =
          clampD(seed.targetEhp(), EHP_TERM_ENABLED ? EHP_SEED_FLOOR : EHP_FLOOR, EHP_FLOOR * 8);
    }
    // 생명 재생 목표 — 실측 중앙값 × 0.6. ⚠ ninja lifeRegen 은 gross 지표라 PoB NetLifeRegen 과 다르다:
    //   실빌드 캐릭터(PoB export)를 우리 엔진으로 재계산해 검증한 결과 ninja 2,085 ↔ PoB net 1,176(비 0.56).
    //   보정 없이 gross 목표를 net 지표에 요구하면 도달 불가 목표를 좇아 DPS 를 과희생한다(실측: −5.3% 트레이드).
    //   자가연소(RF) balanced 만 참조 — 비-자가연소/비-balanced 기준선 불변.
    this.targetLifeRegen =
        seed.targetLifeRegen() > 0 ? Math.min(seed.targetLifeRegen() * 0.6, 20000) : 0d;
    // 원소 저항 목표 — 아키타입 실측 중앙값을 [75, 90] 로 클램프(75 미만은 캡 미달이라 75 유지, 90=게임 최대저항 상한).
    //   대부분 75(불변)지만 치프틴 RF/화염덫처럼 최대저항 90 특화 아키타입은 90을 목표로 → survivalScore/저항채움 반영.
    // P2 카오스 저항 목표 — 실빌드 중앙값(캡 75, 음수/0이면 비활성). balancedSurvival (2c)에서 감쇠로 유도.
    this.targetChaosRes = Math.max(0, Math.min(75, seed.chaosRes()));
    // 주문 억제 목표 — 억제 특화 아키타입(중앙값 60%+)만. 회피·억제 레이어 실빌드(Penance Brand 류,
    //   억제 100%)의 EHP 를 EHP 연속 팩터만으론 못 좇아 명시 목표로 조향(P2 카오스와 같은 약한 감쇠).
    this.targetSpellSuppress = seed.spellSuppress() >= 60 ? Math.min(100, seed.spellSuppress()) : 0;
    if (targetSpellSuppress > 0) {
      log("poe.ninja 억제 특화 아키타입: 주문 억제 목표 " + targetSpellSuppress + "%");
    }
    // 주문 막기 목표 — 막기 특화 아키타입(중앙값 50%+)만. 캡 75(게임 막기 상한).
    //   실측: Penance Brand Elementalist 모집단은 억제 10/주문막기 78 — 방어 메타가 막기 레이어.
    this.targetSpellBlock = seed.spellBlock() >= 50 ? Math.min(75, seed.spellBlock()) : 0;
    if (targetSpellBlock > 0) {
      log("poe.ninja 막기 특화 아키타입: 주문 막기 목표 " + targetSpellBlock + "%");
    }
    this.targetFireRes = Math.max(75, Math.min(90, seed.fireRes()));
    this.targetColdRes = Math.max(75, Math.min(90, seed.coldRes()));
    this.targetLightRes = Math.max(75, Math.min(90, seed.lightRes()));
    if (targetFireRes > 75 || targetColdRes > 75 || targetLightRes > 75) {
      log(
          String.format(
              "poe.ninja 최대저항 특화 아키타입: 저항 목표 화%d/냉%d/번%d (75 캡 대신 아키타입 메타 사용)",
              targetFireRes, targetColdRes, targetLightRes));
    }
    log(
        String.format(
            "poe.ninja 생존 목표(%s/%s): maxhit≈%.0f ehp≈%.0f (n=%d)",
            ascendancy == null || ascendancy.isEmpty() ? "any" : ascendancy,
            seedSource,
            targetMaxHit,
            targetEhp,
            seed.sample()));
    // 시드 내역 — 저항/억제 목표까지 확정된 **메서드 끝**에서 한 줄. 진단할 때마다 시드 on/off 토글 실험을
    //   반복하지 않으려는 것(실측: 원소 강타가 시드 켬 6,102,794 vs 끔 10,961,381 로 −44%였는데 로그가 없어
    //   어느 요소 탓인지 바로 못 짚었다).
    if (seedLogPending) {
      seedLogPending = false;
      log(
          String.format(
              "시드 프로파일: 최약목표 %,.0f · EHP목표 %,.0f · 저항 화%d/냉%d/번%d/카%d · 억제 %d · 재생 %,.0f · 키스톤 [%s] · 마스터리 %d개 · 무기 %s",
              targetMaxHit,
              targetEhp,
              targetFireRes,
              targetColdRes,
              targetLightRes,
              targetChaosRes,
              targetSpellSuppress,
              targetLifeRegen,
              String.join(", ", seededKeystones),
              metaMasteries.size(),
              metaWeaponClasses.isEmpty() ? "-" : String.join("/", metaWeaponClasses)));
    }
  }

  private static double clampD(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
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

  /** 최근 결과 이력 한 건의 목록용 요약(전체 결과 JSON 은 historyResult(id) 로 별도 조회). id = 저장 시각 epochMs. */
  public record OptimizeHistoryEntry(
      long id,
      String gemName,
      String gemNameKo,
      String className,
      String classNameKo,
      String ascendancy,
      String ascendancyKo,
      String objective,
      String scenario,
      String scenarioKo,
      boolean combatBuffs,
      String finalValue,
      int evalCount,
      long durationMs) {}

  /** 완료 결과를 sim/history/&lt;epochMs&gt;.json 으로 남기고 상한(HISTORY_LIMIT)을 넘는 오래된 건을 정리한다. */
  private void saveHistory(String resultJson) {
    try {
      Files.createDirectories(historyDir);
      Files.writeString(
          historyDir.resolve(System.currentTimeMillis() + ".json"),
          resultJson,
          StandardCharsets.UTF_8);
      pruneHistory();
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 저장 실패", e);
    }
  }

  /** epochMs 파일명은 13자리 고정폭이라 문자열 내림차순 = 시각 내림차순 — 상위 N 건만 남기고 삭제. */
  private void pruneHistory() throws java.io.IOException {
    try (var stream = Files.list(historyDir)) {
      var files =
          stream
              .filter(p -> p.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
              .toList();
      for (int i = HISTORY_LIMIT; i < files.size(); i++) {
        try {
          Files.deleteIfExists(files.get(i));
        } catch (java.io.IOException ignore) {
          // 개별 삭제 실패는 무시 — 다음 실행에서 다시 정리된다
        }
      }
    }
  }

  /** 최근 결과 목록(최신순, 최대 HISTORY_LIMIT). 파일이 없으면 빈 목록. */
  public List<OptimizeHistoryEntry> history() {
    if (!Files.exists(historyDir)) {
      return List.of();
    }
    JsonMapper jsonMapper = JsonMapper.builder().build();
    try (var stream = Files.list(historyDir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .limit(HISTORY_LIMIT)
          .map(
              p -> {
                try {
                  long id = Long.parseLong(p.getFileName().toString().replace(".json", ""));
                  PoeOptimizeResult r =
                      jsonMapper.readValue(Files.readString(p), PoeOptimizeResult.class);
                  return new OptimizeHistoryEntry(
                      id,
                      r.gemName(),
                      r.gemNameKo(),
                      r.className(),
                      r.classNameKo(),
                      r.ascendancy(),
                      r.ascendancyKo(),
                      r.objective(),
                      r.scenario(),
                      r.scenarioKo(),
                      r.combatBuffs(),
                      r.finalValue(),
                      r.evalCount(),
                      r.durationMs());
                } catch (Exception e) {
                  return null; // 깨진 파일 한 건이 목록 전체를 막지 않게
                }
              })
          .filter(java.util.Objects::nonNull)
          .toList();
    } catch (java.io.IOException e) {
      logger.warn("PoE 최적화 이력 조회 실패", e);
      return List.of();
    }
  }

  /** 이력 결과 한 건 전체 조회(id = 저장 시각 epochMs) — 없거나 깨졌으면 null. */
  public PoeOptimizeResult historyResult(long id) {
    Path file = historyDir.resolve(id + ".json");
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return JsonMapper.builder()
          .build()
          .readValue(Files.readString(file), PoeOptimizeResult.class);
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 로드 실패: {}", id, e);
      return null;
    }
  }

  /** 최근 결과 이력 한 건 삭제(사용자 요청). id = 저장 시각 epochMs. 삭제했으면 true. */
  public boolean deleteHistory(long id) {
    Path file = historyDir.resolve(id + ".json");
    // 디렉토리 이탈 방지 — 정규화 경로가 historyDir 하위인지 확인
    if (!file.normalize().startsWith(historyDir.normalize())) {
      return false;
    }
    try {
      return Files.deleteIfExists(file);
    } catch (Exception e) {
      logger.warn("PoE 최적화 이력 삭제 실패: {}", id, e);
      return false;
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

  /**
   * 실행 중인 최적화 잡 중지 요청 — 취소 플래그를 세우고 잡 스레드를 인터럽트한다. 잡은 다음 페이즈/라운드 경계(checkCancel)에서 조기 종료하고
   * lastStatus=CANCELLED 로 마감한다. 실행 중이 아니면 false.
   */
  public boolean cancel() {
    if (!running.get()) {
      return false;
    }
    cancelRequested = true;
    Thread t = jobThread;
    if (t != null) {
      t.interrupt();
    }
    log("사용자 중지 요청 — 현재 단계 종료 후 중단합니다");
    return true;
  }

  /** 취소 요청 시 JobCancelledException 을 던져 잡을 조기 종료한다. 페이즈/라운드 경계에서 호출. */
  private void checkCancel() {
    if (cancelRequested) {
      throw new JobCancelledException();
    }
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

  /** 영원한 축복으로 예약 없이 유지하는 오라(잡 스코프). 없으면 null. */
  private volatile PoeGem blessingAura;

  // 이번 잡의 스킬 키워드 — XML 조립 시 유니크에 엘드리치 임플리싯을 고르는 데 쓴다(레어는 craft 시점에 이미 결정).
  private volatile List<String> currentKeywords = List.of();
  // 이 잡에서 아뮬렛에 걸 도유(잡마다 키워드가 정해질 때 1회 계산). null = 키워드에 맞는 노터블 없음
  private volatile AnointPick currentAnoint = null;
  // 속성 요구치 보정용 보조젬 레벨 하향(slug→level, 기본 20). 전 슬롯 유니크라 레어로 못 채울 때
  // 실전처럼 "요구치 맞는 레벨까지만 젬을 키운" 상태를 만든다.
  private volatile Map<String, Integer> supportLevelOverride = Map.of();
  // 실현 가능성 조향 on/off — **아이템 단계부터** 켠다. 장비가 없는 초반(보조젬/트리)엔 속성이
  // 원래 부족해서, 페널티가 "나중에 채워질 요구치"를 이유로 좋은 보조젬을 걷어차는 왜곡이 났다
  // (실측: 사이클론 10.14M→3.57M 후퇴). 장비가 갖춰지는 시점부터가 판정이 공정하다.
  private volatile boolean feasibilitySteering = false;
  // #1 정의의 화염류(자가연소) 잡 — 선택 지표에 지속력(순생명재생) 게이트 적용. RF 외엔 false 라 기준선 불변.
  private volatile boolean selfBurnRun = false;
  // #235 미니언 잡 — 선택/표시 지표를 max(CombinedDPS, FullDPS)=미니언수 합산 총합으로(ninja 총합 표기 정합).
  //   FullDPS 는 미니언 그룹에만 조건부로 켠 includeInFullDPS XML + PoB calcFullDPS 로 count-정확 집계됨. 미니언
  //   외엔 false + XML 도 원본 동일 → 단일 액터·토템 기준선(arc/cyclone/ED/RF/AW) 구조적 불변.
  private volatile boolean multiActorBuild = false;
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

  /** 사용자가 요청으로 지정한 문신(불가침) — fixedTattoos 는 자동 채택이 병합돼 구분이 필요하다. 잡별 상태 — start() 세팅. */
  private volatile Map<Integer, String> userTattoos = Map.of();

  // 트리 에디터에서 사용자가 고른 도유 노터블 id — 지정 시 자동 전수 스윕 대신 이것으로 고정(사용자 지정은 존중)
  private volatile Integer fixedAnoint = null;

  // 이 실행의 완료 결과를 이력(sim/history)에 남길지 — QA 배터리(고정트리 픽스처 포함)는 false 로 호출해
  // 이력이 픽스처 잡으로 오염되는 것을 막는다(스냅샷 함정 #167 과 같은 계열). 사용자 실행은 기본 true.
  private volatile boolean saveHistoryForRun = true;

  // ── poe.ninja 실빌드 아키타입 시드 (fetch-ninja-builds.mjs 산출) ──
  //   balanced 목표의 생존 목표치(max hit / EHP)를 정적 floor 대신 **아키타입별 실측 중앙값**으로.
  //   파일 없으면 전부 정적 floor → 무회귀. dps/ehp 목표엔 애초에 balancedSurvival 미적용이라 기준선 불변.
  private record NinjaSeed(
      double targetMaxHit,
      double targetEhp,
      int sample,
      List<String> keystones,
      int fireRes,
      int coldRes,
      int lightRes,
      int chaosRes,
      double targetLifeRegen,
      /** 주문 억제 중앙값 — 억제 특화 아키타입(회피·억제 레이어) 감지용. */
      int spellSuppress,
      /** 주문 막기 중앙값 — 막기 특화 아키타입(Aegis·방패 막기 레이어) 감지용. */
      int spellBlock) {}

  // key = "ascendancy|mainSkill"(정확) 및 "mainSkill"(전직 무관 폴백, 표본 최대 아키타입)
  private final Map<String, NinjaSeed> ninjaSeedByKey = new HashMap<>();
  private final Map<String, NinjaSeed> ninjaSeedBySkill = new HashMap<>();

  // 기본 on. 귀속 실험용으로만 끈다(POE_NINJA_SEED=off) — 시드는 목표치뿐 아니라 키스톤·마스터리·
  //   무기클래스·판테온 웜스타트로도 쓰여서, 벤치 갱신 후 결과가 크게 바뀌면 시드 탓인지 가려야 한다.
  /**
   * 시드 키스톤 후보 주입 — **기본 off**(켜려면 POE_SEED_KEYSTONES=on).
   *
   * <p>topKeystones 는 그 아키타입 유저들의 **집계**지 한 빌드가 함께 찍는 조합이 아니다. 그대로 한 빌드에 주입하면 철의 반사신경(회피→방어도)과 유령
   * 무도(회피 시 ES) 처럼 상호배타 키스톤이 동시에 후보가 되고, 빌드가 빈 시점(실측 값 44,344)에 6pt 를 써 채택된 뒤 트리 재대결도 되돌리지 못한다.
   *
   * <p>실측(원소 강타 분광/데드아이): 시드 전체 on 6,102,794 · 시드 전체 off 10,961,381 · **키스톤 주입만 off
   * 15,084,361(+147%)** — 키스톤만 끈 쪽이 시드 전체를 끈 것보다 38% 높다(시드의 목표치·마스터리·저항은 이득). 7종 회귀에서는 전 축
   * **±0.0%**(비트 동일) 로 손해 축이 없다 → 이득 증거 0, 손해 증거 1 이므로 기본 off. CI 아키타입 판정(isEsArchetype)은 이 토글과
   * 무관하게 동작한다.
   */
  /** 시드 마스터리 웜스타트(후보 추가 + 트리 포인트 예약) on/off — 귀속 실험용. 기본 on. */
  private static final boolean SEED_MASTERY_ENABLED =
      !"off".equalsIgnoreCase(System.getenv().getOrDefault("POE_SEED_MASTERY", "on"));

  /** 시드 무기 클래스 제한 on/off — 귀속 실험용. 기본 on. */
  private static final boolean SEED_WEAPON_ENABLED =
      !"off".equalsIgnoreCase(System.getenv().getOrDefault("POE_SEED_WEAPON", "on"));

  private static final boolean SEED_KEYSTONES_ENABLED =
      "on".equalsIgnoreCase(System.getenv().getOrDefault("POE_SEED_KEYSTONES", "off"));

  /** 시드 내역 로그를 한 번만 찍기 위한 플래그. */
  private volatile boolean seedLogPending = false;

  private volatile boolean ninjaSeedEnabled =
      !"off".equalsIgnoreCase(System.getenv().getOrDefault("POE_NINJA_SEED", "on"));
  private static final int MIN_SEED_SAMPLE = 5; // 정확키(전직|스킬) 시드 채택 최소 표본(미만은 스킬폴백)

  /** poe.ninja 실빌드 벤치마크(결과 표시용) — 아키타입 실측 중앙값 프로파일. */
  public record ArchetypeBenchmark(
      String ascendancy,
      String mainSkill,
      int sample,
      long life,
      long energyShield,
      long ehp,
      long dps,
      long physicalMax,
      long fireMax,
      long coldMax,
      long lightningMax,
      long chaosMax,
      int fireRes,
      int coldRes,
      int lightningRes,
      int chaosRes,
      // 생명 재생/초 — RF 등 자가연소·지속형 빌드의 생존 핵심(실빌드는 높은데 시뮬이 미반영하던 갭). 방어층(armour/evasion/block/
      // suppress)·최약최대피격(lowestMax)도 "모든 컬럼 참조" 요구에 맞춰 캡처.
      long lifeRegen,
      long armour,
      long evasion,
      int block,
      int suppress,
      long lowestMax,
      // 전 컬럼 참조(스샷 전 컬럼) — 부가 자원/속성/충전/주문막기·회피/물리피해전환/클러스터주얼·유니크·미러 개수.
      long ward,
      long mana,
      int itemRarity,
      int movementSpeed,
      int spellBlock,
      int spellDodge,
      int physTakenAs,
      int str,
      int dex,
      int intel,
      int enduranceCharges,
      int frenzyCharges,
      int powerCharges,
      int clusterJewels,
      int largeCluster,
      int mediumCluster,
      int smallCluster,
      int uniqueEquip,
      int mirroredItems,
      int mirroredWeapons,
      int mirroredArmours,
      List<String> topKeystones,
      List<String> topCoSkills,
      // 실빌드 마스터리(효과 텍스트) — 캐릭터 상세 JSON 채집(아키타입별 레벨 상위 샘플). 구 데이터엔 없어 null.
      List<String> topMasteries,
      // 실빌드 성향: 이 (전직×스킬) 실측 DPS·EHP 를 전체 아키타입 중앙값과 비교한 편향. dps=공격특화(저EHP·고DPS),
      // ehp=생존특화(고EHP·저DPS), balanced=균형. 셀렉트 대신 이 성향으로 시뮬 목표를 정하는 근거(P2)이자 화면 표시용.
      String lean,
      // 전 컬럼 참조 특화 판정 — 이 아키타입이 두드러진 컬럼 키 목록(dps/ehp/life/es/liferegen/armour/evasion/block/
      // suppress/maxres). 전체 중앙값(저표본 제외) 대비 판정. 게이트가 로케일 라벨로 표시.
      List<String> specializations,
      // 멀티스킬 조합 벤치의 스킬별 전용 DPS 중앙값(dps-<스킬> 필터 실측) — dps 필드는 메인(첫 선택) 스킬 전용.
      // 단일 스킬 벤치에선 null.
      List<SkillDpsEntry> skillDps,
      // 패싯(poe.ninja 검색 사이드바 집계) — 필터 매칭 **전체 모집단** 기준 카운트(top-100 표본 아님).
      // facetTotal = 모집단 수(% 분모). groups:
      // masteries/runegrafts/tattoos/weaponmode/pantheon/atlasskills/
      // anointed/secondascendancy/bandit/items/keypassives/shrinebeltbuffs. 구 데이터엔 없어 0/null.
      long facetTotal,
      Map<String, List<FacetEntry>> facets) {}

  /** 조합 벤치 스킬별 전용 DPS — count = 해당 스킬 전용 DPS 를 보유한 표본 수. */
  public record SkillDpsEntry(String name, long dps, int count) {}

  /** 패싯 항목 — 모집단 중 count 명이 사용. */
  /**
   * @param nameKo 한국어 표기(없으면 null) — 화면이 로케일에 맞게 고른다.
   */
  public record FacetEntry(String name, int count, String nameKo) {}

  private final Map<String, ArchetypeBenchmark> ninjaBenchByKey = new HashMap<>();
  private final Map<String, ArchetypeBenchmark> ninjaBenchBySkill = new HashMap<>();

  // 전체 아키타입의 DPS·EHP 중앙값 — 개별 (전직×스킬)의 성향(lean) 판정 기준선. loadNinjaSeeds 에서 리그 데이터로 채운다.
  private volatile double ninjaGlobalMedianDps = 2_500_000;
  private volatile double ninjaGlobalMedianEhp = 70_000;

  // 특화 판정용 전체 컬럼 중앙값(저표본 제외). 컬럼명 → 중앙값. loadNinjaSeeds 에서 채운다.
  private final Map<String, Double> ninjaColMedian = new HashMap<>();
  // 특화 판정 대상 매그니튜드 컬럼(높을수록 특화). 저항/막기/회피억제는 캡형이라 별도 절대 임계.
  private static final String[] SPECIALIZE_COLS = {
    "medianDPS",
    "medianEHP",
    "medianLife",
    "medianES",
    "medianLifeRegen",
    "medianArmour",
    "medianEvasion",
    // 전 컬럼 참조 — 부가 자원/속성/개수형도 전체 중앙값 대비 특화 판정(사용자 요구: 스샷 전 컬럼).
    "medianWard",
    "medianMana",
    "medianItemRarity",
    "medianMovementSpeed",
    "medianStr",
    "medianDex",
    "medianInt",
    "medianClusterJewels",
    "medianUniqueEquip"
  };
  // 전체 중앙값 계산에서 제외할 저표본 하한(이상치·노이즈 필터). 개별 특화 판단도 이 표본 이상만.
  private static final int SPECIALIZE_MIN_SAMPLE = 10;

  /**
   * 전 컬럼 참조 특화 판정 — 이 아키타입이 어느 컬럼에 특화됐는지(전체 중앙값 대비 두드러지게 높은 컬럼) 키 목록 반환. 매그니튜드 컬럼은 전체 중앙값의 배수 임계,
   * 캡형(저항/막기/회피억제)은 절대 임계. 저표본 아키타입(sample&lt;하한)은 판단 보류(빈 목록). ⚠ 단순 평균 아닌 **중앙값 기준 + 저표본 제외**(사용자
   * 요구: 가중·이상치 필터).
   */
  private List<String> specializationsOf(JsonNode a) {
    List<String> spec = new ArrayList<>();
    if (a.path("sample").asInt() < SPECIALIZE_MIN_SAMPLE) {
      return spec; // 표본 부족 → 특화 판단 보류
    }
    specMag(spec, a, "medianDPS", "dps", 1.6, 0);
    specMag(spec, a, "medianEHP", "ehp", 1.6, 0);
    specMag(spec, a, "medianLife", "life", 1.5, 0);
    specMag(spec, a, "medianES", "es", 1.5, 3000); // ES 는 절대 3000+ 도 요구(저ES 노이즈 배제)
    specMag(spec, a, "medianLifeRegen", "liferegen", 1.6, 0);
    specMag(spec, a, "medianArmour", "armour", 1.8, 0);
    specMag(spec, a, "medianEvasion", "evasion", 1.8, 0);
    // 전 컬럼 참조 — 부가 자원/속성/개수형 매그니튜드 특화(전체 중앙값 대비, 절대 하한으로 노이즈 배제).
    specMag(spec, a, "medianWard", "ward", 1.6, 1000);
    specMag(spec, a, "medianMana", "mana", 1.6, 0);
    specMag(spec, a, "medianItemRarity", "rarity", 1.5, 0);
    specMag(spec, a, "medianMovementSpeed", "movespeed", 1.3, 0);
    specMag(spec, a, "medianStr", "str", 1.4, 0);
    specMag(spec, a, "medianDex", "dex", 1.4, 0);
    specMag(spec, a, "medianInt", "int", 1.4, 0);
    specMag(spec, a, "medianClusterJewels", "cluster", 1.5, 3);
    specMag(spec, a, "medianUniqueEquip", "unique", 1.4, 5);
    // 캡형/개수형 — 절대 임계
    if (a.path("medianBlock").asInt() >= 40) spec.add("block");
    if (a.path("medianSuppress").asInt() >= 60) spec.add("suppress");
    if (a.path("medianSpellBlock").asInt() >= 60) spec.add("spellblock");
    if (a.path("medianSpellDodge").asInt() >= 30) spec.add("spelldodge");
    if (a.path("medianPhysTakenAs").asInt() >= 30) spec.add("phystaken");
    if (a.path("medianEnduranceCharges").asInt() >= 4) spec.add("echarge");
    if (a.path("medianFrenzyCharges").asInt() >= 4) spec.add("fcharge");
    if (a.path("medianPowerCharges").asInt() >= 4) spec.add("pcharge");
    if (a.path("medianMirroredItems").asInt()
            + a.path("medianMirroredWeapons").asInt()
            + a.path("medianMirroredArmours").asInt()
        >= 2) spec.add("mirror");
    int minRes =
        Math.min(
            a.path("medianFireRes").asInt(),
            Math.min(a.path("medianColdRes").asInt(), a.path("medianLightningRes").asInt()));
    if (minRes >= 85) spec.add("maxres");
    return spec;
  }

  /** 매그니튜드 컬럼 특화 판정 — 전체 중앙값 × mult 이상이고 절대 하한 minAbs 이상이면 key 추가. */
  private void specMag(
      List<String> spec, JsonNode a, String col, String key, double mult, double minAbs) {
    double med = ninjaColMedian.getOrDefault(col, 0d);
    double v = a.path(col).asDouble();
    if (med > 0 && v >= med * mult && v >= minAbs) {
      spec.add(key);
    }
  }

  /**
   * 실빌드 성향 판정: 이 아키타입의 DPS·EHP 를 전체 중앙값과 로그비교. dps 축이 EHP 축보다 뚜렷이 높으면 "dps"(공격특화), 반대면 "ehp"(생존특화),
   * 그 사이면 "balanced". 임계 0.35 dex ≈ 약 2.2배 차이.
   */
  private String leanOf(double dps, double ehp) {
    if (dps <= 0 || ehp <= 0) {
      return "balanced";
    }
    double diff = Math.log10(dps / ninjaGlobalMedianDps) - Math.log10(ehp / ninjaGlobalMedianEhp);
    if (diff > 0.35) {
      return "dps";
    }
    if (diff < -0.35) {
      return "ehp";
    }
    return "balanced";
  }

  // job-scoped 생존 목표치 — 기본 = 정적 floor, 아키타입 매칭 시 실측치로 override.
  private volatile double targetMaxHit = MAXHIT_FLOOR;
  private volatile double targetEhp = EHP_FLOOR;
  // job-scoped 원소 저항 목표(화/냉/번) — 아키타입 실측 중앙값(75~90 클램프). 대부분 75지만 치프틴 RF 등
  //   최대저항 특화 아키타입은 90 → survivalScore/저항채움이 90을 목표로(하드코딩 75 대체). 기본 75.
  private volatile int targetFireRes = 75;
  private volatile int targetColdRes = 75;
  private volatile int targetLightRes = 75;
  // job-scoped 시드 키스톤(아키타입 실측 상위) — 트리 탐색 후보 풀에 주입(키워드 미매칭 방어 키스톤 포착용).
  private volatile List<String> seededKeystones = List.of();
  // job-scoped ES/CI(에너지실드 스태킹) 아키타입 여부 — poe.ninja 벤치마크로 판정. ES 서브시스템(트리/장비 시드)의 게이트.
  private volatile boolean esArchetype = false;
  // job-scoped ES 목표(에너지실드 중앙값) — ES 아키타입일 때만 >0. balancedSurvival/장비 시드에서 참조.
  private volatile double targetEs = 0d;
  // job-scoped 생명 재생/초 목표(아키타입 실측 중앙값) — RF 등 자가연소 빌드는 재생이 생존 핵심인데 기존엔 순재생<0 게이트만
  //   있어 시뮬이 재생 0(경계선)에 안주했다. balanced·자가연소 한정으로 이 목표까지 총재생을 끌어올린다(사용자 지적: 실빌드 2666인데 미반영).
  private volatile double targetLifeRegen = 0d;
  // P2 카오스 저항 목표(실빌드 중앙값, 캡 75) — balanced 전용, 0=비활성. 잡마다 리셋.
  private volatile int targetChaosRes = 0;

  /** 주문 억제 목표(%) — 억제 특화 아키타입(중앙값 60%+)만 활성. 잡별 상태 — start() 리셋 블록 필수. */
  private volatile int targetSpellSuppress = 0;

  /** 주문 막기 목표(%) — 막기 특화 아키타입(중앙값 50%+)만 활성. 잡별 상태 — start() 리셋 블록 필수. */
  private volatile int targetSpellBlock = 0;

  /** 이번 잡이 balanced 인지 — craftRare 등 objectiveKey 가 안 닿는 깊은 경로의 분기용. start() 리셋 필수. */
  private volatile boolean balancedJob = false;

  /**
   * 볼록 생존 벌점을 켜는 구간인가 — **최종 재대결 이후에만** true.
   *
   * <p>탐색 중에는 켜면 안 된다. 중간 빌드는 아직 장비가 덜 찼으니 생존이 언제나 목표 미달이고, 볼록 벌점이 그 시점의 후보를 짓눌러 초반에 방어 장비를 사버린 뒤
   * DPS 를 회복하지 못한다(실측: 곡률 1.5 전 축 회귀에서 사신이 생존 목표를 이미 1.18배 넘겼는데도 DPS 만 −44.5%, 번개 화살은 곡률 2.0 에서 생존
   * 1.10 인 채 −53%). 완성된 빌드끼리 겨루는 최종 재대결에서만 켜면 목표를 이미 넘긴 아키타입은 sqrt 가지라 아예 무변화고, 유리대포만 교정된다.
   */
  private volatile boolean convexSurvivalPhase = false;

  /**
   * 가드 스킬(용융 껍질/강철 피부/불사의 외침) — 없으면 null.
   *
   * <p>PoB 는 가드 스킬이 빌드에 **있기만 하면** 버프를 자동 적용하고(CalcPerform 3277행), 피격 계산에서 GuardAbsorb 층을
   * 더한다(CalcDefence 216~306행). 우리 XML 엔 이 그룹이 아예 없어 그 층이 통째로 0 이었다 — 실빌드는 거의 전부 하나씩 낀다(저거넛 뼈 박살 최약
   * 최대피격 실빌드 39,000 vs 우리 11,482 의 유력한 원인).
   */
  private volatile String guardSkill;

  /** 가드 스킬에 링크할 보조젬(없으면 null) — 흡수량은 젬 레벨·품질로 오르므로 기원/강화가 후보다. */
  private volatile String guardSupport;

  /** 유니크 주얼 최종 재대결 후보 풀 — 주얼은 아이템 이전에 확정되므로 완성 문맥에서 다시 겨룬다. */
  private volatile List<PoeUniqueItem> jewelRematchPool = List.of();

  /** 소켓당 최종 재대결에서 시험할 유니크 주얼 수(풀 상위). */
  private static final int JEWEL_REMATCH_PER_SOCKET = 8;

  /** 보조젬 최종 재대결 후보 풀(1라운드 숏리스트) — 랭킹 문맥에서 정해진 선택을 완성 문맥에서 다시 겨룬다. */
  private volatile List<PoeGem> supportRematchPool = List.of();

  // ES 듀얼패스 임시 플래그 — true 동안 craftRare 가 방어 베이스를 ES 변형으로 강제(tryEsTemplate 내부에서만 on).
  private volatile boolean forceEsBase = false;

  /**
   * poe.ninja 벤치마크로 ES/CI(에너지실드 스태킹) 아키타입인지 판정.
   *
   * <p>ES/CI 는 CI(카오스 접종, 생명→1·카오스면역) 키스톤 + ES 베이스 장비 + ES 트리를 <b>함께</b> 갖춰야만 성립하는 조율 아키타입이라, 생명
   * 기준선에서 출발하는 greedy 로는 골짜기를 못 넘는다(ES 개별 변경은 각각 손해). 이 판정으로 ES 서브시스템(force-CI 시드 · ES 장비/트리 편향 ·
   * dual-pass)의 진입을 게이트한다.
   *
   * <p>기준: 표본 충분(≥MIN_SEED_SAMPLE) 且 (CI/EB 키스톤이 실측 상위 || ES 중앙값이 생명 대비 우세). 매칭 없으면 false → 생명 빌드
   * 경로 그대로 → 기준선(arc/RF 등) 불변.
   */
  private boolean isEsArchetype(String ascendancy, String skill) {
    if (!ninjaSeedEnabled) {
      return false;
    }
    ArchetypeBenchmark b = ninjaBenchmark(ascendancy, skill);
    if (b == null || b.sample() < MIN_SEED_SAMPLE) {
      return false;
    }
    boolean ciKeystone =
        b.topKeystones() != null
            && b.topKeystones().stream()
                .anyMatch(
                    k ->
                        k != null
                            && (k.equalsIgnoreCase("Chaos Inoculation")
                                || k.equalsIgnoreCase("Eldritch Battery")));
    // ES 중앙값이 유의미(≥ES_ARCHETYPE_FLOOR)하고 **생명의 2배 이상**이어야 진성 ES 스태킹 — "ES ≥ 생명"
    // 완화 조건은 하이브리드(PB: 생명 3.2k+ES 0.9k 대표, EHP 원천은 럭키 막기)까지 CI 경로로 끌고 가
    // 유리대포 CI 대안을 만들었다(실측 EHP 30k vs 벤치 132.8k — 31단계 분해로 방향 오류 확정).
    boolean esDominant = b.energyShield() >= ES_ARCHETYPE_FLOOR && b.energyShield() >= b.life() * 2;
    return ciKeystone || esDominant;
  }

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
    // 옛 호출부(이력 저장 기본 on) 호환 — 실제 종단 구현은 saveHistory 를 받는 오버로드
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
        anoint,
        true);
  }

  /**
   * @param saveHistory 완료 결과를 최근 결과 이력(sim/history)에 남길지. 사용자 실행은 true, QA 배터리는 false 로 호출해 고정트리
   *     픽스처 잡이 이력을 오염시키지 않게 한다.
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
      String anoint,
      boolean saveHistory) {
    if (!isAvailable()) {
      return false;
    }
    // running 선점을 필드 초기화보다 먼저 — 실행 중 재호출(더블클릭/동시 요청)이 거부되기 전에
    // 아래 잡 스코프 필드들을 덮어쓰면 진행 중인 잡의 중간 상태가 오염된다(실측: Arc 52.39M→49.48M).
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    boolean started = false; // 스레드 기동 전 이탈(검증 실패/예외) 시 finally 에서 선점 해제
    try {
      this.saveHistoryForRun = saveHistory; // 완료 시 이력 저장 여부(잡마다)
      this.fixedTree = parseNodeIds(treeNodes);
      this.fixedTattoos = parseTattoos(tattoos);
      this.userTattoos = this.fixedTattoos;
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
      // objective 자동(성향 구동): 빈값/"auto" 면 **balanced** 로 간다. balanced 는 이미 이 (전직×스킬)의 poe.ninja 실빌드
      //   EHP·최대피격·저항 중앙값을 생존 목표 시드로 쓰므로(=실빌드 프로파일 존중), 그 바닥을 지키며 DPS 를 최대화한다.
      //   ⚠ 성향(lean)을 raw dps/ehp objective 로 직결하면 안 된다: dps objective 는 EHP 바닥이 없어 실측 200k EHP
      // 아키타입도
      //   EHP 2k 유리대포를 낸다(실측 Penance Brand Elementalist). 성향은 화면 표시·강점축 강조(향후 가중)용이지 목표 대체가 아니다.
      //   명시 objective(dps/ehp/balanced)는 그대로 존중 — QA 배터리는 명시 호출이라 기준선 불변.
      String resolvedObjective = objective;
      if (resolvedObjective == null
          || resolvedObjective.isBlank()
          || "auto".equals(resolvedObjective)) {
        String skillName = gem != null ? gem.name() : null;
        ArchetypeBenchmark bench = skillName != null ? ninjaBenchmark(ascendancy, skillName) : null;
        resolvedObjective = "balanced";
        log(
            "성향 자동 목표: balanced (실빌드 프로파일 시드"
                + (bench != null ? ", " + bench.mainSkill() + " lean=" + bench.lean() : ", 미매칭")
                + ")");
      }
      String normalizedObjective =
          "ehp".equals(resolvedObjective) || "balanced".equals(resolvedObjective)
              ? resolvedObjective
              : "dps";
      this.enemyScenario = SCENARIO_KO.containsKey(scenario) ? scenario : "Pinnacle"; // 화이트리스트
      this.combatBuffs = buffs;
      this.secondaryAscendId = 0; // 혈맹 선택 초기화(잡마다)
      this.selectedAuras = new ArrayList<>(); // 방어 오라 초기화(잡마다)
      this.blessingAura = null;
      this.currentKeywords = List.of(); // 키워드 초기화(잡마다)
      this.tattooAllocated = Set.of(); // 문신 할당-이웃 판정 기준 초기화(잡마다)
      this.currentAnoint = null; // 아뮬렛 도유 초기화(잡마다)
      this.supportLevelOverride = Map.of(); // 보조젬 레벨 하향 초기화(잡마다)
      this.feasibilitySteering = false; // 실현 가능성 조향 초기화(잡마다)
      this.selfBurnRun = false; // 자가연소 지속력 게이트 초기화(잡마다)
      this.multiActorBuild = false; // #235 다중 액터(토템/미니언) 총합 지표 초기화(잡마다)
      this.targetMaxHit = MAXHIT_FLOOR; // poe.ninja 생존 목표치 초기화(스킬 확정 후 재설정)
      this.targetEhp = EHP_FLOOR;
      this.seededKeystones = List.of(); // 시드 키스톤 초기화(잡마다)
      // ⚠ 메타 마스터리는 balanced 의 setSurvivalTargets 에서만 채워지므로 **잡마다 여기서 리셋** —
      //   안 하면 직전 balanced 잡의 메타 세트가 dps/ehp 잡으로 누출돼 기준선이 이탈한다
      //   (실사고: RF balanced 후 arc 41.9M→42.5M, 사이클론 9.9M→8.9M 오염, #161 계열).
      this.metaMasteries = Set.of();
      this.targetChaosRes = 0; // P2 카오스 저항 목표도 같은 누출 계열 — 잡마다 리셋
      this.targetSpellSuppress = 0; // 주문 억제 목표 — 같은 누출 계열
      this.targetSpellBlock = 0; // 주문 막기 목표 — 같은 누출 계열
      this.balancedJob = false; // balanced 분기 플래그 — 같은 누출 계열
      this.convexSurvivalPhase = false; // 잡마다 리셋(누출되면 다음 잡의 탐색이 왜곡된다)
      this.guardSkill = null; // 잡마다 리셋
      this.guardSupport = null;
      this.supportRematchPool = List.of();
      this.jewelRematchPool = List.of();
      this.additionalSkillSupports.clear(); // 추가 스킬 보조젬(1b) — 잡마다 리셋(누출 방지)
      this.metaWeaponClasses = Set.of(); // P1② 메타 무기 구성 — 잡마다 리셋(누출 방지)
      this.metaOffhandShield = false;
      this.metaPantheonMajor = ""; // P1③ 메타 판테온 — 잡마다 리셋(누출 방지)
      this.metaPantheonMinor = "";
      this.esArchetype = false; // ES/CI 아키타입 플래그 초기화(잡마다)
      this.targetEs = 0d;
      this.forceEsBase = false;
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
      this.cancelRequested = false; // 잡마다 취소 플래그 초기화
      Thread thread = new Thread(() -> runJob(gem, normalizedObjective), "poe-optimize");
      thread.setDaemon(true);
      this.jobThread = thread;
      thread.start();
      started = true;
      return true;
    } finally {
      if (!started) {
        running.set(false);
      }
    }
  }

  private void runJob(PoeGem gemArg, String objective) {
    long startedAt = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    try {
      String objectiveKey = objective; // objectiveOf 가 objective 문자열을 해석 (dps/ehp/balanced)
      this.balancedJob = "balanced".equals(objectiveKey);
      // 메인 스킬 결정: slug 지정이면 그것. 아니면 —
      //   (a) 사용자가 스킬을 선택했으면 **선택 순서상 첫 스킬**을 메인으로(사용자 의도 존중).
      //       ⚠ 예전엔 DPS 최고를 메인으로 골라, RF+FireTrap 선택 시 DPS 높은 Fire Trap 이 메인이 되고 RF 가 보조로 밀려
      //         "RF 빌드"가 아니라 "RF 얹은 트랩 빌드"가 나왔다(사용자 지적). 대표 스킬(첫 선택)이 곧 빌드 정체성이다.
      //   (b) 스킬 미선택(유니크 anchor) → 전체 데미지 스킬 중 DPS 최고 자동선택.
      PoeGem resolved = gemArg;
      if (resolved == null) {
        List<PoeGem> damageSelected =
            additionalSkills.stream().filter(this::isDamageSkill).toList();
        if (!damageSelected.isEmpty()) {
          resolved = damageSelected.get(0); // (a) 선택 순서상 첫 데미지 스킬
        } else if (!additionalSkills.isEmpty()) {
          resolved = additionalSkills.get(0); // 선택이 전부 비데미지면 첫 스킬
        } else {
          resolved = pickBestSkill(executor, objectiveKey, allDamageSkills()); // (b) 자동선택
        }
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
      List<String> baseKeywords = keywords(gem, objective);
      // 원소 태그가 없는 공격 스킬(회오리 사격 등)은 **피해 축을 스스로 골라야** 한다.
      //   태그만 보면 키워드가 [damage, attack, accuracy, projectile] 뿐이라 크래프트·평가가 물리로 굳고,
      //   그 결과 원소 활 빌드에 잔혹 보조(원소·카오스 피해 0)까지 낀다(실측: 회오리 사격 569,816,
      //   같은 활/데드아이인 번개 화살은 원소 태그 덕에 3,507,801).
      //   축마다 기준 무기를 만들어 엔진으로 재 보고 가장 센 축의 키워드를 이번 잡에 쓴다.
      // 람다들이 참조하므로 effectively final 이어야 한다 — 축 선택까지 끝낸 값을 한 번에 고정한다.
      final List<String> keywords = pickDamageAxis(gem, objective, baseKeywords);
      this.currentKeywords = keywords;
      this.selfBurnRun = isSelfBurnLifeScaled(gem); // #1 RF 류면 선택 지표에 지속력 게이트
      // #235 메인 스킬이 미니언이면 선택/표시 지표를 미니언수 합산 총합(FullDPS)으로 — ninja 총합 표기 정합.
      //   ⚠ 토템은 제외: calcFullDPS 가 토템은 미니언처럼 count 집계하지 않아 FullDPS 기반 탐색이 더 나쁜 빌드로
      //   흘러 AW 3,590,847→2,692,513 회귀(실측). 토템 count 스케일링은 별도 과제.
      this.multiActorBuild = gem.tags() != null && gem.tags().contains("Minion");
      // balanced 잡의 생존 목표치를 poe.ninja 아키타입 실측 중앙값으로. 매칭 없으면 정적 floor 유지.
      //   멀티스킬 선택(RF+화염덫 등)이면 **선택 데미지 스킬 전부를 쓰는 캐릭터만** 집계한 조합 시드를 우선.
      if ("balanced".equals(objectiveKey)) {
        List<String> comboNames = new ArrayList<>();
        comboNames.add(gem.name());
        for (PoeGem g : additionalSkills) {
          if (isDamageSkill(g) && !comboNames.contains(g.name())) {
            comboNames.add(g.name());
          }
        }
        setSurvivalTargets(fixedAscendancy, gem.name(), comboNames);
      }
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
      Map<Integer, Equipped> jewels = new LinkedHashMap<>(); // 소켓 노드 id → 장착 주얼(유니크/레어)

      // 공격 스킬은 **무기가 없으면 피해가 0**이다. 그런데 보조젬·트리 단계는 아이템 단계보다 먼저 돌아,
      // 무기 없는 상태로 후보를 재면 전부 0 이 나와 사실상 무작위로 뽑힌다
      // (실측: 번개 화살 보조젬 채택값 0·0·0·1·1 → 마무리 타격·카오스 피해 추가 같은 엉뚱한 조합).
      // 그래서 공격 스킬이면 **임시 기준 무기**(그 스킬이 쓸 수 있는 베이스의 크래프트 레어)를 미리 끼운다.
      // 아이템 단계가 다시 최적 무기를 고르므로 최종 결과를 묶어 두지 않는다.
      // ⚠ 활 스킬로 한정한다. "Attack" 전부에 끼웠더니 **방패 공격·쌍수 스킬이 0 DPS 로 무너졌다**
      //   (실측: 신성한 폭발 0, 쌍수 강타 0 — 기준 무기가 그 슬롯 구성을 선점해 방패/오프핸드 무기가 못 들어감).
      //   원래 목적도 활 빌드(무기 없으면 DPS 0)라 범위를 그대로 좁힌다.
      if (gem.tags() != null && gem.tags().contains("Bow")) {
        // **무기 한 자루만** 끼운다. 방어구까지 한 벌로 채워도 봤지만 합계로 손해였다(실측, 세 공격 아키타입):
        //   기준 없음 4,669,195 → 무기만 6,755,249 → 한 벌 4,903,225.
        //   전 슬롯 T1 레어는 초기 문맥을 너무 강하게 고정해 이후 유니크·문맥 선택을 밀어낸다.
        if (!items.containsKey(Slot.WEAPON)) {
          RareItem starterWeapon = craftRare(Slot.WEAPON, gem, keywords, 0.0);
          if (starterWeapon != null) {
            items.put(Slot.WEAPON, Equipped.ofRare(starterWeapon));
            log("기준 무기 임시 장착(공격 스킬): " + starterWeapon.baseType() + " — 보조젬이 0 DPS 에서 고르지 않도록");
          }
        }
      }

      enterPhase("baseline");
      Map<String, Double> baselineValues =
          poePobEngineService.calculateValues(
              buildXml(gem, supports, className, ascendancy, ascendancyNodes, allocated, items));
      double baseline = objectiveOf(baselineValues, objectiveKey);
      evalCount.incrementAndGet();
      double current = baseline;
      log("기준값(젬 단독): " + format(baseline) + " / 전직 " + ascendancy);

      // 랭킹 전용 문맥 — 보조젬·트리 후보의 **순위를 매길 때만** 쓰는 가상 장비 한 벌.
      //   장비가 없으면 값이 0~7 수준이라 후보 순위가 사실상 노이즈다(실측: 회오리 사격이 잔혹 보조(원소 피해 0)를
      //   끼고, 폭발 덫은 보조젬을 하나만 달았다). 이걸 빌드에 **저장**하면 이후 유니크 선택을 밀어내 손해였으므로
      //   (합계 4,903,225 vs 무기만 6,755,249) 저장하지 않고 평가에만 쓴다.
      //   ⚠ 조건은 태그가 아니라 **측정한 기준값**이다 — "Attack 태그"로 걸었더니 덫·주문 계열의 같은 증상을
      //     놓쳤다(폭발 덫: 중앙값의 0.37배). 젬 단독으로 잴 수 있는 빌드(Arc 등)는 그대로 둔다.
      Map<Slot, Equipped> rankingItems = new EnumMap<>(items);
      // ⚠ 조건을 "기준값 < 1,000"으로 넓혀 덫 계열까지 켜 봤더니 **오히려 -53%**였다
      //   (폭발 덫 7,485,109 → 3,539,943). 보조젬 순위는 매겨졌지만(1개 → 5개) 랭킹용 가상 장비가
      //   물리 쪽으로 치우쳐 화염 덫에 잔혹 보조(원소 피해 0)를 끼우는 등 **틀린 순위**를 만들었다.
      //   이득이 확인된 활 계열(무기가 곧 피해원)로만 유지한다.
      if (gem.tags() != null
          && gem.tags().contains("Attack")
          && baseline < RANKING_CONTEXT_BASELINE) {
        for (Slot slot :
            new Slot[] {
              Slot.BODY, Slot.HELMET, Slot.GLOVES, Slot.BOOTS,
              Slot.AMULET, Slot.RING1, Slot.RING2, Slot.BELT
            }) {
          if (rankingItems.containsKey(slot)) {
            continue;
          }
          RareItem provisional = craftRare(slot, gem, keywords, 0.0);
          if (provisional != null) {
            rankingItems.put(slot, Equipped.ofRare(provisional));
          }
        }
        if (rankingItems.size() > items.size()) {
          log("랭킹 전용 문맥 사용(기준값 " + format(baseline) + " — 젬 단독으로는 순위를 못 가림)");
        }
      }

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
                        rankingItems),
                objectiveKey);

        List<PoeGem> shortlist =
            firstRound.entrySet().stream()
                .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                .limit(SUPPORT_SHORTLIST)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        // 최종 재대결용으로 보관 — 보조젬은 **랭킹 문맥**(기준 무기만 든 상태, DPS 수십)에서 정해지는데
        //   완성 빌드(수백만)에서 순위가 뒤집힐 수 있다. 실측(번개 화살): 채택된 젬과 신기루 궁수·규칙 뒤집기의
        //   차이가 라운드마다 1~2점(동점 포함)이었고, 대표 실빌드는 바로 그 둘을 쓴다.
        this.supportRematchPool = List.copyOf(shortlist);

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
                              rankingItems),
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
          // 채택 라운드의 상위 후보를 함께 남긴다 — "실빌드가 쓰는 젬이 우리 평가에서 몇 위였나" 를
          //   추측이 아니라 순위로 답하기 위한 것(번개 화살 대표는 신기루 궁수·삼위일체를 쓰는데 우리는 미채택).
          log(
              "  후보 상위: "
                  + round.entrySet().stream()
                      .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                      .limit(6)
                      .map(e -> koName(e.getKey()) + " " + format(e.getValue()))
                      .collect(java.util.stream.Collectors.joining(" · ")));
        }
      }

      // ── 1b) 추가 스킬 보조젬 — 추가 데미지 스킬(화염덫 등)은 실빌드 4링크가 표준인데 단독 젬으로
      // emit 되어 벤치(스킬별 DPS 3.8M)와 비교가 불공정했다. 메인 objective(CombinedDPS=메인 전용)에는
      // 무기여라 일반 greedy 로는 영원히 미선발 — **그 스킬을 임시 메인으로 둔 XML** 로 자체 DPS("dps")
      // 기준 greedy(4링크 가정, 서포트 3개). 메인 지표·기준선 불변(디버프성 서포트가 메인에 영향을 주면
      // 그것대로 정당한 반영).
      for (PoeGem extra : additionalSkills) {
        if (!isDamageSkill(extra)) {
          continue;
        }
        List<PoeGem> extraPool =
            poeGemDataService.search(null, "support", "all", null).stream()
                .filter(s -> !s.levels().isEmpty())
                .filter(this::isProvidedSupport)
                .filter(s -> supportCompatible(extra, s))
                .toList();
        List<PoeGem> extraPicked = new ArrayList<>();
        Map<PoeGem, Double> extraFirst =
            evalBatch(
                executor,
                extraPool,
                s ->
                    buildXml(
                        extra,
                        List.of(s),
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        rankingItems),
                "dps");
        List<PoeGem> extraShort =
            extraFirst.entrySet().stream()
                .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                .limit(SUPPORT_SHORTLIST)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        double extraCur = -1d;
        while (extraPicked.size() < 3 && !extraShort.isEmpty()) {
          Map<PoeGem, Double> round =
              extraPicked.isEmpty()
                  ? extraFirst
                  : evalBatch(
                      executor,
                      extraShort,
                      s ->
                          buildXml(
                              extra,
                              joined(extraPicked, s),
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              rankingItems),
                      "dps");
          Map.Entry<PoeGem, Double> best =
              round.entrySet().stream()
                  .filter(e -> extraShort.contains(e.getKey()))
                  .max(Map.Entry.comparingByValue())
                  .orElse(null);
          if (best == null || best.getValue() <= extraCur * 1.005) {
            break;
          }
          extraPicked.add(best.getKey());
          extraShort.remove(best.getKey());
          extraCur = best.getValue();
        }
        if (!extraPicked.isEmpty()) {
          additionalSkillSupports.put(extra.slug(), List.copyOf(extraPicked));
          log(
              "추가 스킬 보조젬: "
                  + koName(extra)
                  + " → "
                  + extraPicked.stream()
                      .map(this::koName)
                      .collect(java.util.stream.Collectors.joining(", ")));
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
        // poe.ninja 시드 키스톤 주입 — 키워드 미매칭(방어) 키스톤(CI/MoM/EB/피의마법 등)은 실빌드가 흔히 쓰나
        //   위 점수화(데미지 키워드)에서 0점이라 후보에서 빠진다. 실측 상위 키스톤을 baseline 점수로 후보에 넣어
        //   **평가 대상**이 되게 한다(채택은 여전히 full PoB 실측 이득 기준 → 강제 아님). 결정성: id 정렬 순회.
        if (!seededKeystones.isEmpty() && SEED_KEYSTONES_ENABLED) {
          int avg =
              candidateScores.isEmpty()
                  ? 40
                  : (int)
                      candidateScores.values().stream()
                          .mapToInt(Integer::intValue)
                          .average()
                          .orElse(40);
          int ksBaseline = Math.max(30, avg);
          int injected = 0;
          for (PoeTreeGraphService.TreeNode node : poeTreeGraphService.searchCandidates()) {
            if (!"keystone".equals(node.type()) || candidateScores.containsKey(node)) {
              continue;
            }
            String nm = node.name();
            if (nm != null && seededKeystones.stream().anyMatch(k -> k.equalsIgnoreCase(nm))) {
              candidateScores.put(node, ksBaseline);
              injected++;
            }
          }
          if (injected > 0) {
            log("poe.ninja 시드 키스톤 후보 주입: " + injected + "개 (baseline " + ksBaseline + ")");
          }
        }
        log("트리 후보 노터블/키스톤: " + candidateScores.size() + "개");

        // 주얼 소켓 경로용으로 일부 예약.
        // (마스터리용 추가 예약도 재 봤지만 — 4점 예약 시 마스터리 4개 채택으로 잡 내부 이득 +43%,
        //  예약 없이 남는 점으로 1개 채택 시 +41% — 최종 DPS 는 2.44M vs 2.47M 로 차이가 없어 도입하지 않았다)
        // ⚠ 위 실험은 dps 목표 기준. P1 메타 웜스타트(balanced + 메타 마스터리 존재)에선 마스터리 단계 도달 시
        //   잔여 포인트가 0~1 이라(실측: RF 치프틴 후보 1개) 경로 후보가 전부 예산 탈락 — 실빌드가 81% 찍는
        //   재생 숙련이 시도조차 못 된다. 메타가 있을 때만 예약을 부활한다(dps 잡은 메타 빈 집합 → 불변).
        int treeBudget =
            POINT_BUDGET
                - JEWEL_RESERVE
                - (!SEED_MASTERY_ENABLED || metaMasteries.isEmpty() ? 0 : META_MASTERY_RESERVE);
        // ⚠ CLUSTER_RESERVE 를 여기서 빼는 설계는 실패 롤백(2026-08-04): 트리가 11pt 약해진 중간 시점의
        //   낮아진 current 와 비교하니 정크 클러스터도 "개선"으로 채택돼 최종 붕괴(712,940→407,389).
        //   클러스터 예산 경쟁은 최종 컨텍스트 스왑(가장 값싼 가지 제거와 교환) 설계가 필요 — 예약 금지.
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
                        gem, supports, className, ascendancy, ascendancyNodes, trial, rankingItems);
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
                  jewels.put(fixed.getKey(), Equipped.ofUnique(unique));
                  log("주얼 고정(사용자 지정): " + unique.name());
                });
      }
      // 타임리스는 반경 노드 변환 계산이 무거워(시드→노드 매핑 로드) 자동 탐색 풀에 넣으면 잡 전체가 느려진다.
      // 대신 uniqueItemText() 가 타임리스 문구를 붙여, **트리에서 직접 꽂거나 강제 장착할 때** 제대로 계산되게 했다.
      List<PoeUniqueItem> jewelCandidates = globalJewelCandidates(keywords);
      // 최종 재대결용 보관 — 주얼은 **아이템보다 먼저** 정해진다(실측 채택값 502~613, 최종은 수천만).
      //   보조젬과 같은 결함 구조라 같은 처방을 쓴다(보조젬 재대결 실측: 번개 화살 +39.5%).
      this.jewelRematchPool = List.copyOf(jewelCandidates);
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
                  .filter(Equipped::isUnique)
                  .map(e -> e.unique().name())
                  .collect(
                      java.util.stream.Collectors.groupingBy(
                          n -> n, java.util.stream.Collectors.counting()));
          // 유니크 후보만(같은 주얼 Limited-to 상한 준수)로 greedy 진행.
          //   ⚠ 제작 레어 주얼을 여기(주얼 단계=아이템 단계 前)에 넣었더니, 빈약한 부분 빌드 기준으로
          //   제작 주얼이 유니크를 이겨 조기 채택되나 최종 빌드에선 유니크가 더 나아 회귀했다(cyclone 11.70M→10.32M).
          //   → 제작 레어 주얼은 아래 finalizeJewelsWithRares(최종 빌드 기준 단조-개선 패스)에서만 채택한다.
          List<Equipped> candidatesEq = new ArrayList<>();
          jewelCandidates.stream()
              .filter(j -> used.getOrDefault(j.name(), 0L) < jewelLimit(j.name()))
              .map(Equipped::ofUnique)
              .forEach(candidatesEq::add);
          if (candidatesEq.isEmpty()) {
            continue;
          }
          // 이 소켓에 후보 주얼들을 꽂아 평가 (경로도 함께 할당)
          Map<Equipped, Double> results =
              evalBatch(
                  executor,
                  candidatesEq,
                  jewel -> {
                    Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
                    trialNodes.addAll(socketPath.path());
                    Map<Integer, Equipped> trialJewels = new LinkedHashMap<>(jewels);
                    trialJewels.put(socketPath.socketId(), jewel);
                    return buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        trialNodes,
                        rankingItems,
                        trialJewels);
                  },
                  objectiveKey);
          Map.Entry<Equipped, Double> best =
              results.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (best != null && best.getValue() > current * 1.003) {
            allocated.addAll(socketPath.path());
            points += socketPath.path().size();
            jewels.put(socketPath.socketId(), best.getKey());
            current = best.getValue();
            log(
                "주얼 소켓: "
                    + jewelLabel(best.getKey())
                    + " (+"
                    + socketPath.path().size()
                    + "pt) → "
                    + format(current));
          }
        }
      }

      // 랭킹 문맥(가상 장비)에서 매긴 값은 **실제 빌드보다 크다**. 그대로 두면 current 가 부풀어 이후 단계가
      // 아무것도 못 넘는다(실측: 주얼 단계가 45후보 중 0개 채택 → 주얼 없는 빌드). 트리가 끝나면 실제 문맥으로
      // 기준값을 다시 잡는다.
      if (!rankingItems.equals(items)) {
        double realCurrent =
            objectiveOf(
                poePobEngineService.calculateValues(
                    buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items,
                        jewels)),
                objectiveKey);
        evalCount.incrementAndGet();
        log("기준값 재산정(실제 장비): " + format(current) + " → " + format(realCurrent));
        current = realCurrent;
      }

      // ── 4) 아이템 greedy (슬롯 순회) — 고유 후보 + 생성 레어(최상위 티어) 를 함께 평가 ──
      enterPhase("items");
      feasibilitySteering = true; // 지금부터 선택 지표에 요구치 부족 페널티(사유는 필드 주석)
      // 기준값(current)은 방금까지 **무페널티**로 계산된 값 — 그대로 두면 강제 유니크로 부족이 큰 잡에서
      // 모든 후보가 "기준 미달"이 되어 아무것도 채택되지 않는다(실측: EHP 798k→33k 붕괴).
      // 같은 잣대로 비교하도록 현 상태를 페널티 포함으로 재계산해 기준을 맞춘다.
      current =
          objectiveOf(
              poePobEngineService.calculateValues(
                  buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      items,
                      jewels)),
              objectiveKey);
      evalCount.incrementAndGet();
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
        // 오라 단계는 조향을 잠시 끈다 — 오라 요구치(결의 힘155 등)는 채택 후 강등 사다리가
        // 레벨을 낮춰 맞출 수 있는데, 페널티를 걸면 채택 자체가 선제 차단된다
        // (실측: EHP 강제 잡에서 오라 0개). 단계 끝의 repairAttributeShortfalls 가 뒷정리한다.
        feasibilitySteering = false;
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
            double unreservedLife = trialValues.getOrDefault("LifeUnreserved", 1d);
            if (unreserved < MIN_UNRESERVED_MANA || unreservedLife < MIN_UNRESERVED_LIFE) {
              reserveBlocked.add(cand.getKey());
              // 부족량 = 미예약 수치가 하한 아래로 내려간 만큼(결과 화면에서 사유 설명)
              blockedAuraShortfall.put(
                  cand.getKey(),
                  (int) Math.ceil(Math.max(-unreserved, MIN_UNRESERVED_LIFE - unreservedLife)));
              log(
                  "오라 예약 초과 제외: "
                      + cand.getKey().name()
                      + " (미예약 마나 "
                      + Math.round(unreserved)
                      + " / 생명력 "
                      + Math.round(unreservedLife)
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

        // ── 예약 효율 확장 트라이얼 — **도입 실패로 비활성**(2026-08-04). 「노터블 경로 + 오라 1개」 묶음
        // 실측은 두 가지 착시에 걸렸다: ① 혈마법 빌드에선 마나 예약 효율(Sovereignty)·Clarity 가 무의미한데
        // **경로 중간 노드 스탯**이 +3% 착시 이득을 만들어 채택됨 ② 소모한 3pt 가 하류 마스터리 예산을
        // 밀어내(막기 숙련 +4pt 탈락) 최종 EHP −11%·막기 39→23 회귀. 중간 단계 채택은 하류 기회비용을
        // 못 본다 — 이 레버는 최종 컨텍스트 후기 패스로 재설계해야 하며, 그때도 예약이 실제 병목인
        // 빌드(차단 오라 존재·비혈마법)로 게이트해야 한다. (false: 코드는 재설계 참고용으로 보존)
        if (false && "balanced".equals(objectiveKey)) {
          record ResTrial(PoeTreeGraphService.TreeNode notable, List<Integer> path, PoeGem aura) {}
          List<ResTrial> resTrials = new ArrayList<>();
          List<PoeGem> chosenNow = List.copyOf(selectedAuras);
          List<PoeGem> auraLeft = auraPool.stream().filter(a -> !chosenNow.contains(a)).toList();
          if (!auraLeft.isEmpty()) {
            List<PoeTreeGraphService.TreeNode> resNotables =
                poeTreeGraphService.searchCandidates().stream()
                    .filter(n -> "notable".equals(n.type()) && !allocated.contains(n.id()))
                    .filter(
                        n ->
                            n.stats() != null
                                && n.stats().stream()
                                    .anyMatch(
                                        s ->
                                            s != null
                                                && s.toLowerCase(Locale.ROOT)
                                                    .contains("reservation efficiency")))
                    .toList();
            record ResPath(PoeTreeGraphService.TreeNode notable, List<Integer> path) {}
            List<ResPath> resPaths = new ArrayList<>();
            for (PoeTreeGraphService.TreeNode rn : resNotables) {
              List<Integer> path = poeTreeGraphService.shortestPath(allocated, rn.id());
              if (path == null
                  || path.isEmpty()
                  || path.size() > 5
                  || points + path.size() > POINT_BUDGET) {
                continue;
              }
              boolean crossesKeystone =
                  path.stream()
                      .anyMatch(
                          id -> {
                            PoeTreeGraphService.TreeNode pn = poeTreeGraphService.node(id);
                            return pn != null && "keystone".equals(pn.type());
                          });
              if (crossesKeystone) {
                continue;
              }
              resPaths.add(new ResPath(rn, path));
            }
            resPaths.sort(Comparator.comparingInt(rp -> rp.path().size()));
            for (ResPath rp : resPaths.subList(0, Math.min(3, resPaths.size()))) {
              for (PoeGem aura : auraLeft) {
                resTrials.add(new ResTrial(rp.notable(), rp.path(), aura));
              }
            }
          }
          if (!resTrials.isEmpty()) {
            Map<ResTrial, Double> resResults =
                evalBatch(
                    executor,
                    resTrials,
                    trial -> {
                      Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
                      trialNodes.addAll(trial.path());
                      return buildXmlAuras(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          trialNodes,
                          items,
                          jewels,
                          joined(chosenNow, trial.aura()));
                    },
                    objectiveKey);
            List<Map.Entry<ResTrial, Double>> resRanked =
                resResults.entrySet().stream()
                    .sorted(Map.Entry.<ResTrial, Double>comparingByValue().reversed())
                    .toList();
            for (Map.Entry<ResTrial, Double> cand : resRanked) {
              if (cand.getValue() <= auraCurrent * 1.003) {
                break; // 최고 후보도 이득 없음
              }
              ResTrial trial = cand.getKey();
              Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
              trialNodes.addAll(trial.path());
              Map<String, Double> trialValues =
                  poePobEngineService.calculateValues(
                      buildXmlAuras(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          trialNodes,
                          items,
                          jewels,
                          joined(chosenNow, trial.aura())));
              evalCount.incrementAndGet();
              double unreserved = trialValues.getOrDefault("ManaUnreserved", 0d);
              double unreservedLife = trialValues.getOrDefault("LifeUnreserved", 1d);
              if (unreserved < MIN_UNRESERVED_MANA || unreservedLife < MIN_UNRESERVED_LIFE) {
                continue; // 예약 초과 — 다음 후보
              }
              allocated.addAll(trial.path());
              points += trial.path().size();
              selectedAuras.add(trial.aura());
              auraCurrent = cand.getValue();
              log(
                  "예약 효율 확장: "
                      + trial.notable().name()
                      + " (+"
                      + trial.path().size()
                      + "pt) + 오라 "
                      + trial.aura().name()
                      + " → "
                      + format(auraCurrent));
              break;
            }
          }
        }
      }

      // ── 키스톤 후회 패스(혈마법) — balanced 전용. 트리 greedy(오라 이전)는 혈마법의 기회비용
      // (마나 예약 오라 스택)을 못 본다: 실측 RF 실빌드는 마나 96.8% 예약으로 오라를 쌓는데, 우리는
      // 혈마법 채택→마나 0→오라가 생명 예약 2개에서 멈췄다. 오라 확정 후 「혈마법 제거 + 오라 추가
      // greedy 재실행」을 통째 트라이얼로 실측해 이득일 때만 채택(비-balanced 는 미실행 — dps 기준선 불변).
      if ("balanced".equals(objectiveKey)) {
        Integer bloodMagicId = null;
        for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.searchCandidates()) {
          if ("keystone".equals(n.type())
              && n.name() != null
              && n.name().equalsIgnoreCase("Blood Magic")
              && allocated.contains(n.id())) {
            bloodMagicId = n.id();
            break;
          }
        }
        if (bloodMagicId != null) {
          double base =
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
                          selectedAuras)),
                  objectiveKey);
          evalCount.incrementAndGet();
          Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
          trialNodes.remove(bloodMagicId);
          List<PoeGem> trialAuras = new ArrayList<>(selectedAuras);
          double trialCurrent =
              objectiveOf(
                  poePobEngineService.calculateValues(
                      buildXmlAuras(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          trialNodes,
                          items,
                          jewels,
                          trialAuras)),
                  objectiveKey);
          evalCount.incrementAndGet();
          List<PoeGem> regretPool =
              poeGemDataService.search(null, "active", "all", null).stream()
                  .filter(a -> AURA_NAMES.contains(a.name()))
                  .filter(a -> !a.levels().isEmpty())
                  .filter(a -> additionalSkills.stream().noneMatch(x -> x.slug().equals(a.slug())))
                  .toList();
          Set<PoeGem> regretBlocked = new java.util.HashSet<>();
          boolean regretImproved = true;
          while (regretImproved && trialAuras.size() < MAX_AURAS) {
            regretImproved = false;
            List<PoeGem> chosen = List.copyOf(trialAuras);
            List<PoeGem> remaining =
                regretPool.stream()
                    .filter(a -> !chosen.contains(a) && !regretBlocked.contains(a))
                    .toList();
            if (remaining.isEmpty()) {
              break;
            }
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
                            trialNodes,
                            items,
                            jewels,
                            joined(chosen, aura)),
                    objectiveKey);
            List<Map.Entry<PoeGem, Double>> ranked =
                round.entrySet().stream()
                    .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                    .toList();
            for (Map.Entry<PoeGem, Double> cand : ranked) {
              if (cand.getValue() <= trialCurrent * 1.003) {
                break;
              }
              Map<String, Double> trialValues =
                  poePobEngineService.calculateValues(
                      buildXmlAuras(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          trialNodes,
                          items,
                          jewels,
                          joined(chosen, cand.getKey())));
              evalCount.incrementAndGet();
              double unreserved = trialValues.getOrDefault("ManaUnreserved", 0d);
              double unreservedLife = trialValues.getOrDefault("LifeUnreserved", 1d);
              if (unreserved < MIN_UNRESERVED_MANA || unreservedLife < MIN_UNRESERVED_LIFE) {
                regretBlocked.add(cand.getKey());
                continue;
              }
              trialAuras.add(cand.getKey());
              trialCurrent = cand.getValue();
              regretImproved = true;
              break;
            }
          }
          if (trialCurrent > base * 1.003) {
            allocated.remove(bloodMagicId);
            List<String> added =
                trialAuras.stream()
                    .filter(a -> !selectedAuras.contains(a))
                    .map(PoeGem::name)
                    .toList();
            selectedAuras.clear();
            selectedAuras.addAll(trialAuras);
            log(
                "키스톤 후회(혈마법 제거): 오라 추가 "
                    + (added.isEmpty() ? "없음(마나 회복만으로 이득)" : String.join(", ", added))
                    + " → "
                    + format(trialCurrent)
                    + " (유지 시 "
                    + format(base)
                    + ")");
          } else {
            log("키스톤 후회(혈마법): 유지가 우세 — 제거안 " + format(trialCurrent) + " vs 유지 " + format(base));
          }
        }
      }

      feasibilitySteering = true; // 오라 단계 예외 종료(위 주석) — 이후 단계는 다시 조향
      // 오라가 젬 총 요구치를 다시 올렸을 수 있다(Grace=민첩 등) — 보정 한 번 더.
      // (이미 충족이면 1회 평가로 즉시 반환하는 값싼 재검이다)
      repairAttributeShortfalls(
          items, gem, supports, className, ascendancy, ascendancyNodes, allocated, jewels);

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
        // P1 메타 마스터리 호스트 경로 — 후보 id → 할당 경로(마지막이 마스터리 노드). 인접 후보는 [자기 자신].
        Map<Integer, List<Integer>> masteryPaths = new LinkedHashMap<>();
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
              // P1 메타 마스터리 웜스타트 — 실빌드 채택률 40%+ 효과를 가진 마스터리는 후보 상한에서
              // 키워드 점수에 밀려 잘리지 않도록 대폭 가산(재생/방어 효과는 키워드 점수가 낮다).
              if (node.masteryEffects().stream().anyMatch(this::isMetaMasteryEffect)) {
                best += 1000;
              }
              newMasteries.add(neighbor);
              scored.add(new Candidate(neighbor, best));
            }
          }
          // P1 메타 마스터리 호스트 — 인접이 아니어도 **최단 경로로 도달 가능한**(예산 내, 경로 ≤5pt) 메타 효과
          // 보유 마스터리를 후보에 넣는다. 실측: RF 치프틴에서 인접 후보가 1개뿐이라 실빌드가 81% 찍는
          // 재생 숙련이 시도조차 되지 않았다. 채택은 여전히 경로 포함 실측 이득 기준(강제 아님).
          if (SEED_MASTERY_ENABLED && !metaMasteries.isEmpty()) {
            for (PoeTreeGraphService.TreeNode mn : poeTreeGraphService.masteryNodes()) {
              if (allocated.contains(mn.id())
                  || newMasteries.contains(mn.id())
                  || mn.ascendancy() != null
                  || mn.masteryEffects() == null
                  || mn.masteryEffects().stream().noneMatch(this::isMetaMasteryEffect)) {
                continue;
              }
              List<Integer> path = poeTreeGraphService.shortestPath(allocated, mn.id());
              if (path == null
                  || path.isEmpty()
                  || path.size() > 5
                  || points + path.size() > POINT_BUDGET) {
                continue;
              }
              // 경로가 키스톤을 지나면 제외 — 키스톤은 찍는 순간 기제가 통째로 바뀌는 노드라 경로 부수
              // 할당으로 켜지면 안 된다. 실사고: RF 막기 숙련 +4pt 경로가 **혈마법**을 통과 채택 → 마나 0
              // → 오라가 생명 예약 2개 상한(실빌드는 마나 96.8% 예약 오라 스택). 단일 평가는 그 시점
              // 이득만 보고 오라 기회비용(이미 확정된 단계)을 못 본다.
              boolean crossesKeystone =
                  path.stream()
                      .anyMatch(
                          id -> {
                            PoeTreeGraphService.TreeNode pathNode = poeTreeGraphService.node(id);
                            return pathNode != null && "keystone".equals(pathNode.type());
                          });
              if (crossesKeystone) {
                continue;
              }
              masteryPaths.put(mn.id(), path);
              newMasteries.add(mn.id());
              scored.add(new Candidate(mn.id(), 2000 - path.size() * 10)); // 경로 짧을수록 우선
            }
          }
          // 점수 높은 것부터, 예산(경로 비용 반영) 안에서 상한만큼
          scored.sort(java.util.Comparator.comparingInt(Candidate::score).reversed());
          newMasteries.clear();
          int planned = 0;
          for (Candidate candidate : scored) {
            int cost =
                masteryPaths.containsKey(candidate.id())
                    ? masteryPaths.get(candidate.id()).size()
                    : 1;
            if (newMasteries.size() >= MASTERY_MAX_NEW || points + planned + cost > POINT_BUDGET) {
              continue; // 비싼 경로는 건너뛰고 더 싼 후보는 계속 본다
            }
            newMasteries.add(candidate.id());
            planned += cost;
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
            // 신규 후보는 **할당 경로(1~5포인트)까지 포함**해 평가한다 — 이미 찍은 노드는 효과만 본다
            boolean isNew = newMasteries.contains(nodeId);
            List<Integer> allocPath =
                isNew ? masteryPaths.getOrDefault(nodeId, List.of(nodeId)) : List.of();
            if (isNew && points + allocPath.size() > POINT_BUDGET) {
              continue;
            }
            Set<Integer> trialNodes = allocated;
            if (isNew) {
              trialNodes = new LinkedHashSet<>(allocated);
              trialNodes.addAll(allocPath);
            }
            // XML 조립은 공유 필드(fixedMasteries)를 쓰므로 **메인 스레드에서 미리** 만들어 둔다
            // (evalBatch 는 워커 스레드에서 xmlFor 를 호출한다 — 거기서 필드를 바꾸면 경쟁 상태)
            Map<Integer, String> xmlByEffect = new LinkedHashMap<>();
            Map<Integer, Integer> saved = fixedMasteries;
            // 인게임 규칙: 같은 마스터리 효과는 **한 번만** 할당 가능(효과 id 는 그룹 내 공유라 다른 화염 숙련 노드에도
            // 같은 id 로 나온다). 다른 노드가 이미 고른 효과는 후보에서 제외해 "동일 숙련 2회" 불법 조합을 막는다.
            Set<Integer> usedEffects = new java.util.HashSet<>(saved.values());
            usedEffects.remove(saved.get(nodeId)); // 이 노드가 이미 가진 효과는 스스로 제외 대상 아님
            for (PoeTreeGraphService.MasteryEffect effect : node.masteryEffects()) {
              if (usedEffects.contains(effect.id())) {
                continue; // 다른 마스터리 노드가 이미 쓴 효과 — 중복 금지
              }
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
            // ── P1 메타 마스터리 웜스타트 — 실 유저 다수(채택률 40%+)가 고른 효과를 신뢰한다.
            //   단일 지표 greedy 는 패키지 시너지(미초과 화염저항→재생 등)를 저평가하므로,
            //   메타 효과가 최고 평가 대비 1% 이내면 메타를 채택. 신규 할당 문턱도 메타는 +0.05%로 완화.
            Set<Integer> metaIds =
                node.masteryEffects().stream()
                    .filter(this::isMetaMasteryEffect)
                    .map(PoeTreeGraphService.MasteryEffect::id)
                    .collect(java.util.stream.Collectors.toSet());
            Map.Entry<Integer, Double> bestMeta =
                results.entrySet().stream()
                    .filter(en -> metaIds.contains(en.getKey()))
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            Map.Entry<Integer, Double> pick = best;
            if (bestMeta != null && best != null && bestMeta.getValue() >= best.getValue() * 0.99) {
              pick = bestMeta;
            }
            boolean pickIsMeta = pick != null && metaIds.contains(pick.getKey());
            // 이미 찍은 노드의 효과는 공짜라 조금이라도 나으면 채택. 신규 할당은 포인트당 문턱(+0.3%, 메타 +0.05%).
            int cost = Math.max(1, allocPath.size());
            double threshold =
                isNew ? current * (1 + (pickIsMeta ? 0.0005 : 0.003) * cost) : current;
            // 메타 픽이 문턱 미달인데 원래 최고는 통과하는 희귀 케이스 — 원래 최고로 폴백
            if (pick != null
                && pick != best
                && !(pick.getValue() > 0 && pick.getValue() > threshold)
                && best.getValue() > 0
                && best.getValue() > (isNew ? current * (1 + 0.003 * cost) : current)) {
              pick = best;
              pickIsMeta = metaIds.contains(pick.getKey());
              threshold = isNew ? current * (1 + 0.003 * cost) : current;
            }
            if (pick != null && pick.getValue() > 0 && pick.getValue() > threshold) {
              final int pickedEffect = pick.getKey();
              Map<Integer, Integer> merged = new LinkedHashMap<>(fixedMasteries);
              merged.put(nodeId, pickedEffect);
              fixedMasteries = merged;
              if (isNew) {
                allocated.addAll(allocPath);
                points += allocPath.size();
              }
              current = pick.getValue();
              PoeTreeGraphService.MasteryEffect chosen =
                  node.masteryEffects().stream()
                      .filter(e -> e.id() == pickedEffect)
                      .findFirst()
                      .orElse(null);
              String effectText =
                  chosen == null
                      ? String.valueOf(pickedEffect)
                      : (chosen.statsKo() != null && !chosen.statsKo().isEmpty()
                          ? chosen.statsKo().get(0)
                          : chosen.stats().isEmpty() ? "" : chosen.stats().get(0));
              log(
                  (isNew ? "마스터리 신규(+" + allocPath.size() + "pt): " : "마스터리: ")
                      + (node.nameKo() != null ? node.nameKo() : node.name())
                      + " → "
                      + effectText
                      + (pickIsMeta ? " (메타)" : "")
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
        this.tattooAllocated = Set.copyOf(allocated); // tattooFits 할당-이웃 판정 기준
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
        for (Map.Entry<Integer, Equipped> socketed : jewels.entrySet()) {
          // 반경 변환(문신·무궁 주얼)은 유니크 주얼에만 있다 — 제작 레어 주얼은 반경 효과가 없어 건너뛴다.
          if (!socketed.getValue().isUnique()) {
            continue;
          }
          PoeUniqueItem socketedJewel = socketed.getValue().unique();
          double radius = jewelRadiusValue(socketedJewel.radius());
          if (radius <= 0 || !allocated.contains(socketed.getKey())) {
            continue;
          }
          String label =
              socketedJewel.nameKo() != null ? socketedJewel.nameKo() : socketedJewel.name();
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

      // ── 에센스 스왑 실측 패스 — 채택된 **레어 반지**의 같은 젠 어픽스 하나를 특수 에센스 전용
      // 라인으로 바꿔 실측, 이득이면 채택. craftRare 의 후보 주입은 유니크가 슬롯을 이기면 표면화되지
      // 않으므로(잠복), 최종 장비 기준으로 한 번 더 기회를 준다. eval 상한 = 레어 반지 × 에센스 3 × 어픽스 ≤ 수십.
      {
        // 슬롯별 화이트리스트 — **글로벌·무조건부** 라인만. 반지 3종은 DPS, 목걸이/갑옷 Delirium 은
        // 방어(주문 막기 7% / 카오스 DoT 25% 감소)라 EHP 목표에서 표면화된다. 소켓 젬 한정·조건부
        // (정지 시/피격 시) 라인은 엔진 조건 게이트가 애매해 제외.
        record SwapPool(String itemClass, Set<String> families) {}
        Map<String, SwapPool> swapPoolBySlotPrefix =
            Map.of(
                "Ring", new SwapPool("Ring", Set.of("Delirium", "Hysteria", "Horror")),
                "Amulet", new SwapPool("Amulet", Set.of("Delirium")),
                "Body Armour", new SwapPool("Body Armour", Set.of("Delirium")));
        record SwapTrial(Slot slot, int famIdx, PoeEssenceDataService.EssenceEntry entry) {}
        List<SwapTrial> swapTrials = new ArrayList<>();
        for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
          if (entry.getValue().isUnique() || entry.getValue().rare() == null) {
            continue;
          }
          SwapPool swapPool =
              swapPoolBySlotPrefix.entrySet().stream()
                  .filter(e -> entry.getKey().pobName.startsWith(e.getKey()))
                  .map(Map.Entry::getValue)
                  .findFirst()
                  .orElse(null);
          if (swapPool == null) {
            continue;
          }
          List<PoeEssenceDataService.EssenceEntry> slotEssences =
              poeEssenceDataService.forItemClass(swapPool.itemClass());
          if (slotEssences == null) {
            continue;
          }
          List<PoeModPoolDataService.ModFamily> families = entry.getValue().rare().families();
          for (PoeEssenceDataService.EssenceEntry essence : slotEssences) {
            if (!swapPool.families().contains(essence.family())) {
              continue;
            }
            for (int i = 0; i < families.size(); i++) {
              // 같은 젠 어픽스만 교체(접두/접미 각 3개 합법성 유지), 이미 에센스 라인이면 제외
              if (families.get(i).gen().equals(essence.gen())
                  && !families.get(i).key().startsWith("essence")) {
                swapTrials.add(new SwapTrial(entry.getKey(), i, essence));
              }
            }
          }
        }
        if (!swapTrials.isEmpty()) {
          Map<SwapTrial, Double> swapResults =
              evalBatch(
                  executor,
                  swapTrials,
                  trial -> {
                    RareItem rare = items.get(trial.slot()).rare();
                    List<PoeModPoolDataService.ModFamily> swapped =
                        new ArrayList<>(rare.families());
                    swapped.set(
                        trial.famIdx(),
                        new PoeModPoolDataService.ModFamily(
                            "essence" + trial.entry().family(),
                            trial.entry().gen(),
                            List.of("ring"),
                            List.of(),
                            null,
                            List.of(
                                new PoeModPoolDataService.ModTier(
                                    1, trial.entry().en(), trial.entry().ko()))));
                    Map<Slot, Equipped> trialItems = new EnumMap<>(items);
                    trialItems.put(
                        trial.slot(),
                        Equipped.ofRare(
                            new RareItem(
                                rare.baseType(),
                                swapped,
                                rare.tierFraction(),
                                rare.perFractions(),
                                rare.implicitLines(),
                                rare.implicitLinesKo())));
                    return buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        trialItems,
                        jewels);
                  },
                  objectiveKey);
          Map.Entry<SwapTrial, Double> bestSwap =
              swapResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (bestSwap != null && bestSwap.getValue() > current * 1.003) {
            SwapTrial trial = bestSwap.getKey();
            RareItem rare = items.get(trial.slot()).rare();
            List<PoeModPoolDataService.ModFamily> swapped = new ArrayList<>(rare.families());
            String droppedKey = swapped.get(trial.famIdx()).key();
            swapped.set(
                trial.famIdx(),
                new PoeModPoolDataService.ModFamily(
                    "essence" + trial.entry().family(),
                    trial.entry().gen(),
                    List.of("ring"),
                    List.of(),
                    null,
                    List.of(
                        new PoeModPoolDataService.ModTier(
                            1, trial.entry().en(), trial.entry().ko()))));
            items.put(
                trial.slot(),
                Equipped.ofRare(
                    new RareItem(
                        rare.baseType(),
                        swapped,
                        rare.tierFraction(),
                        rare.perFractions(),
                        rare.implicitLines(),
                        rare.implicitLinesKo())));
            current = bestSwap.getValue();
            log(
                "에센스 스왑: "
                    + trial.slot().pobName
                    + " "
                    + droppedKey
                    + " → "
                    + trial.entry().nameKo()
                    + " ("
                    + String.join(", ", trial.entry().ko())
                    + ") → "
                    + format(current));
          }
        }
      }

      // ── 소켓 시너지 스왑 실측 패스 — 실빌드 표준 엘더 헬멧("장착된 젬에 20레벨 화상 피해 보조")의 진짜
      // 이득은 링크 1개 해방이다: 이미 소켓에 Burning Damage 가 있으면 buildXml 의 중복 가드가 헬멧 내장
      // 보조를 무효화해 슬롯 대결에서 표면화되지 않는다(잠복). 최종 장비 기준으로 「헬멧=엘더 레어 + 소켓의
      // Burning Damage → 다른 보조」 조합을 후보 전수 실측해 이득이면 채택한다(에센스 스왑과 같은 패턴).
      if (!"ehp".equals(objective)
          && supports.stream()
              .anyMatch(s -> "Burning Damage".equals(s.name().replaceFirst(" Support$", "")))) {
        RareItem elderHelmet = craftRare(Slot.HELMET, gem, keywords, 0.0);
        boolean hasSocketSynergy =
            elderHelmet != null
                && elderHelmet.families().stream()
                    .anyMatch(f -> "elderBurningSupport".equals(f.key()));
        if (hasSocketSynergy) {
          List<PoeGem> freedCandidates =
              poeGemDataService.search(null, "support", "all", null).stream()
                  .filter(s -> !s.levels().isEmpty())
                  .filter(this::isProvidedSupport)
                  .filter(s -> supportCompatible(gem, s))
                  .filter(s -> supports.stream().noneMatch(cur -> cur.slug().equals(s.slug())))
                  .toList();
          List<PoeGem> socketed =
              supports.stream()
                  .filter(s -> !"Burning Damage".equals(s.name().replaceFirst(" Support$", "")))
                  .toList();
          Map<Slot, Equipped> synergyItems = new EnumMap<>(items);
          synergyItems.put(Slot.HELMET, Equipped.ofRare(elderHelmet));
          Map<PoeGem, Double> synergyResults =
              evalBatch(
                  executor,
                  freedCandidates,
                  candidate ->
                      buildXml(
                          gem,
                          joined(socketed, candidate),
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          synergyItems,
                          jewels),
                  objectiveKey);
          Map.Entry<PoeGem, Double> bestSynergy =
              synergyResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (bestSynergy != null && bestSynergy.getValue() > current * 1.003) {
            items.put(Slot.HELMET, Equipped.ofRare(elderHelmet));
            supports.removeIf(s -> "Burning Damage".equals(s.name().replaceFirst(" Support$", "")));
            supports.add(bestSynergy.getKey());
            current = bestSynergy.getValue();
            log(
                "소켓 시너지 스왑: 투구=엘더 레어(내장 화상 피해 보조 20레벨) + 소켓 Burning Damage → "
                    + bestSynergy.getKey().name()
                    + " → "
                    + format(current));
          } else {
            log(
                "소켓 시너지 스왑: 이득 없음(현 투구 유지) — 최고 "
                    + (bestSynergy != null
                        ? bestSynergy.getKey().name() + " " + format(bestSynergy.getValue())
                        : "-")
                    + " vs 현재 "
                    + format(current));
          }
        }
      }

      // ── 자동 클러스터 주얼 트라이얼(포트 4/5) — balanced 전용. 클러스터는 실빌드 표준 레이어(ninja 중앙값
      // 대2·중1·소1)인데 최적화기는 fixedClusters(에디터 인계)만 알았다. Large 1개를 스킬 키워드로 조립(스킬키 =
      // enchant 스탯 점수 최고, 노터블 = 합법 목록 중 점수 상위 3)해 최근접 Large 소켓에 꽂는 통째 트라이얼을
      // 실측한다. subgraphPlan 가상 노드 id 의 엔드투엔드 검증 겸용 — id 가 틀리면 노터블 기여 0 이라 미채택으로
      // 표면화된다. 예산 초과여도 1회 평가는 남긴다(레벨/예산 설계 근거 데이터).
      if ("balanced".equals(objectiveKey) && poeClusterJewelDataService.hasData()) {
        // 범용 "damage" 키워드는 모든 클러스터 스킬/노터블에 걸려(실사고: RF 에 bow damage 클러스터 조립)
        // 클러스터 스코어링에선 제거 — 원소/도트/방어 등 특이 키워드만 남긴다(parse-mods 와 동일 교훈).
        List<String> clusterKeywords = keywords.stream().filter(k -> !"damage".equals(k)).toList();
        boolean clusterAdopted = false;
        var largeDef = poeClusterJewelDataService.def("Large").orElse(null);
        var statsByName = new java.util.HashMap<String, List<String>>();
        for (var cn : poeTreeGraphService.clusterNotables()) {
          statsByName.put(cn.name(), cn.stats());
        }
        // 스킬키 선택은 **노터블 페이로드 합**(합법 노터블 점수 상위 3 합) 기준 — enchant 스탯만으로는
        // 동점이 흔하다(실사고: bow 의 "Damage Over Time with Bow Skills" 가 RF 의 damage over time 에
        // 걸려 fire 와 1:1 동점 → 순회 순서로 bow 채택). 클러스터의 본체는 노터블이다.
        record SkillCand(String key, long score) {}
        List<String> topSkillKeys =
            largeDef == null
                ? List.of()
                : largeDef.skills().entrySet().stream()
                    .map(
                        e ->
                            new SkillCand(
                                e.getKey(),
                                poeClusterJewelDataService
                                            .legalNotables(e.getKey(), "Large")
                                            .stream()
                                            .map(
                                                n ->
                                                    score(
                                                        statsByName.getOrDefault(n, List.of()),
                                                        clusterKeywords))
                                            .sorted(Comparator.reverseOrder())
                                            .limit(3)
                                            .mapToLong(Integer::longValue)
                                            .sum()
                                        * 10
                                    + score(e.getValue().stats(), clusterKeywords)))
                    .filter(sc -> sc.score() > 0)
                    .sorted(
                        Comparator.comparingLong(SkillCand::score)
                            .reversed()
                            .thenComparing(SkillCand::key))
                    // 페이로드 휴리스틱은 랭킹만 못 믿는다(실사고: 부분문자열 스코어가 RF 에서 fire 4 <
                    // spell 8 — 정크가 상위). 프리컷을 12로 넓혀 실측이 판정하게 한다(순차 ≤12 평가).
                    .limit(12)
                    .map(SkillCand::key)
                    .toList();
        // 소켓은 스킬키와 무관 — 최근접 미사용 Large 소켓 1개
        PoeTreeGraphService.TreeNode bestSocket = null;
        List<Integer> bestSocketPath = null;
        for (var socket : poeTreeGraphService.clusterSockets()) {
          if (allocated.contains(socket.id()) || socket.expansionJewel().size() != 2) {
            continue; // 이미 쓰는 소켓(고정 클러스터)·비 Large 소켓 제외
          }
          List<Integer> path = poeTreeGraphService.shortestPath(allocated, socket.id());
          if (path == null || path.isEmpty() || path.size() > 5) {
            continue;
          }
          if (bestSocketPath == null || path.size() < bestSocketPath.size()) {
            bestSocket = socket;
            bestSocketPath = path;
          }
        }
        // 상위 스킬키를 **각각 실측** — 페이로드 휴리스틱만으론 오선택(bow/spell 실사고). 후보별로
        // fixedClusters 를 바꿔야 해서 병렬 불가(공유 필드) — 순차 ≤3 평가.
        record ScoredNotable(String name, int score) {}
        String bestKey = null;
        List<String> bestNotables = null;
        ClusterSpec bestSpec = null;
        Set<Integer> bestTrialNodes = null;
        int bestCost = 0;
        double bestTrialVal = 0;
        if (bestSocket != null) {
          List<ClusterSpec> savedClusters = fixedClusters;
          for (String skillKey : topSkillKeys) {
            List<String> notables =
                poeClusterJewelDataService.legalNotables(skillKey, "Large").stream()
                    .map(
                        n ->
                            new ScoredNotable(
                                n, score(statsByName.getOrDefault(n, List.of()), clusterKeywords)))
                    .filter(sn -> sn.score() > 0)
                    .sorted(
                        Comparator.comparingInt(ScoredNotable::score)
                            .reversed()
                            .thenComparing(ScoredNotable::name))
                    .limit(3)
                    .map(ScoredNotable::name)
                    .toList();
            if (notables.size() < 2) {
              continue;
            }
            var plan =
                poeClusterJewelDataService.subgraphPlan(
                    bestSocket.expansionJewel(), "Large", 8, notables, 0);
            if (plan.isEmpty()) {
              continue;
            }
            ClusterSpec spec = new ClusterSpec(bestSocket.id(), "Large", 8, skillKey, notables, 0);
            Set<Integer> trialNodes = new LinkedHashSet<>(allocated);
            trialNodes.addAll(bestSocketPath);
            trialNodes.add(bestSocket.id());
            trialNodes.addAll(plan.get().nodeIds());
            int cost =
                bestSocketPath.size()
                    + (bestSocketPath.contains(bestSocket.id()) ? 0 : 1)
                    + plan.get().pointCost();
            List<ClusterSpec> withNew = new ArrayList<>(savedClusters);
            withNew.add(spec);
            double trialVal;
            try {
              fixedClusters = List.copyOf(withNew);
              trialVal =
                  objectiveOf(
                      poePobEngineService.calculateValues(
                          buildXml(
                              gem,
                              supports,
                              className,
                              ascendancy,
                              ascendancyNodes,
                              trialNodes,
                              items,
                              jewels)),
                      objectiveKey);
              evalCount.incrementAndGet();
            } finally {
              fixedClusters = savedClusters;
            }
            log(
                "자동 클러스터 후보: Large "
                    + skillKey
                    + " ["
                    + String.join(", ", notables)
                    + "] → "
                    + format(trialVal));
            if (trialVal > bestTrialVal) {
              bestTrialVal = trialVal;
              bestKey = skillKey;
              bestNotables = notables;
              bestSpec = spec;
              bestTrialNodes = trialNodes;
              bestCost = cost;
            }
          }
          // ── 적재물 재선정 — 승자 스킬키의 노터블 3개는 **키워드 점수**로 골랐다. 그 점수는 이미
          //    오판 전력이 있다(감시자의 눈: "받는 피해" 모드가 1순위로 올라옴). 승자에 한해 후보를
          //    넓혀(상위 6) 하나씩 실측으로 교체해 본다 — 클러스터가 손해로 기각되던 이유가
          //    "클러스터 자체"가 아니라 "적재물 선택"일 수 있기 때문이다.
          if (bestKey != null && bestSocket != null) {
            List<String> pool =
                poeClusterJewelDataService.legalNotables(bestKey, "Large").stream()
                    .map(
                        n ->
                            new ScoredNotable(
                                n, score(statsByName.getOrDefault(n, List.of()), clusterKeywords)))
                    .sorted(
                        Comparator.comparingInt(ScoredNotable::score)
                            .reversed()
                            .thenComparing(ScoredNotable::name))
                    .limit(CLUSTER_NOTABLE_POOL)
                    .map(ScoredNotable::name)
                    .toList();
            List<ClusterSpec> savedForSwap = fixedClusters;
            try {
              for (String candidate : pool) {
                if (bestNotables.contains(candidate)) {
                  continue;
                }
                for (int slot = 0; slot < bestNotables.size(); slot++) {
                  List<String> trialNotables = new ArrayList<>(bestNotables);
                  trialNotables.set(slot, candidate);
                  var trialPlan =
                      poeClusterJewelDataService.subgraphPlan(
                          bestSocket.expansionJewel(), "Large", 8, trialNotables, 0);
                  if (trialPlan.isEmpty()) {
                    continue;
                  }
                  ClusterSpec trialSpec =
                      new ClusterSpec(bestSocket.id(), "Large", 8, bestKey, trialNotables, 0);
                  Set<Integer> nodes = new LinkedHashSet<>(allocated);
                  nodes.addAll(bestSocketPath);
                  nodes.add(bestSocket.id());
                  nodes.addAll(trialPlan.get().nodeIds());
                  List<ClusterSpec> withTrial = new ArrayList<>(savedForSwap);
                  withTrial.add(trialSpec);
                  double val;
                  fixedClusters = List.copyOf(withTrial);
                  val =
                      objectiveOf(
                          poePobEngineService.calculateValues(
                              buildXml(
                                  gem,
                                  supports,
                                  className,
                                  ascendancy,
                                  ascendancyNodes,
                                  nodes,
                                  items,
                                  jewels)),
                          objectiveKey);
                  evalCount.incrementAndGet();
                  if (val > bestTrialVal * 1.003) {
                    bestTrialVal = val;
                    bestNotables = trialNotables;
                    bestSpec = trialSpec;
                    bestTrialNodes = nodes;
                    bestCost =
                        bestSocketPath.size()
                            + (bestSocketPath.contains(bestSocket.id()) ? 0 : 1)
                            + trialPlan.get().pointCost();
                    log("클러스터 적재물 교체: " + candidate + " → " + format(bestTrialVal));
                  }
                }
              }
            } finally {
              fixedClusters = savedForSwap;
            }
          }
        }
        if (bestSpec != null) {
          boolean withinBudget = points + bestCost <= POINT_BUDGET;
          if (withinBudget && bestTrialVal > current * 1.003) {
            clusterAdopted = true;
            List<ClusterSpec> withNew = new ArrayList<>(fixedClusters);
            withNew.add(bestSpec);
            fixedClusters = List.copyOf(withNew);
            allocated.clear();
            allocated.addAll(bestTrialNodes);
            points += bestCost;
            current = bestTrialVal;
            log(
                "자동 클러스터 채택: Large "
                    + bestKey
                    + " ["
                    + String.join(", ", bestNotables)
                    + "] 소켓 "
                    + bestSpec.socket()
                    + " (+"
                    + bestCost
                    + "pt) → "
                    + format(current));
          } else if (!withinBudget && bestTrialVal > current * 1.003) {
            // ── 예산 스왑 — 최종 컨텍스트에서 「최저가치 잎 사슬 제거 ↔ 클러스터 편입」 원자 교환.
            // 예약 방식(트리를 미리 약화)은 실패 롤백됐다(18단계: 712,940→407,389) — 여기서는 완성 빌드
            // 기준으로 제거·편입을 한 번에 실측해 상승할 때만 채택한다. 잎(유도 그래프 차수 ≤1)만 반복
            // 제거하므로 잔여 트리 연결성이 보존된다.
            int deficit = points + bestCost - POINT_BUDGET;
            // 속성 슬랙 — 잎의 다수는 +10 속성 여행 노드라, 무제한 제거하면 젬 요구치가 깨져 빌드가
            // 붕괴한다(실측: 11잎 제거 → 130,896). 현 빌드의 Str/Dex/Int 여유분 안에서만 속성 잎을 뗀다.
            Map<String, Double> curVals =
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
            double slackStr = curVals.getOrDefault("Str", 0d) - curVals.getOrDefault("ReqStr", 0d);
            double slackDex = curVals.getOrDefault("Dex", 0d) - curVals.getOrDefault("ReqDex", 0d);
            double slackInt = curVals.getOrDefault("Int", 0d) - curVals.getOrDefault("ReqInt", 0d);
            java.util.function.BiFunction<List<String>, String, Integer> attrOf =
                (lines, attr) -> {
                  int sum = 0;
                  for (String line : lines == null ? List.<String>of() : lines) {
                    var m =
                        java.util.regex.Pattern.compile(
                                "\\+(\\d+) to " + attr + "(?!\\w)|\\+(\\d+) to all Attributes")
                            .matcher(line);
                    while (m.find()) {
                      sum += Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
                    }
                  }
                  return sum;
                };
            Set<Integer> removable = new LinkedHashSet<>();
            // ⚠ 차수는 **교환 후 평가할 트리**(기존 + 소켓 경로 + 소켓) 기준으로 세야 한다.
            //    예전엔 클러스터를 얹기 전 트리(allocated)로 셌는데, 주얼 소켓까지 가는 경로의 앵커는
            //    보통 기존 트리의 잎이라 "가장 값싼 잎"으로 뽑혀 떨어져 나갔다 → 소켓·클러스터가 통째로
            //    끊겨 빌드가 붕괴(실측: 교환 후 640 vs 현재 587,450)하고 클러스터는 영원히 기각됐다.
            Set<Integer> pool = new HashSet<>(bestTrialNodes);
            // ⚠ 직업 시작 노드도 넣어야 한다. 시작 노드는 allocated 에 없어서, 여기에 안 넣으면 **시작에 붙은
            //    첫 노드가 차수 1(=잎)로 보여** 제거 대상이 된다. 그걸 떼는 순간 트리 전체가 시작점과 끊겨
            //    PoB 가 전부 무시한다(실측: 교환 후 953, 끊김 115/123 — 대조 현 트리는 끊김 0).
            Integer classStartNode = poeTreeGraphService.classStart(className);
            if (classStartNode != null) {
              pool.add(classStartNode);
            }
            while (removable.size() < deficit) {
              Integer pick = null;
              int pickScore = Integer.MAX_VALUE;
              for (int id : pool) {
                if (id >= 0x10000) {
                  continue; // 가상 노드는 대상 아님
                }
                if (!allocated.contains(id)) {
                  continue; // 이번에 새로 얹은 소켓 경로/소켓(과 시작 노드) — 떼면 클러스터·트리가 끊긴다
                }
                int degree = 0;
                for (int nb : poeTreeGraphService.neighbors(id)) {
                  if (pool.contains(nb)) {
                    degree++;
                  }
                }
                if (degree > 1) {
                  continue; // 잎만 — 중간 노드 제거는 연결성을 깬다
                }
                PoeTreeGraphService.TreeNode treeNode = poeTreeGraphService.node(id);
                if (treeNode != null
                    && ("jewel".equals(treeNode.type())
                        || "keystone".equals(treeNode.type())
                        || "mastery".equals(treeNode.type()))) {
                  // 차수 1·score 낮음이라 잎 후보가 되기 쉽지만 떼는 순간 빌드 기제가 통째로 무너지는
                  // 노드들 — 주얼 소켓(유니크 주얼 상실, 실측 130k), 키스톤(혈마법 상실 → 오라 전멸,
                  // 실측 3,247), 마스터리(선택 효과 상실). 전부 제거 대상에서 제외한다. 남는 잎이
                  // 부족하면 "스왑 불가"로 정직하게 물러선다 — 이 예산에서 클러스터를 담는 정공법은
                  // LEVEL 파리티(실빌드 L100 = +10pt)다.
                  continue;
                }
                if (treeNode != null) {
                  // 속성 슬랙 가드 — 이 잎을 떼면 요구치가 깨지는 속성이 있으면 후보 제외
                  int lossStr = attrOf.apply(treeNode.stats(), "Strength");
                  int lossDex = attrOf.apply(treeNode.stats(), "Dexterity");
                  int lossInt = attrOf.apply(treeNode.stats(), "Intelligence");
                  if ((lossStr > 0 && lossStr > slackStr)
                      || (lossDex > 0 && lossDex > slackDex)
                      || (lossInt > 0 && lossInt > slackInt)) {
                    continue;
                  }
                }
                int s = treeNode == null ? 0 : score(treeNode.stats(), keywords);
                if (s < pickScore || (s == pickScore && (pick == null || id < pick))) {
                  pickScore = s;
                  pick = id;
                }
              }
              if (pick == null) {
                break; // 더 뗄 잎이 없음 — 스왑 불가
              }
              PoeTreeGraphService.TreeNode picked = poeTreeGraphService.node(pick);
              if (picked != null) {
                slackStr -= attrOf.apply(picked.stats(), "Strength");
                slackDex -= attrOf.apply(picked.stats(), "Dexterity");
                slackInt -= attrOf.apply(picked.stats(), "Intelligence");
              }
              removable.add(pick);
              pool.remove(pick);
            }
            if (removable.size() >= deficit) {
              Set<Integer> swapNodes = new LinkedHashSet<>(bestTrialNodes);
              swapNodes.removeAll(removable);
              // 진단 — 교환 후 트리가 실제로 이어져 있는지(끊긴 노드는 PoB 가 무시해 빌드가 통째로 무너진다).
              // 이 수치 없이 "교환 후 953" 만 보고는 손익인지 붕괴인지 구분할 수 없었다.
              Integer startNode = poeTreeGraphService.classStart(className);
              if (startNode != null) {
                Set<Integer> withStart = new LinkedHashSet<>(swapNodes);
                withStart.add(startNode);
                Set<Integer> reach = poeTreeGraphService.reachableFrom(startNode, withStart);
                long orphan =
                    swapNodes.stream().filter(id -> id < 0x10000 && !reach.contains(id)).count();
                // 대조군 — 지금 쓰고 있는(정상) 트리에도 같은 잣대를 대 본다. 여기서도 끊김이 크게 나오면
                // 트리가 아니라 진단(시작 노드/그래프 기준)이 틀린 것이다.
                Set<Integer> curWithStart = new LinkedHashSet<>(allocated);
                curWithStart.add(startNode);
                Set<Integer> curReach = poeTreeGraphService.reachableFrom(startNode, curWithStart);
                long curOrphan =
                    allocated.stream().filter(id -> id < 0x10000 && !curReach.contains(id)).count();
                log(
                    "클러스터 스왑 진단: 노드 "
                        + swapNodes.size()
                        + " · 끊김 "
                        + orphan
                        + " · 제거 "
                        + removable.size()
                        + " || 대조(현 트리): 노드 "
                        + allocated.size()
                        + " · 끊김 "
                        + curOrphan
                        + " · start "
                        + startNode);
              }
              List<ClusterSpec> withNew = new ArrayList<>(fixedClusters);
              withNew.add(bestSpec);
              List<ClusterSpec> savedClusters2 = fixedClusters;
              double swapVal;
              try {
                fixedClusters = List.copyOf(withNew);
                swapVal =
                    objectiveOf(
                        poePobEngineService.calculateValues(
                            buildXml(
                                gem,
                                supports,
                                className,
                                ascendancy,
                                ascendancyNodes,
                                swapNodes,
                                items,
                                jewels)),
                        objectiveKey);
                evalCount.incrementAndGet();
              } finally {
                fixedClusters = savedClusters2;
              }
              if (swapVal > current * 1.003) {
                clusterAdopted = true;
                fixedClusters = List.copyOf(withNew);
                allocated.clear();
                allocated.addAll(swapNodes);
                points = points - removable.size() + bestCost;
                current = swapVal;
                log(
                    "자동 클러스터 예산 스왑 채택: Large "
                        + bestKey
                        + " ["
                        + String.join(", ", bestNotables)
                        + "] ↔ 잎 "
                        + removable.size()
                        + "pt 제거 → "
                        + format(current));
              } else {
                log(
                    "자동 클러스터 예산 스왑 기각: 교환 후 "
                        + format(swapVal)
                        + " vs 현재 "
                        + format(current)
                        + " (제거 잎 "
                        + removable.size()
                        + "pt)");
              }
            } else {
              log("자동 클러스터: 스왑 불가(뗄 수 있는 잎 부족) — 미채택");
            }
          } else {
            log(
                "자동 클러스터: 미채택 — 최고 Large "
                    + bestKey
                    + " ["
                    + String.join(", ", bestNotables)
                    + "] 가정값 "
                    + format(bestTrialVal)
                    + " vs 현재 "
                    + format(current)
                    + (withinBudget
                        ? ""
                        : " (예산 부족: " + points + "+" + bestCost + ">" + POINT_BUDGET + ")"));
          }
        }
        // 예약 환급 greedy — 클러스터가 미채택이면 CLUSTER_RESERVE 로 비워 둔 예산을 일반 노터블로
        // 소진한다. 예약을 놀리면 순손실(11단계 교훈: 예산 기회비용)이라 필수. 키스톤 통과 경로 제외.
        if (!clusterAdopted) {
          boolean refundImproved = true;
          while (refundImproved && points < POINT_BUDGET - JEWEL_RESERVE) {
            refundImproved = false;
            record RefundCand(PoeTreeGraphService.TreeNode node, List<Integer> path, int score) {}
            List<RefundCand> refundCands = new ArrayList<>();
            for (var cand : poeTreeGraphService.searchCandidates()) {
              if (allocated.contains(cand.id())) {
                continue;
              }
              int s = score(cand.stats(), keywords);
              if (s <= 0) {
                continue;
              }
              List<Integer> path = poeTreeGraphService.shortestPath(allocated, cand.id());
              if (path == null
                  || path.isEmpty()
                  || points + path.size() > POINT_BUDGET - JEWEL_RESERVE) {
                continue;
              }
              boolean crossesKeystone =
                  path.stream()
                      .anyMatch(
                          id -> {
                            PoeTreeGraphService.TreeNode pn = poeTreeGraphService.node(id);
                            return pn != null && "keystone".equals(pn.type());
                          });
              if (crossesKeystone) {
                continue;
              }
              refundCands.add(new RefundCand(cand, path, s));
            }
            List<RefundCand> topRefunds =
                refundCands.stream()
                    .sorted(
                        Comparator.comparingDouble(
                                (RefundCand rc) -> rc.score() / (double) rc.path().size())
                            .reversed())
                    .limit(TREE_ROUND_CANDIDATES)
                    .toList();
            if (topRefunds.isEmpty()) {
              break;
            }
            Map<RefundCand, Double> refundResults =
                evalBatch(
                    executor,
                    topRefunds,
                    rc -> {
                      Set<Integer> trial = new LinkedHashSet<>(allocated);
                      trial.addAll(rc.path());
                      return buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          trial,
                          items,
                          jewels);
                    },
                    objectiveKey);
            RefundCand bestRefund = null;
            double bestGainPerPoint = 0;
            for (Map.Entry<RefundCand, Double> entry : refundResults.entrySet()) {
              double gainPerPoint = (entry.getValue() - current) / entry.getKey().path().size();
              if (gainPerPoint > bestGainPerPoint) {
                bestGainPerPoint = gainPerPoint;
                bestRefund = entry.getKey();
              }
            }
            if (bestRefund == null) {
              break;
            }
            allocated.addAll(bestRefund.path());
            points += bestRefund.path().size();
            current = refundResults.get(bestRefund);
            refundImproved = true;
            log(
                "클러스터 예약 환급: "
                    + bestRefund.node().name()
                    + " (+"
                    + bestRefund.path().size()
                    + "pt) → "
                    + format(current));
          }
        }
      }

      // ── 방어 슬롯 최종 재대결(balanced) — 아이템 스테이지는 오라/주얼 이전의 중간 컨텍스트라 방어
      // 요소가 저평가된 채 DPS 유니크가 이긴다(실사고: RF 에 Abyssus — 받는 물리 +45% 부채, 방어도 갭
      // 5.9k/18.6k 의 원인). 완성 빌드 기준으로 각 방어 슬롯 유니크를 방어 크래프트 레어와 1:1 재대결,
      // 최고 1건만 채택(에센스/소켓시너지 패스와 동형 — 상승 시만, 강제 유니크 존중).
      if (balancedJob) {
        // 여기서부터 완성 빌드끼리의 재대결 — 볼록 생존 벌점을 켠다(탐색 경로는 위에서 이미 확정).
        this.convexSurvivalPhase = true;
        record DefRematch(Slot slot, RareItem rare) {}
        // 개선 소진까지 반복 — 한 슬롯 교체가 문맥을 바꿔 다음 슬롯의 재대결 결과도 달라진다
        // (실측: 1건 채택만으로 +19.4%). 라운드 상한 = 방어 슬롯 수.
        for (int rematchRound = 0; rematchRound < 7; rematchRound++) {
          List<DefRematch> rematchTrials = new ArrayList<>();
          for (Slot defSlot :
              new Slot[] {
                Slot.BODY,
                Slot.HELMET,
                Slot.GLOVES,
                Slot.BOOTS,
                // 보조장비도 대상 — 활 빌드의 화살통은 "화살 추가" 같은 결정적 모드를 크래프트로만 얻는데,
                // 아이템 단계에선 유니크끼리만 겨뤄(값이 아직 29 수준) 크래프트가 최종 문맥에서 한 번도
                // 비교되지 않았다.
                Slot.OFFHAND,
                // 장신구도 같은 저평가 대상 — 크래프트는 슬롯 문맥(craftRare 카테고리)대로
                Slot.AMULET,
                Slot.RING1,
                Slot.RING2,
                Slot.BELT
              }) {
            Equipped cur = items.get(defSlot);
            if (cur == null || !cur.isUnique()) {
              continue;
            }
            final Equipped curFinal = cur;
            if (fixedUniques.stream().anyMatch(u -> u.slug().equals(curFinal.unique().slug()))) {
              continue; // 사용자 강제 유니크 존중
            }
            // ⚠ 4-인자 craftRare 는 defensive=forceEsBase(생명 빌드에선 false)라 **데미지 레어**를 만든다.
            //   그래서 "방어 슬롯 재대결" 이 실제로는 데미지 레어와 유니크를 붙이고 있었다 — 실측 교환곡선에서
            //   후보 최고 생존 이득이 +4.1%(투구)에 그친 이유(필요치 +269%). 뒤쪽 단계(6069행)는 이미 두 방향을
            //   모두 트라이얼하는데 이 단계만 빠져 있었다. 데미지/방어 두 레어를 함께 올린다(구성 같으면 1회).
            List<RareItem> slotTrials = new ArrayList<>();
            RareItem dmgRare0 = craftRare(defSlot, gem, keywords, 0.0);
            if (dmgRare0 != null) {
              slotTrials.add(dmgRare0);
            }
            RareItem defRare0 =
                DEF_RARE_ENABLED ? craftDefensiveRare(defSlot, gem, keywords) : null;
            if (defRare0 != null
                && (dmgRare0 == null || !defRare0.families().equals(dmgRare0.families()))) {
              slotTrials.add(defRare0);
            }
            for (RareItem rare : slotTrials) {
              if (rematchRound == 0) {
                // 어떤 모드로 만들어졌는지 — 스킬 고유 축(화살 추가/덫 재사용 등)이 실제로 들어갔는지 확인용.
                // 슬롯을 유니크가 이기더라도 "축이 후보에 있었는지"와 "있었는데 졌는지"는 다른 결론이다.
                log(
                    "크래프트 후보 "
                        + defSlot.ko
                        + ": "
                        + rare.baseType()
                        + " ["
                        + rare.families().stream()
                            .map(PoeModPoolDataService.ModFamily::key)
                            .toList()
                        + "]");
              }
              rematchTrials.add(new DefRematch(defSlot, rare));
            }
          }
          if (rematchTrials.isEmpty()) {
            break;
          }
          // 교환 곡선 측정 — 후보별 (DPS, 최약 최대피격) 을 그대로 남긴다. validator 훅은 이미 계산된 스탯을
          //   재사용하므로 엔진 호출이 늘지 않는다. "DPS 조금 내주고 생존 크게 사는 후보가 애초에 있는가"를
          //   점수함수 손대기 전에 눈으로 확인하기 위한 것(볼록 벌점 2판이 실패한 뒤 방향 전환).
          Map<DefRematch, double[]> tradeProbe = new java.util.concurrent.ConcurrentHashMap<>();
          Map<DefRematch, Double> rematchResults =
              evalBatch(
                  executor,
                  rematchTrials,
                  trial -> {
                    Map<Slot, Equipped> trialItems = new EnumMap<>(items);
                    trialItems.put(trial.slot(), Equipped.ofRare(trial.rare()));
                    return buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        trialItems,
                        jewels);
                  },
                  objectiveKey,
                  (trial, values) -> {
                    tradeProbe.put(
                        trial,
                        new double[] {
                          effectiveDps(values),
                          weakestCommonHit(values),
                          values.getOrDefault("TotalEHP", 0d)
                        });
                    return true; // 걸러내지 않는다 — 관측 전용
                  });
          if (rematchRound == 0 && !tradeProbe.isEmpty()) {
            Map<String, Double> curVals =
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
            double curDps = effectiveDps(curVals);
            double curWeak = weakestCommonHit(curVals);
            log(
                String.format(
                    "방어 교환곡선 기준: DPS %,.0f · 최약 %,.0f (목표 %,.0f)", curDps, curWeak, targetMaxHit));
            tradeProbe.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .limit(10)
                .forEach(
                    e ->
                        log(
                            String.format(
                                "  %s 방어레어: DPS %,.0f (%+.1f%%) · 최약 %,.0f (%+.1f%%) · EHP %,.0f",
                                e.getKey().slot().ko,
                                e.getValue()[0],
                                curDps > 0 ? (e.getValue()[0] / curDps - 1) * 100 : 0,
                                e.getValue()[1],
                                curWeak > 0 ? (e.getValue()[1] / curWeak - 1) * 100 : 0,
                                e.getValue()[2])));
          }
          Map.Entry<DefRematch, Double> bestRematch =
              rematchResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (bestRematch != null && bestRematch.getValue() > current * 1.003) {
            DefRematch trial = bestRematch.getKey();
            String droppedName = items.get(trial.slot()).unique().name();
            items.put(trial.slot(), Equipped.ofRare(trial.rare()));
            current = bestRematch.getValue();
            log(
                "방어 슬롯 재대결: "
                    + trial.slot().pobName
                    + " "
                    + droppedName
                    + " → 방어 레어("
                    + trial.rare().baseType()
                    + ") → "
                    + format(current));
          } else {
            log(
                "방어 슬롯 재대결: 유지 — 최고 "
                    + (bestRematch != null ? format(bestRematch.getValue()) : "-")
                    + " vs 현재 "
                    + format(current));
            break;
          }
        }
      }

      // ── 보조젬 최종 재대결 — 방어/무기/유니크 재대결과 같은 이유. 보조젬은 아이템 이전의 **랭킹 문맥**
      //   (기준 무기만 든 상태, 목표값 18~57 수준)에서 확정되는데, 완성 빌드는 수백만이라 순위가 뒤집힐 수 있다.
      //   실측(번개 화살): 라운드마다 채택젬과 신기루 궁수·규칙 뒤집기의 차이가 1~2점(동점 포함)이었고,
      //   대표 실빌드는 바로 그 둘을 쓴다. 슬롯별로 풀의 후보와 1:1 교체를 실측해 상승 시만 채택.
      // 목표 무관 — 랭킹 문맥에서 확정된다는 결함은 dps/ehp 목표에도 똑같이 있다(objectiveKey 로 평가하므로
      //   각 목표의 기준으로 재대결한다).
      if (!supportRematchPool.isEmpty() && !supports.isEmpty()) {
        enterPhase("supports-rematch");
        record SupSwap(int idx, PoeGem cand) {}
        for (int supRound = 0; supRound < 3; supRound++) {
          List<SupSwap> supTrials = new ArrayList<>();
          for (int i = 0; i < supports.size(); i++) {
            for (PoeGem cand : supportRematchPool) {
              if (!supports.contains(cand)) {
                supTrials.add(new SupSwap(i, cand));
              }
            }
          }
          if (supTrials.isEmpty()) {
            break;
          }
          Map<SupSwap, Double> supResults =
              evalBatch(
                  executor,
                  supTrials,
                  trial -> {
                    List<PoeGem> swapped = new ArrayList<>(supports);
                    swapped.set(trial.idx(), trial.cand());
                    return buildXml(
                        gem,
                        swapped,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items,
                        jewels);
                  },
                  objectiveKey);
          Map.Entry<SupSwap, Double> bestSup =
              supResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (bestSup != null && bestSup.getValue() > current * 1.003) {
            PoeGem dropped = supports.get(bestSup.getKey().idx());
            supports.set(bestSup.getKey().idx(), bestSup.getKey().cand());
            current = bestSup.getValue();
            log(
                "보조젬 최종 재대결: "
                    + koName(dropped)
                    + " → "
                    + koName(bestSup.getKey().cand())
                    + " → "
                    + format(current));
          } else {
            log(
                "보조젬 최종 재대결: 유지 — 최고 "
                    + (bestSup != null ? format(bestSup.getValue()) : "-")
                    + " vs 현재 "
                    + format(current)
                    + " (후보 "
                    + supTrials.size()
                    + "개)");
            break;
          }
        }
      }

      // ── 가드 스킬 선택 — 없음 포함 전수 실측 후 상승 시만 채택. 생존만 올리는 층이라 dps 목표에선 무의미,
      //   balanced/ehp 에서만 돈다. 실빌드가 거의 전부 끼는 층인데 우리 XML 엔 아예 없었다.
      if ((balancedJob || "ehp".equals(objectiveKey)) && GUARD_ENABLED) {
        enterPhase("guard");
        List<String> guardPool =
            List.of("Molten Shell", "Steelskin", "Immortal Call", "Vaal Molten Shell");
        // 보조 링크 후보 — null(단독) 포함. 각성한 강화는 실측에서 **전 조합 정확히 0**(품질은 흡수 한도에
        //   영향 없음)이라 제외했다. 기원은 젬 레벨을 올려 흡수 한도를 키운다: 강철 피부 15,844→16,710(+5.5%),
        //   용융 껍질 19,534→19,718, 발라 18,208→18,504. 단 승자였던 불사의 외침은 충전 기반이라 무변화(33,721).
        List<String> guardSupportPool = new ArrayList<>();
        guardSupportPool.add(null);
        guardSupportPool.add("Awakened Empower");
        String savedGuard = guardSkill;
        String savedGuardSup = guardSupport;
        Map<String, Double> guardResults = new LinkedHashMap<>();
        for (String candidate :
            guardPool.stream()
                .flatMap(g -> guardSupportPool.stream().map(sup -> sup == null ? g : g + "|" + sup))
                .toList()) {
          int bar = candidate.indexOf('|');
          guardSkill = bar < 0 ? candidate : candidate.substring(0, bar);
          guardSupport = bar < 0 ? null : candidate.substring(bar + 1);
          Map<String, Double> vals =
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
          guardResults.put(candidate, objectiveOf(vals, objectiveKey));
          log(
              String.format(
                  "가드 스킬 %s: 최약 %,.0f · EHP %,.0f · 목표값 %s",
                  candidate,
                  weakestCommonHit(vals),
                  vals.getOrDefault("TotalEHP", 0d),
                  format(guardResults.get(candidate))));
        }
        guardSkill = savedGuard;
        guardSupport = savedGuardSup;
        Map.Entry<String, Double> bestGuard =
            guardResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (bestGuard != null && bestGuard.getValue() > current * 1.003) {
          int bar = bestGuard.getKey().indexOf('|');
          guardSkill = bar < 0 ? bestGuard.getKey() : bestGuard.getKey().substring(0, bar);
          guardSupport = bar < 0 ? null : bestGuard.getKey().substring(bar + 1);
          current = bestGuard.getValue();
          log("가드 스킬 채택: " + bestGuard.getKey() + " → " + format(current));
        } else {
          log(
              "가드 스킬: 이득 없음 — 최고 "
                  + (bestGuard != null ? format(bestGuard.getValue()) : "-")
                  + " vs 현재 "
                  + format(current));
        }
      }

      // ── 무기 최종 재대결(balanced) — 무기는 아이템 스테이지 마지막(오라/마스터리/에센스/재대결 이전의
      // 중간 문맥)에 채택돼 완성 빌드 기준 최적이 아닐 수 있다(실측: 유니크 롤 정합 후 RF 876k→683k
      // 재수렴 — 중간 문맥 채택이 원인, 45단계). 방어 재대결과 동형: 후보 전수 실측, 상승 시만 교체.
      if (balancedJob) {
        Equipped curWeapon = items.get(Slot.WEAPON);
        boolean weaponFixed =
            curWeapon != null
                && curWeapon.isUnique()
                && fixedUniques.stream().anyMatch(u -> u.slug().equals(curWeapon.unique().slug()));
        if (!weaponFixed) {
          List<Equipped> weaponTrials = new ArrayList<>();
          for (PoeUniqueItem unique : itemCandidates(Slot.WEAPON, gem, keywords, items)) {
            weaponTrials.add(Equipped.ofUnique(unique));
          }
          String weaponCategory =
              (gem.tags() != null && gem.tags().contains("Attack"))
                  ? "weaponAttack"
                  : "weaponSpell";
          for (PoeBaseItem base : weaponBaseCandidates(gem, 6, items.get(Slot.OFFHAND))) {
            RareItem weaponRare = craftRare(weaponCategory, base.name(), keywords, 0.0, false);
            if (weaponRare != null) {
              weaponTrials.add(Equipped.ofRare(weaponRare));
            }
          }
          if (!weaponTrials.isEmpty()) {
            Map<Equipped, Double> weaponResults =
                evalBatch(
                    executor,
                    weaponTrials,
                    trial -> {
                      Map<Slot, Equipped> trialItems = new EnumMap<>(items);
                      trialItems.put(Slot.WEAPON, trial);
                      return buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          trialItems,
                          jewels);
                    },
                    objectiveKey);
            Map.Entry<Equipped, Double> bestWeapon =
                weaponResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            if (bestWeapon != null && bestWeapon.getValue() > current * 1.003) {
              items.put(Slot.WEAPON, bestWeapon.getKey());
              current = bestWeapon.getValue();
              log(
                  "무기 최종 재대결: → "
                      + (bestWeapon.getKey().isUnique()
                          ? bestWeapon.getKey().unique().name()
                          : "레어 " + bestWeapon.getKey().rare().baseType())
                      + " → "
                      + format(current));
            } else {
              log(
                  "무기 최종 재대결: 유지 — 최고 "
                      + (bestWeapon != null ? format(bestWeapon.getValue()) : "-")
                      + " vs 현재 "
                      + format(current));
            }
          }
        }
      }

      // ── 트리 최종 재대결 — 트리는 **장비·오라·주얼이 없는 상태**에서 확정되고 그 뒤로 되돌아보지 않는다.
      // 그래서 완성 빌드에선 값이 죽은 잎(예: 무기가 바뀌어 무의미해진 계열)이 남고, 정작 지금 문맥에서
      // 강한 노터블은 못 들어온다. 후보/예산을 조금만 바꿔도 결과가 ±5~20% 흔들리던 원인이기도 하다
      // (실측: 레벨 +10pt −4.1%, 유니크 후보 +34종 −9.3% — 국소 최적에 갇혀 경로에 휘둘림).
      // 완성 문맥에서 「값싼 잎 제거 ↔ 미할당 노터블 편입」을 상승할 때만 반복 채택한다.
      {
        enterPhase("tree-rematch");
        Integer startNode = poeTreeGraphService.classStart(className);
        List<PoeTreeGraphService.TreeNode> rematchPool =
            poeTreeGraphService.searchCandidates().stream()
                .filter(n -> !allocated.contains(n.id()))
                .filter(n -> score(n.stats(), keywords) > 0)
                .sorted(
                    Comparator.comparingInt(
                            (PoeTreeGraphService.TreeNode n) -> score(n.stats(), keywords))
                        .reversed())
                .limit(TREE_REMATCH_CANDIDATES)
                .toList();
        int adopted = 0;
        for (int round = 0; round < TREE_REMATCH_ROUNDS; round++) {
          PoeTreeGraphService.TreeNode bestNode = null;
          Set<Integer> bestNodes = null;
          double bestVal = current;
          for (PoeTreeGraphService.TreeNode cand : rematchPool) {
            if (allocated.contains(cand.id())) {
              continue;
            }
            List<Integer> path = poeTreeGraphService.shortestPath(allocated, cand.id());
            if (path == null || path.isEmpty()) {
              continue;
            }
            Set<Integer> trial = new LinkedHashSet<>(allocated);
            trial.addAll(path);
            trial.add(cand.id());
            int need = trial.size() - allocated.size() - (POINT_BUDGET - points);
            if (need > 0) {
              Set<Integer> freed =
                  removableLeaves(trial, allocated, need, startNode, keywords, path, cand.id());
              if (freed == null) {
                continue; // 뗄 잎이 부족 — 이 후보는 넘어간다
              }
              trial.removeAll(freed);
            }
            double val =
                objectiveOf(
                    poePobEngineService.calculateValues(
                        buildXml(
                            gem,
                            supports,
                            className,
                            ascendancy,
                            ascendancyNodes,
                            trial,
                            items,
                            jewels)),
                    objectiveKey);
            evalCount.incrementAndGet();
            if (val > bestVal * 1.003) {
              bestVal = val;
              bestNode = cand;
              bestNodes = trial;
            }
          }
          if (bestNode == null) {
            break;
          }
          points = points + bestNodes.size() - allocated.size();
          allocated.clear();
          allocated.addAll(bestNodes);
          log(
              "트리 재대결 채택: "
                  + (bestNode.nameKo() != null ? bestNode.nameKo() : bestNode.name())
                  + " → "
                  + format(bestVal)
                  + " (이전 "
                  + format(current)
                  + ")");
          current = bestVal;
          adopted++;
        }
        if (adopted == 0) {
          log("트리 재대결: 이득 없음(현 트리 유지) — 후보 " + rematchPool.size() + "개");
        }
      }

      // ── 유니크 최종 재대결 — 장비 단계는 주얼·오라·마스터리·문신 이전의 중간 문맥이라, 그 시점의
      // 슬롯별 우승자가 완성 빌드에서도 최선이라는 보장이 없다. 특히 **다른 축으로 값을 내는 유니크**
      // (예: 생명력을 크게 주는 갑옷 — RF 는 생명력이 곧 피해다)는 중간 문맥에서 피해 유니크에 밀린다.
      // 실측 근거: 대표 실빌드는 생명력 21,798 인데 우리 빌드는 그 축을 아예 안 밟는다.
      // 완성 문맥에서 슬롯별 유니크 후보를 다시 재 보고 상승할 때만 교체한다(기존 방어 재대결은
      // "현재 유니크 ↔ 방어 레어" 1:1 이라 유니크 전수는 못 봤다).
      {
        enterPhase("unique-rematch");
        int adoptedUnique = 0;
        for (int round = 0; round < UNIQUE_REMATCH_ROUNDS; round++) {
          Slot bestSlot = null;
          PoeUniqueItem bestPick = null;
          double bestVal = current;
          for (Slot slot :
              new Slot[] {
                Slot.BODY,
                Slot.HELMET,
                Slot.GLOVES,
                Slot.BOOTS,
                Slot.AMULET,
                Slot.RING1,
                Slot.RING2,
                Slot.BELT,
                Slot.OFFHAND
              }) {
            Equipped cur = items.get(slot);
            if (cur != null
                && cur.isUnique()
                && fixedUniques.stream().anyMatch(u -> u.slug().equals(cur.unique().slug()))) {
              continue; // 사용자 강제 유니크 존중
            }
            List<PoeUniqueItem> pool = itemCandidates(slot, gem, keywords, items);
            int tried = 0;
            for (PoeUniqueItem cand : pool) {
              if (tried >= UNIQUE_REMATCH_PER_SLOT) {
                break;
              }
              if (cur != null && cur.isUnique() && cur.unique().slug().equals(cand.slug())) {
                continue;
              }
              tried++;
              Map<Slot, Equipped> trial = new EnumMap<>(items);
              trial.put(slot, Equipped.ofUnique(cand));
              double val =
                  objectiveOf(
                      poePobEngineService.calculateValues(
                          buildXml(
                              gem,
                              supports,
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              trial,
                              jewels)),
                      objectiveKey);
              evalCount.incrementAndGet();
              if (val > bestVal * 1.003) {
                bestVal = val;
                bestSlot = slot;
                bestPick = cand;
              }
            }
          }
          if (bestSlot == null) {
            break;
          }
          items.put(bestSlot, Equipped.ofUnique(bestPick));
          log(
              "유니크 재대결 채택: "
                  + bestSlot.ko
                  + " = "
                  + (bestPick.nameKo() != null ? bestPick.nameKo() : bestPick.name())
                  + " → "
                  + format(bestVal)
                  + " (이전 "
                  + format(current)
                  + ")");
          current = bestVal;
          adoptedUnique++;
        }
        if (adoptedUnique == 0) {
          log("유니크 재대결: 이득 없음(현 장비 유지)");
        }
      }

      // ── 금단의 화염/살점 — **다른 직업의 어센던시 노터블 1개**를 주얼 2칸으로 사 오는 실빌드 표준.
      // PoB 는 아이템 문구("Allocates X if you have the matching modifier on Forbidden Flame/Flesh")를
      // GrantedAscendancyNode 로 읽어 실제로 노터블을 켠다. 우리 유니크 데이터엔 이 둘이 없다 —
      // PoB 가 아이템 목록이 아니라 코드로 생성하기 때문(Special/Generated.lua). 그래서 여기서 합성한다.
      // 근거: 하회 아키타입(RF·번개 화살) 대표 실빌드가 **둘 다** 금단의 살점을 낀다.
      if (!jewels.isEmpty() && forbiddenEnabled) {
        enterPhase("forbidden");
        List<PoeTreeGraphService.TreeNode> notables =
            poeTreeGraphService.foreignAscendancyNotables(className, ascendancy).stream()
                .filter(n -> score(n.stats(), keywords) > 0)
                .sorted(
                    Comparator.comparingInt(
                            (PoeTreeGraphService.TreeNode n) -> score(n.stats(), keywords))
                        .reversed())
                .limit(FORBIDDEN_CANDIDATES)
                .toList();
        // 바꿔 낄 자리는 **지금 가장 값이 낮은 주얼 2칸** — 소켓은 트리에서 이미 확보돼 있다.
        List<Integer> swapSockets =
            jewels.entrySet().stream()
                .sorted(
                    Comparator.comparingInt(
                        e ->
                            e.getValue().isUnique()
                                ? score(e.getValue().unique().explicits(), keywords)
                                : 0))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
        if (swapSockets.size() == 2 && !notables.isEmpty()) {
          PoeTreeGraphService.TreeNode bestNotable = null;
          double bestVal = current;
          for (PoeTreeGraphService.TreeNode notable : notables) {
            Map<Integer, Equipped> trial = new LinkedHashMap<>(jewels);
            trial.put(
                swapSockets.get(0),
                Equipped.ofUnique(forbiddenJewel(notable.name(), true, className)));
            trial.put(
                swapSockets.get(1),
                Equipped.ofUnique(forbiddenJewel(notable.name(), false, className)));
            double val =
                objectiveOf(
                    poePobEngineService.calculateValues(
                        buildXml(
                            gem,
                            supports,
                            className,
                            ascendancy,
                            ascendancyNodes,
                            allocated,
                            items,
                            trial)),
                    objectiveKey);
            evalCount.incrementAndGet();
            // 후보별 실측을 남긴다 — 값이 전부 같으면 "이득 없음"이 아니라 **문구가 안 먹은 것**이다.
            log("금단 후보: " + notable.name() + " (" + notable.ascendancy() + ") → " + format(val));
            if (val > bestVal * 1.003) {
              bestVal = val;
              bestNotable = notable;
            }
          }
          if (bestNotable != null) {
            jewels.put(
                swapSockets.get(0),
                Equipped.ofUnique(forbiddenJewel(bestNotable.name(), true, className)));
            jewels.put(
                swapSockets.get(1),
                Equipped.ofUnique(forbiddenJewel(bestNotable.name(), false, className)));
            log(
                "금단 페어 채택: "
                    + (bestNotable.nameKo() != null ? bestNotable.nameKo() : bestNotable.name())
                    + " ("
                    + bestNotable.ascendancy()
                    + ") → "
                    + format(bestVal)
                    + " (이전 "
                    + format(current)
                    + ")");
            current = bestVal;
          } else {
            log("금단 페어: 이득 없음 — 후보 " + notables.size() + "개(주얼 2칸 교체 기준)");
          }
        }
      }

      // ── 영원한 축복 오라 — 예약 없이 오라 하나를 더 얹는다(실빌드 표준 경로).
      // 우리 빌드는 혈마법/예약 포화로 오라가 1개에서 멈추는데(미예약 마나 0), 대표 실빌드는
      // Eternal Blessing 으로 지속 피해 배율 오라를 공짜로 유지한다. 이 경로가 없으면 오라 연계
      // (감시자의 눈 오라 조건부 모드 등)도 통째로 죽는다 — 실측: 감시자의 눈 후보가 정화의 얼음뿐이라 기각.
      {
        enterPhase("blessing");
        List<PoeGem> blessingPool =
            poeGemDataService.search(null, "active", "all", null).stream()
                .filter(a -> AURA_NAMES.contains(a.name()))
                .filter(a -> !a.levels().isEmpty())
                .filter(a -> selectedAuras.stream().noneMatch(x -> x.slug().equals(a.slug())))
                .filter(a -> additionalSkills.stream().noneMatch(x -> x.slug().equals(a.slug())))
                .filter(a -> score(List.of(a.name()), keywords) >= 0)
                .toList();
        PoeGem bestBlessing = null;
        double bestBlessingVal = current;
        PoeGem savedBlessing = blessingAura;
        try {
          for (PoeGem aura : blessingPool) {
            blessingAura = aura;
            double val =
                objectiveOf(
                    poePobEngineService.calculateValues(
                        buildXml(
                            gem,
                            supports,
                            className,
                            ascendancy,
                            ascendancyNodes,
                            allocated,
                            items,
                            jewels)),
                    objectiveKey);
            evalCount.incrementAndGet();
            if (val > bestBlessingVal * 1.003) {
              bestBlessingVal = val;
              bestBlessing = aura;
            }
          }
        } finally {
          blessingAura = savedBlessing;
        }
        if (bestBlessing != null) {
          blessingAura = bestBlessing;
          log(
              "영원한 축복 오라 채택: "
                  + (bestBlessing.nameKo() != null ? bestBlessing.nameKo() : bestBlessing.name())
                  + " → "
                  + format(bestBlessingVal)
                  + " (이전 "
                  + format(current)
                  + ")");
          current = bestBlessingVal;
        } else {
          log("영원한 축복 오라: 이득 없음 — 후보 " + blessingPool.size() + "개");
        }
      }

      // ── 감시자의 눈 — 지금 낀 오라 조건부 모드를 골라 합성해 주얼 한 칸과 맞바꾼다.
      // PoB 가 코드로 만드는 유니크라 우리 데이터엔 없고(Special/WatchersEye.lua = 모드 풀),
      // 그래서 최적화기가 여태 한 번도 못 써 봤다 — 실빌드는 표준 장비다(대표 RF·LA 둘 다 착용).
      if (!jewels.isEmpty()
          && !watchersEyeMods.isEmpty()
          && (!selectedAuras.isEmpty() || blessingAura != null)) {
        enterPhase("watchers-eye");
        // 오라 목록엔 **영원한 축복으로 유지하는 오라**도 포함해야 한다 — 그게 보통 핵심 오라(악의 등)라
        // 감시자의 눈의 값이 거기서 나온다. 축복 단계보다 먼저 돌면 그 오라를 못 봐 늘 기각된다(실측).
        List<PoeGem> auraNames = new ArrayList<>(selectedAuras);
        if (blessingAura != null) {
          auraNames.add(blessingAura);
        }
        List<WatchersEyeMod> pool = new ArrayList<>();
        for (PoeGem aura : auraNames) {
          pool.addAll(watchersEyeMods.getOrDefault(aura.name(), List.of()));
        }
        // 같은 문구가 여러 번 들어 있다(롤 구간별) — 문구 기준 중복 제거 후 키워드 점수 상위만 본다.
        List<WatchersEyeMod> ranked =
            pool.stream()
                .collect(
                    java.util.stream.Collectors.toMap(
                        WatchersEyeMod::en, m -> m, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .sorted(
                    Comparator.comparingInt((WatchersEyeMod m) -> score(List.of(m.en()), keywords))
                        .reversed())
                .limit(WATCHERS_EYE_MOD_CANDIDATES)
                .toList();
        Integer swapSocket =
            jewels.entrySet().stream()
                .min(
                    Comparator.comparingInt(
                        e ->
                            e.getValue().isUnique()
                                ? score(e.getValue().unique().explicits(), keywords)
                                : 0))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (swapSocket != null && ranked.size() >= 2) {
          // 인게임 감시자의 눈은 오라 조건부 모드를 보통 2개 굴린다.
          // ⚠ 어느 2개인지는 **엔진이** 고르게 한다 — 키워드 점수로 뽑으면 "받는 피해" 계열이 1순위로
          //    올라온다(문구에 Fire/Damage 가 들어가서). 실측: 정화의 얼음 방어 모드가 악의 지속피해 배율을
          //    제치고 뽑혀 조합이 통째로 기각됐다. 상위 4개의 모든 짝(6가지)을 재 보는 편이 정직하다.
          List<WatchersEyeMod> shortlist = ranked.subList(0, Math.min(4, ranked.size()));
          List<WatchersEyeMod> bestPair = null;
          double bestPairVal = current;
          for (int i = 0; i < shortlist.size(); i++) {
            for (int j = i + 1; j < shortlist.size(); j++) {
              List<WatchersEyeMod> pair = List.of(shortlist.get(i), shortlist.get(j));
              Map<Integer, Equipped> trial = new LinkedHashMap<>(jewels);
              trial.put(swapSocket, Equipped.ofUnique(watchersEyeJewel(pair)));
              double val =
                  objectiveOf(
                      poePobEngineService.calculateValues(
                          buildXml(
                              gem,
                              supports,
                              className,
                              ascendancy,
                              ascendancyNodes,
                              allocated,
                              items,
                              trial)),
                      objectiveKey);
              evalCount.incrementAndGet();
              if (val > bestPairVal * 1.003) {
                bestPairVal = val;
                bestPair = pair;
              }
            }
          }
          if (bestPair != null) {
            jewels.put(swapSocket, Equipped.ofUnique(watchersEyeJewel(bestPair)));
            log(
                "감시자의 눈 채택: "
                    + bestPair.stream().map(WatchersEyeMod::en).toList()
                    + " → "
                    + format(bestPairVal)
                    + " (이전 "
                    + format(current)
                    + ")");
            current = bestPairVal;
          } else {
            log("감시자의 눈: 이득 없음 — 상위 " + shortlist.size() + "개 짝 전수 실측");
          }
        }
      }

      // ⚠ 위치 주의: 이 재대결은 **감시자의 눈 다음, 티어 비교 앞**에 둔다. 원래 가드 앞(이른 자리)에 뒀더니
      //   채택 이후 단계들이 다른 가지로 내려가 정의의 화염이 −4.3% 났다(번개 화살 +19.7%·고행 +6.3% 는 유지).
      //   뒤따르는 단계가 적을수록 경로 이탈 여지가 작다.
      // ── 유니크 주얼 최종 재대결 — 보조젬과 같은 이유. 주얼은 **아이템 이전**에 확정되는데(실측 채택값
      //   502~613, 최종 문맥은 수천만) 이후 유니크 주얼끼리 다시 겨루는 단계가 없었다. 뒤쪽 "주얼 최종
      //   교체(제작 레어)" 는 제작 레어로 바꾸는 경로일 뿐이라 유니크↔유니크 비교를 대신하지 못한다.
      //   소켓별로 풀 상위 후보와 1:1 교체를 실측해 상승 시만 채택.
      if (balancedJob && !jewelRematchPool.isEmpty() && !jewels.isEmpty()) {
        enterPhase("jewels-rematch");
        record JewelSwap(int socket, PoeUniqueItem cand) {}
        for (int jRound = 0; jRound < 2; jRound++) {
          List<JewelSwap> jTrials = new ArrayList<>();
          for (Integer socket : jewels.keySet()) {
            Equipped cur = jewels.get(socket);
            int tried = 0;
            for (PoeUniqueItem cand : jewelRematchPool) {
              if (tried >= JEWEL_REMATCH_PER_SOCKET) {
                break;
              }
              if (cur != null && cur.isUnique() && cur.unique().slug().equals(cand.slug())) {
                continue;
              }
              // 이미 다른 소켓에 낀 유니크는 중복 장착 불가
              if (jewels.values().stream()
                  .anyMatch(
                      j -> j != null && j.isUnique() && j.unique().slug().equals(cand.slug()))) {
                continue;
              }
              tried++;
              jTrials.add(new JewelSwap(socket, cand));
            }
          }
          if (jTrials.isEmpty()) {
            break;
          }
          Map<JewelSwap, Double> jResults =
              evalBatch(
                  executor,
                  jTrials,
                  trial -> {
                    Map<Integer, Equipped> trialJewels = new LinkedHashMap<>(jewels);
                    trialJewels.put(trial.socket(), Equipped.ofUnique(trial.cand()));
                    return buildXml(
                        gem,
                        supports,
                        className,
                        ascendancy,
                        ascendancyNodes,
                        allocated,
                        items,
                        trialJewels);
                  },
                  objectiveKey);
          Map.Entry<JewelSwap, Double> bestJ =
              jResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
          if (bestJ != null && bestJ.getValue() > current * 1.003) {
            Equipped dropped = jewels.get(bestJ.getKey().socket());
            jewels.put(bestJ.getKey().socket(), Equipped.ofUnique(bestJ.getKey().cand()));
            current = bestJ.getValue();
            log(
                "주얼 최종 재대결: "
                    + (dropped == null ? "-" : jewelLabel(dropped))
                    + " → "
                    + bestJ.getKey().cand().name()
                    + " → "
                    + format(current));
          } else {
            log(
                "주얼 최종 재대결: 유지 — 최고 "
                    + (bestJ != null ? format(bestJ.getValue()) : "-")
                    + " vs 현재 "
                    + format(current)
                    + " (후보 "
                    + jTrials.size()
                    + "개)");
            break;
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

      // ── 저항 캡 보정 — balanced 전용. 최종 빌드(트리·마스터리·문신·오라·주얼 모두 반영) 기준으로 미캡 원소 저항을
      //   레어 접미어(저항)로 캡한다. 실빌드는 저항을 반드시 캡(사용자 지적). 변경 시 finalXml/finalValues 재계산.
      //   dps/ehp 목표엔 미적용(기준선 불변). 이미 캡된 빌드(RF 등)는 미발동.
      if ("balanced".equals(objectiveKey)
          && repairResistanceShortfalls(
              items, gem, supports, className, ascendancy, ascendancyNodes, allocated, jewels)) {
        finalXml =
            buildXml(
                gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels);
        finalValues = poePobEngineService.calculateValues(finalXml);
        evalCount.incrementAndGet();
      }

      // ── 재생 후기 스위치 — balanced×자가연소(RF류) 전용. 장비·오라가 확정돼 미초과 화염저항이 최대인
      //   시점에, 이미 찍은 마스터리 노드의 효과를 메타 재생 효과("미초과 화염저항 1%당 재생 1" 등)로 바꿔
      //   재평가한다. 파이프라인 중반 평가에선 저항이 낮아 재생 효과가 구조적으로 저평가된다(실측: 실빌드
      //   81% 채택 효과가 계속 탈락, 가중 강화로는 전 축 회귀 — P2 교훈). 효과 전환은 포인트 0, 목표값이
      //   실제로 오를 때만 채택(단조-개선). dps/ehp·비자가연소 미발동 → 기준선 불변.
      if ("balanced".equals(objectiveKey) && selfBurnRun && !metaMasteries.isEmpty()) {
        double lateCur = objectiveOf(finalValues, objectiveKey);
        boolean lateChanged = false;
        for (int nodeId : new ArrayList<>(allocated)) {
          PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(nodeId);
          if (node == null || node.masteryEffects() == null || node.masteryEffects().isEmpty()) {
            continue;
          }
          Integer chosenEff = fixedMasteries.get(nodeId);
          Set<Integer> usedEffects = new HashSet<>(fixedMasteries.values());
          for (PoeTreeGraphService.MasteryEffect eff : node.masteryEffects()) {
            if (!isMetaMasteryEffect(eff)
                || (chosenEff != null && chosenEff == eff.id())
                || usedEffects.contains(eff.id())) {
              continue; // 메타 아님/이미 선택/타 노드가 사용(동일 효과 중복 금지)
            }
            Map<Integer, Integer> saved = fixedMasteries;
            Map<Integer, Integer> trial = new LinkedHashMap<>(saved);
            trial.put(nodeId, eff.id());
            fixedMasteries = trial;
            String trialXml =
                buildXml(
                    gem,
                    supports,
                    className,
                    ascendancy,
                    ascendancyNodes,
                    allocated,
                    items,
                    jewels);
            fixedMasteries = saved;
            Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
            evalCount.incrementAndGet();
            double v = objectiveOf(trialVals, objectiveKey);
            if (v > lateCur) {
              fixedMasteries = trial;
              lateCur = v;
              lateChanged = true;
              log(
                  "재생 후기 스위치: "
                      + (node.nameKo() != null ? node.nameKo() : node.name())
                      + " → "
                      + (eff.statsKo() != null && !eff.statsKo().isEmpty()
                          ? eff.statsKo().get(0)
                          : eff.stats().isEmpty() ? String.valueOf(eff.id()) : eff.stats().get(0))
                      + " → "
                      + format(lateCur));
            }
          }
        }
        if (lateChanged) {
          finalXml =
              buildXml(
                  gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels);
          finalValues = poePobEngineService.calculateValues(finalXml);
          evalCount.incrementAndGet();
        } else {
          log("재생 후기 스위치: 이득 없음(현 효과 유지)");
        }
      }

      // ── 레어 주얼 최종 리파인 — balanced 전용(저항캡 패스와 동일 원칙: dps/ehp 기준선 보존). 완성 빌드
      //   (장비·트리·오라·주얼 모두 반영) 기준으로 각 주얼 소켓에 제작 레어(DPS/방어)를 시도해 **목표값이 실제로
      //   오를 때만** 교체(단조-개선). 주얼 greedy(아이템 前)에 제작 주얼을 넣으면 빈약한 부분 빌드 기준 조기 채택으로
      //   회귀했다(cyclone 11.70M→10.32M) → 최종 빌드 기준 이 패스로만. balanced 는 "실제로 쓸 수 있는 최상"
      //   목표라 실빌드처럼 레어 주얼을 반영(사용자 요구). dps/ehp 는 이론 상한 목적이라 유니크 greedy 그대로 →
      //   arc dps 44,259,237·cyclone dps 11,699,753 invariant 불변.
      if ("balanced".equals(objectiveKey)
          && finalizeJewelsWithRares(
              jewels,
              gem,
              supports,
              className,
              ascendancy,
              ascendancyNodes,
              allocated,
              items,
              keywords,
              objectiveKey)) {
        finalXml =
            buildXml(
                gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels);
        finalValues = poePobEngineService.calculateValues(finalXml);
        evalCount.incrementAndGet();
      }

      // ── ES/CI 듀얼패스 — esArchetype+balanced 전용. CI 강제 + ES 트리(스코어 greedy) + ES 방어 장비로 대안
      //   빌드를 만들어 생명 빌드와 목표값 비교, 더 나을 때만 채택(단조-개선 → 회귀 불가). 생명 경로 미접촉이라
      //   arc/cyclone/RF·dps/ehp 는 구조적 불변. CI(생명→1)는 ES 장비+ES 트리와 **함께**여야 이득이라
      //   개별 greedy 로는 골짜기를 못 넘어(문서화된 EHP 964k→729k) 완성 빌드 대안 비교 방식으로만 가능.
      if (esArchetype && "balanced".equals(objectiveKey) && classStart != null) {
        // CI 서브그룹 생존 목표 스위치 — 듀얼패스 내부 트라이얼(장비/오라/재선발)이 CI 실빌드의 EHP
        // 수준(|ci 중앙값)을 좇게 한다. 최종 채택 비교 전에 복원 — 생명 빌드와의 비교는 기존 목표로
        // (편향 없는 단조-개선 보존, 채택 여부와 무관하게 이후 단계도 기존 목표).
        double prevTargetEhp = targetEhp;
        double ciMedianEhp = ciSubgroupMedianEhp(ascendancy, gem.name());
        if (ciMedianEhp > 0) {
          targetEhp = clampD(ciMedianEhp, EHP_FLOOR, EHP_FLOOR * 8);
          log(
              "ES/CI 생존 목표 스위치: |ci 서브그룹 EHP 중앙값 "
                  + format(targetEhp)
                  + " (혼재 "
                  + format(prevTargetEhp)
                  + ")");
        }
        EsBuild es;
        try {
          es =
              tryEsTemplate(
                  gem,
                  supports,
                  className,
                  ascendancy,
                  ascendancyNodes,
                  classStart,
                  allocated,
                  items,
                  jewels,
                  keywords,
                  objectiveKey,
                  executor);
        } finally {
          targetEhp = prevTargetEhp;
        }
        if (es != null
            && objectiveOf(es.values(), objectiveKey)
                > objectiveOf(finalValues, objectiveKey) * 1.003) {
          allocated.clear();
          allocated.addAll(es.nodes());
          items.clear();
          items.putAll(es.items());
          finalXml = es.xml();
          finalValues = es.values();
          // ES 대안 전용 오라 세트(Discipline 추가/교체) — 안 맞추면 결과 오라 목록과 XML 이 어긋난다
          if (es.auraOverride() != null) {
            selectedAuras.clear();
            selectedAuras.addAll(es.auraOverride());
          }
          // 4e 후기 서포트 재선발 결과 — 안 맞추면 결과 서포트 목록과 XML 이 어긋난다(오라와 동일 원칙)
          if (es.supportsOverride() != null) {
            supports.clear();
            supports.addAll(es.supportsOverride());
          }
          log(
              "ES/CI 듀얼패스 채택: CI+ES 빌드가 생명 빌드보다 우수 → "
                  + format(objectiveOf(finalValues, objectiveKey))
                  + (es.auraOverride() == null
                      ? ""
                      : " (오라 → "
                          + es.auraOverride().stream()
                              .map(PoeGem::name)
                              .collect(java.util.stream.Collectors.joining(", "))
                          + ")"));
        } else {
          // 수치 없는 승패 로그로는 갭 레버를 특정할 수 없다 — 대안의 목표값·EHP·ES 를 함께 남긴다.
          log(
              "ES/CI 듀얼패스: 생명 빌드 유지 — 대안 "
                  + (es == null
                      ? "생성 실패(CI 경로/예산)"
                      : format(objectiveOf(es.values(), objectiveKey))
                          + " (EHP "
                          + format(es.values().getOrDefault("TotalEHP", 0.0))
                          + " · ES "
                          + format(es.values().getOrDefault("EnergyShield", 0.0))
                          + ")")
                  + " vs 현재 "
                  + format(objectiveOf(finalValues, objectiveKey))
                  + " (EHP "
                  + format(finalValues.getOrDefault("TotalEHP", 0.0))
                  + ")");
        }
      }

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
        // ⚠ 기준은 objectiveOf 로 — 트라이얼(evalBatch)이 objectiveKey 스코어라, displayMetric(effectiveDps)
        //   과 비교하면 balanced 잡(스코어 = DPS×생존팩터 < DPS)에선 어떤 도유도 못 넘어 영구 비활성이었다
        //   (실측: RF "470개 모두 이득 없음" 인데 수동 A/B 는 Discipline and Training +4.8% DPS·EHP).
        double anointBase = objectiveOf(finalValues, objectiveKey);
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
        double unreservedLifeFinal = finalValues.getOrDefault("LifeUnreserved", 1d);
        while ((unreservedFinal < MIN_UNRESERVED_MANA || unreservedLifeFinal < MIN_UNRESERVED_LIFE)
            && !selectedAuras.isEmpty()) {
          PoeGem dropped = selectedAuras.remove(selectedAuras.size() - 1);
          log(
              "오라 최종 예약 초과 — 해제: "
                  + dropped.name()
                  + " (미예약 마나 "
                  + Math.round(unreservedFinal)
                  + " / 생명력 "
                  + Math.round(unreservedLifeFinal)
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
          unreservedLifeFinal = finalValues.getOrDefault("LifeUnreserved", 1d);
        }
      }

      // ── 최종 재대결(도유 이후, balanced) — 방어 슬롯 재대결은 도유·재생 후기 스위치 **이전** 문맥이라,
      // 이후 문맥에서 역전되는 차선 유니크가 남는다(실사고: Blasphemer's Grasp — 재대결 시점 651,675 에선
      // 우세였지만 도유 후 737,301 문맥에선 평범 방어 레어가 +4.9% DPS/+6.6% EHP). 완성 finalXml 문맥에서
      // 방어 슬롯 유니크를 방어 레어와 1:1 재대결, 상승 시만 채택(도유 재적용 포함, 개선 소진까지 최대 2라운드).
      if (balancedJob) {
        for (int finalRound = 0; finalRound < 2; finalRound++) {
          boolean improvedFinal = false;
          for (Slot defSlot :
              new Slot[] {
                Slot.BODY,
                Slot.HELMET,
                Slot.GLOVES,
                Slot.BOOTS,
                Slot.AMULET,
                Slot.RING1,
                Slot.RING2,
                Slot.BELT
              }) {
            Equipped cur = items.get(defSlot);
            if (cur == null || !cur.isUnique()) {
              continue;
            }
            final Equipped curFinal2 = cur;
            if (fixedUniques.stream().anyMatch(u -> u.slug().equals(curFinal2.unique().slug()))) {
              continue;
            }
            // ⚠ 4-인자 craftRare 는 defensive=forceEsBase(평시 false)라 **데미지 레어**를 만든다 —
            //   Blasphemer 를 이긴 건 생명+저항 방어 레어(craftDefensiveRare)였다(실측 +4.9%/+6.6%).
            //   두 방향 모두 트라이얼(중복 구성은 1회).
            List<RareItem> rareTrials = new ArrayList<>();
            RareItem dmgRare = craftRare(defSlot, gem, keywords, 0.0);
            if (dmgRare != null) {
              rareTrials.add(dmgRare);
            }
            RareItem defRare = craftDefensiveRare(defSlot, gem, keywords);
            if (defRare != null
                && (dmgRare == null || !defRare.families().equals(dmgRare.families()))) {
              rareTrials.add(defRare);
            }
            // 부족 속성 보정 재도전 — 유니크가 속성 공급원일 때(실사고: Blasphemer's Grasp 민첩 +50)
            // 단순 레어는 feasibility 감쇠(0.5%/pt, ~40pt=×0.79)로 항상 진다. 기각된 트라이얼의 부족
            // 최대 속성 접미(+60 T1)를 편입한 변형으로 1회 재도전(변형까지 지면 유니크 유지가 정당).
            List<RareItem> pendingTrials = new ArrayList<>(rareTrials);
            for (int t = 0; t < pendingTrials.size(); t++) {
              RareItem rare = pendingTrials.get(t);
              Map<Slot, Equipped> trialItems = new EnumMap<>(items);
              trialItems.put(defSlot, Equipped.ofRare(rare));
              String trialXml =
                  buildXml(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      allocated,
                      trialItems,
                      jewels);
              if (currentAnoint != null) {
                trialXml = withAnoint(trialXml, currentAnoint.name());
              }
              Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
              evalCount.incrementAndGet();
              if (!(objectiveOf(trialVals, objectiveKey)
                  > objectiveOf(finalValues, objectiveKey) * 1.003)) {
                double shortStr =
                    trialVals.getOrDefault("ReqStr", 0d) - trialVals.getOrDefault("Str", 0d);
                double shortDex =
                    trialVals.getOrDefault("ReqDex", 0d) - trialVals.getOrDefault("Dex", 0d);
                double shortInt =
                    trialVals.getOrDefault("ReqInt", 0d) - trialVals.getOrDefault("Int", 0d);
                String attrKey =
                    shortStr >= shortDex && shortStr >= shortInt && shortStr > 0
                        ? "str"
                        : shortDex >= shortInt && shortDex > 0
                            ? "dex"
                            : shortInt > 0 ? "int" : null;
                if (attrKey != null && pendingTrials.size() < rareTrials.size() + 2) {
                  RareItem fixed = withAttributeSuffix(rare, defSlot, attrKey);
                  if (fixed != null) {
                    pendingTrials.add(fixed);
                  }
                }
              }
              // (수사 종결 — 유니크가 이기는 주 요인은 feasibilityFactor: 예. Blasphemer's Grasp 민첩 +50이
              //  젬 요구치를 지탱, 레어 교체 시 속성 ~40pt 부족 감쇠 ×0.79. raw DPS/EHP 우세만으로 교체하면
              //  실전 착용 불가 조합이 된다 — 스코어의 정당 선호.)
              if (objectiveOf(trialVals, objectiveKey)
                  > objectiveOf(finalValues, objectiveKey) * 1.003) {
                items.put(defSlot, Equipped.ofRare(rare));
                finalXml = trialXml;
                finalValues = trialVals;
                improvedFinal = true;
                log(
                    "최종 재대결(도유 후): "
                        + defSlot.pobName
                        + " "
                        + curFinal2.unique().name()
                        + " → 방어 레어("
                        + rare.baseType()
                        + ") → "
                        + format(objectiveOf(finalValues, objectiveKey)));
                break;
              }
            }
          }
          if (!improvedFinal) {
            break;
          }
        }
      }

      // ── 저항 채움 패스(balanced) — 아키타입 저항 목표(치프틴 RF 등 상향 캡 90) 미달분을 레어 접미 교체로 채운다.
      // 조향(2b 감쇠)은 있었지만 탐색에 "+N% 저항" 수(move)가 없어 화염 86 정체(실측, 사용자 지적) — 속성 보정과
      // 같은 패턴의 명시적 재도전. 가장 미달이 큰 원소부터, 레어 슬롯 하나씩 T1 저항 접미 교체 트라이얼, 개선 시 채택.
      if (balancedJob) {
        String[] resValueKeys = {"FireResist", "ColdResist", "LightningResist"};
        String[] resFamilyKeys = {"fireRes", "coldRes", "lightRes"};
        int[] resFillTargets = {targetFireRes, targetColdRes, targetLightRes};
        for (int fillRound = 0; fillRound < 3; fillRound++) {
          int worst = -1;
          double worstShort = 0.5; // 0.5 미만 미달은 무시(반올림 노이즈)
          for (int i = 0; i < resValueKeys.length; i++) {
            double r = finalValues.getOrDefault(resValueKeys[i], (double) resFillTargets[i]);
            double shortfall = resFillTargets[i] - r;
            if (shortfall > worstShort) {
              worstShort = shortfall;
              worst = i;
            }
          }
          if (worst < 0) {
            break;
          }
          boolean filled = false;
          for (Map.Entry<Slot, Equipped> entry : new ArrayList<>(items.entrySet())) {
            if (entry.getValue().isUnique()) {
              continue;
            }
            Slot slot = entry.getKey();
            RareItem swapped =
                withSuffixFamily(entry.getValue().rare(), slot, resFamilyKeys[worst]);
            if (swapped == null) {
              continue;
            }
            Map<Slot, Equipped> trialItems = new EnumMap<>(items);
            trialItems.put(slot, Equipped.ofRare(swapped));
            String trialXml =
                buildXml(
                    gem,
                    supports,
                    className,
                    ascendancy,
                    ascendancyNodes,
                    allocated,
                    trialItems,
                    jewels);
            if (currentAnoint != null) {
              trialXml = withAnoint(trialXml, currentAnoint.name());
            }
            Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
            evalCount.incrementAndGet();
            if (objectiveOf(trialVals, objectiveKey)
                > objectiveOf(finalValues, objectiveKey) * 1.003) {
              items.put(slot, Equipped.ofRare(swapped));
              finalXml = trialXml;
              finalValues = trialVals;
              filled = true;
              log(
                  "저항 채움: "
                      + slot.pobName
                      + " 접미 → "
                      + resFamilyKeys[worst]
                      + " (목표 "
                      + resFillTargets[worst]
                      + ", 이전 "
                      + String.format("%.0f", resFillTargets[worst] - worstShort)
                      + ") → "
                      + format(objectiveOf(finalValues, objectiveKey)));
              break;
            }
          }
          if (!filled) {
            break;
          }
        }
      }

      // ── 저항 캡 채움(문신, balanced) — Missing==0(총량은 캡 초과)인데 캡이 아키타입 목표(치프틴 RF 90 등)에
      // 미달인 원소를, **문신 없는** 소형 속성 노드에 "저항 최대치 +1%" 문신을 새겨 캡을 올린다.
      // 문신 greedy 는 장비 확정 전이라 그 시점엔 총량<캡이어서 최대치 문신이 무가치 판정되는 순서 문제(실측:
      // RF 화염 캡 86, OverCap 501). 사용자 지정·기존 자동 문신 노드는 건드리지 않는다(신규만 — 보수적 v1).
      if (balancedJob && poeTattooDataService.hasData()) {
        this.tattooAllocated = Set.copyOf(allocated); // 후기 트리 변경(재생 스위치 등) 반영해 갱신
        String[] capStatEn = {
          "maximum Fire Resistance", "maximum Cold Resistance", "maximum Lightning Resistance"
        };
        String[] capResKeys = {"FireResist", "ColdResist", "LightningResist"};
        String[] capMissingKeys = {
          "MissingFireResist", "MissingColdResist", "MissingLightningResist"
        };
        int[] capTargets = {targetFireRes, targetColdRes, targetLightRes};
        for (int e = 0; e < capResKeys.length; e++) {
          for (int round = 0; round < 5; round++) {
            double res = finalValues.getOrDefault(capResKeys[e], (double) capTargets[e]);
            Double missing = finalValues.get(capMissingKeys[e]);
            // 캡 병목일 때만: 목표 미달 + 총량은 캡에 닿음(Missing≈0). Missing 미제공(구 워커)이면 보수적으로 중단.
            if (res >= capTargets[e] - 0.5 || missing == null || missing > 0.5) {
              break;
            }
            final int elemIdx = e;
            boolean adopted = false;
            capFill:
            for (String attr : List.of("Strength", "Dexterity", "Intelligence")) {
              for (PoeTattooDataService.Tattoo tattoo :
                  poeTattooDataService.candidates("normal", attr)) {
                if (tattoo.stats().isEmpty()
                    || tattoo.stats().stream().noneMatch(l -> l.contains(capStatEn[elemIdx]))) {
                  continue;
                }
                long used =
                    fixedTattoos.values().stream().filter(dn -> dn.equals(tattoo.dn())).count();
                if (used >= tattooLimit(tattoo)) {
                  continue;
                }
                for (int nodeId : allocated) {
                  // 사용자 지정 문신만 불가침 — 자동 채택 문신(비둘기 등)은 교체 시험 대상.
                  // (v1 은 신규만이었는데 최대치 문신이 잎 전용(maxConnected=1)이라 잎을 선점한 자동
                  //  문신에 막혀 시도 0건이었다 — 실측)
                  if (userTattoos.containsKey(nodeId)
                      || tattoo.dn().equals(fixedTattoos.get(nodeId))
                      || !attr.equals(smallAttributeOf(poeTreeGraphService.node(nodeId)))
                      || !tattooFits(tattoo, nodeId)) {
                    continue;
                  }
                  Map<Integer, String> trial = new LinkedHashMap<>(fixedTattoos);
                  trial.put(nodeId, tattoo.dn());
                  Map<Integer, String> savedTattoos = fixedTattoos;
                  String trialXml;
                  try {
                    fixedTattoos = Map.of();
                    trialXml =
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
                            trial,
                            allocated);
                  } finally {
                    fixedTattoos = savedTattoos;
                  }
                  if (currentAnoint != null) {
                    trialXml = withAnoint(trialXml, currentAnoint.name());
                  }
                  Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
                  evalCount.incrementAndGet();
                  if (objectiveOf(trialVals, objectiveKey)
                      > objectiveOf(finalValues, objectiveKey) * 1.003) {
                    fixedTattoos = trial;
                    finalXml = trialXml;
                    finalValues = trialVals;
                    adopted = true;
                    log(
                        "저항 캡 채움(문신): "
                            + (tattoo.nameKo() != null ? tattoo.nameKo() : tattoo.dn())
                            + " ("
                            + capResKeys[elemIdx]
                            + " "
                            + String.format("%.0f", res)
                            + "→목표 "
                            + capTargets[elemIdx]
                            + ") → "
                            + format(objectiveOf(finalValues, objectiveKey)));
                    break capFill;
                  }
                }
              }
            }
            if (!adopted) {
              // 매달린 소형 신설 — 기존 자리(할당-이웃≤1 소형)가 트리에 없을 때(실측: 경로 소형은 이웃 2,
              // 경로 끝은 노터블/소켓): 가치 낮은 잎 소형 1pt 를 회수하고 경로 인접 미할당 속성 소형을 새로
              // 할당해 여정 문신 전용 자리를 만든다 — ninja 실빌드의 표준 기법.
              dangling:
              for (String attr : List.of("Strength", "Dexterity", "Intelligence")) {
                for (PoeTattooDataService.Tattoo tattoo :
                    poeTattooDataService.candidates("normal", attr)) {
                  if (tattoo.stats().isEmpty()
                      || tattoo.stats().stream().noneMatch(l -> l.contains(capStatEn[elemIdx]))) {
                    continue;
                  }
                  long used =
                      fixedTattoos.values().stream().filter(dn -> dn.equals(tattoo.dn())).count();
                  if (used >= tattooLimit(tattoo)) {
                    continue;
                  }
                  // 매달린 후보: 미할당 소형(해당 속성) && 할당 이웃 정확히 1(할당 시 여정 문신 조건 충족)
                  List<Integer> danglings = new ArrayList<>();
                  for (int a : allocated) {
                    for (int nb : poeTreeGraphService.neighbors(a)) {
                      if (allocated.contains(nb)
                          || danglings.contains(nb)
                          || !attr.equals(smallAttributeOf(poeTreeGraphService.node(nb)))) {
                        continue;
                      }
                      long nbAlloc =
                          poeTreeGraphService.neighbors(nb).stream()
                              .filter(allocated::contains)
                              .count();
                      if (nbAlloc == 1) {
                        danglings.add(nb);
                      }
                    }
                  }
                  danglings.sort(null);
                  log(
                      "캡 채움 진단["
                          + capResKeys[elemIdx]
                          + "]: "
                          + tattoo.dn()
                          + " 매달린 후보 "
                          + danglings.size()
                          + "개");
                  // 회수 후보 잎: 할당-차수 ≤1 소형(사용자 문신 제외 — 자동 문신 잎은 제거 허용,
                  // 끝단이라 제거해도 고아가 생기지 않는다)
                  // 잎 후보는 **연결성 검증을 먼저** 통과한 것만 — 상위 N 컷을 먼저 하면 가짜 잎(다리)만
                  // 남아 트라이얼 0건이 된다(실측: 후보 10개인데 상위 3잎 전부 연결성 탈락).
                  // 다리 오판 원인 = 할당-이웃 수 판정이 그래프 밖 연결(클러스터 소켓 경유)을 모름
                  // (실측: 잎 50904 제거 → 하위 트리 고아 → 784 붕괴).
                  // ⚠ 절대 기준(전 노드 도달)으로 검사하면 애초에 그래프상 도달 불가인 할당 노드(전직 등)가
                  // 섞여 있을 때 모든 잎이 탈락한다(실측: 통과 잎 0개). **기준선 대비 상대 비교** —
                  // 제거 전에도 도달 불가였던 노드는 무시하고, 제거로 "새로" 도달 불가가 되는 노드가 없어야 잎.
                  Set<Integer> baselineWithStart = new LinkedHashSet<>(allocated);
                  baselineWithStart.add(classStart);
                  Set<Integer> baselineReach =
                      poeTreeGraphService.reachableFrom(classStart, baselineWithStart);
                  List<Integer> leaves = new ArrayList<>();
                  for (int a : allocated) {
                    PoeTreeGraphService.TreeNode leafNode = poeTreeGraphService.node(a);
                    if (leafNode == null
                        || !"normal".equals(leafNode.type())
                        || userTattoos.containsKey(a)) {
                      continue;
                    }
                    long deg =
                        poeTreeGraphService.neighbors(a).stream()
                            .filter(allocated::contains)
                            .count();
                    if (deg > 1) {
                      continue;
                    }
                    Set<Integer> withStart = new LinkedHashSet<>(allocated);
                    withStart.remove(a);
                    withStart.add(classStart);
                    Set<Integer> reach = poeTreeGraphService.reachableFrom(classStart, withStart);
                    final int removed = a;
                    long newOrphans =
                        baselineReach.stream()
                            .filter(
                                id ->
                                    id != removed && allocated.contains(id) && !reach.contains(id))
                            .count();
                    log(
                        "캡 채움 잎 후보: "
                            + a
                            + " deg="
                            + deg
                            + " 신규고아="
                            + newOrphans
                            + " baseline도달="
                            + baselineReach.size());
                    if (newOrphans == 0) {
                      leaves.add(a);
                    }
                  }
                  leaves.sort(null);
                  log(
                      "캡 채움 진단: 연결성 통과 잎 "
                          + leaves.size()
                          + "개"
                          + (leaves.isEmpty()
                              ? ""
                              : " " + leaves.subList(0, Math.min(3, leaves.size()))));
                  for (int d : danglings.subList(0, Math.min(2, danglings.size()))) {
                    for (int l : leaves.subList(0, Math.min(3, leaves.size()))) {
                      // 잎 제거로 매달린 노드의 유일 연결이 끊기는 조합은 무효
                      if (l == d || poeTreeGraphService.neighbors(d).contains(l)) {
                        continue;
                      }
                      Set<Integer> trialAlloc = new LinkedHashSet<>(allocated);
                      trialAlloc.remove(l);
                      trialAlloc.add(d);
                      Map<Integer, String> trialTattoos = new LinkedHashMap<>(fixedTattoos);
                      trialTattoos.remove(l);
                      trialTattoos.put(d, tattoo.dn());
                      Map<Integer, String> savedTattoos = fixedTattoos;
                      String trialXml;
                      try {
                        fixedTattoos = Map.of();
                        trialXml =
                            withTattoos(
                                buildXml(
                                    gem,
                                    supports,
                                    className,
                                    ascendancy,
                                    ascendancyNodes,
                                    trialAlloc,
                                    items,
                                    jewels),
                                trialTattoos,
                                trialAlloc);
                      } finally {
                        fixedTattoos = savedTattoos;
                      }
                      if (currentAnoint != null) {
                        trialXml = withAnoint(trialXml, currentAnoint.name());
                      }
                      Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
                      evalCount.incrementAndGet();
                      log(
                          "캡 채움 트라이얼: 잎 "
                              + l
                              + "→매달린 "
                              + d
                              + " = "
                              + format(objectiveOf(trialVals, objectiveKey))
                              + " (기준 "
                              + format(objectiveOf(finalValues, objectiveKey))
                              + ", fire "
                              + trialVals.getOrDefault("FireResist", 0d)
                              + ")");
                      if (objectiveOf(trialVals, objectiveKey)
                          > objectiveOf(finalValues, objectiveKey) * 1.003) {
                        allocated.remove(l);
                        allocated.add(d);
                        this.tattooAllocated = Set.copyOf(allocated);
                        fixedTattoos = trialTattoos;
                        finalXml = trialXml;
                        finalValues = trialVals;
                        adopted = true;
                        log(
                            "저항 캡 채움(매달린 소형): 잎 "
                                + l
                                + " 회수 → "
                                + d
                                + " 할당 + "
                                + (tattoo.nameKo() != null ? tattoo.nameKo() : tattoo.dn())
                                + " → "
                                + format(objectiveOf(finalValues, objectiveKey)));
                        break dangling;
                      }
                    }
                  }
                }
              }
            }
            if (!adopted) {
              break;
            }
          }
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
      // 영문 평행 목록 — EN 로케일 화면이 저장된 한글 이름을 그대로 보여주던 것을 해소한다.
      // (결과는 실행 시점에 저장되므로 화면에서 뒤늦게 번역할 수 없다 → 만들 때 둘 다 싣는다)
      List<String> notablesEn = new ArrayList<>();
      List<Integer> notableIds = new ArrayList<>(); // 이름과 평행 — focus 딥링크용
      for (int nodeId : allNodes) {
        PoeTreeGraphService.TreeNode node = poeTreeGraphService.node(nodeId);
        if (node != null && ("notable".equals(node.type()) || "keystone".equals(node.type()))) {
          notables.add(node.nameKo() != null ? node.nameKo() : node.name());
          notablesEn.add(node.name());
          notableIds.add(nodeId);
        }
      }
      // 클러스터 주얼이 얹은 노터블은 트리 그래프에 없어(생성 노드) 위 루프에 안 걸린다 —
      // 목록에서 빠지면 "무슨 노터블을 쓰는 빌드인지"가 결과 화면에서 사라진다.
      // 클러스터 노터블은 **영문 이름**으로 들고 다닌다 — 한글 목록엔 사전(ClusterNotable.nameKo)으로 옮겨 담아야
      // 한국어 화면에 영문이 섞이지 않는다(트리 에디터 팝업은 이미 같은 사전으로 한글화한다).
      Map<String, String> clusterNotableKo = new HashMap<>();
      for (PoeTreeGraphService.ClusterNotable cn : poeTreeGraphService.clusterNotables()) {
        if (cn.nameKo() != null && !cn.nameKo().isBlank()) {
          clusterNotableKo.put(cn.name(), cn.nameKo());
        }
      }
      for (ClusterSpec spec : fixedClusters) {
        for (String cn : spec.notables()) {
          notables.add(clusterNotableKo.getOrDefault(cn, cn));
          notablesEn.add(cn);
          notableIds.add(0); // 생성 노드 — 트리 그래프에 없어 focus 불가
        }
      }
      // 도유 노터블도 배지 목록에 — 포인트 없이 활성인 트리의 일부(결과 링크 an= 과 focus 로 연결)
      if (currentAnoint != null) {
        notables.add("(도유) " + currentAnoint.nameKo());
        notablesEn.add("(Anoint) " + currentAnoint.name());
        notableIds.add(currentAnoint.nodeId());
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
                      support -> {
                        // 속성 보정으로 레벨을 낮춘 젬은 표시에도 드러낸다(표시=실제) — 안 그러면
                        // 사용자는 20레벨 젬 DPS 로 오해한다
                        int base = defaultGemLevel(support);
                        int level = supportLevelOverride.getOrDefault(support.slug(), base);
                        String suffix = level < base ? " (Lv" + level + ")" : "";
                        return new PoeOptimizeResult.SupportPick(
                            support.slug(),
                            support.name() + suffix,
                            support.nameKo() != null ? support.nameKo() + suffix : null);
                      })
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
              // 가드 스킬 그룹 — 채택됐으면 [가드젬, 링크 보조젬] 순. 한글명은 젬 데이터에서 조회.
              guardSkill == null
                  ? List.of()
                  : java.util.stream.Stream.of(guardSkill, guardSupport)
                      .filter(java.util.Objects::nonNull)
                      .map(
                          nm ->
                              // 보조젬은 데이터상 이름이 "... Support" 다(XML nameSpec 은 접미 없이 쓴다) —
                              //   그대로 조회하면 각성한 기원이 slug·한글명 없이 나간다(실측).
                              poeGemDataService
                                  .findByName(nm)
                                  .or(() -> poeGemDataService.findByName(nm + " Support"))
                                  .map(
                                      g ->
                                          new PoeOptimizeResult.SupportPick(
                                              g.slug(), g.name(), g.nameKo()))
                                  .orElseGet(() -> new PoeOptimizeResult.SupportPick("", nm, nm)))
                      .toList(),
              List.copyOf(allNodes),
              notables,
              notablesEn,
              notableIds,
              jewels.values().stream()
                  .map(
                      jewel ->
                          jewel.isUnique()
                              ? new PoeOptimizeResult.SupportPick(
                                  jewel.unique().slug(),
                                  jewel.unique().name(),
                                  jewel.unique().nameKo())
                              // 제작 레어 주얼 — slug 가 없어 상세 링크는 못 걸지만, **붙은 모드는 보여준다**.
                              //   계산에는 jewelLife/jewelFire 같은 접두가 실제로 들어가는데 화면엔 이름만 떠서
                              //   "속성이 표기 안 된다"는 보고가 나왔다(장비 슬롯은 ItemPick 으로 이미 줄까지 보낸다).
                              : new PoeOptimizeResult.SupportPick(
                                  "",
                                  "Crafted Jewel",
                                  "제작 주얼",
                                  rareModLinesKo(jewel.rare()),
                                  rareModLinesEn(jewel.rare())))
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
              // 트리 딥링크(j=)는 slug 기반이라 유니크 주얼만 왕복 가능 — 제작 레어 주얼은 제외.
              jewels.entrySet().stream()
                  .filter(entry -> entry.getValue().isUnique())
                  .map(entry -> entry.getKey() + ":" + entry.getValue().unique().slug())
                  .collect(java.util.stream.Collectors.joining(",")),
              fixedTattoos.entrySet().stream()
                  .map(entry -> entry.getKey() + ":" + entry.getValue())
                  .collect(java.util.stream.Collectors.joining("|")),
              // 최종 XML 에 실린 마스터리 선택 그대로 — 트리 링크가 이걸 잃으면 표시≠실제가 된다
              fixedMasteries.entrySet().stream()
                  .filter(entry -> allocated.contains(entry.getKey()))
                  .map(entry -> entry.getKey() + ":" + entry.getValue())
                  .collect(java.util.stream.Collectors.joining(",")),
              masteryLabels(fixedMasteries, allocated, true),
              masteryLabels(fixedMasteries, allocated, false),
              tattooLabels(fixedTattoos, true),
              tattooLabels(fixedTattoos, false),
              // 트리 링크(an=)로 도유까지 되돌아가게 — 없으면 트리 화면 수치가 결과보다 약하게 나온다
              currentAnoint != null ? currentAnoint.nodeId() : null,
              belowMetaVerdict(objective, gem, ascendancy, finalValues),
              metaRatioOf(gem, ascendancy, finalValues),
              // 추가 스킬 전용 보조젬(1b) — 계산 XML 에는 이미 링크됐는데 결과에 없어 화면에서 안 보였다
              additionalSkillSupports.entrySet().stream()
                  .collect(
                      java.util.stream.Collectors.toMap(
                          Map.Entry::getKey,
                          entry ->
                              entry.getValue().stream()
                                  .map(
                                      s ->
                                          new PoeOptimizeResult.SupportPick(
                                              s.slug(), s.name(), koName(s)))
                                  .toList())));

      Files.createDirectories(resultFile.getParent());
      JsonMapper jsonMapper = JsonMapper.builder().build();
      String resultJson = jsonMapper.writeValueAsString(result);
      Files.writeString(resultFile, resultJson, StandardCharsets.UTF_8);
      this.lastResult = result;
      // 사용자 실행이면 최근 결과 이력에도 남긴다(QA 배터리는 saveHistoryForRun=false 로 제외)
      if (saveHistoryForRun) {
        saveHistory(resultJson);
      }
      // 생존 계수 내역 — "balanced 인데 왜 실빌드보다 물러터졌나"를 로그만으로 판정할 수 있게. 목표치는
      //   해당 아키타입 ninja 실측 중앙값(setSurvivalTargets)이고, s 가 곧 DPS 에 곱해지는 값이다.
      if ("balanced".equals(objective)) {
        double weakest = weakestCommonHit(finalValues);
        log(
            String.format(
                "생존 계수: 최약최대피격 %,.0f / 목표 %,.0f = %.2f · EHP %,.0f / 목표 %,.0f · surv=%.3f (곡률 %.1f)",
                weakest,
                targetMaxHit,
                targetMaxHit > 0 ? weakest / targetMaxHit : 0d,
                finalValues.getOrDefault("TotalEHP", 0d),
                targetEhp,
                balancedSurvival(finalValues),
                SURVIVAL_SHORTFALL_EXP));
      }
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
    } catch (JobCancelledException e) {
      lastStatus = Status.CANCELLED;
      log("중지됨 — 사용자 요청으로 최적화를 취소했습니다");
    } catch (Throwable e) {
      // ⚠ Exception 만 잡으면 안 된다 — Error(예: NoClassDefFoundError, OutOfMemoryError)는 빠져나가
      //    lastStatus/lastResult 가 **직전 잡의 SUCCESS 와 결과 그대로** 남는다. running 은 finally 에서
      //    false 가 되므로 조회하는 쪽은 "성공했고 결과는 이것"으로 읽어 **조용히 남의 결과를 받는다**.
      //    실제로 2026-08-12 에 그 사고가 났다: ehp/ed 잡이 죽었는데 직전 사이클론 dps 값이 반환돼
      //    기준선이 세 줄 똑같이 찍혔다(로그는 자기 잡 것이라 대조로만 발각됐다).
      if (cancelRequested) {
        lastStatus = Status.CANCELLED;
        log("중지됨 — 사용자 요청으로 최적화를 취소했습니다");
      } else {
        lastStatus = Status.FAILED;
        log("실패: " + e);
        logger.warn("PoE 최적화 잡 실패", e);
      }
    } finally {
      running.set(false);
      Thread.interrupted(); // 인터럽트 상태 클리어(다음 잡 오염 방지)
      executor.shutdownNow(); // 취소 시 진행 중 평가 태스크도 즉시 중단

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
    checkCancel(); // 라운드마다 호출되는 지점 — 사용자 중지 시 여기서 조기 종료(페이즈 경계 대기 없이)
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

  /** 밸런스 목표의 생존 하한 (max hit 미제공 시 폴백용 유효 체력 하한) */
  private static final double EHP_FLOOR = 40000d;

  /**
   * 밸런스 목표의 단일 히트 생존 목표 — 흔한 치명 유형(물리+3원소)의 최대 피격이 이 값 미만이면 원샷 위험으로 DPS 점수 감쇠. 아키타입
   * 무관(생명·ES·방어도·블록·최대저항 무엇으로 올리든 이 값이 오른다).
   */
  /** 완성 단계 생존 래칫 on/off — 기본 off(동작 무변화). */
  private static final boolean SURV_RATCHET_ENABLED =
      "on"
          .equalsIgnoreCase(
              System.getProperty(
                  "poe.survRatchet", System.getenv().getOrDefault("POE_SURV_RATCHET", "off")));

  /**
   * EHP 2차 항 + EHP 시드 존중 — **기본 on**(벤치 갱신 후 승격). 끄려면 POE_EHP_TERM=off.
   *
   * <p>승격 근거(갱신 벤치 기준 깨끗한 A/B, 7종): 영향받는 축은 둘뿐이고 나머지 5축은 완전 동일. 회오리 3,629,914→3,216,209(−11.4%)에
   * EHP 12,666→19,155(+51%), 번개 화살 6,959,002→6,549,649(−5.9%)에 EHP 14,767→27,307(+85%). 두 축 모두 벤치
   * 대비 204%/199% 라 여유가 크고, EHP 는 실빌드 중앙값 (23,000 / 25,000)에 근접할 뿐 과잉이 아니다. 보류했던 유일한 근거("번개 화살이 벤치
   * 96%→90%")는 벤치 갱신으로 소멸했다(현재 199%).
   */
  private static final boolean EHP_TERM_ENABLED =
      "on"
          .equalsIgnoreCase(
              System.getProperty(
                  "poe.ehpTerm", System.getenv().getOrDefault("POE_EHP_TERM", "on")));

  /** EHP 2차 항의 가중(하한은 1-가중/2). 0.3 은 혼의 균열 −13.7% 로 과했고 0.15 가 무릎점(실측). */
  private static final double EHP_TERM_WEIGHT =
      Double.parseDouble(
          System.getProperty(
              "poe.ehpWeight", System.getenv().getOrDefault("POE_EHP_WEIGHT", "0.15")));

  /** EHP 시드 존중 시 절대 바닥. */
  private static final double EHP_SEED_FLOOR =
      Double.parseDouble(
          System.getProperty(
              "poe.ehpSeedFloor", System.getenv().getOrDefault("POE_EHP_SEED_FLOOR", "8000")));

  /** 시드(아키타입 실측)가 있을 때의 절대 바닥 — 이 아래로는 어떤 메타라도 원샷이라 허용하지 않는다. */
  private static final double MAXHIT_SEED_FLOOR =
      Double.parseDouble(
          System.getProperty(
              "poe.maxhitSeedFloor",
              System.getenv().getOrDefault("POE_MAXHIT_SEED_FLOOR", "5000")));

  private static final double MAXHIT_FLOOR =
      Double.parseDouble(
          System.getProperty(
              "poe.maxhitFloor", System.getenv().getOrDefault("POE_MAXHIT_FLOOR", "15000")));

  /** 카오스는 드문 위협이라 별도(더 낮은) 목표 + 약한 2차 가중 — 과투자 방지. */
  private static final double CHAOS_MAXHIT_FLOOR = 8000d;

  /** ES 스태킹 아키타입 판정의 ES 중앙값 하한 — 이 미만이면 ES 가 주 방어풀이라 보기 어려움(생명 빌드의 부수 ES 배제). */
  private static final double ES_ARCHETYPE_FLOOR = 8000d;

  /**
   * greedy 점수 — objective 별:
   *
   * <ul>
   *   <li>dps: CombinedDPS
   *   <li>ehp: TotalEHP
   *   <li>balanced: CombinedDPS × min(1, EHP/하한) — 유리대포 방지(생존 확보 전엔 DPS 가치 낮춤)
   * </ul>
   */
  /** 유니크 최종 재대결 — 슬롯당 다시 볼 후보 수(키워드 점수 상위). */
  private static final int UNIQUE_REMATCH_PER_SLOT = 10;

  /** 유니크 최종 재대결 라운드 상한 — 한 슬롯 교체가 문맥을 바꿔 다음 라운드 결과도 달라진다. */
  private static final int UNIQUE_REMATCH_ROUNDS = 3;

  /**
   * 랭킹 전용 문맥을 켜는 기준값 — 젬 단독 평가가 이 값보다 작으면 "잴 수 없는 상태"로 본다.
   *
   * <p>활·덫처럼 장비/트리가 있어야 피해가 생기는 계열은 초반 값이 한 자릿수라 후보 순위가 노이즈가 된다.
   */
  private static final double RANKING_CONTEXT_BASELINE = 1000d;

  /** 클러스터 적재물 재선정 시 볼 노터블 후보 수 — 승자 스킬키에 한해 실측으로 교체해 본다. */
  private static final int CLUSTER_NOTABLE_POOL = 6;

  /** 트리 재대결 후보 수 — 완성 문맥에서 다시 볼 미할당 노터블 상위 N(키워드 점수순). */
  private static final int TREE_REMATCH_CANDIDATES = 14;

  /** 트리 재대결 라운드 상한 — 한 번 채택하면 문맥이 바뀌어 다음 후보의 값도 달라진다. */
  private static final int TREE_REMATCH_ROUNDS = 4;

  /**
   * 포인트를 마련하려 뗄 **잎**을 고른다(못 고르면 null).
   *
   * <p>차수는 반드시 <b>교환 후 평가할 트리</b>(trial) 기준으로, 그리고 <b>직업 시작 노드를 포함</b>해서 세야 한다. 시작 노드는 allocated 에
   * 없어서 빼먹기 쉬운데, 빼먹으면 시작에 붙은 첫 노드가 차수 1(=잎)로 보여 제거 대상이 되고 그 순간 트리 전체가 시작점과 끊긴다(실측: 클러스터 스왑에서 끊김
   * 115/123, 교환 후 953 → 붕괴를 손익으로 오인해 클러스터가 영원히 기각됐다).
   *
   * <p>주얼 소켓·키스톤·마스터리는 떼는 순간 빌드 기제가 무너지므로 제외하고, 이번에 새로 얹은 경로(protect)도 보호한다.
   */
  private Set<Integer> removableLeaves(
      Set<Integer> trial,
      Set<Integer> allocated,
      int need,
      Integer startNode,
      List<String> keywords,
      List<Integer> protectedPath,
      Integer protectedTarget) {
    Set<Integer> removable = new LinkedHashSet<>();
    Set<Integer> pool = new HashSet<>(trial);
    if (startNode != null) {
      pool.add(startNode);
    }
    Set<Integer> keep = new HashSet<>();
    if (protectedPath != null) {
      keep.addAll(protectedPath);
    }
    if (protectedTarget != null) {
      keep.add(protectedTarget);
    }
    while (removable.size() < need) {
      Integer pick = null;
      int pickScore = Integer.MAX_VALUE;
      for (int id : pool) {
        if (id >= 0x10000 || !allocated.contains(id) || keep.contains(id)) {
          continue; // 가상 노드 · 새로 얹은 경로 · 시작 노드는 대상 아님
        }
        int degree = 0;
        for (int nb : poeTreeGraphService.neighbors(id)) {
          if (pool.contains(nb)) {
            degree++;
          }
        }
        if (degree > 1) {
          continue; // 잎만 — 중간 노드 제거는 연결성을 깬다
        }
        PoeTreeGraphService.TreeNode treeNode = poeTreeGraphService.node(id);
        if (treeNode != null
            && ("jewel".equals(treeNode.type())
                || "keystone".equals(treeNode.type())
                || "mastery".equals(treeNode.type()))) {
          continue;
        }
        int sc = treeNode == null ? 0 : score(treeNode.stats(), keywords);
        if (sc < pickScore || (sc == pickScore && (pick == null || id < pick))) {
          pickScore = sc;
          pick = id;
        }
      }
      if (pick == null) {
        return null; // 더 뗄 잎이 없음
      }
      removable.add(pick);
      pool.remove(pick);
    }
    return removable;
  }

  /** 금단 페어 후보 노터블 수 — 키워드 점수 상위 N 만 엔진으로 재 본다(1개당 엔진 1회). */
  private static final int FORBIDDEN_CANDIDATES = 8;

  /**
   * 금단의 화염/살점 한 짝을 합성한다. PoB 는 두 아이템의 문구가 **같은 노터블**을 가리킬 때만 그 노터블을 켠다(side=flame/flesh).
   *
   * @param flame true 면 화염(Crimson), false 면 살점(Cobalt)
   */
  private PoeUniqueItem forbiddenJewel(String notable, boolean flame, String className) {
    String other = flame ? "Forbidden Flesh" : "Forbidden Flame";
    return new PoeUniqueItem(
        flame ? "Forbidden Flame" : "Forbidden Flesh",
        flame ? "금단의 화염" : "금단의 살점",
        flame ? "forbidden-flame" : "forbidden-flesh",
        flame ? "Crimson Jewel" : "Cobalt Jewel",
        flame ? "진홍색 주얼" : "코발트색 주얼",
        "jewel",
        68,
        null,
        false,
        null,
        List.of(),
        List.of(),
        // "Limited to:"/"Requires Class" 는 원래 아이템 속성 줄이지만, PoB 는 모드 블록에 있어도 무해하게 넘긴다.
        // 진짜 필수 조건은 **같은 직업의 전직 노터블**이라는 것 — 타 직업 노터블은 엔진이 조용히 무시한다
        // (실측: 타 직업 후보 8개가 전부 동일값 658,704 → 같은 직업으로 바꾸자 학살의 위상 914,736).
        List.of(
            "Limited to: 1",
            "Requires Class " + className,
            "Allocates " + notable + " if you have the matching modifier on " + other),
        List.of("금단의 " + (flame ? "살점" : "화염") + "에 대응하는 모드가 있으면 " + notable + " 할당"),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** 감시자의 눈 합성 시 검토할 모드 수(키워드 점수 상위). */
  private static final int WATCHERS_EYE_MOD_CANDIDATES = 6;

  /** 감시자의 눈 합성 — 프리즘 주얼 + 오라 조건부 모드 2개(인게임 통상 롤). */
  private PoeUniqueItem watchersEyeJewel(List<WatchersEyeMod> mods) {
    return new PoeUniqueItem(
        "Watcher's Eye",
        "감시자의 눈",
        "watchers-eye",
        "Prismatic Jewel",
        "무지개색 주얼",
        "jewel",
        1,
        null,
        false,
        null,
        List.of(
            "(4-6)% increased maximum Energy Shield",
            "(4-6)% increased maximum Life", "(4-6)% increased maximum Mana"),
        List.of("최대 에너지 보호막 (4-6)% 증가", "최대 생명력 (4-6)% 증가", "최대 마나 (4-6)% 증가"),
        mods.stream().map(WatchersEyeMod::en).toList(),
        mods.stream().map(WatchersEyeMod::ko).toList(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private double objectiveOf(Map<String, Double> values, String objective) {
    double factor = feasibilitySteering ? feasibilityFactor(values) : 1.0;
    // #1 정의의 화염류(RF) 지속력 게이트 — 자기 불에 타 죽는(순생명재생<0) 빌드를 선택 지표에서 감쇠.
    //    RF 외(selfBurnRun=false) 또는 NetLifeRegen 부재면 1.0 → 다른 스킬 기준선 불변.
    factor *= sustainFactor(values);
    if ("ehp".equals(objective)) {
      return values.getOrDefault("TotalEHP", 0d) * factor;
    }
    double dps = effectiveDps(values);
    if ("balanced".equals(objective)) {
      // 밸런스 = "실제로 쓸 수 있는 최상의 빌드". 생존을 문턱이 아니라 **연속 가치**로 평가한다.
      //   dps/ehp 분기는 무변경 → 그쪽 기준선 불변.
      double surv = balancedSurvival(values);
      // RF 등 자가연소(selfBurnRun): 실빌드 총생명재생(targetLifeRegen, ninja 중앙값)까지 총재생을 유도한다.
      //   기존 sustainFactor 는 순재생<0(자멸)만 감쇠 → 순재생 0(경계선·지속불가)에 안주하던 것을 교정
      //   (사용자 지적: 실빌드 재생 2666인데 시뮬 0). balanced·자가연소 한정이라 dps/ehp 및 비-RF 기준선 불변.
      if (selfBurnRun && targetLifeRegen > 0) {
        double regen = values.getOrDefault("LifeRegen", 0d);
        double r = Math.max(0d, Math.min(1d, regen / targetLifeRegen));
        // P2 시도: 0.25+0.75r 로 강화했더니 초기 저재생 구간에서 팩터가 전 후보를 짓눌러 탐색이 왜곡,
        //   전 축 하락(730k→580k, 카오스 게이트 무관 — 강도 2종에서 동일 결과로 분리 확인) → 0.5+0.5r 유지.
        surv *= 0.5 + 0.5 * r; // 재생 0 → ×0.5(강한 유도), 목표 도달 → ×1.0
      }
      return dps * surv * factor;
    }
    return dps * factor;
  }

  /**
   * 밸런스 목표의 연속 생존 계수 — "DPS + 생존" 에서 생존을 문턱값이 아니라 연속 가치로 본다.
   *
   * <ol>
   *   <li>EHP: 문턱(EHP_FLOOR) 미만은 선형 급감(유리대포 방지), 초과분은 sqrt 완만 보상(최대 +20%, 탱킹 폭주 방지).
   *   <li>원소 저항 캡(75) 미달: 미달 1%당 2% 감쇠(하한 0.2) — 인게임 생존 필수.
   *   <li>자가연소 지속 불가(순생명재생<0): near-hard 배제(하한 0.02). main/보조 무관(NetLifeRegen 직접 판정)이라 Fire Trap 이
   *       main 이고 RF 가 보조여도 걸린다. RF 는 net>=0 도달에 화염 최대저항(90)·생명재생이 필요하므로 이 게이트가 화염저항·재생·치프틴 저항전환
   *       노드(greedyAscendancy)를 자동으로 끌어올린다.
   * </ol>
   *
   * <p>dps/ehp 목표엔 적용 안 함(dps=유리대포 허용, ehp 기준선 보존).
   */
  /** 흔한 치명 유형(물리+3원소) 최대 피격의 최솟값 — 실질 생존을 결정하는 값. 없으면 0. */
  private static double weakestCommonHit(Map<String, Double> values) {
    double weakest = Double.MAX_VALUE;
    for (String key :
        new String[] {
          "PhysicalMaximumHitTaken", "FireMaximumHitTaken",
          "ColdMaximumHitTaken", "LightningMaximumHitTaken"
        }) {
      Double v = values.get(key);
      if (v != null && v > 0d) {
        weakest = Math.min(weakest, v);
      }
    }
    return weakest == Double.MAX_VALUE ? 0d : weakest;
  }

  /** 방어 재대결의 방어 레어 후보 on/off — 기여도 귀속(A/B) 용. 기본 on. */
  private static final boolean DEF_RARE_ENABLED =
      !"off"
          .equalsIgnoreCase(
              System.getProperty(
                  "poe.defRare", System.getenv().getOrDefault("POE_DEF_RARE", "on")));

  /** 가드 스킬 단계 on/off — 기여도 귀속(A/B) 용. 기본 on. */
  private static final boolean GUARD_ENABLED =
      !"off"
          .equalsIgnoreCase(
              System.getProperty("poe.guard", System.getenv().getOrDefault("POE_GUARD", "on")));

  /** 생존 미달 벌점의 곡률(1.0=선형·기존). 실험용으로만 환경변수/시스템 프로퍼티로 올린다. */
  private static final double SURVIVAL_SHORTFALL_EXP =
      Double.parseDouble(
          System.getProperty(
              "poe.survivalExp", System.getenv().getOrDefault("POE_SURVIVAL_EXP", "1.0")));

  private double balancedSurvival(Map<String, Double> values) {
    // (1) 결과-기반 아키타입-무관 한 방 생존 — PoB 가 계산한 유형별 "최대 피격(단일 히트)". 생명·ES·방어도·블록·최대저항
    //   무엇을 쌓든 그 효과가 이 값에 반영되므로, 스탯(생명/ES 등)을 하드코딩할 필요가 없다. 흔한 치명 유형(물리+3원소)의
    //   **최솟값**이 실질 생존을 결정(약한 타입에 죽는다) — 이 하나로 저항 캡·물리 감소·유효 풀(저생명 배제)이 자동 유도됨.
    //   ⚠ 카오스를 이 최솟값에 넣으면 카오스(올리기 어려움)가 지배해 DPS 과희생(실측) → 카오스는 아래 (2)에서 약한 2차로만.
    double weakestCommon = Double.MAX_VALUE;
    for (String key :
        new String[] {
          "PhysicalMaximumHitTaken",
          "FireMaximumHitTaken",
          "ColdMaximumHitTaken",
          "LightningMaximumHitTaken"
        }) {
      Double v = values.get(key);
      if (v != null && v > 0d) {
        weakestCommon = Math.min(weakestCommon, v);
      }
    }
    // 목표치(maxhitTarget/ehpTarget) = poe.ninja 아키타입 실측 중앙값(setSurvivalTargets). 매칭 없으면 정적 floor.
    double maxhitTarget = targetMaxHit;
    double ehpTarget = targetEhp;
    double s;
    if (weakestCommon != Double.MAX_VALUE) {
      // 미달 벌점의 곡률 — 선형(지수 1)이면 "생존 1% 손해 ↔ DPS 1% 이득"이 등가라, DPS 가 28배까지 벌리는
      //   아키타입에서는 생존이 사실상 무시된다(실측: 저거넛 뼈 박살 balanced 가 최약 최대피격 10k vs 실빌드 32k,
      //   EHP 20k vs 92k 인데 DPS 는 28배). 지수>1 이면 목표 미달이 초선형으로 아파 실빌드 수준으로 끌어올린다.
      //   기본 1.0 = 기존 동작 그대로(기준선 불변). 실험은 POE_SURVIVAL_EXP 로만 켠다.
      s =
          weakestCommon < maxhitTarget
              ? Math.pow(
                  weakestCommon / maxhitTarget, convexSurvivalPhase ? SURVIVAL_SHORTFALL_EXP : 1.0)
              : 1.0 + 0.2 * Math.min(1.0, Math.sqrt(weakestCommon / maxhitTarget) - 1.0);
    } else {
      // max hit 미제공 시 EHP 폴백(아키타입-무관 집계값).
      double ehp = values.getOrDefault("TotalEHP", 0d);
      s =
          ehp < ehpTarget
              ? ehp / ehpTarget
              : 1.0 + 0.2 * Math.min(1.0, Math.sqrt(ehp / ehpTarget) - 1.0);
    }
    // (2a) 생존 래칫 — **완성 단계(convexSurvivalPhase)** 에서만, 목표를 이미 넘긴 빌드가 목표 아래로
    //   내려가는 교환을 근사-배제한다(RF 지속 게이트와 같은 형태). 선형 벌점만으로는 "DPS 이득률 > 생존
    //   손실률" 이면 무조건 채택돼, 도유 단계가 생명 노드(활력)를 공격 노드(복수)로 바꾸며 뼈 박살 최약
    //   최대피격을 34,281(0.88) → 29,043(0.74) 로 떨어뜨렸다(실측). 탐색 구간에는 걸지 않는다 —
    //   중간 빌드는 늘 목표 미달이라 경로가 비틀린다(볼록 벌점 2판 실패의 교훈).
    if (SURV_RATCHET_ENABLED
        && convexSurvivalPhase
        && weakestCommon != Double.MAX_VALUE
        && weakestCommon < maxhitTarget) {
      s *= 0.05;
    }
    // (2) 카오스 — 드문 위협이라 약한 2차 가중. 심하게 낮을 때만 완만 감쇠(하한 0.6, 과투자 방지).
    double chaos = values.getOrDefault("ChaosMaximumHitTaken", 0d);
    if (chaos > 0d && chaos < CHAOS_MAXHIT_FLOOR) {
      s *= Math.max(0.6, chaos / CHAOS_MAXHIT_FLOOR);
    }
    // (2e) EHP 목표 — 최약최대피격은 **단일 히트** 생존이고 EHP 는 **연속 피격** 버팀이라, 한쪽만 맞추면
    //   실빌드와 프로파일이 갈린다(실측: 번개 화살 최약 7,312 로 실메타 7,500 에 맞췄는데 EHP 는 14,767 vs
    //   실빌드 중앙값 25,000 = 0.59배). 다른 2차 항들과 같은 약한 형태 — 미달 비율에 비례한 완만 감쇠(하한 0.85).
    if (EHP_TERM_ENABLED && ehpTarget > 0 && weakestCommon != Double.MAX_VALUE) {
      double ehpNow = values.getOrDefault("TotalEHP", 0d);
      if (ehpNow < ehpTarget) {
        s *=
            Math.max(1.0 - EHP_TERM_WEIGHT / 2, 1.0 - EHP_TERM_WEIGHT * (1.0 - ehpNow / ehpTarget));
      }
    }
    // (2b) 원소 저항 캡 — 기본 75(전 빌드 공통)이나, 아키타입 실측(치프틴 RF 등 최대저항 특화)은 목표가 90.
    //   목표 미달 1%당 2.5% 감쇠(하한 0.15). 목표를 아키타입 seed 로 잡아 90 특화 빌드가 75 에서 멈추지 않게 한다.
    int[] resTargets = {targetFireRes, targetColdRes, targetLightRes};
    String[] resKeys = {"FireResist", "ColdResist", "LightningResist"};
    for (int i = 0; i < resKeys.length; i++) {
      double target = resTargets[i];
      double r = values.getOrDefault(resKeys[i], target);
      if (r < target) {
        s *= Math.max(0.15, 1.0 - (target - r) * 0.025);
      }
    }
    // (2c) P2 카오스 저항 목표 — 실빌드 중앙값(캡 75) 미달 1%당 0.4% 감쇠(하한 0.8).
    //   (2)의 카오스 최대피격 2차 가중만으론 26% 방치(실측) — 명시 목표로 실빌드 파리티 유도.
    //   ⚠ 1%/pt·하한 0.5 로 했더니 초기 트리 탐색까지 지배해 전 축 하락(730k→580k, 생명 10.6k→7.1k 실측) —
    //   카오스저항은 주로 장비 접미어에서 오므로 아이템 단계를 조향할 약한 신호면 충분하다.
    //   targetChaosRes=0(시드 없음/비-balanced)이면 무변경.
    if (targetChaosRes > 0) {
      double cr = values.getOrDefault("ChaosResist", (double) targetChaosRes);
      // CI(카오스 접종) 빌드는 카오스 피해 면역인데 PoB ChaosResist 값은 원시 저항(-5 등)을 그대로 보고한다 —
      // 생명 1 이 CI 의 시그니처. 면역인데 저항 미달 감쇠(최대 ×0.8)를 적용하면 ES/CI 대안이 부당 감점된다.
      boolean chaosImmune = values.getOrDefault("Life", 0d) <= 1d;
      if (!chaosImmune && cr < targetChaosRes) {
        s *= Math.max(0.8, 1.0 - (targetChaosRes - cr) * 0.004);
      }
    }
    // (2d) 주문 억제 목표 — 억제 특화 아키타입(중앙값 60%+)만. 억제 100% = 주문 피해 절반인데 EHP 연속
    //   팩터만으론 DPS 유니크에 밀려 장비 접미(spellSuppress)·트리가 조향되지 않는다(실측: Penance Brand
    //   벤치 억제 100/EHP 107k ↔ 우리 0/21k). 카오스 게이트와 같은 약한 감쇠(0.4%/pt·하한 0.8).
    if (targetSpellSuppress > 0) {
      double suppress = values.getOrDefault("SpellSuppressionChance", 0d);
      if (suppress < targetSpellSuppress) {
        s *= Math.max(0.8, 1.0 - (targetSpellSuppress - suppress) * 0.004);
      }
    }
    // (2e) 주문 막기 목표는 **도입 실패로 롤백**(2026-08-04) — 막기 75%는 현 레버(방패 접미·플라스크·마스터리)로
    //   그리디 중 도달 불가라 게이트가 상시 하한 근처에서 헛돌며 Rumi's 류 막기 추격으로 실생존을 내다버림
    //   (실측: Penance Brand EHP 45,677→8,134 붕괴). 카오스 게이트가 통한 건 접미어로 75 도달이 싸기 때문.
    //   막기 레이어는 목표 게이트가 아니라 **막기 소스 자체**(Aegis/방패 유니크 후보·Glancing Blows)를 후보에
    //   넣는 구조적 접근이 필요하다. targetSpellBlock 시드는 남겨 두되(로그·진단용) 감쇠는 걸지 않는다.
    // (3) 지속 — 단일 히트가 아니라 도트/자가연소(RF) 지속 사망은 max hit 로 안 잡힌다. 순생명재생<0 near-hard 배제.
    //   main/보조 무관(NetLifeRegen 직접) → Fire Trap main + RF 보조도 걸림.
    Double net = values.get("NetLifeRegen");
    if (net != null && net < 0d) {
      double life = Math.max(1.0, values.getOrDefault("Life", 1d));
      s *= Math.max(0.02, 1.0 / (1.0 + (-net / life) * 120.0));
    }
    return s;
  }

  /**
   * DPS 대표값 — 플레이어 CombinedDPS 가 0 이면 FullDPS 로 폴백.
   *
   * <p>미니언/토템/덫/기뢰 등 **플레이어가 직접 때리지 않는** 빌드는 PoB 의 player CombinedDPS 가 0 이고 실제 피해는
   * FullDPS(calcFullDPS 로 미니언/토템 포함 집계, Calcs.lua:81)에 담긴다. CombinedDPS>0 인 일반 빌드는 그대로라
   * arc/cyclone/ED 기준선 불변. 이 폴백이 없으면 미니언 스킬이 DPS 0 으로 나와 최적화기가 아무것도 못 골랐다(SRS 실측 0).
   */
  /**
   * 실빌드 중앙값 대비 배수 — 우리 DPS ÷ 그 아키타입의 poe.ninja 중앙 DPS. 시드가 없으면 null.
   *
   * <p>우리 값은 만렙·전 슬롯 최상위 레어·전 버프 가정이라 중앙값보다 높은 게 정상이다(실측 1.1~2.3x). 다만 배수가 크게 튀는 아키타입은 그 가정이 특히
   * 유리하게 맞아떨어진 경우라(혼의 균열 9.5x), 숫자만 보면 오해한다 — 화면이 그 사실을 같이 보여줄 수 있게 값을 내보낸다.
   */
  private Double metaRatioOf(PoeGem gem, String ascendancy, Map<String, Double> finalValues) {
    if (ascendancy == null || ascendancy.isEmpty() || gem == null) {
      return null;
    }
    ArchetypeBenchmark bench = ninjaBenchByKey.get(ascendancy + "|" + gem.name());
    if (bench == null || bench.dps() <= 0) {
      return null;
    }
    double myDps = effectiveDps(finalValues);
    return myDps > 0 ? Math.round(myDps / bench.dps() * 100d) / 100d : null;
  }

  /** 원소 태그가 없는 공격 스킬의 피해 축 후보 — 물리(현행) + 3원소. 축마다 기준 무기 1회 실측. */
  private static final List<String> DAMAGE_AXES = List.of("physical", "lightning", "cold", "fire");

  /**
   * 피해 축 선택 — 원소 태그가 없는 공격 스킬에만 적용한다.
   *
   * <p>축을 키워드에 넣으면 그 축으로 레어가 크래프트되고 트리·보조젬 평가도 그 축을 따른다. 어느 축이 센지는 게임 데이터가 아니라 **엔진 실측**이 정한다(각 축의
   * 기준 무기를 낀 상태로 젬 단독 평가). 축이 이미 정해진 스킬(번개 화살 등)은 그대로 둔다.
   */
  private List<String> pickDamageAxis(PoeGem gem, String objective, List<String> keywords) {
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    boolean attack = tags.contains("Attack");
    boolean hasElement =
        tags.contains("Fire")
            || tags.contains("Cold")
            || tags.contains("Lightning")
            || tags.contains("Chaos");
    if (!attack || hasElement || "ehp".equals(objective)) {
      return keywords;
    }
    String bestAxis = null;
    double bestVal = -1;
    for (String axis : DAMAGE_AXES) {
      List<String> trial = new ArrayList<>(keywords);
      if (!trial.contains(axis)) {
        trial.add(axis);
      }
      RareItem weapon = craftRare(Slot.WEAPON, gem, trial, 0.0);
      if (weapon == null) {
        continue;
      }
      Map<Slot, Equipped> probe = new EnumMap<>(Slot.class);
      probe.put(Slot.WEAPON, Equipped.ofRare(weapon));
      double val =
          objectiveOf(
              poePobEngineService.calculateValues(
                  buildXml(gem, List.of(), classFor(gem), null, Set.of(), Set.of(), probe)),
              "dps");
      evalCount.incrementAndGet();
      log("피해 축 후보: " + axis + " → " + format(val));
      if (val > bestVal) {
        bestVal = val;
        bestAxis = axis;
      }
    }
    if (bestAxis == null) {
      return keywords;
    }
    List<String> chosen = new ArrayList<>(keywords);
    if (!chosen.contains(bestAxis)) {
      chosen.add(bestAxis);
    }
    log("피해 축 확정: " + bestAxis + " (원소 태그 없는 공격 스킬)");
    return chosen;
  }

  /**
   * 구조형 주얼 판별 — 문구에 피해 낱말이 없어도 빌드 구조를 바꿔 값을 내는 것들.
   *
   * <p>고정 목록(이름 나열) 대신 <b>문구 패턴</b>으로 판별한다: 목록은 리그마다 늙고, 새로 나온 주얼을 놓친다.
   */
  private static final java.util.regex.Pattern STRUCTURAL_JEWEL =
      java.util.regex.Pattern.compile(
          "(?i)(Adds? [0-9]+ Passive Skill|Jewel Socket|Allocates? |in Radius|Passives? in Radius"
              + "|instead of|are Transformed|count as|have no|Notable Passive Skill)");

  private boolean isStructuralJewel(PoeUniqueItem item) {
    for (String line : item.explicits() == null ? List.<String>of() : item.explicits()) {
      if (STRUCTURAL_JEWEL.matcher(line).find()) {
        return true;
      }
    }
    return false;
  }

  private double effectiveDps(Map<String, Double> values) {
    double combined = values.getOrDefault("CombinedDPS", 0d);
    double full = values.getOrDefault("FullDPS", 0d);
    // #235 미니언 잡만: max(Combined, Full)=미니언수 합산 총합(ninja 총합 표기 정합). FullDPS 는 미니언 그룹에만
    //   조건부로 켠 includeInFullDPS XML + PoB calcFullDPS 로 count-정확 집계됨(#230 은 이 XML 없이 시도해 no-op).
    //   multiActorBuild=false(그 외 전부)면 아래 기존 경로 → 단일 액터 기준선(arc/cyclone/ED/RF) 구조적 불변.
    // ⚠ 토템 제외: calcFullDPS 가 토템은 count 집계 안 해 FullDPS 기반 탐색이 AW 3,590,847→2,692,513 회귀시킴(실측).
    // ⚠ 과거 max 를 **전역** 적용했다 RF 이탈 → 미니언 잡으로만 한정.
    if (multiActorBuild) {
      return Math.max(combined, full);
    }
    // CombinedDPS 우선, 0 일 때만 FullDPS 폴백(미니언 등 별도 actor, #218).
    return combined > 0 ? combined : full;
  }

  /**
   * 자가연소(정의의 화염류) 지속력 게이트 — 순생명재생(NetLifeRegen)이 음수면 제 불에 타 죽는 빌드라 선택 지표를 감쇠. 결손 비율(-net/생명)에 하한 없는
   * 연속 감쇠(1/(1+결손×10)) — greedy 가 재생을 늘려 순재생을 0 쪽으로 올리게 유도. RF 외(selfBurnRun=false)·NetLifeRegen
   * 부재·net>=0 이면 1.0(불변).
   */
  private double sustainFactor(Map<String, Double> values) {
    if (!selfBurnRun) {
      return 1.0;
    }
    Double net = values.get("NetLifeRegen");
    if (net == null || net >= 0) {
      return 1.0;
    }
    double life = Math.max(1.0, values.getOrDefault("Life", 1d));
    double deficitRatio = -net / life;
    return 1.0 / (1.0 + deficitRatio * 10.0);
  }

  /**
   * 요구 속성(장비+젬 총계) 부족 페널티 — 부족 1당 0.5% 감쇠(하한 ×0.1).
   *
   * <p>greedy 가 "요구치 성립 불가 조합"(공허 충전기 + 각성 젬 스택으로 민첩/힘 150+ 요구 등)을 애초에 피하게 하는 조향 신호다. 이게 없으면 지표가
   * 요구치를 공짜로 보고 불법 조합을 골랐다가, 사후 강등 사다리가 젬을 갉아먹어 DPS 42M→2.3M 로 무너지는 결말이 났다(실측). 기울기를 완만하게 둔 이유:
   * 초반(트리/장비 미완) 일시 부족은 용인하고, **대안이 비슷할 때만** 실현 가능한 쪽으로 기울게. 선택 지표에만 곱하고 표시(displayMetric)에는 섞지
   * 않는다.
   */
  private static double feasibilityFactor(Map<String, Double> values) {
    double shortfall = 0;
    shortfall += Math.max(0, values.getOrDefault("ReqStr", 0d) - values.getOrDefault("Str", 0d));
    shortfall += Math.max(0, values.getOrDefault("ReqDex", 0d) - values.getOrDefault("Dex", 0d));
    shortfall += Math.max(0, values.getOrDefault("ReqInt", 0d) - values.getOrDefault("Int", 0d));
    if (shortfall <= 0) {
      return 1.0;
    }
    return Math.max(0.1, 1.0 - shortfall * 0.005);
  }

  /** 표시용 대표 수치 — ehp 는 유효 체력, 그 외(dps/balanced)는 DPS (혼합점수 대신 실제 값) */
  private double displayMetric(Map<String, Double> values, String objective) {
    return "ehp".equals(objective) ? values.getOrDefault("TotalEHP", 0d) : effectiveDps(values);
  }

  /**
   * 이번 시즌 실제 제공되는 보조젬인지. 각성한(Awakened) 보조젬은 더 이상 제공되지 않고, 특출난(Exceptional: 향상/강화/계몽) 계열만 각성판이 남는다.
   * 즉 이름이 "Awakened" 로 시작하면서 Exceptional 태그가 없는 젬(각성 화염 추가 등)은 후보에서 제외한다.
   */
  /**
   * 트리거 보조젬 전면 제외 — 우리 최적화기는 트리거 발동률(별도 트리거 소스/조건)을 모델하지 않아, 트리거 서포트가 메인 딜링 그룹에서 내는 이득은 전부 PoB 조건
   * 무시 왜곡이다(실측: PB 에서 Cast on Death 제거 시 13.85M→4.20M = DPS 70%가 왜곡분, Cast on Melee Kill 도 근접 공격 없는
   * 주문 빌드에서 +37% — 둘 다 실전 발동 불가). 태그 "Trigger" 기반 일반화, 태그 누락 대비 이름 3종은 이중 방어.
   */
  private static final Set<String> PASSIVE_TRIGGER_SUPPORTS =
      Set.of("Cast on Death", "Cast when Stunned", "Cast when Damage Taken");

  private boolean isProvidedSupport(PoeGem support) {
    String name = support.name();
    if (name == null) {
      return true;
    }
    List<String> tags = support.tags();
    if (tags != null && tags.contains("Trigger")) {
      return false;
    }
    if (PASSIVE_TRIGGER_SUPPORTS.contains(name.replaceFirst(" Support$", ""))) {
      return false;
    }
    if (!name.startsWith("Awakened")) {
      return true;
    }
    return tags != null && tags.contains("Exceptional");
  }

  /**
   * 하드 아키타입 태그 — 보조젬이 가졌는데 메인 스킬이 없으면 PoB 가 효과를 적용하지 않는다(평가 낭비).
   *
   * <p>⚠ #215서 Totem/Trap/Mine(변환 보조젬)을 이 필터서 빼봤으나 **RF 기준선 회귀**(1,313,292→539,789, netliferegen
   * 미표면화 = RF 가 토템으로 변환돼 자가연소 소실 → 지속게이트 무력화 → 다른 빌드 수렴)를 유발했고, #217서 확인했듯 greedy 트리단계 한계로 토템 채택
   * 이득도 0(arc/hiero 자가시전 유지). = 이득 0 + RF 손상 → **전부 필터 유지(원복)**. 토템/덫/기뢰 발견은 아키타입 인지 최적화(대규모)로만
   * 가능하며 이 필터 완화로는 안 됨.
   */
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
        // 바알 젬은 브라우저 전용 — 소울/지속 업타임을 모델링하지 않아 DPS가 왜곡되므로 자동 스킬 후보에서 제외
        .filter(g -> !g.isVaal())
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
    // 참고(#220 롤백): 미니언 스킬 키워드를 "minion" 만으로 좁혀봤으나 SRS 188,782→181,812 로 오히려 하락 —
    //   기존 광의 키워드가 유효 노드를 이미 잡고 있었다. 미니언 magnitude 병목은 트리 키워드가 아니라 다른 곳
    //   (미니언 오라 Anger/Wrath 미투입, 미니언 젬 레벨/생명 등)으로 추정. 광의 키워드 유지.
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
    // #4 정의의 화염류(도트인데 Duration 태그가 없어 "damage over time" 키워드를 못 받는 스킬) 보강 —
    //    Duration 스킬(ED 등)은 이미 위 매핑으로 도트 키워드를 받으므로 여기 대상이 아니다(기준선 불변).
    if (isDotNoDuration(gem)) {
      keywords.add("damage over time");
      if (tags.contains("Fire")) {
        keywords.add("burning");
      }
    }
    // ⚠ balanced 에 "block" 키워드를 전역 추가하는 방식은 실패 롤백(2026-08-04): 키워드가 트리 greedy
    //   후보 스코어까지 오염해 막기 노드로 포인트가 새고 EHP −42%·카저 75→−5 유리대포화(DPS +17%로
    //   objective 는 올랐지만 balanced 설계 의도 위반). 방패 막기 접두는 craftRare 의 방패 국소 주입으로.
    // #2 자신을 태우고 데미지가 최대 생명에 비례하는 스킬(정의의 화염)엔 생명·재생을 후보 키워드로 —
    //    최대 생명이 곧 데미지 스케일러이자 자가피해 지속원이라 생명/재생 노드·모드가 후보가 돼야 한다.
    //    다른 스킬(사이클론/아크/ED)은 이 게이트에 안 걸려 후보 풀이 그대로다(기준선 불변).
    if (isSelfBurnLifeScaled(gem)) {
      keywords.add("life");
      keywords.add("regen");
    }
    return keywords;
  }

  /**
   * RF 처럼 Duration 태그 없이 지속피해(도트)로 구르는 스킬 판정 — Duration 태그가 있으면(ED 등) 이미 "damage over time" 키워드를
   * 받으므로 제외하고, 설명에 지속피해 신호(burn/per second/over time/degeneration)가 있는 것만.
   */
  private static boolean isDotNoDuration(PoeGem gem) {
    List<String> tags = gem.tags() != null ? gem.tags() : List.of();
    if (tags.contains("Duration")) {
      return false;
    }
    String d = gem.description() != null ? gem.description().toLowerCase(Locale.ROOT) : "";
    return d.contains("burn")
        || d.contains(" per second")
        || d.contains("over time")
        || d.contains("degeneration");
  }

  /**
   * 자신을 태우며 데미지가 최대 생명에 비례하는 스킬(정의의 화염류) 판정 — RF 설명의 "burns you"/"1 life remaining" 으로 좁게 잡는다. 생명이
   * 데미지이자 생존원인 이 부류만 생명/재생 키워드를 받는다.
   */
  private static boolean isSelfBurnLifeScaled(PoeGem gem) {
    String d = gem.description() != null ? gem.description().toLowerCase(Locale.ROOT) : "";
    return d.contains("burns you") || d.contains("1 life remaining");
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
  /** 잡별 할당 노드 스냅샷 — tattooFits 의 "연결된 **할당** 노드 수" 판정용. 잡별 상태 — runJob 트리 확정 시 갱신. */
  private volatile Set<Integer> tattooAllocated = Set.of();

  private boolean tattooFits(PoeTattooDataService.Tattoo tattoo, int nodeId) {
    if (tattoo.minConnected() <= 0 && tattoo.maxConnected() >= 100) {
      return true; // 대부분은 제약이 없다 — 이웃 세는 비용도 아낀다
    }
    // 게임 규칙은 "인접한 **할당된** 패시브 수"다(마캉가 여정 문신 = 1개 이하). 정적 그래프 이웃 수로 세면
    // 허브 근처 할당-끝단 소형(그래프 이웃 4, 할당 이웃 1)이 부당 탈락한다 — 실측: 나마후 문신 자리 0건.
    java.util.Collection<Integer> neighbors = poeTreeGraphService.neighbors(nodeId);
    long linked =
        tattooAllocated.isEmpty()
            ? neighbors.size()
            : neighbors.stream().filter(tattooAllocated::contains).count();
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
  private List<String> masteryLabels(
      Map<Integer, Integer> picks, Set<Integer> allocated, boolean ko) {
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
          ko && effect.statsKo() != null && !effect.statsKo().isEmpty()
              ? effect.statsKo().get(0)
              : effect.stats().isEmpty() ? "" : effect.stats().get(0);
      String nodeName = ko && node.nameKo() != null ? node.nameKo() : node.name();
      labels.add(nodeName + " — " + effectText);
    }
    return labels;
  }

  /** 표시용 문신 요약 — 같은 문신을 여러 패시브에 새기므로 "한글명 ×N" 으로 묶는다. */
  private List<String> tattooLabels(Map<Integer, String> picks, boolean ko) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String dn : picks.values()) {
      String label =
          poeTattooDataService
              .findByDn(dn)
              .map(
                  t -> ko && t.nameKo() != null ? t.nameKo() : t.name() != null ? t.name() : t.dn())
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
      Map<Integer, Equipped> jewels) {
    Set<String> attrKeys = Set.of("str", "dex", "int");
    // 더는 손쓸 수 없는 속성 — 여기 든 속성은 건너뛰고 **다른 속성은 계속** 보정한다
    // (예전엔 dex 불가에서 바로 return 해 str 하향이 통째로 안 돌았다)
    Set<String> unfixable = new LinkedHashSet<>();
    // 직전 "젬 제외"의 (속성, 부족량, 젬) — 제외해도 부족이 안 줄면 되돌리고 그 속성은 포기한다
    // (요구치 주범이 아닌 젬을 연쇄로 갉아먹는 낭비 방지 — 실측에서 Lv1 유틸 젬 2개를 헛제외했다)
    Object[] lastDrop = null; // {String attr, Integer shortfall, PoeGem gem, Boolean isAura}
    // 젬 레벨 하향은 한 번에 3레벨씩 내려가므로 반복이 더 필요하다(레어 경로만일 땐 6회면 충분했다)
    for (int guard = 0; guard < 24; guard++) {
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
        if (unfixable.contains(attrEntry.getKey())) {
          continue;
        }
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
      if (lastDrop != null) {
        if (worstAttr != null
            && worstAttr.equals(lastDrop[0])
            && worstShort >= (Integer) lastDrop[1]) {
          // 제외가 무효였다 — 되돌리고 이 속성은 포기
          PoeGem dropped = (PoeGem) lastDrop[2];
          if (Boolean.TRUE.equals(lastDrop[3])) {
            selectedAuras.add(dropped);
          } else {
            supports.add(dropped);
          }
          log(
              "속성 보정: 젬 제외 되돌림 — "
                  + (dropped.nameKo() != null ? dropped.nameKo() : dropped.name())
                  + " (부족이 줄지 않음, "
                  + worstAttr
                  + " 포기)");
          unfixable.add(worstAttr);
          lastDrop = null;
          continue;
        }
        lastDrop = null;
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
        // 전 슬롯 유니크 — 실전의 마지막 수단: 부족 속성 색상의 보조젬 레벨을 낮춘다
        // (젬 속성 요구치는 레벨에 비례. 색상=속성: 빨강=힘, 초록=민첩, 파랑=지능)
        String color =
            switch (worstAttr) {
              case "str" -> "red";
              case "dex" -> "green";
              default -> "blue";
            };
        // 부족 속성 색상의 젬(보조 + 오라)을 **한 번에 한 단계씩 일괄** 하향 — 한 젬씩은 평가 횟수가 폭증한다
        List<PoeGem> pool = new ArrayList<>(supports);
        pool.addAll(selectedAuras);
        Map<String, Integer> overrides = new LinkedHashMap<>(supportLevelOverride);
        List<String> loweredNames = new ArrayList<>();
        for (PoeGem candidate : pool) {
          if (!color.equals(candidate.color())) {
            continue;
          }
          int level = overrides.getOrDefault(candidate.slug(), defaultGemLevel(candidate));
          if (level <= 1) {
            continue;
          }
          int newLevel = Math.max(1, level - 3);
          overrides.put(candidate.slug(), newLevel);
          loweredNames.add(
              (candidate.nameKo() != null ? candidate.nameKo() : candidate.name())
                  + " Lv"
                  + level
                  + "→"
                  + newLevel);
        }
        if (loweredNames.isEmpty()) {
          // 각성 젬은 Lv1 도 요구치 114(캐릭 72레벨 기반) — 하향으로 못 풀면 **일반판으로 교체**한다
          // (실전에서도 속성이 안 되면 각성 대신 일반 향상/강화를 쓴다)
          boolean swapped = false;
          for (int i = 0; i < supports.size(); i++) {
            PoeGem candidate = supports.get(i);
            if (!color.equals(candidate.color()) || !candidate.name().startsWith("Awakened ")) {
              continue;
            }
            String regularName = candidate.name().substring("Awakened ".length());
            PoeGem regular = poeGemDataService.findByName(regularName).orElse(null);
            if (regular == null) {
              continue;
            }
            supports.set(i, regular);
            Map<String, Integer> cleaned = new LinkedHashMap<>(supportLevelOverride);
            cleaned.remove(candidate.slug());
            supportLevelOverride = cleaned;
            log(
                "속성 보정: 각성 젬 교체 — "
                    + (candidate.nameKo() != null ? candidate.nameKo() : candidate.name())
                    + " → "
                    + (regular.nameKo() != null ? regular.nameKo() : regular.name())
                    + " ("
                    + worstAttr
                    + " "
                    + worstShort
                    + " 부족, 각성판은 Lv1 도 요구치가 높음)");
            swapped = true;
            break;
          }
          if (!swapped) {
            // 마지막 수단: 그 색 젬 중 바닥 요구 레벨이 가장 높은 것(요구치 주범)을 **제외**한다.
            // 6링크→5링크 는 실전에서도 속성이 안 되면 감수하는 결과 — 불법 빌드보단 정직하다.
            PoeGem dropTarget = null;
            boolean dropIsAura = false;
            int dropFloor = 0;
            for (PoeGem candidate : supports) {
              if (color.equals(candidate.color()) && gemFloorLevel(candidate) > dropFloor) {
                dropFloor = gemFloorLevel(candidate);
                dropTarget = candidate;
                dropIsAura = false;
              }
            }
            for (PoeGem candidate : selectedAuras) {
              if (color.equals(candidate.color()) && gemFloorLevel(candidate) > dropFloor) {
                dropFloor = gemFloorLevel(candidate);
                dropTarget = candidate;
                dropIsAura = true;
              }
            }
            if (dropTarget != null) {
              if (dropIsAura) {
                selectedAuras.remove(dropTarget);
              } else {
                supports.remove(dropTarget);
              }
              lastDrop = new Object[] {worstAttr, worstShort, dropTarget, dropIsAura};
              log(
                  "속성 보정: 젬 제외 — "
                      + (dropTarget.nameKo() != null ? dropTarget.nameKo() : dropTarget.name())
                      + (dropIsAura ? " (오라)" : " (보조)")
                      + " — Lv1 도 요구 레벨 "
                      + dropFloor
                      + " 라 충족 불가 ("
                      + worstAttr
                      + " "
                      + worstShort
                      + " 부족)");
            } else {
              log(
                  "속성 보정 불가 — "
                      + worstAttr
                      + " "
                      + worstShort
                      + " 부족: 레어/젬 하향/각성 교체/제외 모두 소진 — 이 속성은 포기");
              unfixable.add(worstAttr);
            }
          }
          continue;
        }
        supportLevelOverride = overrides;
        log(
            "속성 보정: 젬 레벨 일괄 하향("
                + worstAttr
                + " "
                + worstShort
                + " 부족) — "
                + String.join(", ", loweredNames));
        continue;
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

  /**
   * 원소 저항(화/냉/번) 캡(75) 보정 — **balanced 잡 전용**(호출부 게이팅). 미달 원소 저항을 레어 접미어(저항)로 채운다. 실빌드는 저항을 반드시
   * 캡하는데(사용자 지적), balanced 결과가 미캡으로 나오는 케이스(예 Penance Brand 번개43)를 막는다.
   *
   * <p>슬롯 선택: 저항 패밀리 허용 && 그 저항 미보유 레어. 접미어 여유(&lt;3)면 추가, 꽉 찼으면 **최저 가치(키워드 점수 최하) 비저항·비속성 접미어를
   * 교체**(속성 접미어는 요구치 유지 위해 보존, 저항 접미어는 핑퐁 방지). 붙일 곳 없으면 그 원소 포기. 변경이 있었으면 true(호출부가 finalXml 재계산).
   * 최종 빌드 기준 평가(트리·문신·마스터리·오라 반영).
   */
  private boolean repairResistanceShortfalls(
      Map<Slot, Equipped> items,
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> allocated,
      Map<Integer, Equipped> jewels) {
    Map<String, String> resStat = new LinkedHashMap<>();
    resStat.put("fireRes", "FireResist");
    resStat.put("coldRes", "ColdResist");
    resStat.put("lightRes", "LightningResist");
    Set<String> resKeys = Set.of("fireRes", "coldRes", "lightRes", "allRes");
    Set<String> attrKeys = Set.of("str", "dex", "int", "allattr");
    Set<String> unfixable = new LinkedHashSet<>();
    boolean changed = false;
    for (int guard = 0; guard < 12; guard++) {
      Map<String, Double> values =
          poePobEngineService.calculateValues(
              buildXml(
                  gem, supports, className, ascendancy, ascendancyNodes, allocated, items, jewels));
      evalCount.incrementAndGet();
      String worst = null;
      double worstGap = 0;
      for (Map.Entry<String, String> e : resStat.entrySet()) {
        if (unfixable.contains(e.getKey())) {
          continue;
        }
        // 목표 저항 — 아키타입 특화(치프틴 RF 등)면 90, 그 외 75. (하드코딩 75 → 아키타입 타겟)
        double resTarget =
            "fireRes".equals(e.getKey())
                ? targetFireRes
                : "coldRes".equals(e.getKey()) ? targetColdRes : targetLightRes;
        double gap = resTarget - values.getOrDefault(e.getValue(), 0d);
        // 캡 인지 — Missing≈0 이면 총량은 이미 캡 도달(미달분은 최대 저항 부족). 총량 접미를 더 붙여도
        // 무의미하므로 포기 처리(실측: RF 화염 86=캡, OverCap 501 인데 총량 교체만 5회 시도 후 포기하던 낭비).
        Double missing = values.get("Missing" + e.getValue());
        if (gap >= 1d && missing != null && missing <= 0.5) {
          log("저항 보정 생략 — " + e.getKey() + " " + (int) gap + " 미달은 캡 병목(총량은 캡 도달, 최대 저항 소스 필요)");
          unfixable.add(e.getKey());
          continue;
        }
        if (gap > worstGap) {
          worstGap = gap;
          worst = e.getKey();
        }
      }
      if (worst == null || worstGap < 1d) {
        return changed;
      }
      final String resKey = worst;
      // 저항 패밀리(해당 원소 우선, 없으면 allRes)
      PoeModPoolDataService.ModFamily resFamily = null;
      Slot emptySlot = null;
      Slot targetSlot = null;
      for (String familyKey : List.of(resKey, "allRes")) {
        PoeModPoolDataService.ModFamily fam = null;
        for (PoeModPoolDataService.ModFamily f : poeModPoolDataService.families()) {
          if (f.key().equals(familyKey)) {
            fam = f;
            break;
          }
        }
        if (fam == null) {
          continue;
        }
        final PoeModPoolDataService.ModFamily ff = fam;
        // ① 빈 슬롯(오프핸드 제외)
        for (Slot slot : Slot.values()) {
          if (items.containsKey(slot)
              || slot.modSlots.isEmpty()
              || slot == Slot.OFFHAND
              || slot.rareBase == null) {
            continue;
          }
          if (ff.slots().contains(slot.modSlots.get(0))) {
            emptySlot = slot;
            resFamily = ff;
            break;
          }
        }
        if (emptySlot != null) {
          break;
        }
        // ② 기존 레어 중 이 저항 미보유 && (접미어 여유 있거나 교체 가능한 비저항·비속성 접미어 보유)
        for (Map.Entry<Slot, Equipped> entry : items.entrySet()) {
          if (entry.getValue().isUnique() || entry.getKey().modSlots.isEmpty()) {
            continue;
          }
          if (!ff.slots().contains(entry.getKey().modSlots.get(0))) {
            continue;
          }
          List<PoeModPoolDataService.ModFamily> fams = entry.getValue().rare().families();
          if (fams.stream().anyMatch(f -> f.key().equals(ff.key()))) {
            continue; // 이미 이 저항 보유
          }
          long suffixCount = fams.stream().filter(f -> "suffix".equals(f.gen())).count();
          boolean canReplace =
              fams.stream()
                  .anyMatch(
                      f ->
                          "suffix".equals(f.gen())
                              && !resKeys.contains(f.key())
                              && !attrKeys.contains(f.key()));
          if (suffixCount < 3 || canReplace) {
            targetSlot = entry.getKey();
            resFamily = ff;
            break;
          }
        }
        if (targetSlot != null) {
          break;
        }
      }
      if (emptySlot != null) {
        items.put(
            emptySlot, Equipped.ofRare(new RareItem(emptySlot.rareBase, List.of(resFamily), 0.0)));
        log(
            "저항 보정: "
                + emptySlot.ko
                + " 신규 레어(+"
                + resFamily.key()
                + ") (미달 "
                + (int) worstGap
                + ")");
        changed = true;
        continue;
      }
      if (targetSlot == null) {
        log("저항 보정 불가 — " + resKey + " " + (int) worstGap + " 미달: 붙일 레어 슬롯 없음(포기)");
        unfixable.add(resKey);
        continue;
      }
      RareItem rare = items.get(targetSlot).rare();
      List<PoeModPoolDataService.ModFamily> families = new ArrayList<>(rare.families());
      long suffixCount = families.stream().filter(f -> "suffix".equals(f.gen())).count();
      String action;
      if (suffixCount < 3) {
        families.add(resFamily);
        action = "추가";
      } else {
        int replaceAt = -1;
        for (int i = families.size() - 1; i >= 0; i--) {
          PoeModPoolDataService.ModFamily f = families.get(i);
          if ("suffix".equals(f.gen())
              && !resKeys.contains(f.key())
              && !attrKeys.contains(f.key())) {
            replaceAt = i;
            break;
          }
        }
        if (replaceAt < 0) {
          unfixable.add(resKey); // 교체 가능한 접미어 없음(전부 저항/속성)
          continue;
        }
        action = families.get(replaceAt).key() + " 교체";
        families.set(replaceAt, resFamily);
      }
      items.put(
          targetSlot,
          Equipped.ofRare(
              new RareItem(
                  rare.baseType(),
                  List.copyOf(families),
                  rare.tierFraction(),
                  null,
                  rare.implicitLines(),
                  rare.implicitLinesKo())));
      log(
          "저항 보정: "
              + targetSlot.ko
              + " "
              + action
              + "(+"
              + resFamily.key()
              + ", "
              + resKey
              + " "
              + (int) worstGap
              + " 미달)");
      changed = true;
    }
    return changed;
  }

  /** 젬 Lv1 의 요구 캐릭터 레벨 — 각성/상위 계열은 Lv1 도 72(=속성 요구 114). 제외 우선순위 판단용. */
  private static int gemFloorLevel(PoeGem gem) {
    if (gem.levels() == null || gem.levels().isEmpty()) {
      return 0;
    }
    Integer required = gem.levels().get(0).requiredLevel();
    return required != null ? required : 0;
  }

  /** 젬의 실질 최대 레벨 — 각성/특수 젬은 20이 아니다(각성=5 등). 레벨 하향의 시작점. */
  private static int defaultGemLevel(PoeGem gem) {
    if (gem.levels() == null || gem.levels().isEmpty()) {
      return 20;
    }
    int max = 0;
    for (var level : gem.levels()) {
      max = Math.max(max, level.level());
    }
    return Math.min(20, max);
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

  /** 삿된 후보 포함 여부(잡 스코프) — 컨트롤러가 잡 시작 직전에 세팅한다. A/B 실측을 위해 끌 수 있다. */
  private volatile boolean foulbornEnabled = true;

  public void setFoulbornEnabled(boolean enabled) {
    this.foulbornEnabled = enabled;
  }

  /**
   * 지금 리그에서 못 얻는 고유(레거시)를 후보에서 뺄지 — 기본 켬(=뺀다). 끄면 옛 동작 그대로 전부 후보에 넣는다. 제외의 대가(DPS 손실)와 "무엇을 쓰고
   * 있었는지"를 실측으로 귀속하려면 끄고 돌릴 수 있어야 한다.
   */
  private volatile boolean excludeLegacyUniques = true;

  public void setExcludeLegacyUniques(boolean enabled) {
    this.excludeLegacyUniques = enabled;
  }

  /**
   * 금단의 화염/살점 페어 편입 여부 — 기본 켬. 이 페어는 주얼 2칸을 먹으므로 "그 자리에 있던 주얼보다 나은가"를 A/B 로 재려면 끌 수 있어야 한다(실측: SRS
   * 는 채택 시점 +4.9% 인데 최종은 기준선 대비 -15%).
   */
  private volatile boolean forbiddenEnabled = true;

  public void setForbiddenEnabled(boolean enabled) {
    this.forbiddenEnabled = enabled;
  }

  /** 삿된 후보 상한 — 후보 풀 폭발을 막는다(기본 후보 N개 위에 이만큼만 더 얹는다). */
  private static final int FOULBORN_CANDIDATES = 8;

  /**
   * 여러 줄로 쪼개진 한 모드를 아이템 텍스트 한 덩어리로 합친다 — 우리 유니크 본문이 쓰는 것과 같은 모양(LF 로 이은 한 모드).
   *
   * <p>구분자는 언제나 LF 다(윈도우라고 CRLF 를 쓰면 PoB 파서가 줄을 잘못 센다).
   */
  private static String joinLines(List<String> lines) {
    return String.join(String.valueOf((char) 10), lines);
  }

  /** 모드 문구 대조 키 — 숫자·괄호 범위·구두점을 지우고 낱말만 남긴다(우리 PoB 원문 ↔ 게임 스탯 문구의 표기 차 흡수). */
  private static String modKey(String line) {
    return line == null
        ? ""
        : line.replaceAll("\\([^)]*\\)", " ")
            .replaceAll("[0-9]+(\\.[0-9]+)?", " ")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
  }

  /**
   * 이 유니크의 <b>삿된 사본들</b> — 삿된 오브로 기존 모드 하나를 대체한 물건.
   *
   * <p>인게임에선 "삿된 붉은 꿈" 처럼 아이템마다 붙을 수 있는 옵션과 <b>어느 모드를 밀어내는지</b>가 정해져 있다(PoB 삿된 지도). 그 원본 줄을 찾아
   * 바꿔치기해야 실제 아이템이 된다 — 못 찾으면 <b>그 후보를 만들지 않는다</b>. 줄을 못 찾았는데 그냥 덧붙이면 원본+삿된을 동시에 가진, 게임에 없는 아이템이 되어
   * 최적화기가 그걸 최고점으로 고른다(가장 나쁜 실패다).
   */
  private List<PoeUniqueItem> foulbornVariants(PoeUniqueItem item) {
    List<PoeFoulbornDataService.FoulbornGroup> groups =
        poeFoulbornDataService.forUnique(item.name());
    if (groups.isEmpty() || item.explicits() == null || item.explicits().isEmpty()) {
      return List.of();
    }
    List<PoeUniqueItem> out = new ArrayList<>();
    for (PoeFoulbornDataService.FoulbornGroup group : groups) {
      for (PoeFoulbornDataService.FoulbornMod mod : group.mods()) {
        if (mod.origEn() == null
            || mod.origEn().isEmpty()
            || mod.en() == null
            || mod.en().isEmpty()) {
          continue; // 대체 대상 불명(지도 밖 신규 모드) — 어느 줄을 지울지 모르니 만들지 않는다
        }
        String wanted = modKey(String.join(" ", mod.origEn()));
        int index = -1;
        for (int i = 0; i < item.explicits().size(); i++) {
          String key = modKey(item.explicits().get(i));
          if (key.equals(wanted)
              || (!key.isEmpty() && (key.contains(wanted) || wanted.contains(key)))) {
            index = i;
            break;
          }
        }
        if (index < 0) {
          continue;
        }
        List<String> explicits = new ArrayList<>(item.explicits());
        explicits.set(index, joinLines(mod.en()));
        List<String> explicitsKo =
            item.explicitsKo() == null ? null : new ArrayList<>(item.explicitsKo());
        if (explicitsKo != null && index < explicitsKo.size()) {
          explicitsKo.set(
              index,
              mod.ko() != null && !mod.ko().isEmpty() ? joinLines(mod.ko()) : joinLines(mod.en()));
        }
        out.add(
            new PoeUniqueItem(
                "Foulborn " + item.name(),
                item.nameKo() != null ? "삿된 " + item.nameKo() : null,
                item.slug(),
                item.baseType(),
                item.baseTypeKo(),
                item.category(),
                item.requiredLevel(),
                item.league(),
                item.legacy(),
                item.radius(),
                item.implicits(),
                item.implicitsKo(),
                explicits,
                explicitsKo,
                item.variants(),
                item.defaultVariant(),
                item.reqStr(),
                item.reqDex(),
                item.reqInt(),
                item.iconKey()));
      }
    }
    return out;
  }

  /** 변형 후보 상한 — 몰락의 눈(33변형)처럼 많은 아이템이 있어 상한 없이 얹으면 탐색이 무너진다. */
  private static final int VARIANT_CANDIDATES = 8;

  /**
   * 이 유니크의 <b>변형 사본들</b> — 임프레션스 물리/화염/…, 도리아니의 망상 9종처럼 인게임에 동시에 존재하는 서로 다른 아이템.
   *
   * <p>지금까지 최적화기는 <b>기본 변형 하나만</b> 평가했다. 화염 빌드가 "화염 임프레션스"를 못 쓰고 카오스판만 보던 셈이라, 인게임에서 당연히 고르는 선택지가
   * 후보에 아예 없었다.
   */
  private List<PoeUniqueItem> uniqueVariants(PoeUniqueItem item) {
    if (item.variants() == null || item.variants().size() < 2) {
      return List.of();
    }
    Integer base = item.defaultVariant();
    List<PoeUniqueItem> out = new ArrayList<>();
    for (PoeUniqueVariant variant : item.variants()) {
      if (base != null && variant.index() == base.intValue()) {
        continue; // 기본 변형은 이미 후보에 있다
      }
      String label = variant.nameKo() != null ? variant.nameKo() : variant.name();
      out.add(
          new PoeUniqueItem(
              item.name() + " (" + variant.name() + ")",
              item.nameKo() != null ? item.nameKo() + " (" + label + ")" : null,
              item.slug(),
              item.baseType(),
              item.baseTypeKo(),
              item.category(),
              item.requiredLevel(),
              item.league(),
              item.legacy(),
              item.radius(),
              variant.implicits() != null ? variant.implicits() : item.implicits(),
              variant.implicitsKo() != null ? variant.implicitsKo() : item.implicitsKo(),
              variant.explicits() != null ? variant.explicits() : item.explicits(),
              variant.explicitsKo() != null ? variant.explicitsKo() : item.explicitsKo(),
              item.variants(),
              variant.index(),
              item.reqStr(),
              item.reqDex(),
              item.reqInt(),
              item.iconKey()));
    }
    return out;
  }

  /**
   * 후보 목록에 <b>다른 변형</b>을 얹는다 — 삿된과 같은 잣대(이번 빌드 키워드 점수)로 거른다.
   *
   * <p>기본 변형보다 이 빌드에 더 맞는 변형만 들어온다. 상한을 두는 이유는 변형이 최대 33개인 아이템이 있어서다.
   */
  private List<PoeUniqueItem> withUniqueVariants(List<PoeUniqueItem> base, List<String> keywords) {
    List<PoeUniqueItem> out = new ArrayList<>(base);
    int added = 0;
    for (PoeUniqueItem item : base) {
      if (added >= VARIANT_CANDIDATES) {
        break;
      }
      List<String> baseLines = new ArrayList<>(item.implicits());
      baseLines.addAll(item.explicits());
      int baseScore = score(baseLines, keywords);
      for (PoeUniqueItem variant : uniqueVariants(item)) {
        if (added >= VARIANT_CANDIDATES) {
          break;
        }
        List<String> lines = new ArrayList<>(variant.implicits());
        lines.addAll(variant.explicits());
        // **더 나은 변형만** 얹는다(같으면 굳이 후보를 늘릴 이유가 없다 — 탐색만 무거워진다)
        if (score(lines, keywords) > baseScore) {
          out.add(variant);
          added++;
        }
      }
    }
    // 후보가 늘었는지 **보이게** 한다 — 안 보이면 "왜 변형이 안 뽑혔는지"를 추측하게 된다(실측 0건 사고).
    if (added > 0) {
      log("변형 후보 추가 " + added + "개 (기본 변형보다 이 빌드에 맞는 것만)");
    }
    return out;
  }

  /**
   * 후보 목록에 삿된 사본을 얹는다 — 단, <b>이번 빌드 키워드 기준으로 원본만큼은 되는</b> 것만.
   *
   * <p>DPS 로 거르는 게 아니라(그건 평가 전엔 모른다) 기본 후보를 뽑을 때와 같은 잣대(키워드 점수)를 쓴다. 상한을 두는 이유는 유니크마다 옵션이 1~3개라 전부
   * 얹으면 후보가 몇 배로 불어 탐색이 느려지기 때문이다.
   */
  private List<PoeUniqueItem> withFoulbornVariants(
      List<PoeUniqueItem> base, List<String> keywords) {
    if (!foulbornEnabled) {
      return base;
    }
    List<PoeUniqueItem> out = new ArrayList<>(base);
    int added = 0;
    for (PoeUniqueItem item : base) {
      if (added >= FOULBORN_CANDIDATES) {
        break;
      }
      List<String> baseLines = new ArrayList<>(item.implicits());
      baseLines.addAll(item.explicits());
      int baseScore = score(baseLines, keywords);
      for (PoeUniqueItem variant : foulbornVariants(item)) {
        if (added >= FOULBORN_CANDIDATES) {
          break;
        }
        List<String> lines = new ArrayList<>(variant.implicits());
        lines.addAll(variant.explicits());
        if (score(lines, keywords) >= baseScore) {
          out.add(variant);
          added++;
        }
      }
    }
    if (added > 0) {
      log("삿된 후보 추가 " + added + "개");
    }
    return out;
  }

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
        item.legacy(),
        item.radius(),
        item.implicits(),
        item.implicitsKo(),
        lines,
        item.explicitsKo(),
        item.variants(),
        item.defaultVariant(),
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
        .filter(item -> !excludeLegacyUniques || !item.legacy())
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
        // 구조형 주얼은 점수가 0 이어도 통과시킨다 — 값어치가 문구가 아니라 **구조**에서 나오기 때문이다
        // (소켓을 늘린다, 노터블을 할당한다, 반경 안 패시브를 바꾼다 …). 키워드 점수로 거르면 이런 건
        // 평가조차 안 된다. 어느 게 이득인지는 아래 후보 컷을 통과한 뒤 **엔진이** 정한다.
        .filter(scored -> scored.score() > 0 || isStructuralJewel(scored.item()))
        .sorted(Comparator.comparingInt(Scored::score).reversed())
        .limit(ITEM_CANDIDATES)
        .map(Scored::item)
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> withFoulbornVariants(withUniqueVariants(list, keywords), keywords)));
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
    // P1② 메타 무기 구성 — 무기 유니크 후보도 메타 category 로 제약(교집합 비면 무제약 폴백).
    Set<String> metaCategories =
        slot == Slot.WEAPON && SEED_WEAPON_ENABLED && !metaWeaponClasses.isEmpty()
            ? metaWeaponClasses.stream()
                .map(WEAPON_CLASS_TO_CATEGORY::get)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet())
            : Set.of();
    record Scored(PoeUniqueItem item, int score) {}
    return poeUniqueDataService.search(null, "all", null).stream()
        // 지금 리그에서 못 얻는 고유는 추천하지 않는다(명예로운 문신 제외와 같은 이유).
        // 판정 근거는 게임의 고유 수집 탭 표시 플래그 — 실빌드 패싯은 상위 12개로 잘려 근거가 못 된다.
        .filter(item -> !excludeLegacyUniques || !item.legacy())
        .filter(
            item ->
                categories.contains(item.category())
                    || (spellWeapon
                        && item.baseType() != null
                        && item.baseType().contains("Sceptre")))
        .filter(
            item ->
                metaCategories.isEmpty()
                    || slot != Slot.WEAPON
                    || metaCategories.contains(item.category())
                    || (metaWeaponClasses.contains("Sceptre")
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
        .collect(
            java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> withFoulbornVariants(withUniqueVariants(list, keywords), keywords)));
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
      Map<Integer, Equipped> jewels) {
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
      Map<Integer, Equipped> jewels,
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
   * 다단계(skillPart) 스킬의 실빌드 표준 파트 — PoB 는 파트별 데미지가 크게 다른데(예: 칼날 소용돌이 파트3=칼날 10개), 미지정 시 파트1(소수 스택)로
   * 계산돼 실플레이어 중앙값보다 낮게 나온다(실측 ninja 대비 BV 0.36×). PoB skillPart 는 1-base(파트1=기본).
   */
  private static final Map<String, Integer> SKILL_PART_BY_SKILL = Map.of("Blade Vortex", 3);

  /** 메인 Skill 요소에 붙일 skillPart 속성 — 미등록 스킬은 빈 문자열(파트1 기본). */
  private String skillPartAttr(String skillName) {
    Integer part = skillName == null ? null : SKILL_PART_BY_SKILL.get(skillName);
    return part == null ? "" : " skillPart=\"" + part + "\"";
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
      Map<Integer, Equipped> jewels,
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
      Map<Integer, Equipped> jewels,
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
        // #235 includeInFullDPS: PoB calcFullDPS 가 이 그룹을 집계(미니언을 마리수만큼 count-정확 합산)해
        //   output.FullDPS 에 담게 한다(헤드리스 기본 false). **미니언 잡에만 조건부** — 비미니언(단일 액터·토템)은
        //   원본 XML 과 바이트 동일 → arc/cyclone/ED/RF 구조적 불변. (전 빌드에 넣었더니 RF 가 CombinedDPS=0 →
        //   FullDPS 폴백 경로라 1,313,292→2,334,082 이탈했음. 토템은 calcFullDPS 가 count 집계 안 해 AW 회귀.)
        .append("<Skill mainActiveSkill=\"1\" enabled=\"true\"")
        .append(multiActorBuild ? " includeInFullDPS=\"true\"" : "")
        // 다단계(skillPart) 스킬 — PoB 는 파트별로 데미지가 크게 다르다(예: 칼날 소용돌이 파트3=칼날 10개).
        //   미지정 시 파트1(소수 칼날)로 계산돼 과소평가된다(실측 ninja 대비 0.36×). 실빌드 표준 파트를 지정.
        .append(skillPartAttr(gem.name()))
        // 소켓 시너지 투구(엘더 "장착된 젬에 화상 피해 보조")면 메인 그룹을 **투구 소켓에** — PoB 가
        // 아이템 문구로 소켓 지원을 자동 적용한다(실측: 자동 731,840 vs 문구 미적용 546,149). 종전의
        // 명시 Burning Damage 젬(퀄 20)은 실제 문구(퀄 0)보다 +1.6% 과대평가라 자동 적용이 정직하고,
        // 결과 PoB 뷰에서도 "오라가 투구에 소켓된" 모순 표현이 사라진다(사용자 지적).
        .append(hasElderBurningSupport(items) ? " slot=\"Helmet\">" : " slot=\"Body Armour\">")
        .append("<Gem nameSpec=\"")
        .append(gem.name())
        // 메인 스킬 젬 21/20 — 실빌드 96+ 의 표준(부패 +1). 나머지 엔드게임 전제(20/20 보조·최상위 레어·
        // 각성 계몽5)와 같은 계열. RF 등 젬 레벨 스케일 스킬의 ninja 대비 과소평가를 교정(20→21 연소 기본 +19%).
        // ⚠ 전 잡 공통 가정 변경 — 기준선 재확립 필요(2026-08-03, #170 계열).
        .append("\" level=\"21\" quality=\"20\" enabled=\"true\"/>");
    for (PoeGem support : supports) {
      // PoB 의 보조젬 이름은 "Support" 접미사가 없다 ("Spell Echo Support" 는 미인식 → 계산 무효)
      xml.append("<Gem nameSpec=\"")
          .append(support.name().replaceFirst(" Support$", ""))
          .append("\" level=\"")
          // 속성 요구치 보정이 낮춘 레벨(없으면 20) — 실전의 "요구치 맞는 레벨까지만 키운 젬"
          .append(supportLevelOverride.getOrDefault(support.slug(), 20))
          .append("\" quality=\"20\" enabled=\"true\"/>");
    }
    // (소켓 시너지의 명시 Burning Damage 젬은 제거 — 메인 그룹을 투구 슬롯에 두면 PoB 가 아이템 문구로
    //  자동 적용한다. 위 slot 분기 주석 참조. 명시 젬은 퀄 20이라 실제 문구(퀄 0) 대비 과대평가였다.)
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
      // 소켓 시너지 투구면 메인 그룹이 Helmet 을 차지 — 오라는 Body Armour 로 스왑(슬롯 충돌 회피)
      xml.append(
              hasElderBurningSupport(items)
                  ? "<Skill enabled=\"true\" slot=\"Body Armour\">"
                  : "<Skill enabled=\"true\" slot=\"Helmet\">")
          .append(
              "<Gem nameSpec=\"Awakened Enlighten\" level=\"5\" quality=\"0\" enabled=\"true\"/>");
      for (PoeGem aura : auras) {
        xml.append("<Gem nameSpec=\"")
            .append(aura.name())
            .append("\" level=\"")
            // 오라도 속성 요구치 보정 대상 — Grace(민첩)·Haste 등이 젬 총 요구치를 끌어올린다
            .append(supportLevelOverride.getOrDefault(aura.slug(), 20))
            .append("\" quality=\"20\" enabled=\"true\"/>");
      }
      xml.append("</Skill>");
    }
    // 영원한 축복(Eternal Blessing) 오라 — **예약 없이 상시 유지**되는 오라 한 개.
    // 대표 실빌드(RF)의 메인 소켓 그룹이 바로 이 구성이었다(Eternal Blessing + Malevolence + Empower):
    // 혈마법/예약 포화로 오라를 못 늘리는 빌드가 지속 피해 배율 같은 핵심 오라를 공짜로 가져가는 경로다.
    // 우리 XML 엔 이 경로가 아예 없어, 오라가 1개에서 멈춘 채 감시자의 눈 같은 오라 연계도 죽어 있었다.
    if (blessingAura != null) {
      xml.append("<Skill enabled=\"true\" slot=\"Weapon 1\">")
          .append(
              "<Gem nameSpec=\"Eternal Blessing\" level=\"20\" quality=\"20\" enabled=\"true\"/>")
          .append("<Gem nameSpec=\"")
          .append(blessingAura.name())
          .append("\" level=\"")
          .append(supportLevelOverride.getOrDefault(blessingAura.slug(), 20))
          .append("\" quality=\"20\" enabled=\"true\"/>")
          .append("</Skill>");
    }
    // 가드 스킬 — 별도 그룹. PoB 가 자동으로 버프를 머지해 GuardAbsorb 층을 만든다(설정 토글 불필요).
    if (guardSkill != null) {
      xml.append("<Skill enabled=\"true\" slot=\"Boots\"><Gem nameSpec=\"")
          .append(guardSkill)
          .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/>");
      if (guardSupport != null) {
        // 기원/강화는 링크된 젬의 레벨·품질을 올려 흡수 한도를 키운다(지속시간 계열은 최대 피격 계산엔 무의미).
        xml.append("<Gem nameSpec=\"")
            .append(guardSupport)
            .append("\" level=\"4\" quality=\"20\" enabled=\"true\"/>");
      }
      xml.append("</Skill>");
    }
    // 추가 스킬(사용자 지정) — 각자 별도 그룹으로 emit. PoB 가 오라=예약+버프, 커스=적약화, 헤럴드/가드 등 역할대로 반영.
    // 1b 패스가 선발한 전용 보조젬(화염덫 4링크 등)을 함께 링크 — 벤치 스킬별 DPS(4링크 실빌드)와 공정 비교.
    for (PoeGem extra : additionalSkills) {
      xml.append("<Skill enabled=\"true\" slot=\"Gloves\"><Gem nameSpec=\"")
          .append(extra.name())
          .append("\" level=\"20\" quality=\"20\" enabled=\"true\"/>");
      for (PoeGem extraSupport : additionalSkillSupports.getOrDefault(extra.slug(), List.of())) {
        xml.append("<Gem nameSpec=\"")
            .append(extraSupport.name().replaceFirst(" Support$", ""))
            .append("\" level=\"")
            .append(supportLevelOverride.getOrDefault(extraSupport.slug(), 20))
            .append("\" quality=\"20\" enabled=\"true\"/>");
      }
      xml.append("</Skill>");
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
      // 엘드리치 임플리싯(포식자/총주교)은 게임에서 **non-unique(레어/매직/일반)에만** 부여된다 — 엘드리치
      //   잉걸/영액은 고유템에 못 쓴다. 따라서 유니크엔 엘드리치를 붙이지 않는다(rareItemText 만 craft 시점에 반영).
      //   (아뮬렛 도유는 탐색이 끝난 뒤 withAnoint 로 얹는다 — 후보를 실제로 평가해 고르므로 여기서 붙이지 않는다)
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
    // 쌍수 전용 스킬(듀얼 스트라이크 등)은 오프핸드 무기가 없으면 PoB 가 스킬을 비활성 처리한다(실측 5,168).
    //   보조장비가 비어 있으면 주 무기와 같은 표준 무기를 두 번째 손에 지급해 쌍수를 성립시킨다.
    if (!items.containsKey(Slot.OFFHAND)
        && poeSkillWeaponDataService.requiresDualWield(gem.name())) {
      String offWeapon = standardWeapon(gem);
      if (offWeapon != null) {
        itemId++;
        xml.append("<Item id=\"")
            .append(itemId)
            .append("\">\nRarity: RARE\nSim Offhand\n")
            .append(offWeapon)
            .append(
                "\nItem Level: 84\nImplicits: 0\nAdds 60 to 120 Physical Damage\n+2000 to Accuracy Rating\n</Item>");
        slots.append("<Slot name=\"Weapon 2\" itemId=\"").append(itemId).append("\"/>");
      }
    }
    // 주얼 아이템 (900번대 id, 소켓의 itemId 와 일치) — ItemSet 슬롯에는 넣지 않는다(트리 Sockets 로 연결)
    int jewelId = 900;
    for (Equipped jewel : jewels.values()) {
      xml.append("<Item id=\"")
          .append(jewelId++)
          .append("\">\n")
          .append(jewel.isUnique() ? uniqueItemText(jewel.unique()) : rareItemText(jewel.rare()))
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
      // ⚠ Wither(multiplierWitheredStackCount) 는 여기 넣지 않는다: PoB 의 ifFlag="Condition:CanWither" 는
      //    GUI 표시 게이트라 **헤드리스 주입 시 원천 없이 무조건 +90% 카오스 적용**된다. 그러면 greedy 가
      //    "공짜 위더"를 노려 카오스 변환 빌드로 DPS 를 부풀린다(실측: 물리 cyclone 이 +40% 급등 = 근거 없음).
      //    위더는 반드시 최적화기가 실제 위더 원천(위더 스킬 셋업 등)을 넣었을 때만 = self-gated 로 모델링해야 함(후속).
    }
    // P1③ 메타 판테온(balanced 전용, 패싯 최다) — 미설정("")이면 미출력 → dps/ehp 기준선 불변.
    //   ⚠ 정지 조건부 효과(Tukohama 등)는 PoB 의 Stationary 조건 게이트에 따르므로 일부만 반영될 수 있음.
    if (!metaPantheonMajor.isEmpty()) {
      config
          .append("<Input name=\"pantheonMajorGod\" string=\"")
          .append(metaPantheonMajor)
          .append("\"/>");
    }
    if (!metaPantheonMinor.isEmpty()) {
      config
          .append("<Input name=\"pantheonMinorGod\" string=\"")
          .append(metaPantheonMinor)
          .append("\"/>");
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
    } else if (slot == Slot.OFFHAND
        && gem != null
        && gem.tags() != null
        && gem.tags().contains("Bow")) {
      // 활 빌드의 보조장비는 **화살통**이다. 여태 Slot.OFFHAND 는 무조건 방패(Titanium Spirit Shield)로
      // 크래프트돼, 인게임에서 들 수 없는 조합(활+방패)을 평가하고 화살통 전용 모드(화살 추가 등)는
      // 후보에 낄 자리조차 없었다.
      category = "quiver";
      rareBase = "Feathered Arrow Quiver";
    } else {
      if (slot.rareBase == null || slot.modSlots.isEmpty()) {
        return null;
      }
      category = slot.modSlots.get(0);
      rareBase = slot.rareBase;
    }
    // ES 듀얼패스(forceEsBase)에선 방어 크래프트(ES 접두+저항)로 — CI 빌드는 ES 방어 모드가 핵심.
    return craftRare(category, rareBase, keywords, tierFraction, forceEsBase);
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
    // ES 듀얼패스에선 방어 베이스를 ES 변형으로 강제(CI 빌드는 ES 가 주 방어풀). 평소엔 직업 주 속성 휴리스틱.
    String wanted = forceEsBase ? "es" : defenceTypeFor(currentClassName);
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
                        || poeModDataService.canSpawn(
                            baseClass, baseVariant, f.pattern(), influenceOf(f)))
            .toList();
    List<PoeModPoolDataService.ModFamily> prefixPool =
        new ArrayList<>(pool.stream().filter(f -> "prefix".equals(f.gen())).toList());
    List<PoeModPoolDataService.ModFamily> suffixPool =
        new ArrayList<>(pool.stream().filter(f -> "suffix".equals(f.gen())).toList());
    // 특수 에센스(공포/광기/섬망/히스테리아) 전용 **글로벌** 모드 — 일반 크래프팅으로 못 얻는 라인이라
    // 풀에 없다. 에센스 제작은 결정적(화폐 선택)이므로 엘드리치처럼 후보에 넣되, 소켓 젬 한정 라인은
    // 메인 링크가 그 슬롯에 없으면 무효라 제외하고 반지의 글로벌 3종만 큐레이션. 키워드 게이트로
    // 무관한 빌드(예: DoT multi ↔ 히트 빌드)에는 점수 0 → 미채택.
    if ("Ring".equals(baseClass)) {
      // 키워드 2개씩 — 동점(스코어 1) 시 stable sort 로 큐레이션 풀(먼저 추가됨)에 밀리는 것을 방지.
      // 예: ED(카오스 DoT)에서 Delirium 은 "damage over time"+"chaos"=2 로 fireDmgPct(1)를 이긴다.
      Map<String, List<String>> essenceKeywords =
          Map.of(
              "Delirium", List.of("damage over time", "chaos"),
              "Hysteria", List.of("physical", "fire"),
              "Horror", List.of("cold", "attack"));
      List<PoeEssenceDataService.EssenceEntry> ringEssences =
          poeEssenceDataService.forItemClass("Ring");
      if (ringEssences != null) {
        for (PoeEssenceDataService.EssenceEntry entry : ringEssences) {
          List<String> kw = essenceKeywords.get(entry.family());
          if (kw == null || entry.en() == null || entry.en().isEmpty()) {
            continue;
          }
          PoeModPoolDataService.ModFamily pseudo =
              new PoeModPoolDataService.ModFamily(
                  "essence" + entry.family(),
                  entry.gen(),
                  List.of("ring"),
                  kw,
                  null,
                  List.of(new PoeModPoolDataService.ModTier(1, entry.en(), entry.ko())));
          if ("prefix".equals(entry.gen())) {
            prefixPool.add(pseudo);
          } else {
            suffixPool.add(pseudo);
          }
        }
      }
    }

    java.util.function.BiFunction<
            List<PoeModPoolDataService.ModFamily>,
            java.util.function.Predicate<PoeModPoolDataService.ModFamily>,
            PoeModPoolDataService.ModFamily>
        firstMatch = (list, pred) -> list.stream().filter(pred).findFirst().orElse(null);

    List<PoeModPoolDataService.ModFamily> prefixes = new ArrayList<>();
    List<PoeModPoolDataService.ModFamily> suffixes = new ArrayList<>();
    if (defensive) {
      if (forceEsBase) {
        // ES/CI(듀얼패스): 생명 대신 ES 방어 접두(증가 ES% + 최대 ES)를 우선 확보 — CI 는 ES 가 주 방어풀이라
        //   생명 접두는 무의미하다. esPctLocal(증가 ES%)·esLocal(+최대 ES) 둘 다 접두칸에 넣는다.
        prefixPool.stream()
            .filter(f -> "esPctLocal".equals(f.key()) || "esLocal".equals(f.key()))
            .limit(2)
            .forEach(prefixes::add);
      } else {
        // 생명 접두 1개를 먼저 확보 (실전 레어의 방어 기반)
        PoeModPoolDataService.ModFamily life =
            firstMatch.apply(prefixPool, f -> hasKeyword(f, "life"));
        if (life != null) {
          prefixes.add(life);
        }
      }
      // 저항 접미 2개 (공통)
      suffixPool.stream().filter(f -> hasKeyword(f, "resistance")).limit(2).forEach(suffixes::add);
    }
    // balanced 방패 — 실빌드 표준 막기 접두(blockPctLocal)를 국소 보장. "block" 키워드 전역 주입은
    // 트리 greedy 까지 오염해 유리대포화(EHP −42%·카저 −5)로 실패 롤백 — 방패 크래프트에만 결정적 편입.
    if (balancedJob && "shield".equals(category)) {
      for (String blockKey : List.of("blockPctLocal", "spellBlockLocal")) {
        prefixPool.stream()
            .filter(f -> blockKey.equals(f.key()))
            .findFirst()
            .ifPresent(
                f -> {
                  if (!prefixes.contains(f) && prefixes.size() < 3) {
                    prefixes.add(f);
                  }
                });
      }
    }
    // (활 추가 화살 주입은 롤백 — 완성 문맥 A/B 실측에서 화살 수가 CombinedDPS 에 무기여(문구 무관
    //  A=B=C 동일): PoB 단일표적 계산은 투사체 수를 반영하지 않아 접미 슬롯만 낭비한다. 42단계 대표 활
    //  이식 +45.3%는 물리 스택 효과였음. 54단계 판정.)
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
    // 에센스는 아이템당 1개만 쓸 수 있다(에센스 1개 = 보장 모드 1개). 여러 에센스 계열이 뽑혔으면 최상위(먼저 뽑힌) 1개만
    //   남긴다 — 안 그러면 게임에 존재할 수 없는 "에센스 모드 2개" 아이템이 계산된다.
    boolean[] essenceSeen = {false};
    chosen.removeIf(
        f -> {
          if (f.key() != null && f.key().startsWith("essence")) {
            if (essenceSeen[0]) {
              return true;
            }
            essenceSeen[0] = true;
          }
          return false;
        });
    if (chosen.isEmpty()) {
      return null;
    }
    // 엘드리치 임플리싯(총주교 1 + 포식자 1) — 방어구/목걸이 슬롯이면 스킬 키워드에 맞는 최상위 티어를 얹는다.
    // 단, 영향력(엘더 등) 모드가 채택된 아이템엔 게임 규칙상 엘드리치 오브를 못 발라 상호 배타.
    boolean influenced = chosen.stream().anyMatch(f -> influenceOf(f) != null);
    EldritchPick eldritch =
        influenced ? new EldritchPick(List.of(), List.of()) : eldritchImplicits(category, keywords);
    return new RareItem(rareBase, chosen, tierFraction, null, eldritch.en(), eldritch.ko());
  }

  /** 장착 레어 중 소켓 시너지 패밀리(elderBurningSupport)를 가진 것이 있는지 — buildXml 의 젬 추가 트리거. */
  private static boolean hasElderBurningSupport(Map<Slot, Equipped> items) {
    for (Equipped equipped : items.values()) {
      if (equipped != null
          && equipped.rare() != null
          && equipped.rare().families().stream()
              .anyMatch(f -> "elderBurningSupport".equals(f.key()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * 이 큐레이션 패밀리가 요구하는 영향력 — 키 접두("elder…")로 표시한다. 영향력 전용 모드는 무영향력 스폰 풀에 없어 canSpawn 판정과 엘드리치 상호 배타
   * 규칙에 이 값이 필요하다. 일반 패밀리는 null.
   */
  private static String influenceOf(PoeModPoolDataService.ModFamily family) {
    String key = family.key();
    if (key == null) {
      return null;
    }
    if (key.startsWith("elder")) {
      return "elder";
    }
    if (key.startsWith("shaper")) {
      return "shaper";
    }
    return null;
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
    // 표시용 ko 라인에 팩션 마커를 박아 화면에서 총주교/포식자를 구분한다(en 은 XML 용이라 원문 유지).
    List<List<PoeEldritchDataService.EldritchFamily>> byFaction =
        List.of(pools.exarch(), pools.eater());
    String[] factionKo = {"총주교", "포식자"};
    for (int fi = 0; fi < byFaction.size(); fi++) {
      var tier = bestEldritchTier(byFaction.get(fi), keywords);
      if (tier != null) {
        en.addAll(tier.en());
        for (String kline : tier.ko()) {
          ko.add("(" + factionKo[fi] + ") " + kline);
        }
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

  /** 표시용 라벨(유니크=한글명/영문명, 레어 주얼=제작 주얼). */
  private String jewelLabel(Equipped jewel) {
    if (jewel.isUnique()) {
      return jewel.unique().nameKo() != null ? jewel.unique().nameKo() : jewel.unique().name();
    }
    return "제작 주얼";
  }

  /**
   * 최적화기 크래프팅용 레어 주얼 모드 패밀리(큐레이션). mod-pool.json 에는 jewel 슬롯이 없어(장비 슬롯만) 별도로 둔다. 실제 게임의 주얼
   * 접두(데미지/생명)·접미(치명타/속도/저항) 상위 롤을 대표값으로 담는다. 값은 실측이 아니라 대표 티어 근사 — 유니크가 최선이 아닌 빌드에서 제작 주얼이 경쟁하게 하는
   * 게 목적이라 정확 티어보다 "합리적 상위 롤"이면 충분하다.
   */
  private List<PoeModPoolDataService.ModFamily> jewelFamilies() {
    java.util.function.BiFunction<String, String, PoeModPoolDataService.ModTier> t =
        (en, ko) -> new PoeModPoolDataService.ModTier(1, List.of(en), List.of(ko));
    java.util.function.BiFunction<String[], Object[], PoeModPoolDataService.ModFamily> fam =
        (meta, tier) ->
            new PoeModPoolDataService.ModFamily(
                meta[0],
                meta[1],
                List.of("jewel"),
                List.of(meta).subList(2, meta.length),
                null,
                List.of((PoeModPoolDataService.ModTier) tier[0]));
    List<PoeModPoolDataService.ModFamily> f = new ArrayList<>();
    // ── 접두(prefix): 생명 + 데미지 유형 ──
    f.add(
        fam.apply(
            new String[] {"jewelLife", "prefix", "life"},
            new Object[] {t.apply("7% increased maximum Life", "최대 생명력 7% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelFire", "prefix", "fire", "elemental", "damage"},
            new Object[] {t.apply("12% increased Fire Damage", "화염 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelCold", "prefix", "cold", "elemental", "damage"},
            new Object[] {t.apply("12% increased Cold Damage", "냉기 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelLightning", "prefix", "lightning", "elemental", "damage"},
            new Object[] {t.apply("12% increased Lightning Damage", "번개 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelChaos", "prefix", "chaos", "damage"},
            new Object[] {t.apply("12% increased Chaos Damage", "카오스 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelPhys", "prefix", "physical", "damage"},
            new Object[] {t.apply("12% increased Global Physical Damage", "전역 물리 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelSpell", "prefix", "spell", "damage"},
            new Object[] {t.apply("10% increased Spell Damage", "주문 피해 10% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelProj", "prefix", "projectile", "damage"},
            new Object[] {t.apply("12% increased Projectile Damage", "발사체 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelArea", "prefix", "area", "damage"},
            new Object[] {t.apply("12% increased Area Damage", "범위 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelDot", "prefix", "damage over time"},
            new Object[] {t.apply("12% increased Damage over Time", "지속 피해 12% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelMinion", "prefix", "minion", "damage"},
            new Object[] {t.apply("14% increased Minion Damage", "소환수 피해 14% 증가")}));
    // ── 접미(suffix): 치명타/속도 + 저항 ──
    f.add(
        fam.apply(
            new String[] {"jewelCritMulti", "suffix", "critical", "damage"},
            new Object[] {t.apply("+16% to Global Critical Strike Multiplier", "전역 치명타 배율 +16%")}));
    f.add(
        fam.apply(
            new String[] {"jewelCritChance", "suffix", "critical"},
            new Object[] {
              t.apply("12% increased Global Critical Strike Chance", "전역 치명타 확률 12% 증가")
            }));
    f.add(
        fam.apply(
            new String[] {"jewelCastSpeed", "suffix", "cast", "spell", "speed"},
            new Object[] {t.apply("4% increased Cast Speed", "시전 속도 4% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelAttackSpeed", "suffix", "attack", "speed"},
            new Object[] {t.apply("4% increased Attack Speed", "공격 속도 4% 증가")}));
    f.add(
        fam.apply(
            new String[] {"jewelFireRes", "suffix", "resistance"},
            new Object[] {t.apply("+13% to Fire Resistance", "화염 저항 +13%")}));
    f.add(
        fam.apply(
            new String[] {"jewelColdRes", "suffix", "resistance"},
            new Object[] {t.apply("+13% to Cold Resistance", "냉기 저항 +13%")}));
    f.add(
        fam.apply(
            new String[] {"jewelLightningRes", "suffix", "resistance"},
            new Object[] {t.apply("+13% to Lightning Resistance", "번개 저항 +13%")}));
    return f;
  }

  /**
   * 레어 주얼 후보 생성 — 유니크 주얼이 최선이 아닌 빌드를 위해 제작 주얼을 경쟁에 넣는다(사용자 요청). 주얼은 접두 2 + 접미 2 까지.
   *
   * @param dps true=빌드 데미지 키워드에 맞는 데미지 접두 + 치명타/속도 접미(순수 DPS). false=실전 방어 주얼(생명 접두 + 저항 접미 2).
   * @return 붙일 모드가 없으면 null.
   */
  private RareItem craftRareJewel(List<String> keywords, boolean dps) {
    record Scored(PoeModPoolDataService.ModFamily f, int s) {}
    List<PoeModPoolDataService.ModFamily> fams = jewelFamilies();
    List<PoeModPoolDataService.ModFamily> chosen = new ArrayList<>();
    if (dps) {
      fams.stream()
          .filter(x -> "prefix".equals(x.gen()) && !hasKeyword(x, "life"))
          .map(x -> new Scored(x, score(x.keywords(), keywords)))
          .filter(s -> s.s() > 0)
          .sorted(Comparator.comparingInt(Scored::s).reversed())
          .limit(2)
          .map(Scored::f)
          .forEach(chosen::add);
      fams.stream()
          .filter(x -> "suffix".equals(x.gen()) && !hasKeyword(x, "resistance"))
          .map(x -> new Scored(x, score(x.keywords(), keywords)))
          .filter(s -> s.s() > 0)
          .sorted(Comparator.comparingInt(Scored::s).reversed())
          .limit(2)
          .map(Scored::f)
          .forEach(chosen::add);
      if (chosen.isEmpty()) {
        return null; // 데미지 키워드 매칭 없음 → DPS 주얼 무의미(방어 주얼이 따로 후보로 들어감)
      }
    } else {
      fams.stream()
          .filter(x -> "prefix".equals(x.gen()) && hasKeyword(x, "life"))
          .findFirst()
          .ifPresent(chosen::add);
      fams.stream()
          .filter(x -> "suffix".equals(x.gen()) && hasKeyword(x, "resistance"))
          .limit(2)
          .forEach(chosen::add);
      if (chosen.isEmpty()) {
        return null;
      }
    }
    return new RareItem("Crimson Jewel", chosen, 0.0);
  }

  /**
   * 레어 주얼 최종 리파인 — 완성 빌드 기준으로 각 주얼 소켓에 제작 레어(DPS/방어)를 시도해 <b>목표값이 실제로 오를 때만</b> 교체한다(단조-개선).
   *
   * <p>주얼 greedy 는 아이템 단계 前이라 빈약한 부분 빌드 기준이다 — 거기에 제작 주얼을 넣으면 조기 채택으로 최종이 오히려 나빠졌다(cyclone
   * 11.70M→10.32M, greedy 경로 의존). 이 패스는 <b>완성 빌드</b>(장비·트리·오라·주얼)에서 소켓별로 {현재 주얼, 제작 DPS, 제작 방어}를 실측
   * 비교해 최댓값만 남기므로, 유니크가 최선이면 무교체(기준선 불변)·레어가 실제로 나으면 채택(회귀 불가).
   *
   * @return 하나라도 교체했으면 true(호출부에서 finalXml/finalValues 재계산).
   */
  private boolean finalizeJewelsWithRares(
      Map<Integer, Equipped> jewels,
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      Set<Integer> allocated,
      Map<Slot, Equipped> items,
      List<String> keywords,
      String objectiveKey) {
    if (jewels.isEmpty()) {
      return false;
    }
    List<Equipped> crafted = new ArrayList<>();
    RareItem dps = craftRareJewel(keywords, false);
    if (dps != null) {
      crafted.add(Equipped.ofRare(dps));
    }
    RareItem def = craftRareJewel(keywords, true);
    if (def != null) {
      crafted.add(Equipped.ofRare(def));
    }
    if (crafted.isEmpty()) {
      return false;
    }
    java.util.function.Function<Map<Integer, Equipped>, Double> eval =
        trial -> {
          double v =
              objectiveOf(
                  poePobEngineService.calculateValues(
                      buildXml(
                          gem,
                          supports,
                          className,
                          ascendancy,
                          ascendancyNodes,
                          allocated,
                          items,
                          trial)),
                  objectiveKey);
          evalCount.incrementAndGet();
          return v;
        };
    double best = eval.apply(jewels);
    boolean changed = false;
    for (Integer socket : new ArrayList<>(jewels.keySet())) {
      Equipped cur = jewels.get(socket);
      // 사용자가 고정한 주얼 소켓은 존중(교체 금지).
      if (fixedJewels.containsKey(socket)) {
        continue;
      }
      Equipped bestPick = cur;
      for (Equipped cand : crafted) {
        Map<Integer, Equipped> trial = new LinkedHashMap<>(jewels);
        trial.put(socket, cand);
        double v = eval.apply(trial);
        if (v > best * 1.003) {
          best = v;
          bestPick = cand;
        }
      }
      if (bestPick != cur) {
        jewels.put(socket, bestPick);
        changed = true;
        log(
            "주얼 최종 교체(제작 레어): 소켓 "
                + socket
                + " → "
                + jewelLabel(bestPick)
                + " ("
                + format(best)
                + ")");
      }
    }
    return changed;
  }

  /** ES 듀얼패스 결과 — 완성된 CI+ES 대안 빌드(트리/장비/XML/스탯). */
  private record EsBuild(
      Set<Integer> nodes,
      Map<Slot, Equipped> items,
      String xml,
      Map<String, Double> values,
      // ES 대안 전용 오라 세트(Discipline 추가/교체 결과) — 오라 greedy 는 생명 빌드 시점이라 flat ES 층이
      // 손해로 보여 기각된다. null 이면 무변경, 아니면 호출부가 selectedAuras 를 통째로 교체해야
      // 결과 표시와 XML 이 일치한다.
      List<PoeGem> auraOverride,
      // 4e 후기 서포트 재선발 결과 — 서포트 greedy 는 생명 빌드 초기에 돌아 CI 문맥(카오스 전환 여부)의
      // 최적과 다르다. null 이면 무변경, 아니면 호출부가 supports 를 통째로 교체.
      List<PoeGem> supportsOverride) {}

  /** 노드 스탯에 에너지실드/CI 관련 문구가 있으면 가중(ES 트리 greedy 편향용). */
  private int esNodeScore(PoeTreeGraphService.TreeNode node) {
    if (node.stats() == null) {
      return 0;
    }
    int s = 0;
    for (String line : node.stats()) {
      if (line == null) {
        continue;
      }
      String lc = line.toLowerCase(java.util.Locale.ROOT);
      if (lc.contains("energy shield")) {
        // "maximum Energy Shield" 등 ES 증가/추가는 강하게, 그 외 ES 언급은 약하게.
        s += lc.contains("maximum energy shield") ? 8 : 4;
      }
      if (lc.contains("chaos inoculation")
          || lc.contains("ghost reaver")
          || lc.contains("zealot's oath")
          || lc.contains("energy shield recharge")) {
        s += 6;
      }
    }
    return s;
  }

  /**
   * ES/CI 듀얼패스 — CI(카오스 접종) 강제 트리 + ES 방어 장비로 <b>완성된 대안 빌드</b>를 만든다. 생명 빌드와 비교는 호출부가 하며(단조-개선), 이
   * 메서드는 생명 경로의 상태(allocated/items 등)를 건드리지 않고 사본으로만 작업한다.
   *
   * <p>트리는 start→CI 최단경로를 강제 할당한 뒤, ES-가중 스코어(데미지+ES) 기반 greedy 로 예산까지 채운다(라운드마다 score/경로길이 최고 1개).
   * 장비는 무기·반지·목걸이·주얼은 생명 빌드 것을 재사용(그대로 유효)하고, 방어 슬롯(갑옷/투구/장갑/장화)만 ES 베이스 레어로 교체한다(강제 유니크는 존중).
   *
   * @return 완성 ES 빌드, CI 노드가 없거나 경로가 예산 초과면 null.
   */
  private EsBuild tryEsTemplate(
      PoeGem gem,
      List<PoeGem> supports,
      String className,
      String ascendancy,
      Set<Integer> ascendancyNodes,
      int classStart,
      Set<Integer> lifeTree,
      Map<Slot, Equipped> lifeItems,
      Map<Integer, Equipped> jewels,
      List<String> keywords,
      String objectiveKey,
      ExecutorService executor) {
    // 1) CI 키스톤 노드
    PoeTreeGraphService.TreeNode ci = null;
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.searchCandidates()) {
      if ("keystone".equals(n.type())
          && n.name() != null
          && n.name().equalsIgnoreCase("Chaos Inoculation")) {
        ci = n;
        break;
      }
    }
    if (ci == null) {
      return null;
    }
    // 2) ES 장비 먼저 — 방어 슬롯만 ES 베이스 레어로(강제 유니크 존중). 트리 평가가 실제 ES 풀을 반영하도록
    //   greedy 前에 만든다(안 그러면 ES 노드가 풀 없이 평가돼 무가치로 나온다).
    Map<Slot, Equipped> esItems = new EnumMap<>(lifeItems);
    boolean prev = forceEsBase;
    forceEsBase = true;
    try {
      for (Slot slot : new Slot[] {Slot.BODY, Slot.HELMET, Slot.GLOVES, Slot.BOOTS}) {
        Equipped cur = esItems.get(slot);
        // **사용자 강제** 유니크만 존중한다. 생명 빌드가 스스로 고른 유니크까지 건너뛰면 방어 4슬롯이
        // 전부 유니크일 때 ES 교체가 한 슬롯도 안 일어나 ES 풀이 방패 수준(실측 1,282)에 그치고,
        // CI 대안이 구조적으로 경쟁 불가가 된다(실측: 대안 913,955 vs 생명 1,617,655). 교체가 손해면
        // 호출부의 단조-개선 비교가 걸러 준다.
        if (cur != null
            && cur.isUnique()
            && fixedUniques.stream().anyMatch(u -> u.slug().equals(cur.unique().slug()))) {
          continue;
        }
        RareItem r = craftRare(slot, gem, keywords, 0.0);
        if (r != null) {
          esItems.put(slot, Equipped.ofRare(r));
        }
      }
    } finally {
      forceEsBase = prev;
    }
    // 3) 트리 — start→CI 강제, 이후 **라운드별 PoB 실평가** greedy(생명 파이프라인과 동형). 정적 스코어로는
    //   제한 포인트 내 데미지 vs ES% 균형을 못 잡아 CI 빌드가 경쟁력이 없었다 → 실평가로 gain/point 최대 노드 채택.
    int budget = POINT_BUDGET - JEWEL_RESERVE;
    Set<Integer> nodes = new LinkedHashSet<>();
    Set<Integer> withStart = new LinkedHashSet<>();
    withStart.add(classStart);
    List<Integer> ciPath = poeTreeGraphService.shortestPath(withStart, ci.id());
    if (ciPath == null || ciPath.isEmpty() || ciPath.size() > budget) {
      return null;
    }
    nodes.addAll(ciPath);
    withStart.addAll(ciPath);
    int points = nodes.size();
    Map<PoeTreeGraphService.TreeNode, Integer> scores = new LinkedHashMap<>();
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.searchCandidates()) {
      if (n.id() == ci.id()) {
        continue;
      }
      // 생명 빌드 채택 노드에 소보너스(검증된 데미지 노드 우선) + ES 노드 가중. 후보 shortlist 우선순위용일 뿐,
      //   실제 채택은 아래 PoB 실평가가 결정한다.
      int reuse = (lifeTree != null && lifeTree.contains(n.id())) ? 10 : 0;
      int s = score(n.stats(), keywords) + esNodeScore(n) + reuse;
      if (s > 0) {
        scores.put(n, s);
      }
    }
    double current =
        objectiveOf(
            poePobEngineService.calculateValues(
                buildXml(
                    gem, supports, className, ascendancy, ascendancyNodes, nodes, esItems, jewels)),
            objectiveKey);
    evalCount.incrementAndGet();
    record Reachable(PoeTreeGraphService.TreeNode node, List<Integer> path, double priority) {}
    for (int round = 0; round < TREE_MAX_ROUNDS && points < budget; round++) {
      List<Reachable> reachable = new ArrayList<>();
      for (Map.Entry<PoeTreeGraphService.TreeNode, Integer> entry : scores.entrySet()) {
        if (nodes.contains(entry.getKey().id())) {
          continue;
        }
        List<Integer> path = poeTreeGraphService.shortestPath(withStart, entry.getKey().id());
        if (path == null || path.isEmpty() || points + path.size() > budget) {
          continue;
        }
        reachable.add(new Reachable(entry.getKey(), path, entry.getValue() / (double) path.size()));
      }
      if (reachable.isEmpty()) {
        break;
      }
      List<Reachable> topCandidates =
          reachable.stream()
              .sorted(Comparator.comparingDouble(Reachable::priority).reversed())
              .limit(TREE_ROUND_CANDIDATES)
              .toList();
      double before = current;
      Map<Reachable, Double> round0 =
          evalBatch(
              executor,
              topCandidates,
              candidate -> {
                Set<Integer> trial = new LinkedHashSet<>(nodes);
                trial.addAll(candidate.path());
                return buildXml(
                    gem, supports, className, ascendancy, ascendancyNodes, trial, esItems, jewels);
              },
              objectiveKey);
      Reachable best = null;
      double bestGainPerPoint = 0;
      for (Map.Entry<Reachable, Double> entry : round0.entrySet()) {
        double gainPerPoint = (entry.getValue() - before) / entry.getKey().path().size();
        if (gainPerPoint > bestGainPerPoint) {
          bestGainPerPoint = gainPerPoint;
          best = entry.getKey();
        }
      }
      if (best == null) {
        topCandidates.forEach(candidate -> scores.remove(candidate.node()));
        continue;
      }
      nodes.addAll(best.path());
      withStart.addAll(best.path());
      points += best.path().size();
      current = round0.get(best);
      scores.remove(best.node());
    }
    // 4) 완성 ES 빌드 평가
    String xml =
        buildXml(gem, supports, className, ascendancy, ascendancyNodes, nodes, esItems, jewels);
    Map<String, Double> vals = poePobEngineService.calculateValues(xml);
    evalCount.incrementAndGet();
    // 4b) Discipline 오라 트라이얼 — ES 빌드 표준 flat ES 오라인데, 오라 greedy 는 생명 빌드 시점에
    //   돌아 ES 풀 없는 상태에선 손해로 보여 기각된다. 완성 ES 빌드 위에서 「추가 + 기존 오라 각 1개와
    //   교체」를 전수 실측(≤1+N개), 예약 여유(미예약 마나 ≥0·생명 >0)와 목표값 상승을 모두 충족하는
    //   최고안만 채택(실측: 미예약 마나 167 < Discipline 예약 ≈290 이라 추가 단독으론 못 들어간다).
    List<PoeGem> auraOverride = null;
    if (selectedAuras.stream().noneMatch(a -> "Discipline".equals(a.name()))) {
      PoeGem discipline =
          poeGemDataService.search(null, "active", "all", null).stream()
              .filter(a -> "Discipline".equals(a.name()))
              .filter(a -> !a.levels().isEmpty())
              .findFirst()
              .orElse(null);
      if (discipline != null) {
        List<List<PoeGem>> auraTrials = new ArrayList<>();
        auraTrials.add(joined(selectedAuras, discipline)); // 추가
        for (int i = 0; i < selectedAuras.size(); i++) { // 교체
          List<PoeGem> swapped = new ArrayList<>(selectedAuras);
          swapped.set(i, discipline);
          auraTrials.add(swapped);
        }
        double bestObjective = objectiveOf(vals, objectiveKey);
        for (List<PoeGem> trialAuras : auraTrials) {
          String trialXml =
              buildXmlAuras(
                  gem,
                  supports,
                  className,
                  ascendancy,
                  ascendancyNodes,
                  nodes,
                  esItems,
                  jewels,
                  trialAuras);
          Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
          evalCount.incrementAndGet();
          if (trialVals.getOrDefault("ManaUnreserved", -1d) >= 0d
              && trialVals.getOrDefault("LifeUnreserved", 1d) > 0d
              && objectiveOf(trialVals, objectiveKey) > bestObjective) {
            bestObjective = objectiveOf(trialVals, objectiveKey);
            xml = trialXml;
            vals = trialVals;
            auraOverride = trialAuras;
          }
        }
      }
    }
    // 4b+) 헤럴드 스택 트라이얼 — |ci 대표(CuddleCorpse)의 주 DPS 소스가 헤럴드 5개 스택
    //   ("헤럴드당 8% 피해" 마스터리 채택률 96%)인데 우리 오라 greedy 는 생명 문맥에서 돌아 CI 완성
    //   빌드에선 재평가가 필요하다. greedy 라운드(상한 3): 현 오라 세트에 각 헤럴드 추가/교체 전수
    //   트라이얼, 예약 여유+목표 상승을 충족하는 라운드 최고안 채택, 개선 없으면 종료.
    {
      List<String> heraldNames =
          List.of(
              "Herald of Ash",
              "Herald of Thunder",
              "Herald of Ice",
              "Herald of Purity",
              "Herald of Agony");
      List<PoeGem> heraldGems =
          poeGemDataService.search(null, "active", "all", null).stream()
              .filter(a -> heraldNames.contains(a.name()))
              .filter(a -> !a.levels().isEmpty())
              .toList();
      for (int round = 0; round < 3; round++) {
        List<PoeGem> curAuras = auraOverride != null ? auraOverride : selectedAuras;
        double bestObjective = objectiveOf(vals, objectiveKey);
        List<PoeGem> bestAuras = null;
        String bestXml = null;
        Map<String, Double> bestVals = null;
        for (PoeGem herald : heraldGems) {
          if (curAuras.stream().anyMatch(a -> a.name().equals(herald.name()))) {
            continue;
          }
          List<List<PoeGem>> trials = new ArrayList<>();
          trials.add(joined(curAuras, herald));
          for (int i = 0; i < curAuras.size(); i++) {
            List<PoeGem> swapped = new ArrayList<>(curAuras);
            swapped.set(i, herald);
            trials.add(swapped);
          }
          for (List<PoeGem> trialAuras : trials) {
            String trialXml =
                buildXmlAuras(
                    gem,
                    supports,
                    className,
                    ascendancy,
                    ascendancyNodes,
                    nodes,
                    esItems,
                    jewels,
                    trialAuras);
            Map<String, Double> trialVals = poePobEngineService.calculateValues(trialXml);
            evalCount.incrementAndGet();
            if (trialVals.getOrDefault("ManaUnreserved", -1d) >= 0d
                && trialVals.getOrDefault("LifeUnreserved", 1d) > 0d
                && objectiveOf(trialVals, objectiveKey) > bestObjective) {
              bestObjective = objectiveOf(trialVals, objectiveKey);
              bestAuras = trialAuras;
              bestXml = trialXml;
              bestVals = trialVals;
            }
          }
        }
        if (bestAuras == null) {
          break;
        }
        auraOverride = bestAuras;
        xml = bestXml;
        vals = bestVals;
        log(
            "ES 듀얼패스 헤럴드 트라이얼: → "
                + bestAuras.stream()
                    .map(PoeGem::name)
                    .collect(java.util.stream.Collectors.joining(", "))
                + " → "
                + format(bestObjective));
      }
    }
    // 4c) 보조장비(방패) 재평가 — 생명 빌드의 방패는 생명 문맥에서 뽑혀 ES/CI 문맥의 최적이 아니다.
    //   막기/ES 레이어는 목표 게이트로는 못 유도(막기 게이트 도입 실패 실측: EHP 45k→8k 붕괴)하고
    //   **소스 자체를 후보로** 넣어야 한다: 방어 키워드로 유니크 방패(Aegis Aurora/Rathpith 류) 상위 +
    //   ES 방어 레어 방패를 완성 빌드 위에서 전수 실측, 목표값 상승 시만 교체.
    if (!weaponIsBow(esItems)
        && !offhandBlocked(esItems)
        && esItems.get(Slot.OFFHAND) != null
        && !(esItems.get(Slot.OFFHAND).isUnique()
            && fixedUniques.stream()
                .anyMatch(u -> u.slug().equals(esItems.get(Slot.OFFHAND).unique().slug())))) {
      List<String> defKeywords = List.of("energy shield", "block", "armour", "maximum life");
      List<Equipped> offhandTrials = new ArrayList<>();
      for (PoeUniqueItem shield : itemCandidates(Slot.OFFHAND, gem, defKeywords, esItems)) {
        offhandTrials.add(Equipped.ofUnique(shield));
      }
      boolean prevEs = forceEsBase;
      forceEsBase = true;
      try {
        RareItem esShield = craftDefensiveRare(Slot.OFFHAND, gem, keywords);
        if (esShield != null) {
          offhandTrials.add(Equipped.ofRare(esShield));
        }
      } finally {
        forceEsBase = prevEs;
      }
      final List<PoeGem> effectiveAuras = auraOverride != null ? auraOverride : selectedAuras;
      Map<Equipped, Double> offhandResults =
          evalBatch(
              executor,
              offhandTrials,
              trial -> {
                Map<Slot, Equipped> trialItems = new EnumMap<>(esItems);
                trialItems.put(Slot.OFFHAND, trial);
                return buildXmlAuras(
                    gem,
                    supports,
                    className,
                    ascendancy,
                    ascendancyNodes,
                    nodes,
                    trialItems,
                    jewels,
                    effectiveAuras);
              },
              objectiveKey);
      Map.Entry<Equipped, Double> bestOffhand =
          offhandResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
      if (bestOffhand != null && bestOffhand.getValue() > objectiveOf(vals, objectiveKey)) {
        // ES 붕괴 가드 — CI 대안에서 objective(DPS 이득)만으로 ES 없는 생명 유니크가 채택되면
        //   ES 절대량이 무너진다(실측: ES 2,185 vs |ci 대표 6,409 = EHP 갭 -57%의 본체). 스왑 후
        //   ES 가 직전의 70% 미만이면 롤백.
        Equipped prevOffhand = esItems.get(Slot.OFFHAND);
        String prevXml = xml;
        Map<String, Double> prevVals = vals;
        double esBefore = vals.getOrDefault("EnergyShield", 0d);
        esItems.put(Slot.OFFHAND, bestOffhand.getKey());
        xml =
            buildXmlAuras(
                gem,
                supports,
                className,
                ascendancy,
                ascendancyNodes,
                nodes,
                esItems,
                jewels,
                effectiveAuras);
        vals = poePobEngineService.calculateValues(xml);
        evalCount.incrementAndGet();
        double esAfter = vals.getOrDefault("EnergyShield", 0d);
        if (esBefore > 0 && esAfter < esBefore * 0.7) {
          esItems.put(Slot.OFFHAND, prevOffhand);
          xml = prevXml;
          vals = prevVals;
          log(
              "ES 듀얼패스 보조장비 기각(ES 붕괴): "
                  + (bestOffhand.getKey().isUnique()
                      ? bestOffhand.getKey().unique().name()
                      : bestOffhand.getKey().rare().baseType() + " (ES 레어)")
                  + " — ES "
                  + format(esBefore)
                  + " → "
                  + format(esAfter));
        } else {
          log(
              "ES 듀얼패스 보조장비 재평가: → "
                  + (bestOffhand.getKey().isUnique()
                      ? bestOffhand.getKey().unique().name()
                      : bestOffhand.getKey().rare().baseType() + " (ES 레어)")
                  + " → "
                  + format(bestOffhand.getValue()));
        }
      }
    }
    // 4d) 방어 슬롯 유니크 재평가 — 4c(방패)와 동형의 소스 편입. 스텝 2에서 방어 4슬롯을 ES 레어로 밀었는데,
    //   슬롯에 따라 방어 유니크(ES/막기/방어도 스택)가 레어를 이길 수 있다 — 완성 빌드 위에서 슬롯별
    //   전수 실측, 상승 시만 교체(사용자 강제 유니크 슬롯은 스텝 2에서 이미 보존됨이라 건드리지 않는다).
    {
      List<String> defKeywords = List.of("energy shield", "block", "armour", "maximum life");
      List<PoeGem> effectiveAuras = auraOverride != null ? auraOverride : selectedAuras;
      for (Slot defSlot : new Slot[] {Slot.BODY, Slot.HELMET, Slot.GLOVES, Slot.BOOTS}) {
        Equipped curEquip = esItems.get(defSlot);
        if (curEquip != null
            && curEquip.isUnique()
            && fixedUniques.stream().anyMatch(u -> u.slug().equals(curEquip.unique().slug()))) {
          continue;
        }
        List<Equipped> slotTrials =
            itemCandidates(defSlot, gem, defKeywords, esItems).stream()
                .map(Equipped::ofUnique)
                .toList();
        if (slotTrials.isEmpty()) {
          continue;
        }
        Map<Equipped, Double> slotResults =
            evalBatch(
                executor,
                slotTrials,
                trial -> {
                  Map<Slot, Equipped> trialItems = new EnumMap<>(esItems);
                  trialItems.put(defSlot, trial);
                  return buildXmlAuras(
                      gem,
                      supports,
                      className,
                      ascendancy,
                      ascendancyNodes,
                      nodes,
                      trialItems,
                      jewels,
                      effectiveAuras);
                },
                objectiveKey);
        Map.Entry<Equipped, Double> bestSlot =
            slotResults.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (bestSlot != null && bestSlot.getValue() > objectiveOf(vals, objectiveKey)) {
          // ES 붕괴 가드 — 4c 와 동형(주석 참조). 방어 4슬롯이 ES 없는 생명 유니크로 갈리는 것이
          //   CI EHP 갭의 최대 요인(실측: Lightning Coil/Ravenous Passion 채택 → ES 2,185).
          Equipped prevSlotItem = curEquip;
          String prevXml = xml;
          Map<String, Double> prevVals = vals;
          double esBefore = vals.getOrDefault("EnergyShield", 0d);
          esItems.put(defSlot, bestSlot.getKey());
          xml =
              buildXmlAuras(
                  gem,
                  supports,
                  className,
                  ascendancy,
                  ascendancyNodes,
                  nodes,
                  esItems,
                  jewels,
                  effectiveAuras);
          vals = poePobEngineService.calculateValues(xml);
          evalCount.incrementAndGet();
          double esAfter = vals.getOrDefault("EnergyShield", 0d);
          if (esBefore > 0 && esAfter < esBefore * 0.7) {
            if (prevSlotItem != null) {
              esItems.put(defSlot, prevSlotItem);
            } else {
              esItems.remove(defSlot);
            }
            xml = prevXml;
            vals = prevVals;
            log(
                "ES 듀얼패스 "
                    + defSlot.ko
                    + " 기각(ES 붕괴): "
                    + bestSlot.getKey().unique().name()
                    + " — ES "
                    + format(esBefore)
                    + " → "
                    + format(esAfter));
          } else {
            log(
                "ES 듀얼패스 "
                    + defSlot.ko
                    + " 재평가: → "
                    + bestSlot.getKey().unique().name()
                    + " → "
                    + format(bestSlot.getValue()));
          }
        }
      }
    }
    // 4e) 후기 서포트 재선발 — 서포트 greedy 는 생명 빌드 초기(장비/트리/오라 확정 전)에 돌아 CI 완성
    //   빌드의 최적과 다르다(실측 37단계: 우리 5링크가 Cast on Death 류 비무장, 대표 도트 3종을 우리
    //   서포트로 바꾸면 대표 DPS -41%). 특히 Original Sin(원소→카오스 전환)은 ignite 를 소멸시켜 도트
    //   서포트를 무효화하는 서포트 생태계 스위치라, ①현 장비 그대로 재선발 ②Original Sin 링→레어
    //   교체+재선발 두 컨텍스트를 A/B 하고 목표값 상승 시만 채택한다.
    List<PoeGem> supportsOverride = null;
    {
      List<PoeGem> effectiveAuras2 = auraOverride != null ? auraOverride : selectedAuras;
      List<PoeGem> supportPool =
          poeGemDataService.search(null, "support", "all", null).stream()
              .filter(s -> !s.levels().isEmpty())
              .filter(this::isProvidedSupport)
              .filter(s -> supportCompatible(gem, s))
              .toList();
      List<Map<Slot, Equipped>> contexts = new ArrayList<>();
      contexts.add(esItems);
      Slot sinSlot = null;
      for (Slot ringSlot : new Slot[] {Slot.RING1, Slot.RING2}) {
        Equipped ring = esItems.get(ringSlot);
        if (ring != null && ring.isUnique() && "Original Sin".equals(ring.unique().name())) {
          sinSlot = ringSlot;
          RareItem replacement = craftRare(ringSlot, gem, keywords, 0.0);
          if (replacement != null) {
            Map<Slot, Equipped> without = new EnumMap<>(esItems);
            without.put(ringSlot, Equipped.ofRare(replacement));
            contexts.add(without);
          }
          break;
        }
      }
      double bestObjective = objectiveOf(vals, objectiveKey);
      Map<Slot, Equipped> bestItems = null;
      List<PoeGem> bestSupports = null;
      String bestXml = null;
      Map<String, Double> bestVals = null;
      for (Map<Slot, Equipped> ctx : contexts) {
        List<PoeGem> picked = new ArrayList<>();
        Map<PoeGem, Double> firstRound =
            evalBatch(
                executor,
                supportPool,
                s ->
                    buildXmlAuras(
                        gem,
                        List.of(s),
                        className,
                        ascendancy,
                        ascendancyNodes,
                        nodes,
                        ctx,
                        jewels,
                        effectiveAuras2),
                objectiveKey);
        List<PoeGem> shortlist =
            firstRound.entrySet().stream()
                .sorted(Map.Entry.<PoeGem, Double>comparingByValue().reversed())
                .limit(SUPPORT_SHORTLIST)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        double cur = -1d;
        while (picked.size() < MAX_SUPPORTS && !shortlist.isEmpty()) {
          Map<PoeGem, Double> round =
              picked.isEmpty()
                  ? firstRound
                  : evalBatch(
                      executor,
                      shortlist,
                      s ->
                          buildXmlAuras(
                              gem,
                              joined(picked, s),
                              className,
                              ascendancy,
                              ascendancyNodes,
                              nodes,
                              ctx,
                              jewels,
                              effectiveAuras2),
                      objectiveKey);
          Map.Entry<PoeGem, Double> best =
              round.entrySet().stream()
                  .filter(e -> shortlist.contains(e.getKey()))
                  .max(Map.Entry.comparingByValue())
                  .orElse(null);
          if (best == null || best.getValue() <= cur * 1.005) {
            break;
          }
          picked.add(best.getKey());
          shortlist.remove(best.getKey());
          cur = best.getValue();
        }
        if (cur > bestObjective * 1.003) {
          bestObjective = cur;
          bestItems = ctx;
          bestSupports = picked;
          bestXml =
              buildXmlAuras(
                  gem,
                  picked,
                  className,
                  ascendancy,
                  ascendancyNodes,
                  nodes,
                  ctx,
                  jewels,
                  effectiveAuras2);
          bestVals = poePobEngineService.calculateValues(bestXml);
          evalCount.incrementAndGet();
        }
      }
      if (bestSupports != null) {
        boolean sinDropped = bestItems != esItems;
        if (sinDropped) {
          esItems.clear();
          esItems.putAll(bestItems);
        }
        xml = bestXml;
        vals = bestVals;
        supportsOverride = bestSupports;
        log(
            "ES 듀얼패스 서포트 재선발"
                + (sinDropped && sinSlot != null ? "(Original Sin → 레어 " + sinSlot.ko + ")" : "")
                + ": → "
                + bestSupports.stream()
                    .map(PoeGem::name)
                    .collect(java.util.stream.Collectors.joining(", "))
                + " → "
                + format(bestObjective));
      }
    }
    return new EsBuild(nodes, esItems, xml, vals, auraOverride, supportsOverride);
  }

  /**
   * 레어의 마지막 접미를 속성 접미(str/dex/int, T1 +60)로 교체한 변형 — 유니크가 속성 공급원인 슬롯의 공정 경쟁용(최종 재대결 보정 재도전). 이미 그
   * 속성 접미가 있거나 풀에 없으면 null. 접미가 하나도 없으면 추가(크래프트 접미 한도 3은 호출부 크래프트가 이미 지킴 — 교체가 기본이라 초과 없음).
   */
  private RareItem withAttributeSuffix(RareItem rare, Slot slot, String attrKey) {
    return withSuffixFamily(rare, slot, attrKey);
  }

  /**
   * 레어에 지정 접미 패밀리(T1)를 편입한 변형 — 속성/저항 보정 재도전 공용. 접미 3개(크래프트 한도) 미만이면 **추가**(기존 모드 무손실 — 저항 채움이 여기에
   * 해당: 접미 2개 레어에서 마지막 접미(민첩 등 젬 요구치 지탱)를 교체하면 feasibility 감쇠로 전부 기각됐다, 실측), 꽉 찼으면 마지막 접미 교체. 이미
   * 있거나 풀에 없으면 null.
   */
  private RareItem withSuffixFamily(RareItem rare, Slot slot, String familyKey) {
    if (slot.modSlots.isEmpty()) {
      return null;
    }
    PoeModPoolDataService.ModFamily family =
        poeModPoolDataService.familiesForSlot(slot.modSlots.get(0)).stream()
            .filter(f -> familyKey.equals(f.key()))
            .findFirst()
            .orElse(null);
    if (family == null || rare.families().stream().anyMatch(f -> familyKey.equals(f.key()))) {
      return null;
    }
    List<PoeModPoolDataService.ModFamily> families = new ArrayList<>(rare.families());
    long suffixCount = families.stream().filter(f -> "suffix".equals(f.gen())).count();
    if (suffixCount < 3) {
      families.add(family);
      return new RareItem(
          rare.baseType(), families, 0.0, null, rare.implicitLines(), rare.implicitLinesKo());
    }
    for (int i = families.size() - 1; i >= 0; i--) {
      if ("suffix".equals(families.get(i).gen())) {
        families.set(i, family);
        return new RareItem(
            rare.baseType(), families, 0.0, null, rare.implicitLines(), rare.implicitLinesKo());
      }
    }
    return null;
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
    // P1② 메타 무기 구성 — 실빌드 최다 구성(점유율 50%+)의 클래스로 제약. 교집합이 비면 원래 후보 유지(폴백).
    List<String> constrained =
        metaWeaponClasses.isEmpty()
            ? List.of()
            : classes.stream().filter(metaWeaponClasses::contains).toList();
    final List<String> allowedClasses = constrained.isEmpty() ? classes : constrained;
    // 보조장비를 이미 낀 상태면 양손 무기 제외 — 게임에서 방패와 양손 무기는 동시에 못 든다.
    // 메타가 "X / Shield" 구성이면 방패 자리를 지키기 위해 양손 무기를 처음부터 제외.
    boolean offhandOccupied = offhand != null;
    return poeBaseItemDataService.search(null, null).stream()
        .filter(base -> base.weapon() != null && allowedClasses.contains(base.itemClass()))
        .filter(
            base ->
                (!offhandOccupied && !metaOffhandShield)
                    || !TWO_HANDED_CLASSES.contains(base.itemClass()))
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
    // 엘드리치 임플리싯(총주교/포식자)을 맨 위에 — 게임에서도 임플리싯이 익스플리싯 위에 뜬다.
    // ko 라인엔 이미 "(총주교)/(포식자)" 팩션 마커가 박혀 있다(eldritchImplicits) → 그대로 사용, 없으면 폴백.
    if (rare.implicitLinesKo() != null) {
      for (String line : rare.implicitLinesKo()) {
        lines.add(line.startsWith("(") ? line : "(엘드리치) " + line);
      }
    }
    for (int i = 0; i < rare.families().size(); i++) {
      PoeModPoolDataService.ModFamily family = rare.families().get(i);
      // 출처·젠 마커 — (도유)/(엘드리치) 관례 + 인게임 Alt(고급 모드 설명)의 접두/접미 표기 파리티.
      // 에센스 전용 라인은 일반 크래프팅으론 못 얻는 라인임을 함께 알린다.
      String genKo =
          "prefix".equals(family.gen()) ? "접두" : "suffix".equals(family.gen()) ? "접미" : null;
      boolean essence = family.key().startsWith("essence");
      // 영향력(엘더/셰이퍼) 모드는 마커에 명시 — 이 아이템이 엘더 아이템임을 화면에서 알 수 있게
      // (실사고: 소켓 시너지 엘더 투구가 영향력 미표기라 "엘더 아이템 미착용"으로 오해, Blasphemer's
      //  Grasp 의 엘더 스택 유효성도 안 보였다).
      String influence = influenceOf(family);
      String influenceKo =
          "elder".equals(influence) ? "엘더" : "shaper".equals(influence) ? "셰이퍼" : null;
      String marker =
          essence && genKo != null
              ? "(에센스·" + genKo + ") "
              : essence
                  ? "(에센스) "
                  : influenceKo != null && genKo != null
                      ? "(" + influenceKo + "·" + genKo + ") "
                      : genKo != null ? "(" + genKo + ") " : "";
      for (String line : tierAt(family, rare.fractionFor(i)).ko()) {
        lines.add(marker + line);
      }
    }
    // rareItemText 가 공격 무기에 얹는 명중 전제도 함께 보여준다 — XML 엔 들어가는데 목록에만 없으면
    // 화면의 모드 합이 실제 계산과 달라진다(표시=실제 원칙).
    if (isAttackWeaponBase(rare.baseType())) {
      lines.add("명중 +2000 (시뮬 가정)");
    }
    return lines;
  }

  /**
   * rareModLinesKo/En 와 1:1 정렬된 티어 라벨. 티어 없는 줄(임플리싯·명중가정)은 빈 문자열, 익스플리싯은 "T{순위}/{총티어}". ⚠
   * rareModLinesKo 의 줄 생산 순서·개수를 **정확히 미러**해야 정렬이 어긋나지 않는다(임플리싯 → 패밀리별 tierAt().ko() → 명중). 순위는
   * family.tiers()(best-first) 인덱스+1 로 /poe/mods 카드(T1..Tn)와 동일 관례.
   */
  private List<String> rareModTiers(RareItem rare) {
    List<String> tiers = new ArrayList<>();
    if (rare.implicitLinesKo() != null) {
      for (int k = 0; k < rare.implicitLinesKo().size(); k++) {
        tiers.add("");
      }
    }
    for (int i = 0; i < rare.families().size(); i++) {
      PoeModPoolDataService.ModFamily family = rare.families().get(i);
      double fraction = rare.fractionFor(i);
      int n = family.tiers().size();
      int index = Math.min(Math.max((int) Math.round(fraction * (n - 1)), 0), Math.max(n - 1, 0));
      String label = "T" + (index + 1) + "/" + n;
      for (int k = 0; k < tierAt(family, fraction).ko().size(); k++) {
        tiers.add(label);
      }
    }
    if (isAttackWeaponBase(rare.baseType())) {
      tiers.add("");
    }
    return tiers;
  }

  /** rareModLinesKo 의 영문판 — 마커도 영문 (prefix)/(suffix)/(essence·prefix)/(eldritch). */
  private List<String> rareModLinesEn(RareItem rare) {
    List<String> lines = new ArrayList<>();
    if (rare.implicitLines() != null) {
      for (String line : rare.implicitLines()) {
        lines.add("(eldritch) " + line);
      }
    }
    for (int i = 0; i < rare.families().size(); i++) {
      PoeModPoolDataService.ModFamily family = rare.families().get(i);
      String genEn =
          "prefix".equals(family.gen())
              ? "prefix"
              : "suffix".equals(family.gen()) ? "suffix" : null;
      boolean essence = family.key().startsWith("essence");
      String influenceEn = influenceOf(family); // "elder"/"shaper" — ko 마커와 동일 취지(영향력 가시화)
      String marker =
          essence && genEn != null
              ? "(essence·" + genEn + ") "
              : essence
                  ? "(essence) "
                  : influenceEn != null && genEn != null
                      ? "(" + influenceEn + "·" + genEn + ") "
                      : genEn != null ? "(" + genEn + ") " : "";
      for (String line : tierAt(family, rare.fractionFor(i)).en()) {
        lines.add(marker + line);
      }
    }
    if (isAttackWeaponBase(rare.baseType())) {
      lines.add("+2000 Accuracy (sim assumption)");
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
            List.of("물리 피해 60-120 추가", "명중 +2000"),
            List.of("Adds 60-120 Physical Damage", "+2000 Accuracy"),
            // 시뮬 표준 무기는 실제 롤이 아닌 고정 가정 — 티어 없음
            List.of("", ""),
            null,
            null));
  }

  private PoeOptimizeResult.ItemPick itemPick(Slot slot, Equipped equipped) {
    if (equipped.isUnique()) {
      PoeUniqueItem unique = equipped.unique();
      // 엘드리치 임플리싯은 유니크에 안 붙는다(게임 규칙: non-unique 전용) — 계산(buildXml)에서 뺐으므로 표시에도
      //   붙이지 않는다(표시=실제 유지). 아뮬렛 도유만 유니크에도 얹힌다.
      List<String> lines = new ArrayList<>(anointLineKo(slot));
      lines.addAll(uniqueModLines(unique));
      // EN 로케일용 병렬 라인 — 결과 페이지가 유일한 ko 전용 표면이었다(다른 페이지는 전부 이중언어)
      List<String> linesEn = new ArrayList<>(anointLineEn(slot));
      linesEn.addAll(uniqueModLinesEn(unique));
      // 유니크는 롤 가능한 티어가 없다 — 줄 수만큼 빈 티어로 정렬 유지(템플릿 인덱스 정합)
      List<String> tiers = new ArrayList<>();
      for (int k = 0; k < lines.size(); k++) {
        tiers.add("");
      }
      return new PoeOptimizeResult.ItemPick(
          slot.pobName,
          slot.ko,
          "UNIQUE",
          unique.slug(),
          unique.name(),
          unique.nameKo(),
          lines,
          linesEn,
          tiers,
          // 유니크도 원클릭 링크 제공(사용자 요청) — 고유명 검색 + 즉시 구입. 실속형은 티어 개념이 없어 미제공.
          uniqueTradeQuery(unique),
          null);
    }
    RareItem rare = equipped.rare();
    // 레어도 베이스 한글명을 채운다 — 유니크만 한글로 나오고 레어는 영문이라 목록이 뒤죽박죽이었다
    PoeBaseItem baseItem = poeBaseItemDataService.findByName(rare.baseType()).orElse(null);
    String baseNameKo = baseItem != null ? baseItem.nameKo() : null;
    List<String> rareLines = new ArrayList<>(anointLineKo(slot));
    rareLines.addAll(rareModLinesKo(rare));
    List<String> rareLinesEn = new ArrayList<>(anointLineEn(slot));
    rareLinesEn.addAll(rareModLinesEn(rare));
    // 티어: 도유 라인(선두)은 빈 티어, 이어서 레어 익스플리싯 티어(rareModTiers 가 rareModLinesKo 순서 미러)
    List<String> rareTiers = new ArrayList<>();
    for (int k = 0; k < anointLineKo(slot).size(); k++) {
      rareTiers.add("");
    }
    rareTiers.addAll(rareModTiers(rare));
    return new PoeOptimizeResult.ItemPick(
        slot.pobName,
        slot.ko,
        "RARE",
        // 레어의 slug = 베이스 아이템 상세(모드 풀) 링크용 — 유니크 slug 와 같은 자리(게이트가 rarity 로 경로 분기)
        baseItem != null ? baseItem.slug() : null,
        rare.baseType(),
        baseNameKo,
        rareLines,
        rareLinesEn,
        rareTiers,
        tradeQueryFor(rare, baseNameKo, false),
        tradeQueryFor(rare, baseNameKo, true));
  }

  /**
   * 레어의 거래소 검색 쿼리(q JSON) — 베이스(한글명) + 익스플리싯 스탯 필터(min = **그 티어의 최저 롤**, 다중 수치/미매칭 라인은 존재 필터 생략).
   * 최대롤 기준(×0.7 포함) 검색은 매물이 없거나 미러급이라 구매 불가(사용자 피드백) — 같은 티어면 시장에서 살 수 있는 하한으로 잡는다. koMin 없는 구 데이터는
   * ×0.7 폴백. daum(한국) 거래소 기준이며 stat id 는 서버 공통. 사전 미로드/매칭 0건이면 null(링크 미표시).
   */
  private String tradeQueryFor(RareItem rare, String baseNameKo, boolean budget) {
    List<String> filters = new ArrayList<>();
    for (int i = 0; i < rare.families().size(); i++) {
      // 실속형(budget): 필수(픽 우선순위 상위) 모드만 필터에 넣고, min 은 2티어 최저 롤(T2 이상 매물).
      // 최상위 롤 매물이 없거나 비쌀 때의 대안 검색(사용자 요청) — 나머지 모드는 조건에서 제외.
      if (budget && i >= BUDGET_ESSENTIAL) {
        continue;
      }
      PoeModPoolDataService.ModFamily family = rare.families().get(i);
      PoeModPoolDataService.ModTier tier =
          budget
              ? family.tiers().get(Math.min(1, family.tiers().size() - 1))
              : tierAt(family, rare.fractionFor(i));
      List<String> koLines = tier.ko();
      List<String> koMinLines = tier.koMin();
      for (int j = 0; j < koLines.size(); j++) {
        String line = koLines.get(j);
        // 생명력/저항/능력치는 합산(pseudo) 필터 우선 — 시장 매물은 순수+하이브리드 여러 모드로 총량을 채우므로
        // explicit 단일 모드 min 은 T1 롤 매물만 잡혀 사실상 검색 불가(사용자 피드백: 화면의 +189 는 합산 표시)
        String statId = poeTradeStatDataService.pseudoIdFor(line);
        if (statId == null) {
          statId = poeTradeStatDataService.statIdFor(line);
        }
        if (statId == null) {
          continue;
        }
        boolean hasMinLine = koMinLines != null && j < koMinLines.size();
        String minLine = hasMinLine ? koMinLines.get(j) : line;
        // 단일 수치 라인만 min 필터 — "피해 X~Y 추가" 류 다중 수치는 id 존재 필터만
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(minLine);
        Double value = null;
        if (m.find()) {
          double first = Double.parseDouble(m.group());
          value = m.find() ? null : first;
        }
        if (value != null && !hasMinLine) {
          value = Math.floor(value * 0.7); // koMin 없는 구 데이터 폴백
        }
        filters.add(
            value != null
                ? "{\"id\":\"" + statId + "\",\"value\":{\"min\":" + value + "}}"
                : "{\"id\":\"" + statId + "\"}");
      }
    }
    if (filters.isEmpty()) {
      return null;
    }
    String typePart =
        baseNameKo != null && !baseNameKo.isBlank()
            ? "\"type\":\"" + baseNameKo.replace("\"", "") + "\","
            : "";
    // status=securable = 상단 드롭다운 "즉시 구입" — online(직접 거래)은 자리 비운 판매자를 기다려야 해 구매 동선이 나쁘다(사용자 피드백).
    // sale_type 필터는 status 와 별개 축이라 드롭다운을 못 바꾼다(1차 시도 실패 교훈) — securable 로 대체.
    return "{\"query\":{\"status\":{\"option\":\"securable\"},"
        + typePart
        + "\"stats\":[{\"type\":\"and\",\"filters\":["
        + String.join(",", filters)
        + "]}]},\"sort\":{\"price\":\"asc\"}}";
  }

  /**
   * 잡 키워드(EN) → 거래소 임플리싯 사전(ko 텍스트) 매칭용 한글 조각 — **피해(DPS) 관련만**. 광의 "damage" 는 수백 건이 걸려 제외.
   * life/regen(자기연소 스킬의 최적화기 키워드)은 매핑하지 않는다 — 마나 재생/생명력 획득 류 비DPS 임플리싯이 유효 옵션으로 섞였다(사용자 피드백). ehp
   * 잡의 방어 키워드(resistance/armour 등)도 동일 이유로 제외 — 이 필터는 "이 유니크를 왜 쓰는가"(피해 스케일링)에 맞춘다.
   */
  private static final Map<String, String> KO_KEYWORD_BY_EN =
      Map.ofEntries(
          Map.entry("fire", "화염"),
          Map.entry("cold", "냉기"),
          Map.entry("lightning", "번개"),
          Map.entry("chaos", "카오스"),
          Map.entry("physical", "물리"),
          Map.entry("spell", "주문"),
          Map.entry("cast speed", "시전 속도"),
          Map.entry("attack", "공격"),
          Map.entry("projectile", "투사체"),
          Map.entry("area", "광역"),
          Map.entry("minion", "소환수"),
          Map.entry("critical", "치명타"),
          Map.entry("damage over time", "지속 피해"),
          Map.entry("burning", "화상"),
          Map.entry("totem", "토템"),
          Map.entry("trap", "덫"),
          Map.entry("mine", "지뢰"),
          Map.entry("brand", "낙인"));

  /** 임플리싯 그룹 필터 상한 — 60은 거래소 서버가 "쿼리가 너무 복잡합니다"로 거부(실측). 길이순 상위 30에 핵심 전부 포함. */
  private static final int UNIQUE_IMPLICIT_FILTER_CAP = 30;

  /** 유니크 템플릿 줄에 롤 범위 "(x-y)" 가 있는지 — 하한 음수 허용. */
  private static boolean hasRollRange(String line) {
    return line != null && line.matches(".*\\(-?\\d+(?:\\.\\d+)?[-~–]-?\\d+(?:\\.\\d+)?\\).*");
  }

  /**
   * 유니크의 거래소 검색 쿼리 — 고유명(daum 서버는 한글명, 없으면 영문) + 즉시 구입 + 가격순. 변형/롤 유니크(옵션이 매물마다 다르거나 일부만 달림)는 이름만으로
   * 검색하면 빌드에 무효한 옵션 매물이 섞이므로, 이번 잡 키워드(스킬 태그·직업 유래 — 엘드리치 선택과 동일 기준)에 유효한 옵션 라인을 존재 필터로 함께 건다(사용자
   * 요청). 롤 수치는 필터하지 않는다(매물 폭 유지) — 어떤 옵션이 달렸느냐만 거른다.
   *
   * <p>결합(Synthesis) 계열 — "고정 속성 부여 규모 증가"(Implicit Modifier magnitudes) 익스플리싯을 가진 유니크(성운 등)는
   * **임플리싯이 매물마다 랜덤**이고 그게 아이템의 존재 이유라, 빌드 키워드에 맞는 임플리싯을 "1개 이상 보유" count 그룹으로 건다(사용자 재지적 — 익스플리싯
   * 필터는 고정 라인이라 아무것도 못 거른다).
   */
  private String uniqueTradeQuery(PoeUniqueItem unique) {
    String name =
        unique.nameKo() != null && !unique.nameKo().isBlank() ? unique.nameKo() : unique.name();
    if (name == null || name.isBlank()) {
      return null;
    }
    List<String> filters = new ArrayList<>();
    List<String> en = unique.explicits() != null ? unique.explicits() : List.of();
    List<String> ko = unique.explicitsKo() != null ? unique.explicitsKo() : List.of();
    boolean implicitMagnitude = false;
    for (int i = 0; i < en.size() && i < ko.size(); i++) {
      if (en.get(i) != null && en.get(i).contains("Implicit Modifier magnitude")) {
        implicitMagnitude = true;
      }
      if (ko.get(i) == null) {
        continue;
      }
      // 롤 범위 "(60-120)%" 있는 옵션**만** 검색에 포함(사용자 확정 규칙) — 매물마다 수치가 달라 사용자가
      // min 만 입력해 재검색하는 대상. 고정 문구 옵션은 전 매물이 동일해 필터 가치가 없고 패널만 어지럽힌다.
      // 비활성(disabled) 포함은 사이트 패널에서 안 보여 무의미했다(1차 시도 실패).
      // 하한이 음수인 범위 "(-35-35)%" 도 롤(전창조 지속시간 실측) — 선행 - 허용.
      // ko 는 여러 EN 줄이 한 줄로 병합되며 범위 표기가 사라지는 데이터가 있어(빛나는 묘약 "(1-2)초" 실측)
      // EN 대응 줄 + (마지막 ko 줄이면) 병합된 꼬리 EN 줄까지 보고 판정한다.
      boolean rolled = hasRollRange(ko.get(i)) || hasRollRange(en.get(i));
      if (!rolled && i == ko.size() - 1) {
        for (int j = i + 1; j < en.size() && !rolled; j++) {
          rolled = hasRollRange(en.get(j));
        }
      }
      if (!rolled) {
        continue;
      }
      // trade 사전 텍스트는 "#%" — 범위를 단일 # 로 접어 조회
      String flatKo = ko.get(i).replaceAll("\\(-?\\d+(?:\\.\\d+)?[-~–]-?\\d+(?:\\.\\d+)?\\)", "#");
      String statId = poeTradeStatDataService.statIdFor(flatKo);
      if (statId != null) {
        filters.add("{\"id\":\"" + statId + "\"}");
      }
    }
    List<String> groups = new ArrayList<>();
    boolean weighted = false;
    if (implicitMagnitude) {
      List<String> koKeys =
          currentKeywords.stream()
              .map(KO_KEYWORD_BY_EN::get)
              .filter(java.util.Objects::nonNull)
              .toList();
      List<String> implicitIds = poeTradeStatDataService.implicitIdsMatching(koKeys);
      if (!implicitIds.isEmpty()) {
        // 가중 합계(weight2) 그룹 — min:1 로 "유효 고정 속성 1개 이상" 필터를 겸하면서, 정렬을 이 그룹의
        // 합(값×가중치)으로 걸어 좋은 고정 속성이 크게 붙은 매물부터 보여준다(가격순은 싼-무효 매물이 상단 — 사용자 피드백).
        // 후보 값이 대부분 % 스케일이라 가중치는 일괄 1(값 자체가 곧 크기).
        String group =
            implicitIds.stream()
                .limit(UNIQUE_IMPLICIT_FILTER_CAP)
                .map(id -> "{\"id\":\"" + id + "\",\"value\":{\"weight\":1}}")
                .collect(java.util.stream.Collectors.joining(","));
        // 정렬 키가 statgroup.0(첫 그룹) 이라 weight2 그룹을 반드시 맨 앞에 둔다
        groups.add("{\"type\":\"weight2\",\"value\":{\"min\":1},\"filters\":[" + group + "]}");
        weighted = true;
      }
    }
    if (!filters.isEmpty()) {
      groups.add("{\"type\":\"and\",\"filters\":[" + String.join(",", filters) + "]}");
    }
    String stats = groups.isEmpty() ? "" : ",\"stats\":[" + String.join(",", groups) + "]";
    String sort = weighted ? "{\"statgroup.0\":\"desc\"}" : "{\"price\":\"asc\"}";
    return "{\"query\":{\"status\":{\"option\":\"securable\"},\"name\":\""
        + name.replace("\"", "")
        + "\""
        + stats
        + "},\"sort\":"
        + sort
        + "}";
  }

  /** 고유 아이템 표시용 모드 라인 (임플리싯 + 익스플리싯, 한국어 우선·없으면 영문). 레어와 표시 일관성. */
  private List<String> uniqueModLines(PoeUniqueItem unique) {
    List<String> lines = new ArrayList<>();
    mergeLocaleLines(lines, unique.implicits(), unique.implicitsKo());
    mergeLocaleLines(lines, unique.explicits(), unique.explicitsKo());
    return lines;
  }

  /** uniqueModLines 의 영문판 — EN 로케일 결과 표시용. */
  private List<String> uniqueModLinesEn(PoeUniqueItem unique) {
    List<String> lines = new ArrayList<>();
    if (unique.implicits() != null) {
      lines.addAll(unique.implicits());
    }
    if (unique.explicits() != null) {
      lines.addAll(unique.explicits());
    }
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

  /** anointLineKo 의 영문판 — EN 로케일 결과 표시용. */
  private List<String> anointLineEn(Slot slot) {
    AnointPick pick = currentAnoint;
    return slot == Slot.AMULET && pick != null
        ? List.of("(anoint) Allocates " + pick.name())
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
      text.append(withMaxRoll(implicit)).append("\n");
    }
    for (String implicit : extras) {
      text.append(withMaxRoll(implicit)).append("\n");
    }
    for (String explicit : item.explicits()) {
      text.append(withMaxRoll(explicit)).append("\n");
    }
    // 타임리스(무궁한) 주얼은 시드 줄이 있어야 PoB 가 반경 패시브 변환을 계산한다(없으면 평범한 스탯 주얼).
    for (String line : timelessLines(item)) {
      text.append(line).append("\n");
    }
    return text.toString();
  }

  /**
   * 해로운 모드 휴리스틱 — 실빌드는 이로운 모드 최대롤·해로운 모드 최소롤을 산다. 전면 {range:1}은 해로운 모드 과대 악화 왜곡(실측: 전면 최대화에서 RF
   * -22%·arc -14% 하락)이라 방향별로 나눈다.
   */
  private static final java.util.regex.Pattern HARMFUL_MOD_LINE =
      java.util.regex.Pattern.compile(
          "(?i)(damage taken|takes? \\d|increased damage taken|reduced (life|energy shield|armour|evasion"
              + "|maximum|resistance|attack speed|cast speed|movement speed)|less (life|energy shield|armour|damage)"
              + "|receive|lose \\()");

  /**
   * 유니크 롤 정합 — 범위 표기 "(a-b)" 모드 라인에 PoB range 프리픽스: 이로운 모드 {range:1}(최대롤), 해로운 모드 {range:0}(최소롤).
   * 미지정 시 PoB 는 중간롤(0.5)로 계산하는데 실빌드 96+ 는 좋은 롤 전제라 과소평가된다(실측: 동일 Grace of the Goddess 대표 롤이 우리
   * 중간롤보다 DPS +7.8%). 21/20 젬·최상위 레어와 같은 엔드게임 전제 계열. {range} 문법 검증: 최소 1,768 < 중간 1,798 < 최대
   * 1,827(단조).
   */
  private String withMaxRoll(String line) {
    if (line == null || !line.matches(".*\\(\\d+(?:\\.\\d+)?-\\d+(?:\\.\\d+)?\\).*")) {
      return line;
    }
    return (HARMFUL_MOD_LINE.matcher(line).find() ? "{range:0}" : "{range:1}") + line;
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
    Map<Integer, Equipped> jewels = new LinkedHashMap<>();
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
                        Equipped.ofUnique(
                            spec.length >= 2
                                ? withTimeless(
                                    u, spec[1].trim(), spec.length >= 3 ? spec[2].trim() : null)
                                : u)));
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
        // evaluateTree 의 주얼은 전부 유니크(jewelSlugs → ofUnique)라 unique() 안전.
        jewels.values().stream()
            .map(Equipped::unique)
            .map(u -> new TreeJewel(u.slug(), u.name(), u.nameKo()))
            .toList(),
        standardWeapon(gem),
        standardWeapon(gem) == null
            ? null
            : poeBaseItemDataService
                .findByName(standardWeapon(gem))
                .map(PoeBaseItem::nameKo)
                .orElse(null));
  }
}
