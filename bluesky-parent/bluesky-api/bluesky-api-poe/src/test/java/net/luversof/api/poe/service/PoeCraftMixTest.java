package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.service.PoeCraftEngine.Action;
import net.luversof.api.poe.service.PoeCraftEngine.Affix;
import net.luversof.api.poe.service.PoeCraftEngine.Item;
import net.luversof.api.poe.service.PoeCraftEngine.Rarity;

/**
 * 여러 제작 콘텐츠를 섞는 규칙(10-15 C338) — 메타 제작 잠금 · 공격 금지 · 제작대 한도 · 화석 배수 · 수확 태그 보장 · 같은 유형 ± · 저항 변환 ·
 * 태그 추가/제거 · 정복자 엑잘 · 분열 오브.
 */
class PoeCraftMixTest {

  private static final String BASE = "Gloves|str_armour|";
  private static final String CRUSADER = "Gloves|str_armour|crusader";

  private static PoeCraftDataService.CraftMod mod(
      String fam, String gen, int ilvl, int group, String... tags) {
    return new PoeCraftDataService.CraftMod(fam, gen, ilvl, List.of(group), List.of(tags), null);
  }

  private static final Map<String, PoeCraftDataService.CraftMod> MODS = new HashMap<>();
  private static final Map<String, Integer> POOL = new LinkedHashMap<>();
  private static final Map<String, Integer> POOL_CRU = new LinkedHashMap<>();

  static {
    // 접두: 생명력(life) · 방어(defences) · 공격 피해(attack) · 주문 피해(caster) · 화염 피해(fire, attack)
    MODS.put("Life1", mod("IncreasedLife", "prefix", 1, 1, "resource", "life"));
    MODS.put("Armour1", mod("LocalArmour", "prefix", 1, 2, "defences"));
    MODS.put("AttackDmg1", mod("AttackDamage", "prefix", 1, 3, "damage", "attack"));
    MODS.put("SpellDmg1", mod("SpellDamage", "prefix", 1, 4, "damage", "caster"));
    MODS.put(
        "FireAdd1",
        mod("AddedFireDamage", "prefix", 1, 5, "damage", "elemental", "fire", "attack"));
    // 접미: 화염/냉기/번개 저항(2등급씩) · 힘 · 공격 속도(attack)
    MODS.put("FireResist2", mod("FireResist", "suffix", 30, 10, "elemental", "fire", "resistance"));
    MODS.put("FireResist1", mod("FireResist", "suffix", 1, 10, "elemental", "fire", "resistance"));
    MODS.put("ColdResist2", mod("ColdResist", "suffix", 30, 11, "elemental", "cold", "resistance"));
    MODS.put("ColdResist1", mod("ColdResist", "suffix", 1, 11, "elemental", "cold", "resistance"));
    MODS.put(
        "LightningResist1",
        mod("LightningResist", "suffix", 1, 12, "elemental", "lightning", "resistance"));
    MODS.put("Str1", mod("Strength", "suffix", 1, 13, "attribute"));
    MODS.put("AttackSpeed1", mod("IncreasedAttackSpeed", "suffix", 1, 14, "attack", "speed"));
    // 크루세이더 전용
    MODS.put("CruLife1", mod("CrusaderLife", "prefix", 1, 20, "life"));
    for (String id :
        List.of(
            "Life1",
            "Armour1",
            "AttackDmg1",
            "SpellDmg1",
            "FireAdd1",
            "FireResist2",
            "FireResist1",
            "ColdResist2",
            "ColdResist1",
            "LightningResist1",
            "Str1",
            "AttackSpeed1")) {
      POOL.put(id, 1000);
      POOL_CRU.put(id, 1000);
    }
    POOL_CRU.put("CruLife1", 1000);
  }

  private static final Map<String, PoeCraftDataService.Fossil> FOSSILS =
      Map.of(
          "Pristine",
              new PoeCraftDataService.Fossil(
                  "Pristine",
                  "Pristine Fossil",
                  "깨끗한 화석",
                  null,
                  Map.of("life", 10d, "defences", 0d),
                  List.of(),
                  List.of(),
                  0,
                  false,
                  List.of(),
                  List.of(),
                  true),
          "Scorched",
              new PoeCraftDataService.Fossil(
                  "Scorched",
                  "Scorched Fossil",
                  "그을린 화석",
                  null,
                  Map.of("fire", 10d, "cold", 0d),
                  List.of(),
                  List.of(),
                  0,
                  false,
                  List.of(),
                  List.of(),
                  true),
          "Tangled",
              new PoeCraftDataService.Fossil(
                  "Tangled",
                  "Tangled Fossil",
                  "얽힌 화석",
                  null,
                  Map.of(),
                  List.of("X"),
                  List.of(),
                  10,
                  false,
                  List.of(),
                  List.of(),
                  false));

  private static final Map<String, PoeCraftDataService.BenchCraft> BENCH = new HashMap<>();

  static {
    BENCH.put("CraftLife", bench("CraftLife", "prefix", 99));
    BENCH.put("CraftStr", bench("CraftStr", "suffix", 13));
    BENCH.put("CraftDex", bench("CraftDex", "suffix", 30));
    BENCH.put(
        "StrMasterItemGenerationCannotChangePrefixes",
        bench("StrMasterItemGenerationCannotChangePrefixes", "suffix", 1176));
    BENCH.put(
        "DexMasterItemGenerationCannotChangeSuffixes",
        bench("DexMasterItemGenerationCannotChangeSuffixes", "prefix", 1177));
    BENCH.put(
        "IntMasterItemGenerationCannotRollAttackAffixes",
        bench("IntMasterItemGenerationCannotRollAttackAffixes", "suffix", 1178));
    BENCH.put(
        "StrIntMasterItemGenerationCanHaveMultipleCraftedMods",
        bench("StrIntMasterItemGenerationCanHaveMultipleCraftedMods", "suffix", 1180));
  }

  private static PoeCraftDataService.BenchCraft bench(String id, String gen, int group) {
    return new PoeCraftDataService.BenchCraft(
        gen,
        1,
        0,
        id,
        id,
        List.of(group),
        List.of(),
        1,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  private static final Map<String, PoeCraftDataService.Harvest> HARVEST =
      Map.of(
          "ReforgeFire",
              new PoeCraftDataService.Harvest(
                  "ReforgeFire", "reroll_with_mod", "fire ON rare", "", "", "Vivid", 50, 0),
          "ReforgeAttack",
              new PoeCraftDataService.Harvest(
                  "ReforgeAttack", "reroll_with_mod", "attack ON rare", "", "", "Vivid", 75, 0),
          "ReforgeMore",
              new PoeCraftDataService.Harvest(
                  "ReforgeMore",
                  "reroll_with_current_tags_affinity_multiplier",
                  "900",
                  "",
                  "",
                  "Wild",
                  50,
                  0),
          "ColdToFire",
              new PoeCraftDataService.Harvest(
                  "ColdToFire",
                  "convert_mod",
                  "ColdResistance CONVERT cold fire",
                  "",
                  "",
                  "Primal",
                  50,
                  0),
          "AddRemoveFire",
              new PoeCraftDataService.Harvest(
                  "AddRemoveFire",
                  "remove_type_and_add_type_mod",
                  "ANY FOR fire noinfluence",
                  "",
                  "",
                  "Vivid",
                  50,
                  0));

  private static final Map<String, PoeCraftDataService.VeiledMod> VEILED = new HashMap<>();
  private static final Map<String, Integer> VEILED_POOL = new LinkedHashMap<>();

  static {
    for (int i = 0; i < 5; i++) {
      VEILED.put(
          "VP" + i,
          new PoeCraftDataService.VeiledMod(
              "prefix",
              1,
              List.of(300 + i),
              List.of(),
              "Chosen",
              "선택",
              List.of("p" + i),
              List.of(),
              List.of(),
              List.of()));
      VEILED.put(
          "VS" + i,
          new PoeCraftDataService.VeiledMod(
              "suffix",
              1,
              List.of(400 + i),
              List.of(),
              "of the Order",
              "결사",
              List.of("s" + i),
              List.of(),
              List.of(),
              List.of()));
      VEILED_POOL.put("VP" + i, 100 * (i + 1));
      VEILED_POOL.put("VS" + i, 100);
    }
  }

  private static PoeCraftDataService.Fossil special(
      String slug, Map<String, Double> w, List<String> added, List<String> forced, int ce) {
    return new PoeCraftDataService.Fossil(
        slug, slug, slug, null, w, added, forced, ce, false, List.of(), List.of(), false);
  }

  private static final Map<String, PoeCraftDataService.Fossil> SPECIAL =
      Map.of(
          "Faceted", special("Faceted", Map.of("gem", 10d), List.of("GemStr"), List.of(), 0),
          "Hollow", special("Hollow", Map.of(), List.of(), List.of("Abyss"), 0),
          "Glyphic", special("Glyphic", Map.of(), List.of(), List.of(), 100),
          "CurrencyDelveCraftingVaal",
              special("CurrencyDelveCraftingVaal", Map.of(), List.of(), List.of(), 0),
          "CurrencyDelveCraftingMirror",
              special("CurrencyDelveCraftingMirror", Map.of(), List.of(), List.of(), 0));

  private static PoeCraftDataService.VeiledMod delve(String gen, int group, String... tags) {
    return new PoeCraftDataService.VeiledMod(
        gen,
        1,
        List.of(group),
        List.of(tags),
        "d",
        "d",
        List.of("d"),
        List.of(),
        List.of(),
        List.of());
  }

  private static final Map<String, PoeCraftDataService.VeiledMod> FOSSIL_MODS =
      Map.of("GemStr", delve("suffix", 500, "gem"), "Abyss", delve("suffix", 501));

  private static final PoeCraftDataService.Essence HORROR =
      new PoeCraftDataService.Essence(
          "Horror",
          "Essence of Horror",
          "경악의 에센스",
          null,
          8,
          0,
          "suffix",
          "of the Essence",
          "HorrorMod",
          List.of(502),
          63,
          "CurrencyEssenceHorror1",
          List.of("h"),
          List.of(),
          List.of(),
          List.of());

  private static final Map<String, PoeCraftDataService.EldritchMod> ELD = new HashMap<>();

  static {
    for (int t = 1; t <= 4; t++) {
      ELD.put(
          "Ex" + t,
          new PoeCraftDataService.EldritchMod(
              "exarch",
              1,
              List.of(700),
              List.of(),
              "",
              "",
              List.of("x" + t),
              List.of(),
              List.of(),
              List.of(),
              t));
      ELD.put(
          "Ea" + t,
          new PoeCraftDataService.EldritchMod(
              "eater",
              1,
              List.of(701),
              List.of(),
              "",
              "",
              List.of("e" + t),
              List.of(),
              List.of(),
              List.of(),
              t));
    }
  }

  private static final PoeCraftEngine E =
      new PoeCraftEngine(
          new PoeCraftEngine.Data() {
            @Override
            public Map<String, Integer> pool(String poolKey) {
              return BASE.equals(poolKey) ? POOL : CRUSADER.equals(poolKey) ? POOL_CRU : Map.of();
            }

            @Override
            public PoeCraftDataService.CraftMod mod(String id) {
              return MODS.get(id);
            }

            @Override
            public PoeCraftDataService.Essence essence(String itemClass, String slug) {
              return null;
            }

            @Override
            public PoeCraftDataService.Fossil fossil(String slug) {
              return FOSSILS.containsKey(slug) ? FOSSILS.get(slug) : SPECIAL.get(slug);
            }

            @Override
            public Map<String, Integer> fossilPool(String poolKey) {
              return BASE.equals(poolKey) ? Map.of("GemStr", 1000, "Abyss", 2000) : Map.of();
            }

            @Override
            public PoeCraftDataService.VeiledMod fossilMod(String id) {
              return FOSSIL_MODS.get(id);
            }

            @Override
            public List<PoeCraftDataService.Essence> corruptEssences(String itemClass) {
              return List.of(HORROR);
            }

            @Override
            public Map<String, Integer> eldritchPool(String poolKey) {
              Map<String, Integer> m = new HashMap<>();
              ELD.keySet().forEach(id -> m.put(id, 100));
              return m;
            }

            @Override
            public PoeCraftDataService.EldritchMod eldritchMod(String id) {
              return ELD.get(id);
            }

            @Override
            public Map<String, Integer> corruptedPool(String poolKey) {
              // 바알 오브(C358) — CorHigh 는 아이템 레벨 90 이라 84 짜리엔 안 붙는다
              return BASE.equals(poolKey) ? Map.of("CorCurse", 1000, "CorHigh", 1000) : Map.of();
            }

            @Override
            public PoeCraftDataService.VeiledMod corruptedMod(String id) {
              return switch (id) {
                case "CorCurse" ->
                    new PoeCraftDataService.VeiledMod(
                        "corrupted",
                        30,
                        List.of(900),
                        List.of(),
                        "",
                        "",
                        List.of("Curse"),
                        List.of("Curse"),
                        List.of("저주"),
                        List.of("저주"));
                case "CorHigh" ->
                    new PoeCraftDataService.VeiledMod(
                        "corrupted",
                        90,
                        List.of(901),
                        List.of(),
                        "",
                        "",
                        List.of("High"),
                        List.of("High"),
                        List.of("높음"),
                        List.of("높음"));
                default -> null;
              };
            }

            @Override
            public PoeCraftDataService.BenchCraft bench(String itemClass, String modId) {
              return BENCH.get(modId);
            }

            @Override
            public PoeCraftDataService.Harvest harvest(String id) {
              return HARVEST.get(id);
            }

            @Override
            public Map<String, Integer> veiledPool(String poolKey) {
              return BASE.equals(poolKey) ? VEILED_POOL : Map.of();
            }

            @Override
            public PoeCraftDataService.VeiledMod veiledMod(String id) {
              return VEILED.get(id);
            }
          });

  private static Affix a(String id) {
    PoeCraftDataService.CraftMod m = MODS.get(id);
    return new Affix(id, m.g(), m.grp(), false, false);
  }

  private static Affix craft(String id) {
    PoeCraftDataService.BenchCraft b = BENCH.get(id);
    return new Affix(id, b.gen(), b.groups(), true, false);
  }

  private static Item rare(Affix... affixes) {
    return new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(affixes), false);
  }

  @Test
  void 접두_잠금이면_카오스_소멸_정제가_접두를_남긴다() {
    Item it =
        rare(
            a("Life1"),
            a("Armour1"),
            a("Str1"),
            craft("StrMasterItemGenerationCannotChangePrefixes"));
    SplittableRandom rng = new SplittableRandom(1);
    for (int i = 0; i < 2_000; i++) {
      Item r = E.apply(it, Action.CHAOS, null, rng).item();
      assertThat(r.affixes()).contains(a("Life1"), a("Armour1"));
      // 잠긴 쪽은 기존 옵션만 지키고 빈자리엔 새 접두가 붙을 수 있다(C430 — 공식 포럼 3312058 'Not a bug')
      assertThat(r.affixes().stream().filter(x -> "prefix".equals(x.gen())).count())
          .isBetween(2L, 3L);
      // 메타 옵션(접미)은 다시 굴리면 사라진다
      assertThat(r.affixes()).noneMatch(x -> x.id().contains("CannotChangePrefixes"));
    }
    for (int i = 0; i < 200; i++) {
      Item an = E.apply(it, Action.ANNUL, null, rng).item();
      assertThat(an.affixes()).contains(a("Life1"), a("Armour1"));
    }
    Item sc = E.apply(it, Action.SCOUR, null, rng).item();
    assertThat(sc.affixes()).containsExactlyInAnyOrder(a("Life1"), a("Armour1"));
    assertThat(sc.rarity()).isEqualTo(Rarity.RARE);
  }

  @Test
  void 공격_금지가_남아_있으면_엑잘에_공격_옵션이_안_붙는다() {
    Item it = rare(a("Life1"), a("Str1"), craft("IntMasterItemGenerationCannotRollAttackAffixes"));
    SplittableRandom rng = new SplittableRandom(2);
    for (int i = 0; i < 3_000; i++) {
      Item r = E.apply(it, Action.EXALT, null, rng).item();
      String added = r.affixes().get(r.affixes().size() - 1).id();
      assertThat(MODS.get(added).t()).doesNotContain("attack");
    }
    // 메타가 없으면 공격 옵션도 붙는다(대조)
    boolean sawAttack = false;
    for (int i = 0; i < 3_000 && !sawAttack; i++) {
      Item r = E.apply(rare(a("Life1"), a("Str1")), Action.EXALT, null, rng).item();
      sawAttack = MODS.get(r.affixes().get(2).id()).t().contains("attack");
    }
    assertThat(sawAttack).isTrue();
  }

  @Test
  void 제작대_옵션은_하나_제작_옵션_3개_메타면_셋() {
    Item it = rare(a("Life1"), a("Str1"));
    Item one = E.apply(it, Action.BENCH, "CraftDex", null).item();
    assertThat(one.affixes()).anyMatch(x -> x.id().equals("CraftDex") && x.crafted());
    assertThat(E.apply(one, Action.BENCH, "CraftLife", null).error()).contains("하나만");
    // 같은 그룹(힘)은 안 된다
    assertThat(E.apply(it, Action.BENCH, "CraftStr", null).error()).contains("그룹");
    Item mm =
        E.apply(it, Action.BENCH, "StrIntMasterItemGenerationCanHaveMultipleCraftedMods", null)
            .item();
    Item two = E.apply(mm, Action.BENCH, "CraftDex", null).item();
    Item three = E.apply(two, Action.BENCH, "CraftLife", null).item();
    assertThat(three.affixes().stream().filter(Affix::crafted)).hasSize(3);
    // 제작 옵션 제거
    Item removed = E.apply(three, Action.BENCH_REMOVE, null, null).item();
    assertThat(removed.affixes()).containsExactlyInAnyOrder(a("Life1"), a("Str1"));
    // 일반 아이템에 하면 매직
    Item normal = new Item("Gloves", "str_armour", "", 84, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(normal, Action.BENCH, "CraftLife", null).item().rarity())
        .isEqualTo(Rarity.MAGIC);
  }

  @Test
  void 화석_막는_태그는_안_나오고_늘리는_태그는_자주() {
    Item normal = new Item("Gloves", "str_armour", "", 84, Rarity.NORMAL, List.of(), false);
    SplittableRandom rng = new SplittableRandom(3);
    int life = 0, n = 20_000;
    for (int i = 0; i < n; i++) {
      Item r = E.apply(normal, Action.FOSSIL, "Pristine,Scorched", rng).item();
      assertThat(r.rarity()).isEqualTo(Rarity.RARE);
      assertThat(r.affixes()).noneMatch(x -> x.id().equals("Armour1")); // defences ×0
      assertThat(r.affixes()).noneMatch(x -> x.id().startsWith("ColdResist")); // cold ×0
      if (r.affixes().stream().anyMatch(x -> x.id().equals("Life1"))) {
        life++;
      }
    }
    // 생명력 ×10 — 접두 후보(생명 10 · 공격 1 · 주문 1 · 화염 추가 10)에서 거의 늘 붙는다
    assertThat((double) life / n).isGreaterThan(0.85);
    assertThat(E.apply(normal, Action.FOSSIL, "Pristine,Pristine", rng).error()).contains("하나만");
    // 얽혀든(C400) — 이제 다룬다(무작위 효과 · 부패 에센스 일부 확률)
    assertThat(E.apply(normal, Action.FOSSIL, "Tangled", rng).error()).isNull();
    assertThat(E.apply(normal, Action.FOSSIL, "a,b,c,d,e", rng).error()).contains("1~4");
  }

  @Test
  void 수확_태그_재제작은_그_태그_옵션을_보장() {
    Item it = rare(a("Life1"), a("Str1"), a("ColdResist1"), a("Armour1"));
    SplittableRandom rng = new SplittableRandom(4);
    for (int i = 0; i < 3_000; i++) {
      Item r = E.apply(it, Action.HARVEST, "ReforgeFire", rng).item();
      assertThat(r.affixes()).anyMatch(x -> MODS.get(x.id()).t().contains("fire"));
    }
  }

  @Test
  void 같은_유형_더_자주는_지금_태그를_10배() {
    // 지금 옵션: 생명력(life · resource), 힘(attribute) — 다시 굴리면 생명력 · 힘이 자주
    Item it = rare(a("Life1"), a("Str1"), a("Armour1"), a("AttackSpeed1"));
    SplittableRandom rng = new SplittableRandom(5);
    int hit = 0, base = 0, n = 10_000;
    for (int i = 0; i < n; i++) {
      if (E.apply(it, Action.HARVEST, "ReforgeMore", rng).item().affixes().stream()
          .anyMatch(x -> x.id().equals("Life1"))) {
        hit++;
      }
      if (E.apply(it, Action.CHAOS, null, rng).item().affixes().stream()
          .anyMatch(x -> x.id().equals("Life1"))) {
        base++;
      }
    }
    assertThat(hit).isGreaterThan(base);
  }

  @Test
  void 저항_변환은_같은_등급() {
    Item it = rare(a("Life1"), a("ColdResist2"), a("Str1"));
    Item r = E.apply(it, Action.HARVEST, "ColdToFire", new SplittableRandom(6)).item();
    assertThat(r.affixes()).contains(a("FireResist2")).doesNotContain(a("ColdResist2"));
    assertThat(E.apply(rare(a("Life1")), Action.HARVEST, "ColdToFire", null).error())
        .contains("없습니다");
  }

  @Test
  void 태그_추가_제거는_옵션_수를_유지하고_영향력이면_거절() {
    Item it =
        rare(
            a("Life1"),
            a("Armour1"),
            a("AttackDmg1"),
            a("Str1"),
            a("ColdResist1"),
            a("AttackSpeed1"));
    SplittableRandom rng = new SplittableRandom(7);
    for (int i = 0; i < 1_000; i++) {
      PoeCraftEngine.Result r = E.apply(it, Action.HARVEST, "AddRemoveFire", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().affixes())
          .hasSize(6)
          .anyMatch(x -> MODS.get(x.id()).t().contains("fire"));
    }
    Item inf = new Item("Gloves", "str_armour", "crusader", 84, Rarity.RARE, it.affixes(), false);
    assertThat(E.apply(inf, Action.HARVEST, "AddRemoveFire", rng).error()).contains("영향력");
  }

  @Test
  void 정복자_엑잘은_영향력과_전용_옵션() {
    Item it = rare(a("Armour1"), a("Str1"));
    PoeCraftEngine.Result r = E.apply(it, Action.CONQUEROR, "crusader", new SplittableRandom(8));
    assertThat(r.error()).isNull();
    assertThat(r.item().influence()).isEqualTo("crusader");
    assertThat(r.item().affixes()).anyMatch(x -> x.id().equals("CruLife1"));
    assertThat(E.apply(r.item(), Action.CONQUEROR, "crusader", null).error()).contains("이미");
    // 쉐이퍼 · 엘더 엑잘(C391)도 받는다 — 이 시험 풀엔 쉐이퍼 풀이 없어 '그 영향력이 없습니다', 모르는 영향력은 거절
    assertThat(E.apply(it, Action.CONQUEROR, "shaper", null).error()).contains("영향력이 없습니다");
    assertThat(E.apply(it, Action.CONQUEROR, "synthesis", null).error()).contains("영향력 엑잘티드");
  }

  @Test
  void 분열_오브는_제작대가_아닌_옵션_하나를_고정() {
    Item it = rare(a("Life1"), a("Armour1"), a("Str1"), craft("CraftDex"));
    SplittableRandom rng = new SplittableRandom(9);
    for (int i = 0; i < 500; i++) {
      Item r = E.apply(it, Action.FRACTURE, null, rng).item();
      List<Affix> fr = r.affixes().stream().filter(Affix::fractured).toList();
      assertThat(fr).hasSize(1);
      assertThat(fr.get(0).crafted()).isFalse();
    }
    assertThat(E.apply(rare(a("Life1"), a("Str1")), Action.FRACTURE, null, rng).error())
        .contains("4개");
    // 영향력 아이템엔 안 된다(게임 화폐 설명)
    Item inf = new Item("Gloves", "str_armour", "crusader", 84, Rarity.RARE, it.affixes(), false);
    assertThat(E.apply(inf, Action.FRACTURE, null, rng).error()).contains("영향력");
  }

  @Test
  void 장막_카오스는_장막_하나_장막_엑잘은_하나를_바꾼다_해제는_제시_셋_중() {
    SplittableRandom rng = new SplittableRandom(10);
    Item it = rare(a("Life1"), a("Armour1"), a("Str1"), a("ColdResist1"));
    for (int i = 0; i < 1_000; i++) {
      Item r = E.apply(it, Action.VEILED_CHAOS, null, rng).item();
      assertThat(r.affixes().stream().filter(PoeCraftEngine::veiled)).hasSize(1);
    }
    Item ex = E.apply(it, Action.VEILED_EXALT, null, rng).item();
    assertThat(ex.affixes()).hasSize(4);
    assertThat(ex.affixes().stream().filter(PoeCraftEngine::veiled)).hasSize(1);
    // 제시 셋은 아이템 상태로 정해진다 — 두 번 봐도 같고, 그 쪽 해제 옵션만
    List<String> o1 = E.unveilOffers(ex);
    List<String> o2 = E.unveilOffers(ex);
    assertThat(o1).isEqualTo(o2).hasSize(3).doesNotHaveDuplicates();
    String gen = ex.affixes().stream().filter(PoeCraftEngine::veiled).findFirst().get().gen();
    assertThat(o1).allMatch(id -> VEILED.get(id).g().equals(gen));
    PoeCraftEngine.Result un = E.apply(ex, Action.UNVEIL, o1.get(0), rng);
    assertThat(un.error()).isNull();
    assertThat(un.item().affixes())
        .noneMatch(PoeCraftEngine::veiled)
        .anyMatch(x -> x.id().equals(o1.get(0)));
    String other = VEILED.keySet().stream().filter(id -> !o1.contains(id)).findFirst().get();
    assertThat(E.apply(ex, Action.UNVEIL, other, rng).error()).contains("셋 중");
    assertThat(E.apply(it, Action.UNVEIL, "VP0", rng).error()).contains("장막 옵션이 없습니다");
    // 분열 오브는 장막 자리 표시를 고정하지 않는다
    for (int i = 0; i < 300; i++) {
      Item fr = E.apply(ex, Action.FRACTURE, null, rng).item();
      assertThat(fr.affixes().stream().filter(Affix::fractured)).noneMatch(PoeCraftEngine::veiled);
    }
  }

  @Test
  void 특수_화석_추가_강제_부패에센스_부패_분열() {
    SplittableRandom rng = new SplittableRandom(11);
    Item normal = new Item("Gloves", "str_armour", "", 84, Rarity.NORMAL, List.of(), false);
    // 연마한 = 젬 옵션이 풀에 더해지고 ×10 — 풀 13개(각 1000) 사이에서 1만 · 다른 화석 없이는 안 나온다
    int gem = 0;
    for (int i = 0; i < 2_000; i++) {
      Item r = E.apply(normal, Action.FOSSIL, "Faceted", rng).item();
      gem += r.affixes().stream().anyMatch(x -> x.id().equals("GemStr")) ? 1 : 0;
      assertThat(E.apply(normal, Action.FOSSIL, "Pristine", rng).item().affixes())
          .noneMatch(x -> x.id().equals("GemStr"));
    }
    assertThat(gem).isGreaterThan(800);
    // 공허의 = 심연 홈 강제, 상형 문자 = 부패 에센스 옵션 강제
    for (int i = 0; i < 200; i++) {
      assertThat(E.apply(normal, Action.FOSSIL, "Hollow", rng).item().affixes())
          .anyMatch(x -> x.id().equals("Abyss"));
      assertThat(E.apply(normal, Action.FOSSIL, "Glyphic,Hollow", rng).item().affixes())
          .anyMatch(x -> x.id().equals("HorrorMod"))
          .anyMatch(x -> x.id().equals("Abyss"));
    }
    // 피얼룩 = 결과가 부패(더 손댈 수 없다)
    Item vaal = E.apply(normal, Action.FOSSIL, "CurrencyDelveCraftingVaal", rng).item();
    assertThat(vaal.corrupted()).isTrue();
    assertThat(E.apply(vaal, Action.CHAOS, null, rng).error()).isNotNull();
    // 분열된 = 옵션 하나 분열, 분열 · 영향력 아이템엔 못 쓴다
    Item fr = E.apply(normal, Action.FOSSIL, "CurrencyDelveCraftingMirror", rng).item();
    assertThat(fr.affixes().stream().filter(Affix::fractured)).hasSize(1);
    assertThat(E.apply(fr, Action.FOSSIL, "CurrencyDelveCraftingMirror", rng).error())
        .contains("분열");
    Item cru = new Item("Gloves", "str_armour", "crusader", 84, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(cru, Action.FOSSIL, "CurrencyDelveCraftingMirror", rng).error())
        .contains("영향력");
    // 강제 옵션이 이 종류에 안 붙으면 거절
    Item ring = new Item("Ring", "", "", 84, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(ring, Action.FOSSIL, "Hollow", rng).error()).contains("쓸 수 없습니다");
  }

  @Test
  void 엘드리치_불씨_영액은_등급대로_우세한_쪽만_섬뜩한_화폐가_건드린다() {
    SplittableRandom rng = new SplittableRandom(12);
    Item it = rare(a("Life1"), a("Armour1"), a("Str1"), a("ColdResist1"));
    assertThat(E.dominance(it)).isNull();
    assertThat(E.apply(it, Action.ELDRITCH_CHAOS, null, rng).error()).contains("우세");
    Item ex = E.apply(it, Action.EMBER, "3", rng).item();
    assertThat(ex.implicits()).extracting(Affix::id).containsExactly("Ex3");
    // 같은 진영은 바뀐다 · 등급 높은 쪽이 우세, 같으면 없음
    Item both = E.apply(E.apply(ex, Action.EMBER, "2", rng).item(), Action.ICHOR, "1", rng).item();
    assertThat(both.implicits()).extracting(Affix::id).containsExactlyInAnyOrder("Ex2", "Ea1");
    assertThat(E.dominance(both)).isEqualTo(PoeCraftEngine.EXARCH);
    Item tie = E.apply(both, Action.ICHOR, "2", rng).item();
    assertThat(E.dominance(tie)).isNull();
    Item eater = E.apply(both, Action.ICHOR, "4", rng).item();
    assertThat(E.dominance(eater)).isEqualTo(PoeCraftEngine.EATER);
    // 총주교 우세 → 섬뜩한 카오스는 접미 그대로 · 접두만 바뀜, 소멸은 접두만, 엑잘은 접두만
    List<String> suf =
        both.affixes().stream()
            .filter(x -> x.gen().equals("suffix"))
            .map(Affix::id)
            .sorted()
            .toList();
    for (int i = 0; i < 500; i++) {
      Item r = E.apply(both, Action.ELDRITCH_CHAOS, null, rng).item();
      assertThat(
              r.affixes().stream()
                  .filter(x -> x.gen().equals("suffix"))
                  .map(Affix::id)
                  .sorted()
                  .toList())
          .isEqualTo(suf);
      assertThat(r.implicits()).isEqualTo(both.implicits());
      PoeCraftEngine.Result an = E.apply(both, Action.ELDRITCH_ANNUL, null, rng);
      assertThat(an.removed()).allMatch(id -> MODS.get(id).g().equals("prefix"));
    }
    Item onePre = rare(a("Life1"), a("Str1"), a("ColdResist1")).withImplicits(both.implicits());
    for (int i = 0; i < 300; i++) {
      PoeCraftEngine.Result ad = E.apply(onePre, Action.ELDRITCH_EXALT, null, rng);
      assertThat(ad.added()).allMatch(id -> MODS.get(id).g().equals("prefix"));
    }
    // 섬뜩한 소멸도 공격 금지 메타가 있으면 공격 옵션은 안 지운다(C433 검토 — CoE 'Eldritch Orb of Annulment' 공격 YES)
    Item guarded =
        rare(a("AttackDmg1"), a("Life1"), craft("IntMasterItemGenerationCannotRollAttackAffixes"))
            .withImplicits(both.implicits());
    for (int i = 0; i < 200; i++) {
      assertThat(E.apply(guarded, Action.ELDRITCH_ANNUL, null, rng).removed())
          .containsExactly("Life1");
    }
    // 정제는 임플리싯을 남긴다, 정복자는 엘드리치가 있으면 거절, 불씨는 영향력 · 다른 부위엔 못 쓴다
    assertThat(E.apply(both, Action.SCOUR, null, rng).item().implicits()).hasSize(2);
    assertThat(E.apply(both, Action.CONQUEROR, "crusader", rng).error())
        .contains("영향력을 추가할 수 없습니다");
    Item cru =
        new Item("Gloves", "str_armour", "crusader", 84, Rarity.RARE, List.of(a("Life1")), false);
    assertThat(E.apply(cru, Action.EMBER, "1", rng).error()).contains("영향력");
    Item ring = new Item("Ring", "", "", 84, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(ring, Action.ICHOR, "1", rng).error()).contains("갑옷");
  }

  @Test
  void 지배의_오브는_영향력_옵션_하나를_올리고_하나를_지운다() {
    // 사냥꾼 풀 = 기본 풀 + H1(접두) · H2(H1 한 등급 위) · HS(접미). H2 위는 고양 옵션 HE
    Map<String, PoeCraftDataService.CraftMod> mods = new HashMap<>(MODS);
    mods.put("H1", mod("HunterLife", "prefix", 1, 30, "life"));
    mods.put("H2", mod("HunterLife", "prefix", 50, 30, "life"));
    mods.put("HS", mod("HunterRes", "suffix", 1, 31, "resistance"));
    Map<String, Integer> hunter = new LinkedHashMap<>(POOL);
    hunter.put("H1", 500);
    hunter.put("H2", 500);
    hunter.put("HS", 500);
    PoeCraftEngine e =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return BASE.equals(poolKey)
                    ? POOL
                    : poolKey.endsWith("basilisk") ? hunter : Map.of();
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return mods.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }

              @Override
              public Map<String, String> upgrade(String poolKey) {
                return Map.of("H1", "H2", "H2", "HE");
              }

              @Override
              public PoeCraftDataService.VeiledMod elevatedMod(String id) {
                return "HE".equals(id) ? delve("prefix", 30) : null;
              }
            });
    SplittableRandom rng = new SplittableRandom(13);
    Affix h1 = new Affix("H1", "prefix", List.of(30), false, false);
    Affix hs = new Affix("HS", "suffix", List.of(31), false, false);
    Item it =
        new Item(
            "Gloves",
            "str_armour",
            "basilisk",
            84,
            Rarity.RARE,
            List.of(a("Armour1"), h1, a("Str1"), hs),
            false);
    int upH = 0;
    for (int i = 0; i < 400; i++) {
      PoeCraftEngine.Result r = e.apply(it, Action.DOMINANCE, null, rng);
      assertThat(r.error()).isNull();
      List<String> ids = r.item().affixes().stream().map(Affix::id).toList();
      // 영향력 아닌 옵션은 그대로, 영향력 옵션은 하나만 남는다(올라간 것)
      assertThat(ids).contains("Armour1", "Str1").hasSize(3);
      assertThat(ids.contains("H2") || ids.contains("HS")).isTrue();
      upH += ids.contains("H2") ? 1 : 0;
    }
    assertThat(upH).isBetween(140, 260);
    // 최고 등급 → 고양 옵션, 영향력 옵션이 하나면 거절, 다른 부위면 거절
    Item top =
        new Item(
            "Gloves",
            "str_armour",
            "basilisk",
            84,
            Rarity.RARE,
            List.of(new Affix("H2", "prefix", List.of(30), false, false), hs),
            false);
    boolean elev = false;
    for (int i = 0; i < 50 && !elev; i++) {
      elev =
          e.apply(top, Action.DOMINANCE, null, rng).item().affixes().stream()
              .anyMatch(x -> x.id().equals("HE"));
    }
    assertThat(elev).isTrue();
    Item one =
        new Item(
            "Gloves", "str_armour", "basilisk", 84, Rarity.RARE, List.of(h1, a("Str1")), false);
    assertThat(e.apply(one, Action.DOMINANCE, null, rng).error()).contains("둘 이상");
    Item ring = new Item("Ring", "", "basilisk", 84, Rarity.RARE, List.of(h1, hs), false);
    assertThat(e.apply(ring, Action.DOMINANCE, null, rng).error()).contains("갑옷");
    // 분열된 영향력 옵션은 올리지도 지우지도 않는다(C433 검토) — 나머지 하나만으론 올리고 지울 짝이 없다
    Item frac =
        new Item(
            "Gloves",
            "str_armour",
            "basilisk",
            84,
            Rarity.RARE,
            List.of(new Affix("H1", "prefix", List.of(30), false, true), hs, a("Str1")),
            false);
    for (int i = 0; i < 50; i++) {
      assertThat(e.apply(frac, Action.DOMINANCE, null, rng).error()).isNotNull();
    }
  }

  @Test
  void 각성자의_오브는_두_영향력으로_새로_굴리며_양쪽_영향력_옵션을_하나씩_남긴다() {
    Map<String, PoeCraftDataService.CraftMod> mods = new HashMap<>(MODS);
    mods.put("C1", mod("CruA", "prefix", 1, 40, "life"));
    mods.put("C2", mod("CruB", "suffix", 1, 41));
    mods.put("H1", mod("HunA", "suffix", 1, 42));
    Map<String, Integer> cru = new LinkedHashMap<>(POOL);
    cru.put("C1", 100);
    cru.put("C2", 100);
    Map<String, Integer> hun = new LinkedHashMap<>(POOL);
    hun.put("H1", 100);
    PoeCraftEngine e =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                Map<String, Integer> out = new LinkedHashMap<>();
                String inf = poolKey.substring(poolKey.lastIndexOf('|') + 1);
                if (inf.isEmpty()) {
                  return BASE.equals(poolKey) ? POOL : Map.of();
                }
                for (String x : inf.split(",")) {
                  out.putAll("crusader".equals(x) ? cru : "basilisk".equals(x) ? hun : Map.of());
                }
                return out;
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return mods.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }
            });
    SplittableRandom rng = new SplittableRandom(14);
    Affix c1 = new Affix("C1", "prefix", List.of(40), false, false);
    Item it =
        new Item(
            "Gloves", "str_armour", "crusader", 84, Rarity.RARE, List.of(c1, a("Str1")), false);
    for (int i = 0; i < 300; i++) {
      PoeCraftEngine.Result r = e.apply(it, Action.AWAKENER, "basilisk", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().influence()).isEqualTo("basilisk,crusader");
      assertThat(r.item().rarity()).isEqualTo(Rarity.RARE);
      // 이 아이템의 영향력 옵션(C1) · 없앤 아이템 쪽 옵션(사냥꾼 전용은 H1 하나)이 남는다
      assertThat(r.item().affixes()).extracting(Affix::id).contains("C1", "H1");
      assertThat(r.item().affixes().size()).isBetween(4, 6);
    }
    assertThat(e.apply(it, Action.AWAKENER, "crusader", rng).error()).contains("다른 영향력");
    assertThat(e.apply(it, Action.AWAKENER, "basilisk:C2", rng).error()).contains("옵션이 아닙니다");
    Item dual = e.apply(it, Action.AWAKENER, "basilisk:H1", rng).item();
    assertThat(e.apply(dual, Action.AWAKENER, "eyrie", rng).error()).contains("하나인");
    Item none = new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(a("Str1")), false);
    assertThat(e.apply(none, Action.AWAKENER, "basilisk", rng).error()).contains("하나인");
  }

  @Test
  void 수확_영향력_유형_재련은_영향력을_바꾸고_다시_굴린다() {
    PoeCraftDataService.Harvest h =
        new PoeCraftDataService.Harvest(
            "RerollInfluenceType",
            "reroll_influence_types",
            "Gloves Boots",
            "",
            "",
            "Wild",
            1000,
            0);
    PoeCraftEngine e =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return poolKey.endsWith("|")
                        || poolKey.endsWith("crusader")
                        || poolKey.endsWith("basilisk")
                        || poolKey.endsWith("eyrie")
                    ? POOL
                    : Map.of();
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return MODS.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }

              @Override
              public PoeCraftDataService.Harvest harvest(String id) {
                return h;
              }
            });
    SplittableRandom rng = new SplittableRandom(15);
    Item it =
        new Item(
            "Gloves",
            "str_armour",
            "crusader",
            84,
            Rarity.RARE,
            List.of(a("Life1"), a("Str1")),
            false);
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (int i = 0; i < 200; i++) {
      PoeCraftEngine.Result r = e.apply(it, Action.HARVEST, "RerollInfluenceType", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().influence()).isNotEqualTo("crusader");
      assertThat(r.item().affixes().size()).isBetween(4, 6);
      seen.add(r.item().influence());
    }
    assertThat(seen).containsExactlyInAnyOrder("basilisk", "eyrie");
    // 영향력 둘이면 둘인 채로
    Item dual =
        new Item(
            "Gloves",
            "str_armour",
            "basilisk,crusader",
            84,
            Rarity.RARE,
            List.of(a("Life1")),
            false);
    assertThat(
            e.apply(dual, Action.HARVEST, "RerollInfluenceType", rng).item().influence().split(","))
        .hasSize(2);
    Item plain = new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(a("Life1")), false);
    assertThat(e.apply(plain, Action.HARVEST, "RerollInfluenceType", rng).error())
        .contains("영향력이 있는");
    Item ring = new Item("Ring", "", "crusader", 84, Rarity.RARE, List.of(), false);
    assertThat(e.apply(ring, Action.HARVEST, "RerollInfluenceType", rng).error()).contains("종류");
  }

  @Test
  void 옵션_직접_넣기는_풀_그룹_자리를_지키고_희귀도를_올린다() {
    SplittableRandom rng = new SplittableRandom(16);
    Item it = new Item("Gloves", "str_armour", "", 84, Rarity.NORMAL, List.of(), false);
    Item a1 = E.apply(it, Action.MANUAL_ADD, "Life1:F", rng).item();
    assertThat(a1.rarity()).isEqualTo(Rarity.MAGIC);
    assertThat(a1.affixes()).singleElement().matches(Affix::fractured);
    Item a2 = E.apply(a1, Action.MANUAL_ADD, "Str1", rng).item();
    assertThat(a2.rarity()).isEqualTo(Rarity.MAGIC);
    Item a3 = E.apply(a2, Action.MANUAL_ADD, "Armour1", rng).item();
    assertThat(a3.rarity()).isEqualTo(Rarity.RARE);
    assertThat(E.apply(a3, Action.MANUAL_ADD, "Life1", rng).error()).contains("그룹");
    assertThat(E.apply(a3, Action.MANUAL_ADD, "CruLife1", rng).error()).contains("풀");
    assertThat(E.apply(a3, Action.MANUAL_ADD, "FireResist2:F", rng).item().affixes()).hasSize(4);
    Item low = new Item("Gloves", "str_armour", "", 10, Rarity.NORMAL, List.of(), false);
    assertThat(E.apply(low, Action.MANUAL_ADD, "FireResist2", rng).error()).contains("아이템 레벨");
    Item full =
        E.apply(
                E.apply(a3, Action.MANUAL_ADD, "AttackDmg1", rng).item(),
                Action.MANUAL_ADD,
                "SpellDmg1",
                rng)
            .item();
    assertThat(full.count("prefix")).isEqualTo(3);
    assertThat(E.apply(full, Action.MANUAL_ADD, "FireAdd1", rng).error()).contains("자리");
    // 빼기 — 분열 옵션도 뺄 수 있고, 다 빼면 일반
    Item r1 = E.apply(a1, Action.MANUAL_REMOVE, "Life1", rng).item();
    assertThat(r1.affixes()).isEmpty();
    assertThat(r1.rarity()).isEqualTo(Rarity.NORMAL);
  }

  @Test
  void 바알_오브는_타락시키며_넷_중_하나_타락하면_더_못_바꾼다() {
    SplittableRandom rng = new SplittableRandom(3);
    Item it = rare(a("Life1"), a("Armour1"), a("Str1"), a("ColdResist1"));
    Item withEx = E.apply(it, Action.EMBER, "3", rng).item();
    Map<String, Integer> seen = new HashMap<>();
    for (int i = 0; i < 2000; i++) {
      PoeCraftEngine.Result r = E.apply(withEx, Action.VAAL, null, rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().corrupted()).isTrue();
      seen.merge(r.note(), 1, Integer::sum);
      switch (r.note()) {
        case "vaal:none", "vaal:sockets" -> {
          assertThat(r.item().affixes()).isEqualTo(withEx.affixes());
          assertThat(r.item().implicits()).isEqualTo(withEx.implicits());
        }
        case "vaal:implicit" -> {
          // 엘드리치 임플리싯까지 부패 임플리싯 하나로, 아이템 레벨 넘는 건 안 붙는다
          assertThat(r.item().implicits()).extracting(Affix::id).containsExactly("CorCurse");
          assertThat(r.removed()).containsExactly("Ex3");
          assertThat(r.item().affixes()).isEqualTo(withEx.affixes());
        }
        case "vaal:reforge" -> {
          assertThat(r.item().rarity()).isEqualTo(Rarity.RARE);
          assertThat(r.item().implicits()).isEqualTo(withEx.implicits());
        }
        default -> throw new AssertionError(r.note());
      }
      // 타락하면 바알 오브 · 카오스 모두 거절
      assertThat(E.apply(r.item(), Action.VAAL, null, rng).error()).contains("타락");
    }
    // 넷이 대략 균등(각 500 ± 100)
    assertThat(seen.keySet())
        .containsExactlyInAnyOrder("vaal:none", "vaal:sockets", "vaal:implicit", "vaal:reforge");
    assertThat(seen.values()).allMatch(n -> n > 400 && n < 600);
    // 분열 옵션은 레어 재구성에서도 남는다
    Item fr =
        rare(new Affix("Life1", "prefix", List.of(1), false, true), a("Str1"), a("ColdResist1"));
    for (int i = 0; i < 300; i++) {
      PoeCraftEngine.Result r = E.apply(fr, Action.VAAL, null, rng);
      assertThat(r.item().affixes()).extracting(Affix::id).contains("Life1");
    }
    // 플라스크엔 못 쓰고, 반지의 소켓 결과는 "변화 없음"
    Item flask = new Item("LifeFlask", "", "", 84, Rarity.MAGIC, List.of(), false);
    assertThat(E.apply(flask, Action.VAAL, null, rng).error()).contains("플라스크");
    Item ring = new Item("Ring", "", "", 84, Rarity.RARE, List.of(), false);
    for (int i = 0; i < 100; i++) {
      assertThat(E.apply(ring, Action.VAAL, null, rng).note()).isNotEqualTo("vaal:sockets");
    }
  }

  @Test
  void 더럽혀진_화폐는_타락한_레어에만_둘_중_하나() {
    SplittableRandom rng = new SplittableRandom(5);
    Item clean = rare(a("Life1"), a("Armour1"), a("FireResist1"), a("Str1"));
    assertThat(E.apply(clean, Action.TAINTED_CHAOS, null, rng).error()).contains("타락한 희귀");
    Item magic = new Item("Gloves", "str_armour", "", 84, Rarity.MAGIC, List.of(a("Life1")), true);
    assertThat(E.apply(magic, Action.TAINTED_EXALT, null, rng).error()).contains("타락한 희귀");
    Item cor = clean.corrupt();
    // 카오스 — 다시 굴림 | 전부 지움(레어 · 타락 그대로), 반반
    Map<String, Integer> seen = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      PoeCraftEngine.Result r = E.apply(cor, Action.TAINTED_CHAOS, null, rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().corrupted()).isTrue();
      assertThat(r.item().rarity()).isEqualTo(Rarity.RARE);
      seen.merge(r.note(), 1, Integer::sum);
      if ("tainted:clear".equals(r.note())) {
        assertThat(r.item().affixes()).isEmpty();
      } else {
        assertThat(r.item().affixes().size()).isBetween(4, 6);
      }
    }
    assertThat(seen.get("tainted:clear")).isBetween(420, 580);
    // 엑잘티드 — 붙임 | 지움 반반, 꽉 찼으면 지우기만 · 비었으면 붙이기만
    seen.clear();
    for (int i = 0; i < 1000; i++) {
      PoeCraftEngine.Result r = E.apply(cor, Action.TAINTED_EXALT, null, rng);
      seen.merge(r.note(), 1, Integer::sum);
      assertThat(r.item().affixes()).hasSize("tainted:add".equals(r.note()) ? 5 : 3);
    }
    assertThat(seen.get("tainted:add")).isBetween(420, 580);
    Item fullItem =
        rare(
                a("Life1"),
                a("Armour1"),
                a("AttackDmg1"),
                a("FireResist1"),
                a("Str1"),
                a("AttackSpeed1"))
            .corrupt();
    Item empty = rare().corrupt();
    for (int i = 0; i < 50; i++) {
      assertThat(E.apply(fullItem, Action.TAINTED_EXALT, null, rng).note())
          .isEqualTo("tainted:remove");
      assertThat(E.apply(empty, Action.TAINTED_EXALT, null, rng).note()).isEqualTo("tainted:add");
    }
    // 신성한 눈물방울 — 화염 저항 1등급(아래)은 올라가거나 그대로, 2등급(위)은 내려가거나 그대로, 등급 하나뿐인 힘은 그대로
    Set<String> low = new java.util.HashSet<>();
    Set<String> high = new java.util.HashSet<>();
    for (int i = 0; i < 200; i++) {
      Item r1 = E.apply(cor, Action.TAINTED_DIVINE, null, rng).item();
      low.add(
          r1.affixes().stream()
              .filter(x -> x.id().startsWith("FireResist"))
              .findFirst()
              .orElseThrow()
              .id());
      assertThat(r1.affixes()).extracting(Affix::id).contains("Str1");
      Item hi = rare(a("FireResist2"), a("Str1")).corrupt();
      Item r2 = E.apply(hi, Action.TAINTED_DIVINE, null, rng).item();
      high.add(
          r2.affixes().stream()
              .filter(x -> x.id().startsWith("FireResist"))
              .findFirst()
              .orElseThrow()
              .id());
    }
    assertThat(low).containsExactlyInAnyOrder("FireResist1", "FireResist2");
    assertThat(high).containsExactlyInAnyOrder("FireResist1", "FireResist2");
    // 분열 옵션은 카오스 · 눈물방울 모두 그대로
    Item fr =
        rare(new Affix("FireResist1", "suffix", List.of(10), false, true), a("Life1")).corrupt();
    for (int i = 0; i < 100; i++) {
      assertThat(E.apply(fr, Action.TAINTED_DIVINE, null, rng).item().affixes())
          .extracting(Affix::id)
          .contains("FireResist1");
      assertThat(E.apply(fr, Action.TAINTED_CHAOS, null, rng).item().affixes())
          .extracting(Affix::id)
          .contains("FireResist1");
    }
  }

  @Test
  void 타락한_아이템도_직접_편집은_된다_화폐는_안_된다() {
    SplittableRandom rng = new SplittableRandom(9);
    Item cor = rare(a("Life1"), a("Str1")).corrupt();
    assertThat(E.apply(cor, Action.CHAOS, null, rng).error()).contains("타락");
    PoeCraftEngine.Result rm = E.apply(cor, Action.MANUAL_REMOVE, "Str1", rng);
    assertThat(rm.error()).isNull();
    assertThat(rm.item().corrupted()).isTrue();
    assertThat(rm.item().affixes()).extracting(Affix::id).containsExactly("Life1");
    assertThat(E.apply(rm.item(), Action.MANUAL_ADD, "ColdResist1", rng).error()).isNull();
  }

  @Test
  void 검토_변환_그룹_겹침_섬뜩한_카오스_잠금_최소_하나() {
    SplittableRandom rng = new SplittableRandom(13);
    // 화염 저항이 이미 있으면 냉기 → 화염 변환은 실패(같은 그룹 두 줄 방지)
    Item both = rare(a("Life1"), a("ColdResist2"), a("FireResist1"));
    assertThat(E.apply(both, Action.HARVEST, "ColdToFire", rng).error()).contains("같은 그룹");
    // 총주교 우세 + 접두 잠금 → 섬뜩한 카오스는 잠금 무시(C430 — CoE 표 'NO'): 잠긴 접두도 다시 굴린다
    Item locked =
        E.apply(
                rare(
                    a("Life1"),
                    craft("StrMasterItemGenerationCannotChangePrefixes"),
                    a("ColdResist1")),
                Action.EMBER,
                "3",
                rng)
            .item();
    PoeCraftEngine.Result lr = E.apply(locked, Action.ELDRITCH_CHAOS, null, rng);
    assertThat(lr.error()).isNull();
    assertThat(lr.removed()).contains("Life1");
    // 접미 셋 + 총주교 우세 → 섬뜩한 카오스 뒤 접두가 적어도 하나
    Item three =
        E.apply(rare(a("Str1"), a("ColdResist1"), a("AttackSpeed1")), Action.EMBER, "3", rng)
            .item();
    for (int k = 0; k < 200; k++) {
      Item r = E.apply(three, Action.ELDRITCH_CHAOS, null, rng).item();
      assertThat(r.count("prefix")).isGreaterThanOrEqualTo(1);
    }
  }

  @Test
  void 원소_피해_변환은_유형_기준_같은_자리_같은_등급() {
    // C384 — 유형(mt) 이름으로: 냉기 피해(LocalColdDamage) T2 → 화염 피해(LocalFireDamage) T2
    Map<String, PoeCraftDataService.CraftMod> mods = new HashMap<>();
    mods.put(
        "Cold1",
        new PoeCraftDataService.CraftMod(
            "LocalAddedColdDamage",
            "prefix",
            1,
            List.of(50),
            List.of("cold"),
            null,
            "LocalColdDamage"));
    mods.put(
        "Cold2",
        new PoeCraftDataService.CraftMod(
            "LocalAddedColdDamage",
            "prefix",
            30,
            List.of(50),
            List.of("cold"),
            null,
            "LocalColdDamage"));
    mods.put(
        "Cold3",
        new PoeCraftDataService.CraftMod(
            "LocalAddedColdDamage",
            "prefix",
            60,
            List.of(50),
            List.of("cold"),
            null,
            "LocalColdDamage"));
    mods.put(
        "Fire1",
        new PoeCraftDataService.CraftMod(
            "LocalAddedFireDamage",
            "prefix",
            1,
            List.of(51),
            List.of("fire"),
            null,
            "LocalFireDamage"));
    mods.put(
        "Fire2",
        new PoeCraftDataService.CraftMod(
            "LocalAddedFireDamage",
            "prefix",
            30,
            List.of(51),
            List.of("fire"),
            null,
            "LocalFireDamage"));
    mods.put(
        "Fire3",
        new PoeCraftDataService.CraftMod(
            "LocalAddedFireDamage",
            "prefix",
            60,
            List.of(51),
            List.of("fire"),
            null,
            "LocalFireDamage"));
    mods.put("Str1", MODS.get("Str1"));
    Map<String, Integer> pool = new LinkedHashMap<>();
    mods.keySet().forEach(id -> pool.put(id, 1000));
    Map<String, PoeCraftDataService.Harvest> hv =
        Map.of(
            "ChangeColdToFireDamage",
            new PoeCraftDataService.Harvest(
                "ChangeColdToFireDamage",
                "convert_mod",
                "ColdDamage LocalColdDamage CONVERT cold fire",
                "",
                "",
                "Wild",
                30,
                0),
            "ChangeFireToColdDamage",
            new PoeCraftDataService.Harvest(
                "ChangeFireToColdDamage",
                "convert_mod",
                "FireDamage LocalFireDamage CONVERT fire cold",
                "",
                "",
                "Wild",
                30,
                0));
    PoeCraftEngine ev =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return pool;
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return mods.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }

              @Override
              public PoeCraftDataService.Harvest harvest(String id) {
                return hv.get(id);
              }
            });
    Item it =
        new Item(
            "Gloves",
            "str_armour",
            "",
            84,
            Rarity.RARE,
            List.of(new Affix("Cold2", "prefix", List.of(50), false, false), a("Str1")),
            false);
    PoeCraftEngine.Result r =
        ev.apply(it, Action.HARVEST, "ChangeColdToFireDamage", new SplittableRandom(1));
    assertThat(r.error()).isNull();
    assertThat(r.item().affixes()).extracting(Affix::id).containsExactlyInAnyOrder("Fire2", "Str1");
    // 냉기 피해가 없으면 실패
    Item none = new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(a("Str1")), false);
    assertThat(
            ev.apply(none, Action.HARVEST, "ChangeColdToFireDamage", new SplittableRandom(1))
                .error())
        .contains("없습니다");
  }

  @Test
  void 기폭제는_장신구에_같은_종류면_쌓이고_다른_종류면_처음부터() {
    SplittableRandom rng = new SplittableRandom(3);
    Item ring =
        new Item(
            "Ring",
            "",
            "",
            84,
            Rarity.RARE,
            List.of(a("Life1"), a("Armour1"), a("FireResist1")),
            false);
    // 장신구만
    assertThat(E.apply(rare(a("Life1")), Action.CATALYST, "attack", rng).error()).contains("반지");
    PoeCraftEngine.Result r = E.apply(ring, Action.CATALYST, "resource", rng);
    assertThat(r.item().quality()).isEqualTo("resource:1");
    assertThat(r.note()).isEqualTo("catalyst:resource:1:1");
    // 옵션은 그대로
    assertThat(r.item().affixes()).isEqualTo(ring.affixes());
    // 연달아 30번 요청해도 20% 까지만, 쓴 개수 = 19
    PoeCraftEngine.Result fill = E.apply(r.item(), Action.CATALYST, "resource*30", rng);
    assertThat(fill.item().quality()).isEqualTo("resource:20");
    assertThat(fill.note()).isEqualTo("catalyst:resource:20:19");
    assertThat(E.apply(fill.item(), Action.CATALYST, "resource", rng).error()).contains("최대");
    // 다른 종류 = 바뀌고 처음부터
    assertThat(E.apply(fill.item(), Action.CATALYST, "resistance", rng).item().quality())
        .isEqualTo("resistance:1");
    // 퀄리티는 카오스로 옵션을 다시 굴려도 남는다
    assertThat(E.apply(fill.item(), Action.CHAOS, null, rng).item().quality())
        .isEqualTo("resource:20");
    // 수치가 오르는 옵션 — 생명력(life) / 저항(resistance) / 접두(자리)
    assertThat(E.boosted(fill.item())).containsExactly("Life1");
    Item res = E.apply(fill.item(), Action.CATALYST, "resistance", rng).item();
    assertThat(E.boosted(res)).containsExactly("FireResist1");
    Item pre = E.apply(fill.item(), Action.CATALYST, "prefix", rng).item();
    assertThat(E.boosted(pre)).containsExactlyInAnyOrder("Life1", "Armour1");
    assertThat(E.boosted(ring)).isEmpty();
  }

  @Test
  void 타락한_장신구는_더럽혀진_기폭제만() {
    SplittableRandom rng = new SplittableRandom(4);
    Item ring = new Item("Ring", "", "", 84, Rarity.RARE, List.of(a("Life1")), false);
    assertThat(E.apply(ring, Action.CATALYST, "vaal", rng).error()).contains("타락한 반지");
    Item cor = ring.withQuality("attack:20").corrupt();
    assertThat(E.apply(cor, Action.CATALYST, "attack", rng).error()).contains("타락한 아이템");
    Set<String> kinds = new java.util.HashSet<>();
    for (int i = 0; i < 500; i++) {
      PoeCraftEngine.Result r = E.apply(cor, Action.CATALYST, "vaal", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().qualityAmount()).isBetween(1, 20);
      assertThat(PoeCraftEngine.CATALYST_TAGS).containsKey(r.item().qualityKey());
      kinds.add(r.item().qualityKey());
    }
    assertThat(kinds).hasSize(PoeCraftEngine.CATALYST_TAGS.size());
  }

  @Test
  void 짐승_제작_접미_추가_접두_제거와_영향력_옵션_추가() {
    SplittableRandom rng = new SplittableRandom(11);
    Item it = rare(a("Life1"), a("Armour1"), a("FireResist1"));
    // 희귀만
    Item magic = new Item("Gloves", "str_armour", "", 84, Rarity.MAGIC, List.of(a("Life1")), false);
    assertThat(E.apply(magic, Action.BEAST, "swap-suffix", rng).error()).contains("희귀");
    for (int i = 0; i < 50; i++) {
      PoeCraftEngine.Result r = E.apply(it, Action.BEAST, "swap-suffix", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().count("prefix")).isEqualTo(1);
      assertThat(r.item().count("suffix")).isEqualTo(2);
      assertThat(r.removed()).hasSize(1).allMatch(id -> id.equals("Life1") || id.equals("Armour1"));
    }
    // 반대 — 접미를 지우고 접두를 붙임
    PoeCraftEngine.Result p = E.apply(it, Action.BEAST, "swap-prefix", rng);
    assertThat(p.item().count("prefix")).isEqualTo(3);
    assertThat(p.item().count("suffix")).isZero();
    // 분열 옵션은 지우지 않는다 — 지울 접두가 분열뿐이면 실패
    Item fr =
        rare(new Affix("Life1", "prefix", List.of(1), false, true), a("FireResist1"), a("Str1"));
    assertThat(E.apply(fr, Action.BEAST, "swap-suffix", rng).error()).contains("지울 접두어");
    // 접미 자리가 꽉 찼으면 실패
    Item fullSuf = rare(a("Life1"), a("FireResist1"), a("ColdResist1"), a("Str1"));
    assertThat(E.apply(fullSuf, Action.BEAST, "swap-suffix", rng).error()).contains("빈 접미어");
    // 영향력 옵션 추가 — 그 영향력이 있어야, 영향력은 그대로
    assertThat(E.apply(it, Action.BEAST, "influence:crusader", rng).error()).contains("영향력이 있는");
    Item cru =
        new Item("Gloves", "str_armour", "crusader", 84, Rarity.RARE, List.of(a("Armour1")), false);
    // 엑잘처럼(C430 — maxroll 'does not guarantee an influenced modifier'): 영향력 옵션도 일반 옵션도 나온다
    java.util.Set<String> got = new java.util.HashSet<>();
    for (int i = 0; i < 300; i++) {
      PoeCraftEngine.Result c = E.apply(cru, Action.BEAST, "influence:crusader", rng);
      assertThat(c.error()).isNull();
      assertThat(c.item().influence()).isEqualTo("crusader");
      got.addAll(c.added());
    }
    assertThat(got).contains("CruLife1").anyMatch(id -> !id.startsWith("Cru"));
  }

  @Test
  void 위상_스킬은_제작_접미어_하나_제작대_한도와_같은_그룹을_본다() {
    PoeCraftDataService.VeiledMod cat =
        new PoeCraftDataService.VeiledMod(
            "suffix",
            20,
            List.of(1047),
            List.of("skill"),
            "of Farrul",
            "- 페룰",
            List.of("Grants Level 30 Aspect of the Cat Skill"),
            List.of(),
            List.of("30레벨 고양이의 위상 스킬 사용 가능"),
            List.of());
    PoeCraftDataService.VeiledMod crab =
        new PoeCraftDataService.VeiledMod(
            "suffix",
            20,
            List.of(1047),
            List.of("skill"),
            "of Craiceann",
            "- 크라칸",
            List.of("Grants Level 30 Aspect of the Crab Skill"),
            List.of(),
            List.of("30레벨 게의 위상 스킬 사용 가능"),
            List.of());
    PoeCraftEngine e =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return BASE.equals(poolKey) ? POOL : Map.of();
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return MODS.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }

              @Override
              public PoeCraftDataService.VeiledMod beastMod(String id) {
                return switch (id) {
                  case "GrantsCatAspectCrafted30" -> cat;
                  case "GrantsCrabAspectCrafted30" -> crab;
                  default -> null;
                };
              }
            });
    SplittableRandom rng = new SplittableRandom(1);
    Item normal = new Item("Gloves", "str_armour", "", 84, Rarity.NORMAL, List.of(), false);
    PoeCraftEngine.Result r = e.apply(normal, Action.BEAST, "aspect:cat", rng);
    assertThat(r.error()).isNull();
    assertThat(r.item().rarity()).isEqualTo(Rarity.MAGIC);
    assertThat(r.item().affixes()).singleElement().matches(Affix::crafted);
    // 매직은 접미 하나 — 자리부터 막힌다
    assertThat(e.apply(r.item(), Action.BEAST, "aspect:crab", rng).error()).contains("접미 자리");
    // 위상끼리 같은 그룹(레어 · 제작 옵션 3개 메타가 없어도 그룹이 먼저)
    Item rr = e.apply(rare(a("Life1")), Action.BEAST, "aspect:cat", rng).item();
    assertThat(e.apply(rr, Action.BEAST, "aspect:crab", rng).error()).contains("같은 그룹");
    Item withBench = rare(a("Life1"), new Affix("Bench1", "prefix", List.of(99), true, false));
    assertThat(e.apply(withBench, Action.BEAST, "aspect:cat", rng).error()).contains("제작 옵션");
    assertThat(
            e.apply(
                    rare(a("FireResist1"), a("ColdResist1"), a("Str1")),
                    Action.BEAST,
                    "aspect:cat",
                    rng)
                .error())
        .contains("접미 자리");
    Item flask = new Item("LifeFlask", "", "", 84, Rarity.MAGIC, List.of(), false);
    assertThat(e.apply(flask, Action.BEAST, "aspect:cat", rng).error()).contains("장비");
  }

  @Test
  void 기폭제는_장막_해제_옵션_태그도_보고_위상_스킬_레벨은_올리지_않는다() {
    PoeCraftDataService.VeiledMod jun =
        new PoeCraftDataService.VeiledMod(
            "prefix",
            60,
            List.of(77),
            List.of("life"),
            "Chosen",
            "선택된",
            List.of("+50 to maximum Life"),
            List.of(),
            List.of("최대 생명력 +50"),
            List.of());
    PoeCraftDataService.VeiledMod cat =
        new PoeCraftDataService.VeiledMod(
            "suffix",
            20,
            List.of(1047),
            List.of("skill"),
            "of Farrul",
            "- 페룰",
            List.of("Grants Level 30 Aspect of the Cat Skill"),
            List.of(),
            List.of(),
            List.of());
    PoeCraftEngine e =
        new PoeCraftEngine(
            new PoeCraftEngine.Data() {
              @Override
              public Map<String, Integer> pool(String poolKey) {
                return Map.of();
              }

              @Override
              public PoeCraftDataService.CraftMod mod(String id) {
                return MODS.get(id);
              }

              @Override
              public PoeCraftDataService.Essence essence(String itemClass, String slug) {
                return null;
              }

              @Override
              public PoeCraftDataService.VeiledMod veiledMod(String id) {
                return "JunLife".equals(id) ? jun : null;
              }

              @Override
              public PoeCraftDataService.VeiledMod beastMod(String id) {
                return "GrantsCatAspectCrafted30".equals(id) ? cat : null;
              }
            });
    Item ring =
        new Item(
            "Ring",
            "",
            "",
            84,
            Rarity.RARE,
            List.of(
                new Affix("JunLife", "prefix", List.of(77), false, false),
                new Affix("GrantsCatAspectCrafted30", "suffix", List.of(1047), true, false)),
            false);
    assertThat(e.boosted(ring.withQuality("resource:20"))).containsExactly("JunLife");
    // 우측(접미) 기폭제여도 위상 스킬 레벨은 오르지 않는다
    assertThat(e.boosted(ring.withQuality("suffix:20"))).isEmpty();
  }

  @Test
  void 감사_반영_엘드리치는_아이템_레벨_제한_없음_장막은_하나_영향력은_68부터() {
    SplittableRandom rng = new SplittableRandom(14);
    Item low =
        new Item(
            "Gloves", "str_armour", "", 60, Rarity.RARE, List.of(a("Life1"), a("Armour1")), false);
    assertThat(E.apply(low, Action.EMBER, "3", rng).error()).isNull();
    assertThat(E.apply(low, Action.CONQUEROR, "crusader", rng).error())
        .isEqualTo("아이템 레벨이 68 미만이므로 영향력 속성 부여를 추가할 수 없습니다.");
    Item lowInf =
        new Item(
            "Gloves",
            "str_armour",
            "crusader",
            60,
            Rarity.RARE,
            List.of(a("Life1"), a("Armour1")),
            false);
    assertThat(E.apply(lowInf, Action.AWAKENER, "eyrie", rng).error())
        .isEqualTo("아이템 레벨이 68 미만이므로 영향력 속성 부여를 추가할 수 없습니다.");
    Item it = rare(a("Life1"), a("Armour1"), a("Str1"), a("ColdResist1"));
    Item ex = E.apply(it, Action.VEILED_EXALT, null, rng).item();
    assertThat(E.apply(ex, Action.VEILED_EXALT, null, rng).error()).isEqualTo("이미 장막 속성 부여가 있습니다.");
  }

  @Test
  void 조합_감사_화석_에센스_메타_거절_분열_영향력_거절_짐승_잠금_소멸_금지_정제_분열() {
    SplittableRandom rng = new SplittableRandom(21);
    Item locked =
        rare(a("Life1"), a("FireResist1"), craft("StrMasterItemGenerationCannotChangePrefixes"));
    // 화석 · 에센스 + 메타 옵션 = 인게임 오류(ItemErrorItemHasCraftingMetaMod)
    String metaErr = "속성의 결과에 영향을 미치는 속성 부여가 포함된 아이템에는 해당 화폐를 사용할 수 없습니다.";
    assertThat(E.apply(locked, Action.FOSSIL, "any", rng).error()).isEqualTo(metaErr);
    assertThat(E.apply(locked, Action.ESSENCE, "any", rng).error())
        .isIn(metaErr, "이 아이템 종류에 쓸 수 없는 에센스입니다");
    // 분열 아이템 + 정복자 엑잘 · 각성자 = 인게임 오류(ItemErrorItemIsFractured)
    Item fr =
        rare(
            new Affix("Life1", "prefix", List.of(1), false, true),
            a("FireResist1"),
            a("Str1"),
            a("Armour1"));
    assertThat(E.apply(fr, Action.CONQUEROR, "crusader", rng).error())
        .isEqualTo("아이템에 분열된 속성 부여가 적용되어 있습니다.");
    Item frInf = new Item("Gloves", "str_armour", "crusader", 84, Rarity.RARE, fr.affixes(), false);
    assertThat(E.apply(frInf, Action.AWAKENER, "eyrie", rng).error())
        .isEqualTo("아이템에 분열된 속성 부여가 적용되어 있습니다.");
    // 짐승 맞바꾸기: 지울 쪽(접두)이 잠겨 있으면 지우지 않고 접미만 붙인다
    PoeCraftEngine.Result b = E.apply(locked, Action.BEAST, "swap-suffix", rng);
    assertThat(b.error()).isNull();
    assertThat(b.removed()).isEmpty();
    assertThat(b.item().affixes()).contains(a("Life1"));
    // 정제: 분열 옵션 하나만 남으면 매직
    Item fr1 =
        rare(new Affix("Life1", "prefix", List.of(1), false, true), a("FireResist1"), a("Str1"));
    Item sc = E.apply(fr1, Action.SCOUR, null, rng).item();
    assertThat(sc.rarity()).isEqualTo(Rarity.MAGIC);
    assertThat(sc.affixes()).hasSize(1);
  }

  @Test
  void 수확_같은_유형_재련은_메타_공격_금지를_무시() {
    // CoE 메타 표: 'Harvest, Reforge more likely' 는 공격 · 주문 금지 'NO' — 공격 옵션도 다시 나온다(C431)
    // 접미 잠금이 있어야 금지 메타가 재련 뒤에도 남는다(C433 검토 — 전엔 잠금이 없어 표식 없이도 통과했다). 판정은 새로 붙은 옵션으로
    Item it =
        rare(
            a("Life1"),
            a("AttackDmg1"),
            craft("DexMasterItemGenerationCannotChangeSuffixes"),
            craft("IntMasterItemGenerationCannotRollAttackAffixes"));
    SplittableRandom rng = new SplittableRandom(31);
    boolean sawAttack = false;
    for (int i = 0; i < 3_000 && !sawAttack; i++) {
      PoeCraftEngine.Result r = E.apply(it, Action.HARVEST, "ReforgeMore", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().affixes())
          .contains(craft("IntMasterItemGenerationCannotRollAttackAffixes"));
      sawAttack =
          r.added().stream()
              .anyMatch(id -> MODS.containsKey(id) && MODS.get(id).t().contains("attack"));
    }
    assertThat(sawAttack).isTrue();
  }

  @Test
  void 수확_X_포함_재련도_메타_공격_금지를_무시() {
    // 접미 잠금 + 공격 금지(접미) → 재련해도 공격 금지가 남는다. CoE 'Reroll' 행 공격 'NO' — 공격 포함 재련이 그대로 붙는다(C432)
    Item it =
        rare(
            a("Life1"),
            craft("DexMasterItemGenerationCannotChangeSuffixes"),
            craft("IntMasterItemGenerationCannotRollAttackAffixes"));
    SplittableRandom rng = new SplittableRandom(32);
    for (int i = 0; i < 200; i++) {
      PoeCraftEngine.Result r = E.apply(it, Action.HARVEST, "ReforgeAttack", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().affixes())
          .contains(craft("IntMasterItemGenerationCannotRollAttackAffixes"));
      assertThat(r.added())
          .anyMatch(id -> MODS.containsKey(id) && MODS.get(id).t().contains("attack"));
    }
  }
}
