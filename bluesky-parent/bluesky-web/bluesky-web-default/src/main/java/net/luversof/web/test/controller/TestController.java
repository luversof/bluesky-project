package net.luversof.web.test.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class TestController {
	
	private List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	@RequestMapping("/questions")
	public SseEmitter questions() {
		SseEmitter sseEmitter = new SseEmitter();
		
		emitters.add(sseEmitter);
		
		sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
		
		return sseEmitter;
	}
	
	@RequestMapping(value = "/new-question", method = RequestMethod.POST)
	public void postQuestion(String question) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name("spring").data(question));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@MessageMapping("/questions2")
	public String processQuestion2(String question2) {
		return question2.toUpperCase();
	}
}
