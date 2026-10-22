package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * 영어 화면 문구(10-21 C557 ⑦ — 영어 화면 계산기 표 · 최적 조합에 한국어 라벨 · 흐름 · 상태 설명이 섞였다). 영어 판(labelEn · pathEn ·
 * stateEn …)에 한글이 없어야 한다. 실제 추출 데이터로, 없으면 건너뛴다.
 */
class PoeCraftEnglishTextTest {

  private static final Pattern HANGUL = Pattern.compile("[가-힣]");

  private static boolean english(String s) {
    return s == null || !HANGUL.matcher(s).find();
  }

  @Test
  void 상태_설명_영어_판() {
    assertThat(PoeCraftOptimizer.describeEn("R|5|1,0|2|P", 3))
        .isEqualTo(
            "Rare · targets 1·3 hit · other mods prefix 1/suffix 0 · target 2 slot blocked by other"
                + " mods · prefixes cannot be changed");
    assertThat(PoeCraftOptimizer.describeEn("Me|0|0,0|0|", 2))
        .isEqualTo("Magic · Searing Exarch dominant · no targets hit");
    assertThat(PoeCraftOptimizer.describeEn("N|0|0,0|0|#M|1|0,0|0|", 2))
        .isEqualTo("Normal · no targets hit · imprint: Magic · targets 1 hit");
    // 한국어 판과 같은 열쇠 규칙(자가검사 — 한국어 판엔 한글이 있다)
    assertThat(english(PoeCraftOptimizer.describe("R|5|1,0|2|P", 3))).isFalse();
  }

  @Test
  void 계산기_표와_최적_조합의_영어_판엔_한글이_없다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftService craft = new PoeCraftService(data, new PoeModDataService(dir));
    PoeCraftPriceService prices = new PoeCraftPriceService(dir);
    PoeCraftCalcService calc = new PoeCraftCalcService(craft, prices);
    PoeCraftEngine.Item plain =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    List<String> life =
        craft.pool(plain, PoeCraftEngine.Rarity.RARE).stream()
            .filter(
                p ->
                    "IncreasedLife".equals(p.family()) && p.tierRank() != null && p.tierRank() <= 3)
            .map(PoeCraftService.PoolLine::id)
            .toList();
    List<String> res =
        craft.pool(plain, PoeCraftEngine.Rarity.RARE).stream()
            .filter(
                p ->
                    "FireResistance".equals(p.family())
                        && p.tierRank() != null
                        && p.tierRank() <= 2)
            .map(PoeCraftService.PoolLine::id)
            .toList();
    assertThat(life).isNotEmpty();
    List<PoeCraftCalcService.Target> targets =
        res.isEmpty()
            ? List.of(new PoeCraftCalcService.Target(life))
            : List.of(new PoeCraftCalcService.Target(life), new PoeCraftCalcService.Target(res));
    PoeCraftCalcService.Response r =
        calc.calc(new PoeCraftCalcService.Request(plain, targets, null, null, 2_000));
    assertThat(r.results()).isNotEmpty();
    for (PoeCraftCalcService.MethodResult m : r.results()) {
      assertThat(m.labelEn()).as(m.method()).isNotBlank();
      assertThat(english(m.labelEn())).as(m.labelEn()).isTrue();
      assertThat(english(m.noteEn())).as(m.noteEn()).isTrue();
      // 한국어 판은 그대로(자가검사)
      assertThat(m.label()).isNotBlank();
    }
    assertThat(r.results()).anyMatch(m -> !english(m.label()));
    PoeCraftOptimizer opt = new PoeCraftOptimizer(craft, calc, prices);
    PoeCraftOptimizer.Result o =
        opt.optimize(new PoeCraftOptimizer.Request(plain, targets, 4_000, null, null, null));
    if (o.error() != null) {
      assertThat(o.errorEn()).isNotBlank();
      assertThat(english(o.errorEn())).as(o.errorEn()).isTrue();
      return;
    }
    assertThat(english(o.upperLabelEn())).as(o.upperLabelEn()).isTrue();
    assertThat(english(o.pathEn())).as(o.pathEn()).isTrue();
    assertThat(english(o.noteEn())).as(o.noteEn()).isTrue();
    assertThat(o.methodsEn()).hasSameSizeAs(o.methods());
    assertThat(o.methodsEn()).allMatch(PoeCraftEnglishTextTest::english);
    assertThat(o.rules()).isNotEmpty();
    for (PoeCraftOptimizer.Rule rule : o.rules()) {
      assertThat(english(rule.stateEn())).as(rule.stateEn()).isTrue();
      assertThat(english(rule.actionEn())).as(rule.actionEn()).isTrue();
    }
    // 오류가 없으면 영어 오류도 없다
    assertThat(o.errorEn()).isNull();
  }
}
