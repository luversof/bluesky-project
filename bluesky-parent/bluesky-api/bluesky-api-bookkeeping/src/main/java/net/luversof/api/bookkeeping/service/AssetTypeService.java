package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.repository.AssetTypeRepository;

@Service
public class AssetTypeService {

	@Autowired
	private BookkeepingService bookkeepingService;

	@Autowired
	private AssetTypeRepository assetTypeRepository;

	public void setBookkeepingService(BookkeepingService bookkeepingService) {
		this.bookkeepingService = bookkeepingService;
	}

	public void setAssetTypeRepository(AssetTypeRepository assetTypeRepository) {
		this.assetTypeRepository = assetTypeRepository;
	}

	public AssetType createAssetType(AssetType assetType) {
		checkAssetType(assetType);
		return assetTypeRepository.save(assetType);
	}

	public Optional<AssetType> findById(UUID id) {
		return assetTypeRepository.findById(id);
	}

	public List<AssetType> findByBookkeepingId(UUID bookkeepingId) {
		return assetTypeRepository.findByBookkeepingId(bookkeepingId);
	}

	public List<AssetType> findByBookkeepingIdAndCode(UUID bookkeepingId, AssetTypeCode code) {
		return assetTypeRepository.findByBookkeepingIdAndCode(bookkeepingId, code);
	}

	public AssetType updateAssetType(AssetType assetType) {
		checkAssetType(assetType);

		if (assetType.getCode() == null) {
			BookkeepingErrorCode.NOT_EXIST_ASSETTYPECODE.throwException();
		}

		var targetAssetType = assetTypeRepository.findById(assetType.getId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSETTYPE::exception);
		if (!targetAssetType.getBookkeepingId().equals(assetType.getBookkeepingId())) {
			BookkeepingErrorCode.INVALID_BOOKKEEPING_ID.throwException();
		}

		targetAssetType.setCode(assetType.getCode());
		targetAssetType.setName(assetType.getName());

		return assetTypeRepository.save(targetAssetType);
	}

	public void deleteAssetType(AssetType assetType) {
		checkAssetType(assetType);

		var targetAssetType = assetTypeRepository.findById(assetType.getId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSETTYPE::exception);
		if (!targetAssetType.getBookkeepingId().equals(assetType.getBookkeepingId())) {
			BookkeepingErrorCode.INVALID_BOOKKEEPING_ID.throwException();
		}
		// 대상 assetType을 사용하는 asset이 있는지 확인

		assetTypeRepository.delete(targetAssetType);
	}

	private void checkAssetType(AssetType assetType) {
		bookkeepingService.findById(assetType.getBookkeepingId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING_ID::exception);
	}
}
