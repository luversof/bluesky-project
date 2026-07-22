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
    /** 반경 라벨(Small/Medium/Large/Very Large) — "…in Radius" 모드를 가진 주얼만 값이 있다. */
    String radius,
    List<String> implicits,
    List<String> implicitsKo,
    List<String> explicits,
    List<String> explicitsKo,
    Integer reqStr,
    Integer reqDex,
    Integer reqInt,
    String iconKey) {}
