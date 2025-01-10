package net.luversof.api.bookkeeping.constant;

import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetBitConfig {
	
	ENABLE_DELETE(0),	// asset 삭제 가능
	ENABLE_UPDATE(1),	// asset 수정 가능, 항목별 수정 가능을 구현하려면 어떻게 해야할까?
	ENABLE_DISPLAY(11),	// 외부 노출 가능
	
//	USER_DELETE(21), 	// 유저가 삭제한 자산? 그냥 보이기만 끄면 되지 않을까?, 근데 유저의 action을 기록하려면 어떻게 해야 할가?
	
	;
	
	private int index;

	/**
	 * 설정된 bitConfigList에 현재 bitConfig가 있는지 여부
	 * @param bitConfigIndexList
	 * @return
	 */
	public boolean hasIndexFromIndexList(List<Integer> bitConfigIndexList) {
		return !CollectionUtils.isEmpty(bitConfigIndexList) && bitConfigIndexList.contains(getIndex());
	}
}
