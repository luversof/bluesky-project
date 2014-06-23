package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.EntryGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

public interface EntryGroupRepository extends JpaRepository<EntryGroup, Long>, QueryDslPredicateExecutor<EntryGroup> {
	List<EntryGroup> findByUserId(int userId);
}