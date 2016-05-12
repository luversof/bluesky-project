package net.luversof.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket petApi() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("bluesky-web-default")
				.apiInfo(apiInfo())
				.select()
				.paths(categoryPaths())
				.build().pathMapping("/");
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title("bluesky-web-default")
				.description("호출 api 리스트")
				.termsOfServiceUrl("")
				.license("파란하늘")
				.licenseUrl("")
				.version("2.0")
				.build();
	}
	
	private Predicate<String> categoryPaths() {
        return or(
        		regex("/blog/.*"),
        		regex("/bookkeeping/.*")
        		);
    }
}
