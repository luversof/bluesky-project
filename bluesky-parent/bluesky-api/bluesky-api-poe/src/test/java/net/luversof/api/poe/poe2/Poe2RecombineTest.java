package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.poe2.Poe2CraftEngine.Affix;
import net.luversof.api.poe.poe2.Poe2CraftEngine.DesecratedMod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Mod;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Result;

/** PoE2 재조합기(10-17 C407) — 재료 조건 · 고르기 한도 · 성공 확률(행운) · 바탕 무작위 · 바탕 쪽 분열 옵션. */
class Poe2RecombineTest {

  private static final String POOL = "Gloves|Armour";
  private static final String POOL_EV = "Gloves|Evasion";
  private static final Map<String, Mod> MODS = new HashMap<>();
  private static final List<Mod> LIST = new ArrayList<>();

  static {
    // 접두 6계열(그룹 1~6) · 접미 6계열(그룹 11~16) + 훼손 접두 2개(그룹 21 · 22)
    for (int g = 1; g <= 6; g++) {
      put(new Mod("P" + g, "prefix", 40, List.of(g), List.of()));
      put(new Mod("S" + g, "suffix", 40, List.of(10 + g), List.of()));
    }
  }

  private static void put(Mod m) {
    MODS.put(m.id(), m);
    LIST.add(m);
  }

  private static final Mod D1 = new Mod("D1", "prefix", 65, List.of(21), List.of());
  private static final Mod D2 = new Mod("D2", "prefix", 65, List.of(22), List.of());

  private static final Poe2CraftEngine E =
      new Poe2CraftEngine(
          new Poe2CraftEngine.Data() {
            @Override
            public List<Mod> pool(String poolKey) {
              return LIST;
            }

            @Override
            public Mod mod(String id) {
              return MODS.get(id);
            }

            @Override
            public List<DesecratedMod> desecrated(String poolKey) {
              return List.of(new DesecratedMod(D1, null), new DesecratedMod(D2, null));
            }
          });

  private static Affix a(String id) {
    Mod m = "D1".equals(id) ? D1 : "D2".equals(id) ? D2 : MODS.get(id);
    return new Affix(m.id(), m.gen(), m.level(), m.groups(), false);
  }

  private static Affix fr(String id) {
    Affix x = a(id);
    return new Affix(x.id(), x.gen(), x.level(), x.groups(), true);
  }

  private static Item rare(String pool, int ilvl, Affix... as) {
    return new Item(pool, ilvl, Rarity.RARE, List.of(as), false);
  }

  private static Result run(Item l, Item r, Set<String> li, Set<String> ri, int chance, long seed) {
    return E.recombine(l, r, li, ri, chance, false, new SplittableRandom(seed));
  }

  @Test
  void 재료_조건() {
    Item l = rare(POOL, 82, a("P1"), a("S1"));
    Item r = rare(POOL, 75, a("P2"), a("S2"));
    Item corrupt = new Item(POOL, 82, Rarity.RARE, List.of(a("P2")), true);
    Item normal = new Item(POOL, 82, Rarity.NORMAL, List.of(), false);
    Item sanct = new Item(POOL, 82, Rarity.RARE, List.of(a("P2")), false, null, true);
    for (Item bad : List.of(corrupt, normal, sanct)) {
      assertThat(run(l, bad, Set.of("P1"), Set.of("P2"), 100, 1).error())
          .isEqualTo("아이템을 재조합기에서 사용할 수 없습니다");
    }
    assertThat(run(l, rare("Ring|기본", 82, a("P2")), Set.of("P1"), Set.of("P2"), 100, 1).error())
        .contains("같은 종류");
    // 같은 클래스의 다른 변형(방어도 · 회피 장갑)은 된다
    assertThat(run(l, rare(POOL_EV, 82, a("P2")), Set.of("P1"), Set.of("P2"), 100, 1).error())
        .isNull();
    assertThat(run(l, null, Set.of("P1"), Set.of(), 100, 1).error()).isNotNull();
    assertThat(run(l, r, Set.of("P1"), Set.of("P2"), 100, 1).error()).isNull();
  }

  @Test
  void 고르기_한도() {
    Item l = rare(POOL, 82, a("P1"), a("P2"), a("S1"), a("S2"));
    Item r =
        rare(
            POOL,
            82,
            a("P3"),
            a("P4"),
            a("S3"),
            new Affix("UnrevealedS#0", "suffix", 65, List.of(), false));
    assertThat(run(l, r, Set.of("P1"), Set.of(), 100, 1).error()).contains("하나 이상");
    assertThat(run(l, r, Set.of("P1", "P2"), Set.of("P3", "P4"), 100, 1).error())
        .isEqualTo("접두어 속성 한도 도달");
    assertThat(run(l, r, Set.of("UnknownId"), Set.of("P3"), 100, 1).error())
        .isEqualTo("재조합에 선택할 수 없는 속성");
    assertThat(run(l, r, Set.of("P1"), Set.of("UnrevealedS#0"), 100, 1).error())
        .isEqualTo("재조합에 선택할 수 없는 속성");
    // 같은 그룹(같은 옵션)은 양쪽에서 하나만
    Item r2 = rare(POOL, 82, a("P1"), a("S3"));
    assertThat(run(l, r2, Set.of("P1"), Set.of("P1"), 100, 1).error()).contains("호환되지 않습니다");
    // 훼손 옵션은 하나만
    Item ld = rare(POOL, 82, a("D1"), a("S1"));
    Item rd = rare(POOL, 82, a("D2"), a("S2"));
    assertThat(run(ld, rd, Set.of("D1"), Set.of("D2"), 100, 1).error())
        .isEqualTo("훼손된 속성은 1개만 선택할 수 있습니다.");
    assertThat(run(ld, rd, Set.of("D1"), Set.of("S2"), 100, 1).error()).isNull();
    // 전체 6개 = 접두 3 + 접미 3 은 된다
    Item l6 = rare(POOL, 82, a("P1"), a("P2"), a("S1"), a("S2"));
    Item r6 = rare(POOL, 82, a("P3"), a("S3"));
    Result ok = run(l6, r6, Set.of("P1", "P2", "S1", "S2"), Set.of("P3", "S3"), 100, 1);
    assertThat(ok.error()).isNull();
    assertThat(ok.item().affixes()).hasSize(6);
  }

  @Test
  void 성공하면_고른_옵션만_남는_레어_바탕은_둘_중_무작위() {
    Item l = new Item(POOL, 82, Rarity.MAGIC, List.of(a("P1"), a("S1")), false, "life:20");
    Item r = rare(POOL_EV, 70, a("P2"), a("P3"), a("S2"));
    int left = 0;
    int right = 0;
    for (long s = 0; s < 400; s++) {
      Result res = run(l, r, Set.of("P1"), Set.of("P2", "S2"), 100, s);
      assertThat(res.error()).isNull();
      Item it = res.item();
      assertThat(it.rarity()).isEqualTo(Rarity.RARE);
      assertThat(it.affixes()).extracting(Affix::id).containsExactlyInAnyOrder("P1", "P2", "S2");
      if (res.note().startsWith("recombine:ok:L")) {
        left++;
        assertThat(it.poolKey()).isEqualTo(POOL);
        assertThat(it.ilvl()).isEqualTo(82);
        assertThat(it.quality()).isEqualTo("life:20");
      } else {
        right++;
        assertThat(it.poolKey()).isEqualTo(POOL_EV);
        assertThat(it.ilvl()).isEqualTo(70);
        assertThat(it.quality()).isNull();
      }
    }
    assertThat(left).isBetween(150, 250);
    assertThat(right).isBetween(150, 250);
  }

  @Test
  void 실패하면_둘_다_사라지고_징조는_행운() {
    Item l = rare(POOL, 82, a("P1"), a("S1"));
    Item r = rare(POOL, 82, a("P2"), a("S2"));
    int ok = 0;
    int luckyOk = 0;
    int n = 4000;
    for (long s = 0; s < n; s++) {
      Result res = run(l, r, Set.of("P1"), Set.of("S2"), 30, s);
      if (res.note().startsWith("recombine:ok")) {
        ok++;
      } else {
        assertThat(res.note()).isEqualTo("recombine:fail:30");
        assertThat(res.item().rarity()).isEqualTo(Rarity.NORMAL);
        assertThat(res.item().affixes()).isEmpty();
      }
      Result lr = E.recombine(l, r, Set.of("P1"), Set.of("S2"), 30, true, new SplittableRandom(s));
      assertThat(lr.omensUsed()).containsExactly("Recombination");
      if (lr.note().startsWith("recombine:ok")) {
        luckyOk++;
      }
    }
    // 30% · 행운 1 - 0.7² = 51%
    assertThat(ok / (double) n).isBetween(0.27, 0.33);
    assertThat(luckyOk / (double) n).isBetween(0.48, 0.54);
  }

  @Test
  void 바탕_쪽_분열_옵션은_고르지_않아도_남는다() {
    Item l = rare(POOL, 82, fr("P4"), a("P1"), a("S1"));
    Item r = rare(POOL, 82, fr("S5"), a("P2"), a("P3"), a("S2"));
    boolean sawL = false;
    boolean sawR = false;
    for (long s = 0; s < 60; s++) {
      Result res = run(l, r, Set.of("P1"), Set.of("P2", "P3"), 100, s);
      List<String> ids = res.item().affixes().stream().map(Affix::id).toList();
      if (res.note().startsWith("recombine:ok:L")) {
        sawL = true;
        // 접두 한도 3 — 분열 P4 + 고른 P1 이 바탕 쪽, 다른 쪽 P2 · P3 중 하나만 들어간다
        assertThat(ids).contains("P4", "P1").doesNotContain("S5");
        assertThat(res.item().affixes().stream().filter(x -> "prefix".equals(x.gen())).count())
            .isEqualTo(3);
        assertThat(res.note()).endsWith(":1");
      } else {
        sawR = true;
        assertThat(ids).containsExactlyInAnyOrder("S5", "P1", "P2", "P3");
        assertThat(res.item().affixes())
            .filteredOn(Affix::fractured)
            .extracting(Affix::id)
            .containsExactly("S5");
      }
    }
    assertThat(sawL).isTrue();
    assertThat(sawR).isTrue();
  }

  @Test
  void 주얼은_거절하고_매직끼리도_레어() {
    // 주얼은 재조합기 대상이 아니다(C434 — poe2db Recombinator "weapons, armour, or jewellery"). 전엔 한도 2/2 로 받았다
    String jewel = "Jewel|Ruby";
    Item l = rare(jewel, 82, a("P1"), a("P2"), a("S1"));
    Item r = rare(jewel, 82, a("P3"), a("S2"), a("S3"));
    assertThat(run(l, r, Set.of("P1", "S1"), Set.of("P3", "S2"), 100, 1).error())
        .isEqualTo("아이템을 재조합기에서 사용할 수 없습니다");
    Item m1 = new Item(POOL, 82, Rarity.MAGIC, List.of(a("P1")), false);
    Item m2 = new Item(POOL, 82, Rarity.MAGIC, List.of(a("S2")), false);
    Result mm = run(m1, m2, Set.of("P1"), Set.of("S2"), 100, 1);
    assertThat(mm.item().rarity()).isEqualTo(Rarity.RARE);
    assertThat(mm.item().affixes()).hasSize(2);
  }

  @Test
  void 바탕_쪽_분열_훼손_옵션이_있으면_다른_쪽_훼손_옵션은_빠진다() {
    Item l = rare(POOL, 82, fr("D1"), a("P1"), a("S1"));
    Item r = rare(POOL, 82, a("D2"), a("S2"));
    boolean sawL = false;
    for (long s = 0; s < 60; s++) {
      Result res = run(l, r, Set.of("P1"), Set.of("D2", "S2"), 100, s);
      assertThat(res.error()).isNull();
      List<String> ids = res.item().affixes().stream().map(Affix::id).toList();
      if (res.note().startsWith("recombine:ok:L")) {
        sawL = true;
        assertThat(ids).contains("D1", "P1", "S2").doesNotContain("D2");
        assertThat(res.note()).endsWith(":1");
      } else {
        assertThat(ids).containsExactlyInAnyOrder("D2", "S2", "P1");
      }
    }
    assertThat(sawL).isTrue();
  }

  @Test
  void 다른_쪽에서_고른_분열_옵션은_분열째_옮긴다() {
    Item l = rare(POOL, 82, a("P1"), a("S1"));
    Item r = rare(POOL, 82, fr("S2"), a("P2"));
    for (long s = 0; s < 20; s++) {
      Result res = run(l, r, Set.of("P1"), Set.of("S2"), 100, s);
      assertThat(res.item().affixes())
          .filteredOn(Affix::fractured)
          .extracting(Affix::id)
          .containsExactly("S2");
    }
  }
}
