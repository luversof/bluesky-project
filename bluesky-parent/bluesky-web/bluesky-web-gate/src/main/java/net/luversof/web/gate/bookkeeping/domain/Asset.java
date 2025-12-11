package net.luversof.web.gate.bookkeeping.domain;

import java.util.Map;
import java.util.UUID;

public record Asset(
		UUID id,
		UUID bookkeepingId,
		UUID assetTypeId,
		String name,
		Map<String, Object> jsonConfig) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {
		private UUID id;
		private UUID bookkeepingId;
		private UUID assetTypeId;
		private String name;
		private Map<String, Object> jsonConfig;

		public Builder() {
		}

		public Builder(Asset asset) {
			this.id = asset.id();
			this.bookkeepingId = asset.bookkeepingId();
			this.assetTypeId = asset.assetTypeId();
			this.name = asset.name();
			this.jsonConfig = asset.jsonConfig();
		}

		public Builder id(UUID id) {
			this.id = id;
			return this;
		}

		public Builder bookkeepingId(UUID bookkeepingId) {
			this.bookkeepingId = bookkeepingId;
			return this;
		}

		public Builder assetTypeId(UUID assetTypeId) {
			this.assetTypeId = assetTypeId;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder jsonConfig(Map<String, Object> jsonConfig) {
			this.jsonConfig = jsonConfig;
			return this;
		}

		public Asset build() {
			return new Asset(id, bookkeepingId, assetTypeId, name, jsonConfig);
		}
	}
}
