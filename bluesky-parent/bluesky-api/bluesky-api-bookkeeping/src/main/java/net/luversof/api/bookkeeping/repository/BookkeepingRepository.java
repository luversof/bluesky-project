package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.domain.Bookkeeping;

@Transactional(readOnly = true)
public interface BookkeepingRepository extends CrudRepository<Bookkeeping, UUID> {
	
	List<Bookkeeping> findByUserId(UUID userId);

}
