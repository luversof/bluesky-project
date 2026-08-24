package net.luversof.api.stock.web.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.ActivityFilterIdsResponse;

/** 필터 목록용 id 집계. DISTINCT 4건만 수행하는 경량 엔드포인트다. */
@RestController
@RequestMapping("/api/activityFilterIds")
public class ActivityFilterIdsController {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private DividendRepository dividendRepository;

  @GetMapping
  public ActivityFilterIdsResponse findFilterIds(
      @RequestParam UUID userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate) {
    return new ActivityFilterIdsResponse(
        tradeRepository.findDistinctAccountIds(userId, startDate, endDate),
        tradeRepository.findDistinctStockItemIds(userId, startDate, endDate),
        dividendRepository.findDistinctAccountIds(userId, startDate, endDate),
        dividendRepository.findDistinctStockItemIds(userId, startDate, endDate));
  }
}
