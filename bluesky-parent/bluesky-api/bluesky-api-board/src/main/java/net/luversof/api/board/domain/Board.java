package net.luversof.api.board.domain;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@Table(name = "Board")
public class Board {

	@Null(groups = Create.class)
	@NotNull(groups = Update.class)
	@Id
	@Column("id")
	private UUID id;
	
	@Column("alias")
	private String alias;
	
//	@JdbcTypeCode(SqlTypes.JSON)
	@Column("jsonConfig")
	private Map<String, String> jsonConfig;
//	private JsonConfig jsonConfig;
	
	public interface Create {}
	public interface Update {}
	
	
	@Data
	public static class JsonConfig {
		String key1;
		String key2;
		String key3;
	}
	
}
