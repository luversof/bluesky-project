package net.luversof.api.bookkeeping.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AssetType은 사용자가 자유롭게 정의해서 추가할 수 있음
 * 다만 통계를 위해서 모든 사용자의 AssetType은 정해진 AssetTypeCode 중 하나를 반드시 가져야 함
 */
@AllArgsConstructor
@Getter
public enum AssetTypeCode {
	
	CONTRA_ASSET(0),	// 내부적으로만 사용되는 계정, 내부 용에 대한 구분값이 필요할지도?
	CASH(1),			// 현금
	BANK(11),			// 은행
	CREDITCARD(21),		// 신용카드
	CHECKCARD(22),		// 체크카드
	INVESTMENT(31),		// 투자
	LOAN(41),			// 대출
	INSURANCE(51),		// 보험
	ETC(91),			// 기타
	;
	
	private int code;
	
	public static AssetTypeCode findByCode(int code) {
		for (var assetTypeCode : AssetTypeCode.values()) {
			if (assetTypeCode.getCode() == code) {
				return assetTypeCode;
			}
		}
		return null;
	}
	
}
