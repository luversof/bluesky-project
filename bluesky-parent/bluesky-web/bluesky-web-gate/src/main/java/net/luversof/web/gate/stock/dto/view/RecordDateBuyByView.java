package net.luversof.web.gate.stock.dto.view;

import java.time.LocalDate;

/**
 * 지급 시기 하나의 다음 분배금 기준일과 매수 마감일(2026-10-20 자율 개선). 계산은 {@code KrxRecordDateUtil}.
 *
 * @param payoutWindow {@code MID_MONTH} · {@code MONTH_END}
 * @param recordDate 기준일(이날 주주로 올라 있어야 받는다)
 * @param buyByDate 매수 마감일(기준일의 2 거래일 전 - T+2 결제)
 * @param daysLeft 오늘부터 마감일까지 남은 날(0 = 오늘이 마감)
 * @param holidaysCovered 두 날짜의 해가 공휴일 표에 있는가 &mdash; 없으면 주말 · 휴장일만 뺀 값이다
 */
public record RecordDateBuyByView(
    String payoutWindow,
    LocalDate recordDate,
    LocalDate buyByDate,
    long daysLeft,
    boolean holidaysCovered) {}
