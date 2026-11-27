package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.tax.DividendTaxReportService;
import net.luversof.api.stock.web.dto.response.DividendTaxReport;

/**
 * 배당 종소세 신고 자료
 *
 * <p>귀속연도 배당을 일반 · 고배당 · 과세이연으로 나누고, 고배당 분리과세를 신청할 때와 안 할 때의 종합과세 여부와 추가 납부 추정액을 낸다.
 */
@RestController
@RequestMapping("/api/dividendTaxReport")
public class DividendTaxReportController {

  @Autowired private DividendTaxReportService dividendTaxReportService;

  /**
   * 귀속연도 신고 자료 조회
   *
   * @param userId 사용자
   * @param year 귀속연도(지급일 기준, 한국 시간)
   * @return 신고 자료
   */
  @GetMapping
  public DividendTaxReport report(
      @RequestParam(required = false) UUID userId, @RequestParam(required = false) Integer year) {
    return dividendTaxReportService.report(userId, year);
  }
}
