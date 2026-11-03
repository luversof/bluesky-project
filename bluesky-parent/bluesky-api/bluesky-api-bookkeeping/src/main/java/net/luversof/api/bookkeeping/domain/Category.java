package net.luversof.api.bookkeeping.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import net.luversof.api.bookkeeping.constant.CategoryKind;

/** 분류. parentId 가 null 이면 대분류, 있으면 소분류(2단계 고정). */
@Table("Category")
public record Category(
    @Id UUID id,
    @Column("book_id") UUID bookId,
    @Column("parent_id") UUID parentId,
    CategoryKind kind,
    String name,
    @Column("displayOrder") int displayOrder,
    boolean hidden) {}
