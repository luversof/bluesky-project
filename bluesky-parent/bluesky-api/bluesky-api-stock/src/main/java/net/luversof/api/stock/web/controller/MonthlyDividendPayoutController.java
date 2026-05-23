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

  @GetMapping
  public List<MonthlyDividendPayoutResponse> findPayouts(MonthlyDividendPayoutRequest request) {
    return monthlyDividendPayoutService.findPayouts(request);
  }

  @PostMapping
  public MonthlyDividendPayoutResponse upsertPayout(
      @RequestBody MonthlyDividendPayoutUpsertRequest request) {
    return monthlyDividendPayoutService.upsert(request);
  }

  @DeleteMapping
  public void deletePayout(
      @RequestParam String symbol,
      @RequestParam LocalDate recordDate,
      @RequestParam LocalDate payDate) {
    monthlyDividendPayoutService.deleteBySymbolAndDates(symbol, recordDate, payDate);
  }
}
