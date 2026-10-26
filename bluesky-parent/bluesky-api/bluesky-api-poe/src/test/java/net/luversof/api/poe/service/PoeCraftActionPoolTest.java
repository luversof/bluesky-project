package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 고른 화폐 기준 옵션 표(10-22 P3 — 화석 조합 첫 옵션 확률 C504 를 합침) — 실제 추출 데이터로. 없으면 건너뛴다. 엔진의 첫 pick 가중치를 그대로
 * 받으므로 화석 조합은 계산기가 쓰는 fossilEligible 과 같아야 한다.
 */
class PoeCraftActionPoolTest {

  private PoeCraftService craft;
  private PoeCraftEngine.Item item;

  @BeforeEach
  void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-currencies.json")), "추출 데이터 없음");
    craft = new PoeCraftService(new PoeCraftDataService(dir), new PoeModDataService(dir));
    item =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
  }

  private static double share(List<PoeCraftService.PoolLine> pool, String prefix) {
    return pool.stream()
        .filter(p -> p.id().startsWith(prefix))
        .mapToDouble(PoeCraftService.PoolLine::chance)
        .sum();
  }

  @Test
  void 온전한_화석은_생명력을_늘리고_방어를_막는다_그을린_화석은_냉기를_막는다() {
    List<PoeCraftService.PoolLine> plain = craft.pool(item, PoeCraftEngine.Rarity.RARE);
    PoeCraftService.ActionPool life =
        craft.actionPool(item, PoeCraftEngine.Action.FOSSIL, "CurrencyDelveCraftingLife");
    assertThat(life.picked()).isTrue();
    assertThat(life.baseRarity()).isEqualTo("RARE");
    // 생명력 x10 → 비율이 크게 오른다(배수 10 으로 표시), 방어(defences) x0 → 방어도 증가 옵션은 0
    assertThat(share(life.lines(), "IncreasedLife"))
        .isGreaterThan(share(plain, "IncreasedLife") * 3);
    assertThat(life.mult()).containsEntry("IncreasedLife1", 10.0);
    assertThat(life.lines())
        .noneMatch(p -> p.id().startsWith("LocalIncreasedPhysicalDamageReductionRating"));
    assertThat(life.lines().stream().mapToDouble(PoeCraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, within(1e-6));
    PoeCraftService.ActionPool fire =
        craft.actionPool(item, PoeCraftEngine.Action.FOSSIL, "CurrencyDelveCraftingFire");
    assertThat(fire.lines()).noneMatch(p -> p.id().startsWith("ColdResist"));
    assertThat(share(fire.lines(), "FireResist")).isGreaterThan(share(plain, "FireResist"));
  }

  @Test
  void 화석_조합은_계산기의_fossilEligible_과_같다() {
    for (String slugs :
        List.of(
            "CurrencyDelveCraftingLife",
            "CurrencyDelveCraftingFire,CurrencyDelveCraftingCold",
            "CurrencyDelveCraftingLuckyModRolls")) {
      Map<String, Double> calc = craft.engine().fossilEligible(item, slugs);
      PoeCraftEngine.PickProbe p =
          craft.engine().probeFirstPick(item, PoeCraftEngine.Action.FOSSIL, slugs);
      assertThat(p.weights()).as(slugs).isNotNull();
      assertThat(p.weights().keySet()).as(slugs).isEqualTo(calc.keySet());
      calc.forEach(
          (id, w) ->
              assertThat(p.weights().get(id)[0]).as(slugs + " " + id).isCloseTo(w, within(1e-9)));
    }
  }

  @Test
  void 카오스는_배수_없이_레어로_다시_뽑고_소멸은_표를_뽑지_않는다() {
    PoeCraftService.ActionPool chaos =
        craft.actionPool(
            new PoeCraftEngine.Item(
                "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.RARE, List.of(), false),
            PoeCraftEngine.Action.CHAOS,
            null);
    assertThat(chaos.picked()).isTrue();
    assertThat(chaos.mult()).isEmpty();
    assertThat(chaos.lines().stream().mapToDouble(PoeCraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, within(1e-6));
    PoeCraftService.ActionPool annul =
        craft.actionPool(
            new PoeCraftEngine.Item(
                "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.RARE, List.of(), false),
            PoeCraftEngine.Action.ANNUL,
            null);
    assertThat(annul.picked()).isFalse();
    assertThat(annul.lines()).isEmpty();
  }

  @Test
  void 못_쓰는_화폐는_사유를_준다() {
    // 일반 아이템에 엑잘 — 엔진 사유 그대로, 표는 없음
    PoeCraftService.ActionPool ex = craft.actionPool(item, PoeCraftEngine.Action.EXALT, null);
    assertThat(ex.picked()).isFalse();
    assertThat(ex.error()).isNotBlank();
  }

  @Test
  void 가득_찬_레어의_카오스는_양쪽_다_보이고_합은_1() {
    // 연금으로 레어를 만든 뒤 엑잘을 채워 6옵션 — 카오스(전부 다시 굴림)는 두 씨앗이 같아 한 번으로, 확률 합 1
    PoeCraftEngine.Item rare = item;
    java.util.SplittableRandom rng = new java.util.SplittableRandom(7);
    rare = craft.engine().apply(rare, PoeCraftEngine.Action.ALCHEMY, null, rng).item();
    for (int i = 0; i < 6; i++) {
      PoeCraftEngine.Result r = craft.engine().apply(rare, PoeCraftEngine.Action.EXALT, null, rng);
      if (r.error() == null) {
        rare = r.item();
      }
    }
    PoeCraftService.ActionPool chaos = craft.actionPool(rare, PoeCraftEngine.Action.CHAOS, null);
    assertThat(chaos.picked()).isTrue();
    assertThat(chaos.lines()).anyMatch(l -> "prefix".equals(l.gen()));
    assertThat(chaos.lines()).anyMatch(l -> "suffix".equals(l.gen()));
    assertThat(chaos.lines().stream().mapToDouble(PoeCraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, within(1e-6));
  }
}
