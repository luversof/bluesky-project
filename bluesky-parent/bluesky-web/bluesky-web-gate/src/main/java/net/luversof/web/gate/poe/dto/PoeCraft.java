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
      boolean corrupted) {}

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
      int maxPerGen) {}

  /** 옵션 풀 한 줄 — chance = 다음 옵션 하나가 이것일 확률(0~1). */
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

  public record ApplyRequest(Item item, String action, String essence, Long seed) {}

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

  public record CalcResponse(
      List<MethodResult> results, String league, String pricesAt, String error) {}

  public record PoolRequest(Item item, String rarity) {}
}
