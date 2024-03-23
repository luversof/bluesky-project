package net.luversof.web.common.menu.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import net.luversof.web.common.menu.domain.Menu;


@ConfigurationProperties(prefix = "bluesky.web.common")
public record WebCommonProperties(Map<String, List<Menu>> menu) {

}
