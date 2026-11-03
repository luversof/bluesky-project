package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.bookkeeping.constant.AssetKind;

/** 자산. 잔액은 저장하지 않는다(openingBalance + 내역 합). 금액은 이 자산의 currency 를 따른다. */
@Table("Asset")
public record Asset(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    AssetKind kind,
    String name,
    String currency,
    @Column("openingBalance") BigDecimal openingBalance,
    /* 카드 결제일(1~31). 카드가 아니면 null. */
    @Column("paymentDay") Integer paymentDay,
    @Column("displayOrder") int displayOrder,
    boolean hidden) {}
