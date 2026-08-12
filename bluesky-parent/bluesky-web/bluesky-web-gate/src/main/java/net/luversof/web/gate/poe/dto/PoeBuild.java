package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * Path of Building 공유 코드에서 임포트한 캐릭터 빌드 모델. 시뮬레이터(Phase 4)의 탐색 단위이기도 하다.
 *
 * <p>PoB 코드에는 PoB가 계산해둔 스탯(PlayerStat)이 포함되어 있어 엔진 없이도 요약 표시가 가능하다.
 */
public record PoeBuild(
    String className,
    String classNameKo,
    String ascendClassName,
    String ascendClassNameKo,
    int level,
    String treeVersion,
    List<PlayerStat> stats,
    List<Integer> passiveNodeIds,
    List<SkillGroup> skillGroups,
    List<BuildItem> items) {

  /** PoB가 계산한 캐릭터 스탯 한 줄. label 은 uiMessage 의 {@code poe.build.stat.<key>} 로 표시한다. */
  public record PlayerStat(String key, String value) {}

  /** 소켓 그룹(연결된 젬 묶음). slot 은 PoB 기준 장착 부위명(영문, 없을 수 있음). */
  public record SkillGroup(String slot, String slotKo, boolean enabled, List<BuildGem> gems) {}

  /** 그룹 내 젬 하나. slug 가 있으면 우리 젬 DB와 매칭된 것 (상세 레이어 링크 가능). */
  public record BuildGem(
      String name,
      String nameKo,
      String slug,
      boolean isSupport,
      int level,
      int quality,
      /** 젬 소켓 색(red=힘 green=민첩 blue=지능 white) — 인게임 젬 색 관례로 렌더한다. */
      String color) {}

  /**
   * 장착 아이템 하나. uniqueSlug/baseSlug 가 있으면 각각 고유/일반 아이템 DB와 매칭된 것.
   *
   * @param rarity UNIQUE | RARE | MAGIC | NORMAL | RELIC
   */
  public record BuildItem(
      String slot,
      String slotKo,
      String rarity,
      String name,
      String nameKo,
      String baseType,
      String baseTypeKo,
      String uniqueSlug,
      String baseSlug,
      List<String> modLines,
      List<String> modLinesKo,
      // 인게임 툴팁은 임플리싯(암시)이 구분선 위 별도 섹션이다 — API 가 갈라서 준다.
      List<String> implicitLines,
      List<String> implicitLinesKo,
      /** 인게임 툴팁 맨 아래 빨간 "부패됨" 줄 — PoB 텍스트의 Corrupted 플래그. */
      boolean corrupted,
      /** 아이템 품질 %. 인게임 툴팁은 속성 블록 **첫 줄**에 "품질: +N%%"(값은 매직 파랑)를 둔다. 0이면 표시하지 않는다. */
      int quality,
      PoeBaseItem base) {}
}
