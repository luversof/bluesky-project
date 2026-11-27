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

import net.luversof.web.gate.stock.dto.request.HighDividendDesignationUpsertRequest;
import net.luversof.web.gate.stock.dto.response.HighDividendDesignationResponse;

@HttpExchange(
    url = "/api/highDividendDesignation",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface HighDividendDesignationClient {

  @GetExchange
  List<HighDividendDesignationResponse> findAll();

  @PostExchange
  HighDividendDesignationResponse upsert(@RequestBody HighDividendDesignationUpsertRequest request);

  @DeleteExchange
  void delete(@RequestParam UUID id);
}
