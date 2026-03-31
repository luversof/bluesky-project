package net.luversof.api.bookkeeping.constant;

import static net.luversof.api.bookkeeping.constant.EntryTypeCode.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.api.bookkeeping.domain.EntryType;

/** 기본 생성하여 제공하는 EntryType */
public enum EntryTypeInitialData {
    INCOME_SALARY(INCOME),
    INCOME_BONUS(INCOME),
    INCOME_INTEREST(INCOME),
    INCOME_ETC(INCOME),
    EXPENSE_FOOD(OUTGOING),
    EXPENSE_TRANSFORTATION(OUTGOING),
    EXPENSE_LIVING(OUTGOING),
    EXPENSE_STUDY(OUTGOING),
    EXPENSE_CULTURE(OUTGOING),
    EXPENSE_SOCIAL(OUTGOING),
    EXPENSE_MANAGEMENTFEE(OUTGOING),
    EXPENSE_ETC(OUTGOING),
    ;

    private EntryTypeCode entryTypeCode;

    EntryTypeInitialData(EntryTypeCode entryTypeCode) {
        this.entryTypeCode = entryTypeCode;
    }

    public EntryTypeCode getEntryTypeCode() {
        return entryTypeCode;
    }

    public String getLocalizedName() {
        return MessageUtil.getMessage(
                MessageFormat.format("bookkeeping.constant.entry-transaction-type.{0}", name()),
                name());
    }

    public static List<EntryType> getInitialData(UUID bookkeepingId) {
        var entryTransactionTypeList = new ArrayList<EntryType>();

        for (var transactionTypeInitialData : EntryTypeInitialData.values()) {
            var transactionType = new EntryType();
            transactionType.setBookkeepingId(bookkeepingId);
            transactionType.setName(transactionTypeInitialData.getLocalizedName());
            transactionType.setCode(transactionTypeInitialData.getEntryTypeCode());
            entryTransactionTypeList.add(transactionType);
        }

        return entryTransactionTypeList;
    }
}
