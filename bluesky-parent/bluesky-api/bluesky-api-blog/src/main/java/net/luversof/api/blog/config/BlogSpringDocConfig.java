package net.luversof.api.blog.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@ConditionalOnClass(OpenAPI.class)
@OpenAPIDefinition(
	info = @Info(
		title = "Blog API",
		description = """
			Blog Client API 목록
		""",
		version = "0.0.1-SNAPSHOT"
	)
//	servers = @Server(url = "/", description = "Default Server URL")
)
public class BlogSpringDocConfig {
	
	@Bean
	OpenApiCustomizer serverOpenApiCustomizer() {
		return openApi -> {
			var request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			String scheme = request.getScheme(); // http or https
			String host = request.getServerName(); // e.g. localhost
			int port = request.getServerPort(); // e.g. 8080

			String url = scheme + "://" + host + (port != 80 && port != 443 ? ":" + port : "");
			openApi.setServers(java.util.List.of(new Server().url(url)));
		};
	}
}
