package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendCatalogResponse;

/** 월배당 ETF 목록(종목 단위). 사용자 파라미터가 없다. */
@HttpExchange(
    url = "/api/monthlyDividendCatalog",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface MonthlyDividendCatalogClient {

  @GetExchange
  List<MonthlyDividendCatalogResponse> findCatalog(
      @RequestParam MultiValueMap<String, String> request);
}
