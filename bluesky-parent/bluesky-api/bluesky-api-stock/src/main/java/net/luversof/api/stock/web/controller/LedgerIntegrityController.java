package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.LedgerIntegrityService;
import net.luversof.api.stock.web.dto.response.LedgerIntegrityResponse;

/**
 * 원장 점검 결과 조회. 관리 화면이 "지금 원장에 이상한 값이 있는지"를 보여주는 데 쓴다.
 *
 * <p>읽기 전용이다. 발견한 기록을 고치는 것은 사람이 원장(구글 시트)에서 해야 한다 &mdash; 앱이 임의로 고치면 무엇이 원본인지 알 수 없게 된다.
 */
@RestController
@RequestMapping("/api/ledgerIntegrity")
public class LedgerIntegrityController {

  @Autowired private LedgerIntegrityService ledgerIntegrityService;

  /**
   * @param maxExamples 규칙마다 담을 예시 개수(선택). 기본 3, 상한 100.
   *     <p>기본값만으로는 조치할 수 없는 발견이 생긴다 &mdash; 실측 2026-08-23: 발견 45 건 중 25 건(55%)이 예시 밖이라 화면에서 어느 행인지
   *     볼 수 없었다.
   */
  @GetMapping
  public LedgerIntegrityResponse check(
      @RequestParam UUID userId,
      @RequestParam(required = false, defaultValue = "0") int maxExamples) {
    return ledgerIntegrityService.check(userId, maxExamples);
  }
}
