package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.TaxYearInputService;
import net.luversof.api.stock.web.dto.request.TaxYearInputUpsertRequest;
import net.luversof.api.stock.web.dto.response.TaxYearInputResponse;

/**
 * 귀속연도 세금 입력값
 *
 * <p>다른 종합소득 과세표준과 앱 밖 금융소득. 종소세 신고 자료 계산에 쓴다.
 */
@RestController
@RequestMapping("/api/taxYearInput")
public class TaxYearInputController {

  @Autowired private TaxYearInputService taxYearInputService;

  /**
   * 귀속연도 세금 입력값 조회
   *
   * @param userId 사용자
   * @param year 귀속연도
   * @return 저장된 값. 없으면 두 값이 null
   */
  @GetMapping
  public TaxYearInputResponse find(
      @RequestParam(required = false) UUID userId, @RequestParam(required = false) Integer year) {
    return taxYearInputService.find(userId, year);
  }

  /**
   * 귀속연도 세금 입력값 저장
   *
   * <p>보낸 값으로 통째로 덮는다. 비운 값은 null(모름)로 남는다.
   *
   * @param request 사용자 · 귀속연도 · 두 값
   * @return 저장한 값
   */
  @PostMapping
  public TaxYearInputResponse upsert(@RequestBody TaxYearInputUpsertRequest request) {
    return taxYearInputService.upsert(request);
  }
}
