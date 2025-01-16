package net.luversof.api.bookkeeping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("bluesky.api.bookkeeping")
public class BookkeepingProperties {

	private String databaseCatalog;

}
