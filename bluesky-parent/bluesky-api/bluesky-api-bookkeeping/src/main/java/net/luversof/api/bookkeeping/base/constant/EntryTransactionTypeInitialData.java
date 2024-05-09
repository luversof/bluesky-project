package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.EntryTransactionType;
import net.luversof.api.bookkeeping.base.domain.EntryTransactionTypeCode;

/**
 * 기본 생성하여 제공하는 EntryType
 */
@Getter
@AllArgsConstructor
public enum EntryTransactionTypeInitialData {
	
	INCOME_SALARY(EntryTransactionTypeCode.INCOME),
	INCOME_BONUS(EntryTransactionTypeCode.INCOME),
	INCOME_INTEREST(EntryTransactionTypeCode.INCOME),
	INCOME_ETC(EntryTransactionTypeCode.INCOME),
	EXPENSE_FOOD(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_TRANSFORTATION(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_LIVING(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_STUDY(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_CULTURE(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_SOCIAL(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_MANAGEMENTFEE(EntryTransactionTypeCode.EXPENSE),
	EXPENSE_ETC(EntryTransactionTypeCode.EXPENSE),
	;
	
	private EntryTransactionTypeCode entryTransactionTypeCode;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.entry-transaction-type.{0}", name()), name());
	}
	
	public static List<EntryTransactionType> getInitialData(UUID bookkeepingId) {
		var entryTransactionTypeList = new ArrayList<EntryTransactionType>();
		
		for (var entryTypeInitialData : EntryTransactionTypeInitialData.values()) {
			var entryTransactionType = new EntryTransactionType();
			entryTransactionType.setBookkeepingId(bookkeepingId);
			entryTransactionType.setName(entryTypeInitialData.getLocalizedName());
			entryTransactionType.setCode(entryTypeInitialData.getEntryTransactionTypeCode());
			entryTransactionTypeList.add(entryTransactionType);
		}
		
		return entryTransactionTypeList;
	}
}
