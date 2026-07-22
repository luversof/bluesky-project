package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 트리 에디터에서 찍은 트리를 PoB 엔진으로 실계산한 결과 — API {@code /api/poe/build/tree-stats} 응답 매핑용.
 *
 * <p>최적화기와 달리 탐색이 없어 엔진 1회 호출로 끝난다(장비/보조젬 없음 = 순수 트리 기여분).
 */
public record PoeTreeEvaluation(
    String className,
    String classNameKo,
    String ascendancy,
    String gemName,
    String gemNameKo,
    int nodeCount,
    List<PoeBuild.PlayerStat> stats,
    String pobCode,
    long durationMs,
    List<TreeJewel> jewels,
    // 공격 스킬은 무기 없이 계산이 성립하지 않아 표준 무기를 가정한다(주문은 null)
    String assumedWeapon,
    String assumedWeaponKo) {

  /** 트리 평가에 실제 장착된 주얼(소켓 미할당·미존재 slug 는 API 가 걸러낸다) */
  public record TreeJewel(String slug, String name, String nameKo) {}
}
