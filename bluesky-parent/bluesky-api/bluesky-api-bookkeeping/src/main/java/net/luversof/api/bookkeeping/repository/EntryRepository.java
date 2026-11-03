package net.luversof.api.bookkeeping.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Entry;

/** 기간 검색 · 잔액 · 통계 쿼리는 서비스 단계에서 JdbcClient 로 더한다. */
public interface EntryRepository extends CrudRepository<Entry, UUID> {

  long deleteByBookId(UUID bookId);
}
