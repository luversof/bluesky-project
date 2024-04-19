//package net.luversof.api.bookkeeping.service;
//
//import java.util.List;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.constant.AssetGroupInitialData;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//import net.luversof.api.bookkeeping.domain.AssetGroup;
//import net.luversof.api.bookkeeping.repository.AssetGroupRepository;
//
//@Service
//public class BasicAssetGroupService implements AssetGroupService {
//	
//	@Autowired
//	private AssetGroupRepository assetGroupRepository;
//	
//	/**
//	 * 초기 데이터 insert
//	 * @param bookkeeping
//	 * @return
//	 */
//	// @Transactional(BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER)
//	public List<AssetGroup> createInitialData(UUID bookkeepingId) {
//		return assetGroupRepository.saveAll(AssetGroupInitialData.createAssetGroupList(bookkeepingId));
//	}
//	
//	@Override
//	public AssetGroup create(AssetGroup assetGroup) {
//		// bookkeepingId가 존재하는 것인지 체크를 하려면?
//		return assetGroupRepository.save(assetGroup);
//	}
//	
//	@Override
//	public List<AssetGroup> findByBookkeepingId(UUID bookkeepingId) {
//		return assetGroupRepository.findByBookkeepingId(bookkeepingId);
//	}
//	
//	@Override
//	public AssetGroup update(AssetGroup assetGroup) {
//		AssetGroup targetAssetGroup = assetGroupRepository.findById(assetGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
//		targetAssetGroup.setName(assetGroup.getName());
//		return assetGroupRepository.save(targetAssetGroup);
//	}
//	
//	@Override
//	public void delete(AssetGroup assetGroup) {
//		AssetGroup targetAssetGroup = assetGroupRepository.findById(assetGroup.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_ASSETGROUP));
//		assetGroupRepository.delete(targetAssetGroup);
//	}
//	
//	@Override
//	public void deleteAllByBookkeepingId(UUID bookkeepingId) {
//		List<AssetGroup> assetGroupList = assetGroupRepository.findByBookkeepingId(bookkeepingId);
//		assetGroupRepository.deleteAll(assetGroupList);
//	}
//}
