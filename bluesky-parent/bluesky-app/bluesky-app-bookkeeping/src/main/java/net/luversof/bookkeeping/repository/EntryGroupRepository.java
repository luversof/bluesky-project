package net.luversof.bookkeeping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.EntryGroup;

@Transactional(readOnly = true)
public interface EntryGroupRepository extends JpaRepository<EntryGroup, Long> {

	List<EntryGroup> findByBookkeepingId(String bookkeepingId);
	
	Optional<EntryGroup> findByEntryGroupId(String EntryGroupId);

}