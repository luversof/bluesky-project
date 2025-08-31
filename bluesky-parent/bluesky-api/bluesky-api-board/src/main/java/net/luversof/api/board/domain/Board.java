package net.luversof.api.board.domain;

import java.util.BitSet;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
public class Board {

	@Null(groups = Create.class)
	@NotNull(groups = Update.class)
	@Id
	private UUID id;
	
	private String alias;
	
	private BitSet bitConfig;
	
//	@JdbcTypeCode(SqlTypes.JSON)
//	private Map<String, String> jsonConfig;
	private JsonConfig jsonConfig;
	
	public interface Create {}
	public interface Update {}
	
	
	@Data
	public static class JsonConfig {
		String key1;
		String key2;
		String key3;
	}
	
}
