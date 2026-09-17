package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 한 종목의 하루치 순현금흐름(api-stock {@code /api/trade/cashFlowsByStockItem} 의 같은 이름 레코드). 들어온 돈은 양수, 나간 돈은
 * 음수.
 *
 * <p>매수는 -(가격 x 수량 + 수수료 + 세금), 매도는 +(가격 x 수량 - 수수료 - 세금), 배당은 +(세전 - 세금 - 수수료)이고 같은 날은 더해져 온다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockCashFlowResponse(LocalDate date, BigDecimal amount) {}
