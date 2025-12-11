package net.luversof.web.gate.stock.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.Setter;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

	@Setter(onMethod_ = @Autowired)
	private AccountClient accountClient;

	@Setter(onMethod_ = @Autowired)
	private StockItemClient stockItemClient;

	@GetMapping
	public String index(Model model) {
		UUID userId = UserUtil.getUserId();
		if (userId != null) {
			// 계좌 목록을 모델에 추가하여 select 옵션으로 사용
			var accounts = accountClient.getAccountsByUserId(userId);
			model.addAttribute("accounts", accounts);
		}

		// 종목 목록은 사용자와 무관하게 전체 리스트를 제공
		var stockItems = stockItemClient.getStockItems();
		model.addAttribute("stockItems", stockItems);
		return "stock/index";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "stock/dashboard";
	}

	@GetMapping("/dividend")
	public String dividendPage() {
		return "stock/dividend";
	}
}
