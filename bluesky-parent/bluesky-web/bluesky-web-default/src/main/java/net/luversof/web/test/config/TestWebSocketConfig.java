package net.luversof.web.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import net.luversof.web.test.handler.TestTextWebSocketHandler;

@Configuration
@EnableWebSocket
public class TestWebSocketConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(new TestTextWebSocketHandler(), "/questions2").withSockJS();
	}
	

}
