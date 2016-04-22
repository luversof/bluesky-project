package net.luversof.bookkeeping;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BookkeepingErrorCode {
	public static final String NOT_EXIST_BOOKKEEPING = "NOT_EXIST_BOOKKEEPING";
	public static final String NOT_EXIST_ASSET = "NOT_EXIST_ASSET";
	public static final String NOT_EXIST_ENTRYGROUP = "NOT_EXIST_ENTRYGROUP";
	public static final String NOT_EXIST_ENTRY = "NOT_EXIST_ENTRY";

	public static final String NOT_OWNER_BOOKKEEPING = "NOT_OWNER_BOOKKEEPING";
	public static final String NOT_OWNER_ASSET = "NOT_OWNER_ASSET";
	public static final String NOT_OWNER_ENTRYGROUP = "NOT_OWNER_ENTRYGROUP";
	public static final String NOT_OWNER_ENTRY = "NOT_OWNER_ENTRY";
}
