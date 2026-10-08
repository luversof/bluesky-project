package net.luversof.web.gate.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import net.luversof.web.gate.poe2.dto.Poe2;

/** PoE2 베이스 목록 카드 요약 수치(2026-10-01, PoE1 베이스 목록과 같은 줄) — 0 은 빼고, 물리 DPS = 평균 피해 × 초당 공격. */
class Poe2ViewTest {

  @BeforeEach
  void english() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
  }

  @AfterEach
  void reset() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static Poe2.BaseItem base(Poe2.Armour a, Poe2.Weapon w, Poe2.Flask f) {
    return new Poe2.BaseItem(
        "n", null, "s", "c", null, null, 1, 1, null, null, null, 1, 1, a, w, f, null, null, null,
        null, null, null, null);
  }

  @Test
  void weaponShowsDamageSpeedCritAndPhysicalDps() {
    // Crude Bow: 6-9, 1.2/s, 5% → (6+9)/2 × 1.2 = 9 DPS
    assertThat(Poe2View.cardSummary(base(null, new Poe2.Weapon(6, 9, 5.0, 1.2, 120, null), null)))
        .isEqualTo("6-9 · 1.20/s · 5.00% · 9 DPS");
  }

  @Test
  void armourSkipsZeroDefences() {
    assertThat(Poe2View.cardSummary(base(new Poe2.Armour(0, 10, 0, 0.0, 20), null, null)))
        .isEqualTo("Block 20%  EV 10");
    assertThat(Poe2View.cardSummary(base(new Poe2.Armour(15, 0, 7, null, null), null, null)))
        .isEqualTo("AR 15  ES 7");
    assertThat(Poe2View.cardSummary(base(new Poe2.Armour(0, 0, 0, 0.0, 0), null, null))).isEmpty();
  }

  @Test
  void flaskShowsRecovery() {
    assertThat(Poe2View.cardSummary(base(null, null, new Poe2.Flask(1, 50, 0, 3.0))))
        .isEqualTo("Life 50  3.0s");
    assertThat(Poe2View.cardSummary(null)).isEmpty();
  }
}
