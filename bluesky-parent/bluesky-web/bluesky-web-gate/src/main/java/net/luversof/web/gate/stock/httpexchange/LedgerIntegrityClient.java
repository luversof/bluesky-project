package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.LedgerIntegrityResponse;

/** 원장 점검 결과 조회. 관리 화면 표시용(읽기 전용). */
@HttpExchange(
    url = "/api/ledgerIntegrity",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface LedgerIntegrityClient {

  /**
   * @param maxExamples 규칙마다 받을 예시 개수. 기본 3 건으로는 <b>조치할 수 없는</b> 발견이 생긴다 &mdash; 실측 2026-08-23: 발견
   *     45 건 중 25 건(55%)이 예시 밖이라 화면에서 어느 행인지 볼 수 없었다.
   */
  @GetExchange
  LedgerIntegrityResponse check(
      @RequestParam UUID userId, @RequestParam(required = false) Integer maxExamples);
}
