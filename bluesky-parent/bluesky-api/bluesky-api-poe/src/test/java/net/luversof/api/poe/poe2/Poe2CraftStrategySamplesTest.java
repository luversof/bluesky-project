package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 전략 성공 예시(10-22 Q17, PoE1 Q16 짝) — 실제 추출 데이터로. 없으면 건너뛴다. */
class Poe2CraftStrategySamplesTest {

  private Poe2CraftService craft;
  private Poe2CraftCalcService calc;
  private Poe2CraftEngine.Item normal;

  @BeforeEach
  void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata/poe2";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft2-mods.json")), "추출 데이터 없음");
    Poe2DataService data2 = new Poe2DataService(dir, new Poe2TradeStatDataService(dir));
    craft = new Poe2CraftService(data2, dir);
    calc = new Poe2CraftCalcService(craft, dir);
    normal =
        new Poe2CraftEngine.Item(
            "Gloves|Armour", 82, Poe2CraftEngine.Rarity.NORMAL, List.of(), false);
  }

  @Test
  void 전략은_목표를_채운_아이템을_예시로_남긴다() {
    List<String> prefixes =
        craft
            .pool(
                new Poe2CraftEngine.Item(
                    "Gloves|Armour", 82, Poe2CraftEngine.Rarity.RARE, List.of(), false))
            .stream()
            .filter(l -> "prefix".equals(l.gen()))
            .map(Poe2CraftService.PoolLine::id)
            .toList();
    Poe2CraftCalcService.StrategyResult r =
        calc.strategy(
            new Poe2CraftCalcService.StrategyRequest(
                normal,
                List.of(new Poe2CraftCalcService.Target(prefixes)),
                List.of(new Poe2CraftCalcService.Step("ALCHEMY", null, null, null, null, null)),
                100));
    assertThat(r.successes()).isPositive();
    assertThat(r.samples()).hasSize(Math.min(r.successes(), Poe2CraftCalcService.STRATEGY_SAMPLES));
    for (Poe2CraftCalcService.StrategySample s : r.samples()) {
      assertThat(s.item().rarity()).isEqualTo(Poe2CraftEngine.Rarity.RARE);
      assertThat(s.lines()).hasSize(s.item().affixes().size());
      assertThat(s.item().affixes()).anyMatch(a -> prefixes.contains(a.id()));
    }
  }

  @Test
  void 전략은_목표를_다_채우면_남은_단계를_쓰지_않는다() {
    // 10-23 Q18 — 연금으로 접두 아무거나를 채우면 엑잘은 쓰지 않는다(전엔 늘 100% 도달)
    List<String> prefixes =
        craft
            .pool(
                new Poe2CraftEngine.Item(
                    "Gloves|Armour", 82, Poe2CraftEngine.Rarity.RARE, List.of(), false))
            .stream()
            .filter(l -> "prefix".equals(l.gen()))
            .map(Poe2CraftService.PoolLine::id)
            .toList();
    Poe2CraftCalcService.StrategyResult r =
        calc.strategy(
            new Poe2CraftCalcService.StrategyRequest(
                normal,
                List.of(new Poe2CraftCalcService.Target(prefixes)),
                List.of(
                    new Poe2CraftCalcService.Step("ALCHEMY", null, null, null, null, null),
                    new Poe2CraftCalcService.Step("EXALT", null, null, null, null, null)),
                300));
    assertThat(r.steps().get(1).reached()).isLessThan(0.2);
  }
}
