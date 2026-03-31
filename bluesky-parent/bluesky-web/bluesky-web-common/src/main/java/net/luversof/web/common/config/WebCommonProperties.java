package net.luversof.web.common.config;

import java.util.List;
import java.util.Map;
import net.luversof.web.common.menu.domain.Menu;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bluesky.web.common")
public record WebCommonProperties(Map<String, List<Menu>> menu) {}
