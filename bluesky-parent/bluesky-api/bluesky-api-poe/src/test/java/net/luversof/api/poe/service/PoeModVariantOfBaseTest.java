package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 베이스 → 변형(10-20 C516) — 주얼 색 · 심연 주얼 종류는 베이스마다 붙는 옵션이 달라 베이스 하나 = 변형 하나, 군집 주얼(인챈트마다)과 고유 전용 베이스는
 * null. 실제 추출 데이터(~/.poe-gamedata)로, 없으면 건너뛴다.
 */
class PoeModVariantOfBaseTest {

  private static PoeModDataService mods;

  @BeforeAll
  static void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "mods.json")), "추출 데이터 없음");
    mods = new PoeModDataService(dir);
  }

  @Test
  void 주얼_색과_심연_주얼_종류() {
    assertThat(mods.variantOfBase("Jewel", "Crimson Jewel")).isEqualTo("strjewel");
    assertThat(mods.variantOfBase("Jewel", "Viridian Jewel")).isEqualTo("dexjewel");
    assertThat(mods.variantOfBase("Jewel", "Cobalt Jewel")).isEqualTo("intjewel");
    assertThat(mods.variantOfBase("AbyssJewel", "Murderous Eye Jewel"))
        .isEqualTo("abyss_jewel_melee");
    assertThat(mods.variantOfBase("AbyssJewel", "Ghastly Eye Jewel"))
        .isEqualTo("abyss_jewel_summoner");
  }

  @Test
  void 군집_주얼과_고유_전용_베이스는_없다() {
    assertThat(mods.variantOfBase("Jewel", "Large Cluster Jewel")).isNull();
    assertThat(mods.variantOfBase("Jewel", "Prismatic Jewel")).isNull();
    assertThat(mods.variantOfBase("Jewel", "Timeless Jewel")).isNull();
    assertThat(mods.variantOfBase("AbyssJewel", "Assembled Eye Jewel")).isNull();
  }

  @Test
  void 방어구는_속성_변형() {
    assertThat(mods.variantOfBase("Gloves", "Titan Gauntlets")).isEqualTo("str_armour");
  }
}
