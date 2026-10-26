package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** 계산기 목표 칸 덧붙임(10-21 C548) — 실제 추출 데이터로, 없으면 건너뛴다. */
class PoeCraftCalcExtrasTest {

  @Test
  void 작업대_값어치_바알_타락_영향력_옵션() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftEngine.Item plain =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    PoeCraftCalcService.CalcExtras x = calc.extras(plain);
    // 작업대 — 생명력 계열은 작업대로 채울 수 있는 등급이 있다(그 등급은 1 이상)
    assertThat(x.bench()).anyMatch(b -> "IncreasedLife".equals(b.family()) && b.tier() >= 1);
    assertThat(x.bench()).allMatch(b -> b.modId() != null && b.tier() >= 1);
    // 바알 — 결과 넷 중 '고정 속성' 한 갈래라 확률 합 = 1/4, 태그 이름이 붙는다
    assertThat(x.corrupted()).isNotEmpty();
    assertThat(x.corrupted().stream().mapToDouble(PoeCraftCalcService.CorruptedLine::chance).sum())
        .isCloseTo(0.25, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(x.corrupted()).anyMatch(c -> !c.tagsKo().isEmpty());
    // 영향력 — 없으면 빈 목록, 십자군이면 그 옵션 id(영향력 없는 풀에는 없는 것)
    assertThat(x.influenceIds()).isEmpty();
    PoeCraftCalcService.CalcExtras cr =
        calc.extras(
            new PoeCraftEngine.Item(
                "Gloves",
                "str_armour",
                "crusader",
                84,
                PoeCraftEngine.Rarity.NORMAL,
                List.of(),
                false));
    assertThat(cr.influenceIds()).isNotEmpty();
    // 탐광(C549) — 장갑의 화석 전용 옵션(젬 레벨 +1 · 심연 홈)과 여는 화석
    assertThat(x.delve())
        .anyMatch(d -> d.id().startsWith("DelveStrengthGemLevel") && !d.fossils().isEmpty());
    assertThat(x.delve()).anyMatch(d -> d.id().startsWith("DelveAbyssJewelSocket"));
  }

  @Test
  void 엘드리치_고정속성_목표는_따로_재고_명시_옵션_표에_더한다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftEngine.Item plain =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    PoeCraftCalcService.CalcExtras x = calc.extras(plain);
    assertThat(x.eldritchBlocked()).isNull();
    assertThat(x.eldritch()).anyMatch(f -> "exarch".equals(f.side()));
    assertThat(x.eldritch()).anyMatch(f -> "eater".equals(f.side()));
    PoeCraftCalcService.EldritchFamily ex =
        x.eldritch().stream().filter(f -> "exarch".equals(f.side())).findFirst().orElseThrow();
    // 등급 3 이상 목표 = 등급 3~6 id
    List<String> ids =
        ex.tiers().stream()
            .filter(t -> t.tier() >= 3)
            .map(PoeCraftCalcService.EldritchTier::id)
            .toList();
    PoeCraftCalcService.Response r =
        calc.calc(
            new PoeCraftCalcService.Request(
                plain, List.of(new PoeCraftCalcService.Target(ids)), null, null, 2_000));
    assertThat(r.results()).isEmpty();
    assertThat(r.eldritch()).isNotNull();
    assertThat(r.eldritch().error()).isNull();
    assertThat(r.eldritch().steps()).hasSize(1);
    PoeCraftCalcService.EldritchStep s = r.eldritch().steps().get(0);
    assertThat(s.currencyTier()).isBetween(3, 4);
    assertThat(s.chance()).isGreaterThan(0).isLessThanOrEqualTo(1);
    assertThat(s.attempts()).isCloseTo(1 / s.chance(), org.assertj.core.data.Offset.offset(1e-9));
    // 같은 쪽 둘이면 오류
    PoeCraftCalcService.EldritchFamily ex2 =
        x.eldritch().stream()
            .filter(f -> "exarch".equals(f.side()) && !f.key().equals(ex.key()))
            .findFirst()
            .orElseThrow();
    PoeCraftCalcService.Response r2 =
        calc.calc(
            new PoeCraftCalcService.Request(
                plain,
                List.of(
                    new PoeCraftCalcService.Target(ids),
                    new PoeCraftCalcService.Target(List.of(ex2.tiers().get(0).id()))),
                null,
                null,
                2_000));
    assertThat(r2.eldritch().error()).isNotBlank();
    // 영향력 아이템이면 못 씀
    PoeCraftEngine.Item shaper =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "shaper", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    assertThat(calc.eldritchPlan(shaper, List.of(new PoeCraftCalcService.Target(ids))).error())
        .isNotBlank();
  }

  @Test
  void 탐광_옵션_목표는_여는_화석_조합으로_비용이_나온다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftEngine.Item plain =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    String gem =
        calc.extras(plain).delve().stream()
            .map(PoeCraftCalcService.DelveLine::id)
            .filter(id -> id.startsWith("DelveStrengthGemLevel"))
            .findFirst()
            .orElseThrow();
    List<PoeCraftCalcService.MethodResult> rows =
        calc.fossils(plain, List.of(new PoeCraftCalcService.Target(List.of(gem))), 6_000);
    // 여는 화석이 든 조합이 나오고 비용이 있다(전엔 태그 배수 화석만 봐 빈 목록)
    assertThat(rows).isNotEmpty();
    assertThat(rows).allMatch(r -> r.expectedCost() != null && r.expectedCost() > 0);
  }

  @Test
  void 에센스_전용_옵션은_목표로_고르면_그_에센스_줄이_채운다() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeCraftCalcService calc =
        new PoeCraftCalcService(
            new PoeCraftService(data, new PoeModDataService(dir)), new PoeCraftPriceService(dir));
    PoeCraftEngine.Item plain =
        new PoeCraftEngine.Item(
            "Gloves", "str_armour", "", 84, PoeCraftEngine.Rarity.NORMAL, List.of(), false);
    List<PoeCraftCalcService.EssenceOnly> eo = calc.extras(plain).essenceOnly();
    // 공포(소환수 피해) · 광기(장착 젬)는 장갑 자연 풀에 없고, 탐욕(생명력) · 분노(화염 저항)는 자연 옵션과 같아 빠진다
    assertThat(eo).anyMatch(f -> "Fear".equals(f.family()) && f.tiers().size() >= 3);
    assertThat(eo).anyMatch(f -> "Insanity".equals(f.family()));
    assertThat(eo).noneMatch(f -> "Greed".equals(f.family()) || "Anger".equals(f.family()));
    PoeCraftCalcService.EssenceOnly fear =
        eo.stream().filter(f -> "Fear".equals(f.family())).findFirst().orElseThrow();
    // 태그(C553 — 에센스 옵션은 자연 옵션 표에 없어 비었다)
    assertThat(fear.tags()).isNotEmpty();
    assertThat(fear.tagsKo()).isNotEmpty();
    // 에센스를 쓴 아이템 줄에도 그 옵션 태그
    PoeCraftService craft = new PoeCraftService(data, new PoeModDataService(dir));
    String fearTop = fear.tiers().get(fear.tiers().size() - 1).modId();
    String fearSlug =
        craft.essences("Gloves").stream()
            .filter(e -> fearTop.equals(e.modId()))
            .map(PoeCraftDataService.Essence::slug)
            .findFirst()
            .orElseThrow();
    PoeCraftService.View v = craft.apply(plain, PoeCraftEngine.Action.ESSENCE, fearSlug, 1L);
    assertThat(v.tags().get(fearTop)).isNotEmpty();
    assertThat(fear.tiers())
        .isSortedAccordingTo(
            java.util.Comparator.comparingInt(PoeCraftCalcService.EssenceTier::tier));
    List<String> ids =
        fear.tiers().stream()
            .filter(t -> t.tier() >= 5)
            .map(PoeCraftCalcService.EssenceTier::modId)
            .toList();
    PoeCraftCalcService.Response r =
        calc.calc(
            new PoeCraftCalcService.Request(
                plain, List.of(new PoeCraftCalcService.Target(ids)), null, null, 2_000));
    // 공포 에센스 연타 한 번이면 그 옵션이 반드시 붙는다(자동 에센스 줄)
    assertThat(r.results()).anyMatch(m -> m.method().startsWith("ESSENCE") && m.chance() == 1.0);
    assertThat(r.results())
        .filteredOn(m -> "CHAOS".equals(m.method()))
        .allMatch(m -> m.chance() == 0);
  }
}
