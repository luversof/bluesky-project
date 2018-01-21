package net.luversof.web.index.handler;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TestTextWebSocketHandler extends TextWebSocketHandler {

	
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		log.debug("session : {}", session);
		log.debug("message : {}", message);
	}

	
}
