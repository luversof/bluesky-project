package net.luversof.bookkeeping.domain;

import lombok.Data;

/**
 * 통계 정보 반환을 위한 객체
 * entryList -> 그룹별 통계 List로 변환하여 내려보낸다.
 * @author luver
 *
 */
@Data
public class Statistics {
	
	private EntryGroup entryGroup;

	private long amount;
	
	private EntryType entryType;
	
}
