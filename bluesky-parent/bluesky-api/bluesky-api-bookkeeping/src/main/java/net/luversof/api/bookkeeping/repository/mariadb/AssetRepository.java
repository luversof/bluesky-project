package net.luversof.api.bookkeeping.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Asset;

public interface AssetRepository extends CrudRepository<Asset, UUID> {

	List<Asset> findByBookkeepingId(UUID bookkeepingId);

	long deleteByBookkeepingId(UUID bookkeepingId);
}
