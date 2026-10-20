package net.luversof.web.gate.poe2.dto;

import java.util.List;
import java.util.Map;

/** PoE2 제작 에뮬레이터 DTO(10-15 C351 — API Poe2CraftEngine · Poe2CraftService 와 짝). */
public final class Poe2Craft {

  private Poe2Craft() {}

  /** 옵션 하나 — level = 옵션 레벨, fractured = 분열. */
  public record Affix(String id, String gen, int level, List<Integer> groups, boolean fractured) {}

  /** 아이템 상태 — poolKey = 클래스|변형, rarity = NORMAL | MAGIC | RARE. */
  public record Item(
      String poolKey,
      int ilvl,
      String rarity,
      List<Affix> affixes,
      boolean corrupted,
      // 기폭제 퀄리티(C386) — "종류:양"(주얼용은 "jewel:종류:양"), 없으면 null
      String quality,
      // 축성(C390) — 더 바꿀 수 없다
      boolean sanctified,
      // 공개 씨앗(C483) — 드러낼 셋을 섞는 값, 되돌리면 새로 준다
      Long revealSeed) {

    public Item(String poolKey, int ilvl, String rarity, List<Affix> affixes, boolean corrupted) {
      this(poolKey, ilvl, rarity, affixes, corrupted, null, false, null);
    }

    public Item(
        String poolKey,
        int ilvl,
        String rarity,
        List<Affix> affixes,
        boolean corrupted,
        String quality,
        boolean sanctified) {
      this(poolKey, ilvl, rarity, affixes, corrupted, quality, sanctified, null);
    }

    /** 미공개 훼손 옵션이 있으면 공개 씨앗을 새로(C483) — 되돌린 뒤 다시 열면 다른 셋이 나온다. */
    public Item reseedReveal() {
      if (affixes == null || affixes.stream().noneMatch(a -> a.id().startsWith("Unrevealed"))) {
        return this;
      }
      return new Item(
          poolKey,
          ilvl,
          rarity,
          affixes,
          corrupted,
          quality,
          sanctified,
          java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }
  }

  /** 표시 줄 — tierRank = 그 풀 같은 계열에서 1 이 최상위. */
  public record Line(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      Integer tierRank,
      int level,
      boolean fractured) {}

  public record View(
      Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<String> omensUsed,
      List<Line> lines,
      int maxPerGen,
      Map<String, String> blocked,
      // 드러낼 셋(C354)
      List<Line> revealOffers,
      // 결과 설명(C376 — 바알 오브 "vaal:…", C386 기폭제 "catalyst:종류:양:개수"), 없으면 null
      String note,
      // 기폭제 퀄리티로 수치가 오르는 옵션 id(C386)
      List<String> boosted,
      // 옵션 태그(C480) — 줄 id → 태그 이름(영어 · 한국어)
      java.util.Map<String, List<String>> tags,
      java.util.Map<String, List<String>> tagsKo) {}

  /** 뼈(C354) — classes = 쓸 수 있는 종류, maxIlvl(0 = 없음), minModLevel. */
  public record Bone(
      String slug,
      String name,
      String nameKo,
      List<String> classes,
      int maxIlvl,
      int minModLevel,
      boolean breach) {}

  /** 다음 옵션 — chance = 균등(가중치 비공개). */
  public record PoolLine(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      Integer tierRank,
      int level,
      double chance,
      String family) {}

  /** 시뮬레이터(C352) — 목표 하나 = anyOf 중 하나. */
  public record Target(List<String> anyOf) {}

  /** 최적 조합 요청(C461) — upper · upperLabel = 아는 가장 싼 방법(없으면 API 가 연금 연타로 잰다). */
  /** recombChance = 재조합 성공 확률(C472, 인게임 값) — 있으면 최적 조합이 재조합기(반쪽 합치기)도 쓴다. */
  public record OptimizeRequest(
      Item item,
      List<Target> targets,
      Integer budgetMs,
      Double upper,
      String upperLabel,
      Integer recombChance) {}

  /** recombChance = 반쪽 재조합(C409) 비교에 쓸 재조합 성공 확률(인게임 값 1~100, 없으면 비교 안 함). */
  public record CalcRequest(Item item, List<Target> targets, Integer runs, Integer recombChance) {

    public CalcRequest(Item item, List<Target> targets, Integer runs) {
      this(item, targets, runs, null);
    }
  }

  public record CalcMethod(
      String method,
      String label,
      int runs,
      int successes,
      double successRate,
      Map<String, Double> perSuccess,
      Double costPerSuccess,
      String note) {}

  /** 다단계 전략(C355) — action = 화폐 | SEQ, omens = 이 단계에 걸 징조, until = 채울 목표 번호(없으면 한 번). */
  public record Step(
      String action,
      String arg,
      List<String> omens,
      List<Integer> until,
      Integer max,
      List<Step> sub) {}

  public record StrategyRequest(Item item, List<Target> targets, List<Step> steps, Integer runs) {}

  public record StepStat(String action, String arg, double avgUses, double reached) {}

  public record StrategyResult(
      int runs,
      int successes,
      double successRate,
      Map<String, Double> perRun,
      Double costPerRun,
      Double expectedCost,
      List<StepStat> steps,
      String error) {}

  public record CalcResponse(
      List<CalcMethod> results,
      String league,
      String pricesAt,
      Double exaltPerDivine,
      String error) {}

  /** 이 아이템 종류에 쓸 수 있는 에센스(C353) — kind = normal | perfect | corrupted, 보장 옵션 문구. */
  public record EssenceOption(
      String slug,
      String name,
      String nameKo,
      String kind,
      int tier,
      String modId,
      String gen,
      List<String> en,
      List<String> ko) {}

  /** 징조 — key = 이름 뒤를 붙여 쓴 것(SinistralExaltation). */
  public record Omen(String key, String name, String nameKo, String slug) {}

  /** 제작 화폐 — action = 엔진 동작, tier = "" · greater · perfect, 공식 이름 · 아이콘 슬러그. */
  public record Currency(
      String action,
      String tier,
      String name,
      String nameKo,
      String slug,
      // 기폭제(C386) — 퀄리티 머리글 · 효과 설명, 다른 화폐는 null
      String qualityLabel,
      String qualityLabelKo,
      String description,
      String descriptionKo) {}

  /**
   * 에뮬레이터에서 쓴 단계 하나(C366) — action = 화폐(FORESEE = 히네코라의 머리카락), arg = 등급 · 에센스 · 뼈, omens = 쓰인 징조 키.
   */
  public record SpentStep(String action, String arg, List<String> omens) {}

  public record SpentRequest(String poolKey, List<SpentStep> steps) {}

  public record SpentLine(
      String name, String nameKo, double count, Double unitDivine, Double subtotalDivine) {}

  public record SpentResult(
      List<SpentLine> lines,
      double totalDivine,
      int missing,
      String league,
      String pricesAt,
      // 요청 단계마다(C453) — 같은 순서, 옛 API 면 null
      List<StepCost> steps) {}

  /** 한 단계 비용(C453) — 쓴 화폐 이름(개수 붙임) · 한국어 · 디바인(시세 없는 화폐가 끼면 null). */
  public record StepCost(List<String> names, List<String> namesKo, Double divine) {}

  /**
   * 에뮬레이터 단계 기록 한 줄(C453) — no = 단계 번호(1부터), divine · cumDivine = 그 단계 · 누적 쓴 화폐(모르면 null), added ·
   * removed = 붙은 · 지운 옵션 문구, undoTo = 이 단계 뒤 상태로 되돌리는 이력 크기(지금 상태면 null, 기록 한도로 못 되돌리면 null), now =
   * 지금 상태, note = 아이템을 안 바꾸는 단계(머리카락 · 신성한 오브) 설명.
   */
  public record HistRow(
      int no,
      String label,
      Double divine,
      Double cumDivine,
      List<String> added,
      List<String> removed,
      Integer undoTo,
      boolean now,
      String note) {}

  public record Options(
      List<Poe2.ModPoolSummary> pools,
      List<Omen> omens,
      List<Currency> currencies,
      List<Bone> bones,
      String patch) {}

  /** 화폐 적용 — arg = 화폐 등급(greater · perfect, 없으면 기본), omens = 이번에 걸 징조 키. */
  public record ApplyRequest(Item item, String action, String arg, List<String> omens, Long seed) {}

  /** 재조합기(C407) — left = 지금 아이템, right = 보관한 재료, chance = 인게임 화면의 성공 확률(%), lucky = 재조합의 징조. */
  public record RecombineRequest(
      Item left,
      Item right,
      List<String> leftIds,
      List<String> rightIds,
      int chance,
      boolean lucky,
      Long seed) {}
}
