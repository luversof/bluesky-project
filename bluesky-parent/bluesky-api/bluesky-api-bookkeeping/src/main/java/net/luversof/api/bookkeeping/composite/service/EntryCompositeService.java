package net.luversof.api.bookkeeping.composite.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Entry;
import net.luversof.api.bookkeeping.base.domain.EntryTransaction;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;
import net.luversof.api.bookkeeping.base.service.AccountTypeBaseService;
import net.luversof.api.bookkeeping.base.service.BookkeepingBaseService;
import net.luversof.api.bookkeeping.base.service.EntryBaseService;
import net.luversof.api.bookkeeping.base.service.EntryTransactionBaseService;
import net.luversof.api.bookkeeping.base.service.EntryTypeBaseService;

@RequiredArgsConstructor
@Service
public class EntryCompositeService {
	
	private final EntryBaseService entryBaseService;
	
	private final EntryTransactionBaseService entryTransactionBaseService;

	/**
	 * 일단 debit, credit 하나씩인 경우로 가정
	 */
	@Transactional
	public List<Entry> save() {
		var entryList = new ArrayList<Entry>();
		
		var entryTransaction = new EntryTransaction();
		entryTransactionBaseService.save(entryTransaction);
		
		//credit 대상 entry 생성
		var creditEntry = new Entry();
		creditEntry.setEntryTransactionId(entryTransaction.getId());
		
		entryList.add(creditEntry);
		
		//debit 대상 entry 생성
		var debitEntry = new Entry();
		debitEntry.setEntryTransactionId(entryTransaction.getId());
		
		entryList.add(debitEntry);
		
		entryBaseService.saveAll(entryList);
		
		return entryList;
	}
	
}
