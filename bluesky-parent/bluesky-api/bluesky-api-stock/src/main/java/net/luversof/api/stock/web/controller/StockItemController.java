package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.List;
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
import net.luversof.api.stock.service.StockPriceChartService;
import net.luversof.api.stock.web.dto.response.StockPriceChartPoint;
import net.luversof.api.stock.web.dto.response.StockPriceHistoryPoint;

/** 종목 */
@RestController
@RequestMapping("/api/stockItem")
public class StockItemController {

  @Autowired private StockPriceChartService stockPriceChartService;

  @Autowired private StockItemService stockItemService;

  /**
   * 종목 생성
   *
   * <p>심볼과 이름은 필수다.
   *
   * @param stockItem 만들 종목
   * @return 만든 종목
   */
  @PostMapping
  public StockItem createStockItem(@RequestBody StockItem stockItem) {
    return stockItemService.createStockItem(stockItem);
  }

  /**
   * 종목 조회
   *
   * @param id 종목 id
   * @return 종목, 없으면 빈 값
   */
  @GetMapping("/{id}")
  public Optional<StockItem> getStockItemById(@PathVariable UUID id) {
    return stockItemService.findById(id);
  }

  /**
   * 이름으로 종목 조회
   *
   * @param name 종목 이름
   * @return 종목, 없으면 null
   */
  @GetMapping("/search/findByName/{name}")
  public StockItem findByName(@PathVariable String name) {
    return stockItemService.findByName(name);
  }

  /**
   * 종목 목록 조회
   *
   * @return 종목 목록
   */
  @GetMapping("/search/findAll")
  public List<StockItem> findAll() {
    return stockItemService.findAll();
  }

  /**
   * 종목 일별 종가 조회
   *
   * <p>차트용. 기간을 주지 않으면 전 구간.
   *
   * <p>거래량 0 인 날은 빠진다 - 그 행의 종가 자리에는 직전 종가가 들어 있어 거래가 없던 날에 선이 이어진다.
   */
  @GetMapping("/{id}/priceHistory")
  public List<StockPriceHistoryPoint> priceHistory(
      @PathVariable UUID id,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    return stockItemService.findDailyClosePrices(id, startDate, endDate);
  }

  /**
   * 종목 주가 차트 조회
   *
   * <p>날짜마다 시가 · 고가 · 저가 · 종가(원주가를 분할로만 맞춘 값)와, 사용자를 주면 그 날의 평균 단가. 캔들 차트와 "그 시점 평단" 선이
   * 쓴다(StockPriceChartService, 2026-10-02).
   */
  @GetMapping("/{id}/priceChart")
  public List<StockPriceChartPoint> priceChart(
      @PathVariable UUID id,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @org.springframework.web.bind.annotation.RequestParam(required = false) UUID userId,
      @org.springframework.web.bind.annotation.RequestParam(required = false) String timeZone) {
    return stockPriceChartService.chart(id, startDate, endDate, userId, timeZone);
  }

  /**
   * 태그로 종목 목록 조회
   *
   * <p>예: 월배당 · 커버드콜 · analysis.
   *
   * @param tag 태그
   * @return 종목 목록
   */
  @GetMapping("/search/findAllByTag/{tag}")
  public List<StockItem> findAllByTag(@PathVariable String tag) {
    return stockItemService.findAllByTag(tag);
  }
}
