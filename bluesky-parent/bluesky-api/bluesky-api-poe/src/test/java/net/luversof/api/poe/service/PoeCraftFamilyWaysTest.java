package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 같은 옵션을 얻는 다른 길(10-22 P4) — 실제 추출 데이터로. 없으면 건너뛴다. */
class PoeCraftFamilyWaysTest {

  private PoeCraftCalcService calc;
  private PoeCraftEngine.Item item;

  @BeforeEach
  void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-currencies.json")), "추출 데이터 없음");
    PoeCraftService craft =
        new PoeCraftService(new PoeCraftDataService(dir), new PoeModDataService(dir));
    calc = new PoeCraftCalcService(craft, new PoeCraftPriceService(dir));
    item =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
  }

  @Test
  void 생명력_계열은_탐욕_에센스_온전한_화석_생명력_재련_작업대() {
    PoeCraftCalcService.FamilyWays w = calc.familyWays(item, "prefix:IncreasedLife");
    assertThat(w.essences()).isNotEmpty();
    assertThat(w.essences()).allMatch(e -> e.rank() >= 1 && e.slug() != null);
    // 등급 상당이 높은(작은 숫자) 것부터
    assertThat(w.essences().get(0).rank())
        .isLessThanOrEqualTo(w.essences().get(w.essences().size() - 1).rank());
    assertThat(w.fossils())
        .anySatisfy(
            f -> {
              assertThat(f.slug()).isEqualTo("CurrencyDelveCraftingLife");
              assertThat(f.mult()).isEqualTo(10.0);
            });
    assertThat(w.harvests()).extracting(PoeCraftCalcService.HarvestWay::id).contains("ReforgeLife");
    assertThat(w.bench()).isNotNull();
    assertThat(w.bench().tier()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void 화염_저항은_그을린_화석이_늘리고_화염_재련으로_얻는다() {
    PoeCraftCalcService.FamilyWays w = calc.familyWays(item, "suffix:FireResist");
    assertThat(w.fossils())
        .anySatisfy(f -> assertThat(f.slug()).isEqualTo("CurrencyDelveCraftingFire"));
    assertThat(w.fossils().stream().filter(f -> f.slug().equals("CurrencyDelveCraftingFire")))
        .allMatch(f -> f.mult() > 1);
    // 늘림이 막음보다 먼저
    for (int i = 1; i < w.fossils().size(); i++) {
      assertThat(w.fossils().get(i - 1).mult()).isGreaterThanOrEqualTo(w.fossils().get(i).mult());
    }
    assertThat(w.harvests()).extracting(PoeCraftCalcService.HarvestWay::id).contains("ReforgeFire");
    assertThat(w.essences()).isNotEmpty();
  }

  @Test
  void 모르는_계열은_빈_결과() {
    PoeCraftCalcService.FamilyWays w = calc.familyWays(item, "prefix:NoSuchFamily");
    assertThat(w.essences()).isEmpty();
    assertThat(w.bench()).isNull();
    assertThat(w.fossils()).isEmpty();
    assertThat(w.harvests()).isEmpty();
    assertThat(calc.familyWays(item, "broken").essences()).isEmpty();
  }

  @Test
  void 에센스_확정_옵션은_다른_길과_같은_계열_등급() {
    List<PoeCraftService.Forced> f = calc.essenceForced(item, "CurrencyEssenceGreed7");
    assertThat(f).hasSize(1);
    PoeCraftCalcService.FamilyWays w = calc.familyWays(item, "prefix:IncreasedLife");
    assertThat(f.get(0).family()).isEqualTo("prefix:IncreasedLife");
    assertThat(f.get(0).rank())
        .isEqualTo(
            w.essences().stream()
                .filter(e -> e.slug().equals("CurrencyEssenceGreed7"))
                .findFirst()
                .orElseThrow()
                .rank());
    assertThat(f.get(0).enMin()).isNotEmpty();
    assertThat(calc.essenceForced(item, "NoSuchEssence")).isEmpty();
  }

  @Test
  void 전략도_에센스_옵션을_그_목표로_센다() {
    // 10-22 Q12 — 탐욕 에센스(+91~105)는 생명력 T1~T3 를 채운다, T1(+115~)만이면 못 채운다
    PoeCraftCalcService.Step ess =
        new PoeCraftCalcService.Step("ESSENCE", "CurrencyEssenceGreed7", null, 1, null);
    PoeCraftCalcService.StrategyResult t13 =
        calc.strategy(
            new PoeCraftCalcService.StrategyRequest(
                item,
                List.of(
                    new PoeCraftCalcService.Target(
                        List.of("IncreasedLife8", "IncreasedLife7", "IncreasedLife6"))),
                List.of(ess),
                500));
    assertThat(t13.successRate()).isGreaterThan(0.95);
    PoeCraftCalcService.StrategyResult t1 =
        calc.strategy(
            new PoeCraftCalcService.StrategyRequest(
                item,
                List.of(new PoeCraftCalcService.Target(List.of("IncreasedLife8"))),
                List.of(ess),
                500));
    assertThat(t1.successRate()).isLessThan(0.05);
  }

  @Test
  void 전략은_목표를_채운_아이템을_예시로_남긴다() {
    // 10-22 Q16 — 성공 예시: 처음 5개, 모두 목표 옵션을 가진 레어 · 표시 줄이 아이템 옵션 수와 같다
    PoeCraftCalcService.StrategyResult r =
        calc.strategy(
            new PoeCraftCalcService.StrategyRequest(
                item,
                List.of(
                    new PoeCraftCalcService.Target(
                        List.of("IncreasedLife8", "IncreasedLife7", "IncreasedLife6"))),
                List.of(
                    new PoeCraftCalcService.Step(
                        "ESSENCE", "CurrencyEssenceGreed7", null, 1, null)),
                500));
    assertThat(r.samples()).hasSize(PoeCraftCalcService.STRATEGY_SAMPLES);
    for (PoeCraftCalcService.StrategySample s : r.samples()) {
      assertThat(s.item().rarity()).isEqualTo(PoeCraftEngine.Rarity.RARE);
      assertThat(s.lines()).hasSize(s.item().affixes().size());
      assertThat(s.lines()).anyMatch(PoeCraftService.Line::essence);
    }
    PoeCraftCalcService.StrategyResult none =
        calc.strategy(
            new PoeCraftCalcService.StrategyRequest(
                item,
                List.of(new PoeCraftCalcService.Target(List.of("IncreasedLife8"))),
                List.of(
                    new PoeCraftCalcService.Step(
                        "ESSENCE", "CurrencyEssenceGreed7", null, 1, null)),
                500));
    assertThat(none.samples())
        .hasSize(Math.min(none.successes(), PoeCraftCalcService.STRATEGY_SAMPLES));
  }

  @Test
  void 전략은_목표를_다_채우면_남은_단계를_쓰지_않는다() {
    // 10-23 Q18 — 변화(+확장)로 생명력 T1~T3 를 채우면 제왕 · 엑잘은 쓰지 않는다(전엔 늘 100% 도달)
    PoeCraftCalcService.StrategyResult r =
        calc.strategy(
            new PoeCraftCalcService.StrategyRequest(
                item,
                List.of(
                    new PoeCraftCalcService.Target(
                        List.of("IncreasedLife8", "IncreasedLife7", "IncreasedLife6"))),
                List.of(
                    new PoeCraftCalcService.Step("TRANSMUTE", null, null, null, null),
                    new PoeCraftCalcService.Step(
                        "SEQ",
                        null,
                        List.of(0),
                        3000,
                        List.of(
                            new PoeCraftCalcService.Step("ALTERATION", null, null, null, null),
                            new PoeCraftCalcService.Step("AUGMENTATION", null, null, null, null))),
                    new PoeCraftCalcService.Step("REGAL", null, null, null, null),
                    new PoeCraftCalcService.Step("EXALT", null, null, null, null)),
                2000));
    assertThat(r.successRate()).isGreaterThan(0.99);
    assertThat(r.steps().get(2).reached()).isLessThan(0.01);
    assertThat(r.steps().get(3).reached()).isLessThan(0.01);
    assertThat(r.perRun()).doesNotContainKey("Regal Orb");
  }

  @Test
  void 연타와_적중_확률도_에센스_옵션을_그_목표로_센다() {
    // 10-22 Q13 — 같은 결함(연타 20번 못 채움 · 적중 0%)
    List<PoeCraftCalcService.Target> t13 =
        List.of(
            new PoeCraftCalcService.Target(
                List.of("IncreasedLife8", "IncreasedLife7", "IncreasedLife6")));
    PoeCraftCalcService.SpamResult sp =
        calc.spam(
            new PoeCraftCalcService.SpamRequest(
                item, "ESSENCE", "CurrencyEssenceGreed7", t13, 20, null));
    assertThat(sp.met()).isTrue();
    assertThat(sp.tries()).isEqualTo(1);
    List<PoeCraftCalcService.Hit> hits =
        calc.hitChance(
            new PoeCraftCalcService.HitRequest(
                item,
                t13,
                List.of(new PoeCraftCalcService.HitAction("ESSENCE", "CurrencyEssenceGreed7")),
                200));
    assertThat(hits.get(0).chance()).isGreaterThan(0.95);
  }
}
