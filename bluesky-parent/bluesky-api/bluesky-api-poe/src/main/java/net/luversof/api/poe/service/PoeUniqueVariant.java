package net.luversof.api.poe.service;

import java.util.List;

/**
 * 고유 아이템의 변형 하나 (PoB 의 {@code Variant:} 블록).
 *
 * <p>같은 이름·같은 베이스인데 모드 구성이 다른 것들 — 임프레션스(물리/화염/냉기/번개/카오스), 도리아니의 망상(정화 오라 × 원소), 희망의 실(고리 크기) 등.
 * 인게임엔 이 전부가 별개 아이템으로 존재하는데 예전엔 마지막 하나만 들고 있었다.
 *
 * @param index PoB 블록에서의 1-base 순번 (모드 줄의 <code>{variant:N}</code> 과 대응)
 * @param name 변형 라벨(영문 원문)
 * @param nameKo 확실히 옮길 수 있을 때만 한글, 아니면 null → 화면은 영문으로 폴백
 */
public record PoeUniqueVariant(
    int index,
    String name,
    String nameKo,
    List<String> implicits,
    List<String> implicitsKo,
    List<String> explicits,
    List<String> explicitsKo) {}
