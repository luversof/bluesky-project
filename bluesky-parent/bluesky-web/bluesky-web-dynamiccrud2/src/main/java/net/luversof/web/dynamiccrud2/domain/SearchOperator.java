package net.luversof.web.dynamiccrud2.domain;

import java.util.List;

/**
 * 메뉴 검색 조건의 비교 방식. 쓸 수 있는 방식은 필드 입력 형식이 정한다(첫 번째가 기본).
 *
 * <p>화면 이름은 검색 도구에서 흔히 쓰는 완전 · 앞부분 · 부분 일치로 맞췄다(2026-11-18, "포함 · 시작 · 같음" 이 무슨 검색인지 바로 안 읽힌다는 의견).
 */
public enum SearchOperator {
  CONTAINS("부분 일치"),
  STARTS_WITH("앞부분 일치"),
  EQUALS("완전 일치"),
  RANGE("범위");

  private final String title;

  SearchOperator(String title) {
    this.title = title;
  }

  public String title() {
    return title;
  }

  /**
   * 글자는 완전 · 앞부분 · 부분 일치, 숫자 · 날짜는 범위 · 완전 일치, 참/거짓은 완전 일치. 글자 기본은 완전 일치 - 인덱스를 쓰고, 부분 일치를 기본으로 둘
   * 일은 드물다(사용자 의견 2026-11-18)
   */
  public static List<SearchOperator> allowedFor(InputType inputType) {
    return switch (inputType == null ? InputType.TEXT : inputType) {
      case TEXT, TEXTAREA -> List.of(EQUALS, STARTS_WITH, CONTAINS);
      case NUMBER, DATE, DATETIME -> List.of(RANGE, EQUALS);
      case BOOLEAN -> List.of(EQUALS);
    };
  }

  /** 글자 검색(LIKE) 인가 - 통합 검색에도 이것만 쓸 수 있다 */
  public static boolean isText(InputType inputType) {
    return inputType == null || inputType == InputType.TEXT || inputType == InputType.TEXTAREA;
  }
}
