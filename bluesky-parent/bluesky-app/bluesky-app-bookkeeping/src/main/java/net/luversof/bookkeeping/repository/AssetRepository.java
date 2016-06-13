package net.luversof.bookkeeping.repository;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

import java.util.List;

import net.luversof.bookkeeping.domain.Asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
public interface AssetRepository extends JpaRepository<Asset, Long> {
	
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	List<Asset> findByBookkeepingId(long bookkeepingId);
}
