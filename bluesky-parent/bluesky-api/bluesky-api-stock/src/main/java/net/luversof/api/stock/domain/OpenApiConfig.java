package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Open API 연동 설정 (한국투자증권 등)
 */
@Table("OpenApiConfig")
public class OpenApiConfig {

	@Null(groups = Create.class)
	@NotNull(groups = { Update.class, Delete.class })
	@Id
	@Column("id")
	private UUID id;

	@NotNull(groups = { Create.class, Update.class })
	@Column("provider")
	private String provider;

	@NotNull(groups = { Create.class, Update.class })
	@Column("appKey")
	private String appKey;

	@NotNull(groups = { Create.class, Update.class })
	@Column("appSecret")
	private String appSecret;

	@Column("accessToken")
	private String accessToken;

	@Column("tokenUpdatedDate")
	private Instant tokenUpdatedDate;

	@LastModifiedDate
	@Column("updatedDate")
	private Instant updatedDate;

	public interface Create {}
	public interface Update {}
	public interface Delete {}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public Instant getTokenUpdatedDate() {
		return tokenUpdatedDate;
	}

	public void setTokenUpdatedDate(Instant tokenUpdatedDate) {
		this.tokenUpdatedDate = tokenUpdatedDate;
	}

	public Instant getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Instant updatedDate) {
		this.updatedDate = updatedDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		OpenApiConfig other = (OpenApiConfig) obj;
		return Objects.equals(id, other.id) && Objects.equals(provider, other.provider);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, provider);
	}
}
