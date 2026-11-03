package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import net.luversof.api.bookkeeping.constant.EntryType;

/** 내역 추가 · 수정 입력. 같은 통화 이체는 toAmount 를 비워도 된다(amount 로 채움). */
public record EntryCommand(
    EntryType type,
    LocalDate entryDate,
    LocalTime entryTime,
    BigDecimal amount,
    BigDecimal toAmount,
    UUID assetId,
    UUID toAssetId,
    UUID categoryId,
    String content,
    String memo) {}
