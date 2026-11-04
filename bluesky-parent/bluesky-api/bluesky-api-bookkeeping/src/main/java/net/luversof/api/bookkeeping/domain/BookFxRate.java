package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** 월 환율: 외화 1 단위 = 기준통화 rate. month 는 가계부 월 "2026-11". */
public record BookFxRate(UUID bookId, String month, String currency, BigDecimal rate) {}
