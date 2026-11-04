package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.constant.RecurringCycle;

/**
 * 반복 내역 틀. 자동으로 넣지 않는다 - 화면이 도래한 회차를 보여 주고 사용자가 넣는다({@code RecurringDone} 이 중복을 막는다).
 *
 * <p>내역 칼럼 규칙은 {@link Entry} 와 같다. 주기 칼럼은 cycle 에 맞는 것만 채운다.
 */
@Table("BookRecurring")
public record BookRecurring(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    EntryType type,
    BigDecimal amount,
    @Column("toAmount") BigDecimal toAmount,
    @Column("asset_id") UUID assetId,
    @Column("toAsset_id") UUID toAssetId,
    @Column("category_id") UUID categoryId,
    String content,
    String memo,
    RecurringCycle cycle,
    @Column("dayOfMonth") Integer dayOfMonth,
    @Column("dayOfWeek") Integer dayOfWeek,
    @Column("monthOfYear") Integer monthOfYear,
    @Column("startDate") LocalDate startDate,
    @Column("endDate") LocalDate endDate,
    @Column("createdBy") UUID createdBy,
    @Column("createdDate") Instant createdDate) {}
