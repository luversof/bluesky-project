package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;
import net.luversof.api.bookkeeping.constant.ErrorCode;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.base.AssetBaseService;
import net.luversof.api.bookkeeping.service.base.BookkeepingBaseService;
import net.luversof.api.bookkeeping.service.base.EntryBaseService;
import net.luversof.api.bookkeeping.service.base.EntryTypeBaseService;

@Service
public class EntryService {
	
	@Setter(onMethod_ = @Autowired)
	private BookkeepingBaseService bookkeepingBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private AssetBaseService assetBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryTypeBaseService entryTypeBaseService;
	
	@Setter(onMethod_ = @Autowired)
	private EntryBaseService entryBaseService;
	
	public Entry createEntry(Entry entry) {
		
		checkEntry(entry);
		
		return entryBaseService.save(entry);
	}
	
	public List<Entry> findByBookkeepingId(UUID bookkeepingId) {
		return entryBaseService.findByBookkeepingId(bookkeepingId);
	}
	
	public Entry updateEntry(Entry entry) {
		// 대상 entry를 먼저 조회하여 있는지 확인한 후 update 할 항목들을 반영
		var targetEntry = entryBaseService.findById(entry.getId()).orElseThrow(ErrorCode.NOT_EXIST_ENTRY::exception);
		
		// update 요청의 경우 해당 entry의 정보가 변조되었는지 확인해야 함.
		if (!targetEntry.getBookkeepingId().equals(entry.getBookkeepingId())) {
			ErrorCode.INVALID_BOOKKEEPING_ID.throwException();
		}
		
		// 변경 가능 항목들을 모두 설정
		// 이런 방법 말고 더 좋은 방법은 없나?
		// 그냥 entry를 업데이트 하면 어떻게 되나 봐야겠네
//		targetEntry.setBookkeepingId(entry.getBookkeepingId());
//		targetEntry.setEntryType(entry.getEntryType());
//		targetEntry.setEntryDate(entry.getEntryDate());
//		targetEntry.setIncomeAssetId(entry.getIncomeAssetId());
//		targetEntry.setOutgoingAssetId(entry.getOutgoingAssetId());
//		targetEntry.setAmount(entry.getAmount());
//		targetEntry.setExtraData(entry.getExtraData());
		
		checkEntry(entry);
		
		return entryBaseService.update(entry);
	}
	
	public void deleteEntry(Entry entry) {
		// 삭제 해도 되는지 확인
		entryBaseService.deleteById(entry.getId());
	}

	
	// 기간별 검색도 있어야 할지도?
	// 혹은 통계 검색은 별도로 구성
	
	/**
	 * 생성 / 수정 시 validation check
	 * @param entry
	 */
	private void checkEntry(Entry entry) {
		// 요청 값이 올바른지 확인
		// 1. bookkeeping이 올바른지 확인
		var bookkeeping = bookkeepingBaseService.findById(entry.getBookkeepingId()).orElseThrow(ErrorCode.NOT_EXIST_BOOKKEEPING::exception);
		
		// 2. incomeAsset 확인
		var incomeAsset = assetBaseService.findById(entry.getIncomeAssetId()).orElseThrow(ErrorCode.NOT_EXIST_ASSET::exception);
		if (!incomeAsset.getBookkeeping().equals(bookkeeping)) {
			ErrorCode.INVALID_ASSET_ID.throwException();
		}
		
		
		// 3. outgoingAsset 확인 
		var outgoingAsset = assetBaseService.findById(entry.getOutgoingAssetId()).orElseThrow(ErrorCode.NOT_EXIST_ASSET::exception);
		if (!outgoingAsset.getBookkeeping().equals(bookkeeping)) {
			ErrorCode.INVALID_ASSET_ID.throwException();
		}

		// 4. entryType 확인
		// 이체의 경우엔 entryType이 필요없음
		// 수입/지출의 경우엔 entryType이 있는지, 있으면 올바른 type인지 확인해야함
		boolean isTransfer = outgoingAsset.getAssetType().getCode() != AssetTypeCode.CONTRA_ASSET && incomeAsset.getAssetType().getCode() != AssetTypeCode.CONTRA_ASSET;
		if (!isTransfer	&& (entry.getEntryType() == null || entry.getEntryType().getId() == null)) {
			ErrorCode.NOT_EXIST_ENTRYTYPE.throwException();
		}


		if (!isTransfer) {
			
			var entryType = entryTypeBaseService.findById(entry.getEntryType().getId()).orElseThrow(ErrorCode.NOT_EXIST_ENTRYTYPE::exception);

			if (!entryType.getBookkeepingId().equals(bookkeeping.getId())) {
				ErrorCode.INVALID_ENTRYTYPE.throwException();
			}
			
			if (
				(outgoingAsset.getAssetType().getCode() == AssetTypeCode.CONTRA_ASSET && entryType.getCode() != EntryTypeCode.INCOME) // 수입인 경우 entryType이 수입 관련 코드인지 확인
				|| (incomeAsset.getAssetType().getCode() == AssetTypeCode.CONTRA_ASSET && entryType.getCode() != EntryTypeCode.OUTGOING) // 지출인 경우 entryType이 지출 관련 코드인지 확인
			) {
				ErrorCode.INVALID_ENTRYTYPECODE.throwException();
			}
		}
	}

}
