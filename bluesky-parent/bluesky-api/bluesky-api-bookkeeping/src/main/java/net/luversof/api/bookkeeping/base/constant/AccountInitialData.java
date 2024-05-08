package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.base.domain.Account;
import net.luversof.api.bookkeeping.base.domain.AccountType;

/**
 * 기본 생성하여 제공하는 Account
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum AccountInitialData {

	WALLET(AccountTypeInitialData.CASH)
	;
	
	private AccountTypeInitialData  accountTypeInitialData;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.account.{0}", name()), name());
	}
	
	public static List<Account> getInitialData(UUID bookkeepingId, List<AccountType> accountTypeList) {
		var accountList = new ArrayList<Account>();
		
		for (var accountInitialData : AccountInitialData.values()) {
			var targetAccoutType = accountTypeList.stream().filter(accountType -> accountInitialData.getAccountTypeInitialData().getCode().equals(accountType.getCode())).findFirst().orElseGet(() -> null);
			if (targetAccoutType == null) {
				log.debug("targetAccoutType is not exist : {}", accountInitialData.getAccountTypeInitialData());
				continue;
			}
			
			var account = new Account();
			account.setBookkeepingId(bookkeepingId);
			account.setName(accountInitialData.getLocalizedName());
			account.setAccountTypeId(targetAccoutType.getId());
			accountList.add(account);
		}
		
		return accountList;
	}
}
