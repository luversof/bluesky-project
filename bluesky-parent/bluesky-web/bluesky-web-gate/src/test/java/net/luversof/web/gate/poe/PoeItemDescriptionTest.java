package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 사용 설명 키(10-09 C212). */
class PoeItemDescriptionTest {

  @Test
  void 분류와_베이스로_설명을_고른다() {
    assertThat(PoeItemDescription.key("flask", "Divine Life Flask"))
        .isEqualTo("poe.items.description.flask");
    assertThat(PoeItemDescription.key("tincture", "Ashbark Tincture"))
        .isEqualTo("poe.items.description.tincture");
    assertThat(PoeItemDescription.key("jewel", "Crimson Jewel"))
        .isEqualTo("poe.items.description.jewel");
    assertThat(PoeItemDescription.key("jewel", "Timeless Jewel"))
        .isEqualTo("poe.items.description.jewel");
    assertThat(PoeItemDescription.key("jewel", "Hypnotic Eye Jewel"))
        .isEqualTo("poe.items.description.abyss.jewel");
    assertThat(PoeItemDescription.key("jewel", "Large Cluster Jewel"))
        .isEqualTo("poe.items.description.jewel");
    assertThat(PoeItemDescription.key("jewel", "Small Cluster Jewel"))
        .isEqualTo("poe.items.description.jewel");
  }

  @Test
  void PoE2는_분류로_고른다() {
    assertThat(PoeItemDescription.poe2Key("LifeFlask")).isEqualTo("poe2.items.description.flask");
    assertThat(PoeItemDescription.poe2Key("ManaFlask")).isEqualTo("poe2.items.description.flask");
    assertThat(PoeItemDescription.poe2Key("UtilityFlask"))
        .isEqualTo("poe2.items.description.charm");
    assertThat(PoeItemDescription.poe2Key("Jewel")).isEqualTo("poe2.items.description.jewel");
    assertThat(PoeItemDescription.poe2Key("Talisman")).isNull();
    assertThat(PoeItemDescription.poe2Key(null)).isNull();
  }

  @Test
  void 장비는_설명이_없다() {
    assertThat(PoeItemDescription.key("boots", "Two-Toned Boots")).isNull();
    assertThat(PoeItemDescription.key(null, null)).isNull();
  }
}
