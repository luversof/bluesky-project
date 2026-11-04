package net.luversof.api.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import net.luversof.api.bookkeeping.constant.EntryType;

/**
 * 내역 추가 · 수정 입력. 같은 통화 이체는 toAmount 를 비워도 된다(amount 로 채움). installmentMonths 는 신용카드 지출의 할부 개월(2~36,
 * 비우면 일시불).
 */
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
    String memo,
    Integer installmentMonths) {

  /** 일시불. */
  public EntryCommand(
      EntryType type,
      LocalDate entryDate,
      LocalTime entryTime,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo) {
    this(
        type,
        entryDate,
        entryTime,
        amount,
        toAmount,
        assetId,
        toAssetId,
        categoryId,
        content,
        memo,
        null);
  }
}
