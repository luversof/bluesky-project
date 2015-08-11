package net.luversof.web.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.stereotype.Component;

/**
 * embed tomcat 설정 
 * @author bluesky
 *
 */
@Component
public class BlueskyEmbeddedServletContainerCustomizer implements EmbeddedServletContainerCustomizer {

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		container.setPort(8081);
		TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory = (TomcatEmbeddedServletContainerFactory) container;
		tomcatEmbeddedServletContainerFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
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
		{
		    final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		    connector.setScheme("https");
		    connector.setSecure(true);
		    connector.setPort(8443);
		    connector.setRedirectPort(8081);
		    
		    Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
	        protocol.setSSLEnabled(true);
	        //protocol.setKeystoreFile("C:/Users/luver/keystore.p12");
	        protocol.setKeystoreFile("/Users/choiyong-rak/keystore.p12");
	        protocol.setKeystorePass("password");
	        protocol.setKeystoreType("PKCS12");
	        protocol.setProperty("keystoreProvider", "SunJSSE");
	        protocol.setKeyAlias("tomcat");
			tomcatEmbeddedServletContainerFactory.addAdditionalTomcatConnectors(connector);
		}
		/* (e) https to http redirect */
	}
}
