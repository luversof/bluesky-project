package net.luversof.api.poe.service;

import java.util.Locale;

/**
 * PoE1 목록 검색어 비교(10-02 C23) — PoE2({@code Poe2DataService.norm})와 같은 규칙: 대소문자 · 공백 무시 부분 일치.
 *
 * <p>PoE1 은 공백을 그대로 비교해 "가시나무갑옷" 이 "가시나무 갑옷" 을 못 찾았다(PoE2 는 찾음). 한국어 이름은 띄어쓰기가 사람마다 달라 공백을 빼고 맞댄다.
 */
public final class PoeSearchText {

  private PoeSearchText() {}

  /** 검색어 정규화 — 비었거나 공백뿐이면 null(= 거르지 않음). */
  public static String query(String raw) {
    String normalized = norm(raw);
    return normalized.isEmpty() ? null : normalized;
  }

  /** 정규화한 검색어가 이름 중 하나에 들어 있는가(검색어가 null 이면 늘 참). */
  public static boolean matches(String normalizedQuery, String... names) {
    if (normalizedQuery == null) {
      return true;
    }
    for (String name : names) {
      if (name != null && norm(name).contains(normalizedQuery)) {
        return true;
      }
    }
    return false;
  }

  /** 소문자 + 모든 공백 문자 제거. */
  static String norm(String text) {
    if (text == null) {
      return "";
    }
    String lower = text.toLowerCase(Locale.ROOT);
    StringBuilder out = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (!Character.isWhitespace(c) && c != (char) 0x00A0) {
        out.append(c);
      }
    }
    return out.toString();
  }
}
