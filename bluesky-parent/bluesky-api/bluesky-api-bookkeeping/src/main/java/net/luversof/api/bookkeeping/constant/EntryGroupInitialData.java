//package net.luversof.api.bookkeeping.constant;
//
//import java.text.MessageFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//import io.github.luversof.boot.context.MessageUtil;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import net.luversof.api.bookkeeping.domain.EntryGroup;
//
///**
// * 최초 생성시 추가되는 항목
// * 
// * @author bluesky
// *
// */
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//public enum EntryGroupInitialData {
//	SALARY("salary", EntryGroupType.INCOME),
//	BONUS("bonus", EntryGroupType.INCOME),
//	INTEREST("interest", EntryGroupType.INCOME),
//	INCOME_ETC("etc", EntryGroupType.INCOME),
//	FOOD("foot", EntryGroupType.EXPENSE),
//	TRANSPORTATION("transportation", EntryGroupType.EXPENSE),
//	LIVING("living", EntryGroupType.EXPENSE),
//	STUDY("study", EntryGroupType.EXPENSE),
//	CULTURE("culture", EntryGroupType.EXPENSE),
//	SOCIAL("social", EntryGroupType.EXPENSE),
//	MAINTENANCE_COST("maintenanceCost", EntryGroupType.EXPENSE),
//	EXPENSE_ETC("etc", EntryGroupType.EXPENSE);
//
//	
//	@Getter
//	private String messageCode;
//	
//	@Getter
//	private EntryGroupType entryGroupType;
//
//	public String getName() {
//		return MessageUtil.getMessage(MessageFormat.format("constant.bookkeeping.entry-group.{0}.{1}.name", this.getEntryGroupType().name(), this.getMessageCode()));
//	}
//	
//	public static List<EntryGroup> createEntryGroupList(UUID bookkeepingId) {
//		List<EntryGroup> entryGroupList = new ArrayList<>();
//		
//		Arrays.asList(EntryGroupInitialData.values()).forEach(entryGroupInitialData -> {
//			EntryGroup entryGroup = new EntryGroup();
//			entryGroup.setBookkeepingId(bookkeepingId);
//			entryGroup.setName(entryGroupInitialData.getName());
//			entryGroup.setEntryGroupType(entryGroupInitialData.getEntryGroupType());
//			entryGroupList.add(entryGroup);
//		});
//		
//		return entryGroupList;
//	}
//}