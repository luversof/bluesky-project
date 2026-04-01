package net.luversof.web.gate.stock.httpexchange;

import java.util.List;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "/api/trade", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface TradeClient {

    @GetExchange
    List<TradeResponse> findTrades(@RequestParam MultiValueMap<String, String> request);
}
