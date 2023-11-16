package net.luversof.web.gate.thymeleaf.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.luversof.web.gate.thymeleaf.layout.domain.Menu;

@ConfigurationProperties(prefix = "gate.thymeleaf")
public record GateThymeleafProperties (Map<String, List<Menu>> menu){
	
}
