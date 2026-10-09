package net.luversof.api.poe.poe2;

import java.util.List;
import java.util.Map;

/**
 * PoE2 표시용 레코드 — tools/poe2-extract 파서가 만든 ~/.poe-gamedata/poe2/*.json 과 필드 이름을 맞춘다.
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
          null,
          null);
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
      Integer requiredLevel) {}

  public record GemData(String patch, List<Gem> gems) {}

  // ─────────────────────────── 트리 평가(10-01) ───────────────────────────

  /** 트리 평가 한 줄 — 키는 빌드 재계산(BuildRecalcRow)과 같은 PoB 스탯 이름. */
  public record TreeEvalRow(String key, Double value) {}

  /**
   * 패시브 트리 평가 — 찍은 노드(+전직)로 레벨 90 캐릭터를 PoB-PoE2 엔진에 넣은 계산(PoE1 트리 "트리 계산"과 같은 자리). 스킬을 고르면 시뮬 랭킹과
   * 같은 표준 무기로 그 스킬 DPS 까지, 안 고르면 방어·능력치만.
   */
  public record TreeEval(
      String className,
      String ascendancy,
      String skill,
      int nodes,
      List<TreeEvalRow> rows,
      String error,
      long elapsedMs,
      // 무기 세트 전용 노드가 있으면 세트 II 를 켠 계산(rows = 세트 I) — 10-02, 없으면 null
      List<TreeEvalRow> rowsSet2) {}

  // ─────────────────────────── 베이스 아이템 ───────────────────────────

  public record ModLine(String en, String ko) {}

  /** 젬 태그 묶음(key = 묶음 id, 이름은 게이트가 PoE1 과 같은 메시지 poe.gemgroup.* 로) — 10-01. */
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
      String image,
      // 암시 줄의 강조 용어 정의(KeywordPopups, 인게임 Alt — 10-04 C113)
      List<Keyword> keywords,
      // 거래소가 이 베이스를 아는가(10-08 C174, 상세 응답에만 채운다) — null = 모름
      Boolean tradable) {

    /** 상세 응답용 — 거래소 사전 대조 결과를 실은 사본. */
    public BaseItem withTradable(Boolean t) {
      return new BaseItem(
          name,
          nameKo,
          slug,
          itemClass,
          itemClassKo,
          category,
          dropLevel,
          reqLevel,
          reqStr,
          reqDex,
          reqInt,
          width,
          height,
          armour,
          weapon,
          flask,
          implicits,
          tags,
          subType,
          icon,
          image,
          keywords,
          t);
    }

    /** 목록용 — 키워드 정의를 뺀 사본(10-04 C117). */
    public BaseItem withoutKeywords() {
      return new BaseItem(
          name,
          nameKo,
          slug,
          itemClass,
          itemClassKo,
          category,
          dropLevel,
          reqLevel,
          reqStr,
          reqDex,
          reqInt,
          width,
          height,
          armour,
          weapon,
          flask,
          implicits,
          tags,
          subType,
          icon,
          image,
          null,
          tradable);
    }
  }

  /** en = 게임 영어 분류 이름(옛 데이터면 null — 화면은 key 로 대신). */
  public record ItemClass(String key, String ko, String category, String en) {}

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
      // text 줄별 강조 용어 Id(mods.json keywords 사전 키 — 10-04 C132). 없으면 null
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
      int baseCount,
      int groupCount) {}

  public record ModData(
      String patch,
      List<ModPool> pools,
      Map<String, Keyword> keywords,
      // 풀 밖 옵션의 영 · 한 쌍(타락 · 에센스 · 영혼 핵 · 무기 국소 …) — 빌드 요약 번역 사전 보강(10-04 C133)
      List<ModPair> extra) {}

  public record ModPair(List<String> text, List<String> textKo) {}

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
      // 주얼 반경(PoB "Radius: Small") — 트리 화면 꽂은 주얼의 연결 없이 찍기(From Nothing) 반경 · 엔진 아이템 원문(10-03 C74)
      String radius,
      // 기본 암시 · 옵션 줄의 강조 용어 정의(KeywordPopups, 인게임 Alt — 10-04 C112, 젬 Gem.keywords 와 같은 모양)
      List<Keyword> keywords) {

    /** 목록용 — 키워드 정의(고유 · 변형)를 뺀 사본. 툴팁은 상세 응답으로 그린다(10-04 C117). */
    public Unique withoutKeywords() {
      List<UniqueVariant> vs =
          variants == null
              ? null
              : variants.stream()
                  .map(
                      v ->
                          new UniqueVariant(
                              v.index(),
                              v.name(),
                              v.nameKo(),
                              v.implicits(),
                              v.implicitsKo(),
                              v.explicits(),
                              v.explicitsKo(),
                              null))
                  .toList();
      return new Unique(
          name,
          nameKo,
          slug,
          baseType,
          baseTypeKo,
          itemClass,
          itemClassKo,
          category,
          requiredLevel,
          implicits,
          implicitsKo,
          explicits,
          explicitsKo,
          vs,
          defaultVariant,
          flavour,
          flavourKo,
          league,
          image,
          radius,
          null);
    }
  }

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
      // 거래소 주소 경로의 리그(/trade2/search/poe2/<리그>) — 아이템 거래소 링크용(10-08). 사전에 없으면 null(게이트가 Standard)
      String tradeLeague) {}

  public record BuildStat(String key, Double value) {}

  // ── 업그레이드 가이드(PoB-PoE2 비교 계산기) — 증감은 모두 % (life 만 절대값) ──
  public record GuideBase(
      Double dps,
      Double ehp,
      Double life,
      Double es,
      Double maxHit,
      String skill,
      String skillKo) {}

  /** 칸을 비우면 — dps/ehp/maxHit 은 떨어지는 폭(음수). */
  public record GuideSlot(
      String slot,
      String item,
      String itemKo,
      String base,
      String rarity,
      Double dps,
      Double ehp,
      Double maxHit) {}

  /** 보조젬을 끄면 떨어지는 폭을 기여(양수)로 — applied=false 면 인게임에서 이 스킬에 적용되지 않는 보조. */
  public record GuideSupport(
      String name,
      String nameKo,
      String slug,
      Boolean applied,
      Double dps,
      Double ehp,
      // 계보(lineage) 젬 — 드물게 얻는다. 교체 추천에서 "쉽게 할 수 있는 교체"로 읽히지 않게 화면이 표시한다(2026-10-01)
      Boolean lineage) {}

  /** 특화·핵심 패시브를 빼면 떨어지는 폭을 기여(양수)로. */
  public record GuideNode(
      Integer id,
      String name,
      String nameKo,
      String type,
      String ascendancy,
      Double dps,
      Double ehp) {}

  /** 고유 아이템으로 바꾸면 오르는 폭. */
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
      // 거래소 검색 쿼리(고유 이름, 10-08 C172)
      String tradeQuery) {}

  /** 옵션 목표 한 줄 — 그 칸 아이템에 이 계열의 최고 등급(최대 롤)을 더하면. */
  public record GuideMod(
      String modType,
      String gen,
      String tierName,
      String tierNameKo,
      List<String> lines,
      List<String> linesKo,
      Double dps,
      Double ehp) {}

  /** 칸별 옵션 목표 — 희귀·마법·일반 장비에 붙일 수 있는 옵션 중 DPS·EHP 를 가장 올리는 것. */
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
      Integer mainSet,
      // 레어 목표 — 칸마다 같은 베이스의 좋은 레어(2티어 중간 롤 접두3·접미3)로 바꾸면(10-02 사용자 요청)
      List<GuideRareTarget> rareTargets) {}

  /** 레어 목표만(10-02) — 가이드 뒤에 따로 불러온다(본 가이드가 4~6초 느려지지 않게). */
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

  /** 고른 레어 — 옵션 줄(영문 PoB 모양 · 한국어 화면용)과 지금 아이템 대비 증감(%). */
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
      // 거래소 검색 쿼리(분류 + 옵션 85% 이상, 10-08 C172). 없으면 null
      String tradeQuery) {}

  /**
   * 엔진 재계산 한 줄 — PoB 가 저장해 둔 값(saved) · PoB-PoE2 엔진으로 다시 계산한 값(computed). set1·set2 = 무기 세트를 나눠 쓰는
   * 빌드일 때 세트별 재계산(아니면 null). computed 는 저장 당시 켜진 세트 값(저장값과 비교용).
   */
  public record BuildRecalcRow(
      String key, Double saved, Double computed, Double set1, Double set2) {}

  /**
   * 엔진 재계산 — available=false 면 이 서버에 엔진이 없다(pob2-src 미설치), error 는 계산 실패 사유. 경고 둘은 PoB 가 사이드바에만 띄우고
   * 수치엔 반영하지 않는 "인게임에선 안 되는" 상태: 적용되지 않는 보조젬 수, 주 스킬 비용 부족.
   */
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
      // mods 앞 N줄이 암시(PoB "Implicits: N") — 툴팁이 인게임처럼 구분선으로 가른다(10-04 C132)
      Integer implicitCount,
      // 옵션 줄의 강조 용어 정의(Alt 설명) — 고유도(10-04 C140)
      List<Keyword> keywords,
      // 고유 로어(플레이버) — 인게임 고유 툴팁 맨 아래(고유만, 10-04 C140)
      List<String> flavour,
      List<String> flavourKo,
      // 베이스(속성 칸 · 요구 사항 — PoE1 빌드 툴팁 짝, 10-04 C147). 키워드는 뺀 사본
      BaseItem base,
      // 품질 %(PoB "Quality: N") — 인게임 속성 칸 첫 줄. 0 이면 없음(10-04 C148)
      Integer quality,
      // 거래소 검색 쿼리(q JSON) — 고유는 이름, 레어 · 마법은 베이스 + 옵션(PoeTradeQueries, 10-08). 만들 수 없으면 null
      String tradeQuery) {}

  public record BuildNode(Integer id, String name, String nameKo) {}

  public record BuildTree(
      Integer allocated,
      Integer ascendancyPoints,
      List<BuildNode> keystones,
      List<BuildNode> notables,
      List<BuildNode> ascendancyNodes) {}

  // ─────────────────────────── 요약 ───────────────────────────

  public record Meta(
      String patch,
      int gems,
      int baseItems,
      int modPools,
      int augments,
      int uniques,
      boolean tree,
      String loadedAt,
      // 거래소 주소 경로의 리그(10-08 C172 — 가이드 조각이 거래소 링크에 쓴다). 사전에 없으면 null
      String tradeLeague) {}
}
