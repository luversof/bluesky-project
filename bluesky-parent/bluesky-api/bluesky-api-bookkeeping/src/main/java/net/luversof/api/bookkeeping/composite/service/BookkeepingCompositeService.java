package net.luversof.api.bookkeeping.composite.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.constant.AccountInitialData;
import net.luversof.api.bookkeeping.base.constant.AccountTypeInitialData;
import net.luversof.api.bookkeeping.base.constant.EntryTransactionTypeInitialData;
import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;
import net.luversof.api.bookkeeping.base.service.AccountTypeBaseService;
import net.luversof.api.bookkeeping.base.service.BookkeepingBaseService;
import net.luversof.api.bookkeeping.base.service.EntryTransactionTypeBaseService;

@RequiredArgsConstructor
@Service
public class BookkeepingCompositeService {
	
	private final BookkeepingBaseService bookkeepingBaseService;
	private final AccountTypeBaseService accountTypeBaseService;
	private final AccountBaseService accountBaseService;
	private final EntryTransactionTypeBaseService entryTransactionTypeBaseService;
	
	/**
	 * 가계부 생성
	 * 1. bookkeeping 생성
	 * 2. 기본적인 데이터 (Bookkeeping, Account, AccountType, EntryTransactionType) 생성
	 */
	@Transactional
	public void create(Bookkeeping bookkeeping) {
		
		bookkeepingBaseService.save(bookkeeping);
		
		var accountTypeList = AccountTypeInitialData.getInitialData(bookkeeping.getId());
		accountTypeBaseService.saveAll(accountTypeList);
		
		var accountList = AccountInitialData.getInitialData(bookkeeping.getId(), accountTypeList);
		accountBaseService.saveAll(accountList);
		
		var entryTransactionTypeList = EntryTransactionTypeInitialData.getInitialData(bookkeeping.getId());
		entryTransactionTypeBaseService.saveAll(entryTransactionTypeList);
		
	}
}
