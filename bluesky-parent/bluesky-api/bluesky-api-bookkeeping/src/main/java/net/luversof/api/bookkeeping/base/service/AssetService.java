package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Asset;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetRepository;

@RequiredArgsConstructor
@Service
public class AssetService extends AbstractBaseService<Asset, UUID> {

	@Getter
	private final AssetRepository repository;
	
	public List<Asset> findByLedgerId(UUID ledgerId) {
		return repository.findByLedgerId(ledgerId);
	}
	
}
