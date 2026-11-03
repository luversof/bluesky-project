package net.luversof.api.bookkeeping.domain;

import java.time.Instant;
import java.util.UUID;

import net.luversof.api.bookkeeping.constant.BookRole;

/** 가계부 멤버. PK 가 (book_id, user_id) 라 data-jdbc 엔티티가 아니고 BookMemberRepository(JdbcClient)로 다룬다. */
public record BookMember(UUID bookId, UUID userId, BookRole role, Instant joinedDate) {}
