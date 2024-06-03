package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.AssetType;

public interface AssetTypeRepository extends JpaRepository<AssetType, UUID> {

	List<AssetType> findByLedgerId(UUID ledgerId);

}
