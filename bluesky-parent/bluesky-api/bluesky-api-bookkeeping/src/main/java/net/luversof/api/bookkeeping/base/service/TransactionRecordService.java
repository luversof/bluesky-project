package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.TransactionRecord;
import net.luversof.api.bookkeeping.base.repository.mariadb.TransactionRecordRepository;

@RequiredArgsConstructor
@Service
public class TransactionRecordService  extends AbstractBaseService<TransactionRecord, UUID> {


	@Getter
	private final TransactionRecordRepository repository;

	public List<TransactionRecord> findByAssetId(UUID assetId) {
		return repository.findByAssetId(assetId);
	}
}
