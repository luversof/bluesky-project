package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.EntryType;
import net.luversof.api.bookkeeping.base.domain.EntryTypeCode;

/**
 * 기본 생성하여 제공하는 EntryType
 */
@Getter
@AllArgsConstructor
public enum EntryTypeInitialData {
	
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
	
	private EntryTypeCode entryTypeCode;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.entry-type.{0}", name()), name());
	}
	
	public static List<EntryType> getInitialData(UUID bookkeepingId) {
		var entryTypeList = new ArrayList<EntryType>();
		
		for (var entryTypeInitialData : EntryTypeInitialData.values()) {
			var entryType = new EntryType();
			entryType.setBookkeepingId(bookkeepingId);
			entryType.setName(entryTypeInitialData.getLocalizedName());
			entryType.setCode(entryTypeInitialData.getEntryTypeCode());
			entryTypeList.add(entryType);
		}
		
		return entryTypeList;
	}
}
