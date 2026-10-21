package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 모드 페이지의 아이템 클래스 목록 항목 — API {@code /api/poe/mods/item-classes} 응답 매핑용. prefixes/suffixes 는 패밀리 키
 * 목록(개수 표시·목차용).
 */
public record PoeModItemClass(
    String itemClass,
    String name,
    String nameKo,
    /** 속성 변형(방어구 힘/민첩/지능 등) — 변형마다 붙는 모드가 다르다. 없으면 빈 목록. */
    List<Variant> variants,
    /** 영향력(쉐이퍼/엘더/정복자 4종) — 영향력 아이템에만 붙는 전용 모드가 있다. 없으면 빈 목록. */
    List<Influence> influences,
    List<String> prefixes,
    List<String> suffixes) {

  /** 속성 변형 하나 — enchant · enchantKo = 군집 주얼 인챈트 줄(C514, 그 밖의 변형은 null). */
  public record Variant(
      String key,
      String name,
      String nameKo,
      int prefixCount,
      int suffixCount,
      // 이 변형의 베이스 이름(C516 — 주얼 색 · 심연 주얼 종류 · 군집 주얼은 하나)
      List<String> bases,
      List<String> enchant,
      List<String> enchantKo) {}

  /** 영향력 하나 — extraCount = 영향력 전용으로 더 붙는 패밀리 수. */
  public record Influence(String key, String name, String nameKo, int extraCount) {}
}
