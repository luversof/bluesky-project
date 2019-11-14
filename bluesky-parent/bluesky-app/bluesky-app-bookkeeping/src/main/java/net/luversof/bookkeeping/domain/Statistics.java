package net.luversof.bookkeeping.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.luversof.bookkeeping.constant.EntryGroupType;

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
	
	/**
	 * client 의 sort 처리를 위해 entryGroup.entryType alias 처리
	 * @return
	 */
	public EntryGroupType getEntryType() {
		if (entryGroup == null) {
			return null;
		}
		return entryGroup.getEntryType();
	}
}
