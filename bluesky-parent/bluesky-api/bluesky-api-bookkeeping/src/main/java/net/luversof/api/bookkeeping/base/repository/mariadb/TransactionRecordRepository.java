package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.TransactionRecord;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {
	
	List<TransactionRecord> findByAssetId(UUID assetId);

}
