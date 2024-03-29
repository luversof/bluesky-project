package net.luversof.web.gate.feign.bookkeeping.domain;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder(toBuilder = true)
public record EntryRequestParam(String bookkeepingId, String userId, ZonedDateTime startDate, ZonedDateTime endDate) {

}
