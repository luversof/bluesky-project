package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 고른 화폐 기준 옵션 표(10-22 Q2, PoE1 PoeCraftActionPoolTest 짝) — 실제 추출 데이터로. 없으면 건너뛴다. 엔진이 처음 만든 후보를 그대로
 * 받으므로 징조 · 등급 없는 엑잘은 지금 표(pool)와 같은 옵션이어야 한다.
 */
class Poe2CraftActionPoolTest {

  private Poe2CraftService craft;
  private Poe2CraftEngine.Item rare;

  @BeforeEach
  void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata/poe2";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft2-mods.json")), "추출 데이터 없음");
    Poe2DataService data2 = new Poe2DataService(dir, new Poe2TradeStatDataService(dir));
    craft = new Poe2CraftService(data2, dir);
    rare =
        new Poe2CraftEngine.Item(
            "Gloves|Armour", 82, Poe2CraftEngine.Rarity.RARE, List.of(), false);
  }

  private static Set<String> ids(List<Poe2CraftService.PoolLine> lines) {
    return lines.stream().map(Poe2CraftService.PoolLine::id).collect(Collectors.toSet());
  }

  @Test
  void 엑잘은_지금_표와_같은_후보_균등() {
    Poe2CraftService.ActionPool ex =
        craft.actionPool(rare, Poe2CraftEngine.Action.EXALT, null, List.of());
    assertThat(ex.picked()).isTrue();
    assertThat(ex.mult()).isEmpty();
    assertThat(ids(ex.lines())).isEqualTo(ids(craft.pool(rare)));
    assertThat(ex.lines().stream().mapToDouble(Poe2CraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, within(1e-9));
  }

  @Test
  void 좌측_찬미는_접두만_완벽한_엑잘은_낮은_등급을_뺀다() {
    Poe2CraftService.ActionPool sin =
        craft.actionPool(rare, Poe2CraftEngine.Action.EXALT, null, List.of("SinistralExaltation"));
    assertThat(sin.lines()).isNotEmpty().allMatch(l -> "prefix".equals(l.gen()));
    Poe2CraftService.ActionPool plain =
        craft.actionPool(rare, Poe2CraftEngine.Action.EXALT, null, List.of());
    Poe2CraftService.ActionPool perfect =
        craft.actionPool(rare, Poe2CraftEngine.Action.EXALT, "perfect", List.of());
    assertThat(perfect.lines().size()).isLessThan(plain.lines().size());
    assertThat(ids(plain.lines())).containsAll(ids(perfect.lines()));
  }

  @Test
  void 소멸은_표를_고르지_않는다() {
    Poe2CraftService.ActionPool annul =
        craft.actionPool(rare, Poe2CraftEngine.Action.ANNUL, null, List.of());
    assertThat(annul.picked()).isFalse();
    assertThat(annul.lines()).isEmpty();
  }

  @Test
  void 가득_찬_레어의_카오스는_지울_쪽을_평균해_양쪽_다_보인다() {
    java.util.SplittableRandom rng = new java.util.SplittableRandom(7);
    Poe2CraftEngine.Item full = rare;
    for (int i = 0; i < 6; i++) {
      Poe2CraftEngine.Result r =
          craft.engine().apply(full, Poe2CraftEngine.Action.EXALT, null, Set.of(), rng);
      if (r.error() == null) {
        full = r.item();
      }
    }
    assertThat(full.affixes()).hasSize(6);
    Poe2CraftService.ActionPool chaos =
        craft.actionPool(full, Poe2CraftEngine.Action.CHAOS, null, List.of());
    assertThat(chaos.picked()).isTrue();
    // 씨앗 하나면 지운 쪽 접두 · 접미 하나만 나왔다 — 평균이면 둘 다
    assertThat(chaos.lines()).anyMatch(l -> "prefix".equals(l.gen()));
    assertThat(chaos.lines()).anyMatch(l -> "suffix".equals(l.gen()));
    assertThat(chaos.lines().stream().mapToDouble(Poe2CraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, within(1e-6));
  }

  @Test
  void 에센스를_고르면_보장_옵션이_확정_목록에() {
    Poe2CraftService.EssenceOption e =
        craft.essences("Gloves|Armour").stream()
            .filter(x -> "normal".equals(x.kind()) && x.modId() != null)
            .findFirst()
            .orElseThrow();
    java.util.SplittableRandom rng = new java.util.SplittableRandom(3);
    Poe2CraftEngine.Item magic =
        craft
            .engine()
            .apply(
                new Poe2CraftEngine.Item(
                    "Gloves|Armour", 82, Poe2CraftEngine.Rarity.NORMAL, List.of(), false),
                Poe2CraftEngine.Action.TRANSMUTE,
                null,
                Set.of(),
                rng)
            .item();
    Poe2CraftService.ActionPool ap =
        craft.actionPool(magic, Poe2CraftEngine.Action.ESSENCE, e.slug(), List.of());
    assertThat(ap.forced()).containsExactly(e.modId());
  }
}
