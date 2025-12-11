package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.bookkeeping.constant.AssetJsonConfigConstant;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.repository.AssetRepository;
import net.luversof.api.bookkeeping.repository.EntryRepository;

@Service
public class AssetService {

	@Autowired
	private BookkeepingService bookkeepingService;

	@Autowired
	private AssetRepository assetRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private AssetTypeService assetTypeService;

	public void setBookkeepingService(BookkeepingService bookkeepingService) {
		this.bookkeepingService = bookkeepingService;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public void setEntryRepository(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	public void setAssetTypeService(AssetTypeService assetTypeService) {
		this.assetTypeService = assetTypeService;
	}

	public Asset createAsset(Asset asset) {

		checkAsset(asset);

		// 사용자가 추가한 asset은 custom 설정으로 초기화
		asset.setJsonConfig(AssetJsonConfigConstant.getCustomConfigList());

		return assetRepository.save(asset);
	}

	public Optional<Asset> findById(UUID id) {
		return assetRepository.findById(id);
	}

	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetRepository.findByBookkeepingId(bookkeepingId);
	}

	public Asset updateAsset(Asset asset) {

		checkAsset(asset);

		var targetAsset = assetRepository.findById(asset.getId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSET::exception);
		if (!targetAsset.getBookkeepingId().equals(asset.getBookkeepingId())) {
			BookkeepingErrorCode.INVALID_BOOKKEEPING_ID.throwException();
		}

		var enableUpdate = targetAsset.getJsonConfig().getOrDefault(AssetJsonConfigConstant.ENABLE_UPDATE,
				Boolean.FALSE);
		if (!enableUpdate.equals(Boolean.TRUE)) {
			BookkeepingErrorCode.UNABLE_UPDATE_ASSET.throwException();
		}

		// update 가능한 값들에 대해서만 처리
		targetAsset.setAssetTypeId(asset.getAssetTypeId());
		targetAsset.setName(asset.getName());

		return assetRepository.save(targetAsset);
	}

	/**
	 * asset id를 기준으로 삭제 처리
	 * bookkeeping.id, bookkeeping.userId를 전달받아 체크해야 할듯?
	 * 
	 * @param asset
	 */
	public void deleteAsset(Asset asset) {

		checkAsset(asset);

		// 삭제 대상 확인
		var targetAsset = assetRepository.findById(asset.getId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSET::exception);

		// 대상 asset의 entry가 있는지 확인
		boolean isEnableDelete = entryRepository.findByIncomeAssetId(asset.getId()).isEmpty()
				&& entryRepository.findByOutgoingAssetId(asset.getId()).isEmpty();

		// entry 가 있으면 삭제 불가
		if (!isEnableDelete) {
			BookkeepingErrorCode.UNABLE_DELETE_ASSET.throwException();
		}
		assetRepository.delete(targetAsset);
	}

	/**
	 * Create와 Update에서 공통으로 확인하는 항목
	 * 1. asset의 bookkeepingId 존재 체크
	 * 2. assetTypeId가 있으면 해당 assetType의 bookkeepingId가 asset의 bookkeepingId와 같은지 체크
	 */
	private void checkAsset(Asset asset) {
		var targetBookkeeping = bookkeepingService.findById(asset.getBookkeepingId())
				.orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING_ID::exception);

		if (asset.getAssetTypeId() != null) {
			var targetAssetType = assetTypeService.findById(asset.getAssetTypeId())
					.orElseThrow(BookkeepingErrorCode.INVALID_ASSETTYPE_ID::exception);
			if (!targetAssetType.getBookkeepingId().equals(targetBookkeeping.getId())) {
				BookkeepingErrorCode.INVALID_ASSETTYPE_ID.throwException();
			}
		}

	}

}
