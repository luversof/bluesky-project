package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.MonthlyDividendPayoutService;
import net.luversof.api.stock.web.dto.request.MonthlyDividendPayoutRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendPayoutResponse;

@RestController
@RequestMapping("/api/monthlyDividendPayout")
public class MonthlyDividendPayoutController {

  @Autowired private MonthlyDividendPayoutService monthlyDividendPayoutService;

  /**
   * 월배당 지급 이력을 찾는다.
   *
   * @param request 종목 · 기간 조건
   * @return 지급 이력
   */
  @GetMapping
  public List<MonthlyDividendPayoutResponse> findPayouts(MonthlyDividendPayoutRequest request) {
    return monthlyDividendPayoutService.findPayouts(request);
  }

  /**
   * 월배당 지급 이력 한 건을 넣는다. 같은 종목 · 기준일 · 지급일이 있으면 고친다.
   *
   * @param request 종목 심볼 · 기준일 · 지급일 · 분배율 · 주당 금액 · 과세표준
   * @return 저장한 지급 이력
   */
  @PostMapping
  public MonthlyDividendPayoutResponse upsertPayout(
      @RequestBody MonthlyDividendPayoutUpsertRequest request) {
    return monthlyDividendPayoutService.upsert(request);
  }

  /**
   * 월배당 지급 이력 한 건을 지운다.
   *
   * @param symbol 종목 심볼
   * @param recordDate 기준일
   * @param payDate 지급일
   */
  @DeleteMapping
  public void deletePayout(
      @RequestParam String symbol,
      @RequestParam LocalDate recordDate,
      @RequestParam LocalDate payDate) {
    monthlyDividendPayoutService.deleteBySymbolAndDates(symbol, recordDate, payDate);
  }
}
