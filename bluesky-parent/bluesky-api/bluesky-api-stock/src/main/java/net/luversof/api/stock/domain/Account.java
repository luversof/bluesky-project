package net.luversof.api.stock.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * 계좌
 */
@Data
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
}
