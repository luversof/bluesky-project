package net.luversof.web.gate.poe;

import java.util.Map;

/**
 * 삿된(Foulborn) 화면 분류 이름(10-02 C15) — 데이터(foulborn-mods.json)의 분류는 한글뿐이라(필터 값도 한글) 영어 화면 칩 · 배지가 "갑옷
 * · 반지" 로 나왔다. 한글 분류 → 게임 영어 이름(ItemClasses 복수형 — 다른 영어 칩 "Claws · Wands" 와 같은 꼴).
 *
 * <p>한글 분류는 파서(tools/poe-extract/parse-foulborn.mjs CATEGORY_KO · UNIQUE_CATEGORY_KO)가 정하는 닫힌 목록이다
 * — 새 분류가 생기면 {@code PoeFoulbornCategoriesTest} 가 파서 파일과 대조해 깨진다. 표에 없으면 한글 그대로(빈칸보다 낫다).
 */
public final class PoeFoulbornCategories {

  private static final Map<String, String> EN =
      Map.ofEntries(
          Map.entry("주얼", "Jewels"),
          Map.entry("목걸이", "Amulets"),
          Map.entry("반지", "Rings"),
          Map.entry("허리띠", "Belts"),
          Map.entry("투구", "Helmets"),
          Map.entry("갑옷", "Body Armours"),
          Map.entry("장갑", "Gloves"),
          Map.entry("장화", "Boots"),
          Map.entry("방패", "Shields"),
          Map.entry("화살통", "Quivers"),
          Map.entry("활", "Bows"),
          Map.entry("마법봉", "Wands"),
          Map.entry("단검", "Daggers"),
          Map.entry("클로", "Claws"),
          Map.entry("검", "Swords"),
          Map.entry("한손검", "One Hand Swords"),
          Map.entry("양손검", "Two Hand Swords"),
          Map.entry("도끼", "Axes"),
          Map.entry("한손도끼", "One Hand Axes"),
          Map.entry("양손도끼", "Two Hand Axes"),
          Map.entry("철퇴", "Maces"),
          Map.entry("한손철퇴", "One Hand Maces"),
          Map.entry("양손철퇴", "Two Hand Maces"),
          Map.entry("지팡이", "Staves"),
          Map.entry("셉터", "Sceptres"),
          Map.entry("플라스크", "Flasks"),
          Map.entry("팅크제", "Tinctures"),
          Map.entry("낚싯대", "Fishing Rods"));

  private PoeFoulbornCategories() {}

  /** 화면 언어에 맞는 분류 이름 — 한국어면 그대로, 영어면 게임 영어 이름(표에 없으면 한글 그대로). */
  public static String label(String ko) {
    return PoeText.name(ko, en(ko));
  }

  /** 한글 분류의 영어 이름(없으면 한글 그대로). */
  static String en(String ko) {
    if (ko == null) {
      return null;
    }
    return EN.getOrDefault(ko, ko);
  }

  static Map<String, String> table() {
    return EN;
  }
}
