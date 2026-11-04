package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 월 예산(기준통화, 매달 같은 금액). categoryId 가 null 이면 전체 지출, 있으면 지출 대분류 하나. */
@Table("Budget")
public record Budget(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    @Column("category_id") UUID categoryId,
    BigDecimal amount) {}
