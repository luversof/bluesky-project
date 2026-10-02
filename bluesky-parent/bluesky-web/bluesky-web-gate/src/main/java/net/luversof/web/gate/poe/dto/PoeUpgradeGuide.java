package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 업그레이드 가이드 응답 — bluesky-api-poe 의 PoeUpgradeGuideService 레코드와 필드 이름을 맞춘다.
 *
 * <p>⚠ 숫자 필드는 래퍼 타입이다 — Jackson 3 은 primitive 필드가 응답에 없으면 실패한다(FAIL_ON_NULL_FOR_PRIMITIVES 기본 ON).
 */
public final class PoeUpgradeGuide {

  private PoeUpgradeGuide() {}

  /** 기준선 지표. maxHit = 다섯 피해 유형 최대피격 중 최솟값(가장 약한 곳). minionEhp = 주 스킬 소환수의 EHP(소환수 빌드가 아니면 0). */
  public record Metrics(Double dps, Double ehp, Double maxHit, Double minionEhp) {

    /** 소환수 빌드인지 — 소환수 EHP 를 머리에 보일지 가른다. */
    public boolean hasMinion() {
      return minionEhp != null && minionEhp > 0;
    }
  }

  /**
   * 약한 칸에 끼워 재 본 교체 후보 한 개 — 고유(보통 롤) 또는 같은 베이스의 2티어 레어 목표.
   *
   * @param rarity UNIQUE | RARE
   * @param mods 레어 목표에 붙인 옵션(표시용 한국어, 2티어 범위) — 고유면 null
   * @param minionPct 소환수 EHP 증감(%) — 소환수 빌드가 아니면 null
   * @param metaCount 이 전직·주 스킬 조합의 poe.ninja 캐릭터 중 이 아이템을 쓰는 수(모르면 0)
   * @param metaTotal 그 조합의 캐릭터 수(모르면 0)
   * @param needs 이 교체로 새로 모자라게 되는 요구 능력치("힘 25 · 민첩 34") — 그대로 끼울 수 있으면 null
   */
  public record ItemPick(
      String rarity,
      String slug,
      String name,
      String nameKo,
      String baseType,
      String baseTypeKo,
      List<String> mods,
      Double dpsPct,
      Double ehpPct,
      Double maxHitPct,
      Double minionPct,
      Integer metaCount,
      Integer metaTotal,
      String needs,
      String itemText) {

    /** 실빌드 사용률(%) — 모르면 null. 1% 미만도 1 로 올려 "쓰는 사람이 있다"는 사실을 0 으로 지우지 않는다. */
    public Integer metaPercent() {
      if (metaCount == null || metaTotal == null || metaCount <= 0 || metaTotal <= 0) {
        return null;
      }
      return (int) Math.max(1, Math.round(100.0 * metaCount / metaTotal));
    }
  }

  /**
   * 제안 한 건.
   *
   * @param kind free(측정 없이 확정되는 공짜 수정) · support(보조젬 교체) · item(약한 장비 칸)
   * @param dpsPct 측정한 DPS 증감(%) — 측정 없는 제안이면 null
   * @param minionPct 측정한 소환수 EHP 증감(%) — 측정 없는 제안이거나 소환수 빌드가 아니면 null
   * @param picks item 제안일 때 그 칸에 끼워 재 본 고유 아이템 추천(없으면 빈 목록, 다른 종류면 null)
   */
  public record Suggestion(
      String kind,
      String title,
      String detail,
      Double dpsPct,
      Double ehpPct,
      Double maxHitPct,
      Double minionPct,
      List<ItemPick> picks) {}

  public record Result(
      String className,
      String ascendancy,
      Integer level,
      String mainSkill,
      Metrics baseline,
      List<Suggestion> suggestions,
      Integer evaluations,
      Long durationMs) {}

  public record Status(
      Boolean running, Integer done, Integer total, String phase, Result result, String error) {}

  /** 자동 다듬기 한 단계 — 적용한 교체와 그 단계에서 오른 폭(직전 빌드 대비 %). */
  public record RefineStep(String label, Double dpsPct, Double ehpPct, Double maxHitPct) {}

  /** 자동 다듬기 결과 — code 는 다듬은 빌드 PoB 코드(저장 스탯 = 엔진 최종 값). */
  public record RefineResult(
      Metrics before,
      Metrics after,
      List<RefineStep> steps,
      String code,
      Integer evaluations,
      Long durationMs) {}

  public record RefineStatus(
      Boolean running,
      Integer round,
      Integer rounds,
      String phase,
      Integer done,
      Integer total,
      RefineResult result,
      String error) {}
}
