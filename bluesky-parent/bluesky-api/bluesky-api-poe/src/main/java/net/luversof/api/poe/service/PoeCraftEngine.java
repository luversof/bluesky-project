package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * PoE1 제작 엔진(10-15 C335 — 사용자 요청 "직접 아이템을 제작해 보는 에뮬레이터 · 가장 적은 비용을 계산하는 시뮬레이터"의 바탕). 상태 없는 순수 규칙:
 * 아이템 상태 + 화폐 → 새 상태. 난수는 밖에서 받아(시드 고정 시험 · 몬테카를로) 같은 규칙을 에뮬레이터와 계산기가 같이 쓴다.
 *
 * <p>규칙(게임 · craftofexile 과 같은 계열):
 *
 * <ul>
 *   <li>옵션 하나 = 그 풀에서 붙을 수 있는 옵션(아이템 레벨 이하 · 자리가 남은 접두/접미 · 이미 있는 옵션 그룹이 아님) 중 가중치에 비례해 뽑는다
 *   <li>자리: 매직 접두 1 · 접미 1, 레어 접두 3 · 접미 3(주얼 2 · 2)
 *   <li>옵션 수: 매직 1 또는 2(반반), 레어 4 · 5 · 6 = 8 : 3 : 1(주얼 3 · 4 = 반반) — 클라이언트 데이터에 없는 값이라 커뮤니티
 *       측정값(craftofexile 계산과 같은 가정)
 *   <li>분열(fractured) 옵션은 카오스 · 소멸 · 정제가 건드리지 않고 옵션 수에 센다
 * </ul>
 *
 * <p>여러 제작 콘텐츠를 섞는 규칙(10-15 C338 — 사용자 요청 "다양한 제작 컨텐츠들이 엮여 있고, 섞어서 사용이 가능하고 그에 따른 제약도 있다"):
 *
 * <ul>
 *   <li>메타 제작(제작대): 접두/접미 변경 불가 — 다시 굴리기(변경 · 카오스 · 에센스 · 화석 · 수확 재제작) · 소멸 · 정제가 잠긴 쪽을 남긴다(메타 옵션
 *       자신은 반대쪽이라 다시 굴리면 사라진다). 공격/주문 옵션 금지 — 그 옵션이 남아 있는 동안 새로 붙는 옵션에서 attack/caster 태그를 뺀다. 제작 옵션
 *       3개 — 제작대 옵션 한도 1 → 3
 *   <li>제작대: 자리 · 옵션 그룹 · 제작 옵션 한도. 일반 아이템에 하면 매직이 된다
 *   <li>화석(공명기 1~4칸, 같은 화석 중복 불가): 옵션 태그마다 배수를 곱한다(0 = 막음). 일반은 연금 공명기(→ 레어), 레어는 혼돈 공명기(다시)
 *   <li>수확: 태그 재제작(그 태그 옵션 하나 보장) · 같은 유형 ±(지금 옵션 태그와 겹치면 ×10 / ×0.1) · 저항 변환(같은 등급) · 태그 추가 + 무작위
 *       제거(영향력 없는 아이템) · 영향력 재제작(영향력 옵션 하나 보장)
 *   <li>정복자 엑잘: 영향력 없는 레어에 영향력 + 그 영향력 전용 옵션 하나. 분열 오브: 옵션 4개 이상 레어의 제작대가 아닌 옵션 하나를 고정
 * </ul>
 */
public final class PoeCraftEngine {

  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE
  }

  /**
   * 화폐 — arg: ESSENCE = 에센스 슬러그, FOSSIL = 화석 슬러그 쉼표 목록(1~4), BENCH = 제작대 옵션 id, HARVEST = 수확 id,
   * CONQUEROR = 영향력 키(crusader · eyrie · basilisk · adjudicator).
   */
  public enum Action {
    TRANSMUTE,
    ALTERATION,
    AUGMENTATION,
    REGAL,
    ALCHEMY,
    CHAOS,
    EXALT,
    ANNUL,
    SCOUR,
    ESSENCE,
    FOSSIL,
    BENCH,
    BENCH_REMOVE,
    HARVEST,
    CONQUEROR,
    FRACTURE,
    // 장막(10-15 C341) — 장막 카오스(장막 옵션 하나 보장) · 장막 엑잘(무작위 옵션 하나를 장막으로) · 해제(arg = 제시된 셋 중 고른 옵션 id)
    VEILED_CHAOS,
    VEILED_EXALT,
    UNVEIL,
    // 엘드리치(10-15 C343) — 불씨(작열의 총주교) · 영액(세계 포식자) 임플리싯(arg = 화폐 등급 1~4), 섬뜩한 카오스 · 엑잘 · 소멸(우세한 쪽
    // 접두/접미만)
    EMBER,
    ICHOR,
    ELDRITCH_CHAOS,
    ELDRITCH_EXALT,
    ELDRITCH_ANNUL,
    // 지배의 오브(10-15 C345) — 영향력 옵션 둘 이상: 하나는 한 등급 위(최고면 고양 옵션), 다른 하나는 지움
    DOMINANCE,
    // 삿된 확장 · 제왕 · 엑잘티드(10-17 C401) — 보통 화폐와 같되 "레벨이 높은 속성이 붙을 가능성이 더 높음"(가중 = 축성된 화석과 같다고 가정)
    FOULBORN_AUG,
    FOULBORN_REGAL,
    FOULBORN_EXALT,
    // 갈등의 오브(10-17 C398) — 총주교 · 포식자 고정 속성 둘 다 있을 때, 하나 올리고 하나 내림(관련 강도에 따라)
    CONFLICT,
    // 각성자의 오브(C346) — arg = 없앨 아이템의 영향력[:그 아이템의 영향력 옵션 id]. 두 영향력 레어로 새로 굴리며 양쪽 영향력 옵션 하나씩 남김
    AWAKENER,
    // 기폭제(10-16 C385, 반지 · 목걸이 · 허리띠 퀄리티 — 인게임 설명 "기존 퀄리티 유형을 대체합니다"): arg = 종류 키[*N = N번 연달아],
    // vaal = 더럽혀진 기폭제(타락한 장신구 전용, 무작위 종류 · 양)
    CATALYST,
    // 시작 아이템 직접 편집(10-15 C348 — craftofexile 처럼 분열 베이스 등에서 출발). 화폐가 아니다(비용 없음).
    // MANUAL_ADD arg = 옵션 id[:F](F = 분열로), MANUAL_REMOVE arg = 옵션 id
    // 짐승 제작(10-17 C387, BestiaryRecipes — 아인하르): arg = swap-suffix(접미 추가 + 무작위 접두 제거) |
    // swap-prefix(반대) |
    // influence:영향력(그 영향력 아이템에 그 영향력 옵션 하나)
    BEAST,
    // 두카트(10-17 C391, 군왕호 올플레임 제작): arg = genteel(능력치 하나 → 다른 능력치) | rotmother(희귀 허리띠에 덫 · 지뢰 옵션) |
    // brinehook(무작위 판테온 위상 스킬)
    DUCAT,
    MANUAL_ADD,
    MANUAL_REMOVE,
    // 바알 오브(10-16 C358) — 타락시키며 넷 중 하나(poedb "Effects of Vaal Orbs", 가중치 비공개 → 균등): 변화 없음 · 소켓
    // 다시(옵션은 그대로) ·
    // 레어로 다시 굴림(분열 · 메타 잠금 쪽은 남김) · 임플리싯을 부패 임플리싯 하나로. 플라스크엔 못 쓴다
    VAAL,
    // 더럽혀진 화폐(10-16 C359 — 인게임 설명, 타락한 희귀 아이템 전용 · 둘 중 하나는 확률 비공개 → 균등):
    // 카오스 = 새 옵션으로 다시 굴림 | 옵션을 전부 지움, 엑잘티드 = 옵션 하나 붙임 | 지움, 신성한 눈물방울 = 옵션마다 등급 하나 올림 | 내림
    TAINTED_CHAOS,
    TAINTED_EXALT,
    TAINTED_DIVINE
  }

  /** 영향력 키 — 쉐이퍼 · 엘더 · 정복자 넷. 영향력 둘이면 "a,b"(가나다순 아님, 키 정렬). */
  static final Set<String> INFLUENCES =
      Set.of("shaper", "elder", "crusader", "eyrie", "basilisk", "adjudicator");

  /** 엘드리치 진영 — 작열의 총주교(접두 쪽) · 세계 포식자(접미 쪽). 임플리싯 Affix 의 gen 에 쓴다. */
  public static final String EXARCH = "exarch";

  public static final String EATER = "eater";

  /** 바알 오브 부패 임플리싯(C358) — 임플리싯 Affix 의 gen. */
  public static final String CORRUPTED = "corrupted";

  /** 엘드리치 부위 — 불씨 · 영액 인게임 설명(poedb): 갑옷 · 장화 · 장갑 · 투구. */
  static final Set<String> ELDRITCH_CLASSES = Set.of("Body Armour", "Boots", "Gloves", "Helmet");

  /** 장막 옵션(해제 전) 자리 표시 — 그 쪽 자리를 차지한다. 해제하면 고른 해제 옵션으로 바뀐다. */
  public static final String VEILED_PREFIX = "VeiledPrefix";

  public static final String VEILED_SUFFIX = "VeiledSuffix";

  static boolean veiled(Affix a) {
    return VEILED_PREFIX.equals(a.id()) || VEILED_SUFFIX.equals(a.id());
  }

  static Affix veil(String gen) {
    return "prefix".equals(gen)
        ? new Affix(VEILED_PREFIX, "prefix", List.of(-1), false, false)
        : new Affix(VEILED_SUFFIX, "suffix", List.of(-2), false, false);
  }

  /** 옵션 하나 — crafted = 제작대, fractured = 분열(고정). */
  public record Affix(
      String id, String gen, List<Integer> groups, boolean crafted, boolean fractured) {}

  /** 아이템 상태 — 풀 키 = 클래스|변형|영향력. */
  public record Item(
      String itemClass,
      String variant,
      String influence,
      int ilvl,
      Rarity rarity,
      List<Affix> affixes,
      boolean corrupted,
      // 임플리싯(10-15 C343) — 지금은 엘드리치(gen = exarch|eater)만. 다시 굴리기 · 정제가 건드리지 않는다
      List<Affix> implicits,
      // 기폭제 퀄리티(C385) — "종류:양"(예: attack:20), 없으면 null. 옵션을 다시 굴려도 남는다(퀄리티는 옵션이 아니다)
      String quality,
      // 장막 해제 씨앗(C483) — 해제 제시 셋을 섞는 값, 없으면 아이템 상태만으로. 에뮬레이터가 되돌릴 때 새로 줘서 다른 셋이 나온다
      Long veilSeed) {

    public Item {
      implicits = implicits == null ? List.of() : List.copyOf(implicits);
    }

    public Item(
        String itemClass,
        String variant,
        String influence,
        int ilvl,
        Rarity rarity,
        List<Affix> affixes,
        boolean corrupted,
        List<Affix> implicits,
        String quality) {
      this(
          itemClass, variant, influence, ilvl, rarity, affixes, corrupted, implicits, quality,
          null);
    }

    public Item(
        String itemClass,
        String variant,
        String influence,
        int ilvl,
        Rarity rarity,
        List<Affix> affixes,
        boolean corrupted,
        List<Affix> implicits) {
      this(itemClass, variant, influence, ilvl, rarity, affixes, corrupted, implicits, null);
    }

    /** 기폭제 종류 키, 없으면 null. */
    public String qualityKey() {
      int i = quality == null ? -1 : quality.indexOf(':');
      return i > 0 ? quality.substring(0, i) : null;
    }

    /** 기폭제 퀄리티 양(%), 없으면 0. */
    public int qualityAmount() {
      int i = quality == null ? -1 : quality.indexOf(':');
      if (i < 0) {
        return 0;
      }
      try {
        return Integer.parseInt(quality.substring(i + 1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }

    public Item(
        String itemClass,
        String variant,
        String influence,
        int ilvl,
        Rarity rarity,
        List<Affix> affixes,
        boolean corrupted) {
      this(itemClass, variant, influence, ilvl, rarity, affixes, corrupted, List.of());
    }

    public String poolKey() {
      return itemClass
          + "|"
          + (variant == null ? "" : variant)
          + "|"
          + (influence == null ? "" : influence);
    }

    public long count(String gen) {
      return affixes.stream().filter(a -> gen.equals(a.gen())).count();
    }

    Item with(Rarity r, List<Affix> a) {
      return new Item(
          itemClass,
          variant,
          influence,
          ilvl,
          r,
          List.copyOf(a),
          corrupted,
          implicits,
          quality,
          veilSeed);
    }

    Item withInfluence(String inf) {
      return new Item(
          itemClass, variant, inf, ilvl, rarity, affixes, corrupted, implicits, quality, veilSeed);
    }

    Item withImplicits(List<Affix> imp) {
      return new Item(
          itemClass, variant, influence, ilvl, rarity, affixes, corrupted, imp, quality, veilSeed);
    }

    Item withQuality(String q) {
      return new Item(
          itemClass, variant, influence, ilvl, rarity, affixes, corrupted, implicits, q, veilSeed);
    }

    Item corrupt() {
      return new Item(
          itemClass, variant, influence, ilvl, rarity, affixes, true, implicits, quality, veilSeed);
    }
  }

  /**
   * 결과 — error 가 있으면 item 은 그대로(적용 안 됨). added/removed = 옵션 id. note = 결과 설명(바알 오브 결과 종류, C358).
   */
  public record Result(
      Item item, String error, List<String> added, List<String> removed, String note) {

    Result(Item item, String error, List<String> added, List<String> removed) {
      this(item, error, added, removed, null);
    }

    static Result fail(Item item, String error) {
      return new Result(item, error, List.of(), List.of());
    }
  }

  /** 엔진이 보는 데이터 — 시험에선 작은 가짜 풀을 넣는다. 화석 · 제작대 · 수확은 없으면 null. */
  public interface Data {

    Map<String, Integer> pool(String poolKey);

    PoeCraftDataService.CraftMod mod(String id);

    PoeCraftDataService.Essence essence(String itemClass, String slug);

    default PoeCraftDataService.Fossil fossil(String slug) {
      return null;
    }

    default PoeCraftDataService.BenchCraft bench(String itemClass, String modId) {
      return null;
    }

    default PoeCraftDataService.Harvest harvest(String id) {
      return null;
    }

    default Map<String, Integer> veiledPool(String poolKey) {
      return Map.of();
    }

    default PoeCraftDataService.VeiledMod veiledMod(String id) {
      return null;
    }

    /** 화석 추가 · 강제 옵션 풀(C342) — 옵션 id → 가중치. */
    default Map<String, Integer> fossilPool(String poolKey) {
      return Map.of();
    }

    default PoeCraftDataService.VeiledMod fossilMod(String id) {
      return null;
    }

    /** 지배의 오브(C345) — 그 풀에서 영향력 옵션 id → 한 등급 위 id(최고 등급이면 고양 옵션 id). */
    default Map<String, String> upgrade(String poolKey) {
      return Map.of();
    }

    /** 고양 옵션(…Maven) — 풀엔 없다(가중치 0). */
    default PoeCraftDataService.VeiledMod elevatedMod(String id) {
      return null;
    }

    /** 엘드리치 임플리싯 풀(C343) — 옵션 id → 가중치. */
    default Map<String, Integer> eldritchPool(String poolKey) {
      return Map.of();
    }

    default PoeCraftDataService.EldritchMod eldritchMod(String id) {
      return null;
    }

    /** 바알 오브 부패 임플리싯 풀(C358) — 옵션 id → 가중치. */
    default Map<String, Integer> corruptedPool(String poolKey) {
      return Map.of();
    }

    /** 짐승 제작 위상 스킬 옵션(C388). */
    default PoeCraftDataService.VeiledMod beastMod(String id) {
      return null;
    }

    /** 부패모의 두카트 옵션(C391). */
    default Map<String, PoeCraftDataService.HazardMod> hazardMods() {
      return Map.of();
    }

    /** 사이악산의 두카트 악명 높은 속성(C393). */
    default Map<String, PoeCraftDataService.InfamousMod> infamousMods() {
      return Map.of();
    }

    default PoeCraftDataService.VeiledMod corruptedMod(String id) {
      return null;
    }

    /** 이 아이템 종류의 부패 에센스(섬망 · 경악 · 발작 · 광기) 옵션 — 상형 문자 화석(C342). */
    default List<PoeCraftDataService.Essence> corruptEssences(String itemClass) {
      return List.of();
    }
  }

  /** 메타 제작 — 아이템 옵션 중 제작대 메타 옵션(id 로 판별). */
  public record Meta(
      boolean prefixLocked,
      boolean suffixLocked,
      boolean multimod,
      boolean noAttack,
      boolean noCaster) {

    static final Meta NONE = new Meta(false, false, false, false, false);

    public boolean any() {
      return prefixLocked || suffixLocked || multimod || noAttack || noCaster;
    }
  }

  public static Meta meta(List<Affix> affixes) {
    boolean pl = false, sl = false, mm = false, na = false, nc = false;
    for (Affix a : affixes) {
      String id = a.id();
      if (id == null || !a.crafted()) {
        continue;
      }
      pl |= id.contains("CannotChangePrefixes");
      sl |= id.contains("CannotChangeSuffixes");
      mm |= id.contains("CanHaveMultipleCraftedMods");
      na |= id.contains("CannotRollAttackAffixes");
      nc |= id.contains("CannotRollCasterAffixes");
    }
    return new Meta(pl, sl, mm, na, nc);
  }

  /**
   * 결과에 영향을 주는 메타 옵션(잠금 · 공격/주문 금지)이 있는가 — 화석 · 에센스는 이런 아이템에 못 쓴다(C430, CoE 'Prevents usage', 인게임
   * ItemErrorItemHasCraftingMetaMod).
   */
  static boolean hasOutcomeMeta(Item item) {
    Meta m = meta(item.affixes());
    return m.prefixLocked() || m.suffixLocked() || m.noAttack() || m.noCaster();
  }

  /** 인게임 문구(ClientStrings ItemErrorItemHasCraftingMetaMod). */
  static final String META_MOD_ERROR = "속성의 결과에 영향을 미치는 속성 부여가 포함된 아이템에는 해당 화폐를 사용할 수 없습니다.";

  /** 인게임 문구(ClientStrings ItemErrorItemIsFractured). */
  static final String FRACTURED_ERROR = "아이템에 분열된 속성 부여가 적용되어 있습니다.";

  /** 다시 굴려도 남는 옵션 — 분열 + 잠긴 쪽(메타 제작). */
  static List<Affix> kept(Item item) {
    Meta m = meta(item.affixes());
    List<Affix> out = new ArrayList<>();
    for (Affix a : item.affixes()) {
      boolean pre = "prefix".equals(a.gen());
      if (a.fractured() || (m.prefixLocked() && pre) || (m.suffixLocked() && !pre)) {
        out.add(a);
      }
    }
    return out;
  }

  /** 주얼은 자리가 2 · 2(심연 주얼 포함). */
  static boolean jewel(String itemClass) {
    return itemClass != null && itemClass.contains("Jewel");
  }

  static int maxPerGen(Item item, Rarity rarity) {
    if (rarity == Rarity.MAGIC) {
      return 1;
    }
    return jewel(item.itemClass()) ? 2 : 3;
  }

  /** 정복자 영향력 키 — 크루세이더 · 구원자(eyrie) · 사냥꾼(basilisk) · 전쟁군주(adjudicator). */
  static final Set<String> CONQUERORS = Set.of("crusader", "eyrie", "basilisk", "adjudicator");

  /** 영향력 엑잘티드 오브(C391 — 정복자 넷 + 셰이퍼 · 엘더, CurrencyItems add_influence_mod_to_rare 여섯). */
  static final Set<String> INFLUENCE_EXALTS =
      Set.of("crusader", "eyrie", "basilisk", "adjudicator", "shaper", "elder");

  private final Data data;

  public PoeCraftEngine(Data data) {
    this.data = data;
  }

  public Result apply(Item item, Action action, String arg, RandomGenerator rng) {
    if (action == Action.CATALYST) {
      return catalyst(item, arg, rng);
    }
    boolean tainted =
        action == Action.TAINTED_CHAOS
            || action == Action.TAINTED_EXALT
            || action == Action.TAINTED_DIVINE;
    if (tainted) {
      // 인게임 설명 "타락한 희귀 아이템을 좌클릭해 적용"
      return !item.corrupted() || item.rarity() != Rarity.RARE
          ? Result.fail(item, "타락한 희귀 아이템에만 쓸 수 있습니다")
          : tainted(item, action, kept(item), rng);
    }
    // 직접 편집(화폐 아님, C348)은 타락한 아이템에도(C369 검토 — 타락한 시작 아이템을 손볼 수 없었다)
    if (item.corrupted() && action != Action.MANUAL_ADD && action != Action.MANUAL_REMOVE) {
      // 인게임 바알 오브 설명 문장(CurrencyItems.Directions, C358)
      return Result.fail(item, "타락한 아이템은 다시 속성 부여 개조할 수 없습니다");
    }
    List<Affix> keep = kept(item);
    return switch (action) {
      case TRANSMUTE ->
          item.rarity() != Rarity.NORMAL
              ? Result.fail(item, "일반 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.MAGIC, List.of(), magicCount(rng), Weights.NONE, null, rng);
      case ALTERATION ->
          item.rarity() != Rarity.MAGIC
              ? Result.fail(item, "매직 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.MAGIC, keep, magicCount(rng), Weights.NONE, null, rng);
      case AUGMENTATION -> {
        if (item.rarity() != Rarity.MAGIC) {
          yield Result.fail(item, "매직 아이템에만 쓸 수 있습니다");
        }
        if (item.affixes().size() >= 2) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.MAGIC, Weights.NONE, rng);
      }
      case REGAL ->
          item.rarity() != Rarity.MAGIC
              ? Result.fail(item, "매직 아이템에만 쓸 수 있습니다")
              : add(item, Rarity.RARE, Weights.NONE, rng);
      case ALCHEMY ->
          item.rarity() != Rarity.NORMAL
              ? Result.fail(item, "일반 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.RARE, List.of(), rareCount(item, rng), Weights.NONE, null, rng);
      case CHAOS ->
          item.rarity() != Rarity.RARE
              ? Result.fail(item, "레어 아이템에만 쓸 수 있습니다")
              : roll(item, Rarity.RARE, keep, rareCount(item, rng), Weights.NONE, null, rng);
      case EXALT -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        if (full(item, Rarity.RARE)) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.RARE, Weights.NONE, rng);
      }
      case ANNUL -> annul(item, rng);
      case SCOUR -> {
        if (item.rarity() == Rarity.NORMAL) {
          yield Result.fail(item, "이미 일반 아이템입니다");
        }
        List<String> removed =
            item.affixes().stream().filter(a -> !keep.contains(a)).map(Affix::id).toList();
        // 분열 옵션 하나만 남으면 매직(C430 — poedb Fractured_item 'turn into a Magic item if it had one
        // fractured mod')
        boolean oneFractured = keep.size() == 1 && keep.get(0).fractured();
        yield new Result(
            item.with(
                keep.isEmpty() ? Rarity.NORMAL : oneFractured ? Rarity.MAGIC : item.rarity(), keep),
            null,
            List.of(),
            removed);
      }
      case ESSENCE -> essence(item, arg, keep, rng);
      case FOSSIL -> fossil(item, arg, keep, rng);
      case BENCH -> bench(item, arg);
      case BENCH_REMOVE -> {
        List<Affix> crafted = item.affixes().stream().filter(Affix::crafted).toList();
        if (crafted.isEmpty()) {
          yield Result.fail(item, "제작대 옵션이 없습니다");
        }
        List<Affix> out = new ArrayList<>(item.affixes());
        out.removeAll(crafted);
        yield new Result(
            item.with(item.rarity(), out),
            null,
            List.of(),
            crafted.stream().map(Affix::id).toList());
      }
      case HARVEST -> harvest(item, arg, keep, rng);
      case CONQUEROR -> conqueror(item, arg, rng);
      case BEAST -> beast(item, arg, rng);
      case DUCAT -> ducat(item, arg, rng);
      case FRACTURE -> fracture(item, rng);
      case VEILED_CHAOS -> veiledChaos(item, keep, rng);
      case VEILED_EXALT -> veiledExalt(item, rng);
      case UNVEIL -> unveil(item, arg);
      case EMBER -> eldritchImplicit(item, EXARCH, arg, rng);
      case ICHOR -> eldritchImplicit(item, EATER, arg, rng);
      case ELDRITCH_CHAOS, ELDRITCH_EXALT, ELDRITCH_ANNUL -> eldritch(item, action, keep, rng);
      case DOMINANCE -> dominanceOrb(item, keep, rng);
      case CONFLICT -> conflict(item, rng);
      case FOULBORN_AUG -> {
        if (item.rarity() != Rarity.MAGIC) {
          yield Result.fail(item, "매직 아이템에만 쓸 수 있습니다");
        }
        if (item.affixes().size() >= 2) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.MAGIC, FOULBORN, rng);
      }
      case FOULBORN_REGAL ->
          item.rarity() != Rarity.MAGIC
              ? Result.fail(item, "매직 아이템에만 쓸 수 있습니다")
              : add(item, Rarity.RARE, FOULBORN, rng);
      case FOULBORN_EXALT -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        if (full(item, Rarity.RARE)) {
          yield Result.fail(item, "빈 자리가 없습니다");
        }
        yield add(item, Rarity.RARE, FOULBORN, rng);
      }
      case AWAKENER -> awakener(item, arg, rng);
      case MANUAL_ADD -> manualAdd(item, arg);
      case VAAL -> vaal(item, keep, rng);
      case CATALYST -> catalyst(item, arg, rng); // 위에서 먼저 처리
      case TAINTED_CHAOS, TAINTED_EXALT, TAINTED_DIVINE ->
          tainted(item, action, keep, rng); // 위에서 먼저 처리
      case MANUAL_REMOVE -> {
        Affix gone =
            item.affixes().stream().filter(a -> a.id().equals(arg)).findFirst().orElse(null);
        if (gone == null) {
          yield Result.fail(item, "그 옵션이 없습니다");
        }
        List<Affix> out = new ArrayList<>(item.affixes());
        out.remove(gone);
        yield new Result(
            item.with(out.isEmpty() ? Rarity.NORMAL : item.rarity(), out),
            null,
            List.of(),
            List.of(gone.id()));
      }
    };
  }

  private static boolean full(Item item, Rarity rarity) {
    int max = maxPerGen(item, rarity);
    return item.count("prefix") >= max && item.count("suffix") >= max;
  }

  /** 매직 옵션 수 — 1 · 2 반반. */
  static int magicCount(RandomGenerator rng) {
    return rng.nextInt(2) + 1;
  }

  /** 레어 옵션 수 — 4 · 5 · 6 = 8 : 3 : 1, 주얼 3 · 4 반반. */
  static int rareCount(Item item, RandomGenerator rng) {
    if (jewel(item.itemClass())) {
      return 3 + rng.nextInt(2);
    }
    int r = rng.nextInt(12);
    return r < 8 ? 4 : r < 11 ? 5 : 6;
  }

  /**
   * 가중치 보정 — tagMult = 옵션 태그 → 배수(곱한다, 화석 · 같은 유형 ±), onlyIds = 이 옵션들만(영향력 전용 등). 메타 "공격/주문 옵션 금지"는
   * 굴릴 때 남은 옵션에서 따로 읽는다.
   */
  record Weights(
      Map<String, Double> tagMult,
      Set<String> onlyIds,
      Set<String> requireTags,
      boolean closePrefix,
      boolean closeSuffix,
      // 풀 밖에서 더 들어오는 옵션 id → 가중치(화석 추가 옵션, C342). 데이터는 fossilMod
      Map<String, Integer> extra) {
    static final Weights NONE = new Weights(Map.of(), null, null, false, false, null);

    Weights(Map<String, Double> tagMult, Set<String> onlyIds) {
      this(tagMult, onlyIds, null, false, false, null);
    }

    Weights(Map<String, Double> tagMult, Set<String> onlyIds, Set<String> requireTags) {
      this(tagMult, onlyIds, requireTags, false, false, null);
    }

    /** 보장 조건 — 이 태그를 하나라도 가진 옵션만. */
    Weights withRequireTags(Set<String> tags) {
      return new Weights(tagMult, onlyIds, tags, closePrefix, closeSuffix, extra);
    }

    /** 접두/접미 쪽을 닫는다(메타 제작 잠금 — 다시 굴릴 때). */
    Weights closing(boolean pre, boolean suf) {
      return new Weights(
          tagMult, onlyIds, requireTags, closePrefix || pre, closeSuffix || suf, extra);
    }
  }

  /** 남길 옵션(keep)에서 시작해 옵션 수 target 까지 채운다. requireTag/requireIds 가 있으면 첫 옵션은 그 조건에서(보장). */
  private Result roll(
      Item item,
      Rarity rarity,
      List<Affix> keep,
      int target,
      Weights w,
      Weights require,
      RandomGenerator rng) {
    List<Affix> out = new ArrayList<>(keep);
    List<String> added = new ArrayList<>();
    // 메타 잠금(C430 — 공식 포럼 3312058 '장막 카오스 + 접두 잠금 → 접두가 하나 더 붙음' · 'Not a bug'): 잠긴 쪽은 기존 옵션만
    // 지키고(keep) 빈자리엔
    // 새 옵션이 붙는다. 10-15 에 잠긴 쪽을 통째로 닫은 것은 엔진 결과를 보고 한 판단이었다
    if (require != null) {
      Affix a = pick(item.with(rarity, out), rarity, merge(w, require), rng);
      if (a == null) {
        return Result.fail(item, "보장할 옵션이 붙을 자리가 없습니다");
      }
      out.add(a);
      added.add(a.id());
    }
    while (out.size() < target) {
      Affix a = pick(item.with(rarity, out), rarity, w, rng);
      if (a == null) {
        break;
      }
      out.add(a);
      added.add(a.id());
    }
    List<String> removed =
        item.affixes().stream().filter(a -> !keep.contains(a)).map(Affix::id).toList();
    return new Result(item.with(rarity, out), null, added, removed);
  }

  /** 보장 조건(require)을 보정에 얹는다 — require.tagMult 의 태그를 하나라도 가진 옵션만, onlyIds 면 그 옵션만. */
  private static Weights merge(Weights w, Weights require) {
    return new Weights(
        w.tagMult(),
        require.onlyIds(),
        require.tagMult().isEmpty() ? null : require.tagMult().keySet(),
        w.closePrefix() || require.closePrefix(),
        w.closeSuffix() || require.closeSuffix(),
        w.extra());
  }

  private Result add(Item item, Rarity rarity, Weights w, RandomGenerator rng) {
    Affix a = pick(item, rarity, w, rng);
    if (a == null) {
      return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(a);
    return new Result(item.with(rarity, out), null, List.of(a.id()), List.of());
  }

  private Result annul(Item item, RandomGenerator rng) {
    if (item.rarity() == Rarity.NORMAL) {
      return Result.fail(item, "매직 · 레어 아이템에만 쓸 수 있습니다");
    }
    List<Affix> keep = kept(item);
    // 공격/주문 금지 메타 = 그 옵션은 지우지 않는다(C430 — CoE 'Protects attack mods from removal')
    Meta am = meta(item.affixes());
    List<Affix> removable =
        item.affixes().stream()
            .filter(a -> !keep.contains(a))
            .filter(
                a ->
                    !(am.noAttack() && affixTags(item, a).contains("attack"))
                        && !(am.noCaster() && affixTags(item, a).contains("caster")))
            .toList();
    if (removable.isEmpty()) {
      return Result.fail(item, "지울 수 있는 옵션이 없습니다(분열 · 잠긴 쪽 · 금지 메타)");
    }
    Affix gone = removable.get(rng.nextInt(removable.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    out.remove(gone);
    return new Result(item.with(item.rarity(), out), null, List.of(), List.of(gone.id()));
  }

  private Result essence(Item item, String slug, List<Affix> keep, RandomGenerator rng) {
    PoeCraftDataService.Essence e = data.essence(item.itemClass(), slug);
    if (e == null || e.modId() == null) {
      return Result.fail(item, "이 아이템 종류에 쓸 수 없는 에센스입니다");
    }
    if (hasOutcomeMeta(item)) {
      return Result.fail(item, META_MOD_ERROR);
    }
    if (item.rarity() == Rarity.MAGIC) {
      return Result.fail(item, "일반 · 레어 아이템에만 쓸 수 있습니다");
    }
    // 속삭이는 ~ 울부짖는(등급 1~4)은 일반 아이템에만(C460 — CurrencyItems Directions "left click a normal item",
    // 비명 등급부터 · 타락 · 황폐는 "a normal or rare item")
    if (item.rarity() == Rarity.RARE && e.tier() >= 1 && e.tier() <= 4) {
      return Result.fail(item, "일반 아이템에만 쓸 수 있는 에센스입니다(레어 재련은 비명 등급부터)");
    }
    if (e.ilvlMax() > 0 && item.ilvl() > e.ilvlMax()) {
      return Result.fail(item, "아이템 레벨 " + e.ilvlMax() + " 이하에만 쓸 수 있는 에센스입니다");
    }
    // 레어에 쓰면 카오스처럼 다시 굴리되 그 옵션을 보장(일반이면 레어로)
    Affix forced =
        new Affix(e.modId(), e.gen(), e.groups() == null ? List.of() : e.groups(), false, false);
    Meta m = meta(item.affixes());
    if (("prefix".equals(forced.gen()) && m.prefixLocked())
        || ("suffix".equals(forced.gen()) && m.suffixLocked())) {
      return Result.fail(item, "보장 옵션 쪽이 잠겨 있습니다(메타 제작)");
    }
    if (keep.stream().anyMatch(a -> sharesGroup(a, forced))) {
      return Result.fail(item, "남는 옵션과 같은 그룹이라 쓸 수 없습니다");
    }
    if (keep.stream().filter(a -> a.gen().equals(forced.gen())).count()
        >= maxPerGen(item, Rarity.RARE)) {
      return Result.fail(item, "보장 옵션이 들어갈 자리가 없습니다");
    }
    List<Affix> start = new ArrayList<>(keep);
    start.add(forced);
    Result r =
        roll(
            item,
            Rarity.RARE,
            start,
            Math.max(rareCount(item, rng), start.size()),
            Weights.NONE,
            null,
            rng);
    List<String> added = new ArrayList<>();
    added.add(forced.id());
    added.addAll(r.added());
    List<String> removed =
        item.affixes().stream().filter(a -> !keep.contains(a)).map(Affix::id).toList();
    return new Result(r.item(), null, added, removed);
  }

  /** 화석 — 공명기 칸 1~4, 같은 화석 중복 불가, 태그 배수를 곱한다. 일반 = 연금 공명기(→ 레어), 레어 = 혼돈 공명기. */
  private Result fossil(Item item, String slugs, List<Affix> keep, RandomGenerator rng) {
    if (hasOutcomeMeta(item)) {
      return Result.fail(item, META_MOD_ERROR);
    }
    if (item.rarity() == Rarity.MAGIC) {
      return Result.fail(item, "일반(연금 공명기) · 레어(혼돈 공명기)에만 쓸 수 있습니다");
    }
    List<String> list =
        slugs == null
            ? List.of()
            : java.util.Arrays.stream(slugs.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    if (list.isEmpty() || list.size() > 4) {
      return Result.fail(item, "화석은 1~4개(공명기 칸 수)");
    }
    if (new HashSet<>(list).size() != list.size()) {
      return Result.fail(item, "같은 화석은 한 공명기에 하나만 넣을 수 있습니다");
    }
    Map<String, Double> mult = new java.util.HashMap<>();
    Map<String, Integer> extra = new java.util.LinkedHashMap<>();
    List<Affix> forced = new ArrayList<>();
    boolean corruptEssence = false;
    boolean corrupt = false;
    boolean fracture = false;
    Map<String, Integer> fpool = data.fossilPool(item.poolKey());
    String tangled = null;
    boolean partialEssence = false;
    for (String s : list) {
      PoeCraftDataService.Fossil f = data.fossil(s);
      if (f == null) {
        return Result.fail(item, "모르는 화석: " + s);
      }
      // supported() 는 C400 부터 늘 참(축성된 · 얽혀든까지 계산) — 데이터에 새 특수 화석이 생길 때를 위한 자리
      if (!f.supported()) {
        return Result.fail(item, f.nameKo() + " 은(는) 아직 계산하지 않는 화석입니다(행운 굴림 · 무작위 효과)");
      }
      for (Map.Entry<String, Double> e : f.weights().entrySet()) {
        mult.merge(e.getKey(), e.getValue(), (a, b) -> a * b);
      }
      // 추가 옵션(연마한 화석 젬 레벨) — 이 아이템 종류에 붙는 것만 풀에 더한다
      for (String id : f.added() == null ? List.<String>of() : f.added()) {
        Integer wgt = fpool.get(id);
        if (wgt != null && wgt > 0) {
          extra.put(id, wgt);
        }
      }
      // 강제 옵션(공허의 화석 심연 홈) — 이 아이템 종류에 붙지 않으면 쓸 수 없다
      for (String id : f.forced() == null ? List.<String>of() : f.forced()) {
        PoeCraftDataService.VeiledMod m = data.fossilMod(id);
        if (m == null || !fpool.containsKey(id)) {
          return Result.fail(item, f.nameKo() + " 은(는) 이 아이템 종류에 쓸 수 없습니다");
        }
        forced.add(new Affix(id, m.g(), m.grp() == null ? List.of() : m.grp(), false, false));
      }
      corruptEssence |= f.corruptedEssenceChance() >= 100;
      // 얽혀든 10%(C400) — 일부 확률로 붙는 것은 못 붙으면 조용히 건너뛴다(C402 검토 — 주얼 · 화살통 등에서 무작위로 제작이 실패했다)
      partialEssence |=
          f.corruptedEssenceChance() > 0
              && f.corruptedEssenceChance() < 100
              && rng.nextInt(100) < f.corruptedEssenceChance();
      // 얽혀든 화석(C400) — 무작위 태그 하나 ×10 · 다른 하나 ×0(가정: 단순 화석이 쓰는 태그 중에서, '더' = 다른 화석과 같은 10)
      if ("CurrencyDelveCraftingRandom".equals(f.slug())) {
        List<String> tags = new ArrayList<>(TANGLED_TAGS);
        String more = tags.remove(rng.nextInt(tags.size()));
        String no = tags.get(rng.nextInt(tags.size()));
        mult.merge(more, 10d, (x, y) -> x * y);
        mult.put(no, 0d);
        tangled = "tangled:" + more + ":" + no;
      }
      // 효과가 데이터 필드에 없는 화석 — 슬러그로(피얼룩 = 부패 임플리싯, 분열된 = 옵션 하나 분열, 도금된 = 상점가만)
      corrupt |= "CurrencyDelveCraftingVaal".equals(f.slug());
      // 축성된 화석(C399) — 고레벨 옵션 가중(요구 레벨 40 기준 레벨당 ±1%, poedb 커뮤니티 위키). 수치 행운은 게이트 굴림
      if (f.lucky()) {
        mult.put(LEVEL_BIAS, 1d);
      }
      fracture |= "CurrencyDelveCraftingMirror".equals(f.slug());
    }
    if (fracture
        && (item.affixes().stream().anyMatch(Affix::fractured)
            || (item.influence() != null && !item.influence().isEmpty()))) {
      return Result.fail(item, "분열된 화석은 영향력 · 분열 아이템에 쓸 수 없습니다");
    }
    if (corruptEssence) {
      List<PoeCraftDataService.Essence> ce = data.corruptEssences(item.itemClass());
      if (ce.isEmpty()) {
        return Result.fail(item, "이 아이템 종류엔 부패 에센스 옵션이 없습니다");
      }
      PoeCraftDataService.Essence e = ce.get(rng.nextInt(ce.size()));
      forced.add(
          new Affix(e.modId(), e.gen(), e.groups() == null ? List.of() : e.groups(), false, false));
    }
    List<Affix> start = new ArrayList<>(item.rarity() == Rarity.NORMAL ? List.of() : keep);
    Meta fm = meta(item.affixes());
    if (partialEssence && !corruptEssence) {
      List<PoeCraftDataService.Essence> ce = data.corruptEssences(item.itemClass());
      if (!ce.isEmpty()) {
        PoeCraftDataService.Essence e = ce.get(rng.nextInt(ce.size()));
        Affix pa =
            new Affix(
                e.modId(), e.gen(), e.groups() == null ? List.of() : e.groups(), false, false);
        boolean locked =
            ("prefix".equals(pa.gen()) && fm.prefixLocked())
                || ("suffix".equals(pa.gen()) && fm.suffixLocked());
        boolean noRoom =
            start.stream().anyMatch(x -> sharesGroup(x, pa))
                || forced.stream().anyMatch(x -> sharesGroup(x, pa))
                || start.stream().filter(x -> x.gen().equals(pa.gen())).count()
                        + forced.stream().filter(x -> x.gen().equals(pa.gen())).count()
                    >= maxPerGen(item, Rarity.RARE);
        if (!locked && !noRoom) {
          forced.add(pa);
        }
      }
    }
    for (Affix a : forced) {
      // 강제 옵션 쪽이 메타 잠금이면 못 쓴다(C383 검토 — 에센스와 같은 규칙)
      if (("prefix".equals(a.gen()) && fm.prefixLocked())
          || ("suffix".equals(a.gen()) && fm.suffixLocked())) {
        return Result.fail(item, "강제 옵션 쪽이 잠겨 있습니다(메타 제작)");
      }
      if (start.stream().anyMatch(x -> sharesGroup(x, a))
          || start.stream().filter(x -> x.gen().equals(a.gen())).count()
              >= maxPerGen(item, Rarity.RARE)) {
        return Result.fail(item, "강제 옵션이 들어갈 자리가 없습니다(같은 그룹 · 자리)");
      }
      start.add(a);
    }
    Result r =
        roll(
            item,
            Rarity.RARE,
            start,
            Math.max(rareCount(item, rng), start.size()),
            new Weights(mult, null, null, false, false, extra.isEmpty() ? null : extra),
            null,
            rng);
    if (r.error() != null) {
      return r;
    }
    if (!fracture && !corrupt && forced.isEmpty()) {
      // 얽혀든 화석 결과(C400)를 실어 보낸다
      return tangled == null ? r : new Result(r.item(), null, r.added(), r.removed(), tangled);
    }
    Item out = r.item();
    List<String> added = new ArrayList<>(forced.stream().map(Affix::id).toList());
    added.addAll(r.added());
    if (fracture) {
      List<Affix> cand = out.affixes().stream().filter(a -> !a.crafted() && !veiled(a)).toList();
      Affix fa = cand.get(rng.nextInt(cand.size()));
      List<Affix> as = new ArrayList<>(out.affixes());
      as.set(as.indexOf(fa), new Affix(fa.id(), fa.gen(), fa.groups(), false, true));
      out = out.with(Rarity.RARE, as);
    }
    if (corrupt) {
      // 피얼룩 화석 = 타락 + 부패 임플리싯(C383 검토 — 타락 표시만 했다). 임플리싯은 바알 오브와 같은 풀
      Affix ci = corruptedImplicit(out, rng);
      out = (ci == null ? out : out.withImplicits(List.of(ci))).corrupt();
      if (ci != null) {
        added.add(ci.id());
      }
    }
    List<String> removed =
        item.affixes().stream().filter(a -> !start.contains(a)).map(Affix::id).toList();
    return new Result(out, null, added, removed, tangled);
  }

  /** 얽혀든 화석 무작위 태그(C400) — 단순 화석들이 '증가' · '없음'으로 쓰는 태그. */
  static final List<String> TANGLED_TAGS =
      List.of(
          "attack",
          "attribute",
          "aura",
          "bleed",
          "caster",
          "chaos",
          "cold",
          "critical",
          "curse",
          "defences",
          "elemental",
          "fire",
          "gem",
          "life",
          "lightning",
          "mana",
          "minion",
          "physical",
          "poison",
          "speed");

  /** 제작대 — 자리 · 옵션 그룹 · 제작 옵션 한도(메타 "제작 옵션 3개" 면 3, 아니면 1). 일반이면 매직이 된다. */
  private Result bench(Item item, String modId) {
    PoeCraftDataService.BenchCraft b = data.bench(item.itemClass(), modId);
    if (b == null) {
      return Result.fail(item, "이 아이템 종류의 제작대 옵션이 아닙니다");
    }
    Rarity r = item.rarity() == Rarity.NORMAL ? Rarity.MAGIC : item.rarity();
    if (item.count(b.gen()) >= maxPerGen(item, r)) {
      return Result.fail(item, ("prefix".equals(b.gen()) ? "접두" : "접미") + " 자리가 없습니다");
    }
    Affix a =
        new Affix(b.modId(), b.gen(), b.groups() == null ? List.of() : b.groups(), true, false);
    if (item.affixes().stream().anyMatch(x -> sharesGroup(x, a))) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    long crafted = item.affixes().stream().filter(Affix::crafted).count();
    int limit = meta(item.affixes()).multimod() ? 3 : 1;
    if (crafted >= limit) {
      return Result.fail(item, limit == 1 ? "제작대 옵션은 하나만(제작 옵션 3개 메타 제작이 있으면 3개)" : "제작대 옵션은 3개까지");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(a);
    return new Result(item.with(r, out), null, List.of(a.id()), List.of());
  }

  private Result harvest(Item item, String id, List<Affix> keep, RandomGenerator rng) {
    PoeCraftDataService.Harvest h = data.harvest(id);
    if (h == null) {
      return Result.fail(item, "모르는 수확 제작");
    }
    String[] p = h.params() == null ? new String[0] : h.params().trim().split("\\s+");
    return switch (h.command()) {
      case "reroll_with_mod" -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        String tag = p.length > 0 ? p[0] : "";
        // 메타 공격/주문 금지는 무시(C432 — CoE 표 Harvest 'Reroll' 행 공격 · 주문 'NO'). 보장 옵션에도(merge 가 w.tagMult
        // 를 씀)
        yield roll(
            item,
            Rarity.RARE,
            keep,
            rareCount(item, rng),
            new Weights(Map.of(IGNORE_META_BLOCK, 1d), null),
            new Weights(Map.of(tag, 1d), null),
            rng);
      }
      case "reroll_with_current_tags_affinity_multiplier" -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        double v = p.length > 0 ? Double.parseDouble(p[0]) : 0;
        double f = 1 + v / 100;
        Map<String, Double> mult = new java.util.HashMap<>();
        // 옵션당 한 번만(C383 검토 — 겹치는 태그마다 곱해 10^5 배까지 갔다)
        mult.put(ONCE, 1d);
        // 메타 공격/주문 금지는 무시(C431 — CoE 표 'Harvest, Reforge more likely' 공격 · 주문 'NO')
        mult.put(IGNORE_META_BLOCK, 1d);
        for (Affix a : item.affixes()) {
          PoeCraftDataService.CraftMod m = data.mod(a.id());
          if (m != null && m.t() != null) {
            m.t().forEach(t -> mult.put(t, f));
          }
        }
        yield roll(
            item, Rarity.RARE, keep, rareCount(item, rng), new Weights(mult, null), null, rng);
      }
      case "remove_type_and_add_type_mod" ->
          removeAdd(item, p.length > 2 ? p[2] : "", h.params().contains("noinfluence"), rng);
      case "convert_mod" -> convert(item, p, rng);
      case "reroll_with_influence_mod" -> {
        if (item.rarity() != Rarity.RARE
            || item.influence() == null
            || item.influence().isEmpty()) {
          yield Result.fail(item, "영향력이 있는 레어에만 쓸 수 있습니다");
        }
        Set<String> only = influenceOnly(item, item.influence());
        yield roll(
            item,
            Rarity.RARE,
            keep,
            rareCount(item, rng),
            Weights.NONE,
            new Weights(Map.of(), only),
            rng);
      }
      case "reroll_influence_types" -> {
        // 영향력 유형 무작위 + 재련(10-15 C347, RerollInfluenceType) — 영향력 수는 그대로, 지금과 다른 조합 중 균등(같은 조합 제외는
        // 가정).
        // 매개변수 = 쓸 수 있는 종류(밑줄 = 빈칸)
        if (item.influence() == null || item.influence().isEmpty()) {
          yield Result.fail(item, "영향력이 있는 아이템에만 쓸 수 있습니다");
        }
        if (java.util.Arrays.stream(p)
            .map(x -> x.replace('_', ' '))
            .noneMatch(item.itemClass()::equals)) {
          yield Result.fail(item, "이 아이템 종류엔 쓸 수 없는 수확 제작입니다");
        }
        String head = item.itemClass() + "|" + nz(item.variant()) + "|";
        List<String> avail =
            INFLUENCES.stream().filter(x -> !data.pool(head + x).isEmpty()).sorted().toList();
        int n = item.influence().split(",").length;
        List<String> combos = new ArrayList<>();
        for (int i = 0; i < avail.size(); i++) {
          if (n == 1) {
            combos.add(avail.get(i));
          } else {
            for (int j = i + 1; j < avail.size(); j++) {
              combos.add(avail.get(i) + "," + avail.get(j));
            }
          }
        }
        combos.remove(item.influence());
        if (combos.isEmpty()) {
          yield Result.fail(item, "바꿀 수 있는 다른 영향력이 없습니다");
        }
        Item moved = item.withInfluence(combos.get(rng.nextInt(combos.size())));
        // 분열 · 메타 잠긴 쪽은 남긴다(C383 검토 — 분열만 남겨 잠긴 쪽이 통째로 비었다, 수확 재련은 잠금을 지킨다)
        List<Affix> fixed = kept(item);
        Result r =
            switch (item.rarity()) {
              case NORMAL -> new Result(moved, null, List.of(), List.of());
              case MAGIC ->
                  roll(moved, Rarity.MAGIC, fixed, magicCount(rng), Weights.NONE, null, rng);
              default ->
                  roll(
                      moved,
                      Rarity.RARE,
                      fixed,
                      Math.max(rareCount(item, rng), fixed.size()),
                      Weights.NONE,
                      null,
                      rng);
            };
        yield r.error() == null
            ? new Result(
                r.item(),
                null,
                r.added(),
                item.affixes().stream().filter(a -> !a.fractured()).map(Affix::id).toList())
            : Result.fail(item, r.error());
      }
      default -> Result.fail(item, "아직 계산하지 않는 수확 제작입니다");
    };
  }

  /** 태그 옵션 하나 추가 + 다른 옵션 하나 무작위 제거 — 빈 자리가 있으면 먼저 붙이고, 없으면 먼저 지운 뒤 붙인다(순서는 게임 데이터에 없어 이렇게 둔다). */
  private Result removeAdd(Item item, String tag, boolean noInfluence, RandomGenerator rng) {
    if (item.rarity() == Rarity.NORMAL) {
      return Result.fail(item, "매직 · 레어 아이템에만 쓸 수 있습니다");
    }
    if (noInfluence && item.influence() != null && !item.influence().isEmpty()) {
      return Result.fail(item, "영향력이 없는 아이템에만 쓸 수 있습니다");
    }
    Weights req = new Weights(Map.of(tag, 1d), null).withRequireTags(Set.of(tag));
    List<Affix> keep = kept(item);
    Affix add = pick(item, item.rarity(), req, rng);
    List<Affix> out = new ArrayList<>(item.affixes());
    if (add != null) {
      out.add(add);
      List<Affix> removable = out.stream().filter(a -> a != add && !keep.contains(a)).toList();
      if (removable.isEmpty()) {
        return Result.fail(item, "지울 수 있는 옵션이 없습니다");
      }
      Affix gone = removable.get(rng.nextInt(removable.size()));
      out.remove(gone);
      return new Result(item.with(item.rarity(), out), null, List.of(add.id()), List.of(gone.id()));
    }
    List<Affix> removable = out.stream().filter(a -> !keep.contains(a)).toList();
    if (removable.isEmpty()) {
      return Result.fail(item, "지울 수 있는 옵션이 없습니다");
    }
    Affix gone = removable.get(rng.nextInt(removable.size()));
    out.remove(gone);
    Affix add2 = pick(item.with(item.rarity(), out), item.rarity(), req, rng);
    if (add2 == null) {
      return Result.fail(item, "그 태그 옵션이 붙을 수 없습니다");
    }
    out.add(add2);
    return new Result(item.with(item.rarity(), out), null, List.of(add2.id()), List.of(gone.id()));
  }

  /**
   * 저항 변환 — params 끝 "CONVERT from to"(fire · cold · lightning). 그 원소 저항 옵션을 같은 등급(풀 안 아이템 레벨 순위)의
   * 다른 원소로.
   */
  private Result convert(Item item, String[] p, RandomGenerator rng) {
    int k = java.util.Arrays.asList(p).indexOf("CONVERT");
    if (k < 0 || k + 2 >= p.length) {
      return Result.fail(item, "변환 정보를 읽지 못했습니다");
    }
    List<Affix> fixedSide = kept(item);
    // 옵션 유형(mt)이 있으면 유형 기준으로(C384 — 저항 · 피해 12종 모두)
    List<String> srcTypes = java.util.Arrays.asList(p).subList(0, k);
    Affix byType =
        item.affixes().stream()
            .filter(a -> !a.fractured() && !a.crafted() && !fixedSide.contains(a))
            .filter(
                a -> {
                  PoeCraftDataService.CraftMod m = data.mod(a.id());
                  return m != null && m.mt() != null && srcTypes.contains(m.mt());
                })
            .findFirst()
            .orElse(null);
    if (byType != null) {
      return convertByType(item, byType, srcTypes, p[k + 1], p[k + 2]);
    }
    // 피해 변환은 유형 이름이 있어야 맞게 바꾼다 — 유형이 없는 옛 데이터면 막는다(C383 검토: 저항을 바꾸던 오류)
    if (srcTypes.stream().anyMatch(x -> x.contains("Damage"))) {
      return Result.fail(item, cap(p[k + 1]) + " 피해 옵션이 없습니다");
    }
    String fromFam = cap(p[k + 1]) + "Resist";
    String toFam = cap(p[k + 2]) + "Resist";
    Affix src =
        item.affixes().stream()
            .filter(a -> !a.fractured() && !a.crafted() && !fixedSide.contains(a))
            .filter(
                a -> {
                  PoeCraftDataService.CraftMod m = data.mod(a.id());
                  return m != null && fromFam.equals(m.f());
                })
            .findFirst()
            .orElse(null);
    if (src == null) {
      return Result.fail(item, cap(p[k + 1]) + " 저항 옵션이 없습니다");
    }
    List<String> fromLadder = ladder(item, fromFam);
    List<String> toLadder = ladder(item, toFam);
    int rank = fromLadder.indexOf(src.id());
    if (rank < 0 || toLadder.isEmpty()) {
      return Result.fail(item, "바꿀 등급을 찾지 못했습니다");
    }
    String dst = toLadder.get(Math.min(rank, toLadder.size() - 1));
    PoeCraftDataService.CraftMod dm = data.mod(dst);
    Affix nu = new Affix(dst, dm.g(), dm.grp(), false, false);
    // 바뀐 옵션이 남은 옵션과 같은 그룹이면 못 바꾼다(C383 검토 — 화염 · 냉기 저항이 있을 때 화염 → 냉기면 냉기 저항 두 줄)
    if (item.affixes().stream().filter(a -> a != src).anyMatch(a -> sharesGroup(a, nu))) {
      return Result.fail(item, "바꿀 옵션과 같은 그룹 옵션이 이미 있습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(out.indexOf(src), nu);
    return new Result(item.with(item.rarity(), out), null, List.of(dst), List.of(src.id()));
  }

  /**
   * 유형 기준 원소 변환(C384) — 대상 유형 = 같은 자리의 대상 원소 유형 목록(다른 변환 항목 "Change{대상}To{X}…" 의 원본 목록, 순서가 같다 —
   * 없으면 이름의 원소만 바꿈), 같은 등급(그 유형 사다리 순번), 바뀐 옵션이 남은 옵션과 같은 그룹이면 실패.
   */
  private Result convertByType(
      Item item, Affix src, List<String> srcTypes, String fromEl, String toEl) {
    String srcType = data.mod(src.id()).mt();
    String kind = srcTypes.stream().anyMatch(x -> x.contains("Damage")) ? "Damage" : "Resist";
    List<String> tgtTypes = null;
    for (String x : List.of("Fire", "Cold", "Lightning")) {
      if (x.equalsIgnoreCase(toEl)) {
        continue;
      }
      PoeCraftDataService.Harvest h = data.harvest("Change" + cap(toEl) + "To" + x + kind);
      if (h != null && h.params() != null) {
        List<String> ps = java.util.Arrays.asList(h.params().trim().split("\\s+"));
        int kk = ps.indexOf("CONVERT");
        if (kk > 0) {
          tgtTypes = ps.subList(0, kk);
          break;
        }
      }
    }
    int idx = srcTypes.indexOf(srcType);
    String tgtType =
        tgtTypes != null && idx >= 0 && idx < tgtTypes.size()
            ? tgtTypes.get(idx)
            : srcType.replace(cap(fromEl), cap(toEl));
    List<String> fromLadder = ladderByType(item, srcType);
    List<String> toLadder = ladderByType(item, tgtType);
    int rank = fromLadder.indexOf(src.id());
    if (rank < 0 || toLadder.isEmpty()) {
      return Result.fail(item, "이 아이템에 바꿀 " + cap(toEl) + " 옵션이 없습니다");
    }
    String dst = toLadder.get(Math.min(rank, toLadder.size() - 1));
    PoeCraftDataService.CraftMod dm = data.mod(dst);
    Affix nu = new Affix(dst, dm.g(), dm.grp(), false, false);
    if (item.affixes().stream().filter(a -> a != src).anyMatch(a -> sharesGroup(a, nu))) {
      return Result.fail(item, "바꿀 옵션과 같은 그룹 옵션이 이미 있습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(out.indexOf(src), nu);
    return new Result(item.with(item.rarity(), out), null, List.of(dst), List.of(src.id()));
  }

  /** 풀 안 그 유형(ModType) 옵션, 아이템 레벨 요구 높은 순(T1 먼저) — C384. */
  private List<String> ladderByType(Item item, String type) {
    PoolIndex px = index(item.poolKey());
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < px.ids().length; i++) {
      PoeCraftDataService.CraftMod m = data.mod(px.ids()[i]);
      if (m != null && type.equals(m.mt())) {
        ids.add(px.ids()[i]);
      }
    }
    ids.sort((a, b) -> Integer.compare(data.mod(b).l(), data.mod(a).l()));
    return ids;
  }

  /** 풀 안 그 계열 옵션, 아이템 레벨 요구 높은 순(T1 먼저). */
  private List<String> ladder(Item item, String family) {
    PoolIndex px = index(item.poolKey());
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < px.ids().length; i++) {
      PoeCraftDataService.CraftMod m = data.mod(px.ids()[i]);
      if (m != null && family.equals(m.f())) {
        ids.add(px.ids()[i]);
      }
    }
    ids.sort((a, b) -> Integer.compare(data.mod(b).l(), data.mod(a).l()));
    return ids;
  }

  private static String cap(String s) {
    return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /** 영향력 전용 옵션 — 그 영향력 풀에는 있고 영향력 없는 풀에는 없는 옵션. */
  private Set<String> influenceOnly(Item item, String influence) {
    Map<String, Integer> inf =
        data.pool(item.itemClass() + "|" + nz(item.variant()) + "|" + influence);
    Map<String, Integer> base = data.pool(item.itemClass() + "|" + nz(item.variant()) + "|");
    Set<String> out = new HashSet<>();
    for (String id : inf.keySet()) {
      if (!base.containsKey(id)) {
        out.add(id);
      }
    }
    return out;
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  /** 짐승 제작 영향력 추가가 받는 영향력(C387 — 레시피 "쉐이퍼 · 엘더 · 대속자 · 사냥꾼 · 십자군 · 전쟁군주 아이템에 속성 부여 추가"). */
  static final List<String> BEAST_INFLUENCES =
      List.of("shaper", "elder", "eyrie", "basilisk", "crusader", "adjudicator");

  /**
   * 짐승 제작(C387, BestiaryRecipes EinharMasterCraft30 · 31 · 42~47). swap-suffix = 무작위 접두 하나 지우고 접미
   * 하나 붙임(swap-prefix 반대) — 레시피 메모 "희귀 아이템에만 적용", 지우는 쪽은 소멸처럼 분열 · 메타 잠금 쪽 제외, 붙는 쪽은 엑잘처럼 그 쪽
   * 풀에서(자리가 있어야). influence:X = 이미 X 영향력이 있는 레어에 X 전용 옵션 하나(정복자 엑잘의 옵션 고르기와 같음, 영향력은 그대로).
   */
  private Result beast(Item item, String arg, RandomGenerator rng) {
    String a = arg == null ? "" : arg;
    if (a.startsWith("aspect:")) {
      return aspect(item, a.substring("aspect:".length()));
    }
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "희귀 아이템에만 적용");
    }
    if (a.startsWith("influence:")) {
      String inf = a.substring("influence:".length());
      if (!BEAST_INFLUENCES.contains(inf)) {
        return Result.fail(item, "알 수 없는 영향력입니다");
      }
      List<String> has =
          item.influence() == null || item.influence().isEmpty()
              ? List.of()
              : List.of(item.influence().split(","));
      if (!has.contains(inf)) {
        return Result.fail(item, "그 영향력이 있는 아이템에만 쓸 수 있습니다");
      }
      if (full(item, Rarity.RARE)) {
        return Result.fail(item, "빈 자리가 없습니다");
      }
      // 엑잘처럼(C430 — maxroll 'It does not guarantee an influenced modifier'): 그 영향력 아이템 풀에서 하나
      Affix x = pick(item, Rarity.RARE, Weights.NONE, rng);
      if (x == null) {
        return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
      }
      List<Affix> out = new ArrayList<>(item.affixes());
      out.add(x);
      return new Result(item.with(Rarity.RARE, out), null, List.of(x.id()), List.of());
    }
    boolean addSuffix = "swap-suffix".equals(a);
    if (!addSuffix && !"swap-prefix".equals(a)) {
      return Result.fail(item, "알 수 없는 짐승 제작입니다");
    }
    String addGen = addSuffix ? "suffix" : "prefix";
    String rmGen = addSuffix ? "prefix" : "suffix";
    if (item.count(addGen) >= maxPerGen(item, Rarity.RARE)) {
      return Result.fail(item, addSuffix ? "빈 접미어 자리가 없습니다" : "빈 접두어 자리가 없습니다");
    }
    List<Affix> keep = kept(item);
    Meta bm = meta(item.affixes());
    boolean rmLocked = addSuffix ? bm.prefixLocked() : bm.suffixLocked();
    List<Affix> cand =
        item.affixes().stream().filter(x -> rmGen.equals(x.gen()) && !keep.contains(x)).toList();
    if (cand.isEmpty() && !rmLocked) {
      return Result.fail(item, (addSuffix ? "지울 접두어" : "지울 접미어") + "가 없습니다(분열 · 잠긴 쪽 제외)");
    }
    // 지울 쪽이 잠겨 있으면 지우지 않고 붙이기만(C430 — maxroll 'no prefix is removed but a Suffix is still added')
    Affix gone = rmLocked ? null : cand.get(rng.nextInt(cand.size()));
    List<Affix> mid = new ArrayList<>(item.affixes());
    if (gone != null) {
      mid.remove(gone);
    }
    Item m = item.with(Rarity.RARE, mid);
    Affix x = pick(m, Rarity.RARE, Weights.NONE.closing(addSuffix, !addSuffix), rng);
    if (x == null) {
      return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
    }
    mid.add(x);
    return new Result(
        m.with(Rarity.RARE, mid),
        null,
        List.of(x.id()),
        gone == null ? List.of() : List.of(gone.id()));
  }

  /** 위상 종류 → 옵션 id(C388 — 새 = Bird). */
  static final Map<String, String> ASPECTS =
      Map.of(
          "cat", "GrantsCatAspectCrafted30",
          "avian", "GrantsBirdAspectCrafted30",
          "spider", "GrantsSpiderAspectCrafted30",
          "crab", "GrantsCrabAspectCrafted30");

  /**
   * 위상 스킬 부여(C388) — 제작 접미어 하나(그룹 1047 — 위상끼리 겹침). 제작대 옵션과 같은 규칙: 일반이면 매직이 되고, 제작 옵션 한도(1, 메타 '제작
   * 옵션 3개'면 3)와 같은 그룹을 본다. 플라스크 · 주얼은 제외(가정 — 레시피 표에 대상 종류가 없다).
   */
  private Result aspect(Item item, String kind) {
    return aspectId(item, ASPECTS.get(kind));
  }

  /** 판테온 위상(C391 브라인훅의 두카트) — 옵션 id. */
  static final List<String> PANTHEON_ASPECTS =
      List.of(
          "GrantsArakaaliAspectCrafted",
          "GrantsBrineKingAspectCrafted",
          "GrantsLunarisAspectCrafted",
          "GrantsSolarisAspectCrafted");

  /** 아이템 종류 → 악명 높은 속성 부위 태그(C393, 도메인 41 스폰 태그). */
  static final Map<String, String> INFAMOUS_TAG =
      Map.ofEntries(
          Map.entry("Body Armour", "body_armour"),
          Map.entry("Boots", "boots"),
          Map.entry("Gloves", "gloves"),
          Map.entry("Helmet", "helmet"),
          Map.entry("Amulet", "amulet"),
          Map.entry("Belt", "belt"),
          Map.entry("Ring", "ring"),
          Map.entry("Quiver", "quiver"),
          Map.entry("Bow", "bow"),
          Map.entry("Staff", "staff"),
          Map.entry("Warstaff", "staff"),
          Map.entry("Wand", "wand"));

  /** 능력치 하나 옵션 유형(C391 고상한 자의 두카트). */
  static final List<String> ATTRIBUTE_TYPES = List.of("Strength", "Dexterity", "Intelligence");

  /** 두카트(C391) — 군왕호 올플레임 제작. CurrencyItems 설명 그대로의 동작, 확률은 데이터(가중치) 또는 균등. */
  private Result ducat(Item item, String arg, RandomGenerator rng) {
    String a = arg == null ? "" : arg;
    switch (a) {
      case "genteel" -> {
        if (item.rarity() == Rarity.NORMAL) {
          return Result.fail(item, "매직 · 희귀 아이템에만 쓸 수 있습니다");
        }
        List<Affix> keep = kept(item);
        List<Affix> cand =
            item.affixes().stream()
                .filter(x -> !x.fractured() && !x.crafted() && !keep.contains(x))
                .filter(
                    x -> {
                      PoeCraftDataService.CraftMod m = data.mod(x.id());
                      return m != null && m.mt() != null && ATTRIBUTE_TYPES.contains(m.mt());
                    })
                .toList();
        if (cand.isEmpty()) {
          return Result.fail(item, "능력치 하나를 부여하는 속성이 없습니다");
        }
        Affix src = cand.get(rng.nextInt(cand.size()));
        String srcType = data.mod(src.id()).mt();
        List<String> fromLadder = ladderByType(item, srcType);
        int rank = fromLadder.indexOf(src.id());
        // 갈 수 있는 능력치만(C397 검토 — 사다리 없는 쪽 · 이미 있는 쪽을 먼저 뽑아 무작위로 실패했다)
        List<Affix> targets = new ArrayList<>();
        for (String t : ATTRIBUTE_TYPES) {
          if (t.equals(srcType) || rank < 0) {
            continue;
          }
          List<String> toLadder = ladderByType(item, t);
          if (toLadder.isEmpty()) {
            continue;
          }
          String dst = toLadder.get(Math.min(rank, toLadder.size() - 1));
          PoeCraftDataService.CraftMod dm = data.mod(dst);
          Affix cand2 = new Affix(dst, dm.g(), dm.grp(), false, false);
          if (item.affixes().stream().filter(x -> x != src).noneMatch(x -> sharesGroup(x, cand2))) {
            targets.add(cand2);
          }
        }
        if (targets.isEmpty()) {
          return Result.fail(item, "이 아이템에 바꿀 수 있는 다른 능력치 옵션이 없습니다");
        }
        Affix nu = targets.get(rng.nextInt(targets.size()));
        String dst = nu.id();
        List<Affix> out = new ArrayList<>(item.affixes());
        out.set(out.indexOf(src), nu);
        return new Result(item.with(item.rarity(), out), null, List.of(dst), List.of(src.id()));
      }
      case "rotmother" -> {
        if (!"Belt".equals(item.itemClass()) || item.rarity() != Rarity.RARE) {
          return Result.fail(item, "희귀 허리띠에만 쓸 수 있습니다");
        }
        int max = maxPerGen(item, Rarity.RARE);
        List<String> ids = new ArrayList<>();
        List<Integer> ws = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, PoeCraftDataService.HazardMod> e :
            new java.util.TreeMap<>(data.hazardMods()).entrySet()) {
          PoeCraftDataService.HazardMod h = e.getValue();
          Affix x =
              new Affix(e.getKey(), h.g(), h.grp() == null ? List.of() : h.grp(), false, false);
          if (h.w() <= 0
              || item.count(h.g()) >= max
              || h.l() > item.ilvl()
              || item.affixes().stream().anyMatch(y -> sharesGroup(y, x))) {
            continue;
          }
          ids.add(e.getKey());
          ws.add(h.w());
          total += h.w();
        }
        if (ids.isEmpty()) {
          return Result.fail(item, "붙을 수 있는 덫 · 지뢰 속성이 없습니다(자리 · 이미 있음)");
        }
        double r = rng.nextDouble() * total;
        String id = ids.get(ids.size() - 1);
        for (int i = 0; i < ids.size(); i++) {
          r -= ws.get(i);
          if (r < 0) {
            id = ids.get(i);
            break;
          }
        }
        PoeCraftDataService.HazardMod h = data.hazardMods().get(id);
        List<Affix> out = new ArrayList<>(item.affixes());
        out.add(new Affix(id, h.g(), h.grp() == null ? List.of() : h.grp(), false, false));
        return new Result(item.with(Rarity.RARE, out), null, List.of(id), List.of());
      }
      case "ukatoa" -> {
        if (!"Amulet".equals(item.itemClass())) {
          return Result.fail(item, "비-고유 목걸이에만 쓸 수 있습니다");
        }
        if (item.influence() != null && !item.influence().isEmpty()) {
          // 영향력과 섬뜩한 고정 속성은 함께 못 있다(C397 검토 — 불씨 · 정복자 엑잘과 같은 규칙)
          return Result.fail(item, "영향력이 있는 아이템에는 섬뜩한 고정 속성을 추가할 수 없습니다");
        }
        if (item.implicits().stream()
            .anyMatch(x -> EXARCH.equals(x.gen()) || EATER.equals(x.gen()))) {
          return Result.fail(item, "이미 섬뜩한 고정 속성이 있습니다");
        }
        List<String> ids = new ArrayList<>();
        List<Integer> ws = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, Integer> e :
            new java.util.TreeMap<>(data.eldritchPool(item.poolKey())).entrySet()) {
          PoeCraftDataService.EldritchMod m = data.eldritchMod(e.getKey());
          // 엘드리치 고정 속성은 아이템 레벨 제한 없음(C415)
          if (m == null || e.getValue() <= 0) {
            continue;
          }
          ids.add(e.getKey());
          ws.add(e.getValue());
          total += e.getValue();
        }
        if (ids.isEmpty()) {
          return Result.fail(item, "이 목걸이에 붙을 섬뜩한 고정 속성이 없습니다");
        }
        double r = rng.nextDouble() * total;
        String id = ids.get(ids.size() - 1);
        for (int i = 0; i < ids.size(); i++) {
          r -= ws.get(i);
          if (r < 0) {
            id = ids.get(i);
            break;
          }
        }
        PoeCraftDataService.EldritchMod m = data.eldritchMod(id);
        Affix x = new Affix(id, m.g(), m.grp() == null ? List.of() : m.grp(), false, false);
        return new Result(item.withImplicits(List.of(x)), null, List.of(id), List.of());
      }
      case "cyaxan" -> {
        if (item.rarity() != Rarity.RARE) {
          return Result.fail(item, "희귀 아이템에만 쓸 수 있습니다");
        }
        String tag = INFAMOUS_TAG.get(item.itemClass());
        List<Affix> keep = kept(item);
        int max = maxPerGen(item, Rarity.RARE);
        // 잠긴 쪽도 빈자리면 넣는다(C433 검토 — C430 뒤 roll() 은 잠긴 쪽 기존 옵션만 지키고 빈자리엔 새 옵션을 붙인다. 자리 · 그룹은 keep
        // 기준)
        List<String> ids = new ArrayList<>();
        List<Integer> ws = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, PoeCraftDataService.InfamousMod> e :
            new java.util.TreeMap<>(data.infamousMods()).entrySet()) {
          PoeCraftDataService.InfamousMod im = e.getValue();
          Affix x =
              new Affix(e.getKey(), im.g(), im.grp() == null ? List.of() : im.grp(), false, false);
          if (tag == null
              || im.cls() == null
              || !im.cls().contains(tag)
              || im.w() <= 0
              || im.l() > item.ilvl()
              || keep.stream().filter(y -> im.g().equals(y.gen())).count() >= max
              || keep.stream().anyMatch(y -> sharesGroup(y, x))) {
            continue;
          }
          ids.add(e.getKey());
          ws.add(im.w());
          total += im.w();
        }
        if (ids.isEmpty()) {
          return Result.fail(item, "이 아이템에 붙을 악명 높은 속성이 없습니다(종류 · 아이템 레벨 68)");
        }
        double r = rng.nextDouble() * total;
        String id = ids.get(ids.size() - 1);
        for (int i = 0; i < ids.size(); i++) {
          r -= ws.get(i);
          if (r < 0) {
            id = ids.get(i);
            break;
          }
        }
        PoeCraftDataService.InfamousMod im = data.infamousMods().get(id);
        List<Affix> start = new ArrayList<>(keep);
        start.add(new Affix(id, im.g(), im.grp() == null ? List.of() : im.grp(), false, false));
        Result rr =
            roll(
                item,
                Rarity.RARE,
                start,
                Math.max(rareCount(item, rng), start.size()),
                Weights.NONE,
                null,
                rng);
        List<String> added = new ArrayList<>(rr.added());
        added.add(0, id);
        return new Result(rr.item(), null, added, rr.removed());
      }
      case "merrick" -> {
        if (!"AbyssJewel".equals(item.itemClass()) || item.rarity() != Rarity.RARE) {
          return Result.fail(item, "희귀 심연 주얼에만 쓸 수 있습니다");
        }
        Affix x = pick(item, Rarity.RARE, Weights.NONE, rng, true);
        if (x == null) {
          return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
        }
        List<Affix> out = new ArrayList<>(item.affixes());
        out.add(x);
        return new Result(item.with(Rarity.RARE, out).corrupt(), null, List.of(x.id()), List.of());
      }
      case "brinehook" -> {
        List<String> ids = PANTHEON_ASPECTS.stream().filter(x -> data.beastMod(x) != null).toList();
        if (ids.isEmpty()) {
          return Result.fail(item, "판테온 위상 데이터가 없습니다");
        }
        return aspectId(item, ids.get(rng.nextInt(ids.size())));
      }
      default -> {
        return Result.fail(item, "알 수 없는 두카트입니다");
      }
    }
  }

  private Result aspectId(Item item, String id) {
    PoeCraftDataService.VeiledMod m = id == null ? null : data.beastMod(id);
    if (m == null) {
      return Result.fail(item, "알 수 없는 위상입니다");
    }
    if (item.itemClass().contains("Flask") || item.itemClass().contains("Jewel")) {
      return Result.fail(item, "장비에만 쓸 수 있습니다");
    }
    Rarity r = item.rarity() == Rarity.NORMAL ? Rarity.MAGIC : item.rarity();
    if (item.count("suffix") >= maxPerGen(item, r)) {
      return Result.fail(item, "접미 자리가 없습니다");
    }
    Affix x = new Affix(id, "suffix", m.grp() == null ? List.of() : m.grp(), true, false);
    if (item.affixes().stream().anyMatch(y -> sharesGroup(y, x))) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    long crafted = item.affixes().stream().filter(Affix::crafted).count();
    int limit = meta(item.affixes()).multimod() ? 3 : 1;
    if (crafted >= limit) {
      return Result.fail(item, limit == 1 ? "제작 옵션은 하나만(제작 옵션 3개 메타 제작이 있으면 3개)" : "제작 옵션은 3개까지");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(x);
    return new Result(item.with(r, out), null, List.of(id), List.of());
  }

  /**
   * 갈등의 오브(C398) — 총주교 · 포식자 고정 속성 둘 다 있어야. 오를 쪽 확률 = 50% − 11% × (내 등급 − 상대 등급)(커뮤니티 측정, 5~95% 로
   * 자름), 6등급은 못 올라 상대가 오름. 오른 쪽 +1 등급 · 다른 쪽 −1 등급(1등급이면 지움). note = "conflict:오른 진영:내린
   * 진영[:removed]".
   */
  private Result conflict(Item item, RandomGenerator rng) {
    Affix ex =
        item.implicits().stream().filter(a -> EXARCH.equals(a.gen())).findFirst().orElse(null);
    Affix ea =
        item.implicits().stream().filter(a -> EATER.equals(a.gen())).findFirst().orElse(null);
    if (ex == null || ea == null) {
      // 인게임 설명 그대로
      return Result.fail(item, "작열의 총주교 고정 속성과 세계 포식자 고정 속성이 모두 있는 아이템에만 사용할 수 있습니다");
    }
    int tx = eldritchTier(ex.id());
    int te = eldritchTier(ea.id());
    double pEx = tx >= 6 ? 0 : te >= 6 ? 1 : Math.max(0.05, Math.min(0.95, 0.5 - 0.11 * (tx - te)));
    boolean upEx = rng.nextDouble() < pEx;
    Affix up = upEx ? ex : ea;
    Affix down = upEx ? ea : ex;
    String upId = eldritchStep(item, up.id(), +1);
    if (upId == null) {
      // 고른 쪽에 다음 등급이 없으면 다른 쪽을 올린다(C402 검토)
      Affix t = up;
      up = down;
      down = t;
      upId = eldritchStep(item, up.id(), +1);
    }
    if (upId == null) {
      return Result.fail(item, "더 올릴 수 있는 고정 속성이 없습니다");
    }
    String downId = eldritchStep(item, down.id(), -1);
    List<Affix> out = new ArrayList<>();
    List<String> added = new ArrayList<>();
    List<String> removed = new ArrayList<>(List.of(up.id(), down.id()));
    for (Affix a : item.implicits()) {
      if (a == up) {
        PoeCraftDataService.EldritchMod m = data.eldritchMod(upId);
        out.add(new Affix(upId, a.gen(), m.grp() == null ? List.of() : m.grp(), false, false));
        added.add(upId);
      } else if (a == down) {
        if (downId != null) {
          PoeCraftDataService.EldritchMod m = data.eldritchMod(downId);
          out.add(new Affix(downId, a.gen(), m.grp() == null ? List.of() : m.grp(), false, false));
          added.add(downId);
        }
      } else {
        out.add(a);
      }
    }
    return new Result(
        item.withImplicits(out),
        null,
        added,
        removed,
        "conflict:" + up.gen() + ":" + down.gen() + (downId == null ? ":removed" : ""));
  }

  /** 섬뜩한 고정 속성 등급(1~6), 모르면 0. */
  private int eldritchTier(String id) {
    PoeCraftDataService.EldritchMod m = data.eldritchMod(id);
    return m == null ? 0 : m.tier();
  }

  /** 같은 계열 한 등급 위/아래 id(…EldritchImplicitN 의 N ± 1, 이 아이템 풀에 있어야). 없으면 null. */
  private String eldritchStep(Item item, String id, int d) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("^(.*EldritchImplicit)(\\d+)(.*)$").matcher(id);
    if (!m.matches()) {
      return null;
    }
    int n = Integer.parseInt(m.group(2)) + d;
    if (n < 1 || n > 6) {
      return null;
    }
    String nid = m.group(1) + n + m.group(3);
    return data.eldritchMod(nid) != null && data.eldritchPool(item.poolKey()).containsKey(nid)
        ? nid
        : null;
  }

  /** 정복자 엑잘 — 영향력 없는 레어에 영향력 + 그 영향력 전용 옵션 하나. */
  /** 영향력 옵션을 더할 수 있는 아이템 레벨(C415 — ClientStrings ItemErrorItemLevelNotHighEnough). */
  static final int INFLUENCE_MIN_ILVL = 68;

  static final String INFLUENCE_ILVL_ERROR = "아이템 레벨이 68 미만이므로 영향력 속성 부여를 추가할 수 없습니다.";

  private Result conqueror(Item item, String influence, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
    }
    if (item.affixes().stream().anyMatch(Affix::fractured)) {
      // 분열 아이템은 영향력을 못 받는다(C430 — poedb Fractured_item, 인게임 ItemErrorItemIsFractured)
      return Result.fail(item, FRACTURED_ERROR);
    }
    if (item.ilvl() < INFLUENCE_MIN_ILVL) {
      return Result.fail(item, INFLUENCE_ILVL_ERROR);
    }
    if (item.influence() != null && !item.influence().isEmpty()) {
      return Result.fail(item, "이미 영향력이 있는 아이템입니다");
    }
    if (influence == null || !INFLUENCE_EXALTS.contains(influence)) {
      return Result.fail(item, "영향력 엑잘티드 오브의 영향력이 아닙니다");
    }
    if (!item.implicits().isEmpty()) {
      // 인게임 오류 문구(ClientStrings ItemErrorHasEldritchImplicit)
      return Result.fail(item, "작열의 총주교 속성 또는 세계 포식자 속성이 부여된 아이템에는 영향력을 추가할 수 없습니다");
    }
    if (data.pool(item.itemClass() + "|" + nz(item.variant()) + "|" + influence).isEmpty()) {
      return Result.fail(item, "이 아이템 종류엔 그 영향력이 없습니다");
    }
    if (full(item, Rarity.RARE)) {
      return Result.fail(item, "빈 자리가 없습니다");
    }
    Item inf = item.withInfluence(influence);
    Set<String> only = influenceOnly(item, influence);
    Affix a = pick(inf, Rarity.RARE, new Weights(Map.of(), only), rng);
    if (a == null) {
      return Result.fail(item, "붙을 수 있는 영향력 옵션이 없습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(a);
    return new Result(inf.with(Rarity.RARE, out), null, List.of(a.id()), List.of());
  }

  /** 장막 카오스 — 레어를 다시 굴리되(남는 옵션은 그대로) 장막 옵션 하나 보장(열린 쪽 중 무작위, 잠긴 쪽 제외). */
  private Result veiledChaos(Item item, List<Affix> keep, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
    }
    Meta lock = meta(item.affixes());
    int max = maxPerGen(item, Rarity.RARE);
    List<String> sides = new ArrayList<>();
    if (!lock.prefixLocked() && keep.stream().filter(a -> "prefix".equals(a.gen())).count() < max) {
      sides.add("prefix");
    }
    if (!lock.suffixLocked() && keep.stream().filter(a -> "suffix".equals(a.gen())).count() < max) {
      sides.add("suffix");
    }
    if (sides.isEmpty()) {
      return Result.fail(item, "장막 옵션이 들어갈 자리가 없습니다");
    }
    List<Affix> start = new ArrayList<>(keep);
    Affix v = veil(sides.get(rng.nextInt(sides.size())));
    start.add(v);
    Result r =
        roll(
            item,
            Rarity.RARE,
            start,
            Math.max(rareCount(item, rng), start.size()),
            Weights.NONE,
            null,
            rng);
    List<String> added = new ArrayList<>();
    added.add(v.id());
    added.addAll(r.added());
    List<String> removed =
        item.affixes().stream().filter(a -> !keep.contains(a)).map(Affix::id).toList();
    return new Result(r.item(), null, added, removed);
  }

  /** 영향력 옵션 — 영향력 없는 같은 종류 풀에 없고 영향력 풀에 있는 것, 또는 고양 옵션. */
  boolean influenced(Item item, Affix a) {
    if (item.influence() == null || item.influence().isEmpty() || a.crafted()) {
      return false;
    }
    if (data.elevatedMod(a.id()) != null) {
      return true;
    }
    String base = item.itemClass() + "|" + nz(item.variant()) + "|";
    return !data.pool(base).containsKey(a.id()) && data.pool(item.poolKey()).containsKey(a.id());
  }

  /**
   * 지배의 오브 — 영향력 옵션 둘 이상인 레어 갑옷 · 장화 · 장갑 · 투구(poedb). 영향력 옵션 하나를 한 등급 올리고(최고 등급이면 고양 옵션, 고양 옵션이면
   * 수치만 다시 = 그대로) 다른 하나를 지운다. 고르는 확률은 균등으로 가정(근거 없음). 분열 · 잠긴 쪽 옵션은 지우지 않는다.
   */
  private Result dominanceOrb(Item item, List<Affix> keep, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE || !ELDRITCH_CLASSES.contains(item.itemClass())) {
      return Result.fail(item, "레어 갑옷 · 장화 · 장갑 · 투구에만 쓸 수 있습니다");
    }
    List<Affix> inf = item.affixes().stream().filter(a -> influenced(item, a)).toList();
    if (inf.size() < 2) {
      return Result.fail(item, "영향력 옵션이 둘 이상인 아이템에만 쓸 수 있습니다");
    }
    // 잠긴 쪽 · 분열 옵션은 올리지 않는다(C430 — poedb "modifiers that can't be changed … will not be affected",
    // 분열은 C433 검토)
    Meta dm = meta(item.affixes());
    List<Affix> upCand =
        inf.stream()
            .filter(
                a ->
                    !a.fractured()
                        && !(dm.prefixLocked() && "prefix".equals(a.gen()))
                        && !(dm.suffixLocked() && "suffix".equals(a.gen())))
            .toList();
    if (upCand.isEmpty()) {
      return Result.fail(item, "올릴 수 있는 영향력 옵션이 없습니다(잠긴 쪽)");
    }
    Affix up = upCand.get(rng.nextInt(upCand.size()));
    List<Affix> rest = inf.stream().filter(a -> a != up && !keep.contains(a)).toList();
    if (rest.isEmpty()) {
      return Result.fail(item, "지울 수 있는 다른 영향력 옵션이 없습니다(분열 · 잠긴 쪽)");
    }
    Affix gone = rest.get(rng.nextInt(rest.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    List<String> added = new ArrayList<>();
    String next = data.upgrade(item.poolKey()).get(up.id());
    if (next != null) {
      PoeCraftDataService.VeiledMod el = data.elevatedMod(next);
      PoeCraftDataService.CraftMod m = data.mod(next);
      List<Integer> grp = el != null ? el.grp() : m != null ? m.grp() : up.groups();
      out.set(
          out.indexOf(up),
          new Affix(next, up.gen(), grp == null ? List.of() : grp, false, up.fractured()));
      added.add(next);
    }
    out.remove(gone);
    List<String> removed = new ArrayList<>();
    removed.add(gone.id());
    if (next != null) {
      removed.add(up.id());
    }
    return new Result(item.with(Rarity.RARE, out), null, added, removed);
  }

  /**
   * 각성자의 오브(poedb) — 같은 종류 · 다른 영향력 두 아이템, 없앨 아이템(arg 영향력 · 옵션)의 영향력을 이 아이템에 더하고 두 영향력 레어로 새로 굴린다.
   * 각 아이템의 영향력 옵션 하나씩 보장(둘 이상이면 무작위, 같은 그룹이면 하나를 무작위로 버림), 분열 옵션은 남긴다(가정). 없앨 아이템 옵션을 안 고르면 그 영향력
   * 전용 옵션에서 가중치로 하나(그 아이템을 모르니 대표 옵션을 뽑는 셈).
   */
  private Result awakener(Item item, String arg, RandomGenerator rng) {
    if (item.rarity() == Rarity.NORMAL) {
      // 인게임 문구(ClientStrings ItemErrorItemsAreNormal, C430)
      return Result.fail(item, "아이템 하나 혹은 두 개의 희귀도가 일반입니다.");
    }
    if (item.affixes().stream().anyMatch(Affix::fractured)) {
      // 분열 아이템은 각성자의 오브로 영향력을 못 받는다(C430 — poedb 'cannot be influenced using … Awakener's Orb')
      return Result.fail(item, FRACTURED_ERROR);
    }
    if (item.ilvl() < INFLUENCE_MIN_ILVL) {
      return Result.fail(item, INFLUENCE_ILVL_ERROR);
    }
    String mine = item.influence() == null ? "" : item.influence();
    if (mine.isEmpty() || mine.contains(",")) {
      return Result.fail(item, "영향력이 하나인 아이템에만 쓸 수 있습니다");
    }
    if (!item.implicits().isEmpty()) {
      return Result.fail(item, "작열의 총주교 속성 또는 세계 포식자 속성이 부여된 아이템에는 영향력을 추가할 수 없습니다");
    }
    String other = arg == null ? "" : arg.contains(":") ? arg.substring(0, arg.indexOf(':')) : arg;
    String otherMod = arg != null && arg.contains(":") ? arg.substring(arg.indexOf(':') + 1) : null;
    if (!INFLUENCES.contains(other) || other.equals(mine)) {
      return Result.fail(item, "다른 영향력의 아이템을 고르세요");
    }
    String head = item.itemClass() + "|" + nz(item.variant()) + "|";
    Map<String, Integer> base = data.pool(head);
    Map<String, Integer> otherPool = data.pool(head + other);
    if (otherPool.isEmpty()) {
      return Result.fail(item, "이 아이템 종류엔 그 영향력이 없습니다");
    }
    List<String> exclusive = new ArrayList<>();
    for (Map.Entry<String, Integer> e : otherPool.entrySet()) {
      PoeCraftDataService.CraftMod m = data.mod(e.getKey());
      if (!base.containsKey(e.getKey()) && m != null && m.l() <= item.ilvl() && e.getValue() > 0) {
        exclusive.add(e.getKey());
      }
    }
    String srcId = otherMod;
    if (srcId != null && !exclusive.contains(srcId)) {
      return Result.fail(item, "없앨 아이템의 영향력 옵션이 아닙니다");
    }
    if (srcId == null && !exclusive.isEmpty()) {
      double total = 0;
      for (String id : exclusive) {
        total += otherPool.get(id);
      }
      double r = rng.nextDouble() * total;
      srcId = exclusive.get(exclusive.size() - 1);
      for (String id : exclusive) {
        r -= otherPool.get(id);
        if (r < 0) {
          srcId = id;
          break;
        }
      }
    }
    String both = mine.compareTo(other) < 0 ? mine + "," + other : other + "," + mine;
    Item dual = item.withInfluence(both);
    List<Affix> start = new ArrayList<>();
    for (Affix a : item.affixes()) {
      if (a.fractured()) {
        start.add(a);
      }
    }
    List<Affix> mineInf =
        item.affixes().stream().filter(a -> !a.fractured() && influenced(item, a)).toList();
    List<Affix> keepPair = new ArrayList<>();
    if (!mineInf.isEmpty()) {
      keepPair.add(mineInf.get(rng.nextInt(mineInf.size())));
    }
    if (srcId != null) {
      PoeCraftDataService.CraftMod m = data.mod(srcId);
      keepPair.add(new Affix(srcId, m.g(), m.grp() == null ? List.of() : m.grp(), false, false));
    }
    if (keepPair.size() == 2 && sharesGroup(keepPair.get(0), keepPair.get(1))) {
      keepPair.remove(rng.nextInt(2));
    }
    for (Affix a : keepPair) {
      if (start.stream().noneMatch(x -> sharesGroup(x, a))
          && start.stream().filter(x -> x.gen().equals(a.gen())).count()
              < maxPerGen(item, Rarity.RARE)) {
        start.add(a);
      }
    }
    // 새 아이템(C383 검토) — 원래 아이템의 메타 잠금으로 그 쪽을 닫지 않게, 남길 옵션만 든 아이템에서 굴린다
    Result r =
        roll(
            dual.with(Rarity.RARE, start),
            Rarity.RARE,
            start,
            Math.max(rareCount(item, rng), start.size()),
            Weights.NONE,
            null,
            rng);
    if (r.error() != null) {
      return Result.fail(item, r.error());
    }
    List<String> added = new ArrayList<>();
    for (Affix a : r.item().affixes()) {
      if (!item.affixes().contains(a)) {
        added.add(a.id());
      }
    }
    List<String> removed =
        item.affixes().stream()
            .filter(a -> !r.item().affixes().contains(a))
            .map(Affix::id)
            .toList();
    return new Result(r.item(), null, added, removed);
  }

  /**
   * 옵션 직접 넣기(C348) — 이 아이템 풀(종류 · 변형 · 영향력)에 있는 옵션만, 아이템 레벨 · 같은 그룹 · 자리 규칙은 지킨다. 희귀도는 필요한 만큼
   * 올린다(일반 → 매직 → 레어). 분열 표시(:F)는 분열 베이스에서 시작하려는 것.
   */
  /**
   * 기폭제 종류 → 수치를 올리는 옵션 태그(C385, PoB Item.lua catalystTags — prefix · suffix 는 태그가 아니라 옵션 자리). 키 =
   * 화폐 Id 끝 CurrencyJewelleryQuality{…} 소문자.
   */
  static final Map<String, List<String>> CATALYST_TAGS =
      Map.ofEntries(
          Map.entry("attack", List.of("attack")),
          Map.entry("speed", List.of("speed")),
          Map.entry("suffix", List.of("suffix")),
          Map.entry("resource", List.of("life", "mana", "resource")),
          Map.entry("caster", List.of("caster")),
          Map.entry("attribute", List.of("jewellery_attribute", "attribute")),
          Map.entry("physicalchaos", List.of("physical_damage", "chaos_damage")),
          Map.entry("resistance", List.of("jewellery_resistance", "resistance")),
          Map.entry("prefix", List.of("prefix")),
          Map.entry(
              "defense",
              List.of(
                  "jewellery_defense",
                  "defences",
                  "armour",
                  "evasion",
                  "energyshield",
                  "energy_shield")),
          Map.entry("elemental", List.of("jewellery_elemental", "elemental_damage")),
          Map.entry("critical", List.of("critical")));

  /** 기폭제를 쓸 수 있는 종류(인게임 설명 "반지나 목걸이, 허리띠"). */
  static final Set<String> JEWELLERY = Set.of("Ring", "Amulet", "Belt");

  /** 최대 퀄리티(인게임 설명 "퀄리티는 최대 20%"). */
  static final int MAX_QUALITY = 20;

  /**
   * 기폭제 한 번에 붙는 퀄리티 — 인게임 설명 "아이템 레벨이 낮은 비-고유 장신구일수록 효과가 크며", 구간은 공개 안 됨 → 끝판 제작(높은 아이템 레벨)의 1%
   * 가정(화면에 밝힘).
   */
  static final int CATALYST_STEP = 1;

  /**
   * 기폭제(C385) — 같은 종류면 쌓이고(최대 20%), 다른 종류면 그 종류로 바뀌어 처음부터. 타락한 장신구는 더럽혀진 기폭제만(무작위 종류 · 1~20%, 확률
   * 비공개 → 균등 가정). note = "catalyst:종류:양:쓴 개수".
   */
  private Result catalyst(Item item, String arg, RandomGenerator rng) {
    if (!JEWELLERY.contains(item.itemClass())) {
      return Result.fail(item, "반지 · 목걸이 · 허리띠에만 쓸 수 있습니다");
    }
    String key = arg == null ? "" : arg.trim();
    int times = 1;
    int star = key.indexOf('*');
    if (star > 0) {
      try {
        times = Math.max(1, Math.min(MAX_QUALITY, Integer.parseInt(key.substring(star + 1))));
      } catch (NumberFormatException e) {
        times = 1;
      }
      key = key.substring(0, star);
    }
    if ("vaal".equals(key)) {
      // 인게임 설명 "타락한 반지, 목걸이 또는 허리띠에 무작위 유형 및 수치의 퀄리티 1개를 적용합니다"
      if (!item.corrupted()) {
        return Result.fail(item, "타락한 반지 · 목걸이 · 허리띠에만 쓸 수 있습니다");
      }
      List<String> keys = new ArrayList<>(CATALYST_TAGS.keySet());
      java.util.Collections.sort(keys);
      String k = keys.get(rng.nextInt(keys.size()));
      int q = 1 + rng.nextInt(MAX_QUALITY);
      return new Result(
          item.withQuality(k + ":" + q),
          null,
          List.of(),
          List.of(),
          "catalyst:" + k + ":" + q + ":1");
    }
    if (!CATALYST_TAGS.containsKey(key)) {
      return Result.fail(item, "알 수 없는 기폭제입니다");
    }
    if (item.corrupted()) {
      return Result.fail(item, "타락한 아이템은 다시 속성 부여 개조할 수 없습니다");
    }
    int cur = key.equals(item.qualityKey()) ? item.qualityAmount() : 0;
    if (cur >= MAX_QUALITY) {
      // 인게임 문구(ClientStrings QualityMax)
      return Result.fail(item, "아이템의 퀄리티가 이미 최대입니다.");
    }
    int used = 0;
    while (used < times && cur < MAX_QUALITY) {
      cur = Math.min(MAX_QUALITY, cur + CATALYST_STEP);
      used++;
    }
    return new Result(
        item.withQuality(key + ":" + cur),
        null,
        List.of(),
        List.of(),
        "catalyst:" + key + ":" + cur + ":" + used);
  }

  /**
   * 기폭제 퀄리티로 수치가 오르는 옵션 · 임플리싯 id(C385) — 옵션 태그나 자리(접두/접미)가 그 종류와 맞으면. 수치 = 원래 × (100 + 퀄리티) /
   * 100(PoB getCatalystScalar).
   */
  public List<String> boosted(Item item) {
    List<String> want = item.qualityKey() == null ? null : CATALYST_TAGS.get(item.qualityKey());
    if (want == null || item.qualityAmount() <= 0 || !JEWELLERY.contains(item.itemClass())) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (Affix a : item.affixes()) {
      // 위상 스킬(C388)의 "N레벨 스킬"은 수치가 아니다(C389 검토 — 우측 기폭제 20% 에 36레벨로 보였다)
      if (data.beastMod(a.id()) != null) {
        continue;
      }
      List<String> tags = affixTags(item, a);
      if (want.contains(a.gen()) || tags.stream().anyMatch(want::contains)) {
        out.add(a.id());
      }
    }
    for (Affix a : item.implicits()) {
      List<String> tags = List.of();
      if (CORRUPTED.equals(a.gen())) {
        PoeCraftDataService.VeiledMod c = data.corruptedMod(a.id());
        tags = c == null || c.t() == null ? List.of() : c.t();
      } else {
        PoeCraftDataService.EldritchMod e = data.eldritchMod(a.id());
        tags = e == null || e.t() == null ? List.of() : e.t();
      }
      if (tags.stream().anyMatch(want::contains)) {
        out.add(a.id());
      }
    }
    return out;
  }

  /**
   * 옵션 태그(C389 검토 — 풀 옵션만 봐서 제작대 · 장막 해제 · 화석 · 고양 옵션이 기폭제에서 빠졌다). 에센스 전용 옵션은 데이터에 태그가 없어 빈 목록(자리
   * 기준 좌측 · 우측만 맞는다).
   */
  private List<String> affixTags(Item item, Affix a) {
    PoeCraftDataService.CraftMod m = data.mod(a.id());
    if (m != null && m.t() != null) {
      return m.t();
    }
    if (a.crafted()) {
      PoeCraftDataService.BenchCraft b = data.bench(item.itemClass(), a.id());
      if (b != null && b.tags() != null) {
        return b.tags();
      }
    }
    for (PoeCraftDataService.VeiledMod v :
        java.util.Arrays.asList(
            data.veiledMod(a.id()), data.fossilMod(a.id()), data.elevatedMod(a.id()))) {
      if (v != null && v.t() != null) {
        return v.t();
      }
    }
    return List.of();
  }

  /** 축성된 화석(C399) — tagMult 에 이 키가 있으면 옵션 요구 레벨로 가중. */
  static final String LEVEL_BIAS = "__levelbias__";

  /** 이 키가 tagMult 에 있으면 메타 공격/주문 금지를 무시(C431 — 수확 '같은 유형 더/덜' 재련). */
  static final String IGNORE_META_BLOCK = "__ignoremetablock__";

  /** 삿된 화폐(C401) — 고레벨 가중만(축성된 화석과 같은 가중이라고 가정). */
  static final Weights FOULBORN = new Weights(Map.of(LEVEL_BIAS, 1d), null);

  /** 수확 '같은 유형 더/덜'(C383) — tagMult 에 이 키가 있으면 옵션당 한 번만 곱한다. */
  static final String ONCE = "__once__";

  /** 부패 임플리싯 하나(바알 오브 · 피얼룩 화석, C383) — 이 풀 · 아이템 레벨 안에서 가중치대로. 없으면 null. */
  private Affix corruptedImplicit(Item item, RandomGenerator rng) {
    double total = 0;
    List<String> ids = new ArrayList<>();
    List<Integer> ws = new ArrayList<>();
    for (Map.Entry<String, Integer> e : data.corruptedPool(item.poolKey()).entrySet()) {
      PoeCraftDataService.VeiledMod m = data.corruptedMod(e.getKey());
      if (m != null && m.l() <= item.ilvl() && e.getValue() > 0) {
        ids.add(e.getKey());
        ws.add(e.getValue());
        total += e.getValue();
      }
    }
    if (ids.isEmpty()) {
      return null;
    }
    double r = rng.nextDouble() * total;
    String id = ids.get(ids.size() - 1);
    for (int i = 0; i < ids.size(); i++) {
      r -= ws.get(i);
      if (r < 0) {
        id = ids.get(i);
        break;
      }
    }
    PoeCraftDataService.VeiledMod m = data.corruptedMod(id);
    return new Affix(id, CORRUPTED, m.grp() == null ? List.of() : m.grp(), false, false);
  }

  /** 바알 오브 결과 종류(C358) — 화면 문구 키. */
  public static final List<String> VAAL_OUTCOMES =
      List.of("none", "sockets", "reforge", "implicit");

  /** 바알 오브(C358) — 타락 + 결과 넷 중 하나(균등). note = "vaal:결과[:사유]". */
  private Result vaal(Item item, List<Affix> keep, RandomGenerator rng) {
    if (item.itemClass() != null && item.itemClass().endsWith("Flask")) {
      return Result.fail(item, "플라스크에는 쓸 수 없습니다");
    }
    String outcome = VAAL_OUTCOMES.get(rng.nextInt(VAAL_OUTCOMES.size()));
    switch (outcome) {
      case "reforge" -> {
        // 레어로 다시 굴림 — 임플리싯은 그대로, 분열 · 메타 잠금 쪽은 남긴다(poedb)
        Result r = roll(item, Rarity.RARE, keep, rareCount(item, rng), Weights.NONE, null, rng);
        if (r.error() != null) {
          return new Result(item.corrupt(), null, List.of(), List.of(), "vaal:none:reforge");
        }
        return new Result(r.item().corrupt(), null, r.added(), r.removed(), "vaal:reforge");
      }
      case "implicit" -> {
        double total = 0;
        List<String> ids = new ArrayList<>();
        List<Integer> ws = new ArrayList<>();
        for (Map.Entry<String, Integer> e : data.corruptedPool(item.poolKey()).entrySet()) {
          PoeCraftDataService.VeiledMod m = data.corruptedMod(e.getKey());
          if (m != null && m.l() <= item.ilvl() && e.getValue() > 0) {
            ids.add(e.getKey());
            ws.add(e.getValue());
            total += e.getValue();
          }
        }
        if (ids.isEmpty()) {
          return new Result(item.corrupt(), null, List.of(), List.of(), "vaal:none:implicit");
        }
        double r = rng.nextDouble() * total;
        String id = ids.get(ids.size() - 1);
        for (int i = 0; i < ids.size(); i++) {
          r -= ws.get(i);
          if (r < 0) {
            id = ids.get(i);
            break;
          }
        }
        PoeCraftDataService.VeiledMod m = data.corruptedMod(id);
        // 기존 임플리싯(엘드리치 포함)은 모두 부패 임플리싯 하나로 바뀐다
        List<String> removed = item.implicits().stream().map(Affix::id).toList();
        List<Affix> imp =
            List.of(new Affix(id, CORRUPTED, m.grp() == null ? List.of() : m.grp(), false, false));
        return new Result(
            item.withImplicits(imp).corrupt(), null, List.of(id), removed, "vaal:implicit");
      }
      case "sockets" -> {
        return new Result(
            item.corrupt(),
            null,
            List.of(),
            List.of(),
            socketed(item) ? "vaal:sockets" : "vaal:none:sockets");
      }
      default -> {
        return new Result(item.corrupt(), null, List.of(), List.of(), "vaal:none");
      }
    }
  }

  /**
   * 더럽혀진 화폐(C359). note = "tainted:reforge|clear|add|remove" · "tainted:tier:올림:내림". 분열 · 변경 불가 쪽
   * 옵션은 그대로(다른 화폐와 같은 처리). 엑잘티드는 자리가 꽉 찼으면 지우기만 · 지울 게 없으면 붙이기만(가정 — 화면에 적는다).
   */
  private Result tainted(Item item, Action action, List<Affix> keep, RandomGenerator rng) {
    List<Affix> removable = item.affixes().stream().filter(a -> !keep.contains(a)).toList();
    switch (action) {
      case TAINTED_CHAOS -> {
        if (rng.nextBoolean()) {
          Result r = roll(item, Rarity.RARE, keep, rareCount(item, rng), Weights.NONE, null, rng);
          return new Result(r.item(), r.error(), r.added(), r.removed(), "tainted:reforge");
        }
        return new Result(
            item.with(Rarity.RARE, keep),
            null,
            List.of(),
            removable.stream().map(Affix::id).toList(),
            "tainted:clear");
      }
      case TAINTED_EXALT -> {
        boolean canAdd = !full(item, Rarity.RARE);
        if (!canAdd && removable.isEmpty()) {
          return Result.fail(item, "붙이거나 지울 수 있는 옵션이 없습니다");
        }
        if (canAdd && (removable.isEmpty() || rng.nextBoolean())) {
          Result r = add(item, Rarity.RARE, Weights.NONE, rng);
          if (r.error() == null || removable.isEmpty()) {
            return new Result(r.item(), r.error(), r.added(), r.removed(), "tainted:add");
          }
        }
        Affix gone = removable.get(rng.nextInt(removable.size()));
        List<Affix> out = new ArrayList<>(item.affixes());
        out.remove(gone);
        return new Result(
            item.with(Rarity.RARE, out), null, List.of(), List.of(gone.id()), "tainted:remove");
      }
      default -> {
        // 신성한 눈물방울 — 옵션마다 같은 계열 사다리(이 풀 · 아이템 레벨 안)에서 한 칸 위 또는 아래. 끝이면 그대로
        Map<String, Integer> pool = data.pool(item.poolKey());
        List<Affix> out = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        int up = 0;
        int down = 0;
        for (Affix a : item.affixes()) {
          PoeCraftDataService.CraftMod m =
              keep.contains(a) || a.crafted() ? null : data.mod(a.id());
          if (m == null) {
            out.add(a);
            continue;
          }
          List<String> ladder = new ArrayList<>();
          for (Map.Entry<String, Integer> e : pool.entrySet()) {
            PoeCraftDataService.CraftMod x = data.mod(e.getKey());
            if (x != null
                && e.getValue() > 0
                && x.l() <= item.ilvl()
                && m.g().equals(x.g())
                && java.util.Objects.equals(m.f(), x.f())) {
              ladder.add(e.getKey());
            }
          }
          if (!ladder.contains(a.id())) {
            ladder.add(a.id());
          }
          ladder.sort(
              java.util.Comparator.comparingInt((String id) -> data.mod(id).l())
                  .thenComparing(id -> id));
          int i = ladder.indexOf(a.id());
          boolean raise = rng.nextBoolean();
          int j = raise ? i + 1 : i - 1;
          if (j < 0 || j >= ladder.size()) {
            out.add(a);
            continue;
          }
          String id = ladder.get(j);
          PoeCraftDataService.CraftMod nm = data.mod(id);
          out.add(new Affix(id, nm.g(), nm.grp(), false, false));
          added.add(id);
          removed.add(a.id());
          if (raise) {
            up++;
          } else {
            down++;
          }
        }
        return new Result(
            item.with(Rarity.RARE, out), null, added, removed, "tainted:tier:" + up + ":" + down);
      }
    }
  }

  /** 홈이 있는 장비인가 — 반지 · 목걸이 · 벨트 · 화살통 · 주얼은 소켓 결과가 "변화 없음"과 같다. */
  static boolean socketed(Item item) {
    String c = item.itemClass() == null ? "" : item.itemClass();
    return !(c.equals("Amulet")
        || c.equals("Ring")
        || c.equals("Belt")
        || c.equals("Quiver")
        || jewel(c)
        || c.endsWith("Flask"));
  }

  private Result manualAdd(Item item, String arg) {
    if (arg == null || arg.isBlank()) {
      return Result.fail(item, "넣을 옵션을 고르세요");
    }
    boolean fr = arg.endsWith(":F");
    String id = fr ? arg.substring(0, arg.length() - 2) : arg;
    PoeCraftDataService.CraftMod m = data.mod(id);
    if (m == null || !data.pool(item.poolKey()).containsKey(id) || Boolean.TRUE.equals(m.ess())) {
      return Result.fail(item, "이 아이템 풀의 옵션이 아닙니다");
    }
    if (m.l() > item.ilvl()) {
      return Result.fail(item, "아이템 레벨 " + m.l() + " 이상이어야 붙는 옵션입니다");
    }
    Affix a = new Affix(id, m.g(), m.grp() == null ? List.of() : m.grp(), false, fr);
    if (item.affixes().stream().anyMatch(x -> sharesGroup(x, a))) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(a);
    long pre = out.stream().filter(x -> "prefix".equals(x.gen())).count();
    long suf = out.size() - pre;
    Rarity r = item.rarity();
    if (r == Rarity.NORMAL) {
      r = Rarity.MAGIC;
    }
    if (r == Rarity.MAGIC && (pre > 1 || suf > 1)) {
      r = Rarity.RARE;
    }
    Item probe = item.with(r, out);
    if (pre > maxPerGen(probe, r) || suf > maxPerGen(probe, r)) {
      return Result.fail(item, ("prefix".equals(a.gen()) ? "접두" : "접미") + " 자리가 없습니다");
    }
    return new Result(probe, null, List.of(id), List.of());
  }

  /** 우세 진영 — 임플리싯 등급이 높은 쪽(없는 쪽은 0), 같으면 없음(null). */
  public String dominance(Item item) {
    int ex = 0;
    int ea = 0;
    for (Affix a : item.implicits()) {
      PoeCraftDataService.EldritchMod m = data.eldritchMod(a.id());
      if (m == null) {
        continue;
      }
      if (EXARCH.equals(m.g())) {
        ex = Math.max(ex, m.tier());
      } else {
        ea = Math.max(ea, m.tier());
      }
    }
    return ex > ea ? EXARCH : ea > ex ? EATER : null;
  }

  /** 불씨 · 영액 — 그 진영 임플리싯을 화폐 등급(arg 1~4)으로 하나 붙인다(같은 진영 것은 바뀐다). */
  private Result eldritchImplicit(Item item, String side, String arg, RandomGenerator rng) {
    if (!ELDRITCH_CLASSES.contains(item.itemClass())) {
      return Result.fail(item, "갑옷 · 투구 · 장갑 · 장화에만 쓸 수 있습니다");
    }
    if (item.influence() != null && !item.influence().isEmpty()) {
      // 인게임 설명: Cannot be used on Shaper, Elder or Elderslayer influenced items(엘더학살자 = 정복자)
      return Result.fail(item, "쉐이퍼 · 엘더 · 엘더슬레이어 영향력 아이템에는 쓸 수 없습니다");
    }
    int tier;
    try {
      tier = Integer.parseInt(arg == null ? "" : arg.trim());
    } catch (NumberFormatException e) {
      tier = 0;
    }
    if (tier < 1 || tier > 6) {
      return Result.fail(item, "화폐 등급(1 하급 ~ 4 특출난)을 고르세요");
    }
    double total = 0;
    List<String> ids = new ArrayList<>();
    List<Integer> ws = new ArrayList<>();
    for (Map.Entry<String, Integer> e : data.eldritchPool(item.poolKey()).entrySet()) {
      PoeCraftDataService.EldritchMod m = data.eldritchMod(e.getKey());
      if (m != null
          && side.equals(m.g())
          && m.tier() == tier
          // 아이템 레벨 제한 없음(C415 — maxroll "have no Item-Level restriction", 데이터의 레벨 75 는 요구 레벨이 아니다)
          && e.getValue() > 0) {
        ids.add(e.getKey());
        ws.add(e.getValue());
        total += e.getValue();
      }
    }
    if (ids.isEmpty()) {
      return Result.fail(item, "이 아이템에 붙을 수 있는 임플리싯이 없습니다(종류)");
    }
    double r = rng.nextDouble() * total;
    String id = ids.get(ids.size() - 1);
    for (int i = 0; i < ids.size(); i++) {
      r -= ws.get(i);
      if (r < 0) {
        id = ids.get(i);
        break;
      }
    }
    PoeCraftDataService.EldritchMod m = data.eldritchMod(id);
    List<Affix> imp = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    for (Affix a : item.implicits()) {
      if (side.equals(a.gen())) {
        removed.add(a.id());
      } else {
        imp.add(a);
      }
    }
    imp.add(new Affix(id, side, m.grp() == null ? List.of() : m.grp(), false, false));
    return new Result(item.withImplicits(imp), null, List.of(id), removed);
  }

  /**
   * 섬뜩한 카오스 · 엑잘 · 소멸 — 작열의 총주교 우세면 접두, 세계 포식자 우세면 접미만(poedb 화폐 설명). 카오스는 반대쪽을 잠근 카오스처럼(반대쪽은 그대로 ·
   * 새로 안 붙음 — 메타 제작 잠금과 같은 처리, 옵션 수 분포는 카오스와 같다고 가정).
   */
  private Result eldritch(Item item, Action action, List<Affix> keep, RandomGenerator rng) {
    String dom = dominance(item);
    if (dom == null) {
      return Result.fail(item, "작열의 총주교 · 세계 포식자 우세가 없습니다(임플리싯 등급이 높은 쪽이 우세)");
    }
    boolean pre = EXARCH.equals(dom);
    String gen = pre ? "prefix" : "suffix";
    switch (action) {
      case ELDRITCH_CHAOS -> {
        if (item.rarity() != Rarity.RARE) {
          return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        // 잠금 무시(C430 — CoE 표 섬뜩한 카오스 접두/접미 잠금 'NO'): 우세한 쪽은 분열만 남고 다시 굴린다
        List<Affix> stay = new ArrayList<>();
        for (Affix a : item.affixes()) {
          if (!gen.equals(a.gen()) || a.fractured()) {
            stay.add(a);
          }
        }
        return roll(
            item,
            Rarity.RARE,
            stay,
            // 우세한 쪽에 적어도 하나(C383 검토)
            Math.max(rareCount(item, rng), stay.size() + 1),
            Weights.NONE.closing(!pre, pre),
            null,
            rng);
      }
      case ELDRITCH_EXALT -> {
        if (item.rarity() != Rarity.RARE) {
          return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        if (item.count(gen) >= maxPerGen(item, Rarity.RARE)) {
          return Result.fail(item, (pre ? "접두" : "접미") + " 자리가 없습니다");
        }
        Affix a = pick(item, Rarity.RARE, Weights.NONE.closing(!pre, pre), rng);
        if (a == null) {
          return Result.fail(item, "붙을 수 있는 옵션이 없습니다");
        }
        List<Affix> out = new ArrayList<>(item.affixes());
        out.add(a);
        return new Result(item.with(Rarity.RARE, out), null, List.of(a.id()), List.of());
      }
      default -> {
        if (item.rarity() == Rarity.NORMAL) {
          return Result.fail(item, "매직 · 레어 아이템에만 쓸 수 있습니다");
        }
        // 공격/주문 금지 메타 = 그 옵션은 지우지 않는다(C433 검토 — CoE 'Eldritch Orb of Annulment' 공격 · 주문 YES, 소멸과 같은
        // 보호)
        Meta em = meta(item.affixes());
        List<Affix> removable =
            item.affixes().stream()
                .filter(a -> gen.equals(a.gen()) && !keep.contains(a))
                .filter(
                    a ->
                        !(em.noAttack() && affixTags(item, a).contains("attack"))
                            && !(em.noCaster() && affixTags(item, a).contains("caster")))
                .toList();
        if (removable.isEmpty()) {
          return Result.fail(item, (pre ? "접두" : "접미") + "에 지울 수 있는 옵션이 없습니다");
        }
        Affix gone = removable.get(rng.nextInt(removable.size()));
        List<Affix> out = new ArrayList<>(item.affixes());
        out.remove(gone);
        return new Result(item.with(item.rarity(), out), null, List.of(), List.of(gone.id()));
      }
    }
  }

  /** 장막 엑잘 — 레어의 무작위 옵션 하나(분열 · 잠긴 쪽 제외)를 같은 쪽 장막 옵션으로 바꾼다(게임 동작 replace_rare_mod_veiled). */
  private Result veiledExalt(Item item, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
    }
    if (item.affixes().stream().anyMatch(PoeCraftEngine::veiled)) {
      // 인게임 문구(ClientStrings ItemErrorHasVeiledMod, C415 — 반대쪽에 장막 옵션이 하나 더 붙었다)
      return Result.fail(item, "이미 장막 속성 부여가 있습니다.");
    }
    List<Affix> keep = kept(item);
    List<Affix> removable =
        item.affixes().stream().filter(a -> !keep.contains(a) && !veiled(a)).toList();
    if (removable.isEmpty()) {
      return Result.fail(item, "바꿀 수 있는 옵션이 없습니다");
    }
    Affix gone = removable.get(rng.nextInt(removable.size()));
    Affix v = veil(gone.gen());
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(out.indexOf(gone), v);
    return new Result(item.with(item.rarity(), out), null, List.of(v.id()), List.of(gone.id()));
  }

  /**
   * 해제 제시 셋 — 첫 장막 옵션 쪽의 해제 옵션(아이템 레벨 · 다른 옵션 그룹)에서 가중치로 서로 다른 셋. 아이템 상태(+ 해제 씨앗)로 정해진 난수라 다시 열어도
   * 같다(게임처럼 고를 때까지 바뀌지 않음). 해제 씨앗이 바뀌면(에뮬레이터 되돌리기, C483) 다른 셋.
   */
  public List<String> unveilOffers(Item item) {
    Affix v = item.affixes().stream().filter(PoeCraftEngine::veiled).findFirst().orElse(null);
    if (v == null) {
      return List.of();
    }
    Map<String, Integer> pool = data.veiledPool(item.poolKey());
    Set<Integer> taken = new HashSet<>();
    item.affixes().stream().filter(a -> a != v).forEach(a -> taken.addAll(a.groups()));
    List<String> ids = new ArrayList<>();
    List<Integer> ws = new ArrayList<>();
    for (Map.Entry<String, Integer> e : new java.util.TreeMap<>(pool).entrySet()) {
      PoeCraftDataService.VeiledMod m = data.veiledMod(e.getKey());
      if (m == null
          || e.getValue() <= 0
          || !v.gen().equals(m.g())
          || m.l() > item.ilvl()
          || m.grp().stream().anyMatch(taken::contains)) {
        continue;
      }
      ids.add(e.getKey());
      ws.add(e.getValue());
    }
    StringBuilder seed = new StringBuilder(item.poolKey()).append(item.ilvl());
    item.affixes().stream().map(Affix::id).sorted().forEach(seed::append);
    long mix = seed.toString().hashCode();
    if (item.veilSeed() != null) {
      mix ^= item.veilSeed() * 0x9E3779B97F4A7C15L;
    }
    RandomGenerator rng = new java.util.SplittableRandom(mix);
    List<String> out = new ArrayList<>();
    while (out.size() < 3 && !ids.isEmpty()) {
      long total = 0;
      for (int w : ws) {
        total += w;
      }
      long r = rng.nextLong(total);
      for (int i = 0; i < ids.size(); i++) {
        r -= ws.get(i);
        if (r < 0) {
          out.add(ids.remove(i));
          ws.remove(i);
          break;
        }
      }
    }
    return out;
  }

  private Result unveil(Item item, String choice) {
    Affix v = item.affixes().stream().filter(PoeCraftEngine::veiled).findFirst().orElse(null);
    if (v == null) {
      return Result.fail(item, "장막 옵션이 없습니다");
    }
    List<String> offers = unveilOffers(item);
    if (choice == null || !offers.contains(choice)) {
      return Result.fail(item, offers.isEmpty() ? "해제할 수 있는 옵션이 없습니다" : "제시된 셋 중 하나를 고르세요");
    }
    PoeCraftDataService.VeiledMod m = data.veiledMod(choice);
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(
        out.indexOf(v),
        new Affix(choice, m.g(), m.grp() == null ? List.of() : m.grp(), false, false));
    return new Result(item.with(item.rarity(), out), null, List.of(choice), List.of(v.id()));
  }

  /** 분열 오브 — 옵션 4개 이상 레어, 분열 옵션이 없어야. 제작대가 아닌 옵션 하나를 무작위로 고정. */
  private Result fracture(Item item, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE || item.affixes().size() < 4) {
      return Result.fail(item, "옵션이 4개 이상인 레어에만 쓸 수 있습니다");
    }
    if (item.affixes().stream().anyMatch(Affix::fractured)) {
      return Result.fail(item, "이미 분열된 아이템입니다");
    }
    // 게임 화폐 설명(CurrencyItems.Directions): "Cannot be used on Influenced or Fractured items" — 영향력
    // 아이템도 안 된다(10-15 C340)
    if (item.influence() != null && !item.influence().isEmpty()) {
      return Result.fail(item, "영향력이 있는 아이템에는 쓸 수 없습니다");
    }
    List<Affix> cand = item.affixes().stream().filter(a -> !a.crafted() && !veiled(a)).toList();
    if (cand.isEmpty()) {
      return Result.fail(item, "고정할 옵션이 없습니다");
    }
    Affix pickA = cand.get(rng.nextInt(cand.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(out.indexOf(pickA), new Affix(pickA.id(), pickA.gen(), pickA.groups(), false, true));
    return new Result(item.with(Rarity.RARE, out), null, List.of(pickA.id()), List.of());
  }

  private static boolean sharesGroup(Affix a, Affix b) {
    for (Integer g : a.groups()) {
      if (b.groups().contains(g)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 풀 색인(10-15 C336 — 계산기가 수십만 번 굴린다): 옵션마다 맵 조회 · 스트림을 돌던 것을 배열 한 번 훑기로. 데이터가 다시 읽히면(풀 맵이 바뀌면) 새로
   * 만든다. tags = 옵션 태그(화석 · 수확 · 메타 제작).
   */
  private record PoolIndex(
      Map<String, Integer> source,
      String[] ids,
      int[] weights,
      boolean[] prefix,
      int[] ilvl,
      int[][] groups,
      List<Integer>[] groupLists,
      String[][] tags) {}

  private final java.util.concurrent.ConcurrentHashMap<String, PoolIndex> indexes =
      new java.util.concurrent.ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  private PoolIndex index(String poolKey) {
    Map<String, Integer> pool = data.pool(poolKey);
    PoolIndex cached = indexes.get(poolKey);
    if (cached != null && cached.source() == pool) {
      return cached;
    }
    List<String> ids = new ArrayList<>();
    for (Map.Entry<String, Integer> e : pool.entrySet()) {
      PoeCraftDataService.CraftMod m = data.mod(e.getKey());
      if (m != null && e.getValue() > 0 && !Boolean.TRUE.equals(m.ess())) {
        ids.add(e.getKey());
      }
    }
    int n = ids.size();
    String[] idArr = ids.toArray(new String[0]);
    int[] w = new int[n];
    boolean[] pre = new boolean[n];
    int[] lv = new int[n];
    int[][] grp = new int[n][];
    List<Integer>[] grpList = new List[n];
    String[][] tg = new String[n][];
    for (int i = 0; i < n; i++) {
      PoeCraftDataService.CraftMod m = data.mod(idArr[i]);
      w[i] = pool.get(idArr[i]);
      pre[i] = "prefix".equals(m.g());
      lv[i] = m.l();
      grpList[i] = m.grp() == null ? List.of() : m.grp();
      grp[i] = grpList[i].stream().mapToInt(Integer::intValue).toArray();
      tg[i] = m.t() == null ? new String[0] : m.t().toArray(new String[0]);
    }
    PoolIndex px = new PoolIndex(pool, idArr, w, pre, lv, grp, grpList, tg);
    indexes.put(poolKey, px);
    return px;
  }

  /** 지금 붙을 수 있으면 true — 아이템 레벨 · 자리 · 이미 있는 옵션 그룹. */
  private static boolean open(
      PoolIndex px,
      int i,
      int ilvl,
      boolean prefixOpen,
      boolean suffixOpen,
      int[] taken,
      int takenN) {
    if (px.ilvl()[i] > ilvl || (px.prefix()[i] ? !prefixOpen : !suffixOpen)) {
      return false;
    }
    for (int g : px.groups()[i]) {
      for (int k = 0; k < takenN; k++) {
        if (taken[k] == g) {
          return false;
        }
      }
    }
    return true;
  }

  /** 아이템의 옵션 그룹 — [0] 에 개수. */
  private static int[] takenGroups(Item item) {
    int n = 0;
    for (Affix a : item.affixes()) {
      n += a.groups().size();
    }
    int[] out = new int[n + 1];
    int k = 1;
    for (Affix a : item.affixes()) {
      for (Integer g : a.groups()) {
        out[k++] = g;
      }
    }
    out[0] = n;
    return out;
  }

  /** 보정이 없어 factor 가 늘 1 인가(태그 배수 · 한정 · 보장 없음, 메타 공격/주문 금지 없음). */
  static boolean plain(Weights w, Meta meta) {
    // 금지 무시 표식만 있으면(C433 검토 — 'X 포함 재련' 채우기) 금지 메타와 상관없이 늘 1
    boolean ignoreOnly = w.tagMult().size() == 1 && w.tagMult().containsKey(IGNORE_META_BLOCK);
    return (w.tagMult().isEmpty() || ignoreOnly)
        && w.onlyIds() == null
        && (w.requireTags() == null || w.requireTags().isEmpty())
        && (ignoreOnly || (!meta.noAttack() && !meta.noCaster()));
  }

  /** 옵션 i 의 가중치 보정 배수 — 태그 배수 곱 · 보장 조건 · 메타 공격/주문 금지. 0 이면 못 붙는다. */
  private static double factor(PoolIndex px, int i, Weights w, Meta meta) {
    double f = factor(px.ids()[i], px.tags()[i], w, meta);
    if (f > 0 && w.tagMult().containsKey(LEVEL_BIAS)) {
      // 축성된 화석(C399) — × (1 + (요구 레벨 − 40) / 100)
      f *= Math.max(0.01, 1 + (px.ilvl()[i] - 40) / 100.0);
    }
    return f;
  }

  private static double factor(String id, String[] tags, Weights w, Meta meta) {
    if (w.onlyIds() != null && !w.onlyIds().contains(id)) {
      return 0;
    }
    Set<String> require = w.requireTags();
    if (require != null && !require.isEmpty()) {
      boolean hit = false;
      for (String t : tags) {
        if (require.contains(t)) {
          hit = true;
          break;
        }
      }
      if (!hit) {
        return 0;
      }
    }
    double f = 1;
    boolean once = w.tagMult().containsKey(ONCE);
    boolean applied = false;
    boolean ignoreBlock = w.tagMult().containsKey(IGNORE_META_BLOCK);
    for (String t : tags) {
      if (!ignoreBlock
          && ((meta.noAttack() && "attack".equals(t)) || (meta.noCaster() && "caster".equals(t)))) {
        return 0;
      }
      Double m = w.tagMult().get(t);
      if (m != null && !(once && applied)) {
        f *= m;
        applied = true;
      }
    }
    return f;
  }

  /**
   * 화석 조합 기준 첫 옵션 가중치(10-20 C504, craftofexile 의 화석 옵션 풀) — 화석은 레어로 다시 굴리므로 분열 옵션만 남긴 레어 자리에서, 화석
   * 태그 배수 · 추가 옵션 · 축성된 고레벨 가중을 얹는다. 얽혀든 화석의 무작위 태그 · 강제 옵션은 빼고(첫 옵션 확률 표용).
   */
  public Map<String, Double> fossilEligible(Item item, String slugs) {
    Map<String, Double> mult = new java.util.HashMap<>();
    Map<String, Integer> extra = new java.util.LinkedHashMap<>();
    Map<String, Integer> fpool = data.fossilPool(item.poolKey());
    for (String s : slugs == null ? new String[0] : slugs.split(",")) {
      PoeCraftDataService.Fossil f = data.fossil(s.trim());
      if (f == null) {
        continue;
      }
      for (Map.Entry<String, Double> e : f.weights().entrySet()) {
        mult.merge(e.getKey(), e.getValue(), (x, y) -> x * y);
      }
      for (String id : f.added() == null ? List.<String>of() : f.added()) {
        Integer wgt = fpool.get(id);
        if (wgt != null && wgt > 0) {
          extra.put(id, wgt);
        }
      }
      if (f.lucky()) {
        mult.put(LEVEL_BIAS, 1d);
      }
    }
    Weights w = new Weights(mult, null, null, false, false, extra.isEmpty() ? null : extra);
    List<Affix> keep = item.affixes().stream().filter(Affix::fractured).toList();
    Item base = item.with(Rarity.RARE, keep);
    PoolIndex px = index(base.poolKey());
    int max = maxPerGen(base, Rarity.RARE);
    boolean po = base.count("prefix") < max;
    boolean so = base.count("suffix") < max;
    int[] tg = takenGroups(base);
    int[] taken = java.util.Arrays.copyOfRange(tg, 1, tg.length);
    Meta meta = meta(keep);
    Map<String, Double> out = new java.util.LinkedHashMap<>();
    for (int i = 0; i < px.ids().length; i++) {
      if (open(px, i, base.ilvl(), po, so, taken, tg[0])) {
        double f = px.weights()[i] * factor(px, i, w, meta);
        if (f > 0) {
          out.put(px.ids()[i], f);
        }
      }
    }
    for (Map.Entry<String, Integer> e : extra.entrySet()) {
      PoeCraftDataService.VeiledMod m = data.fossilMod(e.getKey());
      if (m == null || m.l() > base.ilvl()) {
        continue;
      }
      Affix a = new Affix(e.getKey(), m.g(), m.grp() == null ? List.of() : m.grp(), false, false);
      if (("prefix".equals(m.g()) ? !po : !so) || keep.stream().anyMatch(x -> sharesGroup(x, a))) {
        continue;
      }
      String[] tags = m.t() == null ? new String[0] : m.t().toArray(new String[0]);
      double f = e.getValue() * factor(e.getKey(), tags, w, meta);
      if (f > 0 && w.tagMult().containsKey(LEVEL_BIAS)) {
        f *= Math.max(0.01, 1 + (m.l() - 40) / 100.0);
      }
      if (f > 0) {
        out.put(e.getKey(), f);
      }
    }
    return out;
  }

  /** 지금 붙을 수 있는 옵션 → 가중치(에뮬레이터 옵션 풀 패널 · 계산기와 같은 규칙, 메타 공격/주문 금지 포함). */
  public Map<String, Integer> eligible(Item item, Rarity rarity) {
    PoolIndex px = index(item.poolKey());
    int max = maxPerGen(item, rarity);
    int pre = (int) item.count("prefix");
    int suf = (int) item.count("suffix");
    int[] tg = takenGroups(item);
    int[] taken = java.util.Arrays.copyOfRange(tg, 1, tg.length);
    Meta meta = meta(item.affixes());
    Map<String, Integer> out = new java.util.LinkedHashMap<>();
    for (int i = 0; i < px.ids().length; i++) {
      if (open(px, i, item.ilvl(), pre < max, suf < max, taken, tg[0])
          && factor(px, i, Weights.NONE, meta) > 0) {
        out.put(px.ids()[i], px.weights()[i]);
      }
    }
    return out;
  }

  private Affix pick(Item item, Rarity rarity, Weights w, RandomGenerator rng) {
    return pick(item, rarity, w, rng, false);
  }

  /** ignoreLimit = 접두/접미 한도 무시(C392 메릭의 두카트 — "속성 개수가 최대치인 경우에도"). 그룹 · 레벨 · 가중치는 그대로. */
  private Affix pick(
      Item item, Rarity rarity, Weights w, RandomGenerator rng, boolean ignoreLimit) {
    PoolIndex px = index(item.poolKey());
    int max = ignoreLimit ? Integer.MAX_VALUE : maxPerGen(item, rarity);
    int pre = 0;
    int suf = 0;
    for (Affix a : item.affixes()) {
      if ("prefix".equals(a.gen())) {
        pre++;
      } else {
        suf++;
      }
    }
    boolean po = pre < max && !w.closePrefix();
    boolean so = suf < max && !w.closeSuffix();
    int[] tg = takenGroups(item);
    int[] taken = java.util.Arrays.copyOfRange(tg, 1, tg.length);
    Meta meta = meta(item.affixes());
    int n = px.ids().length;
    double[] eff = new double[n];
    double total = 0;
    // 보정 없는 뽑기(카오스 · 연금 · 엑잘 등, 금지 메타 없음)는 factor 가 늘 1 — 옵션마다 부르지 않는다(C432 — 계산기가 게이트 10초 제한을 넘었다)
    boolean plain = plain(w, meta);
    for (int i = 0; i < n; i++) {
      if (open(px, i, item.ilvl(), po, so, taken, tg[0])) {
        eff[i] = plain ? px.weights()[i] : px.weights()[i] * factor(px, i, w, meta);
        total += eff[i];
      }
    }
    // 화석 추가 옵션(C342) — 몇 개 안 되니 그때그때 본다
    List<Affix> exA = new ArrayList<>();
    double[] exW = new double[w.extra() == null ? 0 : w.extra().size()];
    if (w.extra() != null) {
      for (Map.Entry<String, Integer> e : w.extra().entrySet()) {
        PoeCraftDataService.VeiledMod m = data.fossilMod(e.getKey());
        if (m == null || m.l() > item.ilvl()) {
          continue;
        }
        Affix a = new Affix(e.getKey(), m.g(), m.grp() == null ? List.of() : m.grp(), false, false);
        if (("prefix".equals(m.g()) ? !po : !so)
            || item.affixes().stream().anyMatch(x -> sharesGroup(x, a))) {
          continue;
        }
        String[] tags = m.t() == null ? new String[0] : m.t().toArray(new String[0]);
        double f = e.getValue() * factor(e.getKey(), tags, w, meta);
        if (f > 0 && w.tagMult().containsKey(LEVEL_BIAS)) {
          // 축성된 화석 고레벨 가중을 추가 옵션에도(C402 검토)
          f *= Math.max(0.01, 1 + (m.l() - 40) / 100.0);
        }
        if (f > 0) {
          exW[exA.size()] = f;
          exA.add(a);
          total += f;
        }
      }
    }
    if (total <= 0) {
      return null;
    }
    double r = rng.nextDouble() * total;
    for (int k = 0; k < exA.size(); k++) {
      r -= exW[k];
      if (r < 0) {
        return exA.get(k);
      }
    }
    for (int i = 0; i < n; i++) {
      if (eff[i] > 0) {
        r -= eff[i];
        if (r < 0) {
          return new Affix(
              px.ids()[i], px.prefix()[i] ? "prefix" : "suffix", px.groupLists()[i], false, false);
        }
      }
    }
    // 부동소수 끝자리 — 마지막 후보
    for (int i = n - 1; i >= 0; i--) {
      if (eff[i] > 0) {
        return new Affix(
            px.ids()[i], px.prefix()[i] ? "prefix" : "suffix", px.groupLists()[i], false, false);
      }
    }
    return null;
  }
}
