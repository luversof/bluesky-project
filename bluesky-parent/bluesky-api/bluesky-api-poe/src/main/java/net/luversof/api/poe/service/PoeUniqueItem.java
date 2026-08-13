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
    String iconKey) {}
