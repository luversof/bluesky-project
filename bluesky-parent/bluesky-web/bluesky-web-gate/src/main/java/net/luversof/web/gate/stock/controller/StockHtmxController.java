package net.luversof.web.gate.stock.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.Setter;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup;
import net.luversof.web.gate.stock.openfeign.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockHtmxController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitClient tradeProfitClient;

	@GetMapping
	public String page() {
		return "stock/tradeProfit";
	}

	@GetMapping("/calculateProfit")
	public String calculateProfit(
			@RequestParam String userId,
			@RequestParam(required = false) String accountIds,
			@RequestParam(required = false) String stockItemIds,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(required = false, defaultValue = "ACCOUNT_AND_STOCKITEM") TradeProfitRequestGroup groupBy,
			Model model) {

		TradeProfitRequest request = new TradeProfitRequest(
			parseUuid(userId),
			parseUuidList(accountIds),
			parseUuidList(stockItemIds),
			parseOffsetDateTime(startDate),
			parseOffsetDateTime(endDate),
			groupBy
		);

		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request);
		model.addAttribute("tradeProfitList", tradeProfitList);
		return "stock/tradeProfit :: result";
	}

	private UUID parseUuid(String value) {
		if (!StringUtils.hasText(value)) return null;
		return UUID.fromString(value.trim());
	}

	private List<UUID> parseUuidList(String csv) {
		if (!StringUtils.hasText(csv)) return null; // let service infer request type
		List<UUID> list = new ArrayList<>();
		for (String token : Arrays.asList(csv.split(","))) {
			String v = token.trim();
			if (v.isEmpty()) continue;
			try {
				list.add(UUID.fromString(v));
			} catch (IllegalArgumentException ignored) {
				// skip invalid token
			}
		}
		return list.isEmpty() ? null : list;
	}

	private OffsetDateTime parseOffsetDateTime(String text) {
		if (!StringUtils.hasText(text)) return null;
		return OffsetDateTime.parse(text.trim());
	}
}
