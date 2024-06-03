package net.luversof.api.bookkeeping.base.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.api.bookkeeping.base.domain.TransactionType;
import net.luversof.api.bookkeeping.base.domain.TransactionTypeCode;

/**
 * 기본 생성하여 제공하는 EntryType
 */
@Getter
@AllArgsConstructor
public enum TransactionTypeInitialData {
	
	INCOME_SALARY(TransactionTypeCode.INCOME),
	INCOME_BONUS(TransactionTypeCode.INCOME),
	INCOME_INTEREST(TransactionTypeCode.INCOME),
	INCOME_ETC(TransactionTypeCode.INCOME),
	EXPENSE_FOOD(TransactionTypeCode.EXPENSE),
	EXPENSE_TRANSFORTATION(TransactionTypeCode.EXPENSE),
	EXPENSE_LIVING(TransactionTypeCode.EXPENSE),
	EXPENSE_STUDY(TransactionTypeCode.EXPENSE),
	EXPENSE_CULTURE(TransactionTypeCode.EXPENSE),
	EXPENSE_SOCIAL(TransactionTypeCode.EXPENSE),
	EXPENSE_MANAGEMENTFEE(TransactionTypeCode.EXPENSE),
	EXPENSE_ETC(TransactionTypeCode.EXPENSE),
	;
	
	private TransactionTypeCode transactionTypeCode;
	
	public String getLocalizedName() {
		return MessageUtil.getMessage(MessageFormat.format("bookkeeping.constant.entry-transaction-type.{0}", name()), name());
	}
	
	public static List<TransactionType> getInitialData(UUID ledgerId) {
		var entryTransactionTypeList = new ArrayList<TransactionType>();
		
		for (var transactionTypeInitialData : TransactionTypeInitialData.values()) {
			var transactionType = new TransactionType();
			transactionType.setLedgerId(ledgerId);
			transactionType.setName(transactionTypeInitialData.getLocalizedName());
			transactionType.setCode(transactionTypeInitialData.getTransactionTypeCode());
			entryTransactionTypeList.add(transactionType);
		}
		
		return entryTransactionTypeList;
	}
}
