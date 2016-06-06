package net.luversof.bookkeeping.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 통계 정보 반환을 위한 객체
 * entryList -> 그룹별 통계 List로 변환하여 내려보낸다.
 * @author luver
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Statistics {
	
	private long amount;
	
	private EntryGroup entryGroup;
	
}
