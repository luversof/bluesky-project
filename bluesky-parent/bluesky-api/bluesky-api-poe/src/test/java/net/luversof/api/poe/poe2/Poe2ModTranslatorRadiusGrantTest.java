package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 반경 주얼 "… in Radius also grant X" 를 게임 틀(ClientStrings ModStatJewelAddToNotable · Small)로
 * 감싼다(10-30 R9 후속).
 */
class Poe2ModTranslatorRadiusGrantTest {

  @SuppressWarnings("unchecked")
  private static Poe2ModTranslator translator() {
    return Poe2ModTranslator.of(
        List.<List<String>[]>of(
            new List[] {
              List.of(
                  "15% increased Critical Damage Bonus",
                  "Notable Passive Skills in Radius also grant 10% increased Projectile Speed"),
              List.of("치명타 피해 보너스 15% 증가", "반경 내 주요 패시브 스킬이 추가로 투사체 속도 10% 증가 부여")
            }));
  }

  @Test
  void 반경_부여_줄은_안쪽을_번역해_게임_틀로_감싼다() {
    Poe2ModTranslator t = translator();
    assertThat(
            t.translate(
                "Notable Passive Skills in Radius also grant 12% increased Critical Damage Bonus"))
        .isEqualTo("반경 내 주요 패시브 스킬이 치명타 피해 보너스 12% 증가도 부여");
    assertThat(
            t.translate(
                "Small Passive Skills in Radius also grant 7% increased Critical Damage Bonus"))
        .isEqualTo("반경 내 소형 패시브 스킬이 치명타 피해 보너스 7% 증가도 부여");
  }

  @Test
  void 사전에_통째로_있는_줄과_못_찾는_안쪽() {
    Poe2ModTranslator t = translator();
    assertThat(
            t.translate(
                "Notable Passive Skills in Radius also grant 6% increased Projectile Speed"))
        .as("게임이 따로 둔 문장이 먼저")
        .isEqualTo("반경 내 주요 패시브 스킬이 추가로 투사체 속도 6% 증가 부여");
    assertThat(
            t.translate("Notable Passive Skills in Radius also grant 5% increased Nothing Known"))
        .isNull();
  }
}
