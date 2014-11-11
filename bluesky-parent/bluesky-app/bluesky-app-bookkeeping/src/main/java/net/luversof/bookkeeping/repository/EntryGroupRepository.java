package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.EntryGroup;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryGroupRepository extends JpaRepository<EntryGroup, Long> {
	List<EntryGroup> findByBookkeepingId(long bookkeeping_id);
}