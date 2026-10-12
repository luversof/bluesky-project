package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 팅크처 속성 칸(10-09 C211). */
class PoeTincturePropsTest {

  @Test
  void 옵션이_없으면_베이스_그대로_인게임_표기() {
    PoeTinctureProps.Result r = PoeTinctureProps.apply(0.6, 7, List.of("25% chance to Ignite"));
    assertThat(r.manaBurnText()).isEqualTo("0.60");
    assertThat(r.cooldownText()).isEqualTo("7");
    assertThat(r.manaBurn().changed()).isFalse();
  }

  @Test
  void 연소_속도_감소는_간격을_늘린다() {
    // 핏빛 분노 — 무쇠나무 0.9 ÷ (1 - 0.15..0.25)
    PoeTinctureProps.Result r =
        PoeTinctureProps.apply(0.9, 10, List.of("(15-25)% reduced Mana Burn rate"));
    assertThat(r.manaBurnText()).isEqualTo("1.06-1.20");
    assertThat(r.manaBurn().changed()).isTrue();
  }

  @Test
  void 재사용_대기시간_회복_속도() {
    // 계절의 수액 — 분광 8 ÷ (1.2..1.4)
    PoeTinctureProps.Result r =
        PoeTinctureProps.apply(0.7, 8, List.of("(20-40)% increased Cooldown Recovery Rate"));
    assertThat(r.cooldownText()).isEqualTo("5.71-6.67");
    assertThat(r.cooldown().changed()).isTrue();
  }
}
