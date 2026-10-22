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
  void 생명력_계열은_탐욕_에센스_온전한_화석_생명력_재련_제작대() {
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
}
