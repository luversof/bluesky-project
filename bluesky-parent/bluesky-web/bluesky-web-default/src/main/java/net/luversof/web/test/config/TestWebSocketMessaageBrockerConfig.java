package net.luversof.web.test.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class TestWebSocketMessaageBrockerConfig extends AbstractWebSocketMessageBrokerConfigurer {


	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry
			.setApplicationDestinationPrefixes("/app")
			.enableStompBrokerRelay("/topic", "/queue")
			.setRelayHost("10.0.75.1")
			.setRelayPort(5672)
			//.setSystemHeartbeatReceiveInterval(10000)
            //.setSystemHeartbeatSendInterval(10000);
			;
	}
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/questions2").withSockJS();
	}
	
	
	public class TestDefaultHandshakeHandler extends DefaultHandshakeHandler {
		
	}

}
