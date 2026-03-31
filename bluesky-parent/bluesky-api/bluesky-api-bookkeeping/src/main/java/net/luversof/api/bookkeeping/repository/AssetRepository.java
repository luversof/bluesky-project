package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.domain.Asset;

public interface AssetRepository extends CrudRepository<Asset, UUID> {

    List<Asset> findByBookkeepingId(UUID bookkeepingId);

    List<Asset> findByAssetTypeId(UUID assetTypeId);

    long deleteByBookkeepingId(UUID bookkeepingId);
}
