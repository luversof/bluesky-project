package net.luversof.api.stock.web.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

/** 귀속연도 세금 입력값 저장. 두 값은 비울 수 있다(null = 모름). 보낸 값으로 통째로 덮는다. */
public record TaxYearInputUpsertRequest(
    UUID userId, Integer year, BigDecimal otherIncomeTaxBase, BigDecimal otherFinancialIncome) {}
