package net.luversof.api.board.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Table("Board")
public class Board {

    @Id
    @Column("id")
    @Null(groups = Create.class)
    @NotNull(groups = Update.class)
    private UUID id;

    @Column("alias")
    private String alias;

    @Column("jsonConfig")
    private Map<String, Object> jsonConfig;

    public interface Create {}

    public interface Update {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<String, Object> getJsonConfig() {
        return jsonConfig;
    }

    public void setJsonConfig(Map<String, Object> jsonConfig) {
        this.jsonConfig = jsonConfig;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Board other = (Board) obj;
        return Objects.equals(alias, other.alias)
                && Objects.equals(id, other.id)
                && Objects.equals(jsonConfig, other.jsonConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, id, jsonConfig);
    }

    @Override
    public String toString() {
        return "Board [id=" + id + ", alias=" + alias + ", jsonConfig=" + jsonConfig + "]";
    }
}
