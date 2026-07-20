package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 그룹 칩 UI용 DTO — bluesky-api-poe 의 {@code /gems/tag-groups}, {@code /base-items/item-class-groups}
 * 응답 매핑(각자 정의). 그룹 라벨은 key 로 게이트가 메시지 해석, 항목 라벨은 데이터의 한국어(ko)/영문(key)을 로케일에 맞춰 표시. 두 응답의 항목 필드명이
 * 다르므로({@code tags}/{@code classes}) 각 record 로 매핑하고, 공통 {@link Group} 로 템플릿을 재사용한다.
 */
public final class PoeGroups {

  private PoeGroups() {}

  /** 한 항목(태그/아이템클래스/카테고리) — key=영문 id(필터값), ko=한국어 라벨, slot=탭 전환 시 필터 유지용 정규 슬롯(젬 태그는 null). */
  public record Entry(String key, String ko, String slot) {}

  /** 그룹 칩 템플릿 공통 뷰 */
  public interface Group {
    String key();

    List<Entry> entries();
  }

  /** 젬 태그 그룹 ({@code /gems/tag-groups}) */
  public record TagGroup(String key, List<Entry> tags) implements Group {
    @Override
    public List<Entry> entries() {
      return tags;
    }
  }

  /**
   * 아이템 클래스 그룹 ({@code /base-items/item-class-groups}, {@code /uniques/category-groups}). 고유도
   * baseType 조인으로 얻은 세부 itemClass 를 같은 shape 으로 내려주므로 일반/고유가 동일 record 를 쓴다.
   */
  public record ClassGroup(String key, List<Entry> classes) implements Group {
    @Override
    public List<Entry> entries() {
      return classes;
    }
  }
}
