package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.Entry;

import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface EntryRepository extends JpaRepository<Entry, Long>, QueryDslPredicateExecutor<Entry> {
	List<Entry> findByAssetUsername(String username);
	Page<Entry> findByAssetUsername(String username, Pageable pageable);
	List<Entry> findByAssetUsernameAndDateBetween(String username, DateTime startDate, DateTime endDate);
}
