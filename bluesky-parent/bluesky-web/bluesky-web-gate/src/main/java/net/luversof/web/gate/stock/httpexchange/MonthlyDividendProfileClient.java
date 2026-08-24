package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse;

@HttpExchange(
    url = "/api/monthlyDividendProfile",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface MonthlyDividendProfileClient {

  @GetExchange
  List<MonthlyDividendProfileResponse> findProfiles(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  @PostExchange
  MonthlyDividendProfileResponse upsertProfile(
      @RequestBody MonthlyDividendProfileUpsertRequest request);

  @PutExchange("/order")
  void reorderProfiles(@RequestBody MonthlyDividendProfileReorderRequest request);

  @DeleteExchange
  void deleteProfile(@RequestParam String symbol);
}
