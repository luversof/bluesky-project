package net.luversof.api.bookkeeping.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.AssetType;

public interface AssetTypeRepository extends CrudRepository<AssetType, UUID> {

	List<AssetType> findByBookkeepingId(UUID bookkeepingId);
	
	long deleteByBookkeepingId(UUID bookkeepingId);

}
