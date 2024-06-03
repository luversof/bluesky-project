package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.AssetType;
import net.luversof.api.bookkeeping.base.repository.mariadb.AssetTypeRepository;

@RequiredArgsConstructor
@Service
public class AssetTypeService extends AbstractBaseService<AssetType, UUID> {

	@Getter
	private final AssetTypeRepository repository;
	
	public List<AssetType> findByLedgerId(UUID ledgerId) {
		return repository.findByLedgerId(ledgerId);
	}

}
