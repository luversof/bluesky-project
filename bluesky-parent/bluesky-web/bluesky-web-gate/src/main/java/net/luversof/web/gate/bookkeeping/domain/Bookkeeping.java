package net.luversof.web.gate.bookkeeping.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record Bookkeeping(long idx, String bookkeepingId, String userId, String name, int baseDate) {

}
