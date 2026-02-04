package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * 계좌
 */
@Table("Account")
public class Account {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	@Column("id")
	private UUID id;

	@NotNull(groups = { Create.class, Update.class, Delete.class })
	@Column("user_id")
	UUID userId;

	@NotBlank(groups = { Create.class, Update.class })
	@Column("name")
	private String name;

	@Column("base_currency")
	private String baseCurrency;

	@CreatedDate
	@Column("createdDate")
	private Instant createdDate;

	@Column("jsonConfig")
	private Map<String, Object> jsonConfig;

	public interface Create {
	}

	public interface Update {
	}

	public interface Delete {
	}

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

	public String getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
		return Objects.equals(createdDate, other.createdDate) && Objects.equals(id, other.id)
				&& Objects.equals(jsonConfig, other.jsonConfig) && Objects.equals(name, other.name)
				&& Objects.equals(userId, other.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDate, id, jsonConfig, name, userId);
	}

	@Override
	public String toString() {
		return "Account [id=" + id + ", userId=" + userId + ", name=" + name + ", createdDate=" + createdDate
				+ ", jsonConfig=" + jsonConfig + "]";
	}
}
