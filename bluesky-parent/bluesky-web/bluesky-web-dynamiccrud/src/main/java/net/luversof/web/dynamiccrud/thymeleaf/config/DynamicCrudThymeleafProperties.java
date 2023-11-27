package net.luversof.web.dynamiccrud.thymeleaf.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.luversof.web.dynamiccrud.thymeleaf.domain.Menu;

@ConfigurationProperties(prefix = "dynamiccrud.thymeleaf")
public record DynamicCrudThymeleafProperties(Map<String, List<Menu>> menu) {

}
