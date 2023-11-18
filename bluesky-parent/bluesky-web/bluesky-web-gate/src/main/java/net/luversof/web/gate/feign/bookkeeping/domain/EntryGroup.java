package net.luversof.web.gate.feign.bookkeeping.domain;

import lombok.Builder;
import net.luversof.web.gate.bookkeeping.constant.EntryGroupType;

@Builder(toBuilder = true)
public record EntryGroup(long idx, String entryGroupId, String bookkeepingId, EntryGroupType entryGroupType, String name) {

}
