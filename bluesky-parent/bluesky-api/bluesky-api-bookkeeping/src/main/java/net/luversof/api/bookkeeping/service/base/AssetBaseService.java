package net.luversof.api.bookkeeping.service.base;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;

@Service
public class AssetBaseService implements BaseService<Asset, UUID> {

	@Getter
	@Setter(onMethod_ = @Autowired)
	private AssetRepository repository;
	
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}
	
}
