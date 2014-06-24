package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.Bookkeeping;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookkeepingRepository extends JpaRepository<Bookkeeping, Long>{
	List<Bookkeeping> findByUserId(long userId);
}
