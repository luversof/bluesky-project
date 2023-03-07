package net.luversof.web.gate.bookkeeping.domain;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder(toBuilder = true)
public record EntryRequestParam(String bookkeepingId, String userId, ZonedDateTime startDate, ZonedDateTime endDate) {

}
