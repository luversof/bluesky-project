package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.service.PoeCraftEngine.Action;
import net.luversof.api.poe.service.PoeCraftEngine.Affix;
import net.luversof.api.poe.service.PoeCraftEngine.Item;
import net.luversof.api.poe.service.PoeCraftEngine.Rarity;

/** 제작 엔진(10-15 C335) — 가짜 풀로 확률이 해석값과 맞는지, 자리 · 옵션 그룹 · 아이템 레벨 · 분열 규칙. */
class PoeCraftEngineTest {

  private static final String POOL = "Gloves|str_armour|";

  private static PoeCraftDataService.CraftMod mod(String gen, int ilvl, int group) {
    return new PoeCraftDataService.CraftMod(
        "F" + group, gen, ilvl, List.of(group), List.of(), null);
  }

  private static PoeCraftEngine engine(
      Map<String, PoeCraftDataService.CraftMod> mods, Map<String, Integer> pool) {
    return new PoeCraftEngine(
        new PoeCraftEngine.Data() {
          @Override
          public Map<String, Integer> pool(String poolKey) {
            return POOL.equals(poolKey) ? pool : Map.of();
          }

          @Override
          public PoeCraftDataService.CraftMod mod(String id) {
            return mods.get(id);
          }

          @Override
          public PoeCraftDataService.Essence essence(String itemClass, String slug) {
            return "ess".equals(slug)
                ? new PoeCraftDataService.Essence(
                    "Greed",
                    "E",
                    "E",
                    null,
                    7,
                    0,
                    "prefix",
                    "x",
                    "ESS",
                    List.of(9),
                    1,
                    "ess",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of())
                : null;
          }
        });
  }

  private static Item normal(int ilvl) {
    return new Item("Gloves", "str_armour", "", ilvl, Rarity.NORMAL, List.of(), false);
  }

  /** 작은 풀: 접두 A(100) · B(300) · 아이템 레벨 86 짜리 E(1000), 접미 C(200) · D(200) — D 는 C 와 같은 그룹. */
  private static PoeCraftEngine small() {
    Map<String, PoeCraftDataService.CraftMod> mods = new HashMap<>();
    mods.put("A", mod("prefix", 1, 1));
    mods.put("B", mod("prefix", 1, 2));
    mods.put("E", mod("prefix", 86, 5));
    mods.put("C", mod("suffix", 1, 3));
    mods.put("D", mod("suffix", 1, 3));
    Map<String, Integer> pool = new LinkedHashMap<>();
    pool.put("A", 100);
    pool.put("B", 300);
    pool.put("E", 1000);
    pool.put("C", 200);
    pool.put("D", 200);
    return engine(mods, pool);
  }

  @Test
  void 변화_옵션_하나는_가중치_비례_둘은_반반() {
    PoeCraftEngine e = small();
    SplittableRandom rng = new SplittableRandom(1);
    int n = 200_000, one = 0, onlyA = 0;
    for (int i = 0; i < n; i++) {
      Item it = e.apply(normal(84), Action.TRANSMUTE, null, rng).item();
      assertThat(it.rarity()).isEqualTo(Rarity.MAGIC);
      assertThat(it.affixes()).noneMatch(a -> a.id().equals("E")); // 아이템 레벨 86 옵션은 84 에 안 붙는다
      if (it.affixes().size() == 1) {
        one++;
        if (it.affixes().get(0).id().equals("A")) {
          onlyA++;
        }
      } else {
        assertThat(it.count("prefix")).isEqualTo(1); // 매직 두 옵션 = 접두 1 · 접미 1
        assertThat(it.count("suffix")).isEqualTo(1);
      }
    }
    assertThat((double) one / n).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
    // 하나짜리에서 A = 100 / (100 + 300 + 200 + 200)
    assertThat((double) onlyA / one)
        .isCloseTo(100.0 / 800, org.assertj.core.data.Offset.offset(0.005));
  }

  @Test
  void 같은_그룹은_같이_안_붙고_자리가_차면_멈춘다() {
    PoeCraftEngine e = small();
    SplittableRandom rng = new SplittableRandom(2);
    for (int i = 0; i < 20_000; i++) {
      Item it = e.apply(normal(84), Action.ALCHEMY, null, rng).item();
      // 붙을 수 있는 게 A · B · (C 또는 D) 셋뿐 — 4~6 을 원해도 3 에서 멈춘다
      assertThat(it.affixes()).hasSize(3);
      assertThat(it.affixes().stream().filter(a -> a.id().equals("C") || a.id().equals("D")))
          .hasSize(1);
    }
    // 아이템 레벨 86 이면 E 도 붙는다
    Item hi = e.apply(normal(86), Action.ALCHEMY, null, new SplittableRandom(3)).item();
    assertThat(hi.affixes()).hasSize(4); // A · B · E + (C 또는 D) — 옵션 수 목표는 4 이상이라 붙을 수 있는 넷 전부
    assertThat(hi.affixes()).anyMatch(a -> a.id().equals("E"));
  }

  @Test
  void 레어_옵션_수는_8_3_1() {
    Map<String, PoeCraftDataService.CraftMod> mods = new HashMap<>();
    Map<String, Integer> pool = new LinkedHashMap<>();
    for (int i = 0; i < 10; i++) {
      mods.put("P" + i, mod("prefix", 1, 100 + i));
      mods.put("S" + i, mod("suffix", 1, 200 + i));
      pool.put("P" + i, 100);
      pool.put("S" + i, 100);
    }
    PoeCraftEngine e = engine(mods, pool);
    SplittableRandom rng = new SplittableRandom(4);
    int[] c = new int[7];
    int n = 120_000;
    for (int i = 0; i < n; i++) {
      Item it = e.apply(normal(84), Action.ALCHEMY, null, rng).item();
      c[it.affixes().size()]++;
      assertThat(it.count("prefix")).isLessThanOrEqualTo(3);
      assertThat(it.count("suffix")).isLessThanOrEqualTo(3);
    }
    assertThat((double) c[4] / n).isCloseTo(8 / 12.0, org.assertj.core.data.Offset.offset(0.01));
    assertThat((double) c[5] / n).isCloseTo(3 / 12.0, org.assertj.core.data.Offset.offset(0.01));
    assertThat((double) c[6] / n).isCloseTo(1 / 12.0, org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  void 분열_옵션은_카오스_소멸_정제가_안_건드린다() {
    PoeCraftEngine e = small();
    Affix frac = new Affix("A", "prefix", List.of(1), false, true);
    Item rare = new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(frac), false);
    SplittableRandom rng = new SplittableRandom(5);
    for (int i = 0; i < 2_000; i++) {
      Item it = e.apply(rare, Action.CHAOS, null, rng).item();
      assertThat(it.affixes()).contains(frac);
      Item an = e.apply(it, Action.ANNUL, null, rng).item();
      assertThat(an.affixes()).contains(frac);
    }
    Item sc = e.apply(rare, Action.SCOUR, null, rng).item();
    assertThat(sc.affixes()).containsExactly(frac);
    // 분열만 남으면 소멸은 지울 게 없다
    assertThat(e.apply(rare, Action.ANNUL, null, rng).error()).isNotNull();
  }

  @Test
  void 에센스는_보장_옵션을_넣고_그_그룹을_막는다() {
    PoeCraftEngine e = small();
    SplittableRandom rng = new SplittableRandom(6);
    for (int i = 0; i < 5_000; i++) {
      PoeCraftEngine.Result r = e.apply(normal(84), Action.ESSENCE, "ess", rng);
      assertThat(r.error()).isNull();
      assertThat(r.item().rarity()).isEqualTo(Rarity.RARE);
      assertThat(r.item().affixes()).anyMatch(a -> a.id().equals("ESS"));
    }
    assertThat(e.apply(normal(84), Action.ESSENCE, "nope", rng).error()).isNotNull();
  }

  @Test
  void 쓸_수_없는_화폐는_거절하고_아이템은_그대로() {
    PoeCraftEngine e = small();
    SplittableRandom rng = new SplittableRandom(7);
    Item n = normal(84);
    assertThat(e.apply(n, Action.CHAOS, null, rng).error()).isNotNull();
    assertThat(e.apply(n, Action.CHAOS, null, rng).item()).isEqualTo(n);
    assertThat(e.apply(n, Action.ALTERATION, null, rng).error()).isNotNull();
    Item corrupt = new Item("Gloves", "str_armour", "", 84, Rarity.RARE, List.of(), true);
    assertThat(e.apply(corrupt, Action.CHAOS, null, rng).error()).contains("타락");
    // 매직 두 옵션이면 확장 불가, 한 옵션이면 남은 쪽 자리에 붙는다
    Item magic1 =
        new Item(
            "Gloves",
            "str_armour",
            "",
            84,
            Rarity.MAGIC,
            List.of(new Affix("A", "prefix", List.of(1), false, false)),
            false);
    Item aug = e.apply(magic1, Action.AUGMENTATION, null, rng).item();
    assertThat(aug.count("suffix")).isEqualTo(1);
    assertThat(e.apply(aug, Action.AUGMENTATION, null, rng).error()).isNotNull();
    // 제왕 = 매직 → 레어 + 하나
    Item regal = e.apply(aug, Action.REGAL, null, rng).item();
    assertThat(regal.rarity()).isEqualTo(Rarity.RARE);
    assertThat(regal.affixes()).hasSize(3);
  }
}
