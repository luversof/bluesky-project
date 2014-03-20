package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.Entry;

import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface EntryRepository extends JpaRepository<Entry, Long>, QueryDslPredicateExecutor<Entry> {
	List<Entry> findByAssetUserId(int id);
	Page<Entry> findByAssetUserId(int id, Pageable pageable);
	List<Entry> findByAssetUserIdAndDateBetween(int id, DateTime startDate, DateTime endDate);
}
