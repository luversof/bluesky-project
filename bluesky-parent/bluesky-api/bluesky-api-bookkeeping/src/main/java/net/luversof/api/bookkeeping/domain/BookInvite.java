package net.luversof.api.bookkeeping.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 1회용 초대 링크. expiresAt 이 지났거나 usedBy 가 차 있으면 못 쓴다. */
@Table("BookInvite")
public record BookInvite(
    @Id String token,
    @Column("book_id") UUID bookId,
    @Column("createdBy") UUID createdBy,
    @Column("createdDate") Instant createdDate,
    @Column("expiresAt") Instant expiresAt,
    @Column("usedBy") UUID usedBy,
    @Column("usedDate") Instant usedDate) {}
