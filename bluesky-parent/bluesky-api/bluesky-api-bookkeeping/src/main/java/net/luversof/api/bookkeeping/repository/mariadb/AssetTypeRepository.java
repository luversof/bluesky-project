package net.luversof.api.bookkeeping.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.domain.AssetType;

public interface AssetTypeRepository extends JpaRepository<AssetType, UUID> {

	List<AssetType> findByBookkeepingId(UUID bookkeepingId);
	
	long deleteByBookkeepingId(UUID bookkeepingId);

}
