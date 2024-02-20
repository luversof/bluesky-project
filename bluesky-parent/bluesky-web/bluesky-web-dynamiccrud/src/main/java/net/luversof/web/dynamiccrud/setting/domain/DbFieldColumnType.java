package net.luversof.web.dynamiccrud.setting.domain;

public enum DbFieldColumnType {

	BOOLEAN,
	DATE,	// 예전 방식, 개선 필요
	INT,
//	LINK,	// 쓰이지 않는 듯 한데...(1건 발견)
	LONG,
	STRING,
	TEXT,
	SPEL,	// 화면 처리용 SpEL
	SPEL_FOR_EDIT	// 입력 처리용 SpEL
}
