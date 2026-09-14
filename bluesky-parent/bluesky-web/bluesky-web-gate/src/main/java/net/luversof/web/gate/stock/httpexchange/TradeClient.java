package net.luversof.web.gate.stock.httpexchange;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.TradeResponse;

@HttpExchange(
    url = "/api/trade",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface TradeClient {

  @GetExchange
  List<TradeResponse> findTrades(@RequestParam MultiValueMap<String, String> request);

  /**
   * 종목별 최초 매수일. 자산 현황의 보유 기간·연평균 수익률이 쓴다.
   *
   * <p>날짜 몇 개를 얻자고 원장 목록(실측 2026-09-14: 258 행)을 받지 않는다 &mdash; 종목별 배당 합계를 따로 받는 것과 같은 이유다.
   */
  @GetExchange("/firstBuyDateByStockItem")
  Map<UUID, LocalDate> findFirstBuyDateByStockItem(
      @RequestParam MultiValueMap<String, String> request);
}
