package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.UUID;
import net.luversof.api.bookkeeping.constant.AssetTypeCode;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryTypeCode;
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.repository.AssetTypeRepository;
import net.luversof.api.bookkeeping.repository.EntryRepository;
import net.luversof.api.bookkeeping.repository.EntryTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntryService {

    @Autowired private BookkeepingService bookkeepingService;

    @Autowired private AssetService assetService;

    @Autowired private AssetTypeRepository assetTypeRepository;

    @Autowired private EntryTypeRepository entryTypeRepository;

    @Autowired private EntryRepository entryRepository;

    public void setBookkeepingService(BookkeepingService bookkeepingService) {
        this.bookkeepingService = bookkeepingService;
    }

    public void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    public void setAssetTypeRepository(AssetTypeRepository assetTypeRepository) {
        this.assetTypeRepository = assetTypeRepository;
    }

    public void setEntryTypeRepository(EntryTypeRepository entryTypeRepository) {
        this.entryTypeRepository = entryTypeRepository;
    }

    public void setEntryRepository(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public Entry createEntry(Entry entry) {

        checkEntry(entry);

        return entryRepository.save(entry);
    }

    public List<Entry> findByBookkeepingId(UUID bookkeepingId) {
        return entryRepository.findByBookkeepingId(bookkeepingId);
    }

    public Entry updateEntry(Entry entry) {
        // 대상 entry를 먼저 조회하여 있는지 확인한 후 update 할 항목들을 반영
        var targetEntry =
                entryRepository
                        .findById(entry.getId())
                        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ENTRY::exception);

        // update 요청의 경우 해당 entry의 정보가 변조되었는지 확인해야 함.
        if (!targetEntry.getBookkeepingId().equals(entry.getBookkeepingId())) {
            BookkeepingErrorCode.INVALID_BOOKKEEPING_ID.throwException();
        }

        // 변경 가능 항목들을 모두 설정
        // 이런 방법 말고 더 좋은 방법은 없나?
        // 그냥 entry를 업데이트 하면 어떻게 되나 봐야겠네
        // targetEntry.setBookkeepingId(entry.getBookkeepingId());
        // targetEntry.setEntryType(entry.getEntryType());
        // targetEntry.setEntryDate(entry.getEntryDate());
        // targetEntry.setIncomeAssetId(entry.getIncomeAssetId());
        // targetEntry.setOutgoingAssetId(entry.getOutgoingAssetId());
        // targetEntry.setAmount(entry.getAmount());
        // targetEntry.setExtraData(entry.getExtraData());

        checkEntry(entry);

        return entryRepository.save(entry);
    }

    public void deleteEntry(Entry entry) {
        // 삭제 해도 되는지 확인
        entryRepository.deleteById(entry.getId());
    }

    // 기간별 검색도 있어야 할지도?
    // 혹은 통계 검색은 별도로 구성

    /**
     * 생성 / 수정 시 validation check
     *
     * @param entry
     */
    private void checkEntry(Entry entry) {
        // 요청 값이 올바른지 확인
        // 1. bookkeeping이 올바른지 확인
        var bookkeeping =
                bookkeepingService
                        .findById(entry.getBookkeepingId())
                        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING::exception);

        // 2. incomeAsset 확인
        var incomeAsset =
                assetService
                        .findById(entry.getIncomeAssetId())
                        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSET::exception);
        if (!incomeAsset.getBookkeepingId().equals(bookkeeping)) {
            BookkeepingErrorCode.INVALID_ASSET_ID.throwException();
        }

        // 3. outgoingAsset 확인
        var outgoingAsset =
                assetService
                        .findById(entry.getOutgoingAssetId())
                        .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ASSET::exception);
        if (!outgoingAsset.getBookkeepingId().equals(bookkeeping)) {
            BookkeepingErrorCode.INVALID_ASSET_ID.throwException();
        }

        // 4. entryType 확인
        // 이체의 경우엔 entryType이 필요없음
        // 수입/지출의 경우엔 entryType이 있는지, 있으면 올바른 type인지 확인해야함
        var outgoingAssetType = getAssetType(outgoingAsset.getAssetTypeId());
        var incomeAssetType = getAssetType(incomeAsset.getAssetTypeId());

        boolean isTransfer =
                outgoingAssetType.getCode() != AssetTypeCode.CONTRA_ASSET
                        && incomeAssetType.getCode() != AssetTypeCode.CONTRA_ASSET;
        if (!isTransfer && entry.getEntryTypeId() == null) {
            BookkeepingErrorCode.NOT_EXIST_ENTRYTYPE.throwException();
        }

        if (!isTransfer) {
            var entryType =
                    entryTypeRepository
                            .findById(entry.getEntryTypeId())
                            .orElseThrow(BookkeepingErrorCode.NOT_EXIST_ENTRYTYPE::exception);

            if (!entryType.getBookkeepingId().equals(bookkeeping.getId())) {
                BookkeepingErrorCode.INVALID_ENTRYTYPE.throwException();
            }

            if ((outgoingAssetType.getCode() == AssetTypeCode.CONTRA_ASSET
                            && entryType.getCode()
                                    != EntryTypeCode.INCOME) // 수입인 경우 entryType이 수입 관련 코드인지 확인
                    || (incomeAssetType.getCode() == AssetTypeCode.CONTRA_ASSET
                            && entryType.getCode()
                                    != EntryTypeCode.OUTGOING) // 지출인 경우 entryType이 지출 관련 코드인지 확인
            ) {
                BookkeepingErrorCode.INVALID_ENTRYTYPECODE.throwException();
            }
        }
    }

    private AssetType getAssetType(UUID id) {
        return assetTypeRepository.findById(id).orElseThrow();
    }
}
