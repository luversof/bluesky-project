package net.luversof.web.test.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class Test2Controller {
	
	@MessageMapping("/questions2")
	//@SendTo("/topic/questions2")
	public String processQuestion2(String question/*, Principal principal*/) {
		//return question.toUpperCase() + "by " + principal.getName();
		return question.toUpperCase();
	}
}
