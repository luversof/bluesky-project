package net.luversof.web.gate.thymeleaf.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.luversof.web.gate.thymeleaf.layout.domain.MainMenu;

@ConfigurationProperties(prefix = "gate.thymeleaf")
public record GateThymeleafProperties (List<MainMenu> mainMenuList){
	
}
