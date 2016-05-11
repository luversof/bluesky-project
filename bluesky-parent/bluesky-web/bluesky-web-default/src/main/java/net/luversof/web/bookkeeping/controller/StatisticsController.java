package net.luversof.web.bookkeeping.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookkeeping/{bookkeepingId}/statistics")
public class StatisticsController {

	//기간별 통계 보기
	//월간, 년간 통계보기를 기본으로 삼자.
}
