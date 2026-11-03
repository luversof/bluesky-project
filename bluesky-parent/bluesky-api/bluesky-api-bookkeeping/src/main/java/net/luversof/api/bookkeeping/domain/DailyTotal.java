package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 하루 · 통화별 수입 · 지출 합(이체 제외). */
public record DailyTotal(LocalDate date, String currency, BigDecimal income, BigDecimal expense) {}
