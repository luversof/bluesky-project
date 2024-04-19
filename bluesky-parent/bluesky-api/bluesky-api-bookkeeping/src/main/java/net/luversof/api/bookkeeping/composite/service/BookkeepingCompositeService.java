package net.luversof.api.bookkeeping.composite.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.luversof.boot.exception.BlueskyException;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.constant.AccountInitialData;
import net.luversof.api.bookkeeping.base.constant.AccountTypeInitialData;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.domain.AccountType;
import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
import net.luversof.api.bookkeeping.base.service.AccountBaseService;
import net.luversof.api.bookkeeping.base.service.AccountTypeBaseService;
import net.luversof.api.bookkeeping.base.service.BookkeepingBaseService;

@Service
@RequiredArgsConstructor
public class BookkeepingCompositeService {
	
	private final BookkeepingBaseService bookkeepingBaseService;
	private final AccountTypeBaseService accountTypeBaseService;
	private final AccountBaseService accountBaseService;
	
	/**
	 * 가계부 생성
	 * 1. bookkeeping 생성
	 * 2. 기본적인 데이터 (Bookkeeping, Account, AccountType, EntryType) 생성
	 */
	@Transactional
	public void create(Bookkeeping bookkeeping) {
		
		bookkeepingBaseService.save(bookkeeping);
		
		var accountTypeList = AccountTypeInitialData.getInitialData(bookkeeping.getId());
		accountTypeBaseService.saveAll(accountTypeList);
		
		List<Account> accountList = AccountInitialData.getInitialData(bookkeeping.getId(), accountTypeList);
		accountBaseService.saveAll(accountList);
		
	}
}
