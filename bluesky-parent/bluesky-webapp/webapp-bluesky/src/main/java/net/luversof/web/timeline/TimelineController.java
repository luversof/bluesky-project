package net.luversof.web.timeline;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/timeline")
public class TimelineController {

	@RequestMapping
	public String index() {
		return "/timeline/index";
	}
}
