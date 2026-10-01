package net.luversof.web.gate.poe2.dto;

import java.util.List;

/**
 * PoE2 표시용 레코드 — bluesky-api-poe 의 net.luversof.api.poe.poe2.Poe2 와 필드 이름을 맞춘다(그쪽이 원본).
 *
 * <p>⚠ 비어 있을 수 있는 숫자는 래퍼 타입이다 — Jackson 3 은 primitive 필드에 null/부재가 오면
 * 실패한다(FAIL_ON_NULL_FOR_PRIMITIVES).
 */
public final class Poe2 {

  private Poe2() {}

  // ─────────────────────────── 젬 ───────────────────────────

  /**
   * @param kind skill | support | meta
   * @param color str | dex | int | white
   */
  public record Gem(
      String id,
      String slug,
      String name,
      String nameKo,
      String kind,
      String color,
      Integer tier,
      Integer maxLevel,
      String gemType,
      String weaponRequirements,
      Integer reqStr,
      Integer reqDex,
      Integer reqInt,
      List<String> tags,
      List<String> tagsKo,
      String description,
      String descriptionKo,
      String supportText,
      String supportTextKo,
      List<String> family,
      List<String> familyKo,
      Boolean lineage,
      String icon,
      String image,
      Integer castTimeMs,
      List<GemLevel> levels) {

    /** 목록용 — 레벨별 수치를 뺀 사본(전체 젬 목록 응답이 수 MB 가 되지 않게). */
    public Gem withoutLevels() {
      return new Gem(
          id,
          slug,
          name,
          nameKo,
          kind,
          color,
          tier,
          maxLevel,
          gemType,
          weaponRequirements,
          reqStr,
          reqDex,
          reqInt,
          tags,
          tagsKo,
          description,
          descriptionKo,
          supportText,
          supportTextKo,
          family,
          familyKo,
          lineage,
          icon,
          image,
          castTimeMs,
          null);
    }
  }

  /** 젬 한 레벨 — 비용(costType: Mana 등) · 정신력 예약 · 재사용 대기 · 치명타 확률(%) · 스탯 문장(영/한). */
  public record GemLevel(
      Integer level,
      Integer cost,
      String costType,
      Integer reservation,
      Integer cooldownMs,
      Double critChance,
      List<String> statLines,
      List<String> statLinesKo,
      // 요구 캐릭터 레벨 — PoB Data/Skills levelRequirement(tools/poe2-extract/parse-gems2.mjs, 10-01)
      Integer requiredLevel) {}

  public record GemData(String patch, List<Gem> gems) {}

  /** 트리 평가 한 줄 — API Poe2.TreeEvalRow 와 쌍. */
  public record TreeEvalRow(String key, Double value) {}

  /** 패시브 트리 평가(10-01) — API Poe2.TreeEval 과 쌍. */
  public record TreeEval(
      String className,
      String ascendancy,
      String skill,
      int nodes,
      List<TreeEvalRow> rows,
      String error,
      long elapsedMs) {}

  // ─────────────────────────── 베이스 아이템 ───────────────────────────

  public record ModLine(String en, String ko) {}

  /** 젬 태그 묶음 — API Poe2.TagGroup 과 쌍. 이름은 PoE1 메시지 poe.gemgroup.* */
  public record TagGroup(String key, List<ModLine> tags) {}

  public record Armour(
      Integer armour,
      Integer evasion,
      Integer energyShield,
      Double movementPenalty,
      Integer block) {}

  public record Weapon(
      Integer damageMin,
      Integer damageMax,
      Double critChance,
      Double attacksPerSecond,
      Integer range,
      Integer reloadTime) {}

  public record Flask(
      Integer type, Integer lifePerUse, Integer manaPerUse, Double recoverySeconds) {}

  public record BaseItem(
      String name,
      String nameKo,
      String slug,
      String itemClass,
      String itemClassKo,
      String category,
      Integer dropLevel,
      Integer reqLevel,
      Integer reqStr,
      Integer reqDex,
      Integer reqInt,
      Integer width,
      Integer height,
      Armour armour,
      Weapon weapon,
      Flask flask,
      List<ModLine> implicits,
      List<String> tags,
      String subType,
      String icon,
      String image) {}

  public record ItemClass(String key, String ko, String category) {}

  public record BaseItemData(String patch, List<ItemClass> classes, List<BaseItem> items) {}

  // ─────────────────────────── 옵션 ───────────────────────────

  public record ModTier(
      String id,
      String name,
      String nameKo,
      Integer level,
      Integer weight,
      List<String> text,
      List<String> textKo) {}

  /** 같은 계열(ModType)의 티어 사다리 — tiers[0] 이 1티어(레벨이 가장 높은 것). gen = prefix | suffix. */
  public record ModGroup(String modType, String gen, List<ModTier> tiers) {}

  /** 옵션 풀 — 같은 아이템 클래스에서 스폰 태그 조합이 같은 베이스 묶음(갑옷은 방어 속성별로 갈린다). */
  public record ModPool(
      String key,
      String itemClass,
      String itemClassKo,
      String category,
      String variant,
      String variantKo,
      List<String> bases,
      List<ModGroup> groups) {}

  /** 풀 목록용 요약(옵션 본문 없이). */
  public record ModPoolSummary(
      String key,
      String itemClass,
      String itemClassKo,
      String category,
      String variant,
      String variantKo,
      Integer baseCount,
      Integer groupCount) {}

  public record ModData(String patch, List<ModPool> pools) {}

  // ─────────────────────────── 증강물(룬 · 영혼 핵 · 우상 …) ───────────────────────────

  public record AugmentEffect(
      String target,
      String targetKo,
      List<String> targetClasses,
      List<String> lines,
      List<String> linesKo) {}

  public record Augment(
      String name,
      String nameKo,
      String slug,
      String kind,
      String kindKo,
      Integer level,
      Integer limit,
      String limitText,
      String limitTextKo,
      List<AugmentEffect> effects,
      String icon,
      String image) {}

  public record AugmentData(String patch, List<Augment> items) {}

  // ─────────────────────────── 고유 아이템 ───────────────────────────

  public record UniqueVariant(
      Integer index,
      String name,
      String nameKo,
      List<String> implicits,
      List<String> implicitsKo,
      List<String> explicits,
      List<String> explicitsKo) {}

  public record Unique(
      String name,
      String nameKo,
      String slug,
      String baseType,
      String baseTypeKo,
      String itemClass,
      String itemClassKo,
      String category,
      Integer requiredLevel,
      List<String> implicits,
      List<String> implicitsKo,
      List<String> explicits,
      List<String> explicitsKo,
      List<UniqueVariant> variants,
      Integer defaultVariant,
      // 로어(플레이버) 줄 — 위키 + 게임 FlavourText 매칭(tools/poe2-extract/wiki-uniques2.mjs, 10-01)
      List<String> flavour,
      List<String> flavourKo,
      // 리그 출처(해당 리그 기제를 돌려야 얻는다) — 데이터엔 있었는데 레코드에 없어 화면에 못 보였다(10-01, 443개 중 173개)
      String league,
      String image) {}

  public record UniqueData(String patch, List<Unique> items) {}

  // ─────────────────────────── 빌드(PoB2 코드) ───────────────────────────

  /** 빌드 요약 — PoB(PoE2) 코드 한 벌. 이름은 데이터와 맞춰 한국어를 붙이고, 못 맞추면 영문 그대로. */
  public record BuildSummary(
      String className,
      String classNameKo,
      String ascendancy,
      String ascendancyKo,
      Integer level,
      String treeVersion,
      List<BuildStat> stats,
      List<BuildSkill> skills,
      List<BuildItem> items,
      BuildTree tree,
      String treeLink) {}

  public record BuildStat(String key, Double value) {}

  public record BuildSkill(String slot, Boolean enabled, Boolean main, List<BuildGem> gems) {}

  public record BuildGem(
      String name,
      String nameKo,
      String slug,
      Integer level,
      Integer quality,
      Boolean support,
      Boolean enabled,
      String color,
      String image) {}

  public record BuildItem(
      String slot,
      String rarity,
      String name,
      String nameKo,
      String baseType,
      String baseTypeKo,
      String uniqueSlug,
      String baseSlug,
      String image,
      List<String> mods,
      List<String> modsKo) {}

  public record BuildNode(Integer id, String name, String nameKo) {}

  // ── 업그레이드 가이드(API Poe2.BuildGuide 와 같은 모양) ──
  public record GuideBase(
      Double dps,
      Double ehp,
      Double life,
      Double es,
      Double maxHit,
      String skill,
      String skillKo) {}

  public record GuideSlot(
      String slot,
      String item,
      String itemKo,
      String base,
      String rarity,
      Double dps,
      Double ehp,
      Double maxHit) {}

  public record GuideSupport(
      String name,
      String nameKo,
      String slug,
      Boolean applied,
      Double dps,
      Double ehp,
      Boolean lineage) {}

  public record GuideNode(
      Integer id,
      String name,
      String nameKo,
      String type,
      String ascendancy,
      Double dps,
      Double ehp) {}

  public record GuideSwap(
      String slot,
      String item,
      String itemKo,
      String slug,
      String image,
      String base,
      Double dps,
      Double ehp,
      Double maxHit,
      Double life) {}

  public record GuideMod(
      String modType,
      String gen,
      String tierName,
      String tierNameKo,
      List<String> lines,
      List<String> linesKo,
      Double dps,
      Double ehp) {}

  public record GuideModTarget(
      String slot,
      String item,
      String itemKo,
      Integer tried,
      List<GuideMod> dps,
      List<GuideMod> ehp) {}

  /** 다음에 찍을 특화·핵심 — points = 가는 길 포함 필요 점수, dps/ehp = 길 전체를 찍었을 때 증감(%). */
  public record GuideNextNode(
      Integer id,
      String name,
      String nameKo,
      String type,
      Integer points,
      Double dps,
      Double ehp,
      String treeLink) {}

  public record BuildGuide(
      Boolean available,
      String error,
      Long elapsedMs,
      GuideBase base,
      Integer unappliedSupports,
      Boolean mainSkillCostWarning,
      List<GuideSlot> slots,
      List<GuideSupport> supports,
      List<GuideNode> nodes,
      List<GuideSwap> swapsDps,
      List<GuideSwap> swapsEhp,
      Integer tried,
      String gemWeakest,
      String gemWeakestKo,
      List<GuideSupport> gemSwaps,
      Integer gemTried,
      List<GuideModTarget> modTargets,
      List<GuideNextNode> nextDps,
      List<GuideNextNode> nextEhp,
      Integer nextTried,
      // 무기 세트를 나눠 쓰는 빌드면 이 가이드를 계산한 세트(1·2)와 주 세트(DPS 큰 쪽, 세트를 지정해 부르면 null), 아니면 둘 다 null(10-01)
      Integer weaponSet,
      Integer mainSet) {}

  /** 엔진 재계산 한 줄 — PoB 저장값 · PoB-PoE2 엔진 재계산값. */
  /** set1·set2 = 무기 세트를 나눠 쓰는 빌드일 때 세트별 재계산(아니면 null). computed 는 저장 당시 켜진 세트 값(저장값과 비교용). */
  public record BuildRecalcRow(
      String key, Double saved, Double computed, Double set1, Double set2) {}

  /** 엔진 재계산(API Poe2.BuildRecalc 와 같은 모양). */
  public record BuildRecalc(
      Boolean available,
      String error,
      Long elapsedMs,
      List<BuildRecalcRow> rows,
      Integer unappliedSupports,
      Boolean mainSkillCostWarning,
      Integer costWarnings,
      // 무기 세트를 나눠 쓰는 빌드면 저장 당시 켜진 세트·주 세트(DPS 가 큰 쪽), 아니면 null(10-01)
      Integer savedSet,
      Integer mainSet) {}

  public record BuildTree(
      Integer allocated,
      Integer ascendancyPoints,
      List<BuildNode> keystones,
      List<BuildNode> notables,
      List<BuildNode> ascendancyNodes) {}

  // ─────────────────────────── 요약 ───────────────────────────

  public record Meta(
      String patch,
      Integer gems,
      Integer baseItems,
      Integer modPools,
      Integer augments,
      Integer uniques,
      Boolean tree,
      String loadedAt) {}

  /** 데이터 추출(run-all2.mjs) 상태 — log 는 마지막 200줄. */
  public record ExtractStatus(
      Boolean running, String startedAt, String finishedAt, Integer exitCode, List<String> log) {}

  // ── poe.ninja 실빌드 시뮬레이터(/poe2/sim) — API Poe2NinjaService·Poe2RefineService 레코드와 필드 이름을 맞춘다 ──
  // ⚠ 숫자는 래퍼 타입(Jackson 3 FAIL_ON_NULL_FOR_PRIMITIVES 기본 ON)

  public record NinjaCount(String name, Integer count) {}

  public record NinjaStart(
      Integer level,
      Double dps,
      Double ehp,
      Double life,
      Double es,
      Integer measured,
      Double medianDps,
      Double medianEhp,
      String code) {}

  public record NinjaArchetype(
      String ascendancy,
      String mainSkill,
      Integer sample,
      Double medianLevel,
      Double medianLife,
      Double medianEs,
      Double medianEhp,
      Double medianDps,
      String lean,
      List<NinjaCount> topKeystones,
      List<NinjaCount> topCoSkills,
      Integer facetTotal,
      List<NinjaCount> topItems,
      List<NinjaCount> topAnointed,
      NinjaStart start) {}

  public record NinjaOverview(
      String league,
      Double globalMedianDps,
      Double globalMedianEhp,
      List<NinjaArchetype> archetypes) {}

  public record RefineMetrics(Double dps, Double ehp, Double life, Double es) {}

  public record RefineStep(String label, Double dpsPct, Double ehpPct) {}

  /** mainSet·sets — 무기 세트를 나눠 쓰는 빌드일 때만(API Poe2RefineService.Result 와 쌍). */
  public record RefineResult(
      RefineMetrics before,
      RefineMetrics after,
      List<RefineStep> steps,
      String code,
      Long durationMs,
      Integer mainSet,
      List<RefineSetMetrics> sets) {}

  /** 무기 세트 하나의 다듬기 전/후(API Poe2RefineService.SetMetrics). */
  public record RefineSetMetrics(Integer set, RefineMetrics before, RefineMetrics after) {}

  public record RefineStatus(
      Boolean running,
      Integer round,
      Integer rounds,
      String phase,
      RefineResult result,
      String error) {}

  // ── 시뮬레이터(/poe2/sim) — API Poe2NinjaService.Options · Poe2SimService 레코드와 필드 이름을 맞춘다 ──

  /** 선택지 — 인원은 없다(API 가 poe.ninja 스냅샷 집계로 순서만 정해 준다). */
  public record SimChoice(String name, String nameKo) {}

  /** snapshot·fetchedAt = 순서의 근거(poe.ninja 버전, 받은 시각) — 새 버전이 나오면 API 가 다시 받는다. */
  /** 전직 셀렉트의 직업별 묶음(PoE1 시뮬 폼과 같은 optgroup) — 트리 순서. */
  public record SimClassGroup(String name, String nameKo, List<SimChoice> ascendancies) {}

  /** popularAscendancies = "많이 쓰는 직업" 묶음(상위 5, 이름만), classes = 직업별 묶음. */
  public record SimOptions(
      String league,
      String snapshot,
      String fetchedAt,
      List<SimChoice> skills,
      List<SimChoice> ascendancies,
      List<String> popularAscendancies,
      List<SimClassGroup> classes) {}

  public record SimCandidate(
      Integer level,
      Double ninjaDps,
      Double ninjaEhp,
      Double dps,
      Double ehp,
      Boolean chosen,
      String error,
      // 이 후보를 잰 무기 세트(1·2, DPS 가 큰 쪽) — 세트를 안 나누는 빌드면 null
      Integer set) {}

  public record SimResult(
      String skill,
      String skillKo,
      String ascendancy,
      String objective,
      String lean,
      String scenario,
      List<SimCandidate> candidates,
      NinjaArchetype benchmark,
      RefineMetrics start,
      RefineMetrics refined,
      List<RefineStep> steps,
      String code,
      Long durationMs,
      Integer mainSet,
      List<RefineSetMetrics> sets,
      // 고정 고유(10-01) — 없으면 null
      String forcedUnique,
      String forcedUniqueKo) {}

  /** 결과 이력 한 건(목록용 요약) — API Poe2SimService.HistoryEntry. id = 저장 시각 epochMs. */
  public record SimHistoryEntry(
      long id,
      String skill,
      String skillKo,
      String ascendancy,
      String objective,
      String scenario,
      Double startDps,
      Double startEhp,
      Double refinedDps,
      Double refinedEhp,
      long durationMs) {}

  /** 젬 DPS 랭킹 한 건 — API Poe2SimRankingService.GemRank(PoE1 PoeGemRank 와 같은 필드). */
  public record SimGemRank(
      String slug,
      String name,
      String nameKo,
      String color,
      List<String> tagsKo,
      String weapon,
      double dps,
      double averageDamage,
      double speed) {}

  public record SimRankingData(String patch, List<SimGemRank> ranking) {}

  public record SimRankingStatus(
      boolean available,
      boolean running,
      String status,
      int progressDone,
      int progressTotal,
      List<String> logLines) {}

  public record SimStatus(
      Boolean running,
      String phase,
      Integer round,
      Integer rounds,
      SimResult result,
      String error) {}
}
