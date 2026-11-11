package net.luversof.api.poe.poe2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * PoE2 제작 규칙(10-15 C351 — 사용자 요청 "제작 에뮬레이터 · 시뮬레이터", P5). 순수 규칙 — 데이터는 Data 로 받는다.
 *
 * <p>근거: poe2db 화폐 · 에센스 · 징조 문구(백로그 "(규칙 10-15 21:2x · 21:3x)"). PoE2 클라이언트 옵션 가중치는 0/1 뿐(GGG
 * 비공개)이라 붙을 수 있는 옵션 중 균등으로 고른다 — 화면이 그렇게 밝힌다.
 *
 * <ul>
 *   <li>진화 · 확장 · 제왕 · 엑잘 · 카오스는 등급(arg "" · greater · perfect)마다 최소 옵션 레벨(진화 · 확장 44/70, 제왕 · 엑잘 ·
 *       카오스 35/50)
 *   <li>연금 = 일반 · 매직 → 레어 4옵션, 카오스 = 하나 지우고 하나 추가, 소멸 = 하나 지움, 분열 = 레어 4옵션 이상 하나 고정
 *   <li>징조는 다음 해당 화폐 한 번에 쓰이고 사라진다(Result.omensUsed)
 * </ul>
 */
public class Poe2CraftEngine {

  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE
  }

  public enum Action {
    TRANSMUTE,
    AUGMENTATION,
    REGAL,
    ALCHEMY,
    EXALT,
    CHAOS,
    ANNUL,
    // PoE2 엔 정제의 오브가 없다
    FRACTURE,
    // 에센스(10-15 C353) — arg = 에센스 슬러그
    ESSENCE,
    // 탈신성화(C354, 심연) — DESECRATE arg = 뼈 슬러그(미공개 옵션 하나), REVEAL arg = 제시된 셋 중 고른 옵션 id
    DESECRATE,
    REVEAL,
    // 메아리 징조(C354) — 드러낼 셋을 한 번 다시
    REVEAL_REROLL,
    // 시작 아이템 직접 편집(C357 — PoE1 C348 과 같은 것, 화폐 아님): MANUAL_ADD arg = 옵션 id[:F 분열], MANUAL_REMOVE
    // arg = 옵션 id
    MANUAL_ADD,
    MANUAL_REMOVE,
    // 바알 오브(10-16 C376 — maxroll "Corruption Outcomes": 매직 · 레어 장비는 변화 없음 · 옵션 1~3개 무작위로 · 타락 인챈트 ·
    // 부위 특수(홈
    // +1 / 완드 · 지팡이 퀄리티), 확률 비공개 → 균등). 타락의 징조 = 변화 없음 제외
    VAAL,
    // 기폭제(10-16 C386 — poe2db "Adds quality that enhances X modifiers on a ring or amulet / jewel",
    // "Replaces other
    // quality types"): arg = 종류 키(주얼용 제련된 = "jewel:" 앞붙임)[*N = N번 연달아]
    CATALYST,
    // 축성(10-17 C390) — 신성한 오브 + 축성의 징조: 레어만, 이후 더 바꿀 수 없다(수치 배수는 게이트)
    SANCTIFY
  }

  /** 뼈 — classes = 쓸 수 있는 아이템 종류, maxIlvl = 아이템 레벨 상한(0 = 없음), minModLevel = 드러낼 옵션 최소 레벨. */
  public record Bone(String slug, List<String> classes, int maxIlvl, int minModLevel) {}

  /** 탈신성 옵션 — lich = ulaman | amanamu | kurgal | null. */
  public record DesecratedMod(Mod mod, String lich) {}

  /** 미공개 탈신성 자리 표시 — id = Unrevealed + P/S [+ ":" 군주], level = 드러낼 최소 레벨. */
  public static final String UNREVEALED = "Unrevealed";

  /** 심연의 군주의 징표(C416) — 심연의 에센스가 붙이는 표식(수치 없음), id = 이것 + P/S. */
  public static final String ABYSS_MARK = "AbyssalLordMark";

  public static boolean abyssMark(Affix a) {
    return a.id().startsWith(ABYSS_MARK);
  }

  public static boolean unrevealed(Affix a) {
    return a.id().startsWith(UNREVEALED);
  }

  /**
   * 에센스 — kind = normal(매직 → 레어 + 보장) | perfect · corrupted(레어에서 하나 지우고 보장), mods = 아이템 종류 → 보장 옵션
   * id.
   */
  public record Essence(String slug, String kind, java.util.Map<String, String> mods) {}

  /**
   * 옵션 — groups = 옵션 그룹(같은 그룹은 한 아이템에 하나), tags = 옵션 태그(동질화 징조), level = 옵션 레벨. type = 옵션
   * 유형(ModType — 최소 속성 레벨 예외의 '속성 유형', C418), 모르면 null(그룹으로 대신).
   */
  public record Mod(
      String id, String gen, int level, List<Integer> groups, List<String> tags, String type) {

    public Mod(String id, String gen, int level, List<Integer> groups, List<String> tags) {
      this(id, gen, level, groups, tags, null);
    }
  }

  public record Affix(String id, String gen, int level, List<Integer> groups, boolean fractured) {}

  /** 아이템 — poolKey = 클래스|변형(mods.json 풀 키). */
  public record Item(
      String poolKey,
      int ilvl,
      Rarity rarity,
      List<Affix> affixes,
      boolean corrupted,
      // 기폭제 퀄리티(C386) — "종류:양"(예: life:20), 없으면 null. 옵션을 다시 굴려도 남는다
      String quality,
      // 축성(C390) — 더 바꿀 수 없다
      boolean sanctified,
      // 공개 씨앗(C483) — 드러낼 셋을 섞는 값, 없으면 아이템 상태만으로. 에뮬레이터가 되돌릴 때 새로 줘서 다른 셋이 나온다
      Long revealSeed) {

    public Item(
        String poolKey,
        int ilvl,
        Rarity rarity,
        List<Affix> affixes,
        boolean corrupted,
        String quality,
        boolean sanctified) {
      this(poolKey, ilvl, rarity, affixes, corrupted, quality, sanctified, null);
    }

    public Item(String poolKey, int ilvl, Rarity rarity, List<Affix> affixes, boolean corrupted) {
      this(poolKey, ilvl, rarity, affixes, corrupted, null, false);
    }

    public Item(
        String poolKey,
        int ilvl,
        Rarity rarity,
        List<Affix> affixes,
        boolean corrupted,
        String quality) {
      this(poolKey, ilvl, rarity, affixes, corrupted, quality, false);
    }

    long count(String gen) {
      return affixes.stream().filter(a -> gen.equals(a.gen())).count();
    }

    Item with(Rarity r, List<Affix> a) {
      return new Item(poolKey, ilvl, r, List.copyOf(a), corrupted, quality, sanctified, revealSeed);
    }

    Item withQuality(String q) {
      return new Item(poolKey, ilvl, rarity, affixes, corrupted, q, sanctified, revealSeed);
    }

    /** 기폭제 종류 키, 없으면 null. */
    public String qualityKey() {
      int i = quality == null ? -1 : quality.lastIndexOf(':');
      return i > 0 ? quality.substring(0, i) : null;
    }

    /** 기폭제 퀄리티 양(%), 없으면 0. */
    public int qualityAmount() {
      int i = quality == null ? -1 : quality.lastIndexOf(':');
      if (i < 0) {
        return 0;
      }
      try {
        return Integer.parseInt(quality.substring(i + 1));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
  }

  public record Result(
      Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<String> omensUsed,
      String note) {

    Result(
        Item item, String error, List<String> added, List<String> removed, List<String> omensUsed) {
      this(item, error, added, removed, omensUsed, null);
    }

    static Result fail(Item item, String error) {
      return new Result(item, error, List.of(), List.of(), List.of());
    }
  }

  /** 기폭제 종류 → 수치를 올리는 옵션 태그(C386 — 설명의 "X modifiers", 갑각 = 방어도 · 회피 · 에너지 보호막). */
  public static final java.util.Map<String, List<String>> CATALYST_TAGS =
      java.util.Map.ofEntries(
          java.util.Map.entry("life", List.of("life")),
          java.util.Map.entry("mana", List.of("mana")),
          java.util.Map.entry(
              "defences", List.of("defences", "armour", "evasion", "energy_shield")),
          java.util.Map.entry("physical", List.of("physical", "physical_damage")),
          java.util.Map.entry("fire", List.of("fire")),
          java.util.Map.entry("cold", List.of("cold")),
          java.util.Map.entry("lightning", List.of("lightning")),
          java.util.Map.entry("chaos", List.of("chaos")),
          java.util.Map.entry("attack", List.of("attack")),
          java.util.Map.entry("caster", List.of("caster")),
          java.util.Map.entry("speed", List.of("speed")),
          java.util.Map.entry("attribute", List.of("attribute")),
          java.util.Map.entry("minion", List.of("minion")));

  /** 최대 퀄리티 20%(PoE1 기폭제와 같다고 가정). */
  public static final int MAX_QUALITY = 20;

  /** 기폭제 한 번에 붙는 퀄리티 — 공개 안 됨 → 높은 아이템 레벨 기준 1% 가정(PoE1 C385 와 같음, 화면에 밝힘). */
  static final int CATALYST_STEP = 1;

  /** 장신구(반지 · 목걸이) 풀인가 — 보통 기폭제 대상. */
  static boolean jewellery(String poolKey) {
    return poolKey != null && (poolKey.startsWith("Ring|") || poolKey.startsWith("Amulet|"));
  }

  /** 종류 키(jewel: 앞붙임 포함)가 이 아이템에 맞는가. */
  public static boolean catalystFits(String poolKey, String key) {
    if (key == null) {
      return false;
    }
    boolean j = key.startsWith("jewel:");
    String k = j ? key.substring("jewel:".length()) : key;
    return CATALYST_TAGS.containsKey(k) && (j ? jewel(poolKey) : jewellery(poolKey));
  }

  /**
   * 기폭제(C386) — 같은 종류면 쌓이고(최대 20%), 다른 종류면 그 종류로 바뀌어 처음부터(설명 "기존 퀄리티 유형을 대체합니다"). note =
   * "catalyst:종류:양:쓴 개수". 타락한 아이템은 apply 첫머리에서 막힌다.
   */
  private Result catalyst(Item item, String arg) {
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
    String base = key.startsWith("jewel:") ? key.substring("jewel:".length()) : key;
    if (!CATALYST_TAGS.containsKey(base)) {
      return Result.fail(item, "알 수 없는 기폭제입니다");
    }
    if (!catalystFits(item.poolKey(), key)) {
      return Result.fail(item, key.startsWith("jewel:") ? "주얼에만 쓸 수 있습니다" : "반지나 목걸이에만 쓸 수 있습니다");
    }
    int cur = key.equals(item.qualityKey()) ? item.qualityAmount() : 0;
    if (cur >= MAX_QUALITY) {
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
        List.of(),
        "catalyst:" + key + ":" + cur + ":" + used);
  }

  /** 기폭제 퀄리티로 수치가 오르는 옵션 id(C386) — 옵션 태그가 그 종류와 맞으면(수치 × (100 + 퀄리티) / 100 은 화면이). */
  public List<String> boosted(Item item) {
    String k = item.qualityKey();
    if (k == null || item.qualityAmount() <= 0 || !catalystFits(item.poolKey(), k)) {
      return List.of();
    }
    List<String> want = CATALYST_TAGS.get(k.startsWith("jewel:") ? k.substring(6) : k);
    List<String> out = new ArrayList<>();
    for (Affix a : item.affixes()) {
      Mod m = data.mod(a.id());
      if (m != null && m.tags() != null && m.tags().stream().anyMatch(want::contains)) {
        out.add(a.id());
      }
    }
    return out;
  }

  public interface Data {
    List<Mod> pool(String poolKey);

    Mod mod(String id);

    default Essence essence(String slug) {
      return null;
    }

    default Bone bone(String slug) {
      return null;
    }

    /** 이 풀의 탈신성 옵션(C354). */
    default List<DesecratedMod> desecrated(String poolKey) {
      return List.of();
    }

    /** 이 풀의 바알 오브 타락 인챈트(C376). */
    default List<Mod> corruptedPool(String poolKey) {
      return List.of();
    }
  }

  /** 징조 키(BaseItemTypes "Omen of …" 의 뒤) — 제작에 쓰이는 것. */
  public static final List<String> OMENS =
      List.of(
          "SinistralExaltation",
          "DextralExaltation",
          "GreaterExaltation",
          "HomogenisingExaltation",
          "SinistralAnnulment",
          "DextralAnnulment",
          "GreaterAnnulment",
          "Whittling",
          "SinistralErasure",
          "DextralErasure",
          "SinistralAlchemy",
          "DextralAlchemy",
          "SinistralCoronation",
          "DextralCoronation",
          "HomogenisingCoronation",
          "SinistralCrystallisation",
          "DextralCrystallisation",
          "SinistralNecromancy",
          "DextralNecromancy",
          "Sovereign",
          "Liege",
          "Blackblooded",
          "Light",
          "AbyssalEchoes",
          "Putrefaction",
          // 타락의 징조(C376) — 다음 바알 오브의 "변화 없음" 결과를 없앤다
          "Corruption",
          // 촉진하는 찬미의 징조(C386) — 다음 엑잘이 기폭제 퀄리티를 모두 소모해 같은 종류 옵션 가중치를 올린다
          "CatalysingExaltation");

  private final Data data;

  public Poe2CraftEngine(Data data) {
    this.data = data;
  }

  /**
   * 재조합기(10-17 C407) — 같은 종류(클래스) 매직 · 레어 둘에서 고른 옵션으로 레어 하나. 인게임 규칙(ClientStrings
   * RecombinatorInfoHover · RecombinatorUI*): 바탕(종류 · 아이템 레벨 · 기폭제 퀄리티)은 둘 중 무작위 · 실패해도 둘 다 소모 ·
   * 접두/접미 한도 · 겹치는 옵션 불가 · 훼손 옵션 1개. 양쪽에서 하나 이상 · 전체 6개까지, 축성 · 타락 · 주얼은 못 씀(커뮤니티 설명 · poe2db). 성공
   * 확률 공식은 공개되지 않아(인게임 화면에 표시) chance(1~100)를 받는다 — lucky = 재조합의 징조(두 번 굴려 좋은 쪽). 바탕 쪽 분열 옵션은 고르지
   * 않아도 남는다(커뮤니티 설명) — 그 때문에 한도 · 겹침 · 훼손 1개를 넘는 다른 쪽 고른 옵션은 빠진다(가정, note 에 밝힘). 다른 쪽에서 고른 분열 옵션은
   * 분열 표시째 옮긴다(가정 — 인게임 근거 없음). 실패면 item = 빈 일반 아이템(왼쪽 종류)과 note "recombine:fail:확률".
   */
  public Result recombine(
      Item left,
      Item right,
      Set<String> leftIds,
      Set<String> rightIds,
      int chance,
      boolean lucky,
      RandomGenerator rng) {
    if (left == null || right == null) {
      return Result.fail(left, "재료로 쓸 아이템이 둘 있어야 합니다");
    }
    for (Item it : List.of(left, right)) {
      if (it.corrupted()
          || it.sanctified()
          || (it.rarity() != Rarity.MAGIC && it.rarity() != Rarity.RARE)) {
        // 인게임 문구(ClientStrings ItemErrorNotRecombinable)
        return Result.fail(left, "아이템을 재조합기에서 사용할 수 없습니다");
      }
    }
    if (!itemClass(left.poolKey()).equals(itemClass(right.poolKey()))) {
      return Result.fail(left, "같은 종류의 아이템만 재조합할 수 있습니다");
    }
    // 주얼은 대상이 아니다(C434 — poe2db Recombinator "weapons, armour, or jewellery of the same item
    // class")
    if (jewel(left.poolKey())) {
      return Result.fail(left, "아이템을 재조합기에서 사용할 수 없습니다");
    }
    List<Affix> l = picked(left, leftIds);
    List<Affix> r = picked(right, rightIds);
    if (l == null || r == null) {
      // 인게임 문구(RecombinatorUIModUnselectable) — 미공개 훼손 자리 · 타락 인챈트 · 없는 옵션
      return Result.fail(left, "재조합에 선택할 수 없는 속성");
    }
    if (l.isEmpty() || r.isEmpty()) {
      return Result.fail(left, "두 아이템에서 각각 속성을 하나 이상 골라야 합니다");
    }
    int per = maxPerGen(left, Rarity.RARE);
    List<Affix> sel = new ArrayList<>(l);
    sel.addAll(r);
    if (sel.size() > per * 2) {
      return Result.fail(left, "속성 한도 도달");
    }
    if (sel.stream().filter(a -> "prefix".equals(a.gen())).count() > per) {
      return Result.fail(left, "접두어 속성 한도 도달");
    }
    if (sel.stream().filter(a -> "suffix".equals(a.gen())).count() > per) {
      return Result.fail(left, "접미어 속성 한도 도달");
    }
    for (int i = 0; i < sel.size(); i++) {
      for (int j = i + 1; j < sel.size(); j++) {
        if (sel.get(i).groups().stream().anyMatch(sel.get(j).groups()::contains)) {
          return Result.fail(left, "이 속성은 다른 고른 속성과 호환되지 않습니다");
        }
      }
    }
    long dese =
        l.stream().filter(a -> isDesecrated(left, a)).count()
            + r.stream().filter(a -> isDesecrated(right, a)).count();
    if (dese > 1) {
      // 인게임 문구(RecombinatorErrorTooManyAbyssMods)
      return Result.fail(left, "훼손된 속성은 1개만 선택할 수 있습니다.");
    }
    int p = Math.max(1, Math.min(100, chance));
    boolean ok = rng.nextInt(100) < p || (lucky && rng.nextInt(100) < p);
    boolean keepLeft = rng.nextBoolean();
    List<String> used = lucky ? List.of("Recombination") : List.of();
    List<String> removed = new ArrayList<>();
    left.affixes().forEach(a -> removed.add(a.id()));
    right.affixes().forEach(a -> removed.add(a.id()));
    if (!ok) {
      Item gone = new Item(left.poolKey(), left.ilvl(), Rarity.NORMAL, List.of(), false);
      return new Result(gone, null, List.of(), removed, used, "recombine:fail:" + p);
    }
    Item base = keepLeft ? left : right;
    List<Affix> own = keepLeft ? l : r;
    List<Affix> other = keepLeft ? r : l;
    List<Affix> out = new ArrayList<>(own);
    // 바탕 쪽 분열 옵션은 고르지 않아도 남는다
    for (Affix a : base.affixes()) {
      if (a.fractured() && !out.contains(a)) {
        out.add(a);
      }
    }
    int dropped = 0;
    Item otherItem = keepLeft ? right : left;
    for (Affix a : other) {
      boolean clash =
          out.stream().anyMatch(x -> x.groups().stream().anyMatch(a.groups()::contains));
      boolean full = out.stream().filter(x -> x.gen().equals(a.gen())).count() >= per;
      // 훼손 옵션은 하나만(C407 검토 — 바탕 쪽 분열 훼손 옵션 + 다른 쪽에서 고른 훼손 옵션이 둘 다 붙었다)
      boolean secondDese =
          isDesecrated(otherItem, a) && out.stream().anyMatch(x -> isDesecrated(base, x));
      if (clash || full || secondDese) {
        dropped++;
      } else {
        out.add(a);
      }
    }
    Item made =
        new Item(base.poolKey(), base.ilvl(), Rarity.RARE, List.copyOf(out), false, base.quality());
    List<String> added = out.stream().map(Affix::id).toList();
    return new Result(
        made,
        null,
        added,
        removed,
        used,
        "recombine:ok:" + (keepLeft ? "L" : "R") + ":" + p + (dropped > 0 ? ":" + dropped : ""));
  }

  /** ids 에 맞는 옵션들(그 아이템에 있는 순서), 고를 수 없는 것(미공개 훼손 · 타락 인챈트 · 없는 id)이 있으면 null. */
  private static List<Affix> picked(Item item, Set<String> ids) {
    if (ids == null) {
      return List.of();
    }
    List<Affix> out = new ArrayList<>();
    for (String id : ids) {
      Affix a = item.affixes().stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
      if (a == null || unrevealed(a) || "corrupted".equals(a.gen())) {
        return null;
      }
      out.add(a);
    }
    out.sort(Comparator.comparingInt(item.affixes()::indexOf));
    return out;
  }

  /** 풀 키 앞쪽(클래스) — "Gloves|Armour" → "Gloves". */
  static String itemClass(String poolKey) {
    int i = poolKey == null ? -1 : poolKey.indexOf('|');
    return i < 0 ? String.valueOf(poolKey) : poolKey.substring(0, i);
  }

  static boolean jewel(String poolKey) {
    return poolKey != null && poolKey.startsWith("Jewel");
  }

  static int maxPerGen(Item item, Rarity r) {
    if (r == Rarity.MAGIC) {
      return 1;
    }
    return jewel(item.poolKey()) ? 2 : 3;
  }

  /** 화폐 등급 최소 옵션 레벨 — 진화 · 확장 44/70, 제왕 · 엑잘 · 카오스 35/50(poe2db). */
  static int minLevel(Action action, String tier) {
    boolean g = "greater".equalsIgnoreCase(tier);
    boolean p = "perfect".equalsIgnoreCase(tier);
    if (!g && !p) {
      return 0;
    }
    return switch (action) {
      case TRANSMUTE, AUGMENTATION -> p ? 70 : 44;
      case REGAL, EXALT, CHAOS -> p ? 50 : 35;
      default -> 0;
    };
  }

  public Result apply(
      Item item, Action action, String arg, Set<String> omens, RandomGenerator rng) {
    // 드러내기는 타락(부패의 징조) 아이템에도 — 부패는 미공개 옵션을 남기고 타락시킨다
    if (item.sanctified() && action != Action.MANUAL_ADD && action != Action.MANUAL_REMOVE) {
      // 인게임 문구(ClientStrings ItemErrorTargetSanctified, C390)
      return Result.fail(item, "아이템이 축성됨");
    }
    if (item.corrupted() && action != Action.REVEAL && action != Action.REVEAL_REROLL) {
      return Result.fail(item, "타락한 아이템은 바꿀 수 없습니다");
    }
    Set<String> om = omens == null ? Set.of() : omens;
    int min = minLevel(action, arg);
    if (min > 0 && item.ilvl() < min) {
      // 최소 속성 레벨 화폐는 아이템 레벨이 그보다 낮으면 못 쓴다(C414 — KeywordPopups 같은 문구, 인게임 오류
      // ItemErrorItemLevelTooLow)
      return Result.fail(item, "아이템 레벨이 너무 낮습니다");
    }
    // 같은 화폐의 좌 · 우 징조를 둘 다 걸면 제작이 막힌다(C437 — poe2wiki Omens "prevent the craft if … invalid
    // combinations")
    String pairKind =
        switch (action) {
          case REGAL -> "Coronation";
          case EXALT -> "Exaltation";
          case ALCHEMY -> "Alchemy";
          case CHAOS -> "Erasure";
          case ANNUL -> "Annulment";
          case DESECRATE -> "Necromancy";
          default -> null;
        };
    if (pairKind != null && omenConflict(om, pairKind)) {
      return Result.fail(item, INVALID_OMENS);
    }
    return switch (action) {
      case TRANSMUTE -> {
        if (item.rarity() != Rarity.NORMAL) {
          yield Result.fail(item, "일반 아이템에만 쓸 수 있습니다");
        }
        yield addMods(item, Rarity.MAGIC, 1, min, null, false, rng, List.of());
      }
      case AUGMENTATION -> {
        if (item.rarity() != Rarity.MAGIC) {
          yield Result.fail(item, "매직 아이템에만 쓸 수 있습니다");
        }
        yield addMods(item, Rarity.MAGIC, 1, min, null, false, rng, List.of());
      }
      case REGAL -> {
        if (item.rarity() != Rarity.MAGIC) {
          yield Result.fail(item, "매직 아이템에만 쓸 수 있습니다");
        }
        String side = side(om, "Coronation");
        boolean homo = om.contains("HomogenisingCoronation");
        yield addMods(
            item,
            Rarity.RARE,
            1,
            min,
            side,
            homo,
            rng,
            used(om, "SinistralCoronation", "DextralCoronation", "HomogenisingCoronation"));
      }
      case EXALT -> {
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        String side = side(om, "Exaltation");
        int n = om.contains("GreaterExaltation") ? 2 : 1;
        List<String> usedOm =
            new ArrayList<>(
                used(
                    om,
                    "SinistralExaltation",
                    "DextralExaltation",
                    "GreaterExaltation",
                    "HomogenisingExaltation"));
        // 촉진하는 찬미(C386) — 맞는 기폭제 퀄리티가 있을 때만 쓰인다(없으면 징조는 남는다)
        boolean cat =
            om.contains("CatalysingExaltation")
                && item.qualityAmount() > 0
                && catalystFits(item.poolKey(), item.qualityKey());
        if (cat) {
          usedOm.add("CatalysingExaltation");
        }
        Result r =
            addMods(
                item,
                Rarity.RARE,
                n,
                min,
                side,
                om.contains("HomogenisingExaltation"),
                rng,
                usedOm,
                cat ? catalystTags(item.qualityKey()) : null,
                cat ? catalysingMult(item.qualityAmount()) : 1);
        yield cat && r.error() == null
            ? new Result(
                r.item().withQuality(null),
                null,
                r.added(),
                r.removed(),
                r.omensUsed(),
                "catalysing:" + item.qualityKey() + ":" + item.qualityAmount())
            : r;
      }
      case ALCHEMY -> alchemy(item, om, rng);
      case CHAOS -> chaos(item, min, om, rng);
      case ANNUL -> annul(item, om, rng);
      case ESSENCE -> essence(item, arg, om, rng);
      case DESECRATE -> desecrate(item, arg, om, rng);
      case REVEAL -> reveal(item, arg);
      case MANUAL_ADD -> manualAdd(item, arg);
      case VAAL -> vaal(item, om, rng);
      case CATALYST -> catalyst(item, arg);
      case SANCTIFY -> {
        // 신성한 오브 + 축성의 징조(C390) — "레어 아이템에 쓴 다음 신성한 오브가 축성"
        if (item.rarity() != Rarity.RARE) {
          yield Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
        }
        if (item.affixes().isEmpty()) {
          yield Result.fail(item, "옵션이 없습니다");
        }
        yield new Result(
            new Item(
                item.poolKey(),
                item.ilvl(),
                item.rarity(),
                item.affixes(),
                false,
                item.quality(),
                true),
            null,
            List.of(),
            List.of(),
            om.contains("Sanctification") ? List.of("Sanctification") : List.of(),
            "sanctify");
      }
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
            List.of(gone.id()),
            List.of());
      }
      case REVEAL_REROLL -> {
        Affix u =
            item.affixes().stream().filter(Poe2CraftEngine::unrevealed).findFirst().orElse(null);
        if (u == null) {
          yield Result.fail(item, "드러낼 훼손된 속성이 없습니다");
        }
        if (!om.contains("AbyssalEchoes")) {
          yield Result.fail(item, "심연의 메아리의 징조가 있어야 셋을 다시 볼 수 있습니다");
        }
        if (u.id().contains("~")) {
          yield Result.fail(item, "셋은 한 번만 다시 볼 수 있습니다");
        }
        List<Affix> out = new ArrayList<>(item.affixes());
        Affix re = new Affix(u.id() + "~", u.gen(), u.level(), u.groups(), false);
        out.set(out.indexOf(u), re);
        yield new Result(
            item.with(item.rarity(), out), null, List.of(), List.of(), List.of("AbyssalEchoes"));
      }
      case FRACTURE -> {
        // 훼손 옵션(미공개 · 드러난)도 4개 셈엔 든다 — 분열은 안 됨(C434 — timesaver.gg 분열 가이드 0.5.4 "cannot be
        // fractured — but they
        // still count toward the 4-modifier minimum", 수순 "desecrate without unveiling"). 10-16 검토의
        // '미공개는 안 셈'은 근거 없었다
        // 심연의 군주의 징표도 자리를 차지하는 옵션이라 셈에 든다(가정 — C436 검토, 근거는 훼손 옵션만 말함)
        if (item.rarity() != Rarity.RARE || item.affixes().size() < 4) {
          yield Result.fail(item, "옵션이 4개 이상인 레어에만 쓸 수 있습니다");
        }
        if (item.affixes().stream().anyMatch(Affix::fractured)) {
          yield Result.fail(item, "이미 분열된 아이템입니다");
        }
        // 심연의 군주의 징표는 분열하지 않는다(C418 검토 — 분열된 징표가 다음 요청에서 풀렸다)
        List<Affix> fc =
            item.affixes().stream().filter(x -> !isDesecrated(item, x) && !abyssMark(x)).toList();
        if (fc.isEmpty()) {
          yield Result.fail(item, "분열할 수 있는 옵션이 없습니다(훼손 옵션 · 징표는 분열되지 않음)");
        }
        Affix a = fc.get(rng.nextInt(fc.size()));
        List<Affix> out = new ArrayList<>(item.affixes());
        out.set(out.indexOf(a), new Affix(a.id(), a.gen(), a.level(), a.groups(), true));
        yield new Result(item.with(Rarity.RARE, out), null, List.of(a.id()), List.of(), List.of());
      }
    };
  }

  /**
   * 에센스 — 보통(하급 · 보통 · 상위)은 매직 → 레어 + 보장 옵션, 완벽 · 타락은 레어에서 무작위 하나 지우고(결정화 징조 = 접두/접미만) 보장 옵션. 아이템
   * 종류 = 풀 키 앞부분.
   */
  private Result essence(Item item, String slug, Set<String> om, RandomGenerator rng) {
    Essence e = slug == null ? null : data.essence(slug);
    String cls = item.poolKey() == null ? "" : item.poolKey().split("[|]")[0];
    String modId = e == null || e.mods() == null ? null : e.mods().get(cls);
    if (ABYSS_MARK.equals(modId)) {
      return abyssEssence(item, om, rng);
    }
    Mod m = modId == null ? null : data.mod(modId);
    if (m == null) {
      return Result.fail(item, "이 아이템 종류에 쓸 수 없는 에센스입니다");
    }
    Affix add = new Affix(m.id(), m.gen(), m.level(), m.groups(), false);
    List<Affix> out = new ArrayList<>(item.affixes());
    List<String> removed = new ArrayList<>();
    List<String> used = List.of();
    if ("normal".equals(e.kind())) {
      if (item.rarity() != Rarity.MAGIC) {
        return Result.fail(item, "매직 아이템에만 쓸 수 있습니다(레어로 바뀌며 보장 옵션 추가)");
      }
    } else {
      if (item.rarity() != Rarity.RARE) {
        return Result.fail(item, "레어 아이템에만 쓸 수 있습니다(하나 지우고 보장 옵션 추가)");
      }
      // 결정화 징조는 완벽 · 타락 에센스만(문구 "Perfect or Corrupted Essence") — 합금(alloy)엔 안 걸린다
      boolean crystal = "perfect".equals(e.kind()) || "corrupted".equals(e.kind());
      if (crystal && omenConflict(om, "Crystallisation")) {
        return Result.fail(item, INVALID_OMENS);
      }
      String side = crystal ? side(om, "Crystallisation") : null;
      List<Affix> cand =
          out.stream()
              .filter(a -> !a.fractured() && (side == null || side.equals(a.gen())))
              .toList();
      // 지울 후보(10-16 검토 — 무작위로 지운 뒤 자리 · 그룹을 보면 같은 아이템이 운에 따라 성공/실패했다): 보장 옵션과 같은 그룹 옵션이 있으면 그것,
      // 보장 옵션 쪽이 꽉 찼으면 그 쪽, 아니면 아무거나(가정 — 게임은 보장 옵션이 들어갈 수 있게 지운다고 봄)
      List<Affix> conflict =
          cand.stream().filter(a -> a.groups().stream().anyMatch(add.groups()::contains)).toList();
      boolean sideFull = item.count(add.gen()) >= maxPerGen(item, Rarity.RARE);
      List<Affix> pick =
          !conflict.isEmpty()
              ? conflict
              : sideFull ? cand.stream().filter(a -> a.gen().equals(add.gen())).toList() : cand;
      if (pick.isEmpty()) {
        return Result.fail(item, "보장 옵션 자리를 낼 수 없습니다(분열 · 징조 쪽)");
      }
      Affix gone = pick.get(rng.nextInt(pick.size()));
      out.remove(gone);
      removed.add(gone.id());
      used = crystal ? used(om, "SinistralCrystallisation", "DextralCrystallisation") : List.of();
    }
    Item mid = item.with(Rarity.RARE, out);
    if (mid.count(add.gen()) >= maxPerGen(mid, Rarity.RARE)) {
      return Result.fail(item, ("prefix".equals(add.gen()) ? "접두" : "접미") + " 자리가 없습니다");
    }
    if (out.stream().anyMatch(a -> a.groups().stream().anyMatch(add.groups()::contains))) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    out.add(add);
    return new Result(item.with(Rarity.RARE, out), null, List.of(add.id()), removed, used);
  }

  /**
   * 심연의 에센스(C416) — 레어에서 무작위 옵션 하나(분열 제외, 결정화 징조면 그 쪽)를 지우고 그 쪽에 심연의 군주의 징표(poe2db "Removes a
   * random modifier and augments … a new guaranteed modifier"). 타락 에센스라 결정화 징조가 걸린다. 징표는 하나만(같은
   * 그룹).
   */
  private Result abyssEssence(Item item, Set<String> om, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다(하나 지우고 보장 옵션 추가)");
    }
    if (item.affixes().stream().anyMatch(Poe2CraftEngine::abyssMark)) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    if (omenConflict(om, "Crystallisation")) {
      return Result.fail(item, INVALID_OMENS);
    }
    String side = side(om, "Crystallisation");
    // 징표 쪽(C417 — C416 회귀가 잡음: 방어구는 훼손 옵션이 접미뿐인데 접두 징표면 훼손을 못 했다) = 이 부위 훼손 옵션이 있는 쪽. 한쪽뿐이면
    // 그쪽(꽉 찼으면 그쪽 옵션을 지운다), 양쪽 다면 지운 옵션 쪽(가정)
    Set<String> markSides = new HashSet<>();
    data.desecrated(item.poolKey()).forEach(d -> markSides.add(d.mod().gen()));
    String only = markSides.size() == 1 ? markSides.iterator().next() : null;
    boolean onlyFull = only != null && item.count(only) >= maxPerGen(item, Rarity.RARE);
    List<Affix> cand =
        item.affixes().stream()
            .filter(
                a ->
                    !a.fractured()
                        && !"corrupted".equals(a.gen())
                        && (side == null || side.equals(a.gen()))
                        && (!onlyFull || only.equals(a.gen())))
            .toList();
    if (cand.isEmpty()) {
      return Result.fail(item, "보장 옵션 자리를 낼 수 없습니다(분열 · 징조 쪽)");
    }
    Affix gone = cand.get(rng.nextInt(cand.size()));
    String markGen = only != null ? only : gone.gen();
    List<Affix> out = new ArrayList<>(item.affixes());
    out.remove(gone);
    Affix mark =
        new Affix(
            ABYSS_MARK + ("prefix".equals(markGen) ? "P" : "S"), markGen, 0, List.of(-4), false);
    out.add(mark);
    return new Result(
        item.with(Rarity.RARE, out),
        null,
        List.of(mark.id()),
        List.of(gone.id()),
        used(om, "SinistralCrystallisation", "DextralCrystallisation"));
  }

  /** 홈이 +1 될 수 있는 부위(C376, maxroll) — 방어구 · 방패 · 마도서 · 근접 무기(활 · 쇠뇌 포함). */
  static final Set<String> SOCKET_CLASSES =
      Set.of(
          "Helmet",
          "Gloves",
          "Boots",
          "Body Armour",
          "Shield",
          "Buckler",
          "Focus",
          "Bow",
          "Crossbow",
          "Claw",
          "Dagger",
          "Flail",
          "Spear",
          "One Hand Axe",
          "One Hand Mace",
          "One Hand Sword",
          "Two Hand Axe",
          "Two Hand Mace",
          "Two Hand Sword",
          "Warstaff");

  /** 퀄리티가 오를 수 있는 부위(C376) — 완드 · 지팡이. */
  static final Set<String> QUALITY_CLASSES = Set.of("Wand", "Staff");

  /**
   * 바알 오브(C376) — 결과 중 하나(균등, 가중치 비공개): 변화 없음(타락의 징조면 제외) · 옵션 1~3개 무작위로 바꿈(같은 쪽 · 같은 그룹 제외 · 아이템
   * 레벨 안, 분열 · 미공개는 그대로) · 타락 인챈트 하나(이 풀) · 부위 특수(홈 +1 / 퀄리티 — 이 에뮬레이터는 홈 · 퀄리티를 다루지 않아 표시만). note
   * = "vaal:none|randomize:N|enchant|socket|quality".
   */
  private Result vaal(Item item, Set<String> om, RandomGenerator rng) {
    String cls = item.poolKey() == null ? "" : item.poolKey().split("[|]")[0];
    boolean omen = om.contains("Corruption");
    List<Affix> real =
        item.affixes().stream()
            .filter(
                a ->
                    !a.fractured()
                        && !unrevealed(a)
                        && !abyssMark(a)
                        && !"corrupted".equals(a.gen()))
            .toList();
    List<Mod> enchants =
        data.corruptedPool(item.poolKey()).stream().filter(m -> m.level() <= item.ilvl()).toList();
    List<String> outcomes = new ArrayList<>();
    if (!omen) {
      outcomes.add("none");
    }
    if (!real.isEmpty()) {
      outcomes.add("randomize");
    }
    if (!enchants.isEmpty()
        && item.affixes().stream().noneMatch(a -> "corrupted".equals(a.gen()))) {
      outcomes.add("enchant");
    }
    if (SOCKET_CLASSES.contains(cls)) {
      outcomes.add("socket");
    } else if (QUALITY_CLASSES.contains(cls)) {
      outcomes.add("quality");
    } else if (jewel(item.poolKey()) && item.rarity() != Rarity.NORMAL) {
      // 매직 · 레어 주얼(C377, maxroll) — 옵션 하나 추가 또는 제거(한도 무시)
      outcomes.add("jewel");
    }
    if (outcomes.isEmpty()) {
      outcomes.add("none");
    }
    List<String> used = omen ? List.of("Corruption") : List.of();
    String o = outcomes.get(rng.nextInt(outcomes.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    List<String> added = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    String note = "vaal:" + o;
    switch (o) {
      case "randomize" -> {
        int k = Math.min(real.size(), 1 + rng.nextInt(3));
        List<Affix> pick = new ArrayList<>(real);
        java.util.Collections.shuffle(pick, new java.util.Random(rng.nextLong()));
        int changed = 0;
        // 고른 옵션들은 다시 뽑히지 않는다(C378 검토 — 앞에서 지운 옵션이 뒤에서 되살아나 '2개 바뀜'으로 셌다)
        Set<String> picked = new HashSet<>();
        pick.subList(0, k).forEach(x -> picked.add(x.id()));
        for (Affix a : pick.subList(0, k)) {
          out.remove(a);
          Set<Integer> usedGroups = new java.util.HashSet<>();
          out.forEach(x -> usedGroups.addAll(x.groups()));
          List<Mod> cand =
              data.pool(item.poolKey()).stream()
                  .filter(m -> m.gen().equals(a.gen()))
                  .filter(m -> m.level() <= item.ilvl())
                  .filter(m -> !picked.contains(m.id()))
                  .filter(m -> m.groups().stream().noneMatch(usedGroups::contains))
                  .toList();
          if (cand.isEmpty()) {
            out.add(a);
            continue;
          }
          Mod m = cand.get(rng.nextInt(cand.size()));
          out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
          added.add(m.id());
          removed.add(a.id());
          changed++;
        }
        note = "vaal:randomize:" + changed;
      }
      case "jewel" -> {
        // 반반 — 붙이기(자리 한도 무시, 같은 그룹 · 아이템 레벨 지킴) | 지우기(분열 · 미공개 제외). 못 하면 다른 쪽
        Set<Integer> usedGroups = new java.util.HashSet<>();
        out.forEach(x -> usedGroups.addAll(x.groups()));
        List<Mod> cand =
            data.pool(item.poolKey()).stream()
                .filter(m -> m.level() <= item.ilvl())
                .filter(m -> m.groups().stream().noneMatch(usedGroups::contains))
                .toList();
        boolean add = rng.nextBoolean();
        if ((add && !cand.isEmpty()) || real.isEmpty()) {
          if (!cand.isEmpty()) {
            Mod m = cand.get(rng.nextInt(cand.size()));
            out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
            added.add(m.id());
            note = "vaal:jewel:add";
          } else {
            note = "vaal:none";
          }
        } else {
          Affix gone = real.get(rng.nextInt(real.size()));
          out.remove(gone);
          removed.add(gone.id());
          note = "vaal:jewel:remove";
        }
      }
      case "enchant" -> {
        Mod m = enchants.get(rng.nextInt(enchants.size()));
        out.add(new Affix(m.id(), "corrupted", m.level(), m.groups(), false));
        added.add(m.id());
      }
      default -> {}
    }
    return new Result(
        new Item(
            item.poolKey(), item.ilvl(), item.rarity(), List.copyOf(out), true, item.quality()),
        null,
        added,
        removed,
        used,
        note);
  }

  /** 옵션 직접 넣기(C357) — 이 풀 옵션만, 아이템 레벨 · 같은 그룹 · 자리 규칙, 희귀도 자동(일반 → 매직 → 레어). */
  private Result manualAdd(Item item, String arg) {
    if (arg == null || arg.isBlank()) {
      return Result.fail(item, "넣을 옵션을 고르세요");
    }
    boolean fr = arg.endsWith(":F");
    String id = fr ? arg.substring(0, arg.length() - 2) : arg;
    Mod m =
        data.pool(item.poolKey()).stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
    if (m == null) {
      return Result.fail(item, "이 아이템 풀의 옵션이 아닙니다");
    }
    if (m.level() > item.ilvl()) {
      return Result.fail(item, "아이템 레벨 " + m.level() + " 이상이어야 붙는 옵션입니다");
    }
    if (item.affixes().stream().anyMatch(x -> x.groups().stream().anyMatch(m.groups()::contains))) {
      return Result.fail(item, "같은 그룹 옵션이 이미 있습니다");
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), fr));
    long pre = out.stream().filter(x -> "prefix".equals(x.gen())).count();
    long suf = out.size() - pre;
    Rarity r = item.rarity() == Rarity.NORMAL ? Rarity.MAGIC : item.rarity();
    if (r == Rarity.MAGIC && (pre > 1 || suf > 1)) {
      r = Rarity.RARE;
    }
    Item probe = item.with(r, out);
    if (pre > maxPerGen(probe, r) || suf > maxPerGen(probe, r)) {
      return Result.fail(item, ("prefix".equals(m.gen()) ? "접두" : "접미") + " 자리가 없습니다");
    }
    return new Result(probe, null, List.of(id), List.of(), List.of());
  }

  /** 탈신성 옵션인가(미공개 자리 표시 포함). */
  boolean isDesecrated(Item item, Affix a) {
    return unrevealed(a)
        || data.desecrated(item.poolKey()).stream().anyMatch(d -> d.mod().id().equals(a.id()));
  }

  /**
   * 탈신성화 — 레어에 미공개 탈신성 옵션 하나(poe2db). 이미 탈신성 옵션이 있으면 못 씀, 그 쪽이 꽉 차면 그 쪽 무작위 하나를 지움. 쪽 = 그 종류에 있는
   * 탈신성 옵션 쪽(갑옷류는 접미만) 중 자리 있는 쪽, 강령 징조면 그 쪽. 군주 징조(주권 = 울라만, 군주 = 아마나무, 검은피 = 쿠르갈)는 드러낼 셋을 그 군주
   * 옵션으로(가정 — 문구는 그 군주 옵션 하나 보장).
   */
  private Result desecrate(Item item, String slug, Set<String> om, RandomGenerator rng) {
    Bone b = slug == null ? null : data.bone(slug);
    String cls = item.poolKey() == null ? "" : item.poolKey().split("[|]")[0];
    if (b == null || b.classes() == null || !b.classes().contains(cls)) {
      return Result.fail(item, "이 아이템 종류에 쓸 수 없는 뼈입니다");
    }
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
    }
    if (b.maxIlvl() > 0 && item.ilvl() > b.maxIlvl()) {
      return Result.fail(item, "아이템 레벨 " + b.maxIlvl() + " 이하에만 쓸 수 있는 뼈입니다");
    }
    if (item.affixes().stream().anyMatch(a -> isDesecrated(item, a))) {
      // 인게임 문구(ClientStrings ItemErrorHasVeiledMod)
      return Result.fail(item, "이미 훼손된 속성이 있습니다.");
    }
    List<DesecratedMod> pool = data.desecrated(item.poolKey());
    // 군주 징조(C414) — 문구 "다음 무기 · 장신구 훼손"(CurrencyItems): 다른 뼈이거나 그 군주 옵션이 이 아이템 레벨에 없으면 인게임 오류(징조는
    // 남는다)
    String lichOmen =
        om.contains("Sovereign")
            ? "ulaman"
            : om.contains("Liege") ? "amanamu" : om.contains("Blackblooded") ? "kurgal" : null;
    if (lichOmen != null) {
      boolean weaponOrJewellery = slug.contains("Weapon") || slug.contains("Jewellery");
      boolean reachable =
          pool.stream().anyMatch(d -> lichOmen.equals(d.lich()) && d.mod().level() <= item.ilvl());
      if (!weaponOrJewellery || !reachable) {
        // 인게임 문구(ClientStrings ItemErrorOmenIncompatibleWithAbyssalBone)
        return Result.fail(item, "이 훼손 아이템은 징조와 호환되지 않습니다");
      }
    }
    Set<String> sides = new HashSet<>();
    for (DesecratedMod d : pool) {
      sides.add(d.mod().gen());
    }
    String want = side(om, "Necromancy");
    if (want != null) {
      sides.retainAll(Set.of(want));
    }
    if (sides.isEmpty()) {
      return Result.fail(item, "이 아이템에 붙을 훼손된 속성이 없습니다(종류 · 징조 쪽)");
    }
    int max = maxPerGen(item, Rarity.RARE);
    if (om.contains("Putrefaction")) {
      // 부패(poe2db) — 모든 옵션을 미공개 훼손 옵션으로(최대 6, 그 종류에 있는 쪽만) 바꾸고 타락
      // 분열 옵션은 그대로 남기고 남은 자리만(10-16 검토 — "분열은 안 바뀐다"와 맞춤, 게임 동작은 미확인)
      List<Affix> rot = new ArrayList<>(item.affixes().stream().filter(Affix::fractured).toList());
      List<String> added = new ArrayList<>();
      for (String g : sides.stream().sorted().toList()) {
        long kept = rot.stream().filter(a -> g.equals(a.gen())).count();
        for (int k = 0; k < max - kept; k++) {
          String id = UNREVEALED + ("prefix".equals(g) ? "P" : "S") + "#" + k;
          rot.add(new Affix(id, g, b.minModLevel(), List.of(-3 - k), false));
          added.add(id);
        }
      }
      Item corrupted =
          new Item(
              item.poolKey(), item.ilvl(), Rarity.RARE, List.copyOf(rot), true, item.quality());
      return new Result(
          corrupted,
          null,
          added,
          item.affixes().stream().filter(a -> !a.fractured()).map(Affix::id).toList(),
          used(om, "Putrefaction", "SinistralNecromancy", "DextralNecromancy"));
    }
    Affix mark =
        item.affixes().stream().filter(Poe2CraftEngine::abyssMark).findFirst().orElse(null);
    if (mark != null) {
      // 심연의 군주의 징표(C416, KeywordPopups) — 훼손하면 징표가 지워지고 그 자리(쪽)에 미공개 훼손 옵션(무작위로 지우지 않는다). '한 등급
      // 높은'은 데이터의 훼손 옵션이 한 등급(레벨 65)뿐이라 효과 없음 — 화면에 밝힘. 강령 징조는 징표 쪽과 다르면 쓰지 않는다
      if (pool.stream().noneMatch(d -> d.mod().gen().equals(mark.gen()))) {
        return Result.fail(item, "징표 쪽에 붙을 훼손된 속성이 없습니다");
      }
      String markLich =
          om.contains("Sovereign")
              ? "ulaman"
              : om.contains("Liege") ? "amanamu" : om.contains("Blackblooded") ? "kurgal" : null;
      String mid =
          UNREVEALED
              + ("prefix".equals(mark.gen()) ? "P" : "S")
              + (markLich == null ? "" : ":" + markLich);
      List<Affix> mout = new ArrayList<>(item.affixes());
      mout.set(mout.indexOf(mark), new Affix(mid, mark.gen(), b.minModLevel(), List.of(-3), false));
      List<String> mused = new ArrayList<>(used(om, "Sovereign", "Liege", "Blackblooded"));
      String want2 = side(om, "Necromancy");
      if (mark.gen().equals(want2)) {
        mused.addAll(used(om, "SinistralNecromancy", "DextralNecromancy"));
      }
      return new Result(
          item.with(Rarity.RARE, mout), null, List.of(mid), List.of(mark.id()), mused, "abyssmark");
    }
    List<String> all = sides.stream().sorted().toList();
    List<String> open = all.stream().filter(g -> item.count(g) < max).toList();
    String gen =
        open.isEmpty() ? all.get(rng.nextInt(all.size())) : open.get(rng.nextInt(open.size()));
    List<Affix> out = new ArrayList<>(item.affixes());
    List<String> removed = new ArrayList<>();
    if (item.count(gen) >= max) {
      List<Affix> cand = out.stream().filter(a -> !a.fractured() && gen.equals(a.gen())).toList();
      if (cand.isEmpty()) {
        return Result.fail(item, "그 쪽 옵션이 모두 분열이라 자리를 낼 수 없습니다");
      }
      Affix gone = cand.get(rng.nextInt(cand.size()));
      out.remove(gone);
      removed.add(gone.id());
    }
    String lich =
        om.contains("Sovereign")
            ? "ulaman"
            : om.contains("Liege") ? "amanamu" : om.contains("Blackblooded") ? "kurgal" : null;
    String id = UNREVEALED + ("prefix".equals(gen) ? "P" : "S") + (lich == null ? "" : ":" + lich);
    out.add(new Affix(id, gen, b.minModLevel(), List.of(-3), false));
    return new Result(
        item.with(Rarity.RARE, out),
        null,
        List.of(id),
        removed,
        used(om, "SinistralNecromancy", "DextralNecromancy", "Sovereign", "Liege", "Blackblooded"));
  }

  /**
   * 드러낼 셋 — 미공개 자리의 쪽 · 최소 레벨 · 군주 · 아이템 레벨 · 다른 옵션 그룹. 아이템 상태(+ 공개 씨앗)로 정해진 난수(다시 열어도 같다). 공개 씨앗이
   * 바뀌면(에뮬레이터 되돌리기, C483) 다른 셋.
   */
  public List<String> revealOffers(Item item) {
    Affix u = item.affixes().stream().filter(Poe2CraftEngine::unrevealed).findFirst().orElse(null);
    if (u == null) {
      return List.of();
    }
    String core = u.id().replace("~", "");
    String lich = core.contains(":") ? core.substring(core.indexOf(':') + 1) : null;
    Set<Integer> taken = new HashSet<>();
    for (Affix a : item.affixes()) {
      // 타락 인챈트(C376)는 옵션 그룹을 막지 않는다(C378 검토)
      if (a != u && !"corrupted".equals(a.gen())) {
        taken.addAll(a.groups());
      }
    }
    // 군주 징조인데 그 쪽 · 레벨에 그 군주 옵션이 없으면 군주 조건 없이(아이템이 드러내지도 다시 훼손하지도 못하고 막혔다, 10-16 검토)
    final String lichF = lich;
    boolean lichAvailable =
        lich != null
            && data.desecrated(item.poolKey()).stream()
                .anyMatch(
                    d ->
                        lichF.equals(d.lich())
                            && d.mod().gen().equals(u.gen())
                            && d.mod().level() <= item.ilvl()
                            && d.mod().level() >= u.level());
    List<String> cand = new ArrayList<>();
    for (DesecratedMod d : data.desecrated(item.poolKey())) {
      Mod m = d.mod();
      if (m.gen().equals(u.gen())
          && m.level() <= item.ilvl()
          && m.level() >= u.level()
          && (lich == null || lich.equals(d.lich()) || !lichAvailable)
          && m.groups().stream().noneMatch(taken::contains)) {
        cand.add(m.id());
      }
    }
    java.util.Collections.sort(cand);
    StringBuilder key = new StringBuilder(item.poolKey()).append('|').append(item.ilvl());
    item.affixes().stream().map(Affix::id).sorted().forEach(x -> key.append('|').append(x));
    long mix = key.toString().hashCode();
    if (item.revealSeed() != null) {
      mix ^= item.revealSeed() * 0x9E3779B97F4A7C15L;
    }
    java.util.Collections.shuffle(cand, new java.util.Random(mix));
    return cand.subList(0, Math.min(3, cand.size()));
  }

  private Result reveal(Item item, String choice) {
    Affix u = item.affixes().stream().filter(Poe2CraftEngine::unrevealed).findFirst().orElse(null);
    if (u == null) {
      return Result.fail(item, "드러낼 훼손된 속성이 없습니다");
    }
    List<String> offers = revealOffers(item);
    if (choice == null || !offers.contains(choice)) {
      return Result.fail(item, offers.isEmpty() ? "드러낼 수 있는 옵션이 없습니다" : "제시된 셋 중 하나를 고르세요");
    }
    Mod m =
        data.desecrated(item.poolKey()).stream()
            .map(DesecratedMod::mod)
            .filter(x -> x.id().equals(choice))
            .findFirst()
            .orElseThrow();
    List<Affix> out = new ArrayList<>(item.affixes());
    out.set(out.indexOf(u), new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
    return new Result(
        item.with(item.rarity(), out), null, List.of(m.id()), List.of(u.id()), List.of());
  }

  /** 인게임 오류(ClientStrings ItemErrorTooManyPrefixes / ItemErrorTooManySuffixes, 한국어 표). */
  static final String TOO_MANY_PREFIXES = "다른 접두어 속성 부여를 추가할 수 없습니다.";

  static final String TOO_MANY_SUFFIXES = "다른 접미어 속성 부여를 추가할 수 없습니다.";

  /**
   * 붙을 옵션이 없는 까닭(C443 UX QA — '자리 · 레벨 · 징조 조건'만으론 원인을 몰랐다): 고른 쪽(좌/우 징조) 또는 양쪽 자리가 차면 인게임 문구, 그 밖은
   * 기존 문구.
   */
  static String noModReason(Item item, Rarity r, String side) {
    int max = maxPerGen(item, r);
    boolean pf = item.count("prefix") >= max;
    boolean sf = item.count("suffix") >= max;
    if ("prefix".equals(side) && pf) {
      return TOO_MANY_PREFIXES;
    }
    if ("suffix".equals(side) && sf) {
      return TOO_MANY_SUFFIXES;
    }
    if (side == null && pf && sf) {
      return TOO_MANY_PREFIXES + " " + TOO_MANY_SUFFIXES + " — 접두 · 접미가 각 " + max + "개로 가득 찼습니다";
    }
    return "붙을 수 있는 옵션이 없습니다(자리 · 레벨 · 징조 조건)";
  }

  /** 깎기의 징조가 보는 레벨 — 미공개 훼손 자리는 1(C438, poe2wiki). */
  static int whittleLevel(Affix a) {
    return unrevealed(a) ? 1 : a.level();
  }

  /** 인게임 오류(ClientStrings ItemErrorInvalidOmenCombination, 한국어 표). */
  static final String INVALID_OMENS = "유효하지 않은 징조 조합입니다.";

  /** 같은 화폐의 좌 · 우 징조를 둘 다 걸었나(C437 — 제작이 막힌다). */
  static boolean omenConflict(Set<String> om, String kind) {
    return om.contains("Sinistral" + kind) && om.contains("Dextral" + kind);
  }

  /** 좌(접두) · 우(접미) 징조 — 둘 다면 null(호출 전에 omenConflict 로 막는다, C437). */
  private static String side(Set<String> om, String kind) {
    boolean s = om.contains("Sinistral" + kind);
    boolean d = om.contains("Dextral" + kind);
    return s == d ? null : s ? "prefix" : "suffix";
  }

  private static List<String> used(Set<String> om, String... keys) {
    List<String> out = new ArrayList<>();
    for (String k : keys) {
      // 좌 · 우를 둘 다 걸면 둘 다 무시(side) — 쓰지도 않는다(10-16 검토: 비용에 둘 다 들어갔다)
      String pair =
          k.startsWith("Sinistral")
              ? "Dextral" + k.substring(9)
              : k.startsWith("Dextral") ? "Sinistral" + k.substring(7) : null;
      if (om.contains(k) && (pair == null || !om.contains(pair))) {
        out.add(k);
      }
    }
    return out;
  }

  /**
   * 첫 뽑기 기록(10-22 Q2, PoE1 PoeCraftEngine.PickProbe 짝) — 수단을 한 번 적용하며 처음 만든 후보(eligible — 등급 최소 레벨
   * · 징조 쪽 · 동질화가 걸린 그대로)와 그 가중(균등, 촉진하는 찬미면 그 태그 배수)을 남긴다. 후보를 안 만드는 수단(소멸 · 신성 · 바알 · 드러내기 등)은
   * cands = null.
   */
  public static final class PickProbe {
    private List<Mod> cands;
    private java.util.Map<String, Double> weights;
    private boolean favoured;
    private Item base;
    private Result result;

    public List<Mod> cands() {
      return cands;
    }

    public java.util.Map<String, Double> weights() {
      return weights;
    }

    public Item base() {
      return base;
    }

    public Result result() {
      return result;
    }
  }

  private static final ThreadLocal<PickProbe> PICK_PROBE = new ThreadLocal<>();

  // 켜진 기록 수 — 0 이면 ThreadLocal 도 안 본다(시뮬레이터 · 최적화기 표본의 뜨거운 길)
  private static final java.util.concurrent.atomic.AtomicInteger PICK_PROBES =
      new java.util.concurrent.atomic.AtomicInteger();

  /** 수단을 한 번(고정 씨앗) 적용하며 첫 후보 · 가중을 기록한다. */
  public PickProbe probeFirstPick(Item item, Action action, String arg, Set<String> omens) {
    return probeFirstPick(item, action, arg, omens, 1);
  }

  /** seed = 카오스(무작위로 하나 지운 뒤 고름)처럼 씨앗마다 갈래가 다르면 서비스가 여러 씨앗을 평균한다(10-22 Q3e 검토). */
  public PickProbe probeFirstPick(
      Item item, Action action, String arg, Set<String> omens, long seed) {
    PickProbe p = new PickProbe();
    PICK_PROBE.set(p);
    PICK_PROBES.incrementAndGet();
    try {
      p.result = apply(item, action, arg, omens, new java.util.SplittableRandom(seed));
    } finally {
      PICK_PROBES.decrementAndGet();
      PICK_PROBE.remove();
    }
    return p;
  }

  /** 붙을 수 있는 옵션 — 아이템 레벨 · 최소 레벨 · 자리 · 같은 그룹 · (쪽) · (동질화 = 있는 옵션과 태그 공유). */
  List<Mod> eligible(Item item, Rarity r, int min, String side, boolean homo) {
    int max = maxPerGen(item, r);
    long pre = item.count("prefix");
    long suf = item.count("suffix");
    Set<Integer> taken = new HashSet<>();
    Set<String> tags = new HashSet<>();
    for (Affix a : item.affixes()) {
      taken.addAll(a.groups());
      Mod m = data.mod(a.id());
      if (m != null && m.tags() != null) {
        tags.addAll(m.tags());
      }
    }
    List<Mod> out = new ArrayList<>();
    // 최소 속성 레벨 예외(C414 — KeywordPopups BetterCurrencyMinimumLevel "특정 속성 유형이 아예 제외되는 경우는 예외"): 유형의
    // 모든
    // 등급이 최소 레벨 아래면 그 유형은 빠지지 않고 최고 등급이 남는다(해석 — 최고 등급만, 동률이면 모두). 유형 = ModType(C418 검토 — 그룹으로
    // 묶으면 허리띠 생명 · 마나 회복처럼 다른 유형이 한 그룹이라 합쳐졌다), 모르면 그룹
    java.util.Map<String, List<Mod>> topBelow = new java.util.LinkedHashMap<>();
    Set<String> famAtMin = new HashSet<>();
    for (Mod m : data.pool(item.poolKey())) {
      boolean p = "prefix".equals(m.gen());
      if (m.level() > item.ilvl()
          || (p ? pre >= max : suf >= max)
          || (side != null && !side.equals(m.gen()))
          || m.groups().stream().anyMatch(taken::contains)
          || (homo && (m.tags() == null || m.tags().stream().noneMatch(tags::contains)))) {
        continue;
      }
      if (min <= 0) {
        // 최소 레벨이 없으면 예외 계산이 필요 없다(C418 — 계열 문자열 · 맵 때문에 엔진 전체가 느려졌다)
        out.add(m);
        continue;
      }
      String fam = m.gen() + (m.type() != null ? "t" + m.type() : "g" + m.groups());
      if (m.level() >= min) {
        out.add(m);
        famAtMin.add(fam);
      } else {
        List<Mod> t = topBelow.computeIfAbsent(fam, k -> new ArrayList<>());
        if (t.isEmpty() || m.level() > t.get(0).level()) {
          t.clear();
          t.add(m);
        } else if (m.level() == t.get(0).level()) {
          t.add(m);
        }
      }
    }
    if (min > 0) {
      for (java.util.Map.Entry<String, List<Mod>> e : topBelow.entrySet()) {
        if (!famAtMin.contains(e.getKey())) {
          out.addAll(e.getValue());
        }
      }
    }
    if (PICK_PROBES.get() > 0 && !out.isEmpty()) {
      PickProbe probe = PICK_PROBE.get();
      if (probe != null && probe.cands == null) {
        probe.cands = out;
        probe.weights = new java.util.LinkedHashMap<>();
        for (Mod m : out) {
          probe.weights.put(m.id(), 1d);
        }
        probe.base = item;
      }
    }
    return out;
  }

  private Result addMods(
      Item item,
      Rarity r,
      int n,
      int min,
      String side,
      boolean homo,
      RandomGenerator rng,
      List<String> omensUsed) {
    return addMods(item, r, n, min, side, homo, rng, omensUsed, null, 1);
  }

  /** 기폭제 종류 키(jewel: 포함)의 태그. */
  static List<String> catalystTags(String key) {
    String k = key != null && key.startsWith("jewel:") ? key.substring("jewel:".length()) : key;
    return k == null ? List.of() : CATALYST_TAGS.getOrDefault(k, List.of());
  }

  /** 촉진하는 찬미 배수(C386) — poe2wiki "20% = ×5, 40% = ×7.5", 그 사이 · 아래는 직선 가정(0% = ×1). */
  static double catalysingMult(int quality) {
    return quality <= 20 ? 1 + 0.2 * quality : 5 + 0.125 * (quality - 20);
  }

  /** 붙일 옵션 고르기 — 균등, favour 태그가 있는 옵션만 mult 배(촉진하는 찬미, C386). */
  private static Mod pick(List<Mod> c, List<String> favour, double mult, RandomGenerator rng) {
    if (favour == null || favour.isEmpty() || mult == 1) {
      return c.get(rng.nextInt(c.size()));
    }
    double[] w = new double[c.size()];
    double total = 0;
    for (int i = 0; i < c.size(); i++) {
      List<String> t = c.get(i).tags();
      w[i] = t != null && t.stream().anyMatch(favour::contains) ? mult : 1;
      total += w[i];
    }
    if (PICK_PROBES.get() > 0) {
      PickProbe probe = PICK_PROBE.get();
      if (probe != null && probe.cands == c && !probe.favoured) {
        probe.favoured = true;
        for (int i = 0; i < c.size(); i++) {
          probe.weights.put(c.get(i).id(), w[i]);
        }
      }
    }
    double x = rng.nextDouble() * total;
    for (int i = 0; i < c.size(); i++) {
      x -= w[i];
      if (x < 0) {
        return c.get(i);
      }
    }
    return c.get(c.size() - 1);
  }

  private Result addMods(
      Item item,
      Rarity r,
      int n,
      int min,
      String side,
      boolean homo,
      RandomGenerator rng,
      List<String> omensUsed,
      List<String> favour,
      double mult) {
    Item cur = item.with(r, item.affixes());
    List<String> added = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      List<Mod> c = eligible(cur, r, min, side, homo);
      if (c.isEmpty()) {
        if (i == 0) {
          return Result.fail(item, noModReason(cur, r, side));
        }
        break;
      }
      Mod m = pick(c, favour, mult, rng);
      List<Affix> out = new ArrayList<>(cur.affixes());
      out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
      cur = cur.with(r, out);
      added.add(m.id());
    }
    return new Result(cur, null, added, List.of(), omensUsed);
  }

  /** 연금 — 일반 · 매직 → 레어 4옵션. 좌/우 연금 징조 = 접두/접미 최대(3 + 1). */
  private Result alchemy(Item item, Set<String> om, RandomGenerator rng) {
    if (item.rarity() == Rarity.RARE) {
      return Result.fail(item, "일반 · 매직 아이템에만 쓸 수 있습니다");
    }
    String side = side(om, "Alchemy");
    List<Affix> keep = item.affixes().stream().filter(Affix::fractured).toList();
    Item cur = item.with(Rarity.RARE, keep);
    List<String> added = new ArrayList<>();
    int max = maxPerGen(cur, Rarity.RARE);
    while (cur.affixes().size() < 4) {
      // 징조 쪽을 먼저 최대까지
      String want = side != null && cur.count(side) < max ? side : null;
      List<Mod> c = eligible(cur, Rarity.RARE, 0, want, false);
      if (c.isEmpty()) {
        c = eligible(cur, Rarity.RARE, 0, null, false);
      }
      if (c.isEmpty()) {
        break;
      }
      Mod m = c.get(rng.nextInt(c.size()));
      List<Affix> out = new ArrayList<>(cur.affixes());
      out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
      cur = cur.with(Rarity.RARE, out);
      added.add(m.id());
    }
    List<String> removed =
        item.affixes().stream().filter(a -> !a.fractured()).map(Affix::id).toList();
    return new Result(cur, null, added, removed, used(om, "SinistralAlchemy", "DextralAlchemy"));
  }

  /**
   * 카오스(PoE2) — 옵션 하나 지우고 하나 추가. 깎기 = 가장 낮은 레벨 옵션을 지움, 좌/우 소거 = 접두/접미만 지움. 추가는 자리 있는 쪽 아무거나(등급 최소
   * 레벨).
   */
  private Result chaos(Item item, int min, Set<String> om, RandomGenerator rng) {
    if (item.rarity() != Rarity.RARE) {
      return Result.fail(item, "레어 아이템에만 쓸 수 있습니다");
    }
    String side = side(om, "Erasure");
    List<Affix> cand =
        item.affixes().stream()
            .filter(a -> !a.fractured() && (side == null || side.equals(a.gen())))
            .toList();
    if (cand.isEmpty()) {
      return Result.fail(item, "지울 수 있는 옵션이 없습니다(분열 · 징조 쪽)");
    }
    Affix gone;
    if (om.contains("Whittling")) {
      // 미공개 훼손 자리도 지울 수 있고 늘 아이템 레벨 1 옵션으로 친다(C438 — poe2wiki 깎기의 징조 "it will always count as an
      // ilvl 1
      // modifier"). 10-16 검토의 '실옵션만'은 근거가 없었다. 징표는 근거가 없어 그대로 뺀다
      List<Affix> real = cand.stream().filter(a -> !abyssMark(a)).toList();
      List<Affix> pool = real.isEmpty() ? cand : real;
      int low = pool.stream().mapToInt(Poe2CraftEngine::whittleLevel).min().orElse(0);
      List<Affix> lows = pool.stream().filter(a -> whittleLevel(a) == low).toList();
      gone = lows.get(rng.nextInt(lows.size()));
    } else {
      gone = cand.get(rng.nextInt(cand.size()));
    }
    List<Affix> out = new ArrayList<>(item.affixes());
    out.remove(gone);
    Item mid = item.with(Rarity.RARE, out);
    List<Mod> c = eligible(mid, Rarity.RARE, min, null, false);
    if (c.isEmpty()) {
      // 지우기만 하고 끝나면 반복 모의에서 아이템이 쪼그라든다(10-16 검토) — 붙일 옵션이 없으면 쓰지 않는다
      return Result.fail(item, "지운 자리에 붙을 옵션이 없습니다(아이템 레벨 · 등급 최소 레벨)");
    }
    List<String> added = new ArrayList<>();
    Mod m = c.get(rng.nextInt(c.size()));
    out.add(new Affix(m.id(), m.gen(), m.level(), m.groups(), false));
    added.add(m.id());
    return new Result(
        item.with(Rarity.RARE, out),
        null,
        added,
        List.of(gone.id()),
        used(om, "Whittling", "SinistralErasure", "DextralErasure"));
  }

  /** 소멸 — 하나(고위 = 둘) 지움, 좌/우 = 접두/접미만. 분열은 안 지움. */
  private Result annul(Item item, Set<String> om, RandomGenerator rng) {
    if (item.rarity() == Rarity.NORMAL) {
      return Result.fail(item, "매직 · 레어 아이템에만 쓸 수 있습니다");
    }
    String side = side(om, "Annulment");
    int n = om.contains("GreaterAnnulment") ? 2 : 1;
    boolean light = om.contains("Light");
    List<Affix> out = new ArrayList<>(item.affixes());
    List<String> removed = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      List<Affix> cand =
          out.stream()
              .filter(a -> !a.fractured() && (side == null || side.equals(a.gen())))
              .filter(a -> !light || isDesecrated(item, a))
              .toList();
      if (cand.isEmpty()) {
        if (i == 0) {
          return Result.fail(
              item, light ? "지울 훼손된 속성이 없습니다(빛의 징조)" : "지울 수 있는 옵션이 없습니다(분열 · 징조 쪽)");
        }
        break;
      }
      Affix gone = cand.get(rng.nextInt(cand.size()));
      out.remove(gone);
      removed.add(gone.id());
    }
    return new Result(
        item.with(item.rarity(), out),
        null,
        List.of(),
        removed,
        used(om, "SinistralAnnulment", "DextralAnnulment", "GreaterAnnulment", "Light"));
  }

  /** 다음 옵션 확률(균등) — 지금 희귀도로(일반이면 매직 자리). */
  public List<Mod> nextPool(Item item) {
    return nextPool(item, null);
  }

  /**
   * 다음 옵션 — tier = 고른 화폐 등급(C441 UX QA): 일반 = 진화, 매직 = 확장, 레어 = 엑잘의 최소 옵션 레벨(유형이 통째로 빠지면 최고 등급은 남는
   * 예외 그대로). 아이템 레벨이 그 최소보다 낮으면 그 등급 화폐를 못 쓰니 빈 목록.
   */
  public List<Mod> nextPool(Item item, String tier) {
    Rarity r = item.rarity() == Rarity.NORMAL ? Rarity.MAGIC : item.rarity();
    Action act =
        item.rarity() == Rarity.NORMAL
            ? Action.TRANSMUTE
            : item.rarity() == Rarity.MAGIC ? Action.AUGMENTATION : Action.EXALT;
    int min = minLevel(act, tier);
    if (min > 0 && item.ilvl() < min) {
      return List.of();
    }
    List<Mod> out = new ArrayList<>(eligible(item, r, min, null, false));
    out.sort(Comparator.comparing(Mod::gen).thenComparing(Mod::level, Comparator.reverseOrder()));
    return out;
  }
}
