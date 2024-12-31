package net.luversof.api.bookkeeping.service.base;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.repository.mariadb.AssetTypeRepository;

@Service
public class AssetTypeBaseService implements BaseService<AssetType, UUID> {

	@Getter
	@Setter(onMethod_ = @Autowired)
	private AssetTypeRepository repository;
	
	public List<AssetType> findByBookkeepingId(UUID bookkeepingId) {
		return repository.findByBookkeepingId(bookkeepingId);
	}

}
