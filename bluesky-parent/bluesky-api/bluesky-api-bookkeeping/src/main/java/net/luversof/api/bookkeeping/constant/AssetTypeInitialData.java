package net.luversof.api.bookkeeping.constant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.api.bookkeeping.domain.AssetType;

/** 기본 생성하여 제공하는 AccountType */
public enum AssetTypeInitialData {
    CONTRA_ASSET(AssetTypeCode.CONTRA_ASSET),
    CASH(AssetTypeCode.CASH),
    BANK(AssetTypeCode.BANK),
    CREDITCARD(AssetTypeCode.CREDITCARD),
    CHECKCARD(AssetTypeCode.CHECKCARD),
    INVESTMENT(AssetTypeCode.INVESTMENT),
    LOAN(AssetTypeCode.LOAN),
    INSURANCE(AssetTypeCode.INSURANCE),
    ETC(AssetTypeCode.ETC),
    ;

    private AssetTypeCode code;

    AssetTypeInitialData(AssetTypeCode code) {
        this.code = code;
    }

    public AssetTypeCode getCode() {
        return code;
    }

    private String getLocalizedName() {
        return MessageUtil.getMessage(
                MessageFormat.format("bookkeeping.constant.account-type.{0}", name()), name());
    }

    public static List<AssetType> getInitialData(UUID bookkeepingId) {
        var accountTypeList = new ArrayList<AssetType>();

        for (var accountTypeInitialData : AssetTypeInitialData.values()) {
            var accountType = new AssetType();
            accountType.setBookkeepingId(bookkeepingId);
            accountType.setName(accountTypeInitialData.getLocalizedName());
            accountType.setCode(accountTypeInitialData.getCode());
            accountTypeList.add(accountType);
        }

        return accountTypeList;
    }
}
