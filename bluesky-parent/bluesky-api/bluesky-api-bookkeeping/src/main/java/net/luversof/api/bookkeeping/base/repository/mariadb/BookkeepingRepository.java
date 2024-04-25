package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.bookkeeping.base.domain.Bookkeeping;

@Transactional(readOnly = true)
public interface BookkeepingRepository extends JpaRepository<Bookkeeping, UUID> {
	
	List<Bookkeeping> findByUserId(UUID userId);

}
