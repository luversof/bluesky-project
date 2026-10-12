package net.luversof.web.gate.poe.dto;

import java.util.List;

/** tools/poe-extract parse-uniques.mjs 가 생성한 표시용 고유 아이템 (PoB 데이터 + 한국어 이름 결합). */
public record PoeUniqueItem(
    String name,
    String nameKo,
    String slug,
    String baseType,
    String baseTypeKo,
    String category,
    Integer requiredLevel,
    String league,
    /** 지금 리그에선 못 얻는 고유(데미갓·Fated·삭제된 레거시). 판정은 게임의 고유 수집 탭 표시 플래그. */
    boolean legacy,
    /** 반경 라벨(Small/Medium/Large/Very Large) — "…in Radius" 모드를 가진 주얼만 값이 있다. */
    String radius,
    List<String> implicits,
    List<String> implicitsKo,
    List<String> explicits,
    List<String> explicitsKo,
    /** 인게임에 동시에 존재하는 변형 전수(임프레션스 5원소 등). 변형이 하나뿐이면 null. */
    List<PoeUniqueVariant> variants,
    /** implicits/explicits 가 어느 변형의 것인지(1-base). variants 가 null 이면 null. */
    Integer defaultVariant,
    Integer reqStr,
    Integer reqDex,
    Integer reqInt,
    String iconKey,
    // 로어(플레이버) 텍스트 줄 — 인게임 툴팁 맨 아래 주황 기울임(API PoeUniqueItem 과 쌍)
    List<String> flavour,
    List<String> flavourKo,
    // 옵션 줄별 인게임 리마인더(회색 부연), explicits 와 같은 길이(10-04 C114, API 와 쌍). 없으면 null
    List<List<String>> explicitsReminders,
    List<List<String>> explicitsRemindersKo,
    // 중복 사용 제한(API 와 짝, 10-08 C197) — 없으면 null
    String limit,
    // 영향력(API 와 짝, 10-08 C198) — 없으면 null
    List<String> influences,
    // 팅크처 베이스 속성(PoB Bases tincture.lua manaBurn · cooldown, 인게임 "N초마다 마나 연소 유발" · "비활성화 시 재사용 대기시간
    // N초",
    // 10-09 C211). 팅크처 아니면 null
    Tincture tincture,
    // 부적 등급(PoB "Talisman Tier: 2", 인게임 "부적 등급: 2", 10-09 C214). 부적 아니면 null
    Integer talismanTier) {

  /** 팅크처 베이스 속성 — 마나 연소 간격(초) · 비활성화 뒤 재사용 대기시간(초). */
  public record Tincture(double manaBurn, double cooldown) {}

  /** 팅크처 없는 옛 모양(10-09 C211 전 — 제한 · 영향력까지). */
  public PoeUniqueItem(
      String name,
      String nameKo,
      String slug,
      String baseType,
      String baseTypeKo,
      String category,
      Integer requiredLevel,
      String league,
      boolean legacy,
      String radius,
      List<String> implicits,
      List<String> implicitsKo,
      List<String> explicits,
      List<String> explicitsKo,
      List<PoeUniqueVariant> variants,
      Integer defaultVariant,
      Integer reqStr,
      Integer reqDex,
      Integer reqInt,
      String iconKey,
      List<String> flavour,
      List<String> flavourKo,
      List<List<String>> explicitsReminders,
      List<List<String>> explicitsRemindersKo,
      String limit,
      List<String> influences) {
    this(
        name,
        nameKo,
        slug,
        baseType,
        baseTypeKo,
        category,
        requiredLevel,
        league,
        legacy,
        radius,
        implicits,
        implicitsKo,
        explicits,
        explicitsKo,
        variants,
        defaultVariant,
        reqStr,
        reqDex,
        reqInt,
        iconKey,
        flavour,
        flavourKo,
        explicitsReminders,
        explicitsRemindersKo,
        limit,
        influences,
        null,
        null);
  }

  /** 제한 없는 옛 모양(게이트가 고유를 다시 짓는 곳 · 시험용). */
  public PoeUniqueItem(
      String name,
      String nameKo,
      String slug,
      String baseType,
      String baseTypeKo,
      String category,
      Integer requiredLevel,
      String league,
      boolean legacy,
      String radius,
      List<String> implicits,
      List<String> implicitsKo,
      List<String> explicits,
      List<String> explicitsKo,
      List<PoeUniqueVariant> variants,
      Integer defaultVariant,
      Integer reqStr,
      Integer reqDex,
      Integer reqInt,
      String iconKey,
      List<String> flavour,
      List<String> flavourKo,
      List<List<String>> explicitsReminders,
      List<List<String>> explicitsRemindersKo) {
    this(
        name,
        nameKo,
        slug,
        baseType,
        baseTypeKo,
        category,
        requiredLevel,
        league,
        legacy,
        radius,
        implicits,
        implicitsKo,
        explicits,
        explicitsKo,
        variants,
        defaultVariant,
        reqStr,
        reqDex,
        reqInt,
        iconKey,
        flavour,
        flavourKo,
        explicitsReminders,
        explicitsRemindersKo,
        null,
        null,
        null,
        null);
  }
}
