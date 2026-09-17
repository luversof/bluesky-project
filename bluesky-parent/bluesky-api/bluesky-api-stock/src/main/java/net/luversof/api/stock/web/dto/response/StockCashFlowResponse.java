package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 한 종목의 하루치 순현금흐름(요청 존의 날짜). 들어온 돈은 양수, 나간 돈은 음수. */
public record StockCashFlowResponse(LocalDate date, BigDecimal amount) {}
