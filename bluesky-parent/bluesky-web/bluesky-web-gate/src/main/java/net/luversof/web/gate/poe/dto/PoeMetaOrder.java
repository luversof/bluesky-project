package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 실빌드 사용 빈도 순서 — 시뮬 폼 목록 정렬용(API {@code /api/poe/meta/order}).
 *
 * <p>이름은 모두 <b>영문</b>이다(ninja 원본 표기). 화면은 한글로 보여도 정렬 대조는 영문 이름으로 한다.
 */
public record PoeMetaOrder(
    List<String> skills,
    List<String> ascendancies,
    List<String> items,
    // 순서의 근거 — poe.ninja 리그·스냅샷 버전·받은 시각(ISO). 새 버전이 나오면 API 가 다시 받는다(PoeNinjaSyncService).
    String league,
    String snapshot,
    String fetchedAt) {}
