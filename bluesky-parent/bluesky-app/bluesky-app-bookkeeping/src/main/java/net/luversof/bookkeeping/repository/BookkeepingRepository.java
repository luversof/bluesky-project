package net.luversof.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Bookkeeping;

@Transactional(readOnly = true)
public interface BookkeepingRepository extends JpaRepository<Bookkeeping, UUID>, RevisionRepository<Bookkeeping, UUID, Integer> {
	
	List<Bookkeeping> findByUserId(@Param("userId") UUID userId);
	
}
