package net.luversof.api.bookkeeping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.bookkeeping.service.StatisticService;

@RestController
@RequestMapping(value = "/api/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {

	@Setter(onMethod_ = @Autowired)
	private StatisticService statisticService;
	
	@GetMapping
	public String test() {
		return "Test";
	}
}
