package net.luversof.api.bookkeeping.base.repository.postgresql;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import net.luversof.api.bookkeeping.base.domain.Asset;

public interface AssetPostgresqlRepository extends JpaRepository<Asset, UUID> {

	List<Asset> findByLedgerId(UUID ledgerId);

}
