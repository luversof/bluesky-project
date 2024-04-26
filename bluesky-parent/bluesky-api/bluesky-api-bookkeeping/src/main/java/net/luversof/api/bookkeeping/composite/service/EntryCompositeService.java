package net.luversof.api.bookkeeping.composite.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Entry;
import net.luversof.api.bookkeeping.base.domain.EntryTransaction;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;
import net.luversof.api.bookkeeping.base.service.AccountTypeBaseService;
import net.luversof.api.bookkeeping.base.service.BookkeepingBaseService;
import net.luversof.api.bookkeeping.base.service.EntryBaseService;
import net.luversof.api.bookkeeping.base.service.EntryTransactionBaseService;
import net.luversof.api.bookkeeping.base.service.EntryTransactionTypeBaseService;

@RequiredArgsConstructor
@Service
public class EntryCompositeService {
	
	private final EntryBaseService entryBaseService;
	
	private final EntryTransactionBaseService entryTransactionBaseService;

	@Transactional
	public List<Entry> save(EntryTransaction entryTransaction, List<Entry> entryList) {
//		var entryList = new ArrayList<Entry>();
		
		// 총합이 0이 되는지 확인
		var debitSum = entryList.stream().filter(entry -> entry.getDebit() != null).map(Entry::getDebit).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		var creditSum = entryList.stream().filter(entry -> entry.getCredit() != null).map(Entry::getCredit).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
		if (!debitSum.equals(creditSum)) {
			throw new BlueskyException("NOT_EQUALS_BETWEEN_DEBIT_AND_CREDIT");
		}
		
		// entryTransaction 생성
		entryTransactionBaseService.save(entryTransaction);
		
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
		
		entryList.forEach(entry -> entry.setEntryTransactionId(entryTransaction.getId()));
		
		// 저장
		entryBaseService.saveAll(entryList);
		
		return entryList;
	}
	
}
