package net.luversof.bookkeeping.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.boot.autoconfigure.context.MessageUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 최초 생성시 추가되는 항목
 * 
 * @author bluesky
 *
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum EntryGroupInitialData {
	SALARY("salary", EntryGroupType.CREDIT),
	BONUS("bonus", EntryGroupType.CREDIT),
	INTEREST("interest", EntryGroupType.CREDIT),
	CREDIT_ETC("etc", EntryGroupType.CREDIT),
	FOOD("foot", EntryGroupType.DEBIT),
	TRANSPORTATION("transportation", EntryGroupType.DEBIT),
	LIVING("living", EntryGroupType.DEBIT),
	STUDY("study", EntryGroupType.DEBIT),
	CULTURE("culture", EntryGroupType.DEBIT),
	SOCIAL("social", EntryGroupType.DEBIT),
	MAINTENANCE_COST("maintenanceCost", EntryGroupType.DEBIT),
	DEBIT_ETC("etc", EntryGroupType.DEBIT);

	
	@Getter
	private String messageCode;
	
	@Getter
	private EntryGroupType entryType;

	public String getName() {
		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.entry-group.{0}.{1}.name", this.getEntryType().name(), this.getMessageCode()));
	}
	
	public static List<EntryGroup> getEntryGroupList(Bookkeeping bookkeeping) {
		List<EntryGroup> entryGroupList = new ArrayList<>();
		
		Arrays.asList(EntryGroupInitialData.values()).forEach(entryGroupInitialData -> {
			EntryGroup entryGroup = new EntryGroup();
			entryGroup.setName(entryGroupInitialData.getName());
			entryGroup.setBookkeeping(bookkeeping);
			entryGroup.setEntryType(entryGroupInitialData.getEntryType());
			entryGroupList.add(entryGroup);
		});
		
		return entryGroupList;
	}
}