package net.luversof.api.bookkeeping.composite.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.uuid.alt.GUID;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.TransactionRecord;
import net.luversof.api.bookkeeping.base.service.TransactionRecordService;

@RequiredArgsConstructor
@Service
public class TransactionRecordCompositeService {
	
	private final TransactionRecordService entryBaseService;
	
	@Transactional
	public List<TransactionRecord> save(List<TransactionRecord> entryList) {
//		var entryList = new ArrayList<Entry>();
		
		// 총합이 0이 되는지 확인
		var debitSum = entryList.stream().filter(entry -> entry.getDebit() != null).map(TransactionRecord::getDebit).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		var creditSum = entryList.stream().filter(entry -> entry.getCredit() != null).map(TransactionRecord::getCredit).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		if (!debitSum.equals(creditSum)) {
			throw new BlueskyException("NOT_EQUALS_BETWEEN_DEBIT_AND_CREDIT");
		}
		
		// entryTransaction 생성
		UUID transactionGroupId = GUID.v7().toUUID();
		
//		//credit 대상 entry 생성
//		var creditEntry = new Entry();
//		creditEntry.setEntryTransactionId(entryTransaction.getId());
//		
//		entryList.add(creditEntry);
//		
//		//debit 대상 entry 생성
//		var debitEntry = new Entry();
//		debitEntry.setEntryTransactionId(entryTransaction.getId());
//		
//		entryList.add(debitEntry);
		
		entryList.forEach(entry -> entry.setTransactionGroupId(transactionGroupId));
		
		// 저장
		entryBaseService.saveAll(entryList);
		
		return entryList;
	}
	
}
