package net.luversof.api.bookkeeping.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 가계부. 소유 · 공유는 {@link BookMember} 로만 표현한다.
 *
 * <p>id 는 서비스가 채운다. 새 행은 JdbcAggregateTemplate.insert, 수정은 repository.save 로 저장한다.
 */
@Table("Book")
public record Book(
    @Id UUID id,
    String name,
    @Column("baseCurrency") String baseCurrency,
    /* 1~28. 25 면 "11월" = 10/25 ~ 11/24(끝나는 달 이름). */
    @Column("monthStartDay") int monthStartDay,
    @Column("createdDate") Instant createdDate) {}
