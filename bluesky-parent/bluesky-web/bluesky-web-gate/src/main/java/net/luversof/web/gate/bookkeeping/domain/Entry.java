package net.luversof.web.gate.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Entry(
    UUID id,
    UUID bookkeepingId,
    UUID entryTypeId,
    UUID incomeAssetId,
    UUID outgoingAssetId,
    Instant entryDate,
    BigDecimal amount,
    Map<String, Object> extraData) {}
