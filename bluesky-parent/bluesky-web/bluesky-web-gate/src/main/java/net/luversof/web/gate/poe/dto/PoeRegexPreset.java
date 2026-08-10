package net.luversof.web.gate.poe.dto;

import java.util.Map;

/**
 * 지도 정규식 프리셋 — bluesky-api-poe 의 RegexPreset 대응. data 는 화면 상태 전체(선택 모드·임계값·옵션)를 클라이언트 정의 그대로 보관해
 * 불러오기/편집 시 화면을 복원한다.
 */
public record PoeRegexPreset(
    long id, String name, long updatedMs, String regex, Map<String, Object> data) {

  /** 목록용 요약 */
  public record Entry(long id, String name, long updatedMs, String regex) {}

  /** 저장 요청(id 있으면 편집) */
  public record SaveRequest(Long id, String name, String regex, Map<String, Object> data) {}
}
