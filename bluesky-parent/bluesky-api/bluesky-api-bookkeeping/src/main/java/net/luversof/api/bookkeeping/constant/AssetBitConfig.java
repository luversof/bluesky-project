package net.luversof.api.bookkeeping.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetBitConfig {
	
	ENABLE_DELETE(0),	// asset 삭제 가능
	ENABLE_DISPLAY(1),	// 외부 노출 가능
	
	;
	
	private int bitIndex;

}
