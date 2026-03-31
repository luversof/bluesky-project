package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;

@HttpExchange(url = "/api/tradeProfit", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface TradeProfitClient {

    @GetExchange("/calculateProfit")
    List<TradeProfit> calculateProfit(
            @org.springframework.web.bind.annotation.RequestParam
                    org.springframework.util.MultiValueMap<String, String> request);

    @GetExchange("/timeSeries")
    List<TradeProfitTimeSeriesPoint> timeSeries(
            @org.springframework.web.bind.annotation.RequestParam
                    org.springframework.util.MultiValueMap<String, String> request);
}
