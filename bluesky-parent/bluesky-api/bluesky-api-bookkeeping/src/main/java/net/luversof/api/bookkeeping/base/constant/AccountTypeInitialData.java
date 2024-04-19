package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.AccountType;
import net.luversof.api.bookkeeping.base.domain.AccountTypeCode;


/**
 * 기본 생성하여 제공하는 AccountType 데이터
 */
@Getter
@AllArgsConstructor
public enum AccountTypeInitialData {
	
	CONTRA_ACCOUNT(AccountTypeCode.CONTRA_ACCOUNT),
	CASH(AccountTypeCode.CASH),
	BANK(AccountTypeCode.BANK),
	CREDITCARD(AccountTypeCode.CREDITCARD),
	CHECKCARD(AccountTypeCode.CHECKCARD),
	INVESTMENT(AccountTypeCode.INVESTMENT),
	LOAN(AccountTypeCode.LOAN),
	INSURANCE(AccountTypeCode.INSURANCE),
	ETC(AccountTypeCode.ETC),
	;
	private AccountTypeCode code;
	
	private String getName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account-type.{0}", name()), name());
	}
	
	
	public static List<AccountType> getInitialData(UUID bookkeepingId) {
		var accountTypeList = new ArrayList<AccountType>();
		
		for (var accountTypeInitialData : AccountTypeInitialData.values()) {
			var accountType = new AccountType();
			accountType.setBookkeepingId(bookkeepingId);
			accountType.setName(accountTypeInitialData.getName());
			accountType.setCode(accountTypeInitialData.getCode());
			accountTypeList.add(accountType);
		}
		
		return accountTypeList;
	}
}
