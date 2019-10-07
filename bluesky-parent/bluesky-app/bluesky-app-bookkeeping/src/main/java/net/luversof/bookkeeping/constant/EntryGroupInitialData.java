package net.luversof.bookkeeping.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.boot.autoconfigure.context.MessageUtil;

import java.text.MessageFormat;

/**
 * 최초 생성시 추가되는 항목
 * 
 * @author bluesky
 *
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EntryGroupInitialData {
	SALARY(EntryType.CREDIT, "salary"),
	BONUS(EntryType.CREDIT, "bonus"),
	INTEREST(EntryType.CREDIT, "interest"),
	CREDIT_ETC(EntryType.CREDIT, "etc"),
	FOOD(EntryType.DEBIT, "foot"),
	TRANSPORTATION(EntryType.DEBIT, "transportation"),
	LIVING(EntryType.DEBIT, "living"),
	STUDY(EntryType.DEBIT, "study"),
	CULTURE(EntryType.DEBIT, "culture"),
	SOCIAL(EntryType.DEBIT, "social"),
	MAINTENANCE_COST(EntryType.DEBIT, "maintenanceCost"),
	DEBIT_ETC(EntryType.DEBIT, "etc");

	@Getter
	private EntryType entryType;
	
	@Getter
	private String messageCode;

	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.entryGroup.{0}.{1}.name", getEntryType().name().toLowerCase(), getMessageCode()));
	}
}