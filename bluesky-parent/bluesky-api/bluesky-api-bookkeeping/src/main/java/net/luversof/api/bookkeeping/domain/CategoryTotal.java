package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import net.luversof.api.bookkeeping.constant.CategoryKind;

/** 분류(내역이 가리킨 분류 그대로, 대분류로 묶는 건 화면이 parentId 로) · 통화별 합. */
public record CategoryTotal(
    UUID categoryId, CategoryKind kind, String currency, BigDecimal amount) {}
