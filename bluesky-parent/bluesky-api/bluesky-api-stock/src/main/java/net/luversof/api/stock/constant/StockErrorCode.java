package net.luversof.api.stock.constant;

import io.github.luversof.boot.exception.BlueskyErrorCode;

public enum StockErrorCode implements BlueskyErrorCode<StockErrorCode> {
    NOT_EXIST_USER_ID,
    INVALID_USER_ID,
    ;
}
