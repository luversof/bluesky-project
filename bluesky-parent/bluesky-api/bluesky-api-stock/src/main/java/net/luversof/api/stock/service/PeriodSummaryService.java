package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.PeriodDividendSummary;
import net.luversof.api.stock.domain.PeriodTradeSummary;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.PeriodSummary;

/** 기간 매매·배당 합계. 두 집계 쿼리를 한 줄로 합친다. */
@Service
public class PeriodSummaryService {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private DividendRepository dividendRepository;

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  public void setDividendRepository(DividendRepository dividendRepository) {
    this.dividendRepository = dividendRepository;
  }

  private static BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  public PeriodSummary findPeriodSummary(UUID userId, Instant startDate, Instant endDate) {
    PeriodTradeSummary trade = tradeRepository.findPeriodSummary(userId, startDate, endDate);
    PeriodDividendSummary dividend =
        dividendRepository.findPeriodSummary(userId, startDate, endDate);

    // 기간에 아무것도 없으면 쿼리가 null 을 돌려줄 수 있다(집계 함수만 있는 SELECT 라 행은 오지만, 매핑이
    // 실패하면 null 이다). 화면이 "0" 과 "못 가져왔다" 를 구분할 필요는 없으므로 0 으로 채운다.
    BigDecimal dividendGross = dividend != null ? safe(dividend.grossAmount()) : BigDecimal.ZERO;
    BigDecimal dividendTax = dividend != null ? safe(dividend.tax()) : BigDecimal.ZERO;
    BigDecimal dividendFee = dividend != null ? safe(dividend.fee()) : BigDecimal.ZERO;

    return new PeriodSummary(
        trade != null ? safe(trade.buyAmount()) : BigDecimal.ZERO,
        trade != null ? safe(trade.sellAmount()) : BigDecimal.ZERO,
        trade != null ? safe(trade.fee()) : BigDecimal.ZERO,
        trade != null ? safe(trade.tax()) : BigDecimal.ZERO,
        trade != null ? safe(trade.realizedProfit()) : BigDecimal.ZERO,
        trade != null ? trade.buyCount() : 0L,
        trade != null ? trade.sellCount() : 0L,
        dividendGross,
        dividend != null ? safe(dividend.taxableAmount()) : BigDecimal.ZERO,
        dividendTax,
        dividendFee,
        // 실수령 = 세전 - 세금 - 수수료. 배당 화면의 "세후" 와 같은 정의다.
        dividendGross.subtract(dividendTax).subtract(dividendFee),
        dividend != null ? dividend.count() : 0L);
  }
}
