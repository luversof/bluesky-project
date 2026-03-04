package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.UUID;

public record StockItemDateRange(UUID stockItemId, Instant minDate, Instant maxDate) {
}
