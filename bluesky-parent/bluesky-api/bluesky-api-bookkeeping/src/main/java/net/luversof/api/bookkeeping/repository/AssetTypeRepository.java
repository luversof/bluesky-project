package net.luversof.api.bookkeeping.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.domain.AssetType;

public interface AssetTypeRepository extends CrudRepository<AssetType, UUID> {

  List<AssetType> findByBookkeepingId(UUID bookkeepingId);

  List<AssetType> findByBookkeepingIdAndCode(UUID bookkeepingId, AssetTypeCode code);

  long deleteByBookkeepingId(UUID bookkeepingId);
}
