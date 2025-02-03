package net.luversof.api.bookkeeping.config;

import java.net.URI;
import java.util.List;

import io.github.luversof.boot.core.ModuleInfo;
import io.github.luversof.boot.web.DomainProperties.DomainPropertiesBuilder;

public enum BookkeepingModuleInfo implements ModuleInfo {
	BOOKKEEPING {

		@Override
		public DomainPropertiesBuilder getDomainPropertiesBuilder() {
			return super.getDomainPropertiesBuilder().webList(List.of(URI.create("https://dev123.bluesky.local/")));
		}
		
	}
}
