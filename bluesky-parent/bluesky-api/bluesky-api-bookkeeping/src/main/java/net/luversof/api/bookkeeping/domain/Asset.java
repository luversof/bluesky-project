package net.luversof.api.bookkeeping.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Table("Asset")
public class Asset {

    @Null(groups = Create.class)
    @NotNull(groups = {Update.class, Delete.class})
    @Id
    @Column("id")
    private UUID id;

    @Column("bookkeeping_id")
    @NotNull(groups = {Update.class, Delete.class})
    private UUID bookkeepingId;

    @Column("assetType_id")
    @NotNull(groups = {Update.class})
    private UUID assetTypeId;

    @NotNull(groups = {Update.class})
    private String name;

    @Column("jsonConfig")
    private Map<String, Object> jsonConfig;

    public interface Create {}

    public interface Update {}

    public interface Delete {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookkeepingId() {
        return bookkeepingId;
    }

    public void setBookkeepingId(UUID bookkeepingId) {
        this.bookkeepingId = bookkeepingId;
    }

    public UUID getAssetTypeId() {
        return assetTypeId;
    }

    public void setAssetTypeId(UUID assetTypeId) {
        this.assetTypeId = assetTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getJsonConfig() {
        return jsonConfig;
    }

    public void setJsonConfig(Map<String, Object> jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return Objects.equals(id, asset.id)
                && Objects.equals(bookkeepingId, asset.bookkeepingId)
                && Objects.equals(assetTypeId, asset.assetTypeId)
                && Objects.equals(name, asset.name)
                && Objects.equals(jsonConfig, asset.jsonConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookkeepingId, assetTypeId, name, jsonConfig);
    }

    @Override
    public String toString() {
        return "Asset{"
                + "id="
                + id
                + ", bookkeepingId="
                + bookkeepingId
                + ", assetTypeId="
                + assetTypeId
                + ", name='"
                + name
                + '\''
                + ", jsonConfig="
                + jsonConfig
                + '}';
    }
}
