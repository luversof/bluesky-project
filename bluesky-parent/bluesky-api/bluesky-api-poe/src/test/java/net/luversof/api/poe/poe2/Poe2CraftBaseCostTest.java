package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 베이스 값(10-23 Q20b, craftofexile 'Base cost') — 실제 추출 데이터 · 시세로. 없으면 건너뛴다. */
class Poe2CraftBaseCostTest {

  private Poe2CraftService craft;
  private Poe2CraftCalcService calc;
  private Poe2CraftEngine.Item normal;
  private List<Poe2CraftCalcService.Target> targets;

  @BeforeEach
  void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata/poe2";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft2-mods.json")), "추출 데이터 없음");
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "ninja", "craft-prices2.json")), "시세 없음");
    Poe2DataService data2 = new Poe2DataService(dir, new Poe2TradeStatDataService(dir));
    craft = new Poe2CraftService(data2, dir);
    calc = new Poe2CraftCalcService(craft, dir);
    normal =
        new Poe2CraftEngine.Item(
            "Gloves|Armour", 82, Poe2CraftEngine.Rarity.NORMAL, List.of(), false);
    List<String> prefixes =
        craft
            .pool(
                new Poe2CraftEngine.Item(
                    "Gloves|Armour", 82, Poe2CraftEngine.Rarity.RARE, List.of(), false))
            .stream()
            .filter(l -> "prefix".equals(l.gen()))
            .map(Poe2CraftService.PoolLine::id)
            .toList();
    targets = List.of(new Poe2CraftCalcService.Target(prefixes));
  }

  @AfterEach
  void tearDown() {
    Poe2CraftCalcService.override(null);
  }

  @Test
  void 베이스_값을_넣으면_새_베이스_방법마다_시도_수만큼_더한다() {
    Poe2CraftCalcService.Response off =
        calc.calc(new Poe2CraftCalcService.Request(normal, targets, 200));
    Poe2CraftCalcService.override(Map.of(Poe2CraftCalcService.BASE_ITEM, 0.5));
    Poe2CraftCalcService.Response on =
        calc.calc(new Poe2CraftCalcService.Request(normal, targets, 200));
    assertThat(on.results()).isNotEmpty();
    int checked = 0;
    for (Poe2CraftCalcService.Method m : on.results()) {
      if (m.successes() == 0 || m.costPerSuccess() == null) {
        continue;
      }
      double bases = (double) m.runs() / m.successes();
      if (m.method().startsWith("RECOMB")) {
        continue;
      }
      assertThat(m.perSuccess().get(Poe2CraftCalcService.BASE_ITEM)).isCloseTo(bases, within(1e-9));
      java.util.Map<String, Double> noBase = new java.util.HashMap<>(m.perSuccess());
      noBase.remove(Poe2CraftCalcService.BASE_ITEM);
      assertThat(m.costPerSuccess() - calc.cost(noBase)).isCloseTo(0.5 * bases, within(1e-6));
      checked++;
    }
    assertThat(checked).isGreaterThanOrEqualTo(3);
    assertThat(off.results())
        .allSatisfy(
            m -> assertThat(m.perSuccess()).doesNotContainKey(Poe2CraftCalcService.BASE_ITEM));
  }

  @Test
  void 지금_아이템에서_시작하면_베이스는_더하지_않는다() {
    Poe2CraftCalcService.override(Map.of(Poe2CraftCalcService.BASE_ITEM, 0.5));
    Poe2CraftEngine.Item rare =
        craft
            .engine()
            .apply(
                normal,
                Poe2CraftEngine.Action.ALCHEMY,
                "",
                java.util.Set.of(),
                new java.util.SplittableRandom(1))
            .item();
    Poe2CraftCalcService.Response r =
        calc.calc(new Poe2CraftCalcService.Request(rare, targets, 100));
    assertThat(r.results()).isNotEmpty();
    assertThat(r.results())
        .allSatisfy(
            m -> assertThat(m.perSuccess()).doesNotContainKey(Poe2CraftCalcService.BASE_ITEM));
  }

  @Test
  void 전략은_새_베이스에서_시작하면_회차마다_베이스_하나() {
    Poe2CraftCalcService.override(Map.of(Poe2CraftCalcService.BASE_ITEM, 0.5));
    Poe2CraftCalcService.StrategyResult r =
        calc.strategy(
            new Poe2CraftCalcService.StrategyRequest(
                normal,
                targets,
                List.of(new Poe2CraftCalcService.Step("ALCHEMY", null, null, null, null, null)),
                100));
    assertThat(r.perRun().get(Poe2CraftCalcService.BASE_ITEM)).isEqualTo(1.0);
  }
}
