package net.luversof.api.bookkeeping.constant;

import java.util.BitSet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetBitConfig {
	
	ENABLE_DELETE(0),	// asset 삭제 가능
	ENABLE_DISPLAY(1),	// 외부 노출 가능
	
	;
	
	private int bitIndex;

	public static BitSet getInitialData() {
		var bitSet =  new BitSet();
		for (var boardBitConfig : AssetBitConfig.values()) {
			bitSet.set(boardBitConfig.getBitIndex());
		}
		return bitSet;
	}

}
