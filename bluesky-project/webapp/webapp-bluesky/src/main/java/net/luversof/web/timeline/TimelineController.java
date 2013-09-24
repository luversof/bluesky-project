package net.luversof.web.timeline;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/timeline")
public class TimelineController {

	@RequestMapping
	public String index() {
		return "/timeline/index";
	}
}
