package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 한 아이템 클래스의 전체 모드 풀 — API {@code /api/poe/mods/for-item-class} 응답 매핑용. 접두/접미를 각각 (패밀리 키 + 티어 사다리)로
 * 담는다.
 */
public record PoeModClass(
    String itemClass,
    String name,
    String nameKo,
    /** 선택된 속성 변형 키(str_armour 등). 변형 없는 클래스는 빈 문자열. */
    String variant,
    /** 이 클래스가 가진 속성 변형 목록(없으면 빈 목록). */
    List<PoeModItemClass.Variant> variants,
    /** 선택된 영향력 키(없으면 빈 문자열). */
    String influence,
    /** 이 클래스가 가진 영향력 목록. */
    List<PoeModItemClass.Influence> influences,
    List<NamedFamily> prefixes,
    List<NamedFamily> suffixes,
    /** 바알 오브 부패 임플리싯 패밀리(영향력 무관). 없으면 빈 목록/null. */
    List<NamedFamily> corrupted,
    /** 플라스크 인챈트(주입/점화 오브) 패밀리 — 플라스크 클래스만. */
    List<NamedFamily> enchants) {

  /** 패밀리 키 + 그 패밀리(gen + 티어 목록). */
  public record NamedFamily(String key, Family family) {}

  /** gen = prefix|suffix, essence = 에센스 전용 여부(보통 null), tiers = ilvl 내림차순. */
  public record Family(String gen, Boolean essence, List<PoeModTier> tiers) {}
}
