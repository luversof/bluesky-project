package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 화폐 한 번의 목표 적중 확률(10-20 C494) — 실제 추출 데이터로. 없으면 건너뛴다. */
class PoeCraftHitChanceTest {

  @Test
  void 남은_자리에_엑잘이면_확률이_있고_정제는_0_변화는_오류() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftService craft = new PoeCraftService(data, new PoeModDataService(dir));
    PoeCraftCalcService calc = new PoeCraftCalcService(craft, new PoeCraftPriceService(dir));
    // 레어 장갑: 생명력 T1 · 냉기 저항 — 목표 셋째(공격 속도)만 남음, 접미 자리 하나 빈
    PoeCraftEngine.Item item =
        new PoeCraftEngine.Item(
            "Gloves",
            "str_armour",
            "",
            84,
            PoeCraftEngine.Rarity.RARE,
            List.of(
                new PoeCraftEngine.Affix("IncreasedLife8", "prefix", List.of(), false, false),
                new PoeCraftEngine.Affix("ColdResist7", "suffix", List.of(), false, false)),
            false);
    List<PoeCraftCalcService.Target> targets =
        List.of(
            new PoeCraftCalcService.Target(List.of("IncreasedLife8")),
            new PoeCraftCalcService.Target(List.of("ColdResist7")),
            new PoeCraftCalcService.Target(
                List.of("IncreasedAttackSpeed3", "IncreasedAttackSpeed4")));
    List<PoeCraftCalcService.Hit> hits =
        calc.hitChance(
            new PoeCraftCalcService.HitRequest(
                item,
                targets,
                List.of(
                    new PoeCraftCalcService.HitAction("EXALT", null),
                    new PoeCraftCalcService.HitAction("SCOUR", null),
                    new PoeCraftCalcService.HitAction("TRANSMUTE", null),
                    new PoeCraftCalcService.HitAction("NOPE", null)),
                2000));
    Map<String, PoeCraftCalcService.Hit> by =
        hits.stream().collect(Collectors.toMap(PoeCraftCalcService.Hit::action, h -> h));
    // 엑잘: 접두 · 접미 중 접미 공격 속도 상위 둘 — 몇 % 안쪽(0 도 아니고 크지도 않다)
    assertThat(by.get("EXALT").chance()).isGreaterThan(0.0).isLessThan(0.2);
    assertThat(by.get("SCOUR").chance()).isZero();
    assertThat(by.get("TRANSMUTE").error()).isNotBlank();
    assertThat(by.get("NOPE").error()).isEqualTo("모르는 화폐");
  }

  @Test
  void 목표까지_연타는_목표를_채우면_멈추고_못_쓰는_화폐는_오류() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftService craft = new PoeCraftService(data, new PoeModDataService(dir));
    PoeCraftCalcService calc = new PoeCraftCalcService(craft, new PoeCraftPriceService(dir));
    PoeCraftEngine.Item rare =
        new PoeCraftEngine.Item(
            "Gloves",
            "str_armour",
            "",
            84,
            PoeCraftEngine.Rarity.RARE,
            List.of(new PoeCraftEngine.Affix("Strength1", "suffix", List.of(), false, false)),
            false);
    List<String> lifeAny = new java.util.ArrayList<>();
    for (int t = 1; t <= 9; t++) {
      lifeAny.add("IncreasedLife" + t);
    }
    PoeCraftCalcService.SpamResult r =
        calc.spam(
            new PoeCraftCalcService.SpamRequest(
                rare, "CHAOS", null, List.of(new PoeCraftCalcService.Target(lifeAny)), 2000));
    assertThat(r.error()).isNull();
    assertThat(r.met()).isTrue();
    assertThat(r.tries()).isBetween(1, 2000);
    assertThat(r.view().item().affixes()).anyMatch(a -> lifeAny.contains(a.id()));
    // 자가검사: 레어에 변화의 오브는 처음부터 못 쓴다
    PoeCraftCalcService.SpamResult bad =
        calc.spam(
            new PoeCraftCalcService.SpamRequest(
                rare, "TRANSMUTE", null, List.of(new PoeCraftCalcService.Target(lifeAny)), 10));
    assertThat(bad.error()).isNotBlank();
    assertThat(bad.tries()).isZero();
    // 올플레임(C524) — 복제본 4개 중 고르면 연타 평균 횟수가 확연히 준다(40번씩 평균, 기대 약 1/p 와 1/(1-(1-p)^4))
    double one = 0;
    double four = 0;
    for (int i = 0; i < 40; i++) {
      one +=
          calc.spam(
                  new PoeCraftCalcService.SpamRequest(
                      rare,
                      "CHAOS",
                      null,
                      List.of(new PoeCraftCalcService.Target(lifeAny)),
                      2000,
                      1))
              .tries();
      four +=
          calc.spam(
                  new PoeCraftCalcService.SpamRequest(
                      rare,
                      "CHAOS",
                      null,
                      List.of(new PoeCraftCalcService.Target(lifeAny)),
                      2000,
                      4))
              .tries();
    }
    assertThat(four).isLessThan(one * 0.7);
  }
}
