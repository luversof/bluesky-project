package net.luversof.web.gate.bookkeeping.constant;

public enum AssetTypeCode {

	CONTRA_ASSET(0), // 내부적으로만 사용되는 계정, 내부 용에 대한 구분값이 필요할지도?
	CASH(1), // 현금
	BANK(11), // 은행
	CREDITCARD(21), // 신용카드
	CHECKCARD(22), // 체크카드
	INVESTMENT(31), // 투자
	LOAN(41), // 대출
	INSURANCE(51), // 보험
	ETC(91), // 기타
	;

	private int code;

	AssetTypeCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static AssetTypeCode findByCode(int code) {
		for (var assetTypeCode : AssetTypeCode.values()) {
			if (assetTypeCode.getCode() == code) {
				return assetTypeCode;
			}
		}
		return null;
	}

}
