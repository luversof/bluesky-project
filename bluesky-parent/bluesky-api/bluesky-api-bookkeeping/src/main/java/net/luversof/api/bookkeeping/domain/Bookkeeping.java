package net.luversof.api.bookkeeping.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("Bookkeeping")
public class Bookkeeping {

    @Null(groups = Create.class)
    @NotNull(groups = {Update.class, Delete.class})
    @Id
    private UUID id;

    @NotNull(groups = {Create.class, Update.class, Delete.class})
    @Column("user_id")
    private UUID userId;

    @NotBlank(groups = {Create.class, Update.class})
    private String name;

    @CreatedDate
    @Column("createdDate")
    private Instant createdDate;

    @Column("jsonConfig")
    private Map<String, Object> jsonConfig;

    public interface Create {}

    public interface Update {}

    public interface Delete {}

    public interface Search {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
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
        Bookkeeping that = (Bookkeeping) o;
        return Objects.equals(id, that.id)
                && Objects.equals(userId, that.userId)
                && Objects.equals(name, that.name)
                && Objects.equals(createdDate, that.createdDate)
                && Objects.equals(jsonConfig, that.jsonConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, name, createdDate, jsonConfig);
    }

    @Override
    public String toString() {
        return "Bookkeeping{"
                + "id="
                + id
                + ", userId="
                + userId
                + ", name='"
                + name
                + '\''
                + ", createdDate="
                + createdDate
                + ", jsonConfig="
                + jsonConfig
                + '}';
    }
}
