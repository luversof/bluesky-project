package net.luversof.api.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.bookkeeping.constant.EntryType;

/**
 * 즐겨찾기: 이름 붙인 내역 틀. 입력 모달을 채우는 용도라 종류 · 이름 말고는 비워 둘 수 있다.
 *
 * <p>가리키는 자산 · 분류가 나중에 지워지면 그 값은 화면이 무시한다(외래 키 없음).
 */
@Table("BookFavorite")
public record BookFavorite(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    String name,
    EntryType type,
    BigDecimal amount,
    @Column("toAmount") BigDecimal toAmount,
    @Column("asset_id") UUID assetId,
    @Column("toAsset_id") UUID toAssetId,
    @Column("category_id") UUID categoryId,
    String content,
    String memo,
    @Column("displayOrder") int displayOrder,
    @Column("createdBy") UUID createdBy,
    @Column("createdDate") Instant createdDate) {}
