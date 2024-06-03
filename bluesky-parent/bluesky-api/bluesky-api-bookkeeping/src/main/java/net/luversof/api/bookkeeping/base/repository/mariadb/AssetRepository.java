package net.luversof.api.bookkeeping.base.repository.mariadb;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.Asset;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

	List<Asset> findByLedgerId(UUID ledgerId);

}
