package net.luversof.web.gate.poe.dto;

import java.util.List;

/** PoE1 제작 에뮬레이터 DTO(10-15 C335 — API PoeCraftEngine · PoeCraftService 와 짝). */
public final class PoeCraft {

  private PoeCraft() {}

  /** 옵션 하나 — crafted = 제작대, fractured = 분열. */
  public record Affix(
      String id, String gen, List<Integer> groups, boolean crafted, boolean fractured) {}

  /** 아이템 상태 — rarity = NORMAL | MAGIC | RARE. */
  public record Item(
      String itemClass,
      String variant,
      String influence,
      int ilvl,
      String rarity,
      List<Affix> affixes,
      boolean corrupted,
      // 임플리싯(10-15 C343, 엘드리치 — gen = exarch|eater)
      List<Affix> implicits,
      // 기폭제 퀄리티(C385) — "종류:양"(예: attack:20), 없으면 null
      String quality,
      // 장막 해제 씨앗(C483) — 해제 제시 셋을 섞는 값, 되돌리면 새로 준다
      Long veilSeed) {

    public Item(
        String itemClass,
        String variant,
        String influence,
        int ilvl,
        String rarity,
        List<Affix> affixes,
        boolean corrupted,
        List<Affix> implicits,
        String quality) {
      this(
          itemClass, variant, influence, ilvl, rarity, affixes, corrupted, implicits, quality,
          null);
    }

    /** 장막 옵션이 있으면 해제 씨앗을 새로(C483) — 되돌린 뒤 다시 열면 다른 셋이 나온다(되돌리기로 다른 셋을 노린다). */
    public Item reseedVeil() {
      if (affixes == null || affixes.stream().noneMatch(a -> a.id().startsWith("Veiled"))) {
        return this;
      }
      return new Item(
          itemClass,
          variant,
          influence,
          ilvl,
          rarity,
          affixes,
          corrupted,
          implicits,
          quality,
          java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }

    public Item(
        String itemClass,
        String variant,
        String influence,
        int ilvl,
        String rarity,
        List<Affix> affixes,
        boolean corrupted) {
      this(itemClass, variant, influence, ilvl, rarity, affixes, corrupted, List.of(), null);
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
      List<String> enMin,
      List<String> koMin,
      Integer tierRank,
      int ilvl,
      boolean crafted,
      boolean fractured,
      boolean essence) {}

  /** 적용 결과 — error 면 item 은 그대로. */
  public record View(
      Item item,
      String error,
      List<String> added,
      List<String> removed,
      List<Line> lines,
      int maxPerGen,
      Meta meta,
      // 장막 해제 제시 셋(C341) — 장막 옵션이 있을 때만, 아이템 상태로 정해진다
      List<Line> unveilOffers,
      // 지금 쓸 수 없는 화폐 → 이유(엔진이 시험 적용, C340). 정복자는 "CONQUEROR:영향력"
      java.util.Map<String, String> blocked,
      // 엘드리치 임플리싯 줄(C343, tierRank = 등급 1~6) · 우세 진영(exarch|eater|null)
      List<Line> implicits,
      String dominance,
      // 결과 설명(C358 — 바알 오브 "vaal:결과[:사유]", C385 기폭제 "catalyst:종류:양:개수"), 없으면 null
      String note,
      // 기폭제 퀄리티로 수치가 오르는 옵션 · 임플리싯 id(C385)
      List<String> boosted,
      // 옵션 태그(C480) — 줄 id → 태그 이름(영어 · 한국어), 인게임 상세 보기 " — 생명력, 방어력"
      java.util.Map<String, List<String>> tags,
      java.util.Map<String, List<String>> tagsKo) {}

  /** 메타 제작 상태(10-15 C338) — 접두/접미 잠금 · 제작 옵션 3개 · 공격/주문 옵션 금지. */
  public record Meta(
      boolean prefixLocked,
      boolean suffixLocked,
      boolean multimod,
      boolean noAttack,
      boolean noCaster) {}

  /**
   * 화석 — simple = 태그 배수만(계산기 자동 조합 후보), supported = 엔진이 다루는 것(C342 특수 화석 포함), weights = 옵션 태그 → 배수.
   */
  public record Fossil(
      String slug,
      String name,
      String nameKo,
      String icon,
      java.util.Map<String, Double> weights,
      List<String> desc,
      List<String> descKo,
      boolean simple,
      boolean supported) {}

  public record Resonator(
      String slug, String name, String nameKo, String kind, int sockets, String icon) {}

  public record Harvest(
      String id,
      String command,
      String params,
      String text,
      String textKo,
      String lifeforce,
      int cost) {}

  public record BenchCraft(
      String gen,
      int tier,
      String modName,
      String modId,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin) {}

  /** 아이템 종류에 쓸 수 있는 제작 수단. */
  public record Options(
      List<Essence> essences,
      List<Fossil> fossils,
      List<Resonator> resonators,
      List<Harvest> harvest,
      List<BenchCraft> bench,
      List<String> conquerors,
      List<CurrencyInfo> currencies) {}

  /** 화폐 인게임 사용 설명(10-15 C340, CurrencyItems) — action = 게임 동작 이름. */
  public record CurrencyInfo(
      String name,
      String nameKo,
      String slug,
      String action,
      String directions,
      String directionsKo,
      // 기폭제(C385) — 종류 키(vaal = 더럽혀진) · 퀄리티 머리글 · 효과 설명. 다른 화폐는 null
      String catalyst,
      String qualityLabel,
      String qualityLabelKo,
      String description,
      String descriptionKo) {}

  /** 옵션 풀 한 줄 — chance = 다음 옵션 하나가 이것일 확률(0~1). */
  /** 해제 옵션 한 줄(C421). */
  public record VeiledLine(
      String id, String gen, String name, String nameKo, List<String> en, List<String> ko) {}

  public record PoolLine(
      String id,
      String gen,
      String family,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      List<String> enMin,
      List<String> koMin,
      Integer tierRank,
      int ilvl,
      int weight,
      double chance) {}

  /** 에센스 — slug 로 고른다. */
  public record Essence(
      String family,
      String name,
      String nameKo,
      String icon,
      int tier,
      int ilvlMax,
      String gen,
      String modName,
      String modId,
      String slug,
      List<String> en,
      List<String> ko) {}

  public record ApplyRequest(Item item, String action, String essence, Long seed, String arg) {}

  /** 에뮬레이터에서 쓴 단계 하나(C365) — action = 화폐 이름(FORESEE = 히네코라의 머리카락), arg = 화폐 인자. */
  public record SpentStep(String action, String arg) {}

  public record SpentRequest(String itemClass, List<SpentStep> steps) {}

  /** 화폐 한 줄 — 개수 · 단가(카오스, 시세 없으면 null) · 합계. */
  public record SpentLine(
      String name, String nameKo, double count, Double unitChaos, Double subtotalChaos) {}

  public record SpentResult(
      List<SpentLine> lines,
      double totalChaos,
      int missing,
      Double divineChaos,
      String league,
      String pricesAt,
      // 단계별(C455) — 요청 steps 와 같은 순서
      List<StepCost> steps) {}

  /** 한 단계 비용(C455) — 쓴 화폐 이름(개수 붙임) · 한국어 · 카오스(시세 없는 화폐가 끼면 null). */
  public record StepCost(List<String> names, List<String> namesKo, Double chaos) {}

  /**
   * 에뮬레이터 단계 기록 한 줄(C455, PoE2 C453 의 짝) — no = 단계 번호(1부터), chaos · cumChaos = 그 단계 · 누적 쓴 화폐(모르면
   * null), added · removed = 붙은 · 지운 옵션 문구, undoTo = 이 단계 뒤 상태로 되돌리는 이력 크기(지금 상태 · 기록 한도 밖이면 null),
   * now = 지금 상태, note = 아이템을 안 바꾸는 단계(머리카락 · 신성한 오브 · 축복의 오브) 설명.
   */
  public record HistRow(
      int no,
      String label,
      Double chaos,
      Double cumChaos,
      List<String> added,
      List<String> removed,
      Integer undoTo,
      boolean now,
      String note) {}

  /** 계산기 목표 하나(10-15 C336) — anyOf 중 하나가 붙으면 충족. */
  public record Target(List<String> anyOf) {}

  public record CalcRequest(
      Item item, List<Target> targets, List<String> methods, String essence, Integer trials) {}

  /** 방법 하나 — chance = 시도 한 번 성공 확률, 비용은 카오스(시세가 없으면 null). */
  public record MethodResult(
      String method,
      String label,
      int trials,
      int hits,
      double chance,
      Double expectedAttempts,
      java.util.Map<String, Double> perAttempt,
      Double costPerAttempt,
      Double expectedCost,
      String note) {}

  /**
   * 다단계 전략 한 단계(10-15 C339) — action "SEQ" 면 sub 를 차례로 한 번씩(묶음을 반복). until = 채울 목표 번호들(없으면 한 번).
   */
  public record Step(String action, String arg, List<Integer> until, Integer max, List<Step> sub) {}

  public record StrategyRequest(
      Item item, List<Target> targets, List<Step> steps, Integer trials) {}

  public record StepStat(String action, String arg, double avgUses, double reached) {}

  public record StrategyResult(
      int runs,
      int successes,
      double successRate,
      java.util.Map<String, Double> perRun,
      Double costPerRun,
      Double expectedCost,
      List<StepStat> steps,
      String error) {}

  public record CalcResponse(
      List<MethodResult> results, String league, String pricesAt, String error) {}

  /** 최적 조합 요청(C457) — upper · upperLabel = 계산기 표의 가장 싼 방법(비교 기준 · 넘길 곳). */
  public record OptimizeRequest(
      Item item,
      List<Target> targets,
      Integer budgetMs,
      Double upper,
      String upperLabel,
      List<String> methods) {}

  /** 최적 조합 백그라운드 작업 상태(C457k) — state = running | done. */
  public record OptimizeStatus(
      String id, String state, long elapsedMs, long budgetMs, OptimizeResult result) {}

  /** 에뮬레이터 따라 하기(C464) — 이 아이템 상태에서 최적 조합 정책의 다음 수단(PoE1 · PoE2 같은 모양). */
  public record OptimizeNext(
      boolean done,
      boolean known,
      String state,
      String action,
      String actionId,
      Double remaining) {}

  /** 다음 수단 요청(PoE1) — imprint = 에뮬레이터 각인(없으면 null). */
  public record OptimizeNextRequest(Item item, Item imprint) {}

  /** 최적 조합 상태별 결정 — 상태 설명 · 수단 · 지나는 비율 · 그 상태에서 남은 기대 비용(카오스). */
  public record OptimizeRule(String state, String action, double share, Double remaining) {}

  /** 최적 조합 결과(C457) — expectedCost = 정책을 실제 아이템에 굴려 잰 성공 한 번당 비용(카오스) ± stderr. */
  public record OptimizeResult(
      Double expectedCost,
      Double stderr,
      Double planCost,
      Double upper,
      String upperLabel,
      int states,
      int runs,
      int successes,
      java.util.Map<String, Double> perSuccess,
      java.util.Map<String, String> namesKo,
      String path,
      List<OptimizeRule> rules,
      List<String> methods,
      String league,
      String pricesAt,
      String note,
      String error) {}

  public record PoolRequest(Item item, String rarity) {}
}
