package net.luversof.web.gate.stock.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.client.user.util.UserUtil;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

	@GetMapping
	public String index(Model model) {
		if (UserUtil.getUserId() == null) {
			model.addAttribute("requireLogin", true);
		}
		return "stock/index";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		if (UserUtil.getUserId() == null) {
			model.addAttribute("requireLogin", true);
		}
		return "stock/dashboard";
	}
}
