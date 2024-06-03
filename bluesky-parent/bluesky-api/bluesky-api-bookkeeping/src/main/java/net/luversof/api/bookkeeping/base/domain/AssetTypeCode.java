package net.luversof.api.bookkeeping.base.domain;

public enum AssetTypeCode {
	
	CONTRA_ACCOUNT,	// 내부적으로만 사용되는 계정, 내부 용에 대한 구분값이 필요할지도?
	CASH,			// 현금
	BANK,			// 은행
	CREDITCARD,		// 신용카드
	CHECKCARD,		// 체크카드
	INVESTMENT,		// 투자
	LOAN,			// 대출
	INSURANCE,		// 보험
	ETC				// 기타
	;
	
}
