package net.luversof.api.bookkeeping.constant;

public enum EntryTypeCode {
    INCOME(1),
    OUTGOING(2),
    TRANSFER(3),
    ;

    private int code;

    EntryTypeCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EntryTypeCode findByCode(int code) {
        for (var entryTypeCode : EntryTypeCode.values()) {
            if (entryTypeCode.getCode() == code) {
                return entryTypeCode;
            }
        }
        return null;
    }
}
