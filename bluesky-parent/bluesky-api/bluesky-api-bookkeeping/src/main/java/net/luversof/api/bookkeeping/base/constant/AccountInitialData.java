package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.domain.AccountType;

@Getter
@AllArgsConstructor
public enum AccountInitialData {

	WALLET(AccountTypeInitialData.CASH)
	;
	
	private AccountTypeInitialData  accountTypeInitialData;
	
	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account.{0}", name()), name());
	}
	
	public static List<Account> getInitialData(UUID bookkeepingId, List<AccountType> accountTypeList) {
		var accountList = new ArrayList<Account>();
		
		for (var accountInitialData : AccountInitialData.values()) {
			var targetAccoutType = accountTypeList.stream().filter(accountType -> accountInitialData.getAccountTypeInitialData().getCode().equals(accountType.getCode())).findFirst().get();
			var account = new Account();
			account.setBookkeepingId(bookkeepingId);
			account.setName(accountInitialData.getName());
			account.setAccountTypeId(targetAccoutType.getId());
			accountList.add(account);
		}
		
		return accountList;
	}
}
