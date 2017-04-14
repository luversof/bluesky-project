package net.luversof.bookkeeping.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Bookkeeping;

@Transactional(readOnly = true)
public interface BookkeepingRepository extends JpaRepository<Bookkeeping, Long>{
	
	List<Bookkeeping> findByUserId(String userId);
}
