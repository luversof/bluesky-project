package net.luversof.web.gate.stock.httpexchange;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.stock.domain.StockItem;

@HttpExchange(
    url = "/api/stockItem",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface StockItemClient {

  @PostExchange
  StockItem createStockItem(@RequestBody StockItem stockItem);

  @GetExchange("/{id}")
  Optional<StockItem> getStockItemById(@PathVariable UUID id);

  @GetExchange("/search/findByName/{name}")
  StockItem findByName(@PathVariable String name);

  @GetExchange("/search/findAll")
  java.util.List<StockItem> getStockItems();

  @GetExchange("/search/findAllByTag/{tag}")
  java.util.List<StockItem> getStockItemsByTag(@PathVariable String tag);

  /**
   * 한 종목의 일별 종가(차트용). 기간을 주지 않으면 전 구간.
   *
   * <p>보유 평가액 추이만으로는 주가 자체를 볼 수 없다 &mdash; 평가액은 수량이 바뀌면 같이 움직이기 때문이다.
   */
  @GetExchange("/{id}/priceHistory")
  java.util.List<net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint> getPriceHistory(
      @PathVariable UUID id,
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> params);
}
