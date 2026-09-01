package net.luversof.api.stock.web.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.StockItemService;

@RestController
@RequestMapping("/api/stockItem")
public class StockItemController {

  @Autowired private StockItemService stockItemService;

  public void setStockItemService(StockItemService stockItemService) {
    this.stockItemService = stockItemService;
  }

  @PostMapping
  public StockItem createStockItem(@RequestBody StockItem stockItem) {
    return stockItemService.createStockItem(stockItem);
  }

  @GetMapping("/{id}")
  public Optional<StockItem> getStockItemById(@PathVariable UUID id) {
    return stockItemService.findById(id);
  }

  @GetMapping("/search/findByName/{name}")
  public StockItem findByName(@PathVariable String name) {
    return stockItemService.findByName(name);
  }

  @GetMapping("/search/findAll")
  public java.util.List<net.luversof.api.stock.domain.StockItem> findAll() {
    return stockItemService.findAll();
  }

  /**
   * 한 종목의 일별 종가(차트용). 기간을 주지 않으면 전 구간.
   *
   * <p>거래량 0 인 날은 빠진다 - 그 행의 종가 자리에는 직전 종가가 들어 있어 거래가 없던 날에 선이 이어진다.
   */
  @GetMapping("/{id}/priceHistory")
  public java.util.List<net.luversof.api.stock.web.dto.response.StockPriceHistoryPoint>
      priceHistory(
          @PathVariable UUID id,
          @org.springframework.web.bind.annotation.RequestParam(required = false)
              @org.springframework.format.annotation.DateTimeFormat(
                  iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
              java.time.LocalDate startDate,
          @org.springframework.web.bind.annotation.RequestParam(required = false)
              @org.springframework.format.annotation.DateTimeFormat(
                  iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
              java.time.LocalDate endDate) {
    return stockItemService.findDailyClosePrices(id, startDate, endDate);
  }

  @GetMapping("/search/findAllByTag/{tag}")
  public java.util.List<net.luversof.api.stock.domain.StockItem> findAllByTag(
      @PathVariable String tag) {
    return stockItemService.findAllByTag(tag);
  }
}
