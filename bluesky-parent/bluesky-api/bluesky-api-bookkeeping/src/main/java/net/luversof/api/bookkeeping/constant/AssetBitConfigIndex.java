package net.luversof.api.bookkeeping.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetBitConfigIndex {
	
	ENABLE_DELETE(0),	// asset 삭제 가능, contra asset은 삭제 불가능함
	ENABLE_DISPLAY(1),	// 외부 노출 가능
	USER_DELETE(2), 	// 유저가 삭제한 자산
	
	;
	
	private int bitIndex;

}
