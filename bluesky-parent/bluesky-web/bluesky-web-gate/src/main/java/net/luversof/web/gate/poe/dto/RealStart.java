package net.luversof.web.gate.poe.dto;

/**
 * poe.ninja 실빌드 출발점 — 아키타입(전직×스킬)의 대표 실빌드(우리 엔진·표준 가정으로 재계산한 정규화 PoB 코드 포함). bluesky-api-poe {@code
 * /api/poe/optimize/real-start} 응답과 필드명 일치.
 *
 * <p>최적화기(빈 빌드에서 탐욕 선택)는 실빌드 생존력의 원천인 주얼·고유 조합에 도달하지 못해(2026-09-30 실측 EHP 0.17~0.55x) 시뮬레이터 결과 옆에
 * "실빌드에서 출발한 빌드"를 함께 보여 준다.
 */
public record RealStart(
    String ascendancy,
    String mainSkill,
    String name,
    Integer level,
    Double dps,
    Double ehp,
    Double life,
    Double energyShield,
    Double netRegen,
    int sample,
    Double medianDps,
    Double medianEhp,
    String league,
    String code) {}
