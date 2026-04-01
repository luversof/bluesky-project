package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;
import net.luversof.api.bookkeeping.domain.Asset;
import org.springframework.data.repository.CrudRepository;

public interface AssetRepository extends CrudRepository<Asset, UUID> {

    List<Asset> findByBookkeepingId(UUID bookkeepingId);

    List<Asset> findByAssetTypeId(UUID assetTypeId);

    long deleteByBookkeepingId(UUID bookkeepingId);
}
