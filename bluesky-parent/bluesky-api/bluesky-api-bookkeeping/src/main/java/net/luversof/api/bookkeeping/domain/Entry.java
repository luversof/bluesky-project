package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.bookkeeping.constant.EntryType;

/**
 * 내역(단식).
 *
 * <ul>
 *   <li>INCOME · EXPENSE: assetId, categoryId 필수. toAssetId · toAmount 는 null.
 *   <li>TRANSFER: assetId(나감) → toAssetId(들어옴), toAmount 필수(같은 통화면 = amount). categoryId 는 null.
 * </ul>
 */
@Table("Entry")
public record Entry(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    EntryType type,
    @Column("entryDate") LocalDate entryDate,
    @Column("entryTime") LocalTime entryTime,
    BigDecimal amount,
    @Column("toAmount") BigDecimal toAmount,
    @Column("asset_id") UUID assetId,
    @Column("toAsset_id") UUID toAssetId,
    @Column("category_id") UUID categoryId,
    String content,
    String memo,
    @Column("createdBy") UUID createdBy,
    @Column("createdDate") Instant createdDate) {}
