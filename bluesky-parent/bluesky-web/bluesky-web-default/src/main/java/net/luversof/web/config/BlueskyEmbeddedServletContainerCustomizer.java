package net.luversof.web.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

/**
 * embed tomcat 설정 
 * @author bluesky
 *
 */
@Component
public class BlueskyEmbeddedServletContainerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
	
	@Value("${spring.profiles.active}")
	private String profile;

	@Override
	public void customize(TomcatServletWebServerFactory server) {
//		container.setPort(8082);
		
		server.addConnectorCustomizers(new TomcatConnectorCustomizer() {
			@Override
			public void customize(Connector connector) {
				connector.setProperty("maxKeepAliveRequests", "1");
				connector.setProperty("connectionTimeout", "20000");
				connector.setProperty("keepAliveTimeout", "1");
				connector.setProperty("maxThreads", "250");
				connector.setProperty("compression", "on");
				connector.setURIEncoding("UTF-8");
			}
		});
		
		/* (s) https to http redirect */
		if (profile.equals("live22")) {
		    final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		    connector.setScheme("https");
		    connector.setSecure(true);
		    connector.setPort(8443);
		    connector.setRedirectPort(8082);
		    
		    Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
	        protocol.setSSLEnabled(true);
	        protocol.setKeystoreFile("file:///" + System.getProperty("user.home").replaceAll("\\\\", "/") + "/keystore.p12");
	        //protocol.setKeystoreFile("/Users/choiyong-rak/keystore.p12");
	        protocol.setKeystorePass("password");
	        protocol.setKeystoreType("PKCS12");
	        protocol.setProperty("keystoreProvider", "SunJSSE");
	        protocol.setKeyAlias("tomcat");
	        server.addAdditionalTomcatConnectors(connector);
		}
		/* (e) https to http redirect */
	}
}
