package net.luversof.bookkeeping.domain;

import lombok.Getter;

/**
 * 최초 생성시 추가되는 항목
 * @author bluesky
 *
 */
public enum EntryGroupInitialData {
	CREDIT(EntryType.CREDIT, new String[]{"월급", "상여금", "이자", "기타"}),
	DEBIT(EntryType.DEBIT, new String[]{"식사", "교통비", "생활품", "자기계발", "관리비", "문화", "사회생활", "교육", "기타"});
	
	@Getter private EntryType entryType;
	@Getter private String[] defaltEntryGroupNames;
	private EntryGroupInitialData(EntryType entryType, String[] defaltEntryGroupNames) {
		this.entryType = entryType;
		this.defaltEntryGroupNames = defaltEntryGroupNames;
	}
}
