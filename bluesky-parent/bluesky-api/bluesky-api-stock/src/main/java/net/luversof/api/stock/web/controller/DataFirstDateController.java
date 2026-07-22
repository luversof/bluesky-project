package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.DataFirstDateResponse;

/** 사용자의 최초 데이터 일자 조회. 집계 2건만 수행하는 경량 엔드포인트로, 게이트가 날짜 선택기 하한을 구하려고 전체 이력을 내려받던 것을 대체한다. */
@RestController
@RequestMapping("/api/dataFirstDate")
public class DataFirstDateController {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private DividendRepository dividendRepository;

  @GetMapping
  public DataFirstDateResponse findDataFirstDate(@RequestParam UUID userId) {
    return new DataFirstDateResponse(
        tradeRepository.findFirstTradeDateByUserId(userId),
        dividendRepository.findFirstDividendDateByUserId(userId));
  }
}
