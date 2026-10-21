package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 화석 조합 기준 첫 옵션 확률(10-20 C504) — 실제 추출 데이터로. 없으면 건너뛴다. */
class PoeCraftFossilPoolTest {

  private static double share(List<PoeCraftService.PoolLine> pool, String prefix) {
    return pool.stream()
        .filter(p -> p.id().startsWith(prefix))
        .mapToDouble(PoeCraftService.PoolLine::chance)
        .sum();
  }

  @Test
  void 온전한_화석은_생명력을_늘리고_방어를_막는다_그을린_화석은_냉기를_막는다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-currencies.json")), "추출 데이터 없음");
    PoeCraftService craft =
        new PoeCraftService(new PoeCraftDataService(dir), new PoeModDataService(dir));
    PoeCraftEngine.Item item =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    List<PoeCraftService.PoolLine> plain = craft.pool(item, PoeCraftEngine.Rarity.RARE);
    List<PoeCraftService.PoolLine> life = craft.fossilPool(item, "CurrencyDelveCraftingLife");
    // 생명력 x10 → 비율이 크게 오른다, 방어(defences) x0 → 방어도 증가 옵션은 0
    assertThat(share(life, "IncreasedLife")).isGreaterThan(share(plain, "IncreasedLife") * 3);
    assertThat(life)
        .noneMatch(p -> p.id().startsWith("LocalIncreasedPhysicalDamageReductionRating"));
    assertThat(life.stream().mapToDouble(PoeCraftService.PoolLine::chance).sum())
        .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    List<PoeCraftService.PoolLine> fire = craft.fossilPool(item, "CurrencyDelveCraftingFire");
    assertThat(fire).noneMatch(p -> p.id().startsWith("ColdResist"));
    assertThat(share(fire, "FireResist")).isGreaterThan(share(plain, "FireResist"));
    // 확률 높은 순
    assertThat(life.get(0).chance()).isGreaterThanOrEqualTo(life.get(life.size() - 1).chance());
  }
}
