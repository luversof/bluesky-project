package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.domain.EntryType;

/**
 * 기본 생성하여 제공하는 EntryType
 */
@Getter
@AllArgsConstructor
public enum TransactionTypeInitialData {
	
	INCOME_SALARY(EntryTypeCode.INCOME),
	INCOME_BONUS(EntryTypeCode.INCOME),
	INCOME_INTEREST(EntryTypeCode.INCOME),
	INCOME_ETC(EntryTypeCode.INCOME),
	EXPENSE_FOOD(EntryTypeCode.EXPENSE),
	EXPENSE_TRANSFORTATION(EntryTypeCode.EXPENSE),
	EXPENSE_LIVING(EntryTypeCode.EXPENSE),
	EXPENSE_STUDY(EntryTypeCode.EXPENSE),
	EXPENSE_CULTURE(EntryTypeCode.EXPENSE),
	EXPENSE_SOCIAL(EntryTypeCode.EXPENSE),
	EXPENSE_MANAGEMENTFEE(EntryTypeCode.EXPENSE),
	EXPENSE_ETC(EntryTypeCode.EXPENSE),
	;
	
	private EntryTypeCode transactionTypeCode;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.entry-transaction-type.{0}", name()), name());
	}
	
	public static List<EntryType> getInitialData(UUID bookkeepingId) {
		var entryTransactionTypeList = new ArrayList<EntryType>();
		
		for (var transactionTypeInitialData : TransactionTypeInitialData.values()) {
			var transactionType = new EntryType();
			transactionType.setBookkeepingId(bookkeepingId);
			transactionType.setName(transactionTypeInitialData.getLocalizedName());
			transactionType.setCode(transactionTypeInitialData.getTransactionTypeCode());
			entryTransactionTypeList.add(transactionType);
		}
		
		return entryTransactionTypeList;
	}
}
