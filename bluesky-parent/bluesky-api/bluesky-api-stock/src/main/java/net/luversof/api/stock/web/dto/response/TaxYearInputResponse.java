package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;

/** 귀속연도 세금 입력값. 저장된 적이 없으면 두 값이 null 이다. */
public record TaxYearInputResponse(
    int year, BigDecimal otherIncomeTaxBase, BigDecimal otherFinancialIncome) {}
