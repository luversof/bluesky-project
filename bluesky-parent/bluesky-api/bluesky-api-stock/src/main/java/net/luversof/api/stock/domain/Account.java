package net.luversof.api.stock.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

/**
 * 계좌
 */
@Data
@Table("Account")
public class Account {

	@Id
	@Column("id")
	private UUID id;
	
	@Column("user_id")
	UUID userId;
	
	private String name;
	
	@CreatedDate
	@Column("createdDate")
	private OffsetDateTime createdDate;
	
	@Column("jsonConfig")
	private Map<String, Object> jsonConfig;
}
