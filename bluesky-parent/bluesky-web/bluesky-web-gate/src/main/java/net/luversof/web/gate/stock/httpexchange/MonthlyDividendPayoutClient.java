package net.luversof.web.gate.stock.httpexchange;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;

@HttpExchange(url = "/api/monthlyDividendPayout", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface MonthlyDividendPayoutClient {

  @GetExchange
  List<MonthlyDividendPayoutResponse> findPayouts(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  @PostExchange
  MonthlyDividendPayoutResponse upsertPayout(
      @RequestBody MonthlyDividendPayoutUpsertRequest request);

  @DeleteExchange
  void deletePayout(
      @RequestParam String symbol,
      @RequestParam LocalDate recordDate,
      @RequestParam LocalDate payDate);
}
