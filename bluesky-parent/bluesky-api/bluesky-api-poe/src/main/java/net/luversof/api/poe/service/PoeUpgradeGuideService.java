package net.luversof.api.poe.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;
import java.util.zip.Deflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 업그레이드 가이드 — 사용자가 붙여넣은 PoB 코드를 <b>기준선</b>으로 두고 "무엇을 바꾸면 얼마나 좋아지는가"를 측정해 순위로 알려준다.
 *
 * <p>최적화기가 '처음부터 최적 빌드를 만드는' 도구라면 이건 '지금 내 빌드에서 다음 한 걸음'을 알려주는 도구다. 그래서 전부 갈아엎는 제안이 아니라 <b>한 번에 하나씩
 * 바꿀 수 있는 단위</b>로만 제안하고, 측정한 제안은 증감(%)을 달고 나간다.
 *
 * <p>장비는 두 단계로 본다 — ① 칸마다 빼 보고 기여가 작은 칸을 고른 뒤 ② 그 칸에 고유 아이템 후보를 하나씩 끼워 실측한다. 칸 순위는 ②의 실측 이득으로
 * 매긴다("어느 칸부터 바꾸는 게 가장 효과적인가"는 빼서 떨어지는 폭이 아니라 바꿔서 오르는 폭이 답이다).
 *
 * <p>평가는 반드시 {@link PoePobEngineService#calculateValuesIsolated} 로 한다 — 외부 빌드를 상주 워커에 실으면 PoB 내부
 * 상태가 무너져 다음 빌드의 스펙 임포트가 조용히 실패한다(최적화기까지 오염).
 *
 * <p>DPS · EHP · 최약 최대피격을 <b>따로</b> 보고한다. 합성 목표값 하나만 보이면 "목표값은 올랐는데 DPS 는 내려가 고장처럼 보이는" 일이 생긴다(완주
 * 모드 실측 2026-09-29: 목표값 +11.5% 인데 표시 DPS -7.6%).
 *
 * <p>⚠ XML 은 <b>PoB 가 실제로 쓰는 모양</b>으로 읽는다. PoB 는 속성을 Lua 해시 순서로 적어 {@code <Slot itemId="10"
 * name="Belt"/>} 처럼 name 이 뒤에 오기도 한다(xml.lua 의 pairs). 첫 판은 {@code <Slot name="…"} 로 찾아서, 우리 최적화기가
 * 만든 코드(name 이 앞)로는 통과했지만 실제 사용자 코드에선 모든 칸을 '빈 칸'으로 읽었다(2026-09-29 PoB SaveDB 출력으로 확인). 태그는 속성 순서와
 * 무관하게 찾는다.
 */
@Service
public class PoeUpgradeGuideService {

  private static final Logger logger = LoggerFactory.getLogger(PoeUpgradeGuideService.class);

  /**
   * 동시 평가 수 — 격리 경로는 호출마다 luajit 프로세스(단일 스레드)를 띄운다. 이 서버는 논리 코어 28개·메모리 64GB 라 8 로 둔다(레어 목표까지 재면
   * 평가가 200회 안팎이라 4 로는 3분을 넘었다).
   */
  /**
   * 평가 동시 수 — 평가마다 엔진 프로세스를 새로 띄워(calculateValuesIsolated) CPU 가 병목이다. 코어 수 - 4(최소 8, 예전 고정값).
   * 10-02 실빌드 표본(평가 197회): 8 → 93초, 16 → 75초, 24(28 코어) → 60초, 결과 JSON 은 셋 다 같다(평가가 서로 독립).
   */
  private static final int PARALLEL = Math.max(8, Runtime.getRuntime().availableProcessors() - 4);

  /** 보조젬 교체 후보 상한 — 태그로 추린 뒤 주 스킬과 태그가 많이 겹치는 순으로 자른다(잡 1회를 수 분 안에). */
  private static final int SWAP_CANDIDATES = 48;

  /** 효과가 이보다 작은 교체는 제안하지 않는다(측정 잡음 수준). */
  private static final double MIN_GAIN_PCT = 1.0;

  /** 고유 아이템을 끼워 볼 약한 칸 수(기여가 작은 순, 무기 제외). 칸당 후보가 최대 14개라 평가가 최대 56회 는다. */
  private static final int PICK_SLOTS = 4;

  /** 칸당 후보 — 실빌드 인기(정확한 전직·주 스킬 조합) · 주 스킬 공격 키워드 상위 · 방어 키워드 상위. */
  private static final int META_CANDIDATES = 4;

  private static final int OFFENCE_CANDIDATES = 5;
  private static final int DEFENCE_CANDIDATES = 5;

  /** 다른 축을 이보다 많이 깎는 교체는 업그레이드가 아니라 맞바꿈이라 추천에서 뺀다. */
  private static final double TRADE_TOLERANCE_PCT = 5.0;

  /** 보조젬 사전필터 — 스킬에 없는 아키타입 태그를 가진 보조는 적용될 수 없다(최적화기 supportCompatible 과 같은 규칙). */
  private static final List<String> ARCHETYPE_TAGS =
      List.of("Minion", "Trap", "Mine", "Totem", "Brand", "Warcry", "Bow", "Hex", "Curse");

  private static final List<String> EQUIPMENT_SLOTS =
      List.of(
          "Weapon 1",
          "Weapon 2",
          "Helmet",
          "Body Armour",
          "Gloves",
          "Boots",
          "Amulet",
          "Ring 1",
          "Ring 2",
          "Belt");

  private static final Map<String, String> SLOT_KO = new LinkedHashMap<>();

  static {
    SLOT_KO.put("Weapon 1", "무기");
    SLOT_KO.put("Weapon 2", "보조 무기/방패");
    SLOT_KO.put("Helmet", "투구");
    SLOT_KO.put("Body Armour", "갑옷");
    SLOT_KO.put("Gloves", "장갑");
    SLOT_KO.put("Boots", "장화");
    SLOT_KO.put("Amulet", "목걸이");
    SLOT_KO.put("Ring 1", "반지 1");
    SLOT_KO.put("Ring 2", "반지 2");
    SLOT_KO.put("Belt", "허리띠");
  }

  /**
   * 고유 아이템 후보를 끼워 볼 칸 → 고유 데이터의 category. 무기 칸은 없다 — 무기를 바꾸면 스킬이 요구하는 무기 종류와 보조 칸(방패/화살통)까지 얽혀 한 칸
   * 교체로 끝나지 않는다.
   */
  private static final Map<String, String> SLOT_CATEGORY =
      Map.of(
          "Helmet", "helmet",
          "Body Armour", "body",
          "Gloves", "gloves",
          "Boots", "boots",
          "Amulet", "amulet",
          "Ring 1", "ring",
          "Ring 2", "ring",
          "Belt", "belt");

  private static final List<String> DEFENCE_KEYWORDS =
      List.of(
          "maximum life", "energy shield", "resistance", "armour", "evasion", "block", "suppress");

  /**
   * 기준선 지표. maxHit = 다섯 피해 유형 최대피격 중 최솟값(가장 약한 곳). minionEhp = 주 스킬 소환수의 EHP(calc.lua 의
   * MinionTotalEHP) — 소환수 빌드가 아니면 0 이고, 그러면 소환수 축은 어디에도 끼지 않는다.
   */
  public record Metrics(double dps, double ehp, double maxHit, double minionEhp) {}

  /**
   * 약한 칸에 끼워 재 본 교체 후보 한 개 — 고유(보통 롤) 또는 같은 베이스의 2티어 레어 목표.
   *
   * @param rarity UNIQUE | RARE
   * @param slug 고유 아이템 slug(레어면 null)
   * @param name 고유 아이템 영문 이름(레어면 null)
   * @param baseType 베이스 영문 이름
   * @param mods 레어 목표에 붙인 옵션(표시용 한국어, 2티어 범위) — 고유면 null
   * @param minionPct 소환수 EHP 증감(%) — 소환수 빌드가 아니면 null
   * @param metaCount 이 전직·주 스킬 조합의 poe.ninja 캐릭터 중 이 아이템을 쓰는 수(모르면 0)
   * @param metaTotal 그 조합의 캐릭터 수(모르면 0)
   * @param needs 이 교체로 새로 모자라게 되는 요구 능력치("힘 25 · 민첩 34") — 다른 곳에서 채워야 끼울 수 있다. 그대로 끼울 수 있으면 null
   * @param itemText 레어 목표의 PoB 아이템 텍스트(엔진이 잰 그 아이템 — PoB "Create custom" 에 붙여 넣기, 10-02) — 고유면 null
   */
  public record ItemPick(
      String rarity,
      String slug,
      String name,
      String nameKo,
      String baseType,
      String baseTypeKo,
      List<String> mods,
      double dpsPct,
      double ehpPct,
      double maxHitPct,
      Double minionPct,
      int metaCount,
      int metaTotal,
      String needs,
      String itemText) {}

  /**
   * 제안 한 건.
   *
   * @param kind free(측정 없이 확정되는 공짜 수정) · support(보조젬 교체) · item(약한 장비 칸)
   * @param dpsPct 측정한 DPS 증감(%) — 측정 없는 제안이면 null
   * @param minionPct 측정한 소환수 EHP 증감(%) — 측정 없는 제안이거나 소환수 빌드가 아니면 null
   * @param picks item 제안일 때 그 칸에 끼워 재 본 고유 아이템 추천(없으면 빈 목록, 다른 종류면 null)
   */
  public record Suggestion(
      String kind,
      String title,
      String detail,
      Double dpsPct,
      Double ehpPct,
      Double maxHitPct,
      Double minionPct,
      List<ItemPick> picks) {}

  public record GuideResult(
      String className,
      String ascendancy,
      int level,
      String mainSkill,
      Metrics baseline,
      List<Suggestion> suggestions,
      int evaluations,
      long durationMs) {}

  public record GuideStatus(
      boolean running, int done, int total, String phase, GuideResult result, String error) {}

  /** 자동 다듬기 한 단계 — 적용한 교체와 그 단계에서 오른 폭(직전 빌드 대비 %). */
  public record RefineStep(String label, double dpsPct, double ehpPct, double maxHitPct) {}

  /**
   * 자동 다듬기 결과 — 빌드(보통 poe.ninja 실빌드 출발점)에 가이드가 실측한 교체안을 한 번에 하나씩 실제로 적용하고 다시 재기를 반복한 것.
   *
   * @param code 다듬은 빌드 PoB 코드(저장 스탯은 엔진 최종 값 — 빌드 화면 요약이 저장 스탯을 읽는다)
   */
  public record RefineResult(
      Metrics before,
      Metrics after,
      List<RefineStep> steps,
      String code,
      int evaluations,
      long durationMs) {}

  public record RefineStatus(
      boolean running,
      int round,
      int rounds,
      String phase,
      int done,
      int total,
      RefineResult result,
      String error) {}

  /** 가이드가 실측한 교체안 중 그대로 적용할 수 있는 것(교체한 빌드 XML 포함) — 자동 다듬기 중에만 모은다. */
  private record Applicable(
      String label, String xml, double dpsPct, double ehpPct, double maxHitPct) {}

  /** 주 스킬 그룹 안의 젬 하나 — 원문 위치(start/end)를 들고 다니며 그 자리만 바꾼다. */
  private record GemRef(int start, int end, String nameSpec, boolean enabled, PoeGem gem) {}

  /** 칸을 빼면 줄어드는 몫(%) — minionShare 는 소환수 빌드가 아니면 null. */
  private record SlotShare(String slot, double dpsShare, double ehpShare, Double minionShare) {}

  private record Trial(PoeUniqueItem item, Future<Map<String, Double>> result) {}

  /**
   * @param rare 이 칸의 2티어 레어 목표 실측(베이스를 몰라 못 쟀으면 null)
   * @param rareShown 추천 기준을 넘어 목록에 보였는지
   */
  private record SlotPicks(
      SlotShare share,
      List<ItemPick> picks,
      int tried,
      int blocked,
      ItemPick rare,
      boolean rareShown) {}

  /** 한 칸의 레어 목표 실측 — 빈 레어(강제 옵션만)와 후보 옵션을 하나씩 얹은 것들. */
  private record RareTrial(
      PoeRareTargetService.Plan plan,
      Future<Map<String, Double>> blank,
      List<Future<Map<String, Double>>> singles) {}

  /** 옵션 하나의 이득 — 빈 레어 대비 DPS% + EHP%(기준선 대비 백분율). */
  private record ScoredAffix(PoeRareTargetService.Affix affix, double gain) {}

  private record RareCombo(
      PoeRareTargetService.Plan plan,
      List<ScoredAffix> scored,
      List<PoeRareTargetService.Affix> chosen,
      Future<Map<String, Double>> result) {}

  private final PoePobImportService importService;
  private final PoePobEngineService engine;
  private final PoeGemDataService gemData;
  private final PoeEngineUnmodeledDataService unmodeled;
  private final PoeUniqueDataService uniqueData;
  private final PoeMetaPopularityService meta;
  private final PoeRareTargetService rareTargets;
  private final PoeMercenaryService mercenary;

  private final AtomicBoolean running = new AtomicBoolean();
  private final AtomicInteger done = new AtomicInteger();
  private volatile int total;
  private volatile String phase = "";
  private volatile GuideResult lastResult;
  private volatile String lastError;

  /**
   * 자동 다듬기 최대 단계 — 가이드 1회가 75~95초라 3단계면 약 5분(최적화 잡과 비슷한 체감). 단계마다 가장 이득이 큰 교체 하나만 적용하고 다시 잰다(한꺼번에
   * 적용하면 서로의 문맥을 바꿔 합이 예측과 달라진다).
   */
  static final int REFINE_ROUNDS = 3;

  /**
   * 자동 다듬기가 받아들이는 한 축 손해 한도(%). 가이드 추천(맞바꿈 5%)보다 엄격하다 — 사람이 고르는 추천과 달리 자동 적용은 되돌리는 사람이 없고, 실빌드 출발점의
   * 강점(생존)을 조금씩 깎아 DPS 로 바꾸는 표류를 막아야 한다.
   */
  static final double REFINE_TOLERANCE_PCT = 2.0;

  /** 자동 다듬기 중에만 non-null — 가이드 측정 루프가 적용 가능한 교체안을 여기 넣는다(일반 가이드 실행은 동작 무변화). */
  private volatile List<Applicable> collector;

  private volatile boolean refining;
  private volatile int refineRound;
  private volatile RefineResult lastRefine;
  private volatile String lastRefineError;

  public PoeUpgradeGuideService(
      PoePobImportService importService,
      PoePobEngineService engine,
      PoeGemDataService gemData,
      PoeEngineUnmodeledDataService unmodeled,
      PoeUniqueDataService uniqueData,
      PoeMetaPopularityService meta,
      PoeRareTargetService rareTargets,
      PoeMercenaryService mercenary) {
    this.importService = importService;
    this.engine = engine;
    this.gemData = gemData;
    this.unmodeled = unmodeled;
    this.uniqueData = uniqueData;
    this.meta = meta;
    this.rareTargets = rareTargets;
    this.mercenary = mercenary;
  }

  public GuideStatus status() {
    return new GuideStatus(running.get(), done.get(), total, phase, lastResult, lastError);
  }

  /** 분석 잡 시작 — 이미 돌고 있거나 코드를 못 읽으면 false(사유는 status().error). */
  public boolean start(String code) {
    return start(code, null);
  }

  /**
   * @param mercCode 루미너리 용병 빌드 PoB 코드(선택) — 있으면 용병의 오라·저주를 파티 탭으로 넣은 빌드를 기준선으로 모든 측정을 한다({@link
   *     PoeMercenaryService})
   */
  public boolean start(String code, String mercCode) {
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    String xml;
    PoeMercenaryService.MercBuffs merc = null;
    try {
      xml = importService.decodeToXml(code);
    } catch (RuntimeException e) {
      lastError = "PoB 코드를 읽지 못했습니다: " + e.getMessage();
      lastResult = null;
      running.set(false);
      return false;
    }
    if (mercCode != null && !mercCode.isBlank()) {
      try {
        merc = mercenary.export(mercCode, PoeMercenaryService.hasBestowedKnighthood(xml));
      } catch (RuntimeException e) {
        lastError = "용병 빌드를 계산하지 못했습니다: " + e.getMessage();
        lastResult = null;
        running.set(false);
        return false;
      }
    }
    final PoeMercenaryService.MercBuffs mercBuffs = merc;
    lastError = null;
    lastResult = null;
    done.set(0);
    total = 1;
    phase = "기준선";
    Thread thread =
        new Thread(
            () -> {
              try {
                lastResult = analyze(xml, mercBuffs);
              } catch (Throwable e) {
                logger.warn("업그레이드 가이드 실패", e);
                lastError = "분석 실패: " + e.getMessage();
              } finally {
                phase = "";
                running.set(false);
              }
            },
            "poe-upgrade-guide");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private GuideResult analyze(String rawXml, PoeMercenaryService.MercBuffs merc) throws Exception {
    long startedAt = System.currentTimeMillis();
    ExecutorService pool = Executors.newFixedThreadPool(PARALLEL);
    try {
      // 용병이 있으면 그 버프를 넣은 빌드가 기준선이다 — 이후 모든 교체 측정이 용병 오라를 받은 상태에서 이뤄진다
      String xml = merc == null ? rawXml : PoeMercenaryService.withParty(rawXml, merc);
      Future<Map<String, Double>> withoutMerc = null;
      if (merc != null) {
        total += 1;
        withoutMerc = pool.submit(() -> eval(PoeMercenaryService.withParty(rawXml, EMPTY_MERC)));
      }
      Map<String, Double> baseValues = eval(xml);
      Metrics base = metricsOf(baseValues);
      double baseDeficit = attributeDeficit(baseValues);
      List<Suggestion> suggestions = new ArrayList<>();

      String buildTag = tagOf(xml, "<Build ");
      String className = attr(buildTag, "className");
      String ascendancy = attr(buildTag, "ascendClassName");
      int level = parseInt(attr(buildTag, "level"), 100);

      // ── 주 스킬 그룹 ──
      List<int[]> blocks = skillBlocks(xml);
      int mainIndex = parseInt(attr(buildTag, "mainSocketGroup"), 1) - 1;
      int[] mainBlock = mainIndex >= 0 && mainIndex < blocks.size() ? blocks.get(mainIndex) : null;
      List<GemRef> mainGems = mainBlock == null ? List.of() : gemsIn(xml, mainBlock);
      GemRef activeGem =
          mainGems.stream()
              .filter(g -> g.gem() != null && !g.gem().isSupport())
              .findFirst()
              .orElse(null);
      List<GemRef> supports =
          mainGems.stream()
              .filter(g -> g.gem() != null && g.gem().isSupport() && g.enabled())
              .toList();
      String mainSkill = activeGem == null ? "?" : label(activeGem.gem());
      double baseUnapplied = baseValues.getOrDefault("UnappliedSupportCount", 0d);
      // 한 번 불러 두면 남는 소환수(망령·좀비·수호자·골렘 …: Minion 이고 Duration 이 없다)는 오라를 켜기 전에 소환하면 되므로
      // 주 스킬 자신의 비용 경고는 인게임 제약이 아니다(망령 빌드에서 '스킬 비용 부족' 오탐으로 확인, 2026-09-29).
      boolean castOnce = activeGem != null && isCastOnceMinion(activeGem.gem());

      // ── 0) 용병 — 넣은 버프와 그 몫(용병 없음 대비) ──
      if (merc != null) {
        Metrics without = metricsOf(withoutMerc.get());
        suggestions.add(
            new Suggestion(
                "merc",
                merc.isEmpty() ? "용병 버프 없음" : "용병 버프 반영: " + merc.summary(),
                mercDetail(merc),
                pct(without.dps(), base.dps()),
                pct(without.ehp(), base.ehp()),
                pct(without.maxHit(), base.maxHit()),
                minionPct(without, base),
                null));
      }

      // ── 1) 공짜 수정 — 측정 없이 확정되는 것 ──
      phase = "공짜 수정 점검";
      // 1-a) 인게임에서 적용되지 않는 보조젬 — 엔진 판정(UnappliedSupportCount)으로 이름을 가린다.
      if (baseUnapplied > 0 && !supports.isEmpty()) {
        total += supports.size();
        List<Future<Map<String, Double>>> probes = new ArrayList<>();
        for (GemRef s : supports) {
          String probeXml = disableGem(xml, s);
          probes.add(pool.submit(() -> eval(probeXml)));
        }
        for (int i = 0; i < supports.size(); i++) {
          if (probes.get(i).get().getOrDefault("UnappliedSupportCount", 0d) < baseUnapplied) {
            suggestions.add(
                new Suggestion(
                    "free",
                    "효과 없는 보조젬: " + label(supports.get(i).gem()),
                    "이 보조젬은 "
                        + mainSkill
                        + "에 적용되지 않습니다(PoB 판정). 소켓만 차지하고 있어 다른 보조젬으로 바꾸는 게 이득입니다.",
                    null,
                    null,
                    null,
                    null,
                    null));
          }
        }
      }
      // 1-b) 빈 장비 칸 · 빈 플라스크 칸 — 아이템 세트가 없는 옛 형식이면 칸을 알 수 없어 건너뛴다(거짓 '빈 칸' 방지).
      int[] itemSet = activeItemSetRange(xml);
      if (itemSet != null) {
        for (String slot : EQUIPMENT_SLOTS) {
          if (!equipped(xml, itemSet, slot) && !"Weapon 2".equals(slot)) {
            // 양손 무기·활 등은 보조 칸이 비는 게 정상이라 Weapon 2 는 빼고 본다.
            suggestions.add(
                new Suggestion(
                    "free",
                    "빈 장비 칸: " + SLOT_KO.get(slot),
                    "아무것도 착용하지 않았습니다. 무엇이든 끼우면 이득입니다.",
                    null,
                    null,
                    null,
                    null,
                    null));
          }
        }
        int flasks = 0;
        for (int i = 1; i <= 5; i++) {
          if (equipped(xml, itemSet, "Flask " + i)) {
            flasks++;
          }
        }
        if (flasks < 5) {
          suggestions.add(
              new Suggestion(
                  "free",
                  "빈 플라스크 칸 " + (5 - flasks) + "개",
                  "플라스크 " + flasks + "/5 개만 착용 중입니다.",
                  null,
                  null,
                  null,
                  null,
                  null));
        }
      } else {
        logger.warn("업그레이드 가이드 — 아이템 세트가 없는 옛 형식이라 장비 분석을 건너뜁니다");
      }
      // 1-c) 엔진이 모델링하지 않는 전직(예: 사이온 루미너리 = 용병)
      if (ascendancy != null) {
        PoeEngineUnmodeledDataService.AscendancySummary summary = unmodeled.summaryOf(ascendancy);
        if (summary != null
            && summary.total() > 0
            && (double) summary.unmodeled() / summary.total() >= 0.4) {
          suggestions.add(
              new Suggestion(
                  "free",
                  "시뮬레이션 한계: " + ascendancy,
                  "이 전직 패시브 "
                      + summary.total()
                      + "개 중 "
                      + summary.unmodeled()
                      + "개를 Path of Building 이 계산하지 못합니다(예: 용병). 아래 수치에서 그 몫이 빠져 있습니다."
                      + ("Luminary".equals(ascendancy)
                          ? (merc != null
                              ? " 용병이 나에게 거는 오라·저주는 위에서 반영했고, 용병 자신의 딜·생존과 충직한 호위병(피해 분담)은 빠져 있습니다."
                              : " 빌드 화면의 '용병 PoB 코드'를 넣으면 용병이 거는 오라·저주를 반영합니다.")
                          : ""),
                  null,
                  null,
                  null,
                  null,
                  null));
        }
      }
      // 1-d) 요구 능력치 부족 — PoB 는 경고 문구만 띄우고 계산은 그대로 한다(BuildDisplayStats 의 "You do not meet the …
      //      requirement"). 인게임에선 요구치를 못 맞춘 젬·아이템이 작동하지 않으므로 기준선 수치부터 부풀어 있다.
      if (baseDeficit > 0.5) {
        List<String> lacking = new ArrayList<>();
        for (String[] a : ATTRIBUTES) {
          double need = shortfall(baseValues, a[0]);
          if (need > 0.5) {
            lacking.add(a[1] + " " + Math.round(need));
          }
        }
        suggestions.add(
            new Suggestion(
                "free",
                "요구 능력치 부족: " + String.join(" · ", lacking),
                "인게임에선 요구치를 못 맞춘 젬·아이템이 작동하지 않습니다. PoB 는 경고만 하고 계산하므로 아래 수치는 전부 작동한다고 본 값입니다.",
                null,
                null,
                null,
                null,
                null));
      }

      // 1-e) 주 스킬 비용 부족 — PoB 는 "You do not have enough Mana to use: …" 경고만 띄우고 계산은 그대로
      // 한다(Calcs.lua).
      if (!castOnce && baseValues.getOrDefault("MainSkillCostWarning", 0d) > 0) {
        suggestions.add(
            new Suggestion(
                "free",
                "스킬 비용 부족: " + mainSkill,
                "남은 마나(또는 생명력)로 이 스킬의 비용을 치를 수 없어 인게임에선 쓸 수 없습니다. 예약을 줄이거나 비용을 생명력으로 돌리세요. PoB 는 경고만 하고 계산하므로 아래 수치는 쓸 수 있다고 본 값입니다.",
                null,
                null,
                null,
                null,
                null));
      }

      // ── 2) 보조젬 — 가장 덜 일하는 보조를 찾아 더 나은 후보로 바꿔 본다 ──
      if (activeGem != null && !supports.isEmpty()) {
        phase = "보조젬 기여도 측정";
        total += supports.size();
        List<Future<Map<String, Double>>> removal = new ArrayList<>();
        for (GemRef s : supports) {
          String probeXml = disableGem(xml, s);
          removal.add(pool.submit(() -> eval(probeXml)));
        }
        GemRef weakest = null;
        double weakestLoss = Double.MAX_VALUE;
        double weakestDpsLoss = 0;
        double weakestMinionLoss = 0;
        int essential = 0;
        int assumed = 0;
        int party = 0;
        for (int i = 0; i < supports.size(); i++) {
          Map<String, Double> v = removal.get(i).get();
          if (breaksCost(baseValues, v, castOnce)) {
            essential++;
            // 빼면 스킬 비용을 못 치르게 되는 보조(예: 마나를 오라에 다 묶은 빌드의 생명력 전환) — PoB DPS 로는 16% 짜리로 보여도
            // 인게임에선 없으면 스킬을 못 쓰는 필수 젬이다(실빌드 족장 실측: 빼면 MainSkillCostWarning 0→1).
            continue;
          }
          if (suppliesAssumedBuff(xml, supports.get(i).gem())) {
            assumed++;
            continue;
          }
          if (isPartySupport(supports.get(i).gem())) {
            // 관대함 = 오라가 나에게는 안 걸리고 아군에게 더 세게 — 파티 지원 빌드다. 빼면 내 DPS 가 오르지만(PoB 가 나에게 오라를 건다)
            //   그건 업그레이드가 아니라 역할을 바꾸는 것(09-30 일괄: 성전사 모독 +7,067% · 사이온 강타 +2,137% · 투사 강타 +1,612%)
            party++;
            continue;
          }
          Metrics without = metricsOf(v);
          // 소환수 빌드는 소환수가 버티는 몫까지 기여로 센다 — DPS 만 보면 소환수 생명력 보조가 '기여 0%' 로 잡혀
          // 가장 먼저 빼라는 추천이 나온다(소환수가 죽으면 DPS 도 없다).
          //   두 몫은 더하지 않고 DPS × 소환수 EHP(소환수가 버티는 동안 넣는 피해)로 곱한다. 소환수 피해 보조는 피해 41% 더와
          //   소환수 생명력 25% 덜을 함께 준다(PoB support_minion_damage_minion_life_+%_final) — 기준선 대비로 더하면
          //   29.1% + (-33.3%) 로 생명력 쪽이 부풀려 순손해처럼 보이지만, 곱하면 1.41 × 0.75 = 약 +6% 로 제대로 잡힌다(망령 실측
          // 2026-09-30).
          //   소환수 빌드가 아니면 minionLoss 가 0 이라 예전(DPS 만)과 같다.
          double dpsLoss = base.dps() > 0 ? (base.dps() - without.dps()) / base.dps() : 0;
          double minionLoss =
              base.minionEhp() > 0
                  ? (base.minionEhp() - without.minionEhp()) / base.minionEhp()
                  : 0;
          double loss = 1 - (1 - dpsLoss) * (1 - minionLoss);
          if (loss < weakestLoss) {
            weakestLoss = loss;
            weakestDpsLoss = dpsLoss;
            weakestMinionLoss = minionLoss;
            weakest = supports.get(i);
          }
        }
        if (weakest != null) {
          phase = "보조젬 교체 후보 측정";
          List<PoeGem> candidates = supportCandidates(activeGem.gem(), mainGems);
          total += candidates.size();
          final GemRef target = weakest;
          List<Future<Map<String, Double>>> swaps = new ArrayList<>();
          for (PoeGem c : candidates) {
            String swapXml = replaceGem(xml, target, pobName(c));
            swaps.add(pool.submit(() -> eval(swapXml)));
          }
          PoeGem best = null;
          Metrics bestMetrics = null;
          double bestGain = Double.NEGATIVE_INFINITY;
          int minionCut = 0;
          int dpsCut = 0;
          for (int i = 0; i < candidates.size(); i++) {
            Map<String, Double> v = swaps.get(i).get();
            if (v.getOrDefault("UnappliedSupportCount", 0d) > baseUnapplied) {
              continue; // 인게임에서 적용 안 되는 교체는 버린다
            }
            if (sameFamilyLinked(candidates.get(i), supports, target)) {
              // 같은 계열(일반·상위·각성)이 이미 링크돼 있으면 인게임에선 함께 적용되지 않는다 — PoB 는 둘 다 적용해 부풀렸다
              //   (09-30 자동 다듬기: 데드아이 에테르 칼날에 상위 주문 메아리 + 각성한 주문 메아리가 차례로 붙음)
              continue;
            }
            if (needsText(baseValues, v) != null) {
              continue; // 요구 능력치를 못 맞추는 젬은 인게임에서 효과가 없다
            }
            if (breaksCost(baseValues, v, castOnce)) {
              continue; // 스킬 비용을 못 치르게 되는 교체 — 인게임에선 주 스킬을 못 쓴다
            }
            Metrics m = metricsOf(v);
            double dpsGain = pct(base.dps(), m.dps());
            Double minion = minionPct(base, m);
            if (minion != null) {
              // 소환수 빌드에서 한 축을 허용치 넘게 깎는 교체는 업그레이드가 아니라 맞바꿈이다(장비 추천과 같은 기준)
              if (minion < -TRADE_TOLERANCE_PCT) {
                minionCut++;
                continue;
              }
              if (dpsGain < -TRADE_TOLERANCE_PCT) {
                dpsCut++;
                continue;
              }
            }
            // 소환수 빌드는 약한 보조를 고른 기준(DPS × 소환수 EHP)과 같은 곱으로 가장 나은 후보를 고른다. 아니면 DPS 만.
            double gain =
                minion == null ? dpsGain : ((1 + dpsGain / 100) * (1 + minion / 100) - 1) * 100;
            if (minion == null) {
              offer(
                  "보조젬 교체: " + label(target.gem()) + " → " + label(candidates.get(i)),
                  replaceGem(xml, target, pobName(candidates.get(i))),
                  base,
                  m);
            }
            if (bestMetrics == null || gain > bestGain) {
              bestMetrics = m;
              bestGain = gain;
              best = candidates.get(i);
            }
          }
          if (best != null && bestGain >= MIN_GAIN_PCT) {
            boolean minionBuild = base.minionEhp() > 0;
            List<String> cut = new ArrayList<>();
            if (minionCut > 0) {
              cut.add("소환수 EHP 를 " + (int) TRADE_TOLERANCE_PCT + "% 넘게 깎는 후보 " + minionCut + "개");
            }
            if (dpsCut > 0) {
              cut.add("DPS 를 " + (int) TRADE_TOLERANCE_PCT + "% 넘게 깎는 후보 " + dpsCut + "개");
            }
            suggestions.add(
                new Suggestion(
                    "support",
                    "보조젬 교체: " + label(target.gem()) + " → " + label(best),
                    // 변수 뒤에 조사를 붙이면 받침에 따라 틀리고 공백이 끼어 어색해진다(화면 실측 2026-09-29) — 콜론으로 끊는다.
                    label(target.gem())
                        + (minionBuild
                            ? ": 지금 기여 DPS "
                                + String.format("%.1f", weakestDpsLoss * 100)
                                + "% · 소환수 EHP "
                                + String.format("%.1f", weakestMinionLoss * 100)
                                + "% — 딜과 소환수 생존을 곱하면 "
                                + String.format("%.1f", weakestLoss * 100)
                                + "%로 주 스킬 보조젬 가운데 가장 적습니다"
                            : ": 지금 DPS 기여 "
                                + String.format("%.1f", weakestLoss * 100)
                                + "%로 주 스킬 보조젬 가운데 가장 적습니다")
                        + (essential > 0 ? "(빼면 스킬 비용을 못 치르는 젬 " + essential + "개는 제외)" : "")
                        + (assumed > 0
                            ? "(설정에 켜 둔 버프·충전을 이 빌드에서 공급하는 젬 "
                                + assumed
                                + "개는 제외 — 빼면 인게임에선 그 버프가 사라진다)"
                            : "")
                        + (party > 0 ? "(파티 지원용 관대함 보조는 제외)" : "")
                        + "."
                        + (cut.isEmpty() ? "" : " " + String.join(" · ", cut) + "는 맞바꿈이라 뺐습니다."),
                    pct(base.dps(), bestMetrics.dps()),
                    pct(base.ehp(), bestMetrics.ehp()),
                    pct(base.maxHit(), bestMetrics.maxHit()),
                    minionPct(base, bestMetrics),
                    null));
          }
        }
      }

      if (itemSet != null) {
        suggestions.addAll(
            itemSuggestions(
                xml, itemSet, pool, base, baseValues, level, ascendancy, activeGem, castOnce));
      }

      return new GuideResult(
          className,
          ascendancy,
          level,
          mainSkill,
          base,
          List.copyOf(suggestions),
          done.get(),
          System.currentTimeMillis() - startedAt);
    } finally {
      pool.shutdownNow();
    }
  }

  /**
   * 장비 제안 — ① 칸마다 빼 보고 기여가 작은 칸을 고른 뒤 ② 그 칸에 고유 아이템 후보와 ③ 같은 베이스의 2티어 레어 목표를 끼워 실측한다. 칸 순위는 가장 좋았던
   * 추천의 이득(DPS% + EHP%) 순 — 그대로 끼울 수 있는 추천이 있는 칸이 먼저, 요구 능력치를 보충해야 하는 추천만 있는 칸이 다음, 더 나은 교체가 없던 칸은
   * 맨 뒤에 기여 순으로.
   */
  private List<Suggestion> itemSuggestions(
      String xml,
      int[] itemSet,
      ExecutorService pool,
      Metrics base,
      Map<String, Double> baseValues,
      int level,
      String ascendancy,
      GemRef activeGem,
      boolean castOnce)
      throws Exception {
    double baseUnapplied = baseValues.getOrDefault("UnappliedSupportCount", 0d);
    // ① 기여도 — 빼면 얼마나 떨어지나
    phase = "장비 칸 기여도 측정";
    List<String> equippedSlots = new ArrayList<>();
    for (String slot : EQUIPMENT_SLOTS) {
      if (equipped(xml, itemSet, slot)) {
        equippedSlots.add(slot);
      }
    }
    total += equippedSlots.size();
    List<Future<Map<String, Double>>> itemProbes = new ArrayList<>();
    for (String slot : equippedSlots) {
      String probeXml = removeSlot(xml, slot);
      itemProbes.add(pool.submit(() -> eval(probeXml)));
    }
    List<SlotShare> shares = new ArrayList<>();
    for (int i = 0; i < equippedSlots.size(); i++) {
      Metrics without = metricsOf(itemProbes.get(i).get());
      double dpsShare = base.dps() > 0 ? (base.dps() - without.dps()) / base.dps() * 100 : 0;
      double ehpShare = base.ehp() > 0 ? (base.ehp() - without.ehp()) / base.ehp() * 100 : 0;
      Double minionShare =
          base.minionEhp() > 0
              ? (base.minionEhp() - without.minionEhp()) / base.minionEhp() * 100
              : null;
      shares.add(new SlotShare(equippedSlots.get(i), dpsShare, ehpShare, minionShare));
    }
    // 약한 칸 = 세 몫의 합이 작은 칸 — 소환수 빌드면 소환수 생존을 받치는 칸(소환수 저항 반지 등)이 '약한 칸'으로 잘못 뽑히지 않는다
    shares.sort(
        Comparator.comparingDouble(
            s -> s.dpsShare() + s.ehpShare() + (s.minionShare() == null ? 0 : s.minionShare())));

    // ② 약한 칸에 고유 아이템 후보와 ③ 레어 목표(빈 레어 + 옵션 하나씩)를 한꺼번에 제출한다 — 평가가 모두 독립이라 병렬로 돈다
    phase = "교체 후보 측정";
    List<SlotShare> probeSlots =
        shares.stream().filter(s -> SLOT_CATEGORY.containsKey(s.slot())).limit(PICK_SLOTS).toList();
    Set<String> worn = wornUniqueNames(xml, itemSet);
    PoeMetaPopularityService.ItemUsage usage =
        activeGem == null
            ? PoeMetaPopularityService.ItemUsage.EMPTY
            : meta.itemUsage(ascendancy, activeGem.gem().name());
    List<String> offence = offenceKeywords(activeGem == null ? null : activeGem.gem());
    Map<String, List<Trial>> trials = new LinkedHashMap<>();
    Map<String, RareTrial> rareTrials = new LinkedHashMap<>();
    for (SlotShare s : probeSlots) {
      List<PoeUniqueItem> candidates =
          uniqueCandidates(SLOT_CATEGORY.get(s.slot()), level, offence, usage, worn);
      total += candidates.size();
      List<Trial> list = new ArrayList<>();
      for (PoeUniqueItem c : candidates) {
        String swapXml = equipItem(xml, s.slot(), uniqueItemText(c));
        list.add(new Trial(c, pool.submit(() -> eval(swapXml))));
      }
      trials.put(s.slot(), list);

      Optional<PoeRareTargetService.Plan> plan =
          rareTargets.plan(slotItemText(xml, itemSet, s.slot()), offence);
      if (plan.isEmpty()) {
        logger.info("업그레이드 가이드 — {}: 베이스나 모드 풀을 몰라 레어 목표를 건너뜀", s.slot());
        continue;
      }
      PoeRareTargetService.Plan p = plan.get();
      String blankXml = equipItem(xml, s.slot(), rareTargets.itemText(p, List.of()));
      Future<Map<String, Double>> blank = pool.submit(() -> eval(blankXml));
      List<Future<Map<String, Double>>> singles = new ArrayList<>();
      for (PoeRareTargetService.Affix a : p.candidates()) {
        String oneXml = equipItem(xml, s.slot(), rareTargets.itemText(p, List.of(a)));
        singles.add(pool.submit(() -> eval(oneXml)));
      }
      total += 1 + p.candidates().size();
      rareTrials.put(s.slot(), new RareTrial(p, blank, singles));
    }

    // ③ 레어 목표 한 벌 — 옵션 하나씩 얹어 잰 이득(빈 레어 대비)으로 접두·접미 상위를 고른다
    Map<String, RareCombo> combos = new LinkedHashMap<>();
    for (Map.Entry<String, RareTrial> e : rareTrials.entrySet()) {
      RareTrial rt = e.getValue();
      Metrics blankM = metricsOf(rt.blank().get());
      List<ScoredAffix> scored = new ArrayList<>();
      for (int i = 0; i < rt.plan().candidates().size(); i++) {
        Metrics m = metricsOf(rt.singles().get(i).get());
        // 소환수 빌드는 소환수 EHP 몫도 더한다(추천 판정과 같은 세 축) — 안 더하면 소환수 저항 같은 옵션을 못 고르고,
        // 그 옵션이 있던 지금 아이템보다 소환수 생존이 떨어져 레어 목표가 통째로 맞바꿈이 된다.
        double gain =
            (base.dps() > 0 ? (m.dps() - blankM.dps()) / base.dps() * 100 : 0)
                + (base.ehp() > 0 ? (m.ehp() - blankM.ehp()) / base.ehp() * 100 : 0)
                + (base.minionEhp() > 0
                    ? (m.minionEhp() - blankM.minionEhp()) / base.minionEhp() * 100
                    : 0);
        scored.add(new ScoredAffix(rt.plan().candidates().get(i), gain));
      }
      List<PoeRareTargetService.Affix> chosen = chooseAffixes(scored, rt.plan());
      String comboXml = equipItem(xml, e.getKey(), rareTargets.itemText(rt.plan(), chosen));
      total += 1;
      combos.put(
          e.getKey(), new RareCombo(rt.plan(), scored, chosen, pool.submit(() -> eval(comboXml))));
    }
    phase = "레어 목표 측정";
    Map<String, ItemPick> rarePicks = new LinkedHashMap<>();
    for (Map.Entry<String, RareCombo> e : combos.entrySet()) {
      RareCombo c = e.getValue();
      List<PoeRareTargetService.Affix> chosen = c.chosen();
      Map<String, Double> v = c.result().get();
      // 요구 능력치가 모자라게 되면 — 모자란 능력치 옵션이 이 베이스에 붙으면 이득이 가장 작은 접미 하나를 그걸로 바꿔 한 번 더 잰다
      List<PoeRareTargetService.Affix> fixed = withAttributeFix(c, baseValues, v);
      if (fixed != null) {
        String fixXml = equipItem(xml, e.getKey(), rareTargets.itemText(c.plan(), fixed));
        total += 1;
        Map<String, Double> fv = pool.submit(() -> eval(fixXml)).get();
        // 보충이 완전히 풀렸을 때만 바꾼다 — 덜 풀렸으면 원래 한 벌(이득이 더 큰 쪽)에 '보충 필요'를 달아 보인다
        if (needsText(baseValues, fv) == null) {
          chosen = fixed;
          v = fv;
        }
      }
      if (v.getOrDefault("UnappliedSupportCount", 0d) > baseUnapplied
          || breaksCost(baseValues, v, castOnce)) {
        logger.info("업그레이드 가이드 — {}: 레어 목표가 보조젬/스킬 비용을 깨뜨려 제외", e.getKey());
        continue;
      }
      Metrics m = metricsOf(v);
      List<String> modsKo = new ArrayList<>();
      // 옵션 하나 = 한 항목 — 복합 옵션(방어도+생명력)의 두 줄을 따로 늘어놓으면 옵션 둘로 읽힌다
      for (PoeRareTargetService.Affix a : c.plan().forced()) {
        modsKo.add(String.join(" / ", a.ko()));
      }
      for (PoeRareTargetService.Affix a : chosen) {
        modsKo.add(String.join(" / ", a.ko()));
      }
      // 이득이 없어 비운 칸 — 저항이 이미 넘치는 빌드면 저항 옵션도 수치를 못 올린다. 사는 쪽에선 "그 칸은 아무 옵션이어도 된다"는 정보다.
      long usedPrefixes =
          c.plan().forced().stream().filter(PoeRareTargetService.Affix::prefix).count()
              + chosen.stream().filter(PoeRareTargetService.Affix::prefix).count();
      long usedSuffixes = c.plan().forced().size() + chosen.size() - usedPrefixes;
      long freePrefixes = PoeRareTargetService.MAX_PREFIXES - usedPrefixes;
      long freeSuffixes = PoeRareTargetService.MAX_SUFFIXES - usedSuffixes;
      if (freePrefixes > 0 || freeSuffixes > 0) {
        modsKo.add(
            "남은 칸(접두 "
                + Math.max(0, freePrefixes)
                + " · 접미 "
                + Math.max(0, freeSuffixes)
                + ")은 이 빌드 수치에 영향이 없어 아무 옵션이어도 됨");
      }
      ItemPick pick =
          new ItemPick(
              "RARE",
              null,
              null,
              null,
              c.plan().base().name(),
              c.plan().base().nameKo(),
              List.copyOf(modsKo),
              pct(base.dps(), m.dps()),
              pct(base.ehp(), m.ehp()),
              pct(base.maxHit(), m.maxHit()),
              minionPct(base, m),
              0,
              0,
              needsText(baseValues, v),
              rareTargets.itemText(c.plan(), chosen));
      rarePicks.put(e.getKey(), pick);
      if (pick.needs() == null && pick.minionPct() == null) {
        offer(
            "레어 목표: "
                + SLOT_KO.getOrDefault(e.getKey(), e.getKey())
                + " "
                + c.plan().base().name()
                + " ["
                + String.join(" / ", modsKo.stream().filter(t -> !t.startsWith("남은 칸")).toList())
                + "]",
            equipItem(xml, e.getKey(), rareTargets.itemText(c.plan(), chosen)),
            base,
            m);
      }
      logger.info(
          "업그레이드 가이드 — {}: 레어 목표 {} [{}] {} 보충 {} (옵션 후보 {}개 실측)",
          e.getKey(),
          c.plan().base().name(),
          String.join(" / ", modsKo),
          deltasText(pick),
          pick.needs(),
          c.scored().size());
    }

    List<SlotPicks> measured = new ArrayList<>();
    for (SlotShare s : probeSlots) {
      List<ItemPick> fits = new ArrayList<>();
      List<ItemPick> needy = new ArrayList<>();
      int blocked = 0;
      List<Trial> list = trials.get(s.slot());
      for (Trial t : list) {
        Map<String, Double> v = t.result().get();
        if (v.getOrDefault("UnappliedSupportCount", 0d) > baseUnapplied
            || breaksCost(baseValues, v, castOnce)) {
          blocked++;
          continue; // 보조젬이 안 먹거나 스킬 비용을 못 치르게 되는 교체 — 능력치처럼 따로 메울 수 있는 게 아니다
        }
        Metrics m = metricsOf(v);
        PoeUniqueItem u = t.item();
        // 요구 능력치를 새로 못 맞추게 되면 그대로는 못 낀다(PoB 는 경고만 하고 계산은 한다) — 버리지 않고 '보충 필요'로 따로 둔다.
        String needs = needsText(baseValues, v);
        ItemPick pick =
            new ItemPick(
                "UNIQUE",
                u.slug(),
                u.name(),
                u.nameKo(),
                u.baseType(),
                u.baseTypeKo(),
                null,
                pct(base.dps(), m.dps()),
                pct(base.ehp(), m.ehp()),
                pct(base.maxHit(), m.maxHit()),
                minionPct(base, m),
                usage.counts().getOrDefault(u.name(), 0),
                usage.total(),
                needs,
                null);
        (needs == null ? fits : needy).add(pick);
        if (needs == null && pick.minionPct() == null) {
          offer(
              "고유 교체: " + SLOT_KO.getOrDefault(s.slot(), s.slot()) + " ← " + u.name(),
              equipItem(xml, s.slot(), uniqueItemText(u)),
              base,
              m);
        }
        logger.debug("업그레이드 가이드 — {} 후보 {}: {} 보충 {}", s.slot(), u.name(), deltasText(pick), needs);
      }
      // 그대로 끼울 수 있는 추천 = 고유(DPS·EHP·소환수 EHP 최고) + 2티어 레어 목표(고유와 같은 기준: 한 축 +1% 이상, 다른 축 -5% 이내)
      ItemPick rare = rarePicks.get(s.slot());
      boolean rareUpgrade = rare != null && isUpgrade(rare);
      List<ItemPick> picks = new ArrayList<>(choosePicks(fits));
      if (rareUpgrade && rare.needs() == null) {
        picks.add(rare);
      }
      // 능력치를 보충해야 하는 후보는 그대로 끼울 추천을 **모두 모은 뒤** 그보다 이득이 클 때만 덧붙인다(작으면 굳이 조건을 달 까닭이 없다).
      //   레어를 넣기 전에 비교하면 보충이 필요한 고유(+18.1%)가 보충 없는 레어(+20.7%)보다 못한데도 보였다(자가검사 2026-09-29).
      double bestAsIs = bestGain(picks);
      ItemPick bestNeedy = choosePicks(needy).stream().findFirst().orElse(null);
      if (bestNeedy != null && gainOf(bestNeedy) > bestAsIs) {
        picks.add(bestNeedy);
      }
      if (rareUpgrade && rare.needs() != null && gainOf(rare) > bestAsIs) {
        picks.add(rare);
      }
      boolean rareShown = rare != null && picks.contains(rare);
      picks.sort(
          Comparator.comparing((ItemPick p) -> p.needs() != null)
              .thenComparingDouble(p -> -gainOf(p)));
      logger.info(
          "업그레이드 가이드 — {}: 고유 후보 {}개 · 보충 필요 {}개 · 막힘(보조젬 미적용/비용 부족) {}개 · 추천 {}",
          s.slot(),
          list.size(),
          needy.size(),
          blocked,
          picks.stream()
              .map(
                  p ->
                      (p.name() == null ? "레어(" + p.baseType() + ")" : p.name())
                          + "("
                          + deltasText(p)
                          + ")"
                          + (p.needs() == null ? "" : "[보충 " + p.needs() + "]"))
              .toList());
      measured.add(new SlotPicks(s, List.copyOf(picks), list.size(), blocked, rare, rareShown));
    }
    measured.sort(
        Comparator.comparing((SlotPicks p) -> bestGain(asIs(p.picks())) == Double.NEGATIVE_INFINITY)
            .thenComparingDouble(p -> -bestGain(p.picks())));

    List<Suggestion> out = new ArrayList<>();
    int rank = 0;
    for (SlotPicks p : measured) {
      String slotKo = SLOT_KO.get(p.share().slot());
      String share = shareText(p.share());
      String excluded =
          p.blocked() > 0 ? "(보조젬이 안 먹거나 스킬 비용을 못 치르게 되는 " + p.blocked() + "개 제외)" : "";
      String tried =
          " 고유 아이템 "
              + p.tried()
              + "개"
              + (p.rare() != null ? "와 같은 베이스 2티어 레어" : "")
              + "를 이 칸에 끼워 재 봤습니다"
              + excluded
              + ".";
      // 레어 목표를 쟀는데 보이지 않은 경우 — 까닭을 나눠 적는다. "지금 아이템이 이미 2티어 레어보다 낫다"는 것 자체가 정보다.
      String rareNote;
      if (p.rare() == null) {
        rareNote = " (레어 목표는 베이스를 몰라 재지 못했습니다.)";
      } else if (p.rareShown()) {
        rareNote = "";
      } else if (isUpgrade(p.rare())) {
        rareNote =
            " 같은 베이스 2티어 레어("
                + deltasText(p.rare())
                + ")는 "
                + p.rare().needs()
                + " 보충이 필요한 데다 위 추천보다 이득이 작아 뺐습니다.";
      } else if (gainsOnSomeAxis(p.rare())) {
        // 한 축은 오르는데 다른 축을 허용치 넘게 잃는 경우 — "지금 것이 낫다"가 아니라 맞바꿈이다
        //   (반지 DPS -8.6% · EHP +24.7% 를 "지금 아이템이 더 낫다"고 적었던 것을 화면 판독에서 잡았다, 2026-09-29)
        //   소환수 빌드는 세 축 중 가장 크게 잃는 축과 가장 크게 얻는 축을 적는다.
        rareNote =
            " 같은 베이스 2티어 레어는 "
                + deltasText(p.rare())
                + "로 "
                + axisName(p.rare(), false)
                + "을 크게 내주는 맞바꿈이라 추천에서 뺐습니다("
                + axisName(p.rare(), true)
                + "이 더 급하면 고려할 만합니다).";
      } else {
        rareNote = " 같은 베이스 2티어 레어는 " + deltasText(p.rare()) + "라 지금 아이템이 더 낫거나 비슷합니다.";
      }
      // 맞바꿈이 있는 칸은 "뒤로 미뤄도 된다"고 하지 않는다
      boolean tradeOff =
          p.rare() != null && !p.rareShown() && !isUpgrade(p.rare()) && gainsOnSomeAxis(p.rare());
      if (!p.picks().isEmpty()) {
        rank++;
        out.add(
            new Suggestion(
                "item",
                "업그레이드 " + rank + "순위: " + slotKo,
                share + tried + rareNote,
                null,
                null,
                null,
                null,
                p.picks()));
      } else {
        out.add(
            new Suggestion(
                "item",
                slotKo + ": 더 나은 교체 없음",
                share
                    + tried
                    + " 지금보다 뚜렷이 나은 것이 없었습니다."
                    + rareNote
                    + (p.rare() != null && !tradeOff ? " 이 칸은 뒤로 미뤄도 됩니다." : ""),
                null,
                null,
                null,
                null,
                List.of()));
      }
    }
    // 무기 칸이 기여 하위 3위 안이면 알린다(고유·레어 비교는 하지 않는다).
    for (int i = 0; i < Math.min(3, shares.size()); i++) {
      SlotShare s = shares.get(i);
      if (!SLOT_CATEGORY.containsKey(s.slot())) {
        out.add(
            new Suggestion(
                "item",
                "약한 칸: " + SLOT_KO.get(s.slot()),
                shareText(s) + " 무기는 스킬이 요구하는 무기 종류와 얽혀 있어 교체 비교에서는 뺐습니다.",
                null,
                null,
                null,
                null,
                List.of()));
      }
    }
    return out;
  }

  /** 고유 추천과 같은 기준 — 한 축이 +1% 이상 오르고 다른 축이 -5% 넘게 깎이지 않는다. 축 = DPS · EHP · 소환수 EHP(소환수 빌드일 때만). */
  static boolean isUpgrade(ItemPick p) {
    return withinTolerance(p) && gainsOnSomeAxis(p);
  }

  /** 어느 축도 {@link #TRADE_TOLERANCE_PCT} 넘게 깎이지 않는다. */
  private static boolean withinTolerance(ItemPick p) {
    return p.dpsPct() >= -TRADE_TOLERANCE_PCT
        && p.ehpPct() >= -TRADE_TOLERANCE_PCT
        && (p.minionPct() == null || p.minionPct() >= -TRADE_TOLERANCE_PCT);
  }

  /** 한 축이라도 {@link #MIN_GAIN_PCT} 이상 오른다. */
  private static boolean gainsOnSomeAxis(ItemPick p) {
    return p.dpsPct() >= MIN_GAIN_PCT
        || p.ehpPct() >= MIN_GAIN_PCT
        || (p.minionPct() != null && p.minionPct() >= MIN_GAIN_PCT);
  }

  /** 추천 순위에 쓰는 이득 — 축 증감(%)의 합. 소환수 빌드가 아니면 DPS + EHP. */
  static double gainOf(ItemPick p) {
    return p.dpsPct() + p.ehpPct() + (p.minionPct() == null ? 0 : p.minionPct());
  }

  /** "DPS +3.0% · EHP -1.2%" — 소환수 빌드면 " · 소환수 EHP +0.4%" 가 붙는다. */
  static String deltasText(ItemPick p) {
    return "DPS "
        + signed(p.dpsPct())
        + " · EHP "
        + signed(p.ehpPct())
        + (p.minionPct() == null ? "" : " · 소환수 EHP " + signed(p.minionPct()));
  }

  /** 맞바꿈 문구의 축 이름 — gained 면 가장 크게 오른 축, 아니면 가장 크게 깎인 축(딜 · 생존 · 소환수 생존). */
  static String axisName(ItemPick p, boolean gained) {
    String name = "딜";
    double value = p.dpsPct();
    if (gained ? p.ehpPct() > value : p.ehpPct() <= value) {
      name = "생존";
      value = p.ehpPct();
    }
    if (p.minionPct() != null && (gained ? p.minionPct() > value : p.minionPct() < value)) {
      name = "소환수 생존";
    }
    return name;
  }

  private static String signed(double pct) {
    return Math.abs(pct) < 0.05 ? "0.0%" : String.format("%+.1f%%", pct);
  }

  /**
   * 레어 한 벌의 옵션 — 옵션 하나씩 얹어 잰 이득이 큰 순으로 접두 {@value PoeRareTargetService#MAX_PREFIXES}개(강제 옵션이 차지한 만큼
   * 뺀다)와 접미 {@value PoeRareTargetService#MAX_SUFFIXES}개. 이득이 없는 옵션은 채우지 않는다(무의미한 옵션으로 칸을 채워 "완성품"처럼
   * 보이게 하지 않는다).
   */
  static List<PoeRareTargetService.Affix> chooseAffixes(
      List<ScoredAffix> scored, PoeRareTargetService.Plan plan) {
    long prefixes = plan.forced().stream().filter(PoeRareTargetService.Affix::prefix).count();
    long suffixes = plan.forced().size() - prefixes;
    List<PoeRareTargetService.Affix> out = new ArrayList<>();
    List<ScoredAffix> ranked =
        scored.stream()
            .filter(sa -> sa.gain() > 0.05)
            .sorted(
                Comparator.comparingDouble((ScoredAffix sa) -> -sa.gain())
                    .thenComparing(sa -> sa.affix().family()))
            .toList();
    for (ScoredAffix sa : ranked) {
      PoeRareTargetService.Affix a = sa.affix();
      if (a.prefix()
          ? prefixes >= PoeRareTargetService.MAX_PREFIXES
          : suffixes >= PoeRareTargetService.MAX_SUFFIXES) {
        continue;
      }
      // 같은 계열(ModFamily)이 이미 있으면 한 아이템에 같이 못 붙는다 — 방어도+생명력 복합과 회피+생명력 복합 등
      if (plan.forced().stream().anyMatch(a::conflicts) || out.stream().anyMatch(a::conflicts)) {
        continue;
      }
      out.add(a);
      if (a.prefix()) {
        prefixes++;
      } else {
        suffixes++;
      }
    }
    // 표시 순서는 인게임처럼 접두 먼저(각각 이득 순)
    List<PoeRareTargetService.Affix> ordered = new ArrayList<>();
    out.stream().filter(PoeRareTargetService.Affix::prefix).forEach(ordered::add);
    out.stream().filter(a -> !a.prefix()).forEach(ordered::add);
    return ordered;
  }

  /**
   * 레어 목표가 요구 능력치를 모자라게 만들면, 모자란 능력치 옵션(힘/민첩/지능 접미)이 이 베이스에 붙는 경우 이득이 가장 작은 접미 하나를 그걸로 바꾼 옵션 목록. 바꿀
   * 게 없으면 null.
   */
  private List<PoeRareTargetService.Affix> withAttributeFix(
      RareCombo c, Map<String, Double> baseValues, Map<String, Double> now) {
    if (needsText(baseValues, now) == null) {
      return null;
    }
    List<PoeRareTargetService.Affix> out = new ArrayList<>(c.chosen());
    List<PoeRareTargetService.Affix> added = new ArrayList<>();
    boolean changed = false;
    for (String[] a :
        new String[][] {{"Str", "Strength"}, {"Dex", "Dexterity"}, {"Int", "Intelligence"}}) {
      if (shortfall(now, a[0]) - shortfall(baseValues, a[0]) <= 0.5) {
        continue;
      }
      PoeRareTargetService.Affix attr =
          c.plan().candidates().stream()
              .filter(x -> a[1].equals(x.family()))
              .findFirst()
              .orElse(null);
      if (attr == null
          || out.contains(attr)
          || c.plan().forced().stream().anyMatch(attr::conflicts)) {
        continue;
      }
      long suffixes =
          out.stream().filter(x -> !x.prefix()).count()
              + c.plan().forced().stream().filter(x -> !x.prefix()).count();
      if (suffixes >= PoeRareTargetService.MAX_SUFFIXES) {
        // 이득이 가장 작은 접미를 뺀다 — 방금 넣은 능력치 옵션은 빼지 않는다(힘을 넣고 민첩을 넣을 때 힘을 도로 빼던 결함, 2026-09-29)
        PoeRareTargetService.Affix weakest =
            c.scored().stream()
                .filter(
                    sa ->
                        !sa.affix().prefix()
                            && out.contains(sa.affix())
                            && !added.contains(sa.affix()))
                .min(Comparator.comparingDouble(ScoredAffix::gain))
                .map(ScoredAffix::affix)
                .orElse(null);
        if (weakest == null) {
          continue;
        }
        out.remove(weakest);
      }
      if (out.stream().anyMatch(attr::conflicts)) {
        continue;
      }
      out.add(attr);
      added.add(attr);
      changed = true;
    }
    return changed ? out : null;
  }

  /** 활성 세트에서 그 칸에 낀 아이템의 원문(없으면 빈 문자열). */
  private static String slotItemText(String xml, int[] set, String slot) {
    int[] tag = slotTag(xml, set, slot);
    if (tag == null) {
      return "";
    }
    String id = attr(xml.substring(tag[0], tag[1]), "itemId");
    return id == null || "0".equals(id) ? "" : itemText(xml, id);
  }

  /** 파티를 비운 빌드를 만들 때 쓰는 빈 용병 — 사용자 빌드에 원래 있던 파티 버프까지 빼고 "용병 없음"을 잰다. */
  private static final PoeMercenaryService.MercBuffs EMPTY_MERC =
      new PoeMercenaryService.MercBuffs(Map.of(), List.of(), List.of(), List.of(), false, false);

  private static String mercDetail(PoeMercenaryService.MercBuffs merc) {
    if (merc.isEmpty()) {
      return "용병 빌드에서 나에게 오는 오라·저주가 없습니다 — 용병 빌드의 젬(오라·저주)을 확인하세요.";
    }
    return "용병 빌드(젬·장비)로 계산한 오라·저주를 PoB 파티 탭으로 넣었습니다."
        + (merc.knighthood()
            ? (merc.knighthoodInMerc()
                ? " 수여된 기사 작위(용병 오라 효과 +50%)는 용병 빌드에 이미 있어 따로 넣지 않았습니다."
                : " 수여된 기사 작위로 용병 오라 효과 +50% 를 넣었습니다.")
            : "")
        + " 증감은 용병이 더하는 몫이고, 아래 수치는 모두 용병 버프를 포함합니다. 용병 자신의 딜·생존과 충직한 호위병(피해 분담)은 계산에 없습니다.";
  }

  /**
   * "이 칸을 빼면 DPS 17.7% · EHP 5.4% 줄어들고 소환수 EHP 12.1% 늘어납니다." — 빼면 오히려 느는 몫(소환수 생명력을 깎는 고유 투구 고대의 해골
   * 등)을 "-12.1% 줄어듭니다"로 적으면 이중 부정으로 읽혀 따로 적는다(망령 표본 화면 판독 2026-09-30).
   */
  private static String shareText(SlotShare s) {
    List<String> less = new ArrayList<>();
    List<String> more = new ArrayList<>();
    shareInto("DPS", s.dpsShare(), less, more);
    shareInto("EHP", s.ehpShare(), less, more);
    if (s.minionShare() != null) {
      shareInto("소환수 EHP", s.minionShare(), less, more);
    }
    StringBuilder text = new StringBuilder("이 칸을 빼면 ");
    if (!less.isEmpty()) {
      text.append(String.join(" · ", less)).append(more.isEmpty() ? " 줄어듭니다." : " 줄어들고 ");
    }
    if (!more.isEmpty()) {
      text.append(String.join(" · ", more)).append(" 늘어납니다.");
    }
    return text.toString();
  }

  /** 몫 하나를 줄어드는 쪽/느는 쪽에 나눠 담는다 — 0.05% 미만의 음수는 부호 없는 0.0%(부호 붙은 0 금지). */
  private static void shareInto(String label, double share, List<String> less, List<String> more) {
    if (share <= -0.05) {
      more.add(label + " " + String.format("%.1f", -share) + "%");
    } else {
      less.add(label + " " + String.format("%.1f", Math.max(0, share)) + "%");
    }
  }

  /**
   * 한 칸의 추천 — DPS 쪽 최고와 EHP 쪽 최고, 소환수 빌드면 소환수 EHP 쪽 최고까지(같은 아이템이면 하나). 다른 축을 {@link
   * #TRADE_TOLERANCE_PCT} 넘게 깎는 교체는 맞바꿈이라 뺀다. 순서는 축 합이 큰 쪽 먼저.
   */
  static List<ItemPick> choosePicks(List<ItemPick> fits) {
    List<ItemPick> out = new ArrayList<>();
    for (ToDoubleFunction<ItemPick> axis :
        List.<ToDoubleFunction<ItemPick>>of(
            ItemPick::dpsPct,
            ItemPick::ehpPct,
            p -> p.minionPct() == null ? Double.NEGATIVE_INFINITY : p.minionPct())) {
      fits.stream()
          .filter(p -> axis.applyAsDouble(p) >= MIN_GAIN_PCT && withinTolerance(p))
          .max(Comparator.comparingDouble(axis).thenComparing(ItemPick::slug))
          .filter(best -> !out.contains(best))
          .ifPresent(out::add);
    }
    out.sort(Comparator.comparingDouble((ItemPick p) -> -gainOf(p)));
    return List.copyOf(out);
  }

  private static double bestGain(List<ItemPick> picks) {
    return picks.stream()
        .mapToDouble(PoeUpgradeGuideService::gainOf)
        .max()
        .orElse(Double.NEGATIVE_INFINITY);
  }

  /** 그대로 끼울 수 있는 추천만(요구 능력치 보충이 필요 없는 것). */
  private static List<ItemPick> asIs(List<ItemPick> picks) {
    return picks.stream().filter(p -> p.needs() == null).toList();
  }

  // ─────────────────────────── 평가 · 지표 ───────────────────────────

  public RefineStatus refineStatus() {
    return new RefineStatus(
        refining,
        refineRound,
        REFINE_ROUNDS,
        phase,
        done.get(),
        total,
        lastRefine,
        lastRefineError);
  }

  /**
   * 자동 다듬기 시작 — 가이드를 돌려 적용 가능한 교체안(보조젬·고유·2티어 레어 목표) 중 두 축 모두 {@link #REFINE_TOLERANCE_PCT} 넘게 깎지
   * 않으면서 DPS%+EHP% 가 가장 큰 것 하나를 실제로 적용하고, 다시 잰다({@link #REFINE_ROUNDS} 회). 가이드와 엔진을 함께 쓰므로 가이드 잡과
   * 동시에 돌지 않는다.
   *
   * <p>왜: 최적화기(빈 빌드 탐욕 선택)는 실빌드 생존력의 원천인 주얼·고유 조합에 닿지 못한다(2026-09-30 실측 EHP 0.17~0.55x). 실빌드에서 출발해
   * 그 조합을 유지한 채 다듬으면 두 축을 함께 가져갈 수 있다. 소환수 빌드는 축이 하나 더 있어(소환수 EHP) 이 합 기준이 맞지 않아 교체안을 모으지 않는다.
   *
   * @return 이미 돌고 있거나 코드를 못 읽으면 false(사유는 refineStatus().error)
   */
  public boolean startRefine(String code) {
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    String xml;
    try {
      xml = importService.decodeToXml(code);
    } catch (RuntimeException e) {
      lastRefineError = "PoB 코드를 읽지 못했습니다: " + e.getMessage();
      lastRefine = null;
      running.set(false);
      return false;
    }
    lastRefineError = null;
    lastRefine = null;
    refining = true;
    refineRound = 0;
    done.set(0);
    total = 1;
    phase = "기준선";
    Thread thread =
        new Thread(
            () -> {
              try {
                lastRefine = refine(xml);
              } catch (Throwable e) {
                logger.warn("자동 다듬기 실패", e);
                lastRefineError = "다듬기 실패: " + e.getMessage();
              } finally {
                collector = null;
                phase = "";
                refining = false;
                running.set(false);
              }
            },
            "poe-guide-refine");
    thread.setDaemon(true);
    thread.start();
    return true;
  }

  private RefineResult refine(String startXml) throws Exception {
    long startedAt = System.currentTimeMillis();
    String xml = startXml;
    Metrics before = metricsOf(eval(xml));
    List<RefineStep> steps = new ArrayList<>();
    for (int round = 1; round <= REFINE_ROUNDS; round++) {
      refineRound = round;
      List<Applicable> found = Collections.synchronizedList(new ArrayList<>());
      collector = found;
      try {
        analyze(xml, null);
      } finally {
        collector = null;
      }
      Applicable best = pickRefinement(found);
      if (best == null) {
        logger.info("자동 다듬기 {}단계: 적용할 교체 없음(후보 {}개) — 종료", round, found.size());
        break;
      }
      logger.info(
          "자동 다듬기 {}단계: {} (DPS {} · EHP {}, 후보 {}개)",
          round,
          best.label(),
          signed(best.dpsPct()),
          signed(best.ehpPct()),
          found.size());
      xml = best.xml();
      steps.add(new RefineStep(best.label(), best.dpsPct(), best.ehpPct(), best.maxHitPct()));
    }
    phase = "최종 계산";
    Map<String, Double> finalValues = eval(xml);
    return new RefineResult(
        before,
        metricsOf(finalValues),
        List.copyOf(steps),
        encodePobCode(withPlayerStats(xml, finalValues)),
        done.get(),
        System.currentTimeMillis() - startedAt);
  }

  /** 두 축 모두 한도 안에서 DPS%+EHP% 가 가장 큰 교체 — 최소 {@link #MIN_GAIN_PCT} 넘게 올라야 한다. 없으면 null. */
  static Applicable pickRefinement(List<Applicable> found) {
    Applicable best = null;
    double bestGain = MIN_GAIN_PCT;
    for (Applicable a : found) {
      if (a.dpsPct() < -REFINE_TOLERANCE_PCT || a.ehpPct() < -REFINE_TOLERANCE_PCT) {
        continue;
      }
      double gain = a.dpsPct() + a.ehpPct();
      if (gain > bestGain) {
        best = a;
        bestGain = gain;
      }
    }
    return best;
  }

  private void offer(String label, String xml, Metrics base, Metrics m) {
    List<Applicable> c = collector;
    if (c != null) {
      c.add(
          new Applicable(
              label,
              xml,
              pct(base.dps(), m.dps()),
              pct(base.ehp(), m.ehp()),
              pct(base.maxHit(), m.maxHit())));
    }
  }

  /**
   * Config 체크만으로 켜지는 버프·충전을 실제로 공급하는 보조젬 → 그 Config 변수. PoB 는 이 버프를 공급원 없이도 체크만 보고 적용하므로, 체크가 켜진
   * 빌드에서 이 젬을 빼 보면 손실이 0으로 잰다 — 실빌드 저거넛(표준 가정 buffFortify=true)에서 방어 상승 보조가 "가장 약한 보조"로 잡혀 무자비와 교체
   * 추천이 나왔다(2026-09-30 자동 다듬기 실측). 인게임에선 이 젬이 그 버프의 출처라 빼면 사라진다. 분노 보조는 PoB 가 '분노를 얻을 수 있음' 조건을 따로
   * 봐서 빼면 제대로 사라지므로 넣지 않는다.
   */
  private static final Map<String, String> SUPPORT_SUPPLIES_CONFIG =
      Map.of(
          "Fortify Support", "buffFortify",
          "Endurance Charge on Melee Stun Support", "useEnduranceCharges",
          "Power Charge On Critical Support", "usePowerCharges");

  /**
   * 보조젬 계열 — "Awakened "·"Greater "·"Lesser " 접두와 " Support" 접미를 뗀 이름(Spell Echo · Greater Spell
   * Echo · Awakened Spell Echo = 한 계열).
   */
  static String supportFamily(String name) {
    if (name == null) {
      return "";
    }
    String n = name.replaceFirst(" Support$", "");
    for (String prefix : List.of("Awakened ", "Greater ", "Lesser ")) {
      if (n.startsWith(prefix)) {
        n = n.substring(prefix.length());
      }
    }
    return n;
  }

  /** 후보가 (바꿀 대상 말고) 이미 링크된 보조젬과 같은 계열인가. */
  static boolean sameFamilyLinked(PoeGem candidate, List<GemRef> linked, GemRef target) {
    String fam = supportFamily(candidate == null ? null : candidate.name());
    if (fam.isEmpty()) {
      return false;
    }
    for (GemRef g : linked) {
      if (g != target && g.gem() != null && fam.equals(supportFamily(g.gem().name()))) {
        return true;
      }
    }
    return false;
  }

  /** 주 스킬을 발동형으로 바꾸는 보조("Cast when Stunned", "Awakened Cast On Critical Strike" …). */
  static boolean isTriggerConversion(PoeGem gem) {
    String n = gem == null || gem.name() == null ? "" : gem.name();
    return n.startsWith("Cast ") || n.startsWith("Awakened Cast ");
  }

  /** 파티 지원 보조(관대함) — 빼면 오라가 나에게 걸려 DPS 가 오르지만 역할을 바꾸는 것이라 "약한 보조" 후보에서 뺀다. */
  static boolean isPartySupport(PoeGem gem) {
    return gem != null && gem.name() != null && gem.name().startsWith("Generosity");
  }

  /** 이 보조젬이 Config 가 켜 둔 버프·충전의 공급원인가 — 켜져 있을 때만 참(꺼져 있으면 빼 본 손실이 제대로 잰다). */
  static boolean suppliesAssumedBuff(String xml, PoeGem gem) {
    String var = gem == null ? null : SUPPORT_SUPPLIES_CONFIG.get(gem.name());
    if (var == null) {
      return false;
    }
    // 속성 순서는 PoB 저장이 해시 순이라 고정돼 있지 않다 — 태그를 통째로 잡아 두 속성을 따로 본다
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("<Input\\b[^>]*\\bname=\"" + var + "\"[^>]*>").matcher(xml);
    while (m.find()) {
      if (m.group().contains("boolean=\"true\"")) {
        return true;
      }
    }
    return false;
  }

  /** 빌드 요약(PoePobImportService.STAT_KEYS)이 읽는 저장 스탯 이름 → 엔진 값 키. */
  private static final Map<String, String> PLAYER_STAT_FROM_ENGINE = new LinkedHashMap<>();

  static {
    for (String k :
        List.of(
            "CombinedDPS",
            "TotalDPS",
            "AverageDamage",
            "Life",
            "EnergyShield",
            "Mana",
            "Armour",
            "Evasion",
            "TotalEHP",
            "FireResist",
            "ColdResist",
            "LightningResist",
            "ChaosResist",
            "CritChance")) {
      PLAYER_STAT_FROM_ENGINE.put(k, k);
    }
    PLAYER_STAT_FROM_ENGINE.put("EffectiveSpellSuppressionChance", "SpellSuppressionChance");
    PLAYER_STAT_FROM_ENGINE.put("EffectiveBlockChance", "BlockChance");
    PLAYER_STAT_FROM_ENGINE.put("EffectiveSpellBlockChance", "SpellBlockChance");
  }

  /** 저장 스탯(PlayerStat)을 엔진 값으로 교체 — 옛 값은 전부 지우고 엔진에 있는 키만 넣는다(일부만 옛 값이 섞이지 않게). */
  static String withPlayerStats(String xml, Map<String, Double> values) {
    StringBuilder lines = new StringBuilder();
    for (Map.Entry<String, String> e : PLAYER_STAT_FROM_ENGINE.entrySet()) {
      Double v = values.get(e.getValue());
      if (v != null && !v.isNaN() && !v.isInfinite()) {
        lines
            .append("\n<PlayerStat stat=\"")
            .append(e.getKey())
            .append("\" value=\"")
            .append(v)
            .append("\"/>");
      }
    }
    String stripped = xml.replaceAll("\\s*<PlayerStat\\b[^>]*/>", "");
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("<Build\\b[^>]*>").matcher(stripped);
    if (!m.find()) {
      return stripped;
    }
    return stripped.substring(0, m.end()) + lines + stripped.substring(m.end());
  }

  /** PoB 공유 코드 인코딩(zlib deflate → base64url) — PoeOptimizeService.encodePobCode 와 같은 규칙. */
  private static String encodePobCode(String xml) {
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

  private Map<String, Double> eval(String xml) {
    try {
      return engine.calculateValuesIsolated(xml);
    } finally {
      done.incrementAndGet();
    }
  }

  private static Metrics metricsOf(Map<String, Double> v) {
    double dps = v.getOrDefault("CombinedDPS", 0d);
    if (dps <= 0) {
      dps = v.getOrDefault("FullDPS", 0d); // 미니언 빌드는 CombinedDPS 가 0 이다
    }
    double maxHit = Double.MAX_VALUE;
    boolean any = false;
    for (String k :
        List.of(
            "PhysicalMaximumHitTaken",
            "FireMaximumHitTaken",
            "ColdMaximumHitTaken",
            "LightningMaximumHitTaken",
            "ChaosMaximumHitTaken")) {
      Double x = v.get(k);
      if (x != null && x > 0) {
        maxHit = Math.min(maxHit, x);
        any = true;
      }
    }
    return new Metrics(
        dps,
        v.getOrDefault("TotalEHP", 0d),
        any ? maxHit : 0d,
        v.getOrDefault("MinionTotalEHP", 0d));
  }

  /** 소환수 EHP 증감(%) — from 에 소환수가 없으면(소환수 빌드가 아니면) null 이라 화면·판정에서 이 축이 빠진다. */
  private static Double minionPct(Metrics from, Metrics to) {
    return from.minionEhp() > 0 ? pct(from.minionEhp(), to.minionEhp()) : null;
  }

  private static final String[][] ATTRIBUTES = {{"Str", "힘"}, {"Dex", "민첩"}, {"Int", "지능"}};

  /** 한 능력치의 부족분 — (요구 − 보유)의 양수 부분. PoB 는 못 맞춰도 계산을 하므로 따로 본다. */
  static double shortfall(Map<String, Double> v, String attr) {
    return Math.max(0, v.getOrDefault("Req" + attr, 0d) - v.getOrDefault(attr, 0d));
  }

  /** 요구 능력치 부족분 합(힘·민첩·지능). */
  static double attributeDeficit(Map<String, Double> v) {
    double deficit = 0;
    for (String[] a : ATTRIBUTES) {
      deficit += shortfall(v, a[0]);
    }
    return deficit;
  }

  /**
   * 교체로 인게임에서 스킬 비용을 못 치르게 되면 true — 주 스킬이 새로 비용 경고에 걸리거나, 비용을 못 치르는 스킬 수가 늘 때(calc.lua 의
   * MainSkillCostWarning · CostWarningCount). PoB 는 경고만 하고 DPS 는 그대로 계산하므로 수치로는 안 보인다.
   *
   * @param castOnce 주 스킬이 지속형 소환수라 비용을 한 번만 치르면 되는지 — 그러면 주 스킬 자신의 경고는 보지 않는다
   */
  static boolean breaksCost(Map<String, Double> base, Map<String, Double> now, boolean castOnce) {
    double baseMain = base.getOrDefault("MainSkillCostWarning", 0d);
    double nowMain = now.getOrDefault("MainSkillCostWarning", 0d);
    if (!castOnce && nowMain > baseMain) {
      return true;
    }
    // 주 스킬을 뺀 나머지 스킬의 비용 경고 수 — 지속형 소환수 빌드도 이동기·저주 같은 다른 스킬은 매번 비용을 치른다.
    double baseOthers = base.getOrDefault("CostWarningCount", 0d) - baseMain;
    double nowOthers = now.getOrDefault("CostWarningCount", 0d) - nowMain;
    return nowOthers > baseOthers;
  }

  /**
   * 한 번 불러 두면 남는 소환수 스킬 — Minion 태그가 있고 Duration 태그가 없다(격노한 영혼·해골·되살린 무기처럼 지속시간이 있으면 다시 불러야 한다).
   */
  static boolean isCastOnceMinion(PoeGem gem) {
    List<String> tags = gem.tags() == null ? List.of() : gem.tags();
    return tags.contains("Minion") && !tags.contains("Duration");
  }

  /**
   * 기준선보다 새로 모자라게 된 능력치("힘 25 · 민첩 34") — 늘어난 게 없으면 null. 능력치마다 따로 본다: 합으로 보면 힘 부족을 메우며 민첩 부족을 새로
   * 만드는 교체가 "변화 없음"으로 빠져나간다.
   */
  static String needsText(Map<String, Double> base, Map<String, Double> now) {
    List<String> parts = new ArrayList<>();
    for (String[] a : ATTRIBUTES) {
      double more = shortfall(now, a[0]) - shortfall(base, a[0]);
      if (more > 0.5) {
        parts.add(a[1] + " " + Math.round(more));
      }
    }
    return parts.isEmpty() ? null : String.join(" · ", parts);
  }

  private static Double pct(double base, double now) {
    return base > 0 ? (now / base - 1) * 100 : 0d;
  }

  // ─────────────────────────── 보조젬 후보 ───────────────────────────

  private List<PoeGem> supportCandidates(PoeGem skill, List<GemRef> socketed) {
    List<String> skillTags = skill.tags() == null ? List.of() : skill.tags();
    List<String> present =
        socketed.stream().filter(g -> g.gem() != null).map(g -> g.gem().slug()).toList();
    return gemData.search(null, "support", "all", null).stream()
        .filter(g -> g.levels() != null && !g.levels().isEmpty())
        .filter(g -> !present.contains(g.slug()))
        // 주 스킬을 발동형으로 바꾸는 보조("Cast when Stunned" 등)는 스킬 사용 방식을 통째로 바꾸고, PoB 는 발동 빈도 가정에 따라 수치가 크게
        // 달라진다 —
        //   같은 저울의 "보조젬 교체"가 아니다(2026-09-30 자동 다듬기 59빌드 일괄: 데드아이 에테르 칼날에 '기절 시 시전' +476%).
        .filter(g -> !isTriggerConversion(g))
        .filter(g -> compatible(skillTags, g.tags() == null ? List.of() : g.tags()))
        .sorted(
            Comparator.comparingLong((PoeGem g) -> -overlap(skillTags, g.tags()))
                .thenComparing(PoeGem::slug))
        .limit(SWAP_CANDIDATES)
        .toList();
  }

  private static boolean compatible(List<String> skillTags, List<String> supportTags) {
    if (skillTags.isEmpty() || supportTags.isEmpty()) {
      return true;
    }
    for (String arch : ARCHETYPE_TAGS) {
      if (supportTags.contains(arch) && !skillTags.contains(arch)) {
        return false;
      }
    }
    boolean supSpell = supportTags.contains("Spell");
    boolean supAttack = supportTags.contains("Attack");
    boolean skillSpell = skillTags.contains("Spell");
    boolean skillAttack = skillTags.contains("Attack");
    if (supSpell && !supAttack && skillAttack && !skillSpell) {
      return false;
    }
    return !(supAttack && !supSpell && skillSpell && !skillAttack);
  }

  private static long overlap(List<String> a, List<String> b) {
    if (a == null || b == null) {
      return 0;
    }
    return b.stream().filter(a::contains).count();
  }

  /** PoB 의 보조젬 이름은 "Support" 접미사가 없다. */
  private static String pobName(PoeGem gem) {
    return gem.name().replaceFirst(" Support$", "");
  }

  private static String label(PoeGem gem) {
    return gem.nameKo() != null ? gem.nameKo() : gem.name();
  }

  // ─────────────────────────── 고유 아이템 후보 ───────────────────────────

  /**
   * 한 칸에 끼워 볼 고유 아이템 — 실빌드 인기(정확한 전직·주 스킬 조합) → 주 스킬 공격 키워드 상위 → 방어 키워드 상위 순으로 모아 중복을 뺀다. 지금 리그에서 못
   * 얻는 고유 · 캐릭터 레벨로 못 끼는 고유 · 이미 낀 고유는 뺀다.
   */
  private List<PoeUniqueItem> uniqueCandidates(
      String category,
      int level,
      List<String> offence,
      PoeMetaPopularityService.ItemUsage usage,
      Set<String> worn) {
    List<PoeUniqueItem> pool =
        uniqueData.search(null, category, null).stream()
            .filter(u -> !u.legacy())
            .filter(u -> u.name() != null && u.baseType() != null && u.slug() != null)
            .filter(u -> u.requiredLevel() == null || u.requiredLevel() <= level)
            .filter(u -> !worn.contains(u.name()))
            .toList();
    Set<PoeUniqueItem> out = new LinkedHashSet<>();
    pool.stream()
        .filter(u -> usage.counts().getOrDefault(u.name(), 0) > 0)
        .sorted(
            Comparator.comparingInt((PoeUniqueItem u) -> -usage.counts().get(u.name()))
                .thenComparing(PoeUniqueItem::slug))
        .limit(META_CANDIDATES)
        .forEach(out::add);
    out.addAll(topByKeywords(pool, offence, OFFENCE_CANDIDATES));
    out.addAll(topByKeywords(pool, DEFENCE_KEYWORDS, DEFENCE_CANDIDATES));
    return List.copyOf(out);
  }

  private static List<PoeUniqueItem> topByKeywords(
      List<PoeUniqueItem> pool, List<String> keywords, int limit) {
    return pool.stream()
        .filter(u -> keywordScore(u, keywords) > 0)
        .sorted(
            Comparator.comparingInt((PoeUniqueItem u) -> -keywordScore(u, keywords))
                .thenComparing(PoeUniqueItem::slug))
        .limit(limit)
        .toList();
  }

  private static int keywordScore(PoeUniqueItem u, List<String> keywords) {
    int score = 0;
    List<String> lines = new ArrayList<>();
    if (u.implicits() != null) {
      lines.addAll(u.implicits());
    }
    if (u.explicits() != null) {
      lines.addAll(u.explicits());
    }
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

  /** 주 스킬 태그 → 아이템 모드 키워드(최적화기 keywords 와 같은 대응을 간추린 것 — 여기선 후보를 추리는 데만 쓰고 순위는 실측으로 낸다). */
  private static List<String> offenceKeywords(PoeGem gem) {
    List<String> out = new ArrayList<>(List.of("damage"));
    if (gem == null || gem.tags() == null) {
      return out;
    }
    for (String tag : gem.tags()) {
      switch (tag) {
        case "AoE" -> out.add("area");
        case "Duration" -> out.add("damage over time");
        case "Spell" -> {
          out.add("spell");
          out.add("cast speed");
        }
        case "Attack" -> {
          out.add("attack");
          out.add("accuracy");
        }
        case "Fire",
            "Cold",
            "Lightning",
            "Chaos",
            "Physical",
            "Projectile",
            "Minion",
            "Critical",
            "Totem",
            "Trap",
            "Mine",
            "Brand" ->
            out.add(tag.toLowerCase(Locale.ROOT));
        default -> {
          // 키워드로 옮길 게 없는 태그
        }
      }
    }
    return out;
  }

  /**
   * PoB 아이템 원문 — 범위 모드는 접두 없이 둔다(PoB 기본 = 보통 롤). 최적화기는 엔드게임 전제로 최대 롤을 쓰지만, 가이드는 '지금 사서 끼울 것'을 재므로
   * 최대 롤을 전제하면 이득을 부풀린다.
   */
  static String uniqueItemText(PoeUniqueItem u) {
    StringBuilder text =
        new StringBuilder("Rarity: UNIQUE\n")
            .append(u.name())
            .append('\n')
            .append(u.baseType())
            .append('\n');
    // 반경 라벨이 빠지면 "…in Radius" 모드를 PoB 가 통째로 무시한다(최적화기 uniqueItemText 와 같은 이유).
    if (u.radius() != null && !u.radius().isBlank()) {
      text.append("Radius: ").append(u.radius()).append('\n');
    }
    List<String> implicits = u.implicits() == null ? List.of() : u.implicits();
    text.append("Implicits: ").append(implicits.size()).append('\n');
    for (String line : implicits) {
      text.append(line).append('\n');
    }
    if (u.explicits() != null) {
      for (String line : u.explicits()) {
        text.append(line).append('\n');
      }
    }
    return text.toString();
  }

  // ─────────────────────────── XML 조작(정규식 없이 문자열 탐색) ───────────────────────────

  private static String tagOf(String xml, String open) {
    int a = xml.indexOf(open);
    if (a < 0) {
      return "";
    }
    int b = xml.indexOf('>', a);
    return b < 0 ? "" : xml.substring(a, b + 1);
  }

  private static String attr(String tag, String name) {
    String key = " " + name + "=\"";
    int a = tag.indexOf(key);
    if (a < 0) {
      return null;
    }
    int from = a + key.length();
    int b = tag.indexOf('"', from);
    return b < 0 ? null : tag.substring(from, b);
  }

  private static int parseInt(String s, int fallback) {
    try {
      return s == null ? fallback : Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  /**
   * 활성 세트 블록 [시작, 끝) — 부모 태그({@code <Items>}·{@code <Skills>})의 활성 번호와 id 가 같은 세트, 못 찾으면 첫 세트, 세트가
   * 아예 없으면 null. 자식 없는 세트({@code <ItemSet .../>})는 여는 태그 하나가 곧 블록이다 — 닫는 태그를 찾아 나가면 다음 세트를 삼킨다.
   */
  static int[] activeBlock(
      String xml, String parentOpen, String activeAttr, String setOpen, String setClose) {
    String active = attr(tagOf(xml, parentOpen), activeAttr);
    int[] first = null;
    int from = 0;
    while (true) {
      int a = xml.indexOf(setOpen, from);
      if (a < 0) {
        break;
      }
      int gt = xml.indexOf('>', a);
      if (gt < 0) {
        break;
      }
      int b;
      if (xml.charAt(gt - 1) == '/') {
        b = gt + 1;
      } else {
        b = xml.indexOf(setClose, gt);
        if (b < 0) {
          break;
        }
        b += setClose.length();
      }
      int[] block = {a, b};
      if (active != null && active.equals(attr(xml.substring(a, gt + 1), "id"))) {
        return block;
      }
      if (first == null) {
        first = block;
      }
      from = b;
    }
    return first;
  }

  static int[] activeItemSetRange(String xml) {
    return activeBlock(xml, "<Items ", "activeItemSet", "<ItemSet ", "</ItemSet>");
  }

  /**
   * 활성 스킬 세트의 {@code <Skill>} 블록 위치들(원문 순서) — mainSocketGroup 은 활성 세트 안의 번호다(레벨링용 세트 등이 따로 있으면 전체에서
   * 세면 엉뚱한 그룹을 잡는다). 세트가 없는 옛 형식이면 문서 전체. 자식 없는 {@code <Skill .../>} 도 한 그룹으로 센다(번호가 밀리지 않게).
   */
  static List<int[]> skillBlocks(String xml) {
    int[] set = activeBlock(xml, "<Skills ", "activeSkillSet", "<SkillSet ", "</SkillSet>");
    int from = set == null ? 0 : set[0];
    int to = set == null ? xml.length() : set[1];
    List<int[]> out = new ArrayList<>();
    while (true) {
      int a = xml.indexOf("<Skill ", from);
      if (a < 0 || a >= to) {
        break;
      }
      int gt = xml.indexOf('>', a);
      if (gt < 0) {
        break;
      }
      int b;
      if (xml.charAt(gt - 1) == '/') {
        b = gt + 1;
      } else {
        b = xml.indexOf("</Skill>", gt);
        if (b < 0) {
          break;
        }
        b += "</Skill>".length();
      }
      out.add(new int[] {a, b});
      from = b;
    }
    return out;
  }

  private List<GemRef> gemsIn(String xml, int[] block) {
    List<GemRef> out = new ArrayList<>();
    int from = block[0];
    while (true) {
      int a = xml.indexOf("<Gem ", from);
      if (a < 0 || a >= block[1]) {
        break;
      }
      int b = xml.indexOf("/>", a);
      if (b < 0) {
        break;
      }
      b += 2;
      String tag = xml.substring(a, b);
      String nameSpec = attr(tag, "nameSpec");
      boolean enabled = !"false".equals(attr(tag, "enabled"));
      PoeGem gem = null;
      if (nameSpec != null) {
        Optional<PoeGem> found = gemData.findByName(nameSpec);
        if (found.isEmpty()) {
          found = gemData.findByName(nameSpec + " Support");
        }
        gem = found.orElse(null);
      }
      out.add(new GemRef(a, b, nameSpec, enabled, gem));
      from = b;
    }
    return out;
  }

  private static String disableGem(String xml, GemRef g) {
    String tag = xml.substring(g.start(), g.end());
    String changed =
        tag.contains(" enabled=\"true\"")
            ? tag.replace(" enabled=\"true\"", " enabled=\"false\"")
            : tag.replace("<Gem ", "<Gem enabled=\"false\" ");
    return xml.substring(0, g.start()) + changed + xml.substring(g.end());
  }

  private static String replaceGem(String xml, GemRef g, String newNameSpec) {
    String tag = xml.substring(g.start(), g.end());
    // gemId 가 남아 있으면 PoB 가 이름 대신 gemId(+variantId/skillId)로 젬을 찾으므로 지운다 — gemId 가 없으면 variantId 는
    // 보지 않는다.
    String changed = stripAttr(stripAttr(tag, "gemId"), "skillId");
    changed =
        changed.replace(" nameSpec=\"" + g.nameSpec() + "\"", " nameSpec=\"" + newNameSpec + "\"");
    return xml.substring(0, g.start()) + changed + xml.substring(g.end());
  }

  private static String stripAttr(String tag, String name) {
    String key = " " + name + "=\"";
    int a = tag.indexOf(key);
    if (a < 0) {
      return tag;
    }
    int b = tag.indexOf('"', a + key.length());
    return b < 0 ? tag : tag.substring(0, a) + tag.substring(b + 1);
  }

  /**
   * 세트 안에서 {@code slot} 칸의 {@code <Slot .../>} 위치 [시작, 끝) — 속성 순서와 무관하게 name 속성으로 찾는다. 없으면 null.
   */
  static int[] slotTag(String xml, int[] set, String slot) {
    int from = set[0];
    while (true) {
      int a = xml.indexOf("<Slot ", from);
      if (a < 0 || a >= set[1]) {
        return null;
      }
      int b = xml.indexOf("/>", a);
      if (b < 0 || b > set[1]) {
        return null;
      }
      b += 2;
      if (slot.equals(attr(xml.substring(a, b), "name"))) {
        return new int[] {a, b};
      }
      from = b;
    }
  }

  /** PoB 는 빈 칸을 itemId="0" 으로 적는다. */
  static boolean equipped(String xml, int[] set, String slot) {
    int[] tag = slotTag(xml, set, slot);
    if (tag == null) {
      return false;
    }
    String id = attr(xml.substring(tag[0], tag[1]), "itemId");
    return id != null && !"0".equals(id);
  }

  static String removeSlot(String xml, String slot) {
    int[] set = activeItemSetRange(xml);
    if (set == null) {
      return xml;
    }
    int[] tag = slotTag(xml, set, slot);
    if (tag == null) {
      return xml;
    }
    return xml.substring(0, tag[0]) + xml.substring(tag[1]);
  }

  /**
   * 활성 세트의 {@code slot} 칸에 새 아이템을 끼운 XML — 새 {@code <Item>} 은 기존 최대 id + 1 로 첫 세트 앞에 넣고 칸 태그의
   * itemId 만 바꾼다(칸이 없으면 만든다). 세트가 없는 옛 형식이면 원문 그대로.
   */
  static String equipItem(String xml, String slot, String itemText) {
    int[] set = activeItemSetRange(xml);
    if (set == null) {
      return xml;
    }
    int newId = maxItemId(xml) + 1;
    // 칸 태그부터 고친다 — 아이템은 첫 세트 앞(= 이 칸보다 앞)에 끼우므로, 순서를 바꾸면 위에서 구한 위치가 밀린다.
    int[] tag = slotTag(xml, set, slot);
    String withSlot;
    if (tag != null) {
      String t = xml.substring(tag[0], tag[1]);
      String id = attr(t, "itemId");
      String changed =
          id == null
              ? t.replace("<Slot ", "<Slot itemId=\"" + newId + "\" ")
              : t.replace(" itemId=\"" + id + "\"", " itemId=\"" + newId + "\"");
      withSlot = xml.substring(0, tag[0]) + changed + xml.substring(tag[1]);
    } else if (xml.charAt(set[1] - 2) == '/') {
      // 자식 없는 세트 — 여는 태그를 풀어 칸을 넣는다
      String open = xml.substring(set[0], set[1] - 2) + ">";
      withSlot =
          xml.substring(0, set[0])
              + open
              + "<Slot name=\""
              + slot
              + "\" itemId=\""
              + newId
              + "\"/></ItemSet>"
              + xml.substring(set[1]);
    } else {
      int close = set[1] - "</ItemSet>".length();
      withSlot =
          xml.substring(0, close)
              + "<Slot name=\""
              + slot
              + "\" itemId=\""
              + newId
              + "\"/>\n"
              + xml.substring(close);
    }
    int at = withSlot.indexOf("<ItemSet ");
    String itemXml = "<Item id=\"" + newId + "\">\n" + escapeXml(itemText) + "</Item>\n";
    return withSlot.substring(0, at) + itemXml + withSlot.substring(at);
  }

  private static int maxItemId(String xml) {
    int max = 0;
    int from = 0;
    while (true) {
      int a = xml.indexOf("<Item ", from);
      if (a < 0) {
        return max;
      }
      int gt = xml.indexOf('>', a);
      if (gt < 0) {
        return max;
      }
      max = Math.max(max, parseInt(attr(xml.substring(a, gt + 1), "id"), 0));
      from = gt;
    }
  }

  /** 활성 세트에 낀 고유 아이템(유물 포함) 영문 이름들 — 같은 고유를 또 권하지 않는다. */
  static Set<String> wornUniqueNames(String xml, int[] set) {
    Set<String> out = new LinkedHashSet<>();
    int from = set[0];
    while (true) {
      int a = xml.indexOf("<Slot ", from);
      if (a < 0 || a >= set[1]) {
        break;
      }
      int b = xml.indexOf("/>", a);
      if (b < 0) {
        break;
      }
      String id = attr(xml.substring(a, b), "itemId");
      if (id != null && !"0".equals(id)) {
        String name = uniqueNameOf(itemText(xml, id));
        if (name != null) {
          out.add(name);
        }
      }
      from = b + 2;
    }
    return out;
  }

  /** {@code <Item id="..">} 의 내용 — 속성 순서와 무관하게 id 로 찾는다. 없으면 빈 문자열. */
  private static String itemText(String xml, String id) {
    int from = 0;
    while (true) {
      int a = xml.indexOf("<Item ", from);
      if (a < 0) {
        return "";
      }
      int gt = xml.indexOf('>', a);
      if (gt < 0) {
        return "";
      }
      if (id.equals(attr(xml.substring(a, gt + 1), "id"))) {
        int end = xml.indexOf("</Item>", gt);
        return end < 0 ? "" : xml.substring(gt + 1, end);
      }
      from = gt;
    }
  }

  /** 아이템 원문이 고유(유물 포함)면 이름 줄(XML 엔티티를 푼 것), 아니면 null. */
  static String uniqueNameOf(String text) {
    String[] lines = text.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (!line.startsWith("Rarity:")) {
        continue;
      }
      String rarity = line.substring("Rarity:".length()).trim();
      if (!"UNIQUE".equals(rarity) && !"RELIC".equals(rarity)) {
        return null;
      }
      for (int j = i + 1; j < lines.length; j++) {
        String name = lines[j].trim();
        if (!name.isEmpty()) {
          return unescapeXml(name);
        }
      }
      return null;
    }
    return null;
  }

  private static String escapeXml(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String unescapeXml(String s) {
    return s.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&");
  }
}
