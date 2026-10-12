package net.luversof.api.poe.service;

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
    // 지금은 못 얻는 고유(레이스 보상 데미갓, 예언 전용 Fated, 삭제된 레거시 주얼 등).
    // 판정은 게임 데이터 UniqueStashLayout.ShowIfEmpty{ChallengeLeague,Standard} — 둘 다 꺼진 칸이 그것이다.
    boolean legacy,
    // 반경 라벨(Small/Medium/Large/Very Large) — "…in Radius" 모드는 이 줄이 아이템 텍스트에 있어야
    // PoB 가 반경 계산을 한다(없으면 그 모드는 조용히 무효).
    String radius,
    List<String> implicits,
    List<String> implicitsKo,
    List<String> explicits,
    List<String> explicitsKo,
    // 변형(Variant) 전수 — 임프레션스 물리/화염/…, 도리아니의 망상 9종처럼 **인게임에 동시에 존재하는**
    // 선택지들. 과거 버전 보존분(PoB 의 "Pre 3.x")은 추출 단계에서 걸러진다. 변형이 하나뿐인 아이템은 null.
    List<PoeUniqueVariant> variants,
    // variants 가 있을 때, 위 implicits/explicits 가 어느 변형의 것인지(1-base). 없으면 null.
    Integer defaultVariant,
    // 목록/상세 표시용 — baseType→베이스 조인으로 서비스가 채움(JSON 엔 없음). 아이콘 키 = 베이스 slug.
    Integer reqStr,
    Integer reqDex,
    Integer reqInt,
    String iconKey,
    // 로어(플레이버) 텍스트 줄 — 위키 + 게임 FlavourText 매칭(tools/poe-extract/wiki-uniques.mjs, 10-01). 인게임 툴팁 맨
    // 아래 주황 기울임.
    List<String> flavour,
    List<String> flavourKo,
    // 옵션 줄별 인게임 리마인더(회색 부연 — "최근"은 지난 4초 …), explicits 와 같은 길이(10-04 C114). 없으면 null
    List<List<String>> explicitsReminders,
    List<List<String>> explicitsRemindersKo,
    // 중복 사용 제한(주얼 "Limited to: 1" · "1 Historic", PoB 메타 줄 — 인게임 "중복 사용 제한: 1", 10-08 C197). 없으면
    // null
    String limit,
    // 영향력(shaper · elder — PoB "Shaper Item" 줄, 인게임 툴팁 머리 상징, 10-08 C198). 없으면 null
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

  /** 제한 없는 옛 모양 — 최적화기 · 가이드처럼 고유를 다시 짓는 곳이 그대로 쓴다(10-08 C197). */
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

  /** 이름만 — 시뮬 강제 장착 셀렉트처럼 slug · 이름 · 분류 · 변형 이름만 쓰는 곳(10-04 C124: 전체 3MB 대신). */
  public PoeUniqueItem namesOnly() {
    List<PoeUniqueVariant> vs =
        variants == null
            ? null
            : variants.stream()
                .map(
                    v ->
                        new PoeUniqueVariant(
                            v.index(), v.name(), v.nameKo(), null, null, null, null))
                .toList();
    return new PoeUniqueItem(
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
        null,
        null,
        null,
        null,
        vs,
        defaultVariant,
        null,
        null,
        null,
        iconKey,
        null,
        null);
  }

  /** 목록용 — 리마인더(고유 · 변형)를 뺀 사본(10-04 C119). */
  public PoeUniqueItem withoutReminders() {
    List<PoeUniqueVariant> vs =
        variants == null
            ? null
            : variants.stream()
                .map(
                    v ->
                        new PoeUniqueVariant(
                            v.index(),
                            v.name(),
                            v.nameKo(),
                            v.implicits(),
                            v.implicitsKo(),
                            v.explicits(),
                            v.explicitsKo()))
                .toList();
    return new PoeUniqueItem(
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
        vs,
        defaultVariant,
        reqStr,
        reqDex,
        reqInt,
        iconKey,
        flavour,
        flavourKo);
  }

  /** 리마인더 없는 옛 모양 — 최적화기 · 가이드처럼 표시와 무관하게 고유를 다시 짓는 곳이 그대로 쓴다. */
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
      List<String> flavourKo) {
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
        null,
        null);
  }
}
