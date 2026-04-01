package net.luversof.web.gate.bookkeeping.domain;

import java.util.UUID;
import net.luversof.web.gate.bookkeeping.constant.AssetTypeCode;

public record AssetType(UUID id, UUID bookkeepingId, AssetTypeCode code, String name) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private UUID id;
        private UUID bookkeepingId;
        private AssetTypeCode code;
        private String name;

        public Builder() {}

        public Builder(AssetType assetType) {
            this.id = assetType.id();
            this.bookkeepingId = assetType.bookkeepingId();
            this.code = assetType.code();
            this.name = assetType.name();
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder bookkeepingId(UUID bookkeepingId) {
            this.bookkeepingId = bookkeepingId;
            return this;
        }

        public Builder code(AssetTypeCode code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public AssetType build() {
            return new AssetType(id, bookkeepingId, code, name);
        }
    }
}
