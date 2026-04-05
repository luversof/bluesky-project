package net.luversof.web.gate.stock.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TradeSearchRequest(
    UUID userId,
    List<UUID> accountIdList,
    List<UUID> stockItemIdList,
    Instant startDate,
    Instant endDate) {

  public org.springframework.util.MultiValueMap<String, String> toParams() {
    org.springframework.util.MultiValueMap<String, String> params =
        new org.springframework.util.LinkedMultiValueMap<>();
    if (userId != null) params.add("userId", userId.toString());
    if (accountIdList != null)
      accountIdList.forEach(id -> params.add("accountIdList", id.toString()));
    if (stockItemIdList != null)
      stockItemIdList.forEach(id -> params.add("stockItemIdList", id.toString()));
    if (startDate != null) params.add("startDate", startDate.toString());
    if (endDate != null) params.add("endDate", endDate.toString());
    return params;
  }
}
