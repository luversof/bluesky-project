package net.luversof.web.dynamiccrud.use.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.web.dynamiccrud.use.domain.UseParameter;

/**
 * 설정된 데이터를 호출하여 화면을 구성
 */
@Controller
@RequestMapping("/use")
public class UseController {

	@GetMapping("/{product}/{mainMenu}/{subMenu}")
	public String view(UseParameter useParameter, Model model) {
		
		// Setting 정보를 호출하여 해당 요청에 대한 설정이 있는지 확인
		
		// Setting 정보를 기준으로 해당 데이터를 조회
		
		return "use/index";
	}
}
