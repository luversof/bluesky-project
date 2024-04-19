package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;

import io.github.luversof.boot.context.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.EntryTypeCode;

@Getter
@AllArgsConstructor
public enum EntryTypeInitialData {
	
	SALARY(EntryTypeCode.INCOME),
	BONUS(EntryTypeCode.INCOME),
	INTEREST(EntryTypeCode.INCOME),
	INCOME_ETC(EntryTypeCode.INCOME),
	FOOD(EntryTypeCode.EXPENSE),
	TRANSFORTATION(EntryTypeCode.EXPENSE),
	LIVING(EntryTypeCode.EXPENSE),
	STUDY(EntryTypeCode.EXPENSE),
	CULTURE(EntryTypeCode.EXPENSE),
	SOSIAL(EntryTypeCode.EXPENSE),
	TRANSPORTATION(EntryTypeCode.EXPENSE),
	EXPENSE_ETC(EntryTypeCode.EXPENSE),
	;
	
	private EntryTypeCode entryTypeCode;
	
	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.entry-type.{0}", name()), name());
	}
	
}
