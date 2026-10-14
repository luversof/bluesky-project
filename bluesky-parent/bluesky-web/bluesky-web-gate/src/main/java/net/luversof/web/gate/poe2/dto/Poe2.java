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
      // 설명 · 레벨 문장의 강조 용어 정의(KeywordPopups, 인게임 Alt — 10-04 C111). 목록 사본엔 없다
      List<Keyword> keywords,
      List<GemLevel> levels,
      // 퀄리티로 인한 추가 효과(퀄리티 20% 값, API 와 짝, 10-08 C195) — 목록 사본엔 없다
      List<String> qualityStatLines,
      List<String> qualityStatLinesKo,
      // 필요 무기 한국어(게임 ClientStrings · ItemClasses, 10-09 C228). 못 옮기면 null
      String weaponRequirementsKo,
      // 주 스탯 묶음 머리글(추가 묶음이 있을 때, 10-09 C236). 없으면 null
      String statSetLabel,
      String statSetLabelKo,
      // 젬이 덤으로 주는 스킬(10-10 C249). 없으면 null
      List<GrantedSkill> grantedSkills) {

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
          null,
          null,
          null,
          null,
          weaponRequirementsKo,
          statSetLabel,
          statSetLabelKo,
          grantedSkills);
    }
  }

  /** 강조 용어 하나 — 인게임 툴팁에서 Alt 로 보는 용어 · 정의(PoE2 KeywordPopups, 영 · 한). */
  public record Keyword(String term, String termKo, String def, String defKo) {}

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
      Integer requiredLevel,
      // 공격 스킬 배율(인게임 "공격 피해: 기본 수치의 95%" · "공격 속도: 기본 수치의 90%", 10-08 C200 — API 와 짝). 없으면 null
      Integer attackDamagePct,
      Integer attackSpeedPct,
      // 보조 젬 소모 · 점유 배율 %(PoB2 manaMultiplier · reservationMultiplier, 10-09 C231). 없으면 null
      Integer costMultiplierPct,
      Integer reservationMultiplierPct,
      // 추가 스탯 묶음(PoE2 GrantedEffects.AdditionalStatSets — 머리글 "폭파" 등, 10-09 C236). 없으면 null
      List<GemStatSet> statSets,
      // 젬이 덤으로 주는 스킬의 이 레벨 수치(10-10 C249 — Gem.grantedSkills 와 같은 순서, 그 레벨 행이 없으면 null)
      List<GrantedSkillLevel> grantedSkills) {}

  /** 추가 스탯 묶음 한 개(10-09 C236) — 머리글(GrantedEffectLabels) · 치명타 · 공격 피해 · 스탯 줄. */
  public record GemStatSet(
      String label,
      String labelKo,
      Double critChance,
      Integer attackDamagePct,
      List<String> statLines,
      List<String> statLinesKo) {}

  /**
   * 젬이 덤으로 주는 스킬(10-10 C249 — GemEffects.AdditionalGrantedEffects, poe2db 세차게 흐르는 전류 칸). 머리글 =
   * label, 없으면 name.
   */
  public record GrantedSkill(
      String label,
      String labelKo,
      String name,
      String nameKo,
      String description,
      String descriptionKo,
      Boolean copiesLevel,
      Integer castTimeMs,
      // 메타 젬의 보조형 효과(10-10 C250 — 이름 없음, 머리글 "보조")
      Boolean support) {}

  /** 부여 스킬의 레벨별 수치(10-10 C249). */
  public record GrantedSkillLevel(
      Integer cost,
      String costType,
      Integer cooldownMs,
      Double critChance,
      Integer attackDamagePct,
      List<String> statLines,
      List<String> statLinesKo,
      // 부여 스킬의 추가 스탯 묶음(poe2db 촉진시키는 방출 "화염" · "번개" · "냉기"). 없으면 null
      List<GemStatSet> statSets) {}

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
      long elapsedMs,
      // 무기 세트 전용 노드가 있으면 세트 II 를 켠 계산(rows = 세트 I) — 10-02
      List<TreeEvalRow> rowsSet2) {}

  // ─────────────────────────── 베이스 아이템 ───────────────────────────

  public record ModLine(String en, String ko) {}

  /** 젬 태그 묶음 — API Poe2.TagGroup 과 쌍. 이름은 PoE1 메시지 poe.gemgroup.* */
  public record TagGroup(String key, List<ModLine> tags) {}

  public record Armour(
      Integer armour,
      Integer evasion,
      Integer energyShield,
      Double movementPenalty,
      Integer block,
      // 룬 수호(Runic Ward, API 와 짝, 10-08 C185). 없으면 null
      Integer ward) {

    /** 수호 없는 방어(시험 · 옛 데이터용). */
    public Armour(
        Integer armour,
        Integer evasion,
        Integer energyShield,
        Double movementPenalty,
        Integer block) {
      this(armour, evasion, energyShield, movementPenalty, block, null);
    }
  }

  public record Weapon(
      Integer damageMin,
      Integer damageMax,
      Double critChance,
      Double attacksPerSecond,
      Integer range,
      Integer reloadTime) {}

  public record Flask(
      Integer type,
      Integer lifePerUse,
      Integer manaPerUse,
      Double recoverySeconds,
      // 충전 최대 · 사용당 소모(API 와 짝, 10-08 C189) — 없으면 null
      Integer maxCharges,
      Integer perCharge,
      // 호신부 버프 줄(API 와 짝, 10-08 C189) — 없으면 null
      List<ModLine> buffLines) {

    /** 충전 · 버프 없는 플라스크(시험 · 옛 데이터용). */
    public Flask(Integer type, Integer lifePerUse, Integer manaPerUse, Double recoverySeconds) {
      this(type, lifePerUse, manaPerUse, recoverySeconds, null, null, null);
    }
  }

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
      String image,
      // 암시 줄의 강조 용어 정의(KeywordPopups, 인게임 Alt — 10-04 C113)
      List<Keyword> keywords,
      // 거래소가 이 베이스를 아는가(10-08 C174, 상세 응답만) — false 면 "거래소에서 찾기" 단추를 숨긴다, null = 모름(보인다)
      Boolean tradable,
      // 정신력(셉터 베이스 속성, API 와 짝, 10-08 C188) — 없으면 null
      Integer spirit) {}

  /** en = 게임 영어 분류 이름(옛 데이터면 null). 화면 글자는 enName() — 없으면 key. */
  public record ItemClass(String key, String ko, String category, String en) {
    public String enName() {
      return en == null || en.isBlank() ? key : en;
    }
  }

  public record BaseItemData(String patch, List<ItemClass> classes, List<BaseItem> items) {}

  // ─────────────────────────── 옵션 ───────────────────────────

  public record ModTier(
      String id,
      String name,
      String nameKo,
      Integer level,
      Integer weight,
      List<String> text,
      List<String> textKo,
      // text 줄별 강조 용어 Id(10-04 C132, API 와 쌍)
      List<List<String>> kw) {}

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

  public record ModData(
      String patch,
      List<ModPool> pools,
      java.util.Map<String, Keyword> keywords,
      // 풀 밖 옵션의 영 · 한 쌍(10-04 C133, API 와 쌍)
      List<ModPair> extra) {}

  public record ModPair(List<String> text, List<String> textKo) {}

  // ─────────────────────────── 증강물(룬 · 영혼 핵 · 우상 …) ───────────────────────────

  public record AugmentEffect(
      String target,
      String targetKo,
      List<String> targetClasses,
      List<String> lines,
      List<String> linesKo,
      // 결속 효과(인게임 "결속됨:" — SoulCoreStats.BondedStats, 10-09 C238). 없으면 null
      List<String> bondedLines,
      List<String> bondedLinesKo) {}

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
      Integer stackSize,
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
      List<String> explicitsKo,
      // 기본과 용어가 다른 변형만 자기 키워드 정의(빈 목록 = 없음), null 이면 고유 keywords 그대로(10-04 C117)
      List<Keyword> keywords) {}

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
      String image,
      // 주얼 반경(10-03 C74) — 옛 응답엔 없어 null
      String radius,
      // 기본 암시 · 옵션 줄의 강조 용어 정의(KeywordPopups, 인게임 Alt — 10-04 C112, 젬 Gem.keywords 와 같은 모양)
      List<Keyword> keywords,
      // 중복 사용 제한(API 와 짝, 10-08 C197) — 없으면 null
      String limit) {}

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
      String treeLink,
      // 거래소 주소 경로의 리그(API 와 짝, 10-08) — null 이면 Standard
      String tradeLeague) {}

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
      List<String> modsKo,
      // mods 앞 N줄이 암시 · 레어 등 옵션 줄의 강조 용어 정의(10-04 C132, API 와 쌍)
      Integer implicitCount,
      List<Keyword> keywords,
      // 고유 로어(10-04 C140, API 와 쌍)
      List<String> flavour,
      List<String> flavourKo,
      // 베이스(속성 칸 · 요구 사항, 10-04 C147, API 와 쌍)
      BaseItem base,
      // 품질 %(10-04 C148, API 와 쌍)
      Integer quality,
      // 거래소 검색 쿼리(q JSON, API 와 짝, 10-08) — null 이면 링크 없음
      String tradeQuery,
      // 인게임 요구 레벨(10-08 C179, API 와 짝) — null 이면 베이스 요구 레벨
      Integer levelReq,
      // 실제 방어 수치(10-08 C184, API 와 짝) — null 이면 베이스 + 로컬 옵션 계산
      net.luversof.web.gate.poe.dto.PoeBuild.Defences defences) {}

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
      Double ehp,
      // 빼면 풀리는 점수 · 무기 세트 전용이면 그 세트(10-14 C316 — API 와 짝, 꺼진 세트 노드는 안 실린다)
      Integer free,
      Integer set) {}

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
      Double life,
      // 거래소 검색 쿼리(고유 이름, API 와 짝, 10-08 C172)
      String tradeQuery) {}

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
      String treeLink,
      // 켜진 무기 세트에서 출발하는 길이면 그 세트(그 세트 점수로 찍는다, 10-14 C316)
      Integer set) {}

  /** 레어 목표만(10-02) — API Poe2.GuideRares. */
  public record GuideRares(
      Boolean available,
      String error,
      Long elapsedMs,
      Integer weaponSet,
      List<GuideRareTarget> rareTargets) {}

  /** 레어 목표 한 칸 — item/rarity = 지금 낀 것, dps/ehp = 그 축에 맞춰 고른 레어(없으면 null). */
  public record GuideRareTarget(
      String slot,
      String item,
      String itemKo,
      String rarity,
      String base,
      String baseKo,
      Integer tried,
      GuideRare dps,
      GuideRare ehp) {}

  /** 고른 레어 — 옵션 줄(영문 · 한국어)과 지금 아이템 대비 증감(%). */
  /**
   * itemText = PoB 아이템 텍스트(영어, 엔진이 잰 것과 같은 모양 — 희귀도 · 이름 · 베이스 · 암시 · 옵션). PoB "Create custom" 에
   * 그대로 붙여 넣는다(10-02).
   */
  public record GuideRare(
      Double dps,
      Double ehp,
      List<String> lines,
      List<String> linesKo,
      String itemText,
      // 거래소 검색 쿼리(분류 + 옵션, API 와 짝, 10-08 C172)
      String tradeQuery) {}

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
      Integer mainSet,
      // 레어 목표 — 칸마다 같은 베이스의 좋은 레어(2티어 중간 롤 접두3·접미3)로 바꾸면(10-02 사용자 요청) — API Poe2.GuideRareTarget
      List<GuideRareTarget> rareTargets) {}

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
      String loadedAt,
      // 거래소 주소 경로의 리그(API 와 짝, 10-08 C172)
      String tradeLeague) {}

  /**
   * 데이터 추출(run-all2.mjs) 상태 — log 는 마지막 200줄. 실행 가능 · ImageMagick · 엔진 · 결과는 PoE1 관리 화면 짝(API 와 짝,
   * 10-12 C261).
   */
  public record ExtractStatus(
      Boolean running,
      String startedAt,
      String finishedAt,
      Integer exitCode,
      List<String> log,
      Boolean available,
      Boolean imageMagickInstalled,
      Boolean engineAvailable,
      String status) {}

  /** 데이터 버전 — 현재 · 설정 · 패치 서버 최신(GGG 식 번호) + 카카오 CDN 번호(API 와 짝, 10-12 C261). */
  public record ExtractVersion(
      String dataPatch,
      String configPatch,
      String latestPatch,
      Boolean upToDate,
      String configCdnVersion,
      String latestCdnVersion) {}

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

  /**
   * phase = 단계 코드(baseline · unique · guide · apply → poe2.refine.phase.*), evals = 엔진 계산 수(10-12
   * C269).
   */
  public record RefineStatus(
      Boolean running,
      Integer round,
      Integer rounds,
      String phase,
      RefineResult result,
      String error,
      Integer evals) {}

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
      Integer set,
      // 후보의 전직(영문 · 한국어, C311 — API 와 짝)
      String ascendancy,
      String ascendancyKo) {}

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
      String forcedUniqueKo,
      // 내 트리에서 출발이면 그 노드 수(10-02)
      Integer fromTreeNodes,
      // 고른 전직 한국어 · 전직 자동 여부(C311 — API 와 짝, 옛 이력엔 null)
      String ascendancyKo,
      Boolean ascendancyAuto,
      // 참고한 닌자 공통 특성(10-14 — API 와 짝): 실빌드 수(0 = 집계 대상 아님) · 공통 트리 겹침(0~1). 옛 이력이면 null
      Integer consensusN,
      Double consensusOverlap,
      // 전직 자동일 때 전직별 비교(10-14 P5 — API 와 짝): 최고 후보 · 그 후보에 공통 트리를 덮은 값. 옛 이력 · 전직 지정이면 null
      List<AscCompare> ascCompare) {}

  /** 닌자 공통 특성 중 빌드에 없는 것 한 줄(10-14 C318 — API Poe2CommonGapsService.Gap). slot 은 고유만. */
  public record CommonGap(String name, String nameKo, String slot, int count) {}

  /** 닌자 공통 특성 비교(C318) — n = 0 이면 그 전직 · 스킬은 집계 대상이 아님. */
  public record CommonGaps(
      int n,
      String ascendancy,
      String ascendancyKo,
      String skill,
      String skillKo,
      List<CommonGap> supports,
      List<CommonGap> uniques,
      List<CommonGap> nodes) {}

  /** 전직 비교 한 줄(P5) — API Poe2SimService.AscCompare. */
  public record AscCompare(
      String ascendancy,
      String ascendancyKo,
      Double dps,
      Double ehp,
      Double treeDps,
      Double treeEhp,
      Integer consensusN,
      boolean chosen) {}

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
      long durationMs,
      // 전직 한국어 이름(10-03 C57) — 옛 API 응답엔 없어 null 이면 영문
      String ascendancyKo) {}

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

  /**
   * phase = 단계 코드(fetch · measure · refine.&lt;다듬기 단계&gt;), round/rounds = 그 단계 진행, evals = 엔진 계산
   * 수, cancelled = 사용자가 중지한 마지막 실행(PoE1 simOptimizeStatus 짝, 10-12 C269).
   */
  public record SimStatus(
      Boolean running,
      String phase,
      Integer round,
      Integer rounds,
      SimResult result,
      String error,
      Integer evals,
      Boolean cancelled) {}
}
