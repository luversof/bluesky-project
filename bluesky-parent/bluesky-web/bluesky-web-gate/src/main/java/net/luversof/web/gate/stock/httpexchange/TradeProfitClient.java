package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;

@HttpExchange(url = "/api/tradeProfit", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface TradeProfitClient {

  @GetExchange("/calculateProfit")
  List<TradeProfit> calculateProfit(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  @GetExchange("/timeSeries")
  List<TradeProfitTimeSeriesPoint> timeSeries(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  @GetExchange("/holdingsSnapshot")
  List<HoldingsSnapshotItem> holdingsSnapshot(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> params);

  /**
   * 여러 날짜의 보유 스냅샷을 1회 호출로 조회한다. 응답 키는 ISO 날짜 문자열(yyyy-MM-dd).
   *
   * <p>날짜별로 {@link #holdingsSnapshot} 을 순차 호출하면 날짜 수만큼 왕복이 생긴다(배당 기준일이 수십 개라 수 초 소요).
   */
  @GetExchange("/holdingsSnapshotBatch")
  java.util.Map<String, List<HoldingsSnapshotItem>> holdingsSnapshotBatch(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> params);
}
