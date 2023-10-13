package net.luversof.web.gate.bookkeeping.domain;

import lombok.Data;

@Data
public class Bookkeeping {
	
	private long idx;
	private String bookkeepingId;
	private String userId;
	private String name;
	private int baseDate;

}
