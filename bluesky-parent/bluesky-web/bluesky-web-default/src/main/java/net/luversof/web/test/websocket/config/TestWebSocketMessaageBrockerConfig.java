package net.luversof.web.test.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
//@EnableWebSocketMessageBroker
public class TestWebSocketMessaageBrockerConfig implements WebSocketMessageBrokerConfigurer {


	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry
			.setApplicationDestinationPrefixes("/app")
			.enableStompBrokerRelay("/topic", "/queue")
			.setRelayHost("10.0.75.1")
			//.setSystemHeartbeatReceiveInterval(10000)
            //.setSystemHeartbeatSendInterval(10000);
			;
	}
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/questions2")
//			.setAllowedOrigins("http://10.0.75.1")
			.setAllowedOrigins("*")
			.withSockJS();
	}
	
	
	public class TestDefaultHandshakeHandler extends DefaultHandshakeHandler {
		
	}

}
