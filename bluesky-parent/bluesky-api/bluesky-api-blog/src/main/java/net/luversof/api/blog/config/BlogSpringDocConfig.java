package net.luversof.api.blog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OpenAPI.class)
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Blog API",
                        description =
                                """
			Blog Client API 목록
		""",
                        version = "0.0.1-SNAPSHOT"))
public class BlogSpringDocConfig {}
