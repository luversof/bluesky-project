package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.DividendMetaResponse;
import net.luversof.web.gate.stock.dto.response.DividendResponse;

@HttpExchange(url = "/api/dividend", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface DividendClient {

  @GetExchange
  List<DividendResponse> findDividends(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  /** 배당 메타(최초 기준일 + 배당 보유 종목 ID). 전 기간 배당 이력을 내려받던 것을 대체한다. */
  @GetExchange("/meta")
  DividendMetaResponse findDividendMeta(
      @org.springframework.web.bind.annotation.RequestParam java.util.UUID userId);
}
