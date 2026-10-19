package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** 제작 에뮬레이터 수치 굴리기(10-16 C370). */
class PoeModRollTest {

  @Test
  void 범위마다_양끝_포함_정수로_굴리고_나머지는_그대로() {
    Set<String> seen = new HashSet<>();
    for (long s = 0; s < 400; s++) {
      String r = PoeModRoll.roll("생명력 최대치 +(70-72)", s);
      assertThat(r).matches("생명력 최대치 \\+7[0-2]");
      seen.add(r);
    }
    assertThat(seen).hasSize(3);
    assertThat(PoeModRoll.roll("화염 저항 +25%", 1)).isEqualTo("화염 저항 +25%");
  }

  @Test
  void 기폭제_퀄리티는_숫자를_올리고_0쪽으로_버린다() {
    // 48 × 1.2 = 57.6 → 57, 범위 문장은 양 끝 모두
    assertThat(PoeModRoll.scale("+48% to Fire Resistance", 20))
        .isEqualTo("+57% to Fire Resistance");
    assertThat(PoeModRoll.scale("화염 저항 +(41-45)%", 20)).isEqualTo("화염 저항 +(49-54)%");
    assertThat(PoeModRoll.scale("Adds 3 to 7 Physical Damage", 10))
        .isEqualTo("Adds 3 to 7 Physical Damage");
    assertThat(PoeModRoll.scale("0.4% of Physical Attack Damage Leeched as Life", 20))
        .isEqualTo("0.4% of Physical Attack Damage Leeched as Life");
    assertThat(PoeModRoll.scale("0.8% of Physical Attack Damage Leeched as Life", 20))
        .isEqualTo("0.9% of Physical Attack Damage Leeched as Life");
    assertThat(PoeModRoll.scale("-10% to Cold Resistance", 20))
        .isEqualTo("-12% to Cold Resistance");
    assertThat(PoeModRoll.scale("+48% to Fire Resistance", 0)).isEqualTo("+48% to Fire Resistance");
  }

  @Test
  void 행운_굴림은_평균이_더_높다() {
    double plain = 0;
    double lucky = 0;
    for (long s = 0; s < 2000; s++) {
      plain +=
          Integer.parseInt(PoeModRoll.roll("+(1-100) to maximum Life", s).replaceAll("\\D", ""));
      lucky +=
          Integer.parseInt(
              PoeModRoll.rollLucky("+(1-100) to maximum Life", "+(1-100) to maximum Life", s)
                  .replaceAll("\\D", ""));
    }
    // 균등 평균 50.5, 두 번 중 큰 값 평균 ≈ 67.2
    assertThat(plain / 2000).isBetween(47.0, 54.0);
    assertThat(lucky / 2000).isBetween(64.0, 70.0);
    assertThat(PoeModRoll.rollLucky("화염 저항 +25%", "Fire +25%", 3)).isEqualTo("화염 저항 +25%");
  }

  @Test
  void 축성은_배수를_곱하고_올린다() {
    // 48 × 0.78 = 37.44 → 38, 48 × 1.22 = 58.56 → 59
    assertThat(PoeModRoll.scaleUp("+48% to Fire Resistance", 78))
        .isEqualTo("+38% to Fire Resistance");
    assertThat(PoeModRoll.scaleUp("+48% to Fire Resistance", 122))
        .isEqualTo("+59% to Fire Resistance");
    assertThat(PoeModRoll.scaleUp("0.4% of Damage Leeched", 122))
        .isEqualTo("0.5% of Damage Leeched");
    assertThat(PoeModRoll.scaleUp("+48% to Fire Resistance", 100))
        .isEqualTo("+48% to Fire Resistance");
    // 음수는 0 쪽으로 올림: -7 × 0.78 = -5.46 → -5, -7 × 1.22 = -8.54 → -8
    assertThat(PoeModRoll.scaleUp("-7% to Cold Resistance", 78))
        .isEqualTo("-5% to Cold Resistance");
    assertThat(PoeModRoll.scaleUp("-7% to Cold Resistance", 122))
        .isEqualTo("-8% to Cold Resistance");
  }

  @Test
  void 같은_씨앗은_같은_값_범위_둘은_따로() {
    String t = "Adds (3-5) to (7-8) Physical Damage";
    assertThat(PoeModRoll.roll(t, 42)).isEqualTo(PoeModRoll.roll(t, 42));
    assertThat(PoeModRoll.roll(t, 42)).matches("Adds [3-5] to [78] Physical Damage");
  }

  @Test
  void 음수_소수_줄표() {
    for (long s = 0; s < 200; s++) {
      int v = Integer.parseInt(PoeModRoll.roll("(-40-40)%", s).replace("%", ""));
      assertThat(v).isBetween(-40, 40);
      double d = Double.parseDouble(PoeModRoll.roll("(0.2-0.4)", s));
      assertThat(d).isBetween(0.2, 0.4);
      assertThat(PoeModRoll.roll("(0.2-0.4)", s)).matches("0\\.[234]");
      assertThat(PoeModRoll.roll("+(130–144)", s)).matches("\\+1[34]\\d");
    }
  }

  @Test
  void 한국어는_영어_범위_위치로_같은_값() {
    String en = "Adds (25-29) to (37-45) Fire damage to Attacks";
    String ko = "공격 시 화염 피해 (25~29)~(37~45) 추가";
    for (long s = 0; s < 100; s++) {
      String e = PoeModRoll.roll(en, en, s);
      String k = PoeModRoll.roll(ko, en, s);
      java.util.regex.Matcher me =
          java.util.regex.Pattern.compile("Adds (\\d+) to (\\d+)").matcher(e);
      java.util.regex.Matcher mk =
          java.util.regex.Pattern.compile("피해 (\\d+)~(\\d+) 추가").matcher(k);
      assertThat(me.find()).isTrue();
      assertThat(mk.find()).isTrue();
      assertThat(mk.group(1)).isEqualTo(me.group(1));
      assertThat(mk.group(2)).isEqualTo(me.group(2));
    }
    // 괄호 없는 한국어 단일 범위
    assertThat(PoeModRoll.roll("방어도 39~42% 증가", "(39-42)% increased Armour", 3))
        .matches("방어도 (39|40|41|42)% 증가");
    // 기준에 범위가 없으면 그대로
    assertThat(PoeModRoll.roll("정신력 +30", "+30 to Spirit", 3)).isEqualTo("정신력 +30");
  }

  @Test
  void 바깥_괄호를_먹지_않고_문장_끝_마침표도() {
    // C378 검토 — 괄호를 따로 찾아 "(최대 10~20)" 의 바깥 괄호를 먹었고, "10~20." 은 못 찾았다
    for (long s = 0; s < 50; s++) {
      assertThat(PoeModRoll.roll("(최대 10~20)", "up to (10-20)", s)).matches("\\(최대 (1\\d|20)\\)");
      assertThat(PoeModRoll.roll("피해 10~20.", "(10-20) damage.", s)).matches("피해 (1\\d|20)\\.");
    }
  }

  @Test
  void 소수점은_로케일과_무관() {
    java.util.Locale old = java.util.Locale.getDefault();
    try {
      java.util.Locale.setDefault(java.util.Locale.GERMANY);
      assertThat(PoeModRoll.roll("(0.2-0.4)", 1)).matches("0\\.[234]");
    } finally {
      java.util.Locale.setDefault(old);
    }
  }
}
