package net.luversof.api.bookkeeping.composite.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.constant.AssetInitialData;
import net.luversof.api.bookkeeping.base.constant.AssetTypeInitialData;
import net.luversof.api.bookkeeping.base.constant.TransactionTypeInitialData;
import net.luversof.api.bookkeeping.base.domain.Ledger;
import net.luversof.api.bookkeeping.base.service.AssetService;
import net.luversof.api.bookkeeping.base.service.AssetTypeService;
import net.luversof.api.bookkeeping.base.service.LedgerService;
import net.luversof.api.bookkeeping.base.service.TransactionTypeService;

@RequiredArgsConstructor
@Service
public class BookkeepingCompositeService {
	
	private final LedgerService bookkeepingBaseService;
	private final AssetTypeService accountTypeBaseService;
	private final AssetService accountBaseService;
	private final TransactionTypeService entryTransactionTypeBaseService;
	
	/**
	 * 가계부 생성
	 * 1. bookkeeping 생성
	 * 2. 기본적인 데이터 (Bookkeeping, Account, AccountType, EntryTransactionType) 생성
	 */
	@Transactional
	public void create(Ledger bookkeeping) {
		
		bookkeepingBaseService.save(bookkeeping);
		
		var accountTypeList = AssetTypeInitialData.getInitialData(bookkeeping.getId());
		accountTypeBaseService.saveAll(accountTypeList);
		
		var accountList = AssetInitialData.getInitialData(bookkeeping.getId(), accountTypeList);
		accountBaseService.saveAll(accountList);
		
		var entryTransactionTypeList = TransactionTypeInitialData.getInitialData(bookkeeping.getId());
		entryTransactionTypeBaseService.saveAll(entryTransactionTypeList);
		
	}
}
