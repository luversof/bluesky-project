package net.luversof.web.gate.stock.httpexchange;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

@HttpExchange(
    url = "/api/monthlyDividendSnapshot",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface MonthlyDividendSnapshotClient {

  @GetExchange
  List<MonthlyDividendSnapshotResponse> findSnapshots(
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> request);

  @PostExchange
  MonthlyDividendSnapshotResponse upsertSnapshot(
      @RequestBody MonthlyDividendSnapshotUpsertRequest request);

  @DeleteExchange
  void deleteSnapshot(@RequestParam UUID userId, @RequestParam String symbol);
}
