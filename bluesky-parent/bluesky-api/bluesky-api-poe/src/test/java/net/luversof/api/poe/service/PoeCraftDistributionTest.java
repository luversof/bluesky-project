package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 결과 분포(10-21 C533, craftofexile 대량 모의) — 실제 추출 데이터로, 없으면 건너뛴다. */
class PoeCraftDistributionTest {

  @Test
  void 연금은_옵션_4에서_6개_생명력이_나오고_변화는_오류() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftEngine.Item normal =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    PoeCraftCalcService.DistResult r =
        calc.distribution(new PoeCraftCalcService.DistRequest(normal, "ALCHEMY", null, 2000));
    assertThat(r.error()).isNull();
    assertThat(r.samples()).isEqualTo(2000);
    // 레어 연금 = 옵션 4~6개(그 밖의 개수가 나오면 엔진 · 집계가 틀린 것)
    assertThat(r.affixes().keySet()).allMatch(k -> k >= 4 && k <= 6);
    assertThat(r.affixes().values().stream().mapToDouble(Double::doubleValue).sum())
        .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(r.lines()).anyMatch(l -> l.family().startsWith("IncreasedLife") && l.share() > 0.05);
    assertThat(r.lines()).allMatch(l -> l.share() <= 1.0 && l.top() <= l.share());
    // 일반 아이템에 변화의 오브는 못 쓴다
    assertThat(
            calc.distribution(new PoeCraftCalcService.DistRequest(normal, "ALTERATION", null, 100))
                .error())
        .isNotBlank();
  }
}
