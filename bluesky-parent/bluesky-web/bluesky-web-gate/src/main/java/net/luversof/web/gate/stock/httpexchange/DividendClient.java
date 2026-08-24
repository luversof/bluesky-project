package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.DividendMetaResponse;
import net.luversof.web.gate.stock.dto.response.DividendResponse;

@HttpExchange(
    url = "/api/dividend",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface DividendClient {

  @GetExchange
  List<DividendResponse> findDividends(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  /** 같은 조건(계좌/종목/기간)의 세후 배당 합계. 합계 하나 때문에 목록 전체를 받지 않는다. */
  @GetExchange("/total")
  java.math.BigDecimal findDividendTotal(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  /**
   * 같은 조건의 세후 배당 합계를 종목별로 나눈 것. 합계는 이 값들의 합이므로 {@code /total} 을 따로 부르지 않는다.
   *
   * <p>종목 승률(수익권 종목 비율)이 배당을 빼고 세던 것을 고치려고 들어왔다. 목록 전체(실측 193건 79,919 바이트)를 되받지 않는다(실측: 이 사용자 18행).
   */
  @GetExchange("/totalByStockItem")
  java.util.Map<java.util.UUID, java.math.BigDecimal> findDividendTotalByStockItem(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  /** 배당 메타(최초 기준일 + 배당 보유 종목 ID). 전 기간 배당 이력을 내려받던 것을 대체한다. */
  @GetExchange("/meta")
  DividendMetaResponse findDividendMeta(
      @org.springframework.web.bind.annotation.RequestParam java.util.UUID userId);
}
